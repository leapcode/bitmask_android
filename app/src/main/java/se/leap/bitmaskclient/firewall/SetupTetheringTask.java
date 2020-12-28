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
package se.leap.bitmaskclient.firewall;

import android.os.AsyncTask;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;

import se.leap.bitmaskclient.tethering.TetheringObservable;
import se.leap.bitmaskclient.tethering.TetheringState;

import static se.leap.bitmaskclient.firewall.FirewallManager.BITMASK_FORWARD;
import static se.leap.bitmaskclient.firewall.FirewallManager.BITMASK_POSTROUTING;
import static se.leap.bitmaskclient.base.utils.Cmd.runBlockingCmd;

public class SetupTetheringTask extends AsyncTask<Void, Boolean, Boolean> {

    private static final String TAG = SetupTetheringTask.class.getSimpleName();
    private WeakReference<FirewallCallback> callbackWeakReference;

    SetupTetheringTask(FirewallCallback callback) {
        callbackWeakReference = new WeakReference<>(callback);
    }

    @Override
    protected Boolean doInBackground(Void... args) {
        TetheringState tetheringState = TetheringObservable.getInstance().getTetheringState();
        StringBuilder log = new StringBuilder();

        String[] bitmaskChain = new String[]{
                "su",
                "id",
                "iptables -t filter --list " + BITMASK_FORWARD + " && iptables -t nat --list " + BITMASK_POSTROUTING };

        try {
            boolean hasBitmaskChain = runBlockingCmd(bitmaskChain, log) == 0;
            boolean allowSu = log.toString().contains("uid=0");
            FirewallCallback callback = callbackWeakReference.get();
            if (callback != null) {
                callback.onSuRequested(allowSu);
            }
            if (!allowSu) {
                return false;
            }

            boolean success = true;
            log = new StringBuilder();

            if (!hasBitmaskChain && tetheringState.hasAnyVpnTetheringAllowed() && tetheringState.hasAnyDeviceTetheringEnabled()) {
                createChains(log);
            }

            if (tetheringState.tetherWifiVpn()) {
                log = new StringBuilder();
                success = addWifiTetheringRules(tetheringState, log);
                logError(success, log);
            } else if (!tetheringState.isVpnWifiTetheringAllowed){
                success = removeWifiTetheringRules(tetheringState, log);
                logError(success, log);
            }

            log = new StringBuilder();
            if (tetheringState.tetherUsbVpn()) {
                success = success && addUsbTetheringRules(tetheringState, log);
                logError(success, log);
            } else if (!tetheringState.isVpnUsbTetheringAllowed) {
                success = success && removeUsbTetheringRules(tetheringState, log);
                logError(success, log);
            }

            log = new StringBuilder();
            if (tetheringState.tetherBluetoothVpn()) {
                success = success && addBluetoothTetheringRules(tetheringState, log);
                logError(success, log);
            } else if (!tetheringState.isVpnBluetoothTetheringAllowed) {
                success = success && removeBluetoothTetheringRules(tetheringState, log);
                logError(success, log);
            }
            return success;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(FirewallManager.TAG, log.toString());
        }
        return false;
    }

    private void logError(boolean success, StringBuilder log) {
        if (!success) {
            Log.e(TAG, log.toString());
        }
    }


    private void createChains(StringBuilder log) throws Exception {
        boolean success;
        String[] createChains = new String[]{
                "su",
                "iptables -t filter --new-chain " + BITMASK_FORWARD,
                "iptables -t nat --new-chain " + BITMASK_POSTROUTING,
                "iptables -t filter --insert FORWARD --jump " + BITMASK_FORWARD,
                "iptables -t nat --insert POSTROUTING --jump " + BITMASK_POSTROUTING,
        };
        success = runBlockingCmd(createChains, log) == 0;
        Log.d(FirewallManager.TAG, "added " + BITMASK_FORWARD + " and " + BITMASK_POSTROUTING+" to iptables: " + success);
        Log.d(FirewallManager.TAG, log.toString());
    }

    private boolean addWifiTetheringRules(TetheringState state, StringBuilder log) throws Exception {
        Log.d(TAG, "add Wifi tethering Rules");
        String[] addRules = getAdditionRules(state.wifiAddress, state.wifiInterface);
        return runBlockingCmd(addRules, log) == 0;
    }

