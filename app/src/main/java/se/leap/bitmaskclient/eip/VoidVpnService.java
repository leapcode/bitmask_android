package se.leap.bitmaskclient.eip;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;

import java.io.IOException;

public class VoidVpnService extends VpnService  {

    static final String TAG = VoidVpnService.class.getSimpleName();
    static ParcelFileDescriptor fd;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : "";
        if (action == Constants.START_BLOCKING_VPN_PROFILE) {
            start();
        }

        return 0;
    }

    protected void start() {
        Builder builder = new Builder();
        builder.setSession("Blocking until running");
        builder.addAddress("10.42.0.8", 16);
        builder.addRoute("0.0.0.0", 1);
        builder.addRoute("192.168.1.0", 24);
        builder.addDnsServer("10.42.0.1");
        try {
            fd = builder.establish();
            if (fd != null)
                android.util.Log.d(TAG, "VoidVpnService set up");
            else
                android.util.Log.d(TAG, "VoidVpnService failed to set up");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
