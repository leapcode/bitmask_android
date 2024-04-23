/**
 * Copyright (c) 2018 LEAP Encryption Access Project and contributers
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

package se.leap.bitmaskclient.providersetup;

import static android.text.TextUtils.isEmpty;
import static se.leap.bitmaskclient.BuildConfig.DEBUG_MODE;
import static se.leap.bitmaskclient.R.string.certificate_error;
import static se.leap.bitmaskclient.R.string.downloading_vpn_certificate_failed;
import static se.leap.bitmaskclient.R.string.error_io_exception_user_message;
import static se.leap.bitmaskclient.R.string.error_json_exception_user_message;
import static se.leap.bitmaskclient.R.string.error_no_such_algorithm_exception_user_message;
import static se.leap.bitmaskclient.R.string.malformed_url;
import static se.leap.bitmaskclient.R.string.server_unreachable_message;
import static se.leap.bitmaskclient.R.string.service_is_down_error;
import static se.leap.bitmaskclient.R.string.setup_error_text;
import static se.leap.bitmaskclient.R.string.setup_error_text_custom;
import static se.leap.bitmaskclient.R.string.warning_corrupted_provider_cert;
import static se.leap.bitmaskclient.R.string.warning_corrupted_provider_details;
import static se.leap.bitmaskclient.R.string.warning_expired_provider_cert;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_KEY;
import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_START;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.base.utils.BuildConfigHelper.isDefaultBitmask;
import static se.leap.bitmaskclient.base.utils.CertificateHelper.getFingerprintFromCertificate;
import static se.leap.bitmaskclient.base.utils.ConfigHelper.getProviderFormattedString;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.CORRECTLY_DOWNLOADED_EIP_SERVICE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.CORRECTLY_DOWNLOADED_GEOIP_JSON;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.CORRECTLY_DOWNLOADED_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.CORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.DOWNLOAD_GEOIP_JSON;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.DOWNLOAD_MOTD;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.DOWNLOAD_SERVICE_JSON;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.DOWNLOAD_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.ERRORS;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INCORRECTLY_DOWNLOADED_EIP_SERVICE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INCORRECTLY_DOWNLOADED_GEOIP_JSON;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INCORRECTLY_DOWNLOADED_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INCORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.PROVIDER_NOK;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.PROVIDER_OK;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.QUIETLY_UPDATE_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.SET_UP_PROVIDER;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.UPDATE_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderSetupFailedDialog.DOWNLOAD_ERRORS.ERROR_CERTIFICATE_PINNING;
import static se.leap.bitmaskclient.providersetup.ProviderSetupFailedDialog.DOWNLOAD_ERRORS.ERROR_CORRUPTED_PROVIDER_JSON;
import static se.leap.bitmaskclient.providersetup.ProviderSetupFailedDialog.DOWNLOAD_ERRORS.ERROR_INVALID_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderSetupObservable.DOWNLOADED_CA_CERT;
import static se.leap.bitmaskclient.providersetup.ProviderSetupObservable.DOWNLOADED_EIP_SERVICE_JSON;
import static se.leap.bitmaskclient.providersetup.ProviderSetupObservable.DOWNLOADED_GEOIP_JSON;
import static se.leap.bitmaskclient.providersetup.ProviderSetupObservable.DOWNLOADED_PROVIDER_JSON;
import static se.leap.bitmaskclient.providersetup.ProviderSetupObservable.DOWNLOADED_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.tor.TorStatusObservable.TorStatus.OFF;
import static se.leap.bitmaskclient.tor.TorStatusObservable.getProxyPort;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.net.UnknownServiceException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;

import de.blinkt.openvpn.core.VpnStatus;
import okhttp3.OkHttpClient;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.models.ProviderObservable;
import se.leap.bitmaskclient.base.utils.ConfigHelper;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.eip.EIP;
import se.leap.bitmaskclient.motd.MotdClient;
import se.leap.bitmaskclient.providersetup.connectivity.OkHttpClientGenerator;
import se.leap.bitmaskclient.providersetup.models.LeapSRPSession;
import se.leap.bitmaskclient.tor.TorStatusObservable;

/**
 * Implements the logic of the provider api http requests. The methods of this class need to be called from
 * a background thread.
 */


