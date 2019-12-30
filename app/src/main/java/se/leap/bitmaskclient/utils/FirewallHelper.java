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

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import java.lang.ref.WeakReference;

import static se.leap.bitmaskclient.utils.Cmd.runBlockingCmd;

interface FirewallCallback {
    void onFirewallStarted(boolean success);
    void onFirewallStopped(boolean success);
}


public class FirewallHelper implements FirewallCallback {
    private static String BITMASK_CHAIN = "bitmask_fw";
    private static final String TAG = FirewallHelper.class.getSimpleName();


    @Override
    public void onFirewallStarted(boolean success) {
        Log.d(TAG, "Firewall started " + success);
    }

    @Override
    public void onFirewallStopped(boolean success) {
        Log.d(TAG, "Firewall stopped " + success);
    }


    static class StartFirewallTask extends  AsyncTask<Void, Boolean, Boolean> {

       WeakReference<FirewallCallback> callbackWeakReference;

        public StartFirewallTask(FirewallCallback callback) {
            callbackWeakReference = new WeakReference<>(callback);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            if (requestSU()) {
                Log.d(TAG, "su acquired");
                StringBuilder log = new StringBuilder();
                String[] bitmaskChain = new String[]{
                        "su",
                        "ip6tables --list " + BITMASK_CHAIN };
                try {
                    boolean hasBitmaskChain = runBlockingCmd(bitmaskChain, log) == 0;
                    Log.d(TAG, log.toString());
                    if (!hasBitmaskChain) {
                        String[] createChain = new String[]{
                                "su",
                                "ip6tables --new-chain " + BITMASK_CHAIN,
                                "ip6tables --insert OUTPUT --jump " + BITMASK_CHAIN };
                        log = new StringBuilder();
                        int success = runBlockingCmd(createChain, log);
                        Log.d(TAG, "added " + BITMASK_CHAIN + " to ip6tables: " + success);
                        Log.d(TAG, log.toString());
                        if (success != 0) {
                            return false;
                        }
                    }

                    log = new StringBuilder();
                    String[] addRules = new String[] {
                            "su",
                            "ip6tables --append " + BITMASK_CHAIN + " -p tcp --jump REJECT",
                            "ip6tables --append " + BITMASK_CHAIN + " -p udp --jump REJECT" };
                    boolean successResult = runBlockingCmd(addRules, log) == 0;
                    Log.d(TAG, log.toString());
                    return successResult;
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, log.toString());
                }
            };
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

    static class ShutdownFirewallTask extends AsyncTask<Void, Boolean, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {

            if (requestSU()) {
                StringBuilder log = new StringBuilder();
                String[] deleteChain = new String[]{
                        "su",
                        "ip6tables --delete OUTPUT --jump " + BITMASK_CHAIN,
                        "ip6tables --flush " + BITMASK_CHAIN,
                        "ip6tables --delete-chain " + BITMASK_CHAIN
                };
                try {
                    runBlockingCmd(deleteChain, log);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, log.toString());
                }

            }

            return null;
        }
    }


    public void startFirewall() {
        StartFirewallTask task = new StartFirewallTask(this);
        task.execute();
    }

    public void shutdownFirewall() {
        ShutdownFirewallTask task = new ShutdownFirewallTask();
        task.execute();
    }

    public static boolean hasSU() {
        StringBuilder log = new StringBuilder();

        try {
            String suCommand = "su -v";
            runBlockingCmd(new String[]{suCommand}, log);
        } catch (Exception e) {
            return false;
        }

        return !TextUtils.isEmpty(log) && !log.toString().contains("su: not found");
    }

    public static boolean requestSU() {
        try {
            String suCommand = "su";
            return  runBlockingCmd(new String[]{suCommand}, null) == 0;
        } catch (Exception e) {
            return false;
        }
    }

}
