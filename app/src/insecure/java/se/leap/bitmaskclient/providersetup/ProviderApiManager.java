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

import android.content.SharedPreferences;
import android.content.res.Resources;
import static se.leap.bitmaskclient.BuildConfig.DEBUG_MODE;
import android.os.Bundle;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Scanner;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import de.blinkt.openvpn.core.VpnStatus;
import okhttp3.OkHttpClient;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.utils.ConfigHelper;
import se.leap.bitmaskclient.eip.EIP;
import se.leap.bitmaskclient.providersetup.connectivity.OkHttpClientGenerator;

import static android.text.TextUtils.isEmpty;
import static se.leap.bitmaskclient.R.string.certificate_error;
import static se.leap.bitmaskclient.R.string.downloading_vpn_certificate_failed;
import static se.leap.bitmaskclient.R.string.error_io_exception_user_message;
import static se.leap.bitmaskclient.R.string.malformed_url;
import static se.leap.bitmaskclient.R.string.setup_error_text;
import static se.leap.bitmaskclient.R.string.warning_corrupted_provider_cert;
import static se.leap.bitmaskclient.R.string.warning_corrupted_provider_details;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_KEY;
import static se.leap.bitmaskclient.base.models.Constants.DANGER_ON;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.base.utils.ConfigHelper.getProviderFormattedString;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.ERRORS;
import static se.leap.bitmaskclient.providersetup.ProviderSetupFailedDialog.DOWNLOAD_ERRORS.ERROR_CERTIFICATE_PINNING;
import static se.leap.bitmaskclient.providersetup.ProviderSetupFailedDialog.DOWNLOAD_ERRORS.ERROR_CORRUPTED_PROVIDER_JSON;
import static se.leap.bitmaskclient.tor.TorStatusObservable.getProxyPort;

/**
 * Created by cyberta on 04.01.18.
 */

public class ProviderApiManager extends ProviderApiManagerBase {

    private static final String TAG = ProviderApiManagerBase.class.getName();

    protected static boolean lastDangerOn = true;


    public ProviderApiManager(SharedPreferences preferences, Resources resources, OkHttpClientGenerator clientGenerator, ProviderApiServiceCallback callback) {
        super(preferences, resources, clientGenerator, callback);
    }

    public static boolean lastDangerOn() {
        return lastDangerOn;
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

        if (task != null) {
            lastDangerOn = task.containsKey(DANGER_ON) && task.getBoolean(DANGER_ON);
        }

        if (isEmpty(provider.getMainUrlString()) || provider.getMainUrl().isDefault()) {
            setErrorResult(currentDownload, malformed_url, null);
            currentDownload.putParcelable(PROVIDER_KEY, provider);
            VpnStatus.logWarning("[API] MainURL String is not set. Cannot setup provider.");
            return currentDownload;
        }

        getPersistedProviderUpdates(provider);
        currentDownload = validateProviderDetails(provider);

        //provider details invalid
        if (currentDownload.containsKey(ERRORS)) {
            currentDownload.putParcelable(PROVIDER_KEY, provider);
            return currentDownload;
        }

        //no provider certificate available
        if (currentDownload.containsKey(BROADCAST_RESULT_KEY) && !currentDownload.getBoolean(BROADCAST_RESULT_KEY)) {
            resetProviderDetails(provider);
        }

        currentDownload = getAndSetProviderJson(provider, lastDangerOn);
        if (provider.hasDefinition() || (currentDownload.containsKey(BROADCAST_RESULT_KEY) && currentDownload.getBoolean(BROADCAST_RESULT_KEY))) {
            if (!provider.hasCaCert())
                currentDownload = downloadCACert(provider, lastDangerOn);
            if (provider.hasCaCert() || (currentDownload.containsKey(BROADCAST_RESULT_KEY) && currentDownload.getBoolean(BROADCAST_RESULT_KEY))) {
                currentDownload = getAndSetEipServiceJson(provider);
            }
            if (provider.hasEIP() && !provider.allowsRegistered() && !provider.allowsAnonymous()) {
                setErrorResult(currentDownload, setup_error_text, null);
            }
        }
        currentDownload.putParcelable(PROVIDER_KEY, provider);
        return currentDownload;
    }

