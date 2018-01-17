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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.UnknownServiceException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLHandshakeException;

import okhttp3.OkHttpClient;
import se.leap.bitmaskclient.userstatus.SessionDialog;
import se.leap.bitmaskclient.userstatus.User;
import se.leap.bitmaskclient.userstatus.UserStatus;

import static se.leap.bitmaskclient.ConfigHelper.getFingerprintFromCertificate;
import static se.leap.bitmaskclient.Constants.PROVIDER_ALLOWED_REGISTERED;
import static se.leap.bitmaskclient.Constants.PROVIDER_ALLOW_ANONYMOUS;
import static se.leap.bitmaskclient.Constants.PROVIDER_PRIVATE_KEY;
import static se.leap.bitmaskclient.Constants.PROVIDER_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.DownloadFailedDialog.DOWNLOAD_ERRORS.ERROR_CERTIFICATE_PINNING;
import static se.leap.bitmaskclient.DownloadFailedDialog.DOWNLOAD_ERRORS.ERROR_CORRUPTED_PROVIDER_JSON;
import static se.leap.bitmaskclient.DownloadFailedDialog.DOWNLOAD_ERRORS.ERROR_INVALID_CERTIFICATE;
import static se.leap.bitmaskclient.Provider.MAIN_URL;
import static se.leap.bitmaskclient.ProviderAPI.CORRECTLY_DOWNLOADED_CERTIFICATE;
import static se.leap.bitmaskclient.ProviderAPI.CORRECTLY_DOWNLOADED_EIP_SERVICE;
import static se.leap.bitmaskclient.ProviderAPI.CURRENT_PROGRESS;
import static se.leap.bitmaskclient.ProviderAPI.DOWNLOAD_CERTIFICATE;
import static se.leap.bitmaskclient.ProviderAPI.DOWNLOAD_EIP_SERVICE;
import static se.leap.bitmaskclient.ProviderAPI.ERRORID;
import static se.leap.bitmaskclient.ProviderAPI.ERRORS;
import static se.leap.bitmaskclient.ProviderAPI.FAILED_LOGIN;
import static se.leap.bitmaskclient.ProviderAPI.FAILED_SIGNUP;
import static se.leap.bitmaskclient.ProviderAPI.INCORRECTLY_DOWNLOADED_CERTIFICATE;
import static se.leap.bitmaskclient.ProviderAPI.INCORRECTLY_DOWNLOADED_EIP_SERVICE;
import static se.leap.bitmaskclient.ProviderAPI.PROVIDER_API_EVENT;
import static se.leap.bitmaskclient.ProviderAPI.PROVIDER_SET_UP;
import static se.leap.bitmaskclient.ProviderAPI.LOGOUT_FAILED;
import static se.leap.bitmaskclient.ProviderAPI.LOG_IN;
import static se.leap.bitmaskclient.ProviderAPI.LOG_OUT;
import static se.leap.bitmaskclient.ProviderAPI.PARAMETERS;
import static se.leap.bitmaskclient.ProviderAPI.PROVIDER_NOK;
import static se.leap.bitmaskclient.ProviderAPI.PROVIDER_OK;
import static se.leap.bitmaskclient.ProviderAPI.RECEIVER_KEY;
import static se.leap.bitmaskclient.ProviderAPI.RESULT_CODE;
import static se.leap.bitmaskclient.ProviderAPI.RESULT_KEY;
import static se.leap.bitmaskclient.ProviderAPI.SET_UP_PROVIDER;
import static se.leap.bitmaskclient.ProviderAPI.SIGN_UP;
import static se.leap.bitmaskclient.ProviderAPI.SUCCESSFUL_LOGIN;
import static se.leap.bitmaskclient.ProviderAPI.SUCCESSFUL_LOGOUT;
import static se.leap.bitmaskclient.ProviderAPI.SUCCESSFUL_SIGNUP;
import static se.leap.bitmaskclient.ProviderAPI.UPDATE_PROGRESSBAR;
import static se.leap.bitmaskclient.ProviderAPI.UPDATE_PROVIDER_DETAILS;
import static se.leap.bitmaskclient.R.string.certificate_error;
import static se.leap.bitmaskclient.R.string.error_io_exception_user_message;
import static se.leap.bitmaskclient.R.string.error_json_exception_user_message;
import static se.leap.bitmaskclient.R.string.error_no_such_algorithm_exception_user_message;
import static se.leap.bitmaskclient.R.string.malformed_url;
import static se.leap.bitmaskclient.R.string.routes_info_excl;
import static se.leap.bitmaskclient.R.string.server_unreachable_message;
import static se.leap.bitmaskclient.R.string.service_is_down_error;
import static se.leap.bitmaskclient.R.string.warning_corrupted_provider_cert;
import static se.leap.bitmaskclient.R.string.warning_corrupted_provider_details;
import static se.leap.bitmaskclient.R.string.warning_expired_provider_cert;

