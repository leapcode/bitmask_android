package se.leap.bitmaskclient.tethering;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;

import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.util.Enumeration;

import se.leap.bitmaskclient.utils.Cmd;

public class TetheringStateManager {
    private static final String TAG = TetheringStateManager.class.getSimpleName();
    private static TetheringStateManager instance;

    private WifiManager wifiManager;

    private TetheringStateManager() { }

    public static TetheringStateManager getInstance() {
        if (instance == null) {
            instance = new TetheringStateManager();
        }
        return instance;
    }

    public void init(Context context) {
        TetheringBroadcastReceiver broadcastReceiver = new TetheringBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter("android.net.conn.TETHER_STATE_CHANGED");
        intentFilter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        context.getApplicationContext().registerReceiver(broadcastReceiver, intentFilter);
        instance.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        updateWifiTetheringState();
        updateUsbTetheringState();
        updateBluetoothTetheringState();
    }

    private static boolean isWifiApEnabled() {
        try {
            Method method = instance.wifiManager.getClass().getMethod("getWifiApState");
            int tmp = ((Integer) method.invoke(instance.wifiManager));
            return WifiHotspotState.WIFI_AP_STATE_ENABLED.ordinal() == tmp % 10;
        } catch (Exception e) {
            return false;
        }
    }


    private static boolean getUsbTetheringState() {
        try {
            for(Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface networkInterface = en.nextElement();
                if(!networkInterface.isLoopback()){
                    if(networkInterface.getName().contains("rndis") || networkInterface.getName().contains("usb")){
                        return true;
                    }
                }
            }
        } catch(Exception e){
            e.printStackTrace();
        }

        return false;
    }

    // Check whether Bluetooth tethering is enabled.
    private static boolean isBluetoothTetheringEnabled() {
        StringBuilder log = new StringBuilder();
        boolean hasBtPan = false;
        try {
            hasBtPan = Cmd.runBlockingCmd(new String[] {"ifconfig bt-pan"}, log) == 0;
            //Log.d(TAG, "ifconfig result: " + log.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hasBtPan;

    }

    static void updateUsbTetheringState() {
        TetheringObservable.setUsbTethering(getUsbTetheringState());
    }

    static void updateBluetoothTetheringState() {
        TetheringObservable.setBluetoothTethering(isBluetoothTetheringEnabled());
    }

    static void updateWifiTetheringState() {
        TetheringObservable.setWifiTethering(isWifiApEnabled());
    }

}
