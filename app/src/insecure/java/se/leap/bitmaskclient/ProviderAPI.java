/**
 * Copyright (c) 2013 LEAP Encryption Access Project and contributers
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

import android.os.Bundle;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;
import org.thoughtcrime.ssl.pinning.util.PinningHelper;

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
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import se.leap.bitmaskclient.ProviderListContent.ProviderItem;
import se.leap.bitmaskclient.eip.EIP;

import static se.leap.bitmaskclient.R.string.certificate_error;
import static se.leap.bitmaskclient.R.string.error_io_exception_user_message;
import static se.leap.bitmaskclient.R.string.malformed_url;

/**
 * Implements HTTP api methods used to manage communications with the provider server.
 * It extends the abstract ProviderApiBase and implements the diverging method calls between the different flavors
 * of ProviderAPI.
 * <p/>
 * It extends an  IntentService because it downloads data from the Internet, so it operates in the background.
 *
 * @author parmegv
 * @author MeanderingCode
 * @author cyberta
 */
public class ProviderAPI extends ProviderApiBase {

    private static boolean last_danger_on = true;

    public static boolean lastDangerOn() {
        return last_danger_on;
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
        Bundle current_download = new Bundle();

        if (task != null) {
            last_danger_on = task.containsKey(ProviderItem.DANGER_ON) && task.getBoolean(ProviderItem.DANGER_ON);
            last_provider_main_url = task.containsKey(Provider.MAIN_URL) ?
                    task.getString(Provider.MAIN_URL) :
                    "";
            provider_ca_cert_fingerprint = task.containsKey(Provider.CA_CERT_FINGERPRINT) ?
                    task.getString(Provider.CA_CERT_FINGERPRINT) :
                    "";
            CA_CERT_DOWNLOADED = PROVIDER_JSON_DOWNLOADED = EIP_SERVICE_JSON_DOWNLOADED = false;
            go_ahead = true;
        }

        if (!PROVIDER_JSON_DOWNLOADED)
            current_download = getAndSetProviderJson(last_provider_main_url, last_danger_on, provider_ca_cert_fingerprint);
        if (PROVIDER_JSON_DOWNLOADED || (current_download.containsKey(RESULT_KEY) && current_download.getBoolean(RESULT_KEY))) {
            broadcastProgress(progress++);
            PROVIDER_JSON_DOWNLOADED = true;
            current_download = downloadCACert(last_danger_on);

            if (CA_CERT_DOWNLOADED || (current_download.containsKey(RESULT_KEY) && current_download.getBoolean(RESULT_KEY))) {
                broadcastProgress(progress++);
                CA_CERT_DOWNLOADED = true;
                current_download = getAndSetEipServiceJson();
                if (current_download.containsKey(RESULT_KEY) && current_download.getBoolean(RESULT_KEY)) {
                    broadcastProgress(progress++);
                    EIP_SERVICE_JSON_DOWNLOADED = true;
                }
            }
        }

        return current_download;
    }

