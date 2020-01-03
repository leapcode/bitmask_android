package se.leap.bitmaskclient.tethering;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

public class TetheringBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(intent.getAction())) {
            int apState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
            if (WifiHotspotState.WIFI_AP_STATE_ENABLED.ordinal() == apState % 10) {
                TetheringObserver.getInstance().setWifiTetheringEnabled(true);
            } else {
                TetheringObserver.getInstance().setWifiTetheringEnabled(false);
            }
        } else if ("android.net.conn.TETHER_STATE_CHANGED".equals(intent.getAction())) {
            TetheringObserver.getInstance().updateUsbTetheringState();
        }
    }
}