/**
 * Implements the logic of the http api calls. The methods of this class needs to be called from
 * a background thread.
 */

public abstract class ProviderApiManagerBase {

    public interface ProviderApiServiceCallback {
        void broadcastEvent(Intent intent);
    }

    private ProviderApiServiceCallback serviceCallback;

    protected static volatile boolean
            CA_CERT_DOWNLOADED = false,
            PROVIDER_JSON_DOWNLOADED = false,
            EIP_SERVICE_JSON_DOWNLOADED = false;

    protected static String lastProviderMainUrl;
    protected static boolean go_ahead = true;
    protected static SharedPreferences preferences;
    protected static String providerApiUrl;
    protected static String providerCaCertFingerprint;
    protected static String providerCaCert;
    protected static JSONObject providerDefinition;
    protected Resources resources;
    protected OkHttpClientGenerator clientGenerator;

    public static void stop() {
        go_ahead = false;
    }

    public static String lastProviderMainUrl() {
        return lastProviderMainUrl;
    }

    public ProviderApiManagerBase(SharedPreferences preferences, Resources resources, OkHttpClientGenerator clientGenerator, ProviderApiServiceCallback callback) {
        this.preferences = preferences;
        this.resources = resources;
        this.serviceCallback = callback;
        this.clientGenerator = clientGenerator;
    }

    public void handleIntent(Intent command) {
        final ResultReceiver receiver = command.getParcelableExtra(RECEIVER_KEY);
        String action = command.getAction();
        Bundle parameters = command.getBundleExtra(PARAMETERS);

        if (providerApiUrl == null && preferences.contains(Provider.KEY)) {
            try {
                JSONObject provider_json = new JSONObject(preferences.getString(Provider.KEY, ""));
                providerApiUrl = provider_json.getString(Provider.API_URL) + "/" + provider_json.getString(Provider.API_VERSION);
                go_ahead = true;
            } catch (JSONException e) {
                go_ahead = false;
            }
        }

        if (action.equals(UPDATE_PROVIDER_DETAILS)) {
            resetProviderDetails();
            Bundle task = new Bundle();
            task.putString(MAIN_URL, lastProviderMainUrl);
            Bundle result = setUpProvider(task);
            if (result.getBoolean(RESULT_KEY)) {
                receiver.send(PROVIDER_OK, result);
            } else {
                receiver.send(PROVIDER_NOK, result);
            }
        } else if (action.equalsIgnoreCase(SET_UP_PROVIDER)) {
            Bundle result = setUpProvider(parameters);
            if (go_ahead) {
                if (result.getBoolean(RESULT_KEY)) {
                    sendToReceiverOrBroadcast(receiver, PROVIDER_OK, result);
                } else {
                    sendToReceiverOrBroadcast(receiver, PROVIDER_NOK, result);
                }
            }
        } else if (action.equalsIgnoreCase(SIGN_UP)) {
            UserStatus.updateStatus(UserStatus.SessionStatus.SIGNING_UP, resources);
            Bundle result = tryToRegister(parameters);
            if (result.getBoolean(RESULT_KEY)) {
                sendToReceiverOrBroadcast(receiver, SUCCESSFUL_SIGNUP, result);
            } else {
                sendToReceiverOrBroadcast(receiver, FAILED_SIGNUP, result);
            }
        } else if (action.equalsIgnoreCase(LOG_IN)) {
            UserStatus.updateStatus(UserStatus.SessionStatus.LOGGING_IN, resources);
            Bundle result = tryToAuthenticate(parameters);
            if (result.getBoolean(RESULT_KEY)) {
                sendToReceiverOrBroadcast(receiver, SUCCESSFUL_LOGIN, result);
                UserStatus.updateStatus(UserStatus.SessionStatus.LOGGED_IN, resources);
            } else {
                sendToReceiverOrBroadcast(receiver, FAILED_LOGIN, result);
                UserStatus.updateStatus(UserStatus.SessionStatus.NOT_LOGGED_IN, resources);
            }
        } else if (action.equalsIgnoreCase(LOG_OUT)) {
            UserStatus.updateStatus(UserStatus.SessionStatus.LOGGING_OUT, resources);
            if (logOut()) {
                sendToReceiverOrBroadcast(receiver, SUCCESSFUL_LOGOUT, Bundle.EMPTY);
                UserStatus.updateStatus(UserStatus.SessionStatus.LOGGED_OUT, resources);
            } else {
                sendToReceiverOrBroadcast(receiver, LOGOUT_FAILED, Bundle.EMPTY);
                UserStatus.updateStatus(UserStatus.SessionStatus.DIDNT_LOG_OUT, resources);
            }
        } else if (action.equalsIgnoreCase(DOWNLOAD_CERTIFICATE)) {
            if (updateVpnCertificate()) {
                sendToReceiverOrBroadcast(receiver, CORRECTLY_DOWNLOADED_CERTIFICATE, Bundle.EMPTY);
            } else {
                sendToReceiverOrBroadcast(receiver, INCORRECTLY_DOWNLOADED_CERTIFICATE, Bundle.EMPTY);
            }
        } else if (action.equalsIgnoreCase(DOWNLOAD_EIP_SERVICE)) {
            Bundle result = getAndSetEipServiceJson();
            if (result.getBoolean(RESULT_KEY)) {
                sendToReceiverOrBroadcast(receiver, CORRECTLY_DOWNLOADED_EIP_SERVICE, result);
            } else {
                sendToReceiverOrBroadcast(receiver, INCORRECTLY_DOWNLOADED_EIP_SERVICE, result);
            }
        } else if (action.equalsIgnoreCase(PROVIDER_SET_UP)) {
            if(EIP_SERVICE_JSON_DOWNLOADED && CA_CERT_DOWNLOADED && PROVIDER_JSON_DOWNLOADED ) {
                if(receiver!= null) {
                    receiver.send(PROVIDER_OK, Bundle.EMPTY);
                }
            }
        }
    }

