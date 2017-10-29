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

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;
import org.thoughtcrime.ssl.pinning.util.PinningHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.net.UnknownServiceException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.TlsVersion;
import se.leap.bitmaskclient.ProviderListContent.ProviderItem;
import se.leap.bitmaskclient.eip.Constants;
import se.leap.bitmaskclient.eip.EIP;
import se.leap.bitmaskclient.userstatus.SessionDialog;
import se.leap.bitmaskclient.userstatus.User;
import se.leap.bitmaskclient.userstatus.UserStatus;

import static se.leap.bitmaskclient.R.string.certificate_error;
import static se.leap.bitmaskclient.R.string.error_io_exception_user_message;
import static se.leap.bitmaskclient.R.string.error_json_exception_user_message;
import static se.leap.bitmaskclient.R.string.error_no_such_algorithm_exception_user_message;
import static se.leap.bitmaskclient.R.string.keyChainAccessError;
import static se.leap.bitmaskclient.R.string.malformed_url;
import static se.leap.bitmaskclient.R.string.server_unreachable_message;
import static se.leap.bitmaskclient.R.string.service_is_down_error;

/**
 * Implements HTTP api methods used to manage communications with the provider server.
 * <p/>
 * It's an IntentService because it downloads data from the Internet, so it operates in the background.
 *
 * @author parmegv
 * @author MeanderingCode
 * @author cyberta
 */
public class ProviderAPI extends IntentService {

    final public static String
            TAG = ProviderAPI.class.getSimpleName(),
            SET_UP_PROVIDER = "setUpProvider",
            DOWNLOAD_NEW_PROVIDER_DOTJSON = "downloadNewProviderDotJSON",
            SIGN_UP = "srpRegister",
            LOG_IN = "srpAuth",
            LOG_OUT = "logOut",
            DOWNLOAD_CERTIFICATE = "downloadUserAuthedCertificate",
            PARAMETERS = "parameters",
            RESULT_KEY = "result",
            RECEIVER_KEY = "receiver",
            ERRORS = "errors",
            ERROR = "error",
            UPDATE_PROGRESSBAR = "update_progressbar",
            CURRENT_PROGRESS = "current_progress",
            DOWNLOAD_EIP_SERVICE = TAG + ".DOWNLOAD_EIP_SERVICE";

    final public static int
            SUCCESSFUL_LOGIN = 3,
            FAILED_LOGIN = 4,
            SUCCESSFUL_SIGNUP = 5,
            FAILED_SIGNUP = 6,
            SUCCESSFUL_LOGOUT = 7,
            LOGOUT_FAILED = 8,
            CORRECTLY_DOWNLOADED_CERTIFICATE = 9,
            INCORRECTLY_DOWNLOADED_CERTIFICATE = 10,
            PROVIDER_OK = 11,
            PROVIDER_NOK = 12,
            CORRECTLY_DOWNLOADED_EIP_SERVICE = 13,
            INCORRECTLY_DOWNLOADED_EIP_SERVICE = 14;

    private static boolean
            CA_CERT_DOWNLOADED = false,
            PROVIDER_JSON_DOWNLOADED = false,
            EIP_SERVICE_JSON_DOWNLOADED = false;

    private static String last_provider_main_url;
    private static boolean last_danger_on = false;
    private static boolean go_ahead = true;
    private static SharedPreferences preferences;
    private static String provider_api_url;
    private static String provider_ca_cert_fingerprint;
    private Resources resources;
    public static void stop() {
        go_ahead = false;
    }

