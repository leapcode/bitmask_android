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

package se.leap.bitmaskclient;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import okhttp3.OkHttpClient;
import se.leap.bitmaskclient.eip.EIP;

import static android.text.TextUtils.isEmpty;
import static se.leap.bitmaskclient.Constants.BROADCAST_RESULT_KEY;
import static se.leap.bitmaskclient.Constants.PROVIDER_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.DownloadFailedDialog.DOWNLOAD_ERRORS.ERROR_CERTIFICATE_PINNING;
import static se.leap.bitmaskclient.ProviderAPI.ERRORS;
import static se.leap.bitmaskclient.R.string.malformed_url;
import static se.leap.bitmaskclient.R.string.warning_corrupted_provider_cert;

/**
 * Implements the logic of the provider api http requests. The methods of this class need to be called from
 * a background thread.
 */


public class ProviderApiManager extends ProviderApiManagerBase {

    public ProviderApiManager(SharedPreferences preferences, Resources resources, OkHttpClientGenerator clientGenerator, ProviderApiServiceCallback callback) {
        super(preferences, resources, clientGenerator, callback);
    }

    /**
     * Only used in insecure flavor.
     */
    static boolean lastDangerOn() {
        return false;
    }

    /**
     * Downloads a provider.json from a given URL, adding a new provider using the given name.
     *
     * @param task containing a boolean meaning if the provider is custom or not, another boolean meaning if the user completely trusts this provider, the provider name and its provider.json url.
     * @return a bundle with a boolean value mapped to a key named BROADCAST_RESULT_KEY, and which is true if the update was successful.
     */
    @Override
    protected Bundle setUpProvider(Provider provider, Bundle task) {
        Bundle currentDownload = new Bundle();

        if (isEmpty(provider.getMainUrlString()) || provider.getMainUrl().isDefault()) {
            currentDownload.putBoolean(BROADCAST_RESULT_KEY, false);
            setErrorResult(currentDownload, malformed_url, null);
            return currentDownload;
        }

        getPersistedProviderUpdates(provider);
        currentDownload = validateProviderDetails(provider);

        //provider details invalid
        if (currentDownload.containsKey(ERRORS)) {
            return currentDownload;
        }

        //no provider certificate available
        if (currentDownload.containsKey(BROADCAST_RESULT_KEY) && !currentDownload.getBoolean(BROADCAST_RESULT_KEY)) {
            resetProviderDetails(provider);
        }

        if (!provider.hasDefinition()) {
            currentDownload = getAndSetProviderJson(provider);
        }
        if (provider.hasDefinition() || (currentDownload.containsKey(BROADCAST_RESULT_KEY) && currentDownload.getBoolean(BROADCAST_RESULT_KEY))) {
            if (!provider.hasCaCert())
                currentDownload = downloadCACert(provider);
            if (provider.hasCaCert() || (currentDownload.containsKey(BROADCAST_RESULT_KEY) && currentDownload.getBoolean(BROADCAST_RESULT_KEY))) {
                currentDownload = getAndSetEipServiceJson(provider);
            }
        }

        return currentDownload;
    }


    private Bundle getAndSetProviderJson(Provider provider) {
        Bundle result = new Bundle();

        String caCert = provider.getCaCert();
        JSONObject providerDefinition = provider.getDefinition();

        String providerDotJsonString;
        if(providerDefinition.length() == 0 || caCert.isEmpty()) {
            String providerJsonUrl = provider.getMainUrlString() + "/provider.json";
            providerDotJsonString = downloadWithCommercialCA(providerJsonUrl, provider);
        } else {
            providerDotJsonString = downloadFromApiUrlWithProviderCA("/provider.json", caCert, providerDefinition);
        }

        if (!isValidJson(providerDotJsonString)) {
            setErrorResult(result, malformed_url, null);
            return result;
        }

        try {
            JSONObject providerJson = new JSONObject(providerDotJsonString);
            provider.define(providerJson);

            result.putBoolean(BROADCAST_RESULT_KEY, true);
        } catch (JSONException e) {
            String reason_to_fail = pickErrorMessage(providerDotJsonString);
            result.putString(ERRORS, reason_to_fail);
            result.putBoolean(BROADCAST_RESULT_KEY, false);
        }
        return result;
    }

