package gr.dimitris.batterycycles;

import android.content.Context;
import android.os.Build;
import io.github.muntashirakon.adb.AbsAdbConnectionManager;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.Random;
import android.sun.security.x509.AlgorithmId;
import android.sun.security.x509.CertificateAlgorithmId;
import android.sun.security.x509.CertificateExtensions;
import android.sun.security.x509.CertificateIssuerName;
import android.sun.security.x509.CertificateSerialNumber;
import android.sun.security.x509.CertificateSubjectName;
import android.sun.security.x509.CertificateValidity;
import android.sun.security.x509.CertificateVersion;
import android.sun.security.x509.CertificateX509Key;
import android.sun.security.x509.KeyIdentifier;
import android.sun.security.x509.PrivateKeyUsageExtension;
import android.sun.security.x509.SubjectKeyIdentifierExtension;
import android.sun.security.x509.X500Name;
import android.sun.security.x509.X509CertImpl;
import android.sun.security.x509.X509CertInfo;

/** ADB client that talks to this phone's own Wireless-debugging daemon. */
public class AdbManager extends AbsAdbConnectionManager {
  private static AdbManager INSTANCE;
  private PrivateKey key;
  private Certificate cert;

  public static synchronized AdbManager get(Context c) throws Exception {
    if (INSTANCE == null) INSTANCE = new AdbManager(c.getApplicationContext());
    return INSTANCE;
  }

  private AdbManager(Context c) throws Exception {
    setApi(Build.VERSION.SDK_INT);
    File kf = new File(c.getFilesDir(), "adb_key.pk8");
    File cf = new File(c.getFilesDir(), "adb_cert.der");
    if (kf.exists() && cf.exists()) {
      key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(readAll(kf)));
      cert = CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(readAll(cf)));
      return;
    }
    KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
    g.initialize(2048, new SecureRandom());
    KeyPair kp = g.generateKeyPair();
    PublicKey pub = kp.getPublic();
    key = kp.getPrivate();
    String alg = "SHA512withRSA";
    Date from = new Date(), to = new Date(System.currentTimeMillis() + 10L * 365 * 86400000);
    CertificateExtensions ext = new CertificateExtensions();
    ext.set("SubjectKeyIdentifier", new SubjectKeyIdentifierExtension(new KeyIdentifier(pub).getIdentifier()));
    ext.set("PrivateKeyUsage", new PrivateKeyUsageExtension(from, to));
    X500Name name = new X500Name("CN=Battery Cycles");
    X509CertInfo info = new X509CertInfo();
    info.set("version", new CertificateVersion(2));
    info.set("serialNumber", new CertificateSerialNumber(new Random().nextInt() & Integer.MAX_VALUE));
    info.set("algorithmID", new CertificateAlgorithmId(AlgorithmId.get(alg)));
    info.set("subject", new CertificateSubjectName(name));
    info.set("key", new CertificateX509Key(pub));
    info.set("validity", new CertificateValidity(from, to));
    info.set("issuer", new CertificateIssuerName(name));
    info.set("extensions", ext);
    X509CertImpl impl = new X509CertImpl(info);
    impl.sign(key, alg);
    cert = impl;
    try (FileOutputStream o = new FileOutputStream(kf)) { o.write(key.getEncoded()); }
    try (FileOutputStream o = new FileOutputStream(cf)) { o.write(cert.getEncoded()); }
  }

  private static byte[] readAll(File f) throws Exception {
    try (FileInputStream i = new FileInputStream(f)) {
      byte[] b = new byte[(int) f.length()];
      int n = 0; while (n < b.length) { int r = i.read(b, n, b.length - n); if (r < 0) break; n += r; }
      return b;
    }
  }

  @Override protected PrivateKey getPrivateKey() { return key; }
  @Override protected Certificate getCertificate() { return cert; }
  @Override protected String getDeviceName() { return "BatteryCycles"; }
}
