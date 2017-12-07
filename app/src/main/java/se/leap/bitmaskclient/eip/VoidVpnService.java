package se.leap.bitmaskclient.eip;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;

import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.VpnStatus;
import se.leap.bitmaskclient.R;

import static se.leap.bitmaskclient.Dashboard.SHARED_PREFERENCES;
import static se.leap.bitmaskclient.eip.Constants.ACTION_START_ALWAYS_ON_EIP;


public class VoidVpnService extends VpnService {

    static final String TAG = VoidVpnService.class.getSimpleName();
    static ParcelFileDescriptor fd;
    static Thread thread;
    private final int ALWAYS_ON_MIN_API_LEVEL = Build.VERSION_CODES.N;
    private static final String STATE_ESTABLISH = "ESTABLISHVOIDVPN";
    private static final String STATE_STOP = "STOPVOIDVPN";

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : "";
        if (action.equals(Constants.START_BLOCKING_VPN_PROFILE)) {
            thread = new Thread(new Runnable() {
                public void run() {
                    establishBlockingVpn();
                    SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
                    preferences.edit().putBoolean(Constants.IS_ALWAYS_ON, false).commit();
                    Log.d(TAG, "start blocking vpn profile - always on = false");
                }
            });
            thread.run();
        } else if (action.equals("android.net.VpnService") && Build.VERSION.SDK_INT >= ALWAYS_ON_MIN_API_LEVEL) {
            //only always-on feature triggers this
            thread = new Thread(new Runnable() {
                public void run() {
                    establishBlockingVpn();
                    SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
                    preferences.edit().putBoolean(Constants.IS_ALWAYS_ON, true).commit();
                    requestVpnWithLastSelectedProfile();
                    Log.d(TAG, "start blocking vpn profile - always on = true");
                }
            });
            thread.run();
        }
        return START_STICKY;
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
        VpnStatus.updateStateString(STATE_STOP, "",
                R.string.void_vpn_stopped, ConnectionStatus.LEVEL_NOTCONNECTED);
    }

    public static boolean isRunning() throws NullPointerException {
        return thread.isAlive() && fd != null;
    }

    private static void closeFd() {
        try {
            if (fd != null)
                fd.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Builder prepareBlockingVpnProfile() {
        Builder builder = new Builder();
        builder.setSession("Blocking until running");
        builder.addRoute("0.0.0.0", 1);
        builder.addRoute("192.168.1.0", 24);
        builder.addDnsServer("10.42.0.1");
        builder.addAddress("10.42.0.8", 16);
        return builder;

    }

    private void establishBlockingVpn() {
        try {
            VpnStatus.logInfo(getString(R.string.void_vpn_establish));
            VpnStatus.updateStateString(STATE_ESTABLISH, "",
                    R.string.void_vpn_establish, ConnectionStatus.LEVEL_BLOCKING);
            Builder builder = prepareBlockingVpnProfile();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)  {
                builder.addDisallowedApplication(getPackageName());
            }

            fd = builder.establish();
        } catch (Exception e) {
            // Catch any exception
            e.printStackTrace();
            VpnStatus.logError(R.string.void_vpn_error_establish);
        }
    }

    private void requestVpnWithLastSelectedProfile() {
        Intent startEIP = new Intent(getApplicationContext(), EIP.class);
        startEIP.setAction(ACTION_START_ALWAYS_ON_EIP);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //noinspection NewApi
            getApplicationContext().startForegroundService(startEIP);
        } else {
            getApplicationContext().startService(startEIP);
        }
    }

}