    /**
     * Downloads the eip-service.json from a given URL, and saves eip service capabilities including the offered gateways
     * @return a bundle with a boolean value mapped to a key named BROADCAST_RESULT_KEY, and which is true if the download was successful.
     */
    @Override
    protected Bundle getAndSetEipServiceJson(Provider provider) {
        Bundle result = new Bundle();
        String eipServiceJsonString = "";
        try {
            JSONObject providerJson = provider.getDefinition();
            String eipServiceUrl = providerJson.getString(Provider.API_URL) + "/" + providerJson.getString(Provider.API_VERSION) + "/" + EIP.SERVICE_API_PATH;
            eipServiceJsonString = downloadWithProviderCA(provider.getCaCert(), eipServiceUrl);
            JSONObject eipServiceJson = new JSONObject(eipServiceJsonString);
            eipServiceJson.getInt(Provider.API_RETURN_SERIAL);

            provider.setEipServiceJson(eipServiceJson);

            result.putBoolean(BROADCAST_RESULT_KEY, true);
        } catch (NullPointerException | JSONException e) {
            String reasonToFail = pickErrorMessage(eipServiceJsonString);
            result.putString(ERRORS, reasonToFail);
            result.putBoolean(BROADCAST_RESULT_KEY, false);
        }
        return result;
    }

    /**
     * Downloads a new OpenVPN certificate, attaching authenticated cookie for authenticated certificate.
     *
     * @return true if certificate was downloaded correctly, false if provider.json is not present in SharedPreferences, or if the certificate url could not be parsed as a URI, or if there was an SSL error.
     */
    @Override
    protected boolean updateVpnCertificate(Provider provider) {
        try {
            JSONObject providerJson = provider.getDefinition();
            String providerMainUrl = providerJson.getString(Provider.API_URL);
            URL newCertStringUrl = new URL(providerMainUrl + "/" + providerJson.getString(Provider.API_VERSION) + "/" + PROVIDER_VPN_CERTIFICATE);

            String certString = downloadWithProviderCA(provider.getCaCert(), newCertStringUrl.toString());

            if (ConfigHelper.checkErroneousDownload(certString))
                return false;
            else
                return loadCertificate(certString);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }

    private Bundle downloadCACert(Provider provider) {
        Bundle result = new Bundle();
        try {
            String caCertUrl = provider.getDefinition().getString(Provider.CA_CERT_URI);
            String providerDomain = getDomainFromMainURL(provider.getMainUrlString());
            String certString = downloadWithCommercialCA(caCertUrl, provider);

            if (validCertificate(provider, certString)) {
                provider.setCaCert(certString);
                preferences.edit().putString(Provider.CA_CERT + "." + providerDomain, certString).apply();
                result.putBoolean(BROADCAST_RESULT_KEY, true);
            } else {
                setErrorResult(result, warning_corrupted_provider_cert, ERROR_CERTIFICATE_PINNING.toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Tries to download the contents of the provided url using commercially validated CA certificate from chosen provider.
     *
     */
    private String downloadWithCommercialCA(String stringUrl, Provider provider) {
        String responseString;
        JSONObject errorJson = new JSONObject();

        OkHttpClient okHttpClient = clientGenerator.initCommercialCAHttpClient(errorJson);
        if (okHttpClient == null) {
            return errorJson.toString();
        }

        List<Pair<String, String>> headerArgs = getAuthorizationHeader();

        responseString = sendGetStringToServer(stringUrl, headerArgs, okHttpClient);

        if (responseString != null && responseString.contains(ERRORS)) {
            try {
                // try to download with provider CA on certificate error
                JSONObject responseErrorJson = new JSONObject(responseString);
                if (responseErrorJson.getString(ERRORS).equals(resources.getString(R.string.certificate_error))) {
                    responseString = downloadWithProviderCA(provider.getCaCert(), stringUrl);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return responseString;
    }


    /**
     * Tries to download the contents of the provided url using not commercially validated CA certificate from chosen provider.
     *
     * @return an empty string if it fails, the response body if not.
     */
    private String downloadFromApiUrlWithProviderCA(String path, String caCert, JSONObject providerDefinition) {
        String responseString;
        JSONObject errorJson = new JSONObject();
        String baseUrl = getApiUrl(providerDefinition);
        OkHttpClient okHttpClient = clientGenerator.initSelfSignedCAHttpClient(caCert, errorJson);
        if (okHttpClient == null) {
            return errorJson.toString();
        }

        String urlString = baseUrl + path;
        List<Pair<String, String>> headerArgs = getAuthorizationHeader();
        responseString = sendGetStringToServer(urlString, headerArgs, okHttpClient);

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

        OkHttpClient okHttpClient = clientGenerator.initSelfSignedCAHttpClient(caCert, initError);
        if (okHttpClient == null) {
            return initError.toString();
        }

        List<Pair<String, String>> headerArgs = getAuthorizationHeader();

        responseString = sendGetStringToServer(urlString, headerArgs, okHttpClient);

        return responseString;
    }
}
