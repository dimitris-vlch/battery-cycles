package gr.dimitris.batterycycles;

import android.app.Application;
import java.security.Security;
import org.conscrypt.Conscrypt;

public class App extends Application {
  @Override public void onCreate() {
    super.onCreate();
    Security.insertProviderAt(Conscrypt.newProvider(), 1);
  }
}