    private final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    public ProviderAPI() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        preferences = getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE);
        resources = getResources();
    }

    public static String lastProviderMainUrl() {
        return last_provider_main_url;
    }

    public static boolean lastDangerOn() {
        return last_danger_on;
    }


    @Override
    protected void onHandleIntent(Intent command) {
        final ResultReceiver receiver = command.getParcelableExtra(RECEIVER_KEY);

        String action = command.getAction();
        Bundle parameters = command.getBundleExtra(PARAMETERS);
        if (provider_api_url == null && preferences.contains(Provider.KEY)) {
            try {
                JSONObject provider_json = new JSONObject(preferences.getString(Provider.KEY, "no provider"));
                provider_api_url = provider_json.getString(Provider.API_URL) + "/" + provider_json.getString(Provider.API_VERSION);
                go_ahead = true;
            } catch (JSONException e) {
                go_ahead = false;
            }
        }

        if (action.equalsIgnoreCase(SET_UP_PROVIDER)) {
            Bundle result = setUpProvider(parameters);
            if (result.getBoolean(RESULT_KEY)) {
                receiver.send(PROVIDER_OK, result);
            } else {
                receiver.send(PROVIDER_NOK, result);
            }
        } else if (action.equalsIgnoreCase(SIGN_UP)) {
            UserStatus.updateStatus(UserStatus.SessionStatus.SIGNING_UP, resources);
            Bundle result = tryToRegister(parameters);
            if (result.getBoolean(RESULT_KEY)) {
                receiver.send(SUCCESSFUL_SIGNUP, result);
            } else {
                receiver.send(FAILED_SIGNUP, result);
            }
        } else if (action.equalsIgnoreCase(LOG_IN)) {
            UserStatus.updateStatus(UserStatus.SessionStatus.LOGGING_IN, resources);
            Bundle result = tryToAuthenticate(parameters);
            if (result.getBoolean(RESULT_KEY)) {
                receiver.send(SUCCESSFUL_LOGIN, result);
                UserStatus.updateStatus(UserStatus.SessionStatus.LOGGED_IN, resources);
            } else {
                receiver.send(FAILED_LOGIN, result);
                UserStatus.updateStatus(UserStatus.SessionStatus.NOT_LOGGED_IN, resources);
            }
        } else if (action.equalsIgnoreCase(LOG_OUT)) {
            UserStatus.updateStatus(UserStatus.SessionStatus.LOGGING_OUT, resources);
            if (logOut()) {
                receiver.send(SUCCESSFUL_LOGOUT, Bundle.EMPTY);
                android.util.Log.d(TAG, "Logged out, notifying user status");
                UserStatus.updateStatus(UserStatus.SessionStatus.LOGGED_OUT, resources);
            } else {
                receiver.send(LOGOUT_FAILED, Bundle.EMPTY);
                UserStatus.updateStatus(UserStatus.SessionStatus.DIDNT_LOG_OUT, resources);
            }
        } else if (action.equalsIgnoreCase(DOWNLOAD_CERTIFICATE)) {
            if (updateVpnCertificate()) {
                receiver.send(CORRECTLY_DOWNLOADED_CERTIFICATE, Bundle.EMPTY);
            } else {
                receiver.send(INCORRECTLY_DOWNLOADED_CERTIFICATE, Bundle.EMPTY);
            }
        } else if (action.equalsIgnoreCase(DOWNLOAD_EIP_SERVICE)) {
            Bundle result = getAndSetEipServiceJson();
            if (result.getBoolean(RESULT_KEY)) {
                receiver.send(CORRECTLY_DOWNLOADED_EIP_SERVICE, result);
            } else {
                receiver.send(INCORRECTLY_DOWNLOADED_EIP_SERVICE, result);
            }
        }
    }

    private String formatErrorMessage(final int toastStringId) {
        return formatErrorMessage(getResources().getString(toastStringId));
    }

    private String formatErrorMessage(String errorMessage) {
        return "{ \"" + ERRORS + "\" : \"" + errorMessage + "\" }";
    }

    private JSONObject getErrorMessageAsJson(final int toastStringId) {
        try {
            return new JSONObject(formatErrorMessage(toastStringId));
        } catch (JSONException e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

    private JSONObject getErrorMessageAsJson(String message) {
        try {
            return new JSONObject(formatErrorMessage(message));
        } catch (JSONException e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

    private OkHttpClient initHttpClient(JSONObject initError, boolean isSelfSigned) {
        try {
            TLSCompatSocketFactory sslCompatFactory;
            ConnectionSpec spec = getConnectionSpec();
            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
            if (isSelfSigned) {
                sslCompatFactory = new TLSCompatSocketFactory(preferences.getString(Provider.CA_CERT, ""));

            } else {
                sslCompatFactory = new TLSCompatSocketFactory();
            }
            sslCompatFactory.initSSLSocketFactory(clientBuilder);
            clientBuilder.cookieJar(getCookieJar())
                    .connectionSpecs(Collections.singletonList(spec));
            return clientBuilder.build();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            initError = getErrorMessageAsJson(String.format(getResources().getString(keyChainAccessError), e.getLocalizedMessage()));
        } catch (KeyStoreException e) {
            e.printStackTrace();
            initError = getErrorMessageAsJson(String.format(getResources().getString(keyChainAccessError), e.getLocalizedMessage()));
        } catch (KeyManagementException e) {
            e.printStackTrace();
            initError = getErrorMessageAsJson(String.format(getResources().getString(keyChainAccessError), e.getLocalizedMessage()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            initError = getErrorMessageAsJson(getResources().getString(error_no_such_algorithm_exception_user_message));
        } catch (CertificateException e) {
            e.printStackTrace();
            initError = getErrorMessageAsJson(getResources().getString(certificate_error));
        } catch (UnknownHostException e) {
            e.printStackTrace();
            initError = getErrorMessageAsJson(getResources().getString(server_unreachable_message));
        } catch (IOException e) {
            e.printStackTrace();
            initError = getErrorMessageAsJson(getResources().getString(error_io_exception_user_message));
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
            initError = getErrorMessageAsJson(getResources().getString(error_no_such_algorithm_exception_user_message));
        }
        return null;
    }
    private OkHttpClient initCommercialCAHttpClient(JSONObject initError) {
        return initHttpClient(initError, false);
    }

    private OkHttpClient initSelfSignedCAHttpClient(JSONObject initError) {
        return initHttpClient(initError, true);
    }

    @NonNull
    private ConnectionSpec getConnectionSpec() {
        ConnectionSpec.Builder connectionSpecbuilder = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3);
        //FIXME: restrict connection further to the following recommended cipher suites for ALL supported API levels
        //figure out how to use bcjsse for that purpose
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
            connectionSpecbuilder.cipherSuites(
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256,
                    CipherSuite.TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256
            );
        return connectionSpecbuilder.build();
    }

    @NonNull
    private CookieJar getCookieJar() {
        return new CookieJar() {
            private final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                cookieStore.put(url.host(), cookies);
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {
                List<Cookie> cookies = cookieStore.get(url.host());
                return cookies != null ? cookies : new ArrayList<Cookie>();
            }
        };
    }


    private Bundle tryToRegister(Bundle task) {
        Bundle result = new Bundle();
        int progress = 0;

        String username = User.userName();
        String password = task.getString(SessionDialog.PASSWORD);

        if (validUserLoginData(username, password)) {
            result = register(username, password);
            broadcastProgress(progress++);
        } else {
            if (!wellFormedPassword(password)) {
                result.putBoolean(RESULT_KEY, false);
                result.putString(SessionDialog.USERNAME, username);
                result.putBoolean(SessionDialog.ERRORS.PASSWORD_INVALID_LENGTH.toString(), true);
            }
            if (!validUsername(username)) {
                result.putBoolean(RESULT_KEY, false);
                result.putBoolean(SessionDialog.ERRORS.USERNAME_MISSING.toString(), true);
            }
        }

        return result;
    }

    private Bundle register(String username, String password) {
        JSONObject stepResult = null;
        OkHttpClient okHttpClient = initSelfSignedCAHttpClient(stepResult);
        if (okHttpClient == null) {
            return authFailedNotification(stepResult, username);
        }

        LeapSRPSession client = new LeapSRPSession(username, password);
        byte[] salt = client.calculateNewSalt();

        BigInteger password_verifier = client.calculateV(username, password, salt);

        stepResult = sendNewUserDataToSRPServer(provider_api_url, username, new BigInteger(1, salt).toString(16), password_verifier.toString(16), okHttpClient);

        Bundle result = new Bundle();
        if (stepResult.has(ERRORS))
            result = authFailedNotification(stepResult, username);
        else {
            result.putString(SessionDialog.USERNAME, username);
            result.putString(SessionDialog.PASSWORD, password);
            result.putBoolean(RESULT_KEY, true);
        }

        return result;
    }

    /**
     * Starts the authentication process using SRP protocol.
     *
     * @param task containing: username, password and api url.
     * @return a bundle with a boolean value mapped to a key named RESULT_KEY, and which is true if authentication was successful.
     */
    private Bundle tryToAuthenticate(Bundle task) {
        Bundle result = new Bundle();
        int progress = 0;

        String username = User.userName();
        String password = task.getString(SessionDialog.PASSWORD);
        if (validUserLoginData(username, password)) {
            result = authenticate(username, password);
            broadcastProgress(progress++);
        } else {
            if (!wellFormedPassword(password)) {
                result.putBoolean(RESULT_KEY, false);
                result.putString(SessionDialog.USERNAME, username);
                result.putBoolean(SessionDialog.ERRORS.PASSWORD_INVALID_LENGTH.toString(), true);
            }
            if (!validUsername(username)) {
                result.putBoolean(RESULT_KEY, false);
                result.putBoolean(SessionDialog.ERRORS.USERNAME_MISSING.toString(), true);
            }
        }

        return result;
    }


    private Bundle authenticate(String username, String password) {
        Bundle result = new Bundle();
        JSONObject stepResult = new JSONObject();
        OkHttpClient okHttpClient = initSelfSignedCAHttpClient(stepResult);
        if (okHttpClient == null) {
            return authFailedNotification(stepResult, username);
        }

        LeapSRPSession client = new LeapSRPSession(username, password);
        byte[] A = client.exponential();

        stepResult = sendAToSRPServer(provider_api_url, username, new BigInteger(1, A).toString(16), okHttpClient);
        try {
            String salt = stepResult.getString(LeapSRPSession.SALT);
            byte[] Bbytes = new BigInteger(stepResult.getString("B"), 16).toByteArray();
            byte[] M1 = client.response(new BigInteger(salt, 16).toByteArray(), Bbytes);
            if (M1 != null) {
                stepResult = sendM1ToSRPServer(provider_api_url, username, M1, okHttpClient);
                setTokenIfAvailable(stepResult);
                byte[] M2 = new BigInteger(stepResult.getString(LeapSRPSession.M2), 16).toByteArray();
                if (client.verify(M2)) {
                    result.putBoolean(RESULT_KEY, true);
                } else {
                    authFailedNotification(stepResult, username);
                }
            } else {
                result.putBoolean(RESULT_KEY, false);
                result.putString(SessionDialog.USERNAME, username);
                result.putString(getResources().getString(R.string.user_message), getResources().getString(R.string.error_srp_math_error_user_message));
            }
        } catch (JSONException e) {
            result = authFailedNotification(stepResult, username);
            e.printStackTrace();
        }

        return result;
    }

    private boolean setTokenIfAvailable(JSONObject authentication_step_result) {
        try {
            LeapSRPSession.setToken(authentication_step_result.getString(LeapSRPSession.TOKEN));
        } catch (JSONException e) {
            return false;
        }
        return true;
    }

    private Bundle authFailedNotification(JSONObject result, String username) {
        Bundle userNotificationBundle = new Bundle();
        Object baseErrorMessage = result.opt(ERRORS);
        if (baseErrorMessage != null) {
            if (baseErrorMessage instanceof JSONObject) {
                try {
                    JSONObject errorMessage = result.getJSONObject(ERRORS);
                    String errorType = errorMessage.keys().next().toString();
                    String message = errorMessage.get(errorType).toString();
                    userNotificationBundle.putString(getResources().getString(R.string.user_message), message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (baseErrorMessage instanceof String) {
                try {
                    String errorMessage = result.getString(ERRORS);
                    userNotificationBundle.putString(getResources().getString(R.string.user_message), errorMessage);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        if (!username.isEmpty())
            userNotificationBundle.putString(SessionDialog.USERNAME, username);
        userNotificationBundle.putBoolean(RESULT_KEY, false);

        return userNotificationBundle;
    }

    /**
     * Sets up an intent with the progress value passed as a parameter
     * and sends it as a broadcast.
     *
     * @param progress
     */
    private void broadcastProgress(int progress) {
        Intent intentUpdate = new Intent();
        intentUpdate.setAction(UPDATE_PROGRESSBAR);
        intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
        intentUpdate.putExtra(CURRENT_PROGRESS, progress);
        sendBroadcast(intentUpdate);
    }

    /**
     * Validates parameters entered by the user to log in
     *
     * @param username
     * @param password
     * @return true if both parameters are present and the entered password length is greater or equal to eight (8).
     */
    private boolean validUserLoginData(String username, String password) {
        return validUsername(username) && wellFormedPassword(password);
    }

    private boolean validUsername(String username) {
        return username != null && !username.isEmpty();
    }

    /**
     * Validates a password
     *
     * @param password
     * @return true if the entered password length is greater or equal to eight (8).
     */
    private boolean wellFormedPassword(String password) {
        return password != null && password.length() >= 8;
    }

    /**
     * Sends an HTTP POST request to the authentication server with the SRP Parameter A.
     *
     * @param server_url
     * @param username
     * @param clientA    First SRP parameter sent
     * @param okHttpClient
     * @return response from authentication server
     */
    private JSONObject sendAToSRPServer(String server_url, String username, String clientA, OkHttpClient okHttpClient) {
        SrpCredentials srpCredentials = new SrpCredentials(username, clientA);
        return sendToServer(server_url + "/sessions.json", "POST", srpCredentials.toString(), okHttpClient);
    }

    /**
     * Sends an HTTP PUT request to the authentication server with the SRP Parameter M1 (or simply M).
     *
     * @param server_url
     * @param username
     * @param m1         Second SRP parameter sent
     * @param okHttpClient
     * @return response from authentication server
     */
    private JSONObject sendM1ToSRPServer(String server_url, String username, byte[] m1, OkHttpClient okHttpClient) {

        String m1json = "{\"client_auth\":\"" + new BigInteger(1, ConfigHelper.trim(m1)).toString(16)+ "\"}";
        return sendToServer(server_url + "/sessions/" + username + ".json", "PUT", m1json, okHttpClient);
    }

    /**
     * Sends an HTTP POST request to the api server to register a new user.
     *
     * @param server_url
     * @param username
     * @param salt
     * @param password_verifier
     * @return response from authentication server
     */
    private JSONObject sendNewUserDataToSRPServer(String server_url, String username, String salt, String password_verifier, OkHttpClient okHttpClient) {
        return sendToServer(server_url + "/users.json", "POST", new SrpRegistrationData(username, salt, password_verifier).toString(), okHttpClient);
    }

    private JSONObject sendToServer(String url, String request_method, String jsonString, OkHttpClient okHttpClient) {
        return requestJsonFromServer(url, request_method, jsonString, null, okHttpClient);
    }

    private String sendGetStringToServer(String url, List<Pair<String, String>> headerArgs, OkHttpClient okHttpClient) {
        return requestStringFromServer(url, "GET", null, headerArgs, okHttpClient);
    }



    private JSONObject requestJsonFromServer(String url, String request_method, String jsonString, List<Pair<String, String>> headerArgs, @NonNull OkHttpClient okHttpClient)  {
        JSONObject responseJson;
        String plain_response = requestStringFromServer(url, request_method, jsonString, headerArgs, okHttpClient);

        try {
            responseJson = new JSONObject(plain_response);
        } catch (JSONException e) {
            e.printStackTrace();
            responseJson = getErrorMessageAsJson(error_json_exception_user_message);
        }
        return responseJson;

    }

    private String requestStringFromServer(String url, String request_method, String jsonString, List<Pair<String, String>> headerArgs, @NonNull OkHttpClient okHttpClient) {
        Response response;
        String plainResponseBody = null;

        RequestBody jsonBody = jsonString != null ? RequestBody.create(JSON, jsonString) : null;
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .method(request_method, jsonBody);
        if (headerArgs != null) {
            for (Pair<String, String> keyValPair : headerArgs) {
                requestBuilder.addHeader(keyValPair.first, keyValPair.second);
            }
        }
        //TODO: move to getHeaderArgs()?
        String locale = Locale.getDefault().getLanguage() + Locale.getDefault().getCountry();
        requestBuilder.addHeader("Accept-Language", locale);
        Request request = requestBuilder.build();

        try {
            response = okHttpClient.newCall(request).execute();

            InputStream inputStream = response.body().byteStream();
            Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
            if (scanner.hasNext()) {
                plainResponseBody = scanner.next();
            }

        } catch (NullPointerException npe) {
            plainResponseBody = formatErrorMessage(error_json_exception_user_message);
        } catch (UnknownHostException e) {
            plainResponseBody = formatErrorMessage(server_unreachable_message);
        } catch (MalformedURLException e) {
            plainResponseBody = formatErrorMessage(malformed_url);
        } catch (SocketTimeoutException e) {
            plainResponseBody = formatErrorMessage(server_unreachable_message);
        } catch (SSLHandshakeException e) {
            plainResponseBody = formatErrorMessage(certificate_error);
        } catch (ConnectException e) {
            plainResponseBody = formatErrorMessage(service_is_down_error);
        } catch (IllegalArgumentException e) {
            plainResponseBody = formatErrorMessage(error_no_such_algorithm_exception_user_message);
        } catch (UnknownServiceException e) {
            //unable to find acceptable protocols - tlsv1.2 not enabled?
            plainResponseBody = formatErrorMessage(error_no_such_algorithm_exception_user_message);
        } catch (IOException e) {
            plainResponseBody = formatErrorMessage(error_io_exception_user_message);
        }

        return plainResponseBody;
    }

    /**
     * Downloads a provider.json from a given URL, adding a new provider using the given name.
     *
     * @param task containing a boolean meaning if the provider is custom or not, another boolean meaning if the user completely trusts this provider, the provider name and its provider.json url.
     * @return a bundle with a boolean value mapped to a key named RESULT_KEY, and which is true if the update was successful.
     */
    private Bundle setUpProvider(Bundle task) {
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

    public static boolean caCertDownloaded() {
        return CA_CERT_DOWNLOADED;
    }

    private boolean validCertificate(String cert_string) {
        boolean result = false;
        if (!ConfigHelper.checkErroneousDownload(cert_string)) {
            X509Certificate certificate = ConfigHelper.parseX509CertificateFromString(cert_string);
            try {
                if (certificate != null) {
                    JSONObject provider_json = new JSONObject(preferences.getString(Provider.KEY, ""));
                    String fingerprint = provider_json.getString(Provider.CA_CERT_FINGERPRINT);
                    String encoding = fingerprint.split(":")[0];
                    String expected_fingerprint = fingerprint.split(":")[1];
                    String real_fingerprint = base64toHex(Base64.encodeToString(
                            MessageDigest.getInstance(encoding).digest(certificate.getEncoded()),
                            Base64.DEFAULT));

                    result = real_fingerprint.trim().equalsIgnoreCase(expected_fingerprint.trim());
                } else
                    result = false;
            } catch (JSONException e) {
                result = false;
            } catch (NoSuchAlgorithmException e) {
                result = false;
            } catch (CertificateEncodingException e) {
                result = false;
            }
        }

        return result;
    }

    private String base64toHex(String base64_input) {
        byte[] byteArray = Base64.decode(base64_input, Base64.DEFAULT);
        int readBytes = byteArray.length;
        StringBuffer hexData = new StringBuffer();
        int onebyte;
        for (int i = 0; i < readBytes; i++) {
            onebyte = ((0x000000ff & byteArray[i]) | 0xffffff00);
            hexData.append(Integer.toHexString(onebyte).substring(6));
        }
        return hexData.toString();
    }

    private Bundle getAndSetProviderJson(String provider_main_url, boolean danger_on, String provider_ca_cert_fingerprint) {
        Bundle result = new Bundle();

        if (go_ahead) {
            String provider_dot_json_string;
            if(provider_ca_cert_fingerprint.isEmpty())
                provider_dot_json_string = downloadWithCommercialCA(provider_main_url + "/provider.json", danger_on);
            else
                provider_dot_json_string = downloadWithCommercialCA(provider_main_url + "/provider.json", danger_on, provider_ca_cert_fingerprint);

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

    private Bundle getAndSetEipServiceJson() {
        Bundle result = new Bundle();
        String eip_service_json_string = "";
        if (go_ahead) {
            try {
                JSONObject provider_json = new JSONObject(preferences.getString(Provider.KEY, ""));
                String eip_service_url = provider_json.getString(Provider.API_URL) + "/" + provider_json.getString(Provider.API_VERSION) + "/" + EIP.SERVICE_API_PATH;
                eip_service_json_string = downloadWithProviderCA(eip_service_url, true);
                JSONObject eip_service_json = new JSONObject(eip_service_json_string);
                eip_service_json.getInt(Provider.API_RETURN_SERIAL);

                preferences.edit().putString(Constants.KEY, eip_service_json.toString()).commit();

                result.putBoolean(RESULT_KEY, true);
            } catch (JSONException e) {
                String reason_to_fail = pickErrorMessage(eip_service_json_string);
                result.putString(ERRORS, reason_to_fail);
                result.putBoolean(RESULT_KEY, false);
            }
        }
        return result;
    }

    /**
     * Interprets the error message as a JSON object and extract the "errors" keyword pair.
     * If the error message is not a JSON object, then it is returned untouched.
     *
     * @param string_json_error_message
     * @return final error message
     */
    private String pickErrorMessage(String string_json_error_message) {
        String error_message = "";
        try {
            JSONObject json_error_message = new JSONObject(string_json_error_message);
            error_message = json_error_message.getString(ERRORS);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            error_message = string_json_error_message;
        }

        return error_message;
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
                connection.addRequestProperty(LeapSRPSession.AUTHORIZATION_HEADER, "Token token = " + LeapSRPSession.getToken());
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

        if (responseString.contains(ERRORS)) {
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

    @NonNull
    private List<Pair<String, String>> getAuthorizationHeader() {
        List<Pair<String, String>> headerArgs = new ArrayList<>();
        if (!LeapSRPSession.getToken().isEmpty()) {
            Pair<String, String> authorizationHeaderPair = new Pair<>(LeapSRPSession.AUTHORIZATION_HEADER, "Token token=" + LeapSRPSession.getToken());
            headerArgs.add(authorizationHeaderPair);
        }
        return headerArgs;
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

    /**
     * Logs out from the api url retrieved from the task.
     *
     * @return true if there were no exceptions
     */
    private boolean logOut() {

        OkHttpClient okHttpClient = initSelfSignedCAHttpClient(new JSONObject());
        if (okHttpClient == null) {
            return false;
        }

        String deleteUrl = provider_api_url + "/logout";
        int progress = 0;

        Request.Builder requestBuilder = new Request.Builder()
                .url(deleteUrl)
                .delete();
        Request request = requestBuilder.build();

        try {
            Response response = okHttpClient.newCall(request).execute();
                                            // v---- was already not authorized
            if (response.isSuccessful() || response.code() == 401) {
                broadcastProgress(progress++);
                LeapSRPSession.setToken("");
            }

        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
     * Downloads a new OpenVPN certificate, attaching authenticated cookie for authenticated certificate.
     *
     * @return true if certificate was downloaded correctly, false if provider.json or danger_on flag are not present in SharedPreferences, or if the certificate url could not be parsed as a URI, or if there was an SSL error.
     */
    private boolean updateVpnCertificate() {
        try {
            JSONObject provider_json = new JSONObject(preferences.getString(Provider.KEY, ""));

            String provider_main_url = provider_json.getString(Provider.API_URL);
            URL new_cert_string_url = new URL(provider_main_url + "/" + provider_json.getString(Provider.API_VERSION) + "/" + Constants.VPN_CERTIFICATE);

            boolean danger_on = preferences.getBoolean(ProviderItem.DANGER_ON, false);

            String cert_string = downloadWithProviderCA(new_cert_string_url.toString(), danger_on);

            if (cert_string.isEmpty() || ConfigHelper.checkErroneousDownload(cert_string))
                return false;
            else
                return loadCertificate(cert_string);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }

    private boolean loadCertificate(String cert_string) {
        try {
            // API returns concatenated cert & key.  Split them for OpenVPN options
            String certificateString = null, keyString = null;
            String[] certAndKey = cert_string.split("(?<=-\n)");
            for (int i = 0; i < certAndKey.length - 1; i++) {
                if (certAndKey[i].contains("KEY")) {
                    keyString = certAndKey[i++] + certAndKey[i];
                } else if (certAndKey[i].contains("CERTIFICATE")) {
                    certificateString = certAndKey[i++] + certAndKey[i];
                }
            }
            RSAPrivateKey keyCert = ConfigHelper.parseRsaKeyFromString(keyString);
            keyString = Base64.encodeToString(keyCert.getEncoded(), Base64.DEFAULT);
            preferences.edit().putString(Constants.PRIVATE_KEY, "-----BEGIN RSA PRIVATE KEY-----\n" + keyString + "-----END RSA PRIVATE KEY-----").commit();

            X509Certificate certCert = ConfigHelper.parseX509CertificateFromString(certificateString);
            certificateString = Base64.encodeToString(certCert.getEncoded(), Base64.DEFAULT);

            preferences.edit().putString(Constants.VPN_CERTIFICATE, "-----BEGIN CERTIFICATE-----\n" + certificateString + "-----END CERTIFICATE-----").commit();

            return true;
        } catch (CertificateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }
}
