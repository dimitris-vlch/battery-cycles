package gr.dimitris.batterycycles;

import android.content.Context;
import android.content.SharedPreferences;
import io.github.muntashirakon.adb.AdbStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class BatteryReader {
  public static final int PAIRING = 1, NOT_FOUND = 2, UNSUPPORTED = 3, OTHER = 4;

  public static class ReadException extends Exception {
    public final int kind;
    ReadException(int kind, Throwable cause) { super(cause); this.kind = kind; }
  }

  public static class Result {
    public long usage = -1; public String asoc, bsoh, first; public long time;
    public long cycles() { return Math.round(usage / 100.0); }
  }

  static String val(String dump, String key) {
    for (String l : dump.split("\n")) {
      int i = l.indexOf(key);
      if (i >= 0) return l.substring(i + key.length()).replaceAll("[\\[\\]\\s]", "");
    }
    return null;
  }

  private static final String END = "__BC_END__";

  private static String dump(AdbManager m) throws Exception {
    // adbd does not always signal the end of the stream, so the command prints its own end marker
    // and a watchdog closes the stream if nothing arrives in time.
    final AdbStream s = m.openStream("shell:dumpsys battery | grep -E 'mSavedBattery|FirstUseDate'; echo " + END);
    Thread watchdog = new Thread(() -> {
      try { Thread.sleep(8000); s.close(); } catch (Throwable ignored) { }
    });
    watchdog.start();
    try (InputStream in = s.openInputStream()) {
      ByteArrayOutputStream bo = new ByteArrayOutputStream();
      byte[] buf = new byte[8192]; int n;
      while (bo.toString("UTF-8").indexOf(END) < 0 && (n = in.read(buf)) != -1) bo.write(buf, 0, n);
      String out = bo.toString("UTF-8");
      if (out.indexOf(END) < 0) throw new IOException("no end marker");
      return out;
    } finally {
      watchdog.interrupt();
      try { s.close(); } catch (Throwable ignored) { }
    }
  }

  private static void connect(Context c, AdbManager m) throws ReadException {
    try {
      if (!m.autoConnect(c, 10000)) throw new ReadException(NOT_FOUND, null);
    } catch (ReadException e) {
      throw e;
    } catch (Throwable e) {
      if (e.getClass().getSimpleName().contains("PairingRequired")) throw new ReadException(PAIRING, e);
      throw new ReadException(NOT_FOUND, e);
    }
  }

  /** Connects to the phone's own adbd (must already be paired) and reads the Samsung fields. */
  public static synchronized Result read(Context c) throws ReadException {
    AdbManager m;
    try { m = AdbManager.get(c); } catch (Throwable e) { throw new ReadException(OTHER, e); }
    String dump;
    try {
      if (!m.isConnected()) connect(c, m);
      dump = dump(m);
    } catch (ReadException e) {
      throw e;
    } catch (Throwable first) {
      // The saved connection was stale (Wireless debugging was toggled, Wi-Fi changed...). Reconnect once.
      try { m.disconnect(); } catch (Throwable ignored) { }
      connect(c, m);
      try { dump = dump(m); } catch (Throwable e) { throw new ReadException(OTHER, e); }
    }
    Result r = new Result();
    try { r.usage = Long.parseLong(String.valueOf(val(dump, "mSavedBatteryUsage:"))); }
    catch (NumberFormatException e) { throw new ReadException(UNSUPPORTED, e); }
    if (r.usage < 0) throw new ReadException(UNSUPPORTED, null);
    r.asoc = val(dump, "mSavedBatteryAsoc:");
    r.bsoh = val(dump, "mSavedBatteryBsoh:");
    r.first = val(dump, "FirstUseDate:");
    r.time = System.currentTimeMillis();
    c.getSharedPreferences("last", 0).edit().putLong("usage", r.usage).putString("asoc", r.asoc)
      .putString("bsoh", r.bsoh).putString("first", r.first).putLong("time", r.time).apply();
    return r;
  }

  public static Result cached(Context c) {
    SharedPreferences p = c.getSharedPreferences("last", 0);
    if (!p.contains("usage")) return null;
    Result r = new Result();
    r.usage = p.getLong("usage", -1); r.asoc = p.getString("asoc", null);
    r.bsoh = p.getString("bsoh", null); r.first = p.getString("first", null); r.time = p.getLong("time", 0);
    return r;
  }
}