public class ProviderApiManagerV3 extends ProviderApiManagerBase implements IProviderApiManager {

    private static final String TAG = ProviderApiManagerV3.class.getSimpleName();

    OkHttpClientGenerator clientGenerator;

    public ProviderApiManagerV3(Resources resources, OkHttpClientGenerator clientGenerator, ProviderApiServiceCallback callback) {
        super(resources, callback);
        this.clientGenerator = clientGenerator;
    }

    @Override
    public void handleAction(String action, Provider provider, Bundle parameters, ResultReceiver receiver) {
        Bundle result = new Bundle();
        switch (action) {
            case SET_UP_PROVIDER:
                ProviderObservable.getInstance().setProviderForDns(provider);
                result = setupProvider(provider, parameters);
                if (result.getBoolean(BROADCAST_RESULT_KEY)) {
                    getGeoIPJson(provider);
                    if (provider.hasGeoIpJson()) {
                        ProviderSetupObservable.updateProgress(DOWNLOADED_GEOIP_JSON);
                    }
                    eventSender.sendToReceiverOrBroadcast(receiver, PROVIDER_OK, result, provider);
                } else {
                    eventSender.sendToReceiverOrBroadcast(receiver, PROVIDER_NOK, result, provider);
                }
                ProviderObservable.getInstance().setProviderForDns(null);
                break;
            case DOWNLOAD_VPN_CERTIFICATE:
                ProviderObservable.getInstance().setProviderForDns(provider);
                result = updateVpnCertificate(provider);
                if (result.getBoolean(BROADCAST_RESULT_KEY)) {
                    serviceCallback.saveProvider(provider);
                    ProviderSetupObservable.updateProgress(DOWNLOADED_VPN_CERTIFICATE);
                    eventSender.sendToReceiverOrBroadcast(receiver, CORRECTLY_DOWNLOADED_VPN_CERTIFICATE, result, provider);
                } else {
                    eventSender.sendToReceiverOrBroadcast(receiver, INCORRECTLY_DOWNLOADED_VPN_CERTIFICATE, result, provider);
                }
                ProviderObservable.getInstance().setProviderForDns(null);
                break;
            case QUIETLY_UPDATE_VPN_CERTIFICATE:
                ProviderObservable.getInstance().setProviderForDns(provider);
                result = updateVpnCertificate(provider);
                if (result.getBoolean(BROADCAST_RESULT_KEY)) {
                    Log.d(TAG, "successfully downloaded VPN certificate");
                    provider.setShouldUpdateVpnCertificate(false);
                    PreferenceHelper.storeProviderInPreferences(provider);
                    ProviderObservable.getInstance().updateProvider(provider);
                }
                ProviderObservable.getInstance().setProviderForDns(null);
                break;
            case DOWNLOAD_MOTD:
                MotdClient client = new MotdClient(provider);
                JSONObject motd = client.fetchJson();
                if (motd != null) {
                    provider.setMotdJson(motd);
                    provider.setLastMotdUpdate(System.currentTimeMillis());
                }
                PreferenceHelper.storeProviderInPreferences(provider);
                ProviderObservable.getInstance().updateProvider(provider);
                break;

            case UPDATE_INVALID_VPN_CERTIFICATE:
                ProviderObservable.getInstance().setProviderForDns(provider);
                result = updateVpnCertificate(provider);
                if (result.getBoolean(BROADCAST_RESULT_KEY)) {
                    eventSender.sendToReceiverOrBroadcast(receiver, CORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE, result, provider);
                } else {
                    eventSender.sendToReceiverOrBroadcast(receiver, INCORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE, result, provider);
                }
                ProviderObservable.getInstance().setProviderForDns(null);
                break;
            case DOWNLOAD_SERVICE_JSON:
                ProviderObservable.getInstance().setProviderForDns(provider);
                Log.d(TAG, "update eip service json");
                result = getAndSetEipServiceJson(provider);
                if (result.getBoolean(BROADCAST_RESULT_KEY)) {
                    eventSender.sendToReceiverOrBroadcast(receiver, CORRECTLY_DOWNLOADED_EIP_SERVICE, result, provider);
                } else {
                    eventSender.sendToReceiverOrBroadcast(receiver, INCORRECTLY_DOWNLOADED_EIP_SERVICE, result, provider);
                }
                ProviderObservable.getInstance().setProviderForDns(null);
                break;
            case DOWNLOAD_GEOIP_JSON:
                if (!provider.getGeoipUrl().isDefault()) {
                    boolean startEIP = parameters.getBoolean(EIP_ACTION_START);
                    ProviderObservable.getInstance().setProviderForDns(provider);
                    result = getGeoIPJson(provider);
                    result.putBoolean(EIP_ACTION_START, startEIP);
                    if (result.getBoolean(BROADCAST_RESULT_KEY)) {
                        eventSender.sendToReceiverOrBroadcast(receiver, CORRECTLY_DOWNLOADED_GEOIP_JSON, result, provider);
                    } else {
                        eventSender.sendToReceiverOrBroadcast(receiver, INCORRECTLY_DOWNLOADED_GEOIP_JSON, result, provider);
                    }
                    ProviderObservable.getInstance().setProviderForDns(null);
                }
                break;
        }
    }

