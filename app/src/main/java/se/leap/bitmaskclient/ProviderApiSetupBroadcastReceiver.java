package se.leap.bitmaskclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import se.leap.bitmaskclient.ProviderSetupInterface.ProviderConfigState;

/**
 * Broadcast receiver that handles callback intents of ProviderApi during provider setup.
 * It is used by CustomProviderSetupActivity for custom branded apps and ProviderListActivity
 * for 'normal' Bitmask.
 *
 * Created by cyberta on 17.08.18.
 */

public class ProviderApiSetupBroadcastReceiver extends BroadcastReceiver {
    private final ProviderSetupInterface setupInterface;

    public ProviderApiSetupBroadcastReceiver(ProviderSetupInterface setupInterface) {
        this.setupInterface = setupInterface;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(ProviderListBaseActivity.TAG, "received Broadcast");

        String action = intent.getAction();
        if (action == null || !action.equalsIgnoreCase(Constants.BROADCAST_PROVIDER_API_EVENT)) {
            return;
        }

        if (setupInterface.getConfigState() != null &&
                setupInterface.getConfigState() == ProviderConfigState.SETTING_UP_PROVIDER) {
            int resultCode = intent.getIntExtra(Constants.BROADCAST_RESULT_CODE, ProviderListBaseActivity.RESULT_CANCELED);
            Log.d(ProviderListBaseActivity.TAG, "Broadcast resultCode: " + Integer.toString(resultCode));

            Bundle resultData = intent.getParcelableExtra(Constants.BROADCAST_RESULT_KEY);
            Provider handledProvider = resultData.getParcelable(Constants.PROVIDER_KEY);

            if (handledProvider != null && setupInterface.getProvider() != null &&
                    handledProvider.getDomain().equalsIgnoreCase(setupInterface.getProvider().getDomain())) {
                switch (resultCode) {
                    case ProviderAPI.PROVIDER_OK:
                        setupInterface.handleProviderSetUp(handledProvider);
                        break;
                    case ProviderAPI.PROVIDER_NOK:
                        setupInterface.handleProviderSetupFailed(resultData);
                        break;
                    case ProviderAPI.CORRECTLY_DOWNLOADED_VPN_CERTIFICATE:
                        setupInterface.handleCorrectlyDownloadedCertificate(handledProvider);
                        break;
                    case ProviderAPI.INCORRECTLY_DOWNLOADED_VPN_CERTIFICATE:
                        setupInterface.handleIncorrectlyDownloadedCertificate();
                        break;
                }
            }
        }
    }

}