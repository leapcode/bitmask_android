/**
 * Copyright (c) 2020 LEAP Encryption Access Project and contributers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package se.leap.bitmaskclient.tethering;

import android.content.Context;
import android.content.IntentFilter;

import java.net.NetworkInterface;
import java.util.Enumeration;

import se.leap.bitmaskclient.utils.Cmd;

/**
 * This manager tries to figure out the current tethering states for Wifi, USB and Bluetooth
 * The default behavior differs for failing attempts to get these states:
 * Wifi: keeps old state
 * USB: defaults to false
 * Bluetooth defaults to false
 * For Wifi there's a second method to check the current state (see TetheringBroadcastReceiver).
 * Either of both methods can change the state if they succeed, but are ignored if they fail.
 * This should avoid any interference between both methods.
 */
public class TetheringStateManager {
    private static final String TAG = TetheringStateManager.class.getSimpleName();
    private static TetheringStateManager instance;

    private WifiManagerWrapper wifiManager;

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
        instance.wifiManager = new WifiManagerWrapper(context);
        updateWifiTetheringState();
        updateUsbTetheringState();
        updateBluetoothTetheringState();
    }

    private static boolean isWifiApEnabled() throws Exception {
        return instance.wifiManager.isWifiAPEnabled();
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
        boolean lastState = TetheringObservable.getInstance().isWifiTetheringEnabled();
        try {
            boolean currentState = isWifiApEnabled();
            if (currentState != lastState) {
                TetheringObservable.setWifiTethering(currentState);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
