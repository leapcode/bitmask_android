package se.leap.bitmaskclient.tethering;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

public class TetheringBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = TetheringBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(intent.getAction())) {
            Log.d(TAG, "TETHERING WIFI_AP_STATE_CHANGED");
            int apState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
            if (WifiHotspotState.WIFI_AP_STATE_ENABLED.ordinal() == apState % 10) {
                TetheringObservable.setWifiTethering(true);
            } else {
                TetheringObservable.setWifiTethering(false);
            }
        } else if ("android.net.conn.TETHER_STATE_CHANGED".equals(intent.getAction())) {
            Log.d(TAG, "TETHERING TETHER_STATE_CHANGED");
            TetheringStateManager.updateUsbTetheringState();
            TetheringStateManager.updateBluetoothTetheringState();
            TetheringStateManager.updateWifiTetheringState();
        }
    }
}
