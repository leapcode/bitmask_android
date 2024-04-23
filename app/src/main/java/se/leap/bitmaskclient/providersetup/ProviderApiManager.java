package se.leap.bitmaskclient.providersetup;

import static se.leap.bitmaskclient.BuildConfig.DEBUG_MODE;
import static se.leap.bitmaskclient.R.string.certificate_error;
import static se.leap.bitmaskclient.R.string.error_io_exception_user_message;
import static se.leap.bitmaskclient.R.string.error_json_exception_user_message;
import static se.leap.bitmaskclient.R.string.error_no_such_algorithm_exception_user_message;
import static se.leap.bitmaskclient.R.string.malformed_url;
import static se.leap.bitmaskclient.R.string.server_unreachable_message;
import static se.leap.bitmaskclient.R.string.service_is_down_error;
import static se.leap.bitmaskclient.R.string.warning_corrupted_provider_details;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_KEY;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.DELAY;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.ERRORS;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.MISSING_NETWORK_CONNECTION;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.PARAMETERS;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.RECEIVER_KEY;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.TOR_EXCEPTION;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.TOR_TIMEOUT;
import static se.leap.bitmaskclient.providersetup.ProviderSetupFailedDialog.DOWNLOAD_ERRORS.ERROR_CORRUPTED_PROVIDER_JSON;
import static se.leap.bitmaskclient.providersetup.ProviderSetupFailedDialog.DOWNLOAD_ERRORS.ERROR_TOR_TIMEOUT;
import static se.leap.bitmaskclient.tor.TorStatusObservable.TorStatus.OFF;
import static se.leap.bitmaskclient.tor.TorStatusObservable.getProxyPort;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;
import android.util.Pair;

import org.jetbrains.annotations.Blocking;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.UnknownServiceException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;

import de.blinkt.openvpn.core.VpnStatus;
import okhttp3.OkHttpClient;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.utils.ConfigHelper;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.providersetup.connectivity.OkHttpClientGenerator;
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
        if (command.getParcelableExtra(PROVIDER_KEY) != null) {
            provider = command.getParcelableExtra(PROVIDER_KEY);
        } else {
            //TODO: consider returning error back e.g. NO_PROVIDER
            Log.e(TAG, action +" called without provider!");
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
            downloadProviderDefinition(provider);
        }

        IProviderApiManager apiManager = versionedApiFactory.getProviderApiManager(provider);
        apiManager.handleAction(action, provider, parameters, receiver);
    }

    private void downloadProviderDefinition(Provider provider) {
        getPersistedProviderUpdates(provider);
        if (provider.hasDefinition()) {
            return;
        }
        getAndSetProviderJson(provider);
    }

    private Bundle getAndSetProviderJson(Provider provider) {
        Bundle result = new Bundle();

        String providerJsonUrl = provider.getMainUrlString() + "/provider.json";
        String providerDotJsonString = fetch(providerJsonUrl, true);

        if (ConfigHelper.checkErroneousDownload(providerDotJsonString) || !isValidJson(providerDotJsonString)) {
            eventSender.setErrorResult(result, malformed_url, null);
            return result;
        }

        if (DEBUG_MODE) {
            VpnStatus.logDebug("[API] PROVIDER JSON: " + providerDotJsonString);
        }
        try {
            JSONObject providerJson = new JSONObject(providerDotJsonString);

            if (provider.define(providerJson)) {
                result.putBoolean(BROADCAST_RESULT_KEY, true);
            } else {
                return eventSender.setErrorResult(result, warning_corrupted_provider_details, ERROR_CORRUPTED_PROVIDER_JSON.toString());
            }

        } catch (JSONException e) {
            eventSender.setErrorResult(result, providerDotJsonString);
        }
        return result;
    }



    /**
     * Tries to download the contents of the provided url using commercially validated CA certificate from chosen provider.
     *
     */
    private String fetch(String url, boolean allowRetry) {

        JSONObject errorJson = new JSONObject();
        OkHttpClientGenerator clientGenerator = new OkHttpClientGenerator(resources);

        OkHttpClient okHttpClient = clientGenerator.initCommercialCAHttpClient(errorJson, getProxyPort());
        List<Pair<String, String>> headerArgs = new ArrayList<>();
        if (okHttpClient == null) {
            return errorJson.toString();
        }

        String plainResponseBody;

        try {

            plainResponseBody = ProviderApiConnector.requestStringFromServer(url, "GET", null, headerArgs, okHttpClient);

        } catch (NullPointerException npe) {
            plainResponseBody = eventSender.formatErrorMessage(error_json_exception_user_message);
            VpnStatus.logWarning("[API] Null response body for request " + url + ": " + npe.getLocalizedMessage());
        } catch (UnknownHostException | SocketTimeoutException e) {
            plainResponseBody = eventSender.formatErrorMessage(server_unreachable_message);
            VpnStatus.logWarning("[API] UnknownHostException or SocketTimeoutException for request " + url + ": " + e.getLocalizedMessage());
        } catch (MalformedURLException e) {
            plainResponseBody = eventSender.formatErrorMessage(malformed_url);
            VpnStatus.logWarning("[API] MalformedURLException for request " + url + ": " + e.getLocalizedMessage());
        } catch (SSLHandshakeException | SSLPeerUnverifiedException e) {
            plainResponseBody = eventSender.formatErrorMessage(certificate_error);
            VpnStatus.logWarning("[API] SSLHandshakeException or SSLPeerUnverifiedException for request " + url + ": " + e.getLocalizedMessage());
        } catch (ConnectException e) {
            plainResponseBody = eventSender.formatErrorMessage(service_is_down_error);
            VpnStatus.logWarning("[API] ConnectException for request " + url + ": " + e.getLocalizedMessage());
        } catch (IllegalArgumentException e) {
            plainResponseBody = eventSender.formatErrorMessage(error_no_such_algorithm_exception_user_message);
            VpnStatus.logWarning("[API] IllegalArgumentException for request " + url + ": " + e.getLocalizedMessage());
        } catch (UnknownServiceException e) {
            //unable to find acceptable protocols - tlsv1.2 not enabled?
            plainResponseBody = eventSender.formatErrorMessage(error_no_such_algorithm_exception_user_message);
            VpnStatus.logWarning("[API] UnknownServiceException for request " + url + ": " + e.getLocalizedMessage());
        } catch (IOException e) {
            plainResponseBody = eventSender.formatErrorMessage(error_io_exception_user_message);
            VpnStatus.logWarning("[API] IOException for request " + url + ": " + e.getLocalizedMessage());
        }

        try {
            if (allowRetry &&
                    plainResponseBody != null &&
                    plainResponseBody.contains(ERRORS)  &&
                    TorStatusObservable.getStatus() == OFF &&
                    torHandler.startTorProxy()
            ) {
                return fetch(url, false);
            }
        } catch (InterruptedException | IllegalStateException | TimeoutException e) {
            e.printStackTrace();
        }
        return plainResponseBody;
    }
}
