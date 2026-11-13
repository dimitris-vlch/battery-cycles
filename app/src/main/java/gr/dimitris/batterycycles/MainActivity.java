package gr.dimitris.batterycycles;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends Activity {
  TextView big, info, setupTitle, steps, note;
  Button actionBtn, refreshBtn;
  LinearLayout setup;
  volatile boolean busy;

  int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

  @Override protected void onCreate(Bundle b) {
    super.onCreate(b);
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setGravity(Gravity.CENTER_HORIZONTAL);
    root.setPadding(dp(24), dp(32), dp(24), dp(24));
    big = new TextView(this); big.setTextSize(64); big.setGravity(Gravity.CENTER);
    info = new TextView(this); info.setTextSize(16); info.setPadding(0, dp(12), 0, dp(12)); info.setTextIsSelectable(true);
    setup = new LinearLayout(this); setup.setOrientation(LinearLayout.VERTICAL);
    setupTitle = new TextView(this); setupTitle.setTextSize(18); setupTitle.setPadding(0, dp(8), 0, dp(8));
    steps = new TextView(this); steps.setTextSize(16); steps.setLineSpacing(dp(4), 1f);
    note = new TextView(this); note.setTextSize(15); note.setPadding(0, dp(12), 0, dp(12));
    actionBtn = new Button(this);
    setup.addView(setupTitle); setup.addView(steps); setup.addView(note); setup.addView(actionBtn);
    refreshBtn = new Button(this); refreshBtn.setText(L.s("Ανανέωση", "Refresh"));
    root.addView(big); root.addView(info); root.addView(setup); root.addView(refreshBtn);
    ScrollView scroll = new ScrollView(this);
    scroll.setFitsSystemWindows(true);
    scroll.addView(root);
    setContentView(scroll);
    setup.setVisibility(View.GONE);
    refreshBtn.setOnClickListener(v -> refresh());
  }

  @Override protected void onResume() { super.onResume(); refresh(); }

  @Override public void onRequestPermissionsResult(int code, String[] p, int[] r) {
    if (r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED) { startPairing(); return; }
    Toast.makeText(this, L.s("Χρειάζονται οι ειδοποιήσεις: εκεί γράφεις τον κωδικό σύζευξης.", "Notifications are needed: that is where you type the pairing code."), Toast.LENGTH_LONG).show();
    try { startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); } catch (Exception ignored) { }
  }

  // ---- state checks ----
  boolean wifi() {
    ConnectivityManager cm = getSystemService(ConnectivityManager.class);
    for (Network n : cm.getAllNetworks()) {
      NetworkCapabilities c = cm.getNetworkCapabilities(n);
      // A hotspot shows up as a local Wi-Fi network without the INTERNET capability: it does not count.
      if (c != null && c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return true;
    }
    return false;
  }
  boolean devOptions() { return Settings.Global.getInt(getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1; }
  /** TRUE/FALSE, or null when the system does not let us read the switch. */
  Boolean wirelessDebugging() {
    try { return Settings.Global.getInt(getContentResolver(), "adb_wifi_enabled", 0) == 1; } catch (Throwable e) { return null; }
  }

  // ---- actions ----
  void open(Intent i, String fallback) {
    // Own task: otherwise Settings stays on top of this app and the launcher icon reopens Settings.
    try { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); } catch (Exception e) { Toast.makeText(this, fallback, Toast.LENGTH_LONG).show(); }
  }
  void openDevOptions() {
    open(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).putExtra(":settings:fragment_args_key", "toggle_adb_wireless"),
        L.s("Άνοιξε Ρυθμίσεις → Επιλογές προγραμματιστή.", "Open Settings → Developer options."));
  }
  void startPairing() {
    if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
      requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
      return;
    }
    startForegroundService(new Intent(this, PairingService.class));
    Toast.makeText(this, L.s("Πάτα «Wireless debugging» → «Σύζευξη συσκευής με κωδικό σύζευξης» και γράψε τον κωδικό στην ειδοποίηση.",
        "Tap \"Wireless debugging\" → \"Pair device with pairing code\" and type the code in the notification."), Toast.LENGTH_LONG).show();
    openDevOptions();
  }

  // ---- rendering ----
  static String date(String raw) {
    if (raw != null && raw.matches("\\d{8}")) return raw.substring(6) + "/" + raw.substring(4, 6) + "/" + raw.substring(0, 4);
    return raw;
  }

  void showResult(BatteryReader.Result r, boolean fresh) {
    big.setText(String.valueOf(r.cycles()));
    StringBuilder s = new StringBuilder(L.s("κύκλοι φόρτισης (εκτίμηση)\n\n", "charge cycles (estimate)\n\n"));
    if (r.asoc != null) s.append(L.s("Υγεία μπαταρίας (ASOC): ", "Battery health (ASOC): ")).append(r.asoc).append("%\n");
    if (r.bsoh != null) s.append(L.s("Υγεία μπαταρίας (BSOH): ", "Battery health (BSOH): ")).append(r.bsoh).append("%\n");
    if (r.first != null) s.append(L.s("Πρώτη χρήση: ", "First use: ")).append(date(r.first)).append('\n');
    s.append(L.s("\nΥπολογίζεται από το συνολικό ποσοστό εκφόρτισης (", "\nCalculated from the total discharged percentage (")).append(r.usage)
     .append(L.s("% ÷ 100). Δεν είναι επίσημη τιμή της Samsung.", "% ÷ 100). Not an official Samsung value."));
    if (!fresh) s.append(L.s("\n\nΤελευταία ανάγνωση: ", "\n\nLast read: ")).append(DateFormat.getDateTimeInstance().format(new Date(r.time)));
    info.setText(s.toString());
  }

  static String mark(Boolean ok) { return ok == null ? "•  " : ok ? "✓  " : "✗  "; }

  void showSetup(int kind, boolean haveCached) {
    if (kind == BatteryReader.UNSUPPORTED) {
      setup.setVisibility(View.GONE);
      if (!haveCached) {
        big.setText("?");
        info.setText(L.s("Αυτό το κινητό δεν δίνει τα στοιχεία κύκλων. Η εφαρμογή δουλεύει σε Samsung Galaxy (δοκιμασμένη σε S23 Ultra).",
            "This phone does not provide the cycle data. The app works on Samsung Galaxy phones (tested on S23 Ultra)."));
      }
      return;
    }
    boolean wifi = wifi(), dev = devOptions();
    Boolean wd = dev ? wirelessDebugging() : Boolean.FALSE;
    Boolean paired = kind == BatteryReader.PAIRING ? Boolean.FALSE : null;
    if (!haveCached) {
      big.setText("—");
      info.setText(L.s("Το Android δεν δίνει τους κύκλους σε απλές εφαρμογές. Γι' αυτό η εφαρμογή συνδέεται μία φορά στο «Wireless debugging» του ίδιου του κινητού. Δεν χρειάζεται υπολογιστής.",
          "Android does not give the cycle count to normal apps, so the app pairs once with this phone's own \"Wireless debugging\". No computer needed."));
    }
    setupTitle.setText(haveCached ? L.s("Για νέα ανάγνωση", "To read again") : L.s("Ρύθμιση (μία φορά)", "Setup (once)"));
    steps.setText(mark(wifi) + L.s("1. Σύνδεση σε Wi‑Fi (όχι hotspot)", "1. Connected to Wi‑Fi (not hotspot)") + "\n"
        + mark(dev) + L.s("2. Επιλογές προγραμματιστή", "2. Developer options") + "\n"
        + mark(wd) + L.s("3. Wireless debugging ενεργό", "3. Wireless debugging on") + "\n"
        + mark(paired) + L.s("4. Σύζευξη με κωδικό", "4. Pair with code"));
    if (!wifi) {
      note.setText(L.s("Σύνδεσε το κινητό σε ένα δίκτυο Wi‑Fi. Το Wireless debugging δεν ανοίγει με δεδομένα κινητής ή hotspot.",
          "Connect the phone to a Wi‑Fi network. Wireless debugging does not start on mobile data or hotspot."));
      actionBtn.setText(L.s("Άνοιγμα ρυθμίσεων Wi‑Fi", "Open Wi‑Fi settings"));
      actionBtn.setOnClickListener(v -> open(new Intent(Settings.ACTION_WIFI_SETTINGS), L.s("Άνοιξε τις ρυθμίσεις Wi‑Fi.", "Open Wi‑Fi settings.")));
    } else if (!dev) {
      note.setText(L.s("Άνοιξε «Πληροφορίες λογισμικού» και πάτα 7 φορές τον «Αριθμό δόμησης» (Build number). Μετά γύρνα εδώ.",
          "Open \"Software information\" and tap \"Build number\" 7 times. Then come back here."));
      actionBtn.setText(L.s("Άνοιγμα «Σχετικά με το τηλέφωνο»", "Open \"About phone\""));
      actionBtn.setOnClickListener(v -> open(new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS), L.s("Άνοιξε Ρυθμίσεις → Σχετικά με το τηλέφωνο.", "Open Settings → About phone.")));
    } else if (kind == BatteryReader.PAIRING && !Boolean.FALSE.equals(wd)) {
      note.setText(L.s("Πάτα το κουμπί. Θα ανοίξουν οι Επιλογές προγραμματιστή: πάτα «Wireless debugging» → «Σύζευξη συσκευής με κωδικό σύζευξης» Μετά τράβηξε κάτω τις ειδοποιήσεις, πάτα το βελάκι ⌄ στην ειδοποίηση του Battery Cycles, «Κωδικός», και γράψε τον 6ψήφιο κωδικό.",
          "Tap the button. Developer options will open: tap \"Wireless debugging\" → \"Pair device with pairing code\" Then pull down the notifications, tap the arrow ⌄ on the Battery Cycles notification, \"Code\", and type the 6-digit code."));
      actionBtn.setText(L.s("Έναρξη σύζευξης", "Start pairing"));
      actionBtn.setOnClickListener(v -> startPairing());
    } else if (Boolean.TRUE.equals(wd)) {
      note.setText(L.s("Το Wireless debugging είναι ενεργό αλλά δεν βρέθηκε. Κλείσε το και ξανάνοιξέ το, και πάτα «Ανανέωση».",
          "Wireless debugging is on but was not found. Turn it off and on again, then tap \"Refresh\"."));
      actionBtn.setText(L.s("Άνοιγμα Επιλογών προγραμματιστή", "Open Developer options"));
      actionBtn.setOnClickListener(v -> openDevOptions());
    } else {
      note.setText(L.s("Ενεργοποίησε το «Wireless debugging» (Ασύρματος εντοπισμός σφαλμάτων) και γύρνα εδώ.",
          "Turn on \"Wireless debugging\" and come back here."));
      actionBtn.setText(L.s("Άνοιγμα Επιλογών προγραμματιστή", "Open Developer options"));
      actionBtn.setOnClickListener(v -> openDevOptions());
    }
    setup.setVisibility(View.VISIBLE);
  }

  void refresh() {
    if (busy) return;
    BatteryReader.Result cached = BatteryReader.cached(this);
    if (cached != null) showResult(cached, false);
    else { big.setText("…"); info.setText(L.s("Διάβασμα…", "Reading…")); }
    refreshBtn.setEnabled(false);
    busy = true;
    new Thread(() -> {
      try {
        BatteryReader.Result r = BatteryReader.read(this);
        runOnUiThread(() -> { showResult(r, true); setup.setVisibility(View.GONE); });
      } catch (BatteryReader.ReadException e) {
        runOnUiThread(() -> showSetup(e.kind, cached != null));
      } catch (Throwable e) {
        runOnUiThread(() -> showSetup(BatteryReader.OTHER, cached != null));
      } finally {
        busy = false;
        runOnUiThread(() -> refreshBtn.setEnabled(true));
      }
    }).start();
  }
}