    private boolean removeWifiTetheringRules(TetheringState state, StringBuilder log) throws Exception {
        Log.d(TAG, "remove Wifi tethering Rules");
        String[] removeRules = getDeletionRules(state, state.lastSeenWifiAddress, state.lastSeenWifiInterface);
        return runBlockingCmd(removeRules, log) == 0;
    }

    private boolean addUsbTetheringRules(TetheringState state, StringBuilder log) throws Exception {
        Log.d(TAG, "add usb tethering rules");
        String[] addRules = getAdditionRules(state.usbAddress, state.usbInterface);
        return runBlockingCmd(addRules, log) == 0;
    }

    private boolean removeUsbTetheringRules(TetheringState state, StringBuilder log) throws Exception {
        Log.d(TAG, "remove usb tethering rules");
        String[] addRules = getDeletionRules(state, state.lastSeenUsbAddress, state.lastSeenUsbInterface);
        return runBlockingCmd(addRules, log) == 0;
    }

    private boolean addBluetoothTetheringRules(TetheringState state, StringBuilder log) throws Exception {
        Log.d(TAG, "add bluetooth tethering rules");
        String[] addRules = getAdditionRules(state.bluetoothAddress, state.bluetoothInterface);
        return runBlockingCmd(addRules, log) == 0;
    }

    private boolean removeBluetoothTetheringRules(TetheringState state, StringBuilder log) throws Exception {
        Log.d(TAG, "remove bluetooth tethering rules");
        String[] addRules = getDeletionRules(state, state.lastSeenBluetoothAddress, state.lastSeenBluetoothInterface);
        return runBlockingCmd(addRules, log) == 0;
    }


    private String[] getAdditionRules(String addressRange, String interfaceName) {
        return new String[] {
                "su",
                "iptables -t filter --flush " + BITMASK_FORWARD,
                "iptables -t nat --flush " + BITMASK_POSTROUTING,
                "iptables -t filter --append " + BITMASK_FORWARD + " --jump ACCEPT",
                "iptables -t nat --append " + BITMASK_POSTROUTING + " --jump MASQUERADE",
                "if [[ ! `ip rule show from "+ addressRange+" lookup 61` ]]; " +
                        "then ip rule add from " + addressRange + " lookup 61; " +
                        "fi",
                "if [[ ! `ip route list table 61 | grep 'default dev " + getTunName() + " scope link'` ]]; " +
                        "then ip route add default dev " + getTunName() + " scope link table 61; " +
                        "fi",
                "if [[ ! `ip route list table 61 | grep '"+ addressRange +" dev "+ interfaceName +" scope link'` ]]; " +
                        "then ip route add " + addressRange + " dev " + interfaceName + " scope link table 61; " +
                        "fi",
                "if [[ ! `ip route list table 61 | grep 'broadcast 255.255.255.255 dev " + interfaceName + " scope link'` ]]; " +
                        "then ip route add broadcast 255.255.255.255 dev " + interfaceName + " scope link table 61; " +
                        "fi"
        };
    }

    private String[] getDeletionRules(TetheringState state, String addressRange, String interfaceName) {
        ArrayList<String> list = new ArrayList<>();
        list.add("su");
        list.add("ip route delete broadcast 255.255.255.255 dev " + addressRange +" scope link table 61");
        list.add("ip route delete " + addressRange + " dev " + interfaceName +" scope link table 61");
        if (!state.hasAnyVpnTetheringAllowed() || !state.hasAnyDeviceTetheringEnabled()) {
            list.add("ip route delete default dev " + getTunName() + " scope link table 61");
        }
        list.add("if [[ `ip rule show from " + addressRange + " lookup 61` ]]; " +
                "then ip rule del from " + addressRange + " lookup 61; " +
                "fi");

        return list.toArray(new String[0]);
    }



    private String getTunName() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface networkInterface = en.nextElement();
                if (networkInterface.getName().contains("tun")) {
                    return networkInterface.getName();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        FirewallCallback callback = callbackWeakReference.get();
        if (callback != null) {
            callback.onTetheringStarted(result);
        }
    }
}
