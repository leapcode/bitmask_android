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
import static se.leap.bitmaskclient.DownloadFailedDialog.DOWNLOAD_ERRORS.ERROR_CERTIFICATE_PINNING;
import static se.leap.bitmaskclient.ProviderAPI.ERRORS;
import static se.leap.bitmaskclient.ProviderAPI.RESULT_KEY;
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
     * @return a bundle with a boolean value mapped to a key named RESULT_KEY, and which is true if the update was successful.
     */
    @Override
    protected Bundle setUpProvider(Bundle task) {
        int progress = 0;
        Bundle currentDownload = new Bundle();

        if (task != null) {
            //FIXME: this should be refactored in order to avoid static variables all over here
            lastProviderMainUrl = task.containsKey(Provider.MAIN_URL) ?
                    task.getString(Provider.MAIN_URL) :
                    "";

            if (isEmpty(lastProviderMainUrl)) {
                currentDownload.putBoolean(RESULT_KEY, false);
                setErrorResult(currentDownload, malformed_url, null);
                return currentDownload;
            }

            //TODO: remove that
            providerCaCertFingerprint = task.containsKey(Provider.CA_CERT_FINGERPRINT) ?
                    task.getString(Provider.CA_CERT_FINGERPRINT) :
                    "";
            providerCaCert = task.containsKey(Provider.CA_CERT) ?
                    task.getString(Provider.CA_CERT) :
                    "";

            try {
                providerDefinition = task.containsKey(Provider.KEY) ?
                        new JSONObject(task.getString(Provider.KEY)) :
                        new JSONObject();
            } catch (JSONException e) {
                e.printStackTrace();
                providerDefinition = new JSONObject();
            }
            providerApiUrl = getApiUrlWithVersion(providerDefinition);

            checkPersistedProviderUpdates();
            currentDownload = validateProviderDetails();

            //provider details invalid
            if (currentDownload.containsKey(ERRORS)) {
                return currentDownload;
            }

            //no provider certificate available
            if (currentDownload.containsKey(RESULT_KEY) && !currentDownload.getBoolean(RESULT_KEY)) {
                resetProviderDetails();
            }

            EIP_SERVICE_JSON_DOWNLOADED = false;
            go_ahead = true;
        }

        if (!PROVIDER_JSON_DOWNLOADED)
            currentDownload = getAndSetProviderJson(lastProviderMainUrl, providerCaCert, providerDefinition);
        if (PROVIDER_JSON_DOWNLOADED || (currentDownload.containsKey(RESULT_KEY) && currentDownload.getBoolean(RESULT_KEY))) {
            broadcastProgress(progress++);
            PROVIDER_JSON_DOWNLOADED = true;

            if (!CA_CERT_DOWNLOADED)
                currentDownload = downloadCACert();
            if (CA_CERT_DOWNLOADED || (currentDownload.containsKey(RESULT_KEY) && currentDownload.getBoolean(RESULT_KEY))) {
                broadcastProgress(progress++);
                CA_CERT_DOWNLOADED = true;
                currentDownload = getAndSetEipServiceJson();
                if (currentDownload.containsKey(RESULT_KEY) && currentDownload.getBoolean(RESULT_KEY)) {
                    broadcastProgress(progress++);
                    EIP_SERVICE_JSON_DOWNLOADED = true;
                }
            }
        }

        return currentDownload;
    }


    private Bundle getAndSetProviderJson(String providerMainUrl, String caCert, JSONObject providerDefinition) {
        Bundle result = new Bundle();

        if (go_ahead) {
            String providerDotJsonString;
            if(providerDefinition.length() == 0 || caCert.isEmpty())
                providerDotJsonString = downloadWithCommercialCA(providerMainUrl + "/provider.json");
            else {
                providerDotJsonString = downloadFromApiUrlWithProviderCA("/provider.json", caCert, providerDefinition);
            }

            if (!isValidJson(providerDotJsonString)) {
                setErrorResult(result, malformed_url, null);
                return result;
            }

            try {
                JSONObject providerJson = new JSONObject(providerDotJsonString);
                String providerDomain = getDomainFromMainURL(lastProviderMainUrl);
                providerApiUrl = getApiUrlWithVersion(providerJson);
                //String name = providerJson.getString(Provider.NAME);
                //TODO setProviderName(name);

                preferences.edit().putString(Provider.KEY, providerJson.toString()).
                        putBoolean(Constants.PROVIDER_ALLOW_ANONYMOUS, providerJson.getJSONObject(Provider.SERVICE).getBoolean(Constants.PROVIDER_ALLOW_ANONYMOUS)).
                        putBoolean(Constants.PROVIDER_ALLOWED_REGISTERED, providerJson.getJSONObject(Provider.SERVICE).getBoolean(Constants.PROVIDER_ALLOWED_REGISTERED)).
                        putString(Provider.KEY + "." + providerDomain, providerJson.toString()).commit();
                result.putBoolean(RESULT_KEY, true);
            } catch (JSONException e) {
                String reason_to_fail = pickErrorMessage(providerDotJsonString);
                result.putString(ERRORS, reason_to_fail);
                result.putBoolean(RESULT_KEY, false);
            }
        }
        return result;
    }

    /**
     * Downloads the eip-service.json from a given URL, and saves eip service capabilities including the offered gateways
     * @return a bundle with a boolean value mapped to a key named RESULT_KEY, and which is true if the download was successful.
     */
    @Override
    protected Bundle getAndSetEipServiceJson() {
        Bundle result = new Bundle();
        String eip_service_json_string = "";
        if (go_ahead) {
            try {
                JSONObject provider_json = new JSONObject(preferences.getString(Provider.KEY, ""));
                String eip_service_url = provider_json.getString(Provider.API_URL) + "/" + provider_json.getString(Provider.API_VERSION) + "/" + EIP.SERVICE_API_PATH;
                eip_service_json_string = downloadWithProviderCA(eip_service_url);
                JSONObject eip_service_json = new JSONObject(eip_service_json_string);
                eip_service_json.getInt(Provider.API_RETURN_SERIAL);

                preferences.edit().putString(Constants.PROVIDER_KEY, eip_service_json.toString()).commit();

                result.putBoolean(RESULT_KEY, true);
            } catch (NullPointerException | JSONException e) {
                String reason_to_fail = pickErrorMessage(eip_service_json_string);
                result.putString(ERRORS, reason_to_fail);
                result.putBoolean(RESULT_KEY, false);
            }
        }
        return result;
    }

    /**
     * Downloads a new OpenVPN certificate, attaching authenticated cookie for authenticated certificate.
     *
     * @return true if certificate was downloaded correctly, false if provider.json is not present in SharedPreferences, or if the certificate url could not be parsed as a URI, or if there was an SSL error.
     */
    @Override
    protected boolean updateVpnCertificate() {
        try {
            JSONObject provider_json = new JSONObject(preferences.getString(Provider.KEY, ""));

            String provider_main_url = provider_json.getString(Provider.API_URL);
            URL new_cert_string_url = new URL(provider_main_url + "/" + provider_json.getString(Provider.API_VERSION) + "/" + Constants.PROVIDER_VPN_CERTIFICATE);

            String cert_string = downloadWithProviderCA(new_cert_string_url.toString());

            if (ConfigHelper.checkErroneousDownload(cert_string))
                return false;
            else
                return loadCertificate(cert_string);
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

    private Bundle downloadCACert() {
        Bundle result = new Bundle();
        try {
            JSONObject providerJson = new JSONObject(preferences.getString(Provider.KEY, ""));
            String caCertUrl = providerJson.getString(Provider.CA_CERT_URI);
            String providerDomain = getDomainFromMainURL(lastProviderMainUrl);
            String cert_string = downloadWithCommercialCA(caCertUrl);

            if (validCertificate(cert_string) && go_ahead) {
                preferences.edit().putString(Provider.CA_CERT, cert_string).commit();
                preferences.edit().putString(Provider.CA_CERT + "." + providerDomain, cert_string).commit();
                result.putBoolean(RESULT_KEY, true);
            } else {
                setErrorResult(result, warning_corrupted_provider_cert, ERROR_CERTIFICATE_PINNING.toString());
            }
        } catch (JSONException e) {
            setErrorResult(result, malformed_url, null);
        }

        return result;
    }

    /**
     * Tries to download the contents of the provided url using commercially validated CA certificate from chosen provider.
     *
     * @param string_url
     * @return
     */
    private String downloadWithCommercialCA(String string_url) {
        String responseString;
        JSONObject errorJson = new JSONObject();

        OkHttpClient okHttpClient = clientGenerator.initCommercialCAHttpClient(errorJson);
        if (okHttpClient == null) {
            return errorJson.toString();
        }

        List<Pair<String, String>> headerArgs = getAuthorizationHeader();

        responseString = sendGetStringToServer(string_url, headerArgs, okHttpClient);

        if (responseString != null && responseString.contains(ERRORS)) {
            try {
                // try to download with provider CA on certificate error
                JSONObject responseErrorJson = new JSONObject(responseString);
                if (responseErrorJson.getString(ERRORS).equals(resources.getString(R.string.certificate_error))) {
                    responseString = downloadWithProviderCA(string_url);
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
        OkHttpClient okHttpClient = clientGenerator.initSelfSignedCAHttpClient(errorJson, caCert);
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
    private String downloadWithProviderCA(String urlString) {
        JSONObject initError = new JSONObject();
        String responseString;

        OkHttpClient okHttpClient = clientGenerator.initSelfSignedCAHttpClient(initError);
        if (okHttpClient == null) {
            return initError.toString();
        }

        List<Pair<String, String>> headerArgs = getAuthorizationHeader();

        responseString = sendGetStringToServer(urlString, headerArgs, okHttpClient);

        return responseString;
    }
}
