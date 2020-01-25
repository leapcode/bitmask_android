package se.leap.bitmaskclient.firewall;

import android.os.AsyncTask;
import android.util.Log;

import java.lang.ref.WeakReference;

import static se.leap.bitmaskclient.firewall.FirewallManager.BITMASK_CHAIN;
import static se.leap.bitmaskclient.utils.Cmd.runBlockingCmd;

class ShutdownIPv6FirewallTask extends AsyncTask<Void, Boolean, Boolean> {

    private WeakReference<FirewallCallback> callbackWeakReference;

    ShutdownIPv6FirewallTask(FirewallCallback callback) {
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
            Log.e(FirewallManager.TAG, log.toString());
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