    private Bundle getAndSetProviderJson(Provider provider, boolean dangerOn) {
        Bundle result = new Bundle();

        JSONObject providerDefinition = provider.getDefinition();
        String providerMainUrl = provider.getMainUrlString();

        String providerDotJsonString;
        if(providerDefinition.length() == 0 || provider.getCaCert().isEmpty())
            providerDotJsonString = downloadWithCommercialCA(providerMainUrl + "/provider.json", dangerOn);
        else
            providerDotJsonString = downloadFromApiUrlWithProviderCA("/provider.json", provider, dangerOn);

        if (ConfigHelper.checkErroneousDownload(providerDotJsonString) || !isValidJson(providerDotJsonString)) {
            setErrorResult(result, malformed_url, null);
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
                return setErrorResult(result, warning_corrupted_provider_details, ERROR_CORRUPTED_PROVIDER_JSON.toString());
            }

            result.putBoolean(BROADCAST_RESULT_KEY, true);
        } catch (JSONException e) {
            setErrorResult(result, providerDotJsonString);
        }
        //TODO: check why the following line is not in production
        result.putParcelable(PROVIDER_KEY, provider);
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
            JSONObject providerDefinition = provider.getDefinition();
            String eipServiceUrl = providerDefinition.getString(Provider.API_URL) + "/" + providerDefinition.getString(Provider.API_VERSION) + "/" + EIP.SERVICE_API_PATH;
            eipServiceJsonString = downloadWithProviderCA(provider.getCaCert(), eipServiceUrl, lastDangerOn);
            if (DEBUG_MODE) {
                VpnStatus.logDebug("[API] EIP SERVICE JSON: " + eipServiceJsonString);
            }

            JSONObject eipServiceJson = new JSONObject(eipServiceJsonString);

