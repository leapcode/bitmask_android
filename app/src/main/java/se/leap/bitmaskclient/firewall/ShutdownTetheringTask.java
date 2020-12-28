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
import java.util.ArrayList;

import se.leap.bitmaskclient.tethering.TetheringObservable;
import se.leap.bitmaskclient.tethering.TetheringState;

import static se.leap.bitmaskclient.firewall.FirewallManager.BITMASK_FORWARD;
import static se.leap.bitmaskclient.firewall.FirewallManager.BITMASK_POSTROUTING;
import static se.leap.bitmaskclient.base.utils.Cmd.runBlockingCmd;

public class ShutdownTetheringTask extends AsyncTask<Void, Boolean, Boolean> {

    private WeakReference<FirewallCallback> callbackWeakReference;

    ShutdownTetheringTask(FirewallCallback callback) {
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

            log = new StringBuilder();

            ArrayList<String> removeChains = new ArrayList<>();
            removeChains.add("su");
            removeChains.add("ip route flush table 61");
            removeChains.add("if [[ `ip rule show from " + tetheringState.lastSeenWifiAddress+ " lookup 61` ]]; " +
                    "then ip rule del from " + tetheringState.lastSeenWifiAddress + " lookup 61; " +
                    "fi");
            removeChains.add("if [[ `ip rule show from " + tetheringState.lastSeenUsbAddress+ " lookup 61` ]]; " +
                    "then ip rule del from " + tetheringState.lastSeenUsbAddress + " lookup 61; " +
                    "fi");
            if (hasBitmaskChain) {
                removeChains.add("iptables -t filter --delete FORWARD --jump " + BITMASK_FORWARD);
                removeChains.add("iptables -t nat --delete POSTROUTING --jump " + BITMASK_POSTROUTING);
                removeChains.add("iptables -t filter --flush " + BITMASK_FORWARD);
                removeChains.add("iptables -t nat --flush " + BITMASK_POSTROUTING);
                removeChains.add("iptables -t filter --delete-chain " + BITMASK_FORWARD);
                removeChains.add("iptables -t nat --delete-chain " + BITMASK_POSTROUTING);
            }
            return runBlockingCmd(removeChains.toArray(new String[0]), log) == 0;

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(FirewallManager.TAG, log.toString());
        }
        return false;
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