    protected void resetProviderDetails() {
        CA_CERT_DOWNLOADED = PROVIDER_JSON_DOWNLOADED = false;
        deleteProviderDetailsFromPreferences(providerDefinition);
        providerCaCert = "";
        providerDefinition = new JSONObject();
    }

    protected String formatErrorMessage(final int toastStringId) {
        return formatErrorMessage(resources.getString(toastStringId));
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

    protected void addErrorMessageToJson(JSONObject jsonObject, String errorMessage) {
        try {
            jsonObject.put(ERRORS, errorMessage);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void addErrorMessageToJson(JSONObject jsonObject, String errorMessage, String errorId) {
        try {
            jsonObject.put(ERRORS, errorMessage);
            jsonObject.put(ERRORID, errorId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
        OkHttpClient okHttpClient = clientGenerator.initSelfSignedCAHttpClient(stepResult);
        if (okHttpClient == null) {
            return authFailedNotification(stepResult, username);
        }

        LeapSRPSession client = new LeapSRPSession(username, password);
        byte[] salt = client.calculateNewSalt();

        BigInteger password_verifier = client.calculateV(username, password, salt);

        JSONObject api_result = sendNewUserDataToSRPServer(providerApiUrl, username, new BigInteger(1, salt).toString(16), password_verifier.toString(16), okHttpClient);

        Bundle result = new Bundle();
        if (api_result.has(ERRORS))
            result = authFailedNotification(api_result, username);
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
        OkHttpClient okHttpClient = clientGenerator.initSelfSignedCAHttpClient(stepResult);
        if (okHttpClient == null) {
            return authFailedNotification(stepResult, username);
        }

        LeapSRPSession client = new LeapSRPSession(username, password);
        byte[] A = client.exponential();

        JSONObject step_result = sendAToSRPServer(providerApiUrl, username, new BigInteger(1, A).toString(16), okHttpClient);
        try {
            String salt = step_result.getString(LeapSRPSession.SALT);
            byte[] Bbytes = new BigInteger(step_result.getString("B"), 16).toByteArray();
            byte[] M1 = client.response(new BigInteger(salt, 16).toByteArray(), Bbytes);
            if (M1 != null) {
                step_result = sendM1ToSRPServer(providerApiUrl, username, M1, okHttpClient);
                setTokenIfAvailable(step_result);
                byte[] M2 = new BigInteger(step_result.getString(LeapSRPSession.M2), 16).toByteArray();
                if (client.verify(M2)) {
                    result.putBoolean(RESULT_KEY, true);
                } else {
                    authFailedNotification(step_result, username);
                }
            } else {
                result.putBoolean(RESULT_KEY, false);
                result.putString(SessionDialog.USERNAME, username);
                result.putString(resources.getString(R.string.user_message), resources.getString(R.string.error_srp_math_error_user_message));
            }
        } catch (JSONException e) {
            result = authFailedNotification(step_result, username);
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
                    userNotificationBundle.putString(resources.getString(R.string.user_message), message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (baseErrorMessage instanceof String) {
                try {
                    String errorMessage = result.getString(ERRORS);
                    userNotificationBundle.putString(resources.getString(R.string.user_message), errorMessage);
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

    void sendToReceiverOrBroadcast(ResultReceiver receiver, int resultCode, Bundle resultData) {
        if (receiver != null) {
            receiver.send(resultCode, resultData);
        } else {
            broadcastEvent(PROVIDER_API_EVENT, resultCode, resultData);
        }
    }

    /**
     * Sets up an intent with the progress value passed as a parameter
     * and sends it as a broadcast.
     *
     * @param progress
     */
    void broadcastProgress(int progress) {
        Intent intentUpdate = new Intent(UPDATE_PROGRESSBAR);
        intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
        intentUpdate.putExtra(CURRENT_PROGRESS, progress);
        serviceCallback.broadcastEvent(intentUpdate);
    }

    void broadcastEvent(String action, int resultCode , Bundle resultData) {
        Intent intentUpdate = new Intent(action);
        intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
        intentUpdate.putExtra(RESULT_CODE, resultCode);
        intentUpdate.putExtra(RESULT_KEY, resultData);
        serviceCallback.broadcastEvent(intentUpdate);
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
     * @param okHttpClient
     * @return response from authentication server
     */
    private JSONObject sendNewUserDataToSRPServer(String server_url, String username, String salt, String password_verifier, OkHttpClient okHttpClient) {
        return sendToServer(server_url + "/users.json", "POST", new SrpRegistrationData(username, salt, password_verifier).toString(), okHttpClient);
    }

    /**
     * Executes an HTTP request expecting a JSON response.
     *
     * @param url
     * @param request_method
     * @return response from authentication server
     */
    private JSONObject sendToServer(String url, String request_method, String jsonString, OkHttpClient okHttpClient) {
        return requestJsonFromServer(url, request_method, jsonString, new ArrayList<Pair<String, String>>(), okHttpClient);
    }

    protected String sendGetStringToServer(@NonNull String url, @NonNull List<Pair<String, String>> headerArgs, @NonNull OkHttpClient okHttpClient) {
        return requestStringFromServer(url, "GET", null, headerArgs, okHttpClient);
    }



    private JSONObject requestJsonFromServer(@NonNull String url, @NonNull String request_method, String jsonString, @NonNull List<Pair<String, String>> headerArgs, @NonNull OkHttpClient okHttpClient)  {
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

    private String requestStringFromServer(@NonNull String url, @NonNull String request_method, String jsonString, @NonNull List<Pair<String, String>> headerArgs, @NonNull OkHttpClient okHttpClient) {
        String plainResponseBody = null;

        try {

            plainResponseBody = ProviderApiConnector.requestStringFromServer(url, request_method, jsonString, headerArgs, okHttpClient);

        } catch (NullPointerException npe) {
            plainResponseBody = formatErrorMessage(error_json_exception_user_message);
        } catch (UnknownHostException | SocketTimeoutException e) {
            plainResponseBody = formatErrorMessage(server_unreachable_message);
        } catch (MalformedURLException e) {
            plainResponseBody = formatErrorMessage(malformed_url);
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

    private boolean canConnect(String caCert, JSONObject providerDefinition, Bundle result) {
        JSONObject errorJson = new JSONObject();
        String baseUrl = getApiUrl(providerDefinition);

        OkHttpClient okHttpClient = clientGenerator.initSelfSignedCAHttpClient(errorJson, caCert);
        if (okHttpClient == null) {
            result.putString(ERRORS, errorJson.toString());
            return false;
        }

        try {

            return ProviderApiConnector.canConnect(okHttpClient, baseUrl);

        }  catch (UnknownHostException | SocketTimeoutException e) {
            setErrorResult(result, server_unreachable_message, null);
        } catch (MalformedURLException e) {
            setErrorResult(result, malformed_url, null);
        } catch (SSLHandshakeException e) {
            setErrorResult(result, warning_corrupted_provider_cert, ERROR_INVALID_CERTIFICATE.toString());
        } catch (ConnectException e) {
            setErrorResult(result, service_is_down_error, null);
        } catch (IllegalArgumentException e) {
            setErrorResult(result, error_no_such_algorithm_exception_user_message, null);
        } catch (UnknownServiceException e) {
            //unable to find acceptable protocols - tlsv1.2 not enabled?
            setErrorResult(result, error_no_such_algorithm_exception_user_message, null);
        } catch (IOException e) {
            setErrorResult(result, error_io_exception_user_message, null);
        }
        return false;
    }

    /**
     * Downloads a provider.json from a given URL, adding a new provider using the given name.
     *
     * @param task containing a boolean meaning if the provider is custom or not, another boolean meaning if the user completely trusts this provider, the provider name and its provider.json url.
     * @return a bundle with a boolean value mapped to a key named RESULT_KEY, and which is true if the update was successful.
     */
    protected abstract Bundle setUpProvider(Bundle task);

    /**
     * Downloads the eip-service.json from a given URL, and saves eip service capabilities including the offered gateways
     * @return a bundle with a boolean value mapped to a key named RESULT_KEY, and which is true if the download was successful.
     */
    protected abstract Bundle getAndSetEipServiceJson();

    /**
     * Downloads a new OpenVPN certificate, attaching authenticated cookie for authenticated certificate.
     *
     * @return true if certificate was downloaded correctly, false if provider.json is not present in SharedPreferences, or if the certificate url could not be parsed as a URI, or if there was an SSL error.
     */
    protected abstract boolean updateVpnCertificate();


    protected static boolean caCertDownloaded() {
        return CA_CERT_DOWNLOADED;
    }

    protected boolean isValidJson(String jsonString) {
        try {
            new JSONObject(jsonString);
            return true;
        } catch(JSONException e) {
            return false;
        } catch(NullPointerException e) {
            e.printStackTrace();
            return false;
        }
    }

    protected boolean validCertificate(String cert_string) {
        boolean result = false;
        if (!ConfigHelper.checkErroneousDownload(cert_string)) {
            X509Certificate certificate = ConfigHelper.parseX509CertificateFromString(cert_string);
            try {
                if (certificate != null) {
                    JSONObject provider_json = new JSONObject(preferences.getString(Provider.KEY, ""));
                    String fingerprint = provider_json.getString(Provider.CA_CERT_FINGERPRINT);
                    String encoding = fingerprint.split(":")[0];
                    String expected_fingerprint = fingerprint.split(":")[1];
                    String real_fingerprint = getFingerprintFromCertificate(certificate, encoding);

                    result = real_fingerprint.trim().equalsIgnoreCase(expected_fingerprint.trim());
                } else
                    result = false;
            } catch (JSONException | NoSuchAlgorithmException | CertificateEncodingException e) {
                result = false;
            }
        }

        return result;
    }

    protected void checkPersistedProviderUpdates() {
        String providerDomain = getDomainFromMainURL(lastProviderMainUrl);
        if (hasUpdatedProviderDetails(providerDomain)) {
            providerCaCert = getPersistedProviderCA(providerDomain);
            providerDefinition = getPersistedProviderDefinition(providerDomain);
            providerCaCertFingerprint = getPersistedCaCertFingerprint(providerDomain);
            providerApiUrl = getApiUrlWithVersion(providerDefinition);
        }
    }

    protected Bundle validateProviderDetails() {
        Bundle result = validateCertificateForProvider(providerCaCert, providerDefinition, lastProviderMainUrl);

        //invalid certificate or no certificate
        if (result.containsKey(ERRORS) || (result.containsKey(RESULT_KEY) && !result.getBoolean(RESULT_KEY)) ) {
            return result;
        }

        //valid certificate: skip download, save loaded provider CA cert and provider definition directly
        try {
            preferences.edit().putString(Provider.KEY, providerDefinition.toString()).
                    putBoolean(PROVIDER_ALLOW_ANONYMOUS, providerDefinition.getJSONObject(Provider.SERVICE).getBoolean(PROVIDER_ALLOW_ANONYMOUS)).
                    putBoolean(PROVIDER_ALLOWED_REGISTERED, providerDefinition.getJSONObject(Provider.SERVICE).getBoolean(PROVIDER_ALLOWED_REGISTERED)).
                    putString(Provider.CA_CERT, providerCaCert).commit();
            CA_CERT_DOWNLOADED = true;
            PROVIDER_JSON_DOWNLOADED = true;
            result.putBoolean(RESULT_KEY, true);
        } catch (JSONException e) {
            e.printStackTrace();
            setErrorResult(result, warning_corrupted_provider_details, ERROR_CORRUPTED_PROVIDER_JSON.toString());
        }

        return result;
    }

    protected Bundle validateCertificateForProvider(String cert_string, JSONObject providerDefinition, String mainUrl) {
        Bundle result = new Bundle();
        result.putBoolean(RESULT_KEY, false);

        if (ConfigHelper.checkErroneousDownload(cert_string)) {
            return result;
        }

        X509Certificate certificate = ConfigHelper.parseX509CertificateFromString(cert_string);
        if (certificate == null) {
            return setErrorResult(result, warning_corrupted_provider_cert, ERROR_INVALID_CERTIFICATE.toString());
        }
        try {
            certificate.checkValidity();
            String fingerprint = getCaCertFingerprint(providerDefinition);
            String encoding = fingerprint.split(":")[0];
            String expected_fingerprint = fingerprint.split(":")[1];
            String real_fingerprint = getFingerprintFromCertificate(certificate, encoding);
            if (!real_fingerprint.trim().equalsIgnoreCase(expected_fingerprint.trim())) {
                return setErrorResult(result, warning_corrupted_provider_cert, ERROR_CERTIFICATE_PINNING.toString());
            }


            if (!hasApiUrlExpectedDomain(providerDefinition, mainUrl)){
                return setErrorResult(result, warning_corrupted_provider_details, ERROR_CORRUPTED_PROVIDER_JSON.toString());
            }

            if (!canConnect(cert_string, providerDefinition, result)) {
                return result;
            }
        } catch (NoSuchAlgorithmException e ) {
            return setErrorResult(result, error_no_such_algorithm_exception_user_message, null);
        } catch (ArrayIndexOutOfBoundsException e) {
            return setErrorResult(result, warning_corrupted_provider_details, ERROR_CORRUPTED_PROVIDER_JSON.toString());
        } catch (CertificateEncodingException | CertificateNotYetValidException | CertificateExpiredException e) {
            return setErrorResult(result, warning_expired_provider_cert, ERROR_INVALID_CERTIFICATE.toString());
        }

        result.putBoolean(RESULT_KEY, true);
        return result;
    }

    protected Bundle setErrorResult(Bundle result, int errorMessageId, String errorId) {
        JSONObject errorJson = new JSONObject();
        if (errorId != null) {
            addErrorMessageToJson(errorJson, resources.getString(errorMessageId), errorId);
        } else {
            addErrorMessageToJson(errorJson, resources.getString(errorMessageId));
        }
        result.putString(ERRORS, errorJson.toString());
        result.putBoolean(RESULT_KEY, false);
        return result;
    }

    /**
     * This method aims to prevent attacks where the provider.json file got manipulated by a third party.
     * The main url is visible to the provider when setting up a new provider.
     * The user is responsible to check that this is the provider main url he intends to connect to.
     *
     * @param providerDefinition
     * @param mainUrlString
     * @return
     */
    private boolean hasApiUrlExpectedDomain(JSONObject providerDefinition, String mainUrlString) {
        //  fix against "api_uri": "https://calyx.net.malicious.url.net:4430",
        String apiUrlString = getApiUrl(providerDefinition);
        String providerDomain = getProviderDomain(providerDefinition);
        if (mainUrlString.contains(providerDomain) && apiUrlString.contains(providerDomain  + ":")) {
            return true;
        }
        return false;
    }

    protected String getCaCertFingerprint(JSONObject providerDefinition) {
        try {
            return providerDefinition.getString(Provider.CA_CERT_FINGERPRINT);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    protected String getApiUrl(JSONObject providerDefinition) {
        try {
            return providerDefinition.getString(Provider.API_URL);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    protected String getApiUrlWithVersion(JSONObject providerDefinition) {
        try {
            return providerDefinition.getString(Provider.API_URL) + "/" + providerDefinition.getString(Provider.API_VERSION);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    protected void deleteProviderDetailsFromPreferences(JSONObject providerDefinition) {
        String providerDomain = getProviderDomain(providerDefinition);

        if (preferences.contains(Provider.KEY + "." + providerDomain)) {
            preferences.edit().remove(Provider.KEY + "." + providerDomain).apply();
        }
        if (preferences.contains(Provider.CA_CERT + "." + providerDomain)) {
            preferences.edit().remove(Provider.CA_CERT + "." + providerDomain).apply();
        }
        if (preferences.contains(Provider.CA_CERT_FINGERPRINT + "." + providerDomain)) {
            preferences.edit().remove(Provider.CA_CERT_FINGERPRINT + "." + providerDomain).apply();
        }
    }

    protected String getPersistedCaCertFingerprint(String providerDomain) {
        try {
            return getPersistedProviderDefinition(providerDomain).getString(Provider.CA_CERT_FINGERPRINT);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    protected JSONObject getPersistedProviderDefinition(String providerDomain) {
        try {
            return new JSONObject(preferences.getString(Provider.KEY + "." + providerDomain, ""));
        } catch (JSONException e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

    protected String getPersistedProviderCA(String providerDomain) {
        return preferences.getString(Provider.CA_CERT + "." + providerDomain, "");
    }

    protected String getProviderDomain(JSONObject providerDefinition) {
        try {
            return providerDefinition.getString(Provider.DOMAIN);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return "";
    }

    protected boolean hasUpdatedProviderDetails(String domain) {
        return preferences.contains(Provider.KEY + "." + domain) && preferences.contains(Provider.CA_CERT + "." + domain);
    }

    protected String getDomainFromMainURL(String mainUrl) {
        return mainUrl.replaceFirst("http[s]?://", "").replaceFirst("/.*", "");

    }

    /**
     * Interprets the error message as a JSON object and extract the "errors" keyword pair.
     * If the error message is not a JSON object, then it is returned untouched.
     *
     * @param string_json_error_message
     * @return final error message
     */
    protected String pickErrorMessage(String string_json_error_message) {
        String error_message = "";
        try {
            JSONObject json_error_message = new JSONObject(string_json_error_message);
            error_message = json_error_message.getString(ERRORS);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            error_message = string_json_error_message;
        } catch (NullPointerException e) {
            //do nothing
        }

        return error_message;
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

    private boolean logOut() {
        OkHttpClient okHttpClient = clientGenerator.initSelfSignedCAHttpClient(new JSONObject());
        if (okHttpClient == null) {
            return false;
        }

        String deleteUrl = providerApiUrl + "/logout";
        int progress = 0;

        if (ProviderApiConnector.delete(okHttpClient, deleteUrl)) {
            broadcastProgress(progress++);
            LeapSRPSession.setToken("");
            return true;
        }
        return false;
    }

    //FIXME: don't save private keys in shared preferences! use the keystore
    protected boolean loadCertificate(String cert_string) {
        if (cert_string == null) {
            return false;
        }

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

            RSAPrivateKey key = ConfigHelper.parseRsaKeyFromString(keyString);
            keyString = Base64.encodeToString(key.getEncoded(), Base64.DEFAULT);
            preferences.edit().putString(PROVIDER_PRIVATE_KEY, "-----BEGIN RSA PRIVATE KEY-----\n" + keyString + "-----END RSA PRIVATE KEY-----").commit();

            X509Certificate certificate = ConfigHelper.parseX509CertificateFromString(certificateString);
            certificateString = Base64.encodeToString(certificate.getEncoded(), Base64.DEFAULT);
            preferences.edit().putString(PROVIDER_VPN_CERTIFICATE, "-----BEGIN CERTIFICATE-----\n" + certificateString + "-----END CERTIFICATE-----").commit();
            return true;
        } catch (CertificateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }
}
