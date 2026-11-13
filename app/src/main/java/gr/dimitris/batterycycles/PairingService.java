package gr.dimitris.batterycycles;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import io.github.muntashirakon.adb.android.AdbMdns;

/** Finds the pairing port of Wireless debugging, asks for the code in a notification, pairs and reads. */
public class PairingService extends Service {
  static final String CHANNEL = "pairing";
  static final String ACTION_PAIR = "gr.dimitris.batterycycles.PAIR";
  static final int ID = 1;
  private AdbMdns mdns;
  private volatile boolean pairing;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable timeout = () -> {
    if (pairing) return;
    notifyDone(this, L.s("Η σύζευξη έληξε", "Pairing timed out"), L.s("Άνοιξε την εφαρμογή για να ξαναδοκιμάσεις.", "Open the app to try again."));
    stop();
  };

  @Override public IBinder onBind(Intent i) { return null; }

  private Notification searching() {
    return build(L.s("Περιμένω τον κωδικό σύζευξης", "Waiting for the pairing code"),
        L.s("Wireless debugging → «Σύζευξη συσκευής με κωδικό σύζευξης».", "Wireless debugging → \"Pair device with pairing code\"."), null, 0);
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    NotificationManager nm = getSystemService(NotificationManager.class);
    nm.createNotificationChannel(new NotificationChannel(CHANNEL, L.s("Σύζευξη", "Pairing"), NotificationManager.IMPORTANCE_HIGH));
    if (intent != null && ACTION_PAIR.equals(intent.getAction())) { pair(intent, nm); return START_NOT_STICKY; }
    startForeground(ID, searching(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
    pairing = false;
    handler.removeCallbacks(timeout);
    handler.postDelayed(timeout, 5 * 60 * 1000);
    if (mdns == null) {
      mdns = new AdbMdns(this, AdbMdns.SERVICE_TYPE_TLS_PAIRING, (addr, port) -> {
        if (pairing) return;
        if (port <= 0 || addr == null) { nm.notify(ID, searching()); return; }
        // One notification only: Samsung groups a second one and hides the "Code" button deeper.
        nm.notify(ID, prompt(L.s("Γράψε εδώ τον κωδικό σύζευξης", "Type the pairing code here"), addr.getHostAddress(), port));
      });
      mdns.start();
    }
    return START_NOT_STICKY;
  }

  private void pair(Intent intent, NotificationManager nm) {
    Bundle r = RemoteInput.getResultsFromIntent(intent);
    String host = intent.getStringExtra("host");
    int port = intent.getIntExtra("port", 0);
    String code = r == null ? "" : String.valueOf(r.getCharSequence("code")).replaceAll("\\D", "");
    if (code.length() != 6) {
      nm.notify(ID, prompt(L.s("Ο κωδικός έχει 6 ψηφία, ξαναγράψε τον", "The code has 6 digits, type it again"), host, port));
      return;
    }
    pairing = true;
    startForeground(ID, build(L.s("Σύζευξη…", "Pairing…"), "", null, 0), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
    new Thread(() -> {
      try {
        if (!AdbManager.get(this).pair(host, port, code)) throw new Exception("pair");
        try {
          BatteryReader.Result res = BatteryReader.read(this);
          notifyDone(this, L.s("Έτοιμο: " + res.cycles() + " κύκλοι φόρτισης", "Done: " + res.cycles() + " charge cycles"),
              L.s("Πάτα για να ανοίξεις την εφαρμογή.", "Tap to open the app."));
        } catch (Throwable e) {
          notifyDone(this, L.s("Η σύζευξη έγινε", "Paired"), L.s("Πάτα για να ανοίξεις την εφαρμογή και να δεις το αποτέλεσμα.", "Tap to open the app and see the result."));
        }
      } catch (Throwable e) {
        notifyDone(this, L.s("Η σύζευξη απέτυχε", "Pairing failed"),
            L.s("Έλεγξε τον κωδικό και ξαναδοκίμασε από την εφαρμογή.", "Check the code and try again from the app."));
      } finally {
        handler.post(this::stop);
      }
    }).start();
  }

  private void stop() {
    handler.removeCallbacks(timeout);
    stopForeground(true);
    stopSelf();
  }

  Notification prompt(String title, String host, int port) {
    return build(title, L.s("Πάτα το βελάκι ⌄ στα δεξιά, μετά «Κωδικός», και γράψε τα 6 ψηφία.",
        "Tap the arrow ⌄ on the right, then \"Code\", and type the 6 digits."), host, port);
  }

  Notification build(String title, String text, String host, int port) {
    Notification.Builder b = new Notification.Builder(this, CHANNEL)
        .setSmallIcon(android.R.drawable.stat_notify_sync).setContentTitle(title).setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text)).setOngoing(true);
    if (host != null) {
      Intent i = new Intent(this, PairingService.class).setAction(ACTION_PAIR).putExtra("host", host).putExtra("port", port);
      PendingIntent pi = PendingIntent.getService(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
      RemoteInput ri = new RemoteInput.Builder("code").setLabel(L.s("6ψήφιος κωδικός", "6-digit code")).build();
      b.addAction(new Notification.Action.Builder(Icon.createWithResource(this, android.R.drawable.ic_menu_edit),
          L.s("Κωδικός", "Code"), pi).addRemoteInput(ri).build());
    }
    return b.build();
  }

  @Override public void onDestroy() {
    handler.removeCallbacks(timeout);
    if (mdns != null) mdns.stop();
    super.onDestroy();
  }

  static void notifyDone(Context c, String title, String text) {
    PendingIntent open = PendingIntent.getActivity(c, 1, new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    c.getSystemService(NotificationManager.class).notify(2, new Notification.Builder(c, CHANNEL)
        .setSmallIcon(android.R.drawable.stat_notify_sync).setContentTitle(title).setContentText(text)
        .setContentIntent(open).setAutoCancel(true).build());
  }
}
