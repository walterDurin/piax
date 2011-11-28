package org.piax.trans.ts.nfc.nfc;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;

public abstract class Nfc {
    public static Nfc getInstance(String name) {
        if (name.equals("Android")) {
            try {
                if (Build.VERSION.SDK_INT >= 10) {
                    return (Nfc) Class.forName("org.piax.trans.ts.nfc.nfc.android.AndroidNfc").newInstance();
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    public void setActivity(Activity activity) {};
    public void setupReceiver(NfcReceiver nr) {};
    public void setupNotificationResource(int resid) {};
    public void send(byte[] data) {};
    public void onNewIntent(Intent intent) {};
    public void onResume() {};
    public void onPause() {};
}
