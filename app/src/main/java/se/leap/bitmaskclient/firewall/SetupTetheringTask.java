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
import java.util.Enumeration;

import se.leap.bitmaskclient.tethering.TetheringObservable;
import se.leap.bitmaskclient.tethering.TetheringState;

import static se.leap.bitmaskclient.firewall.FirewallManager.BITMASK_FORWARD;
import static se.leap.bitmaskclient.firewall.FirewallManager.BITMASK_POSTROUTING;
import static se.leap.bitmaskclient.utils.Cmd.runBlockingCmd;

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
            callbackWeakReference.get().onSuRequested(allowSu);
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
            } else if (!tetheringState.isVpnWifiTetheringAllowed){
                success = removeWifiTetheringRules(tetheringState, log);
            }

            if (tetheringState.tetherUsbVpn()) {
                success = success && addUsbTetheringRules(tetheringState, log);
            } else if (!tetheringState.isVpnUsbTetheringAllowed) {
                success = success && removeUsbTetheringRules(tetheringState, log);
            }

            if (tetheringState.tetherBluetoothVpn()) {
                success = success && addBluetoothTetheringRules(tetheringState, log);
            } else if (!tetheringState.isVpnBluetoothTetheringAllowed) {
                success = success && removeBluetoothTetheringRules(tetheringState, log);
            }
            return success;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(FirewallManager.TAG, log.toString());
        }
        return false;
    }


    //TODO: implement the follwing methods -v
    private boolean removeBluetoothTetheringRules(TetheringState tetheringState, StringBuilder log) {
        return true;
    }

    private boolean removeUsbTetheringRules(TetheringState tetheringState, StringBuilder log) {
        return true;
    }

    private boolean addBluetoothTetheringRules(TetheringState tetheringState, StringBuilder log) {
        return true;
    }

    private boolean addUsbTetheringRules(TetheringState tetheringState, StringBuilder log) {
        return true;
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
        String[] addRules = new String[] {
                "su",
                "iptables -t filter --flush " + BITMASK_FORWARD,
                "iptables -t nat --flush " + BITMASK_POSTROUTING,
                "iptables -t filter --append " + BITMASK_FORWARD + " --jump ACCEPT",
                "iptables -t nat --append " + BITMASK_POSTROUTING + " --jump MASQUERADE",
                "if [[ ! `ip rule show from "+ state.wifiAddress+" lookup 61` ]]; " +
                        "then ip rule add from " + state.wifiAddress + " lookup 61; " +
                        "fi",
                "if [[ ! `ip route list table 61 | grep 'default dev " + getTunName() + " scope link'` ]]; " +
                        "then ip route add default dev " + getTunName() + " scope link table 61; " +
                        "fi",
                "if [[ ! `ip route list table 61 | grep '"+ state.wifiAddress+" dev "+ state.wifiInterface+" scope link'` ]]; " +
                        "then ip route add " + state.wifiAddress + " dev " + state.wifiInterface + " scope link table 61; " +
                        "fi",
                "if [[ ! `ip route list table 61 | grep 'broadcast 255.255.255.255 dev " + state.wifiInterface + " scope link'` ]]; " +
                        "then ip route add broadcast 255.255.255.255 dev " + state.wifiInterface + " scope link table 61; " +
                        "fi"
        };

        return runBlockingCmd(addRules, log) == 0;
    }

    private boolean removeWifiTetheringRules(TetheringState state, StringBuilder log) throws Exception {
        Log.d(TAG, "add Wifi tethering Rules");
        String[] removeRules = new String[] {
                "su",
                "ip route delete broadcast 255.255.255.255 dev " + state.wifiInterface +" scope link table 61",
                "ip route delete " + state.lastWifiAddress + " dev " + state.wifiInterface +" scope link table 61",
                "ip route delete default dev " + getTunName() + " scope link table 61",
                "if [[ `ip rule show from " + state.lastWifiAddress+ " lookup 61` ]]; " +
                        "then ip rule del from " + state.lastWifiAddress + " lookup 61; " +
                        "fi",
        };
        return runBlockingCmd(removeRules, log) == 0;
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
