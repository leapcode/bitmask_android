package se.leap.bitmaskclient.firewall;

import android.os.AsyncTask;
import android.util.Log;

import java.lang.ref.WeakReference;

import se.leap.bitmaskclient.tethering.TetheringState;

import static se.leap.bitmaskclient.firewall.FirewallManager.BITMASK_FORWARD;
import static se.leap.bitmaskclient.firewall.FirewallManager.BITMASK_POSTROUTING;
import static se.leap.bitmaskclient.utils.Cmd.runBlockingCmd;

public class ConfigureTetheringTask extends AsyncTask<TetheringState, Boolean, Boolean> {

    private WeakReference<FirewallCallback> callbackWeakReference;

    ConfigureTetheringTask(FirewallCallback callback) {
        callbackWeakReference = new WeakReference<>(callback);
    }

    @Override
    protected Boolean doInBackground(TetheringState... tetheringStates) {
        TetheringState tetheringState = tetheringStates[0];
        StringBuilder log = new StringBuilder();

        String[] bitmaskChain = new String[]{
                "su",
                "id",
                "iptables --list " + BITMASK_FORWARD + " && iptables --list " + BITMASK_POSTROUTING };

        try {
            boolean hasBitmaskChain = runBlockingCmd(bitmaskChain, log) == 0;
            boolean allowSu = log.toString().contains("uid=0");
            callbackWeakReference.get().onSuRequested(allowSu);
            if (!allowSu) {
                return false;
            }

            boolean success;
            log = new StringBuilder();
            if (hasAnyTetheringEnabled(tetheringState)) {
                if (!hasBitmaskChain) {
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

                String[] addRules = new String[] {
                        "su",
                        "iptables -t filter --flush " + BITMASK_FORWARD,
                        "iptables -t nat --flush " + BITMASK_POSTROUTING,
                        "iptables -t filter --append " + BITMASK_FORWARD + " --jump ACCEPT",
                        "iptables -t nat --append " + BITMASK_POSTROUTING + " --jump MASQUERADE" };
                return runBlockingCmd(addRules, log) == 0;
            } else {
                if (!hasBitmaskChain) return true;
                String[] removeChains = new String[] {
                        "su",
                        "iptables -t filter --delete FORWARD --jump " + BITMASK_FORWARD,
                        "iptables -t nat --delete POSTROUTING --jump " + BITMASK_POSTROUTING,
                        "iptables -t filter --flush " + BITMASK_FORWARD,
                        "iptables -t nat --flush " + BITMASK_POSTROUTING,
                        "iptables -t filter --delete-chain " + BITMASK_FORWARD,
                        "iptables -t nat --delete-chain " + BITMASK_POSTROUTING
                };
                return runBlockingCmd(removeChains, log) == 0;
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
            callback.onTetheringConfigured(result);
        }
    }

    private boolean hasAnyTetheringEnabled(TetheringState state) {
        return state.isBluetoothTetheringEnabled || state.isUsbTetheringEnabled || state.isWifiTetheringEnabled;
    }
}