    /**
     * Downloads a provider.json from a given URL, adding a new provider using the given name.
     *
     * @param task containing a boolean meaning if the provider is custom or not, another boolean meaning if the user completely trusts this provider, the provider name and its provider.json url.
     * @return a bundle with a boolean value mapped to a key named BROADCAST_RESULT_KEY, and which is true if the update was successful.
     */
    public Bundle setupProvider(Provider provider, Bundle task) {
        Bundle currentDownload = new Bundle();

        if (isEmpty(provider.getMainUrlString()) || provider.getMainUrl().isDefault()) {
            currentDownload.putBoolean(BROADCAST_RESULT_KEY, false);
            eventSender.setErrorResult(currentDownload, malformed_url, null);
            VpnStatus.logWarning("[API] MainURL String is not set. Cannot setup provider.");
            return currentDownload;
        }

        getPersistedProviderUpdates(provider);
        currentDownload = validateProviderDetails(provider);

        //provider certificate invalid
        if (currentDownload.containsKey(ERRORS)) {
            currentDownload.putParcelable(PROVIDER_KEY, provider);
            return currentDownload;
        }

        //no provider json or certificate available
        if (currentDownload.containsKey(BROADCAST_RESULT_KEY) && !currentDownload.getBoolean(BROADCAST_RESULT_KEY)) {
            resetProviderDetails(provider);
        }

        currentDownload = getAndSetProviderJson(provider);
        if (provider.hasDefinition() || (currentDownload.containsKey(BROADCAST_RESULT_KEY) && currentDownload.getBoolean(BROADCAST_RESULT_KEY))) {
            ProviderSetupObservable.updateProgress(DOWNLOADED_PROVIDER_JSON);
            if (!provider.hasCaCert()) {
                currentDownload = downloadCACert(provider);
            }
            if (provider.hasCaCert() || (currentDownload.containsKey(BROADCAST_RESULT_KEY) && currentDownload.getBoolean(BROADCAST_RESULT_KEY))) {
                ProviderSetupObservable.updateProgress(DOWNLOADED_CA_CERT);
                currentDownload = getAndSetEipServiceJson(provider);
            }

            if (provider.hasEIP() && !provider.allowsRegistered() && !provider.allowsAnonymous()) {
                eventSender.setErrorResult(currentDownload, isDefaultBitmask() ? setup_error_text : setup_error_text_custom, null);
            } else if (provider.hasEIP()) {
                ProviderSetupObservable.updateProgress(DOWNLOADED_EIP_SERVICE_JSON);
            }
        }

        return currentDownload;
    }

