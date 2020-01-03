package se.leap.bitmaskclient.tethering;

import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class TetheringObserver {
    private static TetheringObserver instance;

    private boolean isWifiTetheringEnabled;
    private boolean isUsbTetheringEnabled;
    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;
    private TetheringBroadcastReceiver broadcastReceiver;

    private TetheringObserver() {
    }

    public static void init(Context context) {
        if (instance == null) {
            instance = new TetheringObserver();
            instance.broadcastReceiver = new TetheringBroadcastReceiver();
            IntentFilter intentFilter = new IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED");
            intentFilter.addAction("android.net.conn.TETHER_STATE_CHANGED");
            context.getApplicationContext().registerReceiver(instance.broadcastReceiver, intentFilter);
            instance.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            instance.setWifiTetheringEnabled(instance.isWifiApEnabled());
            instance.connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
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

    private boolean getUsbTetheringState() {
        try {

            for(Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface networkInterface = en.nextElement();
                for(Enumeration<InetAddress> enumIpAddr = networkInterface.getInetAddresses(); enumIpAddr.hasMoreElements();){
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if(!networkInterface.isLoopback()){
                        if(networkInterface.getName().contains("rndis") || networkInterface.getName().contains("usb")){
                            return true;
                        }
                    }
                }
            }
        } catch(Exception e){
            e.printStackTrace();
        }

        return false;
    }

    public static TetheringObserver getInstance() {
        if (instance == null) {
            throw new RuntimeException("Call init() first!");
        }

        return instance;
    }

    void setWifiTetheringEnabled(boolean enabled) {
        isWifiTetheringEnabled = enabled;
    }

    public boolean isWifiTetheringEnabled() {
        return isWifiTetheringEnabled;
    }

    void updateUsbTetheringState() {
        isUsbTetheringEnabled = getUsbTetheringState();
    }

    public boolean isUsbTetheringEnabled() {
        return isUsbTetheringEnabled;
    }
}
