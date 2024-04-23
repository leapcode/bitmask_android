package se.leap.bitmaskclient.providersetup;

import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_KEY;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.CORRECTLY_DOWNLOADED_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.DOWNLOAD_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INCORRECTLY_DOWNLOADED_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.PROVIDER_NOK;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.PROVIDER_OK;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.QUIETLY_UPDATE_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.SET_UP_PROVIDER;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.UPDATE_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderSetupObservable.DOWNLOADED_VPN_CERTIFICATE;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.models.ProviderObservable;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;

public class ProviderApiManagerV5 extends ProviderApiManagerBase implements IProviderApiManager {

    private static final String TAG = ProviderApiManagerV5.class.getSimpleName();

    ProviderApiManagerV5(Resources resources, ProviderApiServiceCallback callback) {
        super(resources, callback);
    }

    @Override
    public void handleAction(String action, Provider provider, Bundle parameters, ResultReceiver receiver) {
        Bundle result = new Bundle();
        switch (action) {
            case SET_UP_PROVIDER:
                result = setupProvider(provider, parameters);
                if (result.getBoolean(BROADCAST_RESULT_KEY)) {
                    eventSender.sendToReceiverOrBroadcast(receiver, PROVIDER_OK, result, provider);
                } else {
                    eventSender.sendToReceiverOrBroadcast(receiver, PROVIDER_NOK, result, provider);
                }
                break;
            case DOWNLOAD_VPN_CERTIFICATE:
                result = updateVpnCertificate(provider);
                if (result.getBoolean(BROADCAST_RESULT_KEY)) {
                    serviceCallback.saveProvider(provider);
                    ProviderSetupObservable.updateProgress(DOWNLOADED_VPN_CERTIFICATE);
                    eventSender.sendToReceiverOrBroadcast(receiver, CORRECTLY_DOWNLOADED_VPN_CERTIFICATE, result, provider);
                } else {
                    eventSender.sendToReceiverOrBroadcast(receiver, INCORRECTLY_DOWNLOADED_VPN_CERTIFICATE, result, provider);
                }
                break;
            case QUIETLY_UPDATE_VPN_CERTIFICATE:
            case UPDATE_INVALID_VPN_CERTIFICATE:
                result = updateVpnCertificate(provider);
                if (result.getBoolean(BROADCAST_RESULT_KEY)) {
                    Log.d(TAG, "successfully downloaded VPN certificate");
                    provider.setShouldUpdateVpnCertificate(false);
                    PreferenceHelper.storeProviderInPreferences(provider);
                    ProviderObservable.getInstance().updateProvider(provider);
                }
                break;
        }

    }

    protected Bundle setupProvider(Provider provider, Bundle task) {
        return null;
    }


    protected Bundle updateVpnCertificate(Provider provider) {
        return null;
    }

}