            if (eipServiceJson.has(ERRORS)) {
                setErrorResult(result, eipServiceJsonString);
            } else{
                provider.setEipServiceJson(eipServiceJson);
                provider.setLastEipServiceUpdate(System.currentTimeMillis());
                result.putBoolean(BROADCAST_RESULT_KEY, true);
            }
        } catch (NullPointerException | JSONException e) {
            setErrorResult(result, R.string.error_json_exception_user_message, null);
        }
        //TODO: check why the following line is not in production
        result.putParcelable(PROVIDER_KEY, provider);
        return result;
    }

    /**
     * Downloads a new OpenVPN certificate, attaching authenticated cookie for authenticated certificate.
     *
     * @return true if certificate was downloaded correctly, false if provider.json is not present in SharedPreferences, or if the certificate url could not be parsed as a URI, or if there was an SSL error.
     */
    @Override
    protected Bundle updateVpnCertificate(Provider provider) {
        Bundle result = new Bundle();
        try {
            URL newCertStringUrl = new URL(provider.getApiUrlWithVersion() + "/" + PROVIDER_VPN_CERTIFICATE);

            String certString = downloadWithProviderCA(provider.getCaCert(), newCertStringUrl.toString(), lastDangerOn);
            if (DEBUG_MODE) {
                VpnStatus.logDebug("[API] VPN CERT: " + certString);
            }
            if (ConfigHelper.checkErroneousDownload(certString)) {
                if (certString == null || certString.isEmpty()) {
                    // probably 204
                    setErrorResult(result, error_io_exception_user_message, null);
                } else {
                    setErrorResult(result, certString);
                    return result;
                }
            }
            result = loadCertificate(provider, certString);
        } catch (IOException e) {
            setErrorResult(result, downloading_vpn_certificate_failed, null);
            e.printStackTrace();
        }
        //TODO: check why the following line is not in production
        result.putParcelable(PROVIDER_KEY, provider);
        return result;
    }

    /**
     * Fetches the Geo ip Json, containing a list of gateways sorted by distance from the users current location
     *
     * @param provider
     * @return
     */
    @Override
    protected Bundle getGeoIPJson(Provider provider) {
        Bundle result = new Bundle();

        if (!provider.shouldUpdateGeoIpJson() || provider.getGeoipUrl().isDefault()) {
            result.putBoolean(BROADCAST_RESULT_KEY, false);
            return result;
        }


        try {
            URL geoIpUrl = provider.getGeoipUrl().getUrl();

            String geoipJsonString = downloadFromUrlWithProviderCA(geoIpUrl.toString(), provider, lastDangerOn);
            if (DEBUG_MODE) {
                VpnStatus.logDebug("[API] MENSHEN JSON: " + geoipJsonString);
            }
            JSONObject geoipJson = new JSONObject(geoipJsonString);

            if (geoipJson.has(ERRORS)) {
                result.putBoolean(BROADCAST_RESULT_KEY, false);
            } else {
                provider.setGeoIpJson(geoipJson);
                provider.setLastEipServiceUpdate(System.currentTimeMillis());
                result.putBoolean(BROADCAST_RESULT_KEY, true);
            }


        } catch (JSONException | NullPointerException e) {
            result.putBoolean(BROADCAST_RESULT_KEY, false);
            e.printStackTrace();
        }
        return result;
    }


    private Bundle downloadCACert(Provider provider, boolean dangerOn) {
        Bundle result = new Bundle();
        try {
            String caCertUrl = provider.getDefinition().getString(Provider.CA_CERT_URI);
            String providerDomain = provider.getDomain();

            String certString = downloadWithCommercialCA(caCertUrl, dangerOn);

            if (validCertificate(provider, certString)) {
                provider.setCaCert(certString);
                if (DEBUG_MODE) {
                    VpnStatus.logDebug("[API] CA CERT: " + certString);
                }
                preferences.edit().putString(Provider.CA_CERT + "." + providerDomain, certString).apply();
                result.putBoolean(BROADCAST_RESULT_KEY, true);
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
     * <p/>
     * If dangerOn flag is true, SSL exceptions will be managed by futher methods that will try to use some bypass methods.
     *
     * @param stringUrl
     * @param dangerOn  if the user completely trusts this provider
     * @return
     */
    private String downloadWithCommercialCA(String stringUrl, boolean dangerOn) {
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
                if (dangerOn && responseErrorJson.getString(ERRORS).equals(
                        getProviderFormattedString(resources, R.string.certificate_error))) {
                    responseString = downloadWithoutCA(stringUrl);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return responseString;
    }

    private String downloadFromApiUrlWithProviderCA(String path, Provider provider, boolean dangerOn) {
        String baseUrl = provider.getApiUrlString();
        String urlString = baseUrl + path;

        return downloadFromUrlWithProviderCA(urlString, provider, dangerOn);
    }

    private String downloadFromUrlWithProviderCA(String urlString, Provider provider, boolean dangerOn) {
        String responseString;
        JSONObject errorJson = new JSONObject();
        OkHttpClient okHttpClient = clientGenerator.initSelfSignedCAHttpClient(provider.getCaCert(), getProxyPort(), errorJson);
        if (okHttpClient == null) {
            return errorJson.toString();
        }

        List<Pair<String, String>> headerArgs = getAuthorizationHeader();
        responseString = sendGetStringToServer(urlString, headerArgs, okHttpClient);

        if (responseString != null && responseString.contains(ERRORS)) {
            try {
                // try to download with provider CA on certificate error
                JSONObject responseErrorJson = new JSONObject(responseString);
                if (dangerOn && responseErrorJson.getString(ERRORS).equals(
                        getProviderFormattedString(resources, R.string.certificate_error))) {
                    responseString = downloadWithCommercialCA(urlString, dangerOn);
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
     * @param urlString as a string
     * @param dangerOn  true to download CA certificate in case it has not been downloaded.
     * @return an empty string if it fails, the url content if not.
     */
    private String downloadWithProviderCA(String caCert, String urlString, boolean dangerOn) {
        JSONObject initError = new JSONObject();
        String responseString;

        OkHttpClient okHttpClient = clientGenerator.initSelfSignedCAHttpClient(caCert, getProxyPort(), initError);
        if (okHttpClient == null) {
            return initError.toString();
        }

        List<Pair<String, String>> headerArgs = getAuthorizationHeader();

        responseString = sendGetStringToServer(urlString, headerArgs, okHttpClient);

        if (responseString.contains(ERRORS)) {
            try {
                // danger danger: try to download without CA on certificate error
                JSONObject responseErrorJson = new JSONObject(responseString);
                if (dangerOn && responseErrorJson.getString(ERRORS).equals(
                        getProviderFormattedString(resources, R.string.certificate_error))) {
                    responseString = downloadWithoutCA(urlString);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return responseString;
    }

    /**
     * Downloads the string that's in the url with any certificate.
     */
    // This method is totally insecure anyways. So no need to refactor that in order to use okHttpClient, force modern TLS etc.. DO NOT USE IN PRODUCTION!
    private String downloadWithoutCA(String urlString) {
        String string = "";
        try {

            HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            class DefaultTrustManager implements X509TrustManager {

                @Override
                public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            }

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(new KeyManager[0], new TrustManager[]{new DefaultTrustManager()}, new SecureRandom());

            URL url = new URL(urlString);
            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setSSLSocketFactory(context.getSocketFactory());
            urlConnection.setHostnameVerifier(hostnameVerifier);
            string = new Scanner(urlConnection.getInputStream()).useDelimiter("\\A").next();
            System.out.println("String ignoring certificate = " + string);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            string = formatErrorMessage(malformed_url);
        } catch (IOException e) {
            // The downloaded certificate doesn't validate our https connection.
            e.printStackTrace();
            string = formatErrorMessage(certificate_error);
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (KeyManagementException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return string;
    }
}
