package se.leap.bitmaskclient.providersetup;

import static se.leap.bitmaskclient.R.string.malformed_url;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.DELAY;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.ERRORS;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.MISSING_NETWORK_CONNECTION;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.PARAMETERS;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.PROVIDER_NOK;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.RECEIVER_KEY;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.TOR_EXCEPTION;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.TOR_TIMEOUT;
import static se.leap.bitmaskclient.providersetup.ProviderApiManagerV5.PROXY_HOST;
import static se.leap.bitmaskclient.providersetup.ProviderApiManagerV5.SOCKS_PROXY_SCHEME;
import static se.leap.bitmaskclient.providersetup.ProviderSetupFailedDialog.DOWNLOAD_ERRORS.ERROR_TOR_TIMEOUT;
import static se.leap.bitmaskclient.providersetup.ProviderSetupObservable.DOWNLOADED_PROVIDER_JSON;
import static se.leap.bitmaskclient.tor.TorStatusObservable.TorStatus.OFF;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import androidx.core.content.IntentCompat;

import org.jetbrains.annotations.Blocking;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeoutException;

import de.blinkt.openvpn.core.VpnStatus;
import mobile.BitmaskMobile;
import se.leap.bitmaskclient.BuildConfig;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.utils.ConfigHelper;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.tor.TorStatusObservable;

public class ProviderApiManager extends ProviderApiManagerBase {
    public static final String TAG = ProviderApiManager.class.getSimpleName();
    public final ProviderApiManagerFactory versionedApiFactory;

    public ProviderApiManager(Resources resources, ProviderApiManagerBase.ProviderApiServiceCallback callback) {
        super(resources, callback);
        this.versionedApiFactory = new ProviderApiManagerFactory(resources, callback);
    }

    @Blocking
    public void handleIntent(Intent command) {
        ResultReceiver receiver = null;
        if (command.getParcelableExtra(RECEIVER_KEY) != null) {
            receiver = command.getParcelableExtra(RECEIVER_KEY);
        }
        String action = command.getAction();
        Bundle parameters = command.getBundleExtra(PARAMETERS);

        if (action == null) {
            Log.e(TAG, "Intent without action sent!");
            return;
        }

        Provider provider = null;
        if (command.hasExtra(PROVIDER_KEY)) {
            provider = IntentCompat.getParcelableExtra(command, PROVIDER_KEY, Provider.class);
        } else {
            //TODO: consider returning error back e.g. NO_PROVIDER
            Log.e(TAG, action + " called without provider!");
            return;
        }

        if (parameters.containsKey(DELAY)) {
            try {
                Thread.sleep(parameters.getLong(DELAY));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (!serviceCallback.hasNetworkConnection()) {
            Bundle result = new Bundle();
            eventSender.setErrorResult(result, R.string.error_network_connection, null);
            eventSender.sendToReceiverOrBroadcast(receiver, MISSING_NETWORK_CONNECTION, result, provider);
            return;
        }
        Bundle result = new Bundle();

        try {
            if (PreferenceHelper.hasSnowflakePrefs() && !VpnStatus.isVPNActive()) {
                torHandler.startTorProxy();
            }
        } catch (InterruptedException | IllegalStateException e) {
            e.printStackTrace();
            eventSender.setErrorResultAction(result, action);
            eventSender.sendToReceiverOrBroadcast(receiver, TOR_EXCEPTION, result, provider);
            return;
        } catch (TimeoutException e) {
            torHandler.stopTorProxy();
            eventSender.setErrorResult(result, R.string.error_tor_timeout, ERROR_TOR_TIMEOUT.toString(), action);
            eventSender.sendToReceiverOrBroadcast(receiver, TOR_TIMEOUT, result, provider);
            return;
        }

        if (!provider.hasDefinition()) {
            result = downloadProviderDefinition(result, provider);
            if (result.containsKey(ERRORS)) {
                eventSender.sendToReceiverOrBroadcast(receiver, PROVIDER_NOK, result, provider);
                return;
            }
        }

        IProviderApiManager apiManager = versionedApiFactory.getProviderApiManager(provider);
        apiManager.handleAction(action, provider, parameters, receiver);
    }

    private Bundle downloadProviderDefinition(Bundle result, Provider provider) {
        getPersistedProviderUpdates(provider);
        if (provider.hasDefinition()) {
            return result;
        }

        try {
            String providerString = fetch(provider, true);
            if (ConfigHelper.checkErroneousDownload(providerString) || !isValidJson(providerString)) {
                return eventSender.setErrorResult(result, malformed_url, null);
            }

            JSONObject jsonObject = new JSONObject(providerString);
            provider.define(jsonObject);
            provider.setModelsProvider(providerString);
            ProviderSetupObservable.updateProgress(DOWNLOADED_PROVIDER_JSON);
        } catch (Exception e) {
            return eventSender.setErrorResult(result, R.string.malformed_url, null);
        }

        return result;
    }

    private String fetch(Provider provider, Boolean allowRetry) {
        BitmaskMobile bm;
        try {
            bm = new BitmaskMobile(provider.getMainUrl(), new PreferenceHelper.SharedPreferenceStore());
            bm.setDebug(BuildConfig.DEBUG);
            if (TorStatusObservable.isRunning() && TorStatusObservable.getSocksProxyPort() != -1) {
                bm.setSocksProxy(SOCKS_PROXY_SCHEME + PROXY_HOST + ":" + TorStatusObservable.getSocksProxyPort());
            } else if (provider.hasIntroducer()) {
                bm.setIntroducer(provider.getIntroducer().toUrl());
            }
            return bm.getProvider();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (allowRetry &&
                        TorStatusObservable.getStatus() == OFF &&
                        torHandler.startTorProxy()
                ) {
                    return fetch(provider, false);
                }
            } catch (InterruptedException | TimeoutException ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }
}