    private Bundle getAndSetProviderJson(Provider provider) {
        Bundle result = new Bundle();

        String providerDotJsonString;
        if(provider.getDefinitionString().length() == 0 || provider.getCaCert().isEmpty()) {
            String providerJsonUrl = provider.getMainUrlString() + "/provider.json";
            providerDotJsonString = downloadWithCommercialCA(providerJsonUrl, provider);
        } else {
            providerDotJsonString = downloadFromApiUrlWithProviderCA("/provider.json", provider);
        }

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
     * Downloads the eip-service.json from a given URL, and saves eip service capabilities including the offered gateways
     * @return a bundle with a boolean value mapped to a key named BROADCAST_RESULT_KEY, and which is true if the download was successful.
     */
    private Bundle getAndSetEipServiceJson(Provider provider) {
        Bundle result = new Bundle();
        String eipServiceJsonString = "";
        try {
            String eipServiceUrl = provider.getApiUrlWithVersion() + "/" + EIP.SERVICE_API_PATH;
            eipServiceJsonString = downloadWithProviderCA(provider.getCaCert(), eipServiceUrl);
            if (DEBUG_MODE) {
                VpnStatus.logDebug("[API] EIP SERVICE JSON: " + eipServiceJsonString);
            }
            JSONObject eipServiceJson = new JSONObject(eipServiceJsonString);
            if (eipServiceJson.has(ERRORS)) {
                eventSender.setErrorResult(result, eipServiceJsonString);
            } else {
                provider.setEipServiceJson(eipServiceJson);
                provider.setLastEipServiceUpdate(System.currentTimeMillis());
                result.putBoolean(BROADCAST_RESULT_KEY, true);
            }
        } catch (NullPointerException | JSONException e) {
            eventSender.setErrorResult(result, R.string.error_json_exception_user_message, null);
        }
        return result;
    }

    /**
     * Downloads a new OpenVPN certificate, attaching authenticated cookie for authenticated certificate.
     *
     * @return true if certificate was downloaded correctly, false if provider.json is not present in SharedPreferences, or if the certificate url could not be parsed as a URI, or if there was an SSL error.
     */
    protected Bundle updateVpnCertificate(Provider provider) {
        Bundle result = new Bundle();
        String certString = downloadFromVersionedApiUrlWithProviderCA("/" + PROVIDER_VPN_CERTIFICATE, provider);
        if (DEBUG_MODE) {
            VpnStatus.logDebug("[API] VPN CERT: " + certString);
        }
        if (ConfigHelper.checkErroneousDownload(certString)) {
            if (TorStatusObservable.isRunning()) {
                eventSender.setErrorResult(result, downloading_vpn_certificate_failed, null);
            } else if (certString == null || certString.isEmpty() ){
                // probably 204
                eventSender.setErrorResult(result, error_io_exception_user_message, null);
            } else {
                eventSender.setErrorResult(result, certString);
            }
            return result;
        }
        return loadCertificate(provider, certString);
    }

    /**
     * Fetches the geo ip Json, containing a list of gateways sorted by distance from the users current location.
     * Fetching is only allowed if the cache timeout of 1 h was reached, a valid geoip service URL exists and the
     * vpn or tor is not running. The latter condition is needed in order to guarantee that the geoip service sees
     * the real ip of the client
     *
     * @param provider
     * @return
     */
    private Bundle getGeoIPJson(Provider provider) {
        Bundle result = new Bundle();

        if (!provider.shouldUpdateGeoIpJson() || provider.getGeoipUrl().isDefault() || VpnStatus.isVPNActive() || TorStatusObservable.getStatus() != OFF) {
            result.putBoolean(BROADCAST_RESULT_KEY, false);
            return result;
        }

        try {
            URL geoIpUrl = provider.getGeoipUrl().getUrl();

            String geoipJsonString = downloadFromUrlWithProviderCA(geoIpUrl.toString(), provider, false);
            if (DEBUG_MODE) {
                VpnStatus.logDebug("[API] MENSHEN JSON: " + geoipJsonString);
            }
            JSONObject geoipJson = new JSONObject(geoipJsonString);

            if (geoipJson.has(ERRORS)) {
                result.putBoolean(BROADCAST_RESULT_KEY, false);
            } else{
                provider.setGeoIpJson(geoipJson);
                provider.setLastGeoIpUpdate(System.currentTimeMillis());
                result.putBoolean(BROADCAST_RESULT_KEY, true);
            }

        } catch (JSONException | NullPointerException e) {
            result.putBoolean(BROADCAST_RESULT_KEY, false);
            e.printStackTrace();
        }
        return result;
    }


    private Bundle downloadCACert(Provider provider) {
        Bundle result = new Bundle();
        try {
            String caCertUrl = provider.getDefinition().getString(Provider.CA_CERT_URI);
            String providerDomain = provider.getDomain();
            String certString = downloadWithCommercialCA(caCertUrl, provider);

            if (validCertificate(provider, certString)) {
                provider.setCaCert(certString);
                if (DEBUG_MODE) {
                    VpnStatus.logDebug("[API] CA CERT: " + certString);
                }
                PreferenceHelper.putProviderString(providerDomain, Provider.CA_CERT, certString);
                result.putBoolean(BROADCAST_RESULT_KEY, true);
            } else {
                eventSender.setErrorResult(result, warning_corrupted_provider_cert, ERROR_CERTIFICATE_PINNING.toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result;
    }

    private String downloadWithCommercialCA(String stringUrl, Provider provider) {
        return downloadWithCommercialCA(stringUrl, provider, true);
    }

        /**
         * Tries to download the contents of the provided url using commercially validated CA certificate from chosen provider.
         *
         */
    private String downloadWithCommercialCA(String stringUrl, Provider provider, boolean allowRetry) {

        String responseString;
        JSONObject errorJson = new JSONObject();

        OkHttpClient okHttpClient = clientGenerator.initCommercialCAHttpClient(errorJson, getProxyPort());
        if (okHttpClient == null) {
            return errorJson.toString();
        }

        List<Pair<String, String>> headerArgs = getAuthorizationHeader();

        responseString = sendGetStringToServer(stringUrl, headerArgs, okHttpClient);

        if (responseString != null && responseString.contains(ERRORS)) {
            try {
                // try to download with provider CA on certificate error
                JSONObject responseErrorJson = new JSONObject(responseString);
                if (responseErrorJson.getString(ERRORS).equals(getProviderFormattedString(resources, R.string.certificate_error))) {
                    responseString = downloadWithProviderCA(provider.getCaCert(), stringUrl);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        try {
            if (allowRetry &&
                    responseString != null &&
                    responseString.contains(ERRORS)  &&
                    TorStatusObservable.getStatus() == OFF &&
                    torHandler.startTorProxy()
            ) {
                return downloadWithCommercialCA(stringUrl, provider, false);
            }
        } catch (InterruptedException | IllegalStateException | TimeoutException e) {
            e.printStackTrace();
        }
        return responseString;
    }


    /**
     * Tries to download the contents of the provided url using not commercially validated CA certificate from chosen provider.
     *
     * @return an empty string if it fails, the response body if not.
     */
    private String downloadFromApiUrlWithProviderCA(String path, Provider provider) {
        String baseUrl = provider.getApiUrlString();
        String urlString = baseUrl + path;
        return downloadFromUrlWithProviderCA(urlString, provider);
    }

    /**
     * Tries to download the contents of $base_url/$version/$path using not commercially validated CA certificate from chosen provider.
     *
     * @return an empty string if it fails, the response body if not.
     */
    private String downloadFromVersionedApiUrlWithProviderCA(String path, Provider provider) {
        String baseUrl = provider.getApiUrlWithVersion();
        String urlString = baseUrl + path;
        return downloadFromUrlWithProviderCA(urlString, provider);
    }

    private String downloadFromUrlWithProviderCA(String urlString, Provider provider) {
        return downloadFromUrlWithProviderCA(urlString, provider, true);
    }

    private String downloadFromUrlWithProviderCA(String urlString, Provider provider, boolean allowRetry) {
        String responseString;
        JSONObject errorJson = new JSONObject();
        OkHttpClient okHttpClient = clientGenerator.initSelfSignedCAHttpClient(provider.getCaCert(), getProxyPort(), errorJson);
        if (okHttpClient == null) {
            return errorJson.toString();
        }

        List<Pair<String, String>> headerArgs = getAuthorizationHeader();
        responseString = sendGetStringToServer(urlString, headerArgs, okHttpClient);

        try {
            if (allowRetry &&
                    responseString != null &&
                    responseString.contains(ERRORS)  &&
                    TorStatusObservable.getStatus() == OFF &&
                    torHandler.startTorProxy()
            ) {
                return downloadFromUrlWithProviderCA(urlString, provider, false);
            }
        } catch (InterruptedException | IllegalStateException | TimeoutException e) {
            e.printStackTrace();
        }

        return responseString;
    }


        /**
         * Tries to download the contents of the provided url using not commercially validated CA certificate from chosen provider.
         *
         * @param urlString as a string
         * @return an empty string if it fails, the url content if not.
         */
    private String downloadWithProviderCA(String caCert, String urlString) {
        JSONObject initError = new JSONObject();
        String responseString;

        OkHttpClient okHttpClient = clientGenerator.initSelfSignedCAHttpClient(caCert, getProxyPort(), initError);
        if (okHttpClient == null) {
            return initError.toString();
        }

        List<Pair<String, String>> headerArgs = getAuthorizationHeader();

        responseString = sendGetStringToServer(urlString, headerArgs, okHttpClient);

        return responseString;
    }

    protected String sendGetStringToServer(@NonNull String url, @NonNull List<Pair<String, String>> headerArgs, @NonNull OkHttpClient okHttpClient) {
        return requestStringFromServer(url, "GET", null, headerArgs, okHttpClient);
    }

    private String requestStringFromServer(@NonNull String url, @NonNull String requestMethod, String jsonString, @NonNull List<Pair<String, String>> headerArgs, @NonNull OkHttpClient okHttpClient) {
        String plainResponseBody;

        try {

            plainResponseBody = ProviderApiConnector.requestStringFromServer(url, requestMethod, jsonString, headerArgs, okHttpClient);

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

        return plainResponseBody;
    }

    private boolean canConnect(Provider provider, Bundle result) {
        return canConnect(provider, result, 0);
    }

    private boolean canConnect(Provider provider, Bundle result, int tries) {
        JSONObject errorJson = new JSONObject();
        String providerUrl = provider.getApiUrlString() + "/provider.json";

        OkHttpClient okHttpClient = clientGenerator.initSelfSignedCAHttpClient(provider.getCaCert(), getProxyPort(), errorJson);
        if (okHttpClient == null) {
            result.putString(ERRORS, errorJson.toString());
            return false;
        }

        if (tries > 0) {
            result.remove(ERRORS);
        }

        try {
            return ProviderApiConnector.canConnect(okHttpClient, providerUrl);

        }  catch (UnknownHostException | SocketTimeoutException e) {
            VpnStatus.logWarning("[API] UnknownHostException or SocketTimeoutException during connection check: " + e.getLocalizedMessage());
            eventSender.setErrorResult(result, server_unreachable_message, null);
        } catch (MalformedURLException e) {
            VpnStatus.logWarning("[API] MalformedURLException during connection check: " + e.getLocalizedMessage());
            eventSender.setErrorResult(result, malformed_url, null);
        } catch (SSLHandshakeException e) {
            VpnStatus.logWarning("[API] SSLHandshakeException during connection check: " + e.getLocalizedMessage());
            eventSender.setErrorResult(result, warning_corrupted_provider_cert, ERROR_INVALID_CERTIFICATE.toString());
        } catch (ConnectException e) {
            VpnStatus.logWarning("[API] ConnectException during connection check: " + e.getLocalizedMessage());
            eventSender.setErrorResult(result, service_is_down_error, null);
        } catch (IllegalArgumentException e) {
            VpnStatus.logWarning("[API] IllegalArgumentException during connection check: " + e.getLocalizedMessage());
            eventSender.setErrorResult(result, error_no_such_algorithm_exception_user_message, null);
        } catch (UnknownServiceException e) {
            VpnStatus.logWarning("[API] UnknownServiceException during connection check: " + e.getLocalizedMessage());
            //unable to find acceptable protocols - tlsv1.2 not enabled?
            eventSender.setErrorResult(result, error_no_such_algorithm_exception_user_message, null);
        } catch (IOException e) {
            VpnStatus.logWarning("[API] IOException during connection check: " + e.getLocalizedMessage());
            eventSender.setErrorResult(result, error_io_exception_user_message, null);
        }

        try {
            if (tries == 0 &&
                    result.containsKey(ERRORS) &&
                    TorStatusObservable.getStatus() == OFF &&
                    torHandler.startTorProxy()
            ) {
                return canConnect(provider, result, 1);
            }
        } catch (InterruptedException | IllegalStateException | TimeoutException e) {
            e.printStackTrace();
        }

        return false;
    }

    Bundle validateProviderDetails(Provider provider) {
        Bundle result = new Bundle();
        result.putBoolean(BROADCAST_RESULT_KEY, false);

        if (!provider.hasDefinition()) {
            return result;
        }

        result = validateCertificateForProvider(result, provider);

        //invalid certificate or no certificate or unable to connect due other connectivity issues
        if (result.containsKey(ERRORS) || (result.containsKey(BROADCAST_RESULT_KEY) && !result.getBoolean(BROADCAST_RESULT_KEY)) ) {
            return result;
        }

        result.putBoolean(BROADCAST_RESULT_KEY, true);

        return result;
    }

    protected Bundle validateCertificateForProvider(Bundle result, Provider provider) {
        String caCert = provider.getCaCert();

        if (ConfigHelper.checkErroneousDownload(caCert)) {
            VpnStatus.logWarning("[API] No provider cert.");
            return result;
        }

        ArrayList<X509Certificate> certificates = ConfigHelper.parseX509CertificatesFromString(caCert);
        if (certificates == null) {
            return eventSender.setErrorResult(result, warning_corrupted_provider_cert, ERROR_INVALID_CERTIFICATE.toString());
        }
        try {
            String encoding = provider.getCertificatePinEncoding();
            String expectedFingerprint = provider.getCertificatePin();

            // Do certificate pinning only if we have 1 cert, otherwise we assume some transitioning of
            // X509 certs, therefore we cannot do cert pinning
            if (certificates.size() == 1) {
                String realFingerprint = getFingerprintFromCertificate(certificates.get(0), encoding);
                if (!realFingerprint.trim().equalsIgnoreCase(expectedFingerprint.trim())) {
                    return eventSender.setErrorResult(result, warning_corrupted_provider_cert, ERROR_CERTIFICATE_PINNING.toString());
                }
            }
            for (X509Certificate certificate : certificates) {
                certificate.checkValidity();
            }

            if (!canConnect(provider, result)) {
                return result;
            }
        } catch (NoSuchAlgorithmException e ) {
            return eventSender.setErrorResult(result, error_no_such_algorithm_exception_user_message, null);
        } catch (ArrayIndexOutOfBoundsException e) {
            return eventSender.setErrorResult(result, warning_corrupted_provider_details, ERROR_CORRUPTED_PROVIDER_JSON.toString());
        } catch (CertificateEncodingException | CertificateNotYetValidException |
                 CertificateExpiredException e) {
            return eventSender.setErrorResult(result, warning_expired_provider_cert, ERROR_INVALID_CERTIFICATE.toString());
        }

        result.putBoolean(BROADCAST_RESULT_KEY, true);
        return result;
    }

    @NonNull
    protected List<Pair<String, String>> getAuthorizationHeader() {
        List<Pair<String, String>> headerArgs = new ArrayList<>();
        if (!LeapSRPSession.getToken().isEmpty()) {
            Pair<String, String> authorizationHeaderPair = new Pair<>(LeapSRPSession.AUTHORIZATION_HEADER, "Token token=" + LeapSRPSession.getToken());
            headerArgs.add(authorizationHeaderPair);
        }
        return headerArgs;
    }

}
