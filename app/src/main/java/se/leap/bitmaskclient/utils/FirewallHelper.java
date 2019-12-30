package se.leap.bitmaskclient.utils;
/**
 * Copyright (c) 2019 LEAP Encryption Access Project and contributers
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

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.lang.ref.WeakReference;

import de.blinkt.openvpn.core.VpnStatus;

import static se.leap.bitmaskclient.utils.Cmd.runBlockingCmd;

interface FirewallCallback {
    void onFirewallStarted(boolean success);
    void onFirewallStopped(boolean success);
    void onSuRequested(boolean success);
}


public class FirewallHelper implements FirewallCallback {
    private static String BITMASK_CHAIN = "bitmask_fw";
    private static final String TAG = FirewallHelper.class.getSimpleName();

    private Context context;

    public FirewallHelper(Context context) {
        this.context = context;
    }


    @Override
    public void onFirewallStarted(boolean success) {
        if (success) {
            VpnStatus.logInfo("[FIREWALL] custom rules established");
        } else {
            VpnStatus.logError("[FIREWALL] could not establish custom rules.");
        }
    }

    @Override
    public void onFirewallStopped(boolean success) {
        if (success) {
            VpnStatus.logInfo("[FIREWALL] custom rules deleted");
        } else {
            VpnStatus.logError("[FIREWALL] could not delete custom rules");
        }
    }

    @Override
    public void onSuRequested(boolean success) {
        PreferenceHelper.setSuPermission(context, success);
        if (!success) {
            VpnStatus.logError("[FIREWALL] Bitmask needs root permission to execute custom firewall rules.");
        }
    }


    private static class StartFirewallTask extends  AsyncTask<Void, Boolean, Boolean> {

       WeakReference<FirewallCallback> callbackWeakReference;

        StartFirewallTask(FirewallCallback callback) {
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
                try {
                    callbackWeakReference.get().onSuRequested(allowSu);
                    Thread.sleep(1000);
                } catch (Exception e) {
                    //ignore
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
                    Log.d(TAG, "added " + BITMASK_CHAIN + " to ip6tables: " + success);
                    Log.d(TAG, log.toString());
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
                Log.e(TAG, log.toString());
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

    private static class ShutdownFirewallTask extends AsyncTask<Void, Boolean, Boolean> {

        WeakReference<FirewallCallback> callbackWeakReference;

        ShutdownFirewallTask(FirewallCallback callback) {
            callbackWeakReference = new WeakReference<>(callback);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            boolean success;
            StringBuilder log = new StringBuilder();
            String[] deleteChain = new String[]{
                    "su",
                    "id",
                    "ip6tables --delete OUTPUT --jump " + BITMASK_CHAIN,
                    "ip6tables --flush " + BITMASK_CHAIN,
                    "ip6tables --delete-chain " + BITMASK_CHAIN
            };
            try {
                success = runBlockingCmd(deleteChain, log) == 0;
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, log.toString());
                return false;
            }

            try {
                boolean allowSu = log.toString().contains("uid=0");
                callbackWeakReference.get().onSuRequested(allowSu);
            } catch (Exception e) {
                //ignore
            }
            return success;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            FirewallCallback callback = callbackWeakReference.get();
            if (callback != null) {
                callback.onFirewallStopped(result);
            }
        }
    }


    public void startFirewall() {
        StartFirewallTask task = new StartFirewallTask(this);
        task.execute();
    }

    public void shutdownFirewall() {
        ShutdownFirewallTask task = new ShutdownFirewallTask(this);
        task.execute();
    }

}