    private Bundle getAndSetProviderJson(String provider_main_url, boolean danger_on, String provider_ca_cert_fingerprint) {
        Bundle result = new Bundle();

        if (go_ahead) {
            String provider_dot_json_string;
            if(provider_ca_cert_fingerprint.isEmpty())
                provider_dot_json_string = downloadWithCommercialCA(provider_main_url + "/provider.json", danger_on);
            else
                provider_dot_json_string = downloadWithCommercialCA(provider_main_url + "/provider.json", danger_on, provider_ca_cert_fingerprint);

            if (!isValidJson(provider_dot_json_string)) {
                result.putString(ERRORS, getString(malformed_url));
                result.putBoolean(RESULT_KEY, false);
                return result;
            }

            try {
                JSONObject provider_json = new JSONObject(provider_dot_json_string);
                provider_api_url = provider_json.getString(Provider.API_URL) + "/" + provider_json.getString(Provider.API_VERSION);
                String name = provider_json.getString(Provider.NAME);
                //TODO setProviderName(name);

                preferences.edit().putString(Provider.KEY, provider_json.toString()).commit();
                preferences.edit().putBoolean(Constants.ALLOWED_ANON, provider_json.getJSONObject(Provider.SERVICE).getBoolean(Constants.ALLOWED_ANON)).commit();
                preferences.edit().putBoolean(Constants.ALLOWED_REGISTERED, provider_json.getJSONObject(Provider.SERVICE).getBoolean(Constants.ALLOWED_REGISTERED)).commit();

                result.putBoolean(RESULT_KEY, true);
            } catch (JSONException e) {
                //TODO Error message should be contained in that provider_dot_json_string
                String reason_to_fail = pickErrorMessage(provider_dot_json_string);
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
                eip_service_json_string = downloadWithProviderCA(eip_service_url, last_danger_on);
                JSONObject eip_service_json = new JSONObject(eip_service_json_string);
                eip_service_json.getInt(Provider.API_RETURN_SERIAL);

                preferences.edit().putString(Constants.KEY, eip_service_json.toString()).commit();

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
            URL new_cert_string_url = new URL(provider_main_url + "/" + provider_json.getString(Provider.API_VERSION) + "/" + Constants.VPN_CERTIFICATE);

            String cert_string = downloadWithProviderCA(new_cert_string_url.toString(), last_danger_on);

            if (cert_string == null || cert_string.isEmpty() || ConfigHelper.checkErroneousDownload(cert_string))
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


    private Bundle downloadCACert(boolean danger_on) {
        Bundle result = new Bundle();
        try {
            JSONObject provider_json = new JSONObject(preferences.getString(Provider.KEY, ""));
            String ca_cert_url = provider_json.getString(Provider.CA_CERT_URI);
            String cert_string = downloadWithCommercialCA(ca_cert_url, danger_on);

            if (validCertificate(cert_string) && go_ahead) {
                preferences.edit().putString(Provider.CA_CERT, cert_string).commit();
                result.putBoolean(RESULT_KEY, true);
            } else {
                String reason_to_fail = pickErrorMessage(cert_string);
                result.putString(ERRORS, reason_to_fail);
                result.putBoolean(RESULT_KEY, false);
            }
        } catch (JSONException e) {
            String reason_to_fail = formatErrorMessage(malformed_url);
            result.putString(ERRORS, reason_to_fail);
            result.putBoolean(RESULT_KEY, false);
        }

        return result;
    }

    //TODO: refactor with ticket #8773
    private String downloadWithCommercialCA(String urlString, boolean dangerOn, String caCertFingerprint) {
        String result = "";
        int seconds_of_timeout = 2;
        String[] pins = new String[] {caCertFingerprint};
        try {
            URL url = new URL(urlString);
            HttpsURLConnection connection = PinningHelper.getPinnedHttpsURLConnection(getApplicationContext(), pins, url);
            connection.setConnectTimeout(seconds_of_timeout * 1000);
            if (!LeapSRPSession.getToken().isEmpty())
                connection.addRequestProperty(LeapSRPSession.AUTHORIZATION_HEADER, "Token token=" + LeapSRPSession.getToken());
            result = new Scanner(connection.getInputStream()).useDelimiter("\\A").next();
        } catch (IOException e) {
            if(e instanceof SSLHandshakeException) {
                result = dangerOn ? downloadWithoutCA(urlString) :
                        formatErrorMessage(R.string.error_security_pinnedcertificate);
            } else
                result = formatErrorMessage(error_io_exception_user_message);
        }

        return result;
    }

    /**
     * Tries to download the contents of the provided url using commercially validated CA certificate from chosen provider.
     * <p/>
     * If danger_on flag is true, SSL exceptions will be managed by futher methods that will try to use some bypass methods.
     *
     * @param string_url
     * @param danger_on  if the user completely trusts this provider
     * @return
     */
    private String downloadWithCommercialCA(String string_url, boolean danger_on) {
        String responseString;
        JSONObject errorJson = new JSONObject();

        OkHttpClient okHttpClient = initCommercialCAHttpClient(errorJson);
        if (okHttpClient == null) {
            return errorJson.toString();
        }

        List<Pair<String, String>> headerArgs = getAuthorizationHeader();

        responseString = sendGetStringToServer(string_url, headerArgs, okHttpClient);

        if (responseString != null && responseString.contains(ERRORS)) {
            try {
                // try to download with provider CA on certificate error
                JSONObject responseErrorJson = new JSONObject(responseString);
                if (danger_on && responseErrorJson.getString(ERRORS).equals(getString(R.string.certificate_error))) {
                    responseString = downloadWithProviderCA(string_url, danger_on);
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
    private String downloadWithProviderCA(String urlString, boolean dangerOn) {
        JSONObject initError = new JSONObject();
        String responseString;

        OkHttpClient okHttpClient = initSelfSignedCAHttpClient(initError);
        if (okHttpClient == null) {
            return initError.toString();
        }

        List<Pair<String, String>> headerArgs = getAuthorizationHeader();

        responseString = sendGetStringToServer(urlString, headerArgs, okHttpClient);

        if (responseString.contains(ERRORS)) {
            try {
                // danger danger: try to download without CA on certificate error
                JSONObject responseErrorJson = new JSONObject(responseString);
                if (dangerOn && responseErrorJson.getString(ERRORS).equals(getString(R.string.certificate_error))) {
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
    private String downloadWithoutCA(String url_string) {
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

            URL url = new URL(url_string);
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
