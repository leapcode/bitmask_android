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

import static se.leap.bitmaskclient.firewall.FirewallManager.BITMASK_CHAIN;
import static se.leap.bitmaskclient.base.utils.Cmd.runBlockingCmd;

class StartIPv6FirewallTask extends AsyncTask<Void, Boolean, Boolean> {

   private WeakReference<FirewallCallback> callbackWeakReference;

    StartIPv6FirewallTask(FirewallCallback callback) {
        callbackWeakReference = new WeakReference<>(callback);
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        StringBuilder log = new StringBuilder();
        String[] bitmaskChain = new String[]{
                "su",
                "id",
                "ip6tables --list " + BITMASK_CHAIN };


        try {
            boolean hasBitmaskChain = runBlockingCmd(bitmaskChain, log) == 0;
            boolean allowSu = log.toString().contains("uid=0");
            callbackWeakReference.get().onSuRequested(allowSu);
            if (!allowSu) {
                return false;
            }

            boolean success;
            log = new StringBuilder();
            if (!hasBitmaskChain) {
                String[] createChainAndRules = new String[]{
                        "su",
                        "ip6tables --new-chain " + BITMASK_CHAIN,
                        "ip6tables --insert OUTPUT --jump " + BITMASK_CHAIN,
                        "ip6tables --append " + BITMASK_CHAIN + " -p tcp --jump REJECT",
                        "ip6tables --append " + BITMASK_CHAIN + " -p udp --jump REJECT"
                };
                success = runBlockingCmd(createChainAndRules, log) == 0;
                Log.d(FirewallManager.TAG, "added " + BITMASK_CHAIN + " to ip6tables: " + success);
                Log.d(FirewallManager.TAG, log.toString());
                return success;
            } else {
                String[] addRules = new String[] {
                        "su",
                        "ip6tables --append " + BITMASK_CHAIN + " -p tcp --jump REJECT",
                        "ip6tables --append " + BITMASK_CHAIN + " -p udp --jump REJECT" };
                return runBlockingCmd(addRules, log) == 0;
            }
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
            callback.onFirewallStarted(result);
        }
    }
}
