package se.leap.bitmaskclient.tethering;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;

import java.lang.reflect.Method;

public class WifiHotspotObserver {
    private static WifiHotspotObserver instance;

    private boolean isEnabled;
    private WifiManager wifiManager;
    private WifiHotspotBroadcastReceiver broadcastReceiver;

    private WifiHotspotObserver() {
    }

    public static void init(Context context) {
        if (instance == null) {
            instance = new WifiHotspotObserver();
            instance.broadcastReceiver = new WifiHotspotBroadcastReceiver();
            IntentFilter intentFilter = new IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED");
            context.getApplicationContext().registerReceiver(instance.broadcastReceiver, intentFilter);
            instance.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            instance.setEnabled(instance.isWifiApEnabled());
        }
    }

    private boolean isWifiApEnabled() {
        try {
            Method method = instance.wifiManager.getClass().getMethod("getWifiApState");
            int tmp = ((Integer) method.invoke(wifiManager));
            return WifiHotspotState.WIFI_AP_STATE_ENABLED.ordinal() == tmp % 10;
        } catch (Exception e) {
            return false;
        }
    }

    public static WifiHotspotObserver getInstance() {
        if (instance == null) {
            throw new RuntimeException("Call init() first!");
        }

        return instance;
    }

    void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public boolean isEnabled() {
        return isEnabled;
    }
}
