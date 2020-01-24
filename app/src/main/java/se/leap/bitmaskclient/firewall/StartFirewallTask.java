package se.leap.bitmaskclient.firewall;

import android.os.AsyncTask;
import android.util.Log;

import java.lang.ref.WeakReference;

import static se.leap.bitmaskclient.firewall.FirewallManager.BITMASK_CHAIN;
import static se.leap.bitmaskclient.utils.Cmd.runBlockingCmd;

class StartFirewallTask extends AsyncTask<Void, Boolean, Boolean> {

   private WeakReference<FirewallCallback> callbackWeakReference;

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
