package se.leap.bitmaskclient.eip;

import android.content.*;
import android.net.*;
import android.os.*;

import java.io.*;

public class VoidVpnService extends VpnService {

    static final String TAG = VoidVpnService.class.getSimpleName();
    static ParcelFileDescriptor fd;

    static Thread thread;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : "";
        if (action == Constants.START_BLOCKING_VPN_PROFILE) {
            thread = new Thread(new Runnable() {
                public void run() {
                    Builder builder = new Builder();
                    builder.setSession("Blocking until running");
                    builder.addAddress("10.42.0.8", 16);
                    builder.addRoute("0.0.0.0", 1);
                    builder.addRoute("192.168.1.0", 24);
                    builder.addDnsServer("10.42.0.1");
                    try {
                        fd = builder.establish();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.run();
        }
        return 0;
    }

    @Override
    public void onRevoke() {
        super.onRevoke();
        closeFd();
    }

    public static void stop() {
        if (thread != null)
            thread.interrupt();
        closeFd();
    }

    private static void closeFd() {
        try {
            if (fd != null)
                fd.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
