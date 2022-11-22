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

import static se.leap.bitmaskclient.R.string.certificate_error;
import static se.leap.bitmaskclient.R.string.error_io_exception_user_message;
import static se.leap.bitmaskclient.R.string.error_json_exception_user_message;
import static se.leap.bitmaskclient.R.string.error_no_such_algorithm_exception_user_message;
import static se.leap.bitmaskclient.R.string.malformed_url;
import static se.leap.bitmaskclient.R.string.server_unreachable_message;
import static se.leap.bitmaskclient.R.string.service_is_down_error;
import static se.leap.bitmaskclient.R.string.vpn_certificate_is_invalid;
import static se.leap.bitmaskclient.R.string.warning_corrupted_provider_cert;
import static se.leap.bitmaskclient.R.string.warning_corrupted_provider_details;
import static se.leap.bitmaskclient.R.string.warning_expired_provider_cert;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_PROVIDER_API_EVENT;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_CODE;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_KEY;
import static se.leap.bitmaskclient.base.models.Constants.CREDENTIALS_PASSWORD;
import static se.leap.bitmaskclient.base.models.Constants.CREDENTIALS_USERNAME;
import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_START;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_MOTD;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_MOTD_HASHES;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_MOTD_LAST_SEEN;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_MOTD_LAST_UPDATED;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_PRIVATE_KEY;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.base.models.Provider.CA_CERT;
import static se.leap.bitmaskclient.base.models.Provider.GEOIP_URL;
import static se.leap.bitmaskclient.base.models.Provider.MOTD_URL;
import static se.leap.bitmaskclient.base.models.Provider.PROVIDER_API_IP;
import static se.leap.bitmaskclient.base.models.Provider.PROVIDER_IP;
import static se.leap.bitmaskclient.base.utils.ConfigHelper.getDomainFromMainURL;
import static se.leap.bitmaskclient.base.utils.ConfigHelper.getFingerprintFromCertificate;
import static se.leap.bitmaskclient.base.utils.ConfigHelper.getProviderFormattedString;
import static se.leap.bitmaskclient.base.utils.ConfigHelper.parseRsaKeyFromString;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.deleteProviderDetailsFromPreferences;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getFromPersistedProvider;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getLongFromPersistedProvider;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getStringSetFromPersistedProvider;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.BACKEND_ERROR_KEY;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.BACKEND_ERROR_MESSAGE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.CORRECTLY_DOWNLOADED_EIP_SERVICE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.CORRECTLY_DOWNLOADED_GEOIP_JSON;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.CORRECTLY_DOWNLOADED_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.CORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.DELAY;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.DOWNLOAD_GEOIP_JSON;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.DOWNLOAD_MOTD;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.DOWNLOAD_SERVICE_JSON;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.DOWNLOAD_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.ERRORID;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.ERRORS;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.FAILED_LOGIN;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.FAILED_SIGNUP;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INCORRECTLY_DOWNLOADED_EIP_SERVICE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INCORRECTLY_DOWNLOADED_GEOIP_JSON;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INCORRECTLY_DOWNLOADED_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INCORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INITIAL_ACTION;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.LOGOUT_FAILED;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.LOG_IN;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.LOG_OUT;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.MISSING_NETWORK_CONNECTION;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.PARAMETERS;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.PROVIDER_NOK;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.PROVIDER_OK;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.QUIETLY_UPDATE_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.RECEIVER_KEY;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.SET_UP_PROVIDER;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.SIGN_UP;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.SUCCESSFUL_LOGIN;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.SUCCESSFUL_LOGOUT;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.SUCCESSFUL_SIGNUP;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.TOR_EXCEPTION;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.TOR_TIMEOUT;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.UPDATE_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.UPDATE_PROVIDER_DETAILS;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.USER_MESSAGE;
import static se.leap.bitmaskclient.providersetup.ProviderSetupFailedDialog.DOWNLOAD_ERRORS.ERROR_CERTIFICATE_PINNING;
import static se.leap.bitmaskclient.providersetup.ProviderSetupFailedDialog.DOWNLOAD_ERRORS.ERROR_CORRUPTED_PROVIDER_JSON;
import static se.leap.bitmaskclient.providersetup.ProviderSetupFailedDialog.DOWNLOAD_ERRORS.ERROR_INVALID_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderSetupFailedDialog.DOWNLOAD_ERRORS.ERROR_TOR_TIMEOUT;
import static se.leap.bitmaskclient.tor.TorStatusObservable.TorStatus.OFF;
import static se.leap.bitmaskclient.tor.TorStatusObservable.TorStatus.ON;
import static se.leap.bitmaskclient.tor.TorStatusObservable.getProxyPort;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

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
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;

import de.blinkt.openvpn.core.VpnStatus;
import okhttp3.OkHttpClient;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.models.Constants.CREDENTIAL_ERRORS;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.models.ProviderObservable;
import se.leap.bitmaskclient.base.utils.ConfigHelper;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.eip.EipStatus;
import se.leap.bitmaskclient.motd.MotdClient;
import se.leap.bitmaskclient.providersetup.connectivity.OkHttpClientGenerator;
import se.leap.bitmaskclient.providersetup.models.LeapSRPSession;
import se.leap.bitmaskclient.providersetup.models.SrpCredentials;
import se.leap.bitmaskclient.providersetup.models.SrpRegistrationData;
import se.leap.bitmaskclient.tor.TorStatusObservable;

/**
 * Implements the logic of the http api calls. The methods of this class needs to be called from
 * a background thread.
 */

public abstract class ProviderApiManagerBase {

    private final static String TAG = ProviderApiManagerBase.class.getName();

    public interface ProviderApiServiceCallback {
        void broadcastEvent(Intent intent);
        boolean startTorService() throws InterruptedException, IllegalStateException, TimeoutException;
        void stopTorService();
        int getTorHttpTunnelPort();
        boolean hasNetworkConnection();
    }

    private final ProviderApiServiceCallback serviceCallback;

    protected SharedPreferences preferences;
    protected Resources resources;
    OkHttpClientGenerator clientGenerator;

    ProviderApiManagerBase(SharedPreferences preferences, Resources resources, OkHttpClientGenerator clientGenerator, ProviderApiServiceCallback callback) {
        this.preferences = preferences;
        this.resources = resources;
        this.serviceCallback = callback;
        this.clientGenerator = clientGenerator;
    }

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
            setErrorResult(result, R.string.error_network_connection, null);
            sendToReceiverOrBroadcast(receiver, MISSING_NETWORK_CONNECTION, result, provider);
            return;
        }

         try {
             if (PreferenceHelper.hasSnowflakePrefs(preferences) && !VpnStatus.isVPNActive()) {
                 startTorProxy();
             }
        } catch (InterruptedException | IllegalStateException e) {
            e.printStackTrace();
             Bundle result = new Bundle();
             setErrorResultAction(result, action);
             sendToReceiverOrBroadcast(receiver, TOR_EXCEPTION, result, provider);
            return;
        } catch (TimeoutException e) {
             serviceCallback.stopTorService();
             Bundle result = new Bundle();
             setErrorResult(result, R.string.error_tor_timeout, ERROR_TOR_TIMEOUT.toString(), action);
             sendToReceiverOrBroadcast(receiver, TOR_TIMEOUT, result, provider);
             return;
         }

        Bundle result = new Bundle();
        switch (action) {
            case UPDATE_PROVIDER_DETAILS:
                ProviderObservable.getInstance().setProviderForDns(provider);
                resetProviderDetails(provider);
                Bundle task = new Bundle();
                result = setUpProvider(provider, task);
                if (result.getBoolean(BROADCAST_RESULT_KEY)) {
                    getGeoIPJson(provider);
                    sendToReceiverOrBroadcast(receiver, PROVIDER_OK, result, provider);
                } else {
                    sendToReceiverOrBroadcast(receiver, PROVIDER_NOK, result, provider);
                }
                ProviderObservable.getInstance().setProviderForDns(null);
                break;
            case SET_UP_PROVIDER:
                ProviderObservable.getInstance().setProviderForDns(provider);
                result = setUpProvider(provider, parameters);
                if (result.getBoolean(BROADCAST_RESULT_KEY)) {
                    getGeoIPJson(provider);
                    sendToReceiverOrBroadcast(receiver, PROVIDER_OK, result, provider);
                } else {
                    sendToReceiverOrBroadcast(receiver, PROVIDER_NOK, result, provider);
                }
                ProviderObservable.getInstance().setProviderForDns(null);
                break;
            case SIGN_UP:
                result = tryToRegister(provider, parameters);
                if (result.getBoolean(BROADCAST_RESULT_KEY)) {
                    sendToReceiverOrBroadcast(receiver, SUCCESSFUL_SIGNUP, result, provider);
                } else {
                    sendToReceiverOrBroadcast(receiver, FAILED_SIGNUP, result, provider);
                }
                break;
            case LOG_IN:
                result = tryToAuthenticate(provider, parameters);
                if (result.getBoolean(BROADCAST_RESULT_KEY)) {
                    sendToReceiverOrBroadcast(receiver, SUCCESSFUL_LOGIN, result, provider);
                } else {
                    sendToReceiverOrBroadcast(receiver, FAILED_LOGIN, result, provider);
                }
                break;
            case LOG_OUT:
                if (logOut(provider)) {
                    sendToReceiverOrBroadcast(receiver, SUCCESSFUL_LOGOUT, Bundle.EMPTY, provider);
                } else {
                    sendToReceiverOrBroadcast(receiver, LOGOUT_FAILED, Bundle.EMPTY, provider);
                }
                break;
            case DOWNLOAD_VPN_CERTIFICATE:
                ProviderObservable.getInstance().setProviderForDns(provider);
                result = updateVpnCertificate(provider);
                if (result.getBoolean(BROADCAST_RESULT_KEY)) {
                    sendToReceiverOrBroadcast(receiver, CORRECTLY_DOWNLOADED_VPN_CERTIFICATE, result, provider);
                } else {
                    sendToReceiverOrBroadcast(receiver, INCORRECTLY_DOWNLOADED_VPN_CERTIFICATE, result, provider);
                }
                ProviderObservable.getInstance().setProviderForDns(null);
                break;
            case QUIETLY_UPDATE_VPN_CERTIFICATE:
                ProviderObservable.getInstance().setProviderForDns(provider);
                result = updateVpnCertificate(provider);
                if (result.getBoolean(BROADCAST_RESULT_KEY)) {
                    Log.d(TAG, "successfully downloaded VPN certificate");
                    provider.setShouldUpdateVpnCertificate(false);
                    PreferenceHelper.storeProviderInPreferences(preferences, provider);
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
                PreferenceHelper.storeProviderInPreferences(preferences, provider);
                ProviderObservable.getInstance().updateProvider(provider);
                break;

            case UPDATE_INVALID_VPN_CERTIFICATE:
                ProviderObservable.getInstance().setProviderForDns(provider);
                result = updateVpnCertificate(provider);
                if (result.getBoolean(BROADCAST_RESULT_KEY)) {
                    sendToReceiverOrBroadcast(receiver, CORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE, result, provider);
                } else {
                    sendToReceiverOrBroadcast(receiver, INCORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE, result, provider);
                }
                ProviderObservable.getInstance().setProviderForDns(null);
                break;
            case DOWNLOAD_SERVICE_JSON:
                ProviderObservable.getInstance().setProviderForDns(provider);
                Log.d(TAG, "update eip service json");
                result = getAndSetEipServiceJson(provider);
                if (result.getBoolean(BROADCAST_RESULT_KEY)) {
                    sendToReceiverOrBroadcast(receiver, CORRECTLY_DOWNLOADED_EIP_SERVICE, result, provider);
                } else {
                    sendToReceiverOrBroadcast(receiver, INCORRECTLY_DOWNLOADED_EIP_SERVICE, result, provider);
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
                        sendToReceiverOrBroadcast(receiver, CORRECTLY_DOWNLOADED_GEOIP_JSON, result, provider);
                    } else {
                        sendToReceiverOrBroadcast(receiver, INCORRECTLY_DOWNLOADED_GEOIP_JSON, result, provider);
                    }
                    ProviderObservable.getInstance().setProviderForDns(null);
                }
                break;
        }
    }

    protected boolean startTorProxy() throws InterruptedException, IllegalStateException, TimeoutException {
        if (EipStatus.getInstance().isDisconnected() &&
                PreferenceHelper.getUseSnowflake(preferences) &&
            serviceCallback.startTorService()) {
            waitForTorCircuits();
            if (TorStatusObservable.isCancelled()) {
                throw new InterruptedException("Cancelled Tor setup.");
            }
            int port = serviceCallback.getTorHttpTunnelPort();
            TorStatusObservable.setProxyPort(port);
            return port != -1;
        }
        return false;
    }

    private void waitForTorCircuits() throws InterruptedException, TimeoutException {
        if (TorStatusObservable.getStatus() == ON) {
            return;
        }
        TorStatusObservable.waitUntil(this::isTorOnOrCancelled, 180);
    }

    private boolean isTorOnOrCancelled() {
        return TorStatusObservable.getStatus() == ON || TorStatusObservable.isCancelled();
    }

    void resetProviderDetails(Provider provider) {
        provider.reset();
        deleteProviderDetailsFromPreferences(preferences, provider.getDomain());
    }

    String formatErrorMessage(final int errorStringId) {
        return formatErrorMessage(getProviderFormattedString(resources, errorStringId));
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

    private void addErrorMessageToJson(JSONObject jsonObject, String errorMessage) {
        try {
            jsonObject.put(ERRORS, errorMessage);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void addErrorMessageToJson(JSONObject jsonObject, String errorMessage, String errorId, String initialAction) {
        try {
            jsonObject.putOpt(ERRORS, errorMessage);
            jsonObject.putOpt(ERRORID, errorId);
            jsonObject.putOpt(INITIAL_ACTION, initialAction);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private Bundle tryToRegister(Provider provider, Bundle task) {
        Bundle result = new Bundle();

        String username = task.getString(CREDENTIALS_USERNAME);
        String password = task.getString(CREDENTIALS_PASSWORD);

        if(provider == null) {
            result.putBoolean(BROADCAST_RESULT_KEY, false);
            Log.e(TAG, "no provider when trying to register");
            return result;
        }

        if (validUserLoginData(username, password)) {
            result = register(provider, username, password);
        } else {
            if (!wellFormedPassword(password)) {
                result.putBoolean(BROADCAST_RESULT_KEY, false);
                result.putString(CREDENTIALS_USERNAME, username);
                result.putBoolean(CREDENTIAL_ERRORS.PASSWORD_INVALID_LENGTH.toString(), true);
            }
            if (!validUsername(username)) {
                result.putBoolean(BROADCAST_RESULT_KEY, false);
                result.putBoolean(CREDENTIAL_ERRORS.USERNAME_MISSING.toString(), true);
            }
        }

        return result;
    }

    private Bundle register(Provider provider, String username, String password) {
        JSONObject stepResult = null;
        OkHttpClient okHttpClient = clientGenerator.initSelfSignedCAHttpClient(provider.getCaCert(), getProxyPort(), stepResult);
        if (okHttpClient == null) {
            return backendErrorNotification(stepResult, username);
        }

        LeapSRPSession client = new LeapSRPSession(username, password);
        byte[] salt = client.calculateNewSalt();

        BigInteger password_verifier = client.calculateV(username, password, salt);

        JSONObject api_result = sendNewUserDataToSRPServer(provider.getApiUrlWithVersion(), username, new BigInteger(1, salt).toString(16), password_verifier.toString(16), okHttpClient);

        Bundle result = new Bundle();
        if (api_result.has(ERRORS) || api_result.has(BACKEND_ERROR_KEY))
            result = backendErrorNotification(api_result, username);
        else {
            result.putString(CREDENTIALS_USERNAME, username);
            result.putString(CREDENTIALS_PASSWORD, password);
            result.putBoolean(BROADCAST_RESULT_KEY, true);
        }

        return result;
    }

    /**
     * Starts the authentication process using SRP protocol.
     *
     * @param task containing: username, password and provider
     * @return a bundle with a boolean value mapped to a key named BROADCAST_RESULT_KEY, and which is true if authentication was successful.
     */
    private Bundle tryToAuthenticate(Provider provider, Bundle task) {
        Bundle result = new Bundle();

        String username = task.getString(CREDENTIALS_USERNAME);
        String password = task.getString(CREDENTIALS_PASSWORD);

        if (validUserLoginData(username, password)) {
            result = authenticate(provider, username, password);
        } else {
            if (!wellFormedPassword(password)) {
                result.putBoolean(BROADCAST_RESULT_KEY, false);
                result.putString(CREDENTIALS_USERNAME, username);
                result.putBoolean(CREDENTIAL_ERRORS.PASSWORD_INVALID_LENGTH.toString(), true);
            }
            if (!validUsername(username)) {
                result.putBoolean(BROADCAST_RESULT_KEY, false);
                result.putBoolean(CREDENTIAL_ERRORS.USERNAME_MISSING.toString(), true);
            }
        }

        return result;
    }

    private Bundle authenticate(Provider provider, String username, String password) {
        Bundle result = new Bundle();
        JSONObject stepResult = new JSONObject();

        String providerApiUrl = provider.getApiUrlWithVersion();

        OkHttpClient okHttpClient = clientGenerator.initSelfSignedCAHttpClient(provider.getCaCert(), getProxyPort(), stepResult);
        if (okHttpClient == null) {
            return backendErrorNotification(stepResult, username);
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
                    result.putBoolean(BROADCAST_RESULT_KEY, true);
                } else {
                    backendErrorNotification(step_result, username);
                }
            } else {
                result.putBoolean(BROADCAST_RESULT_KEY, false);
                result.putString(CREDENTIALS_USERNAME, username);
                result.putString(USER_MESSAGE, resources.getString(R.string.error_srp_math_error_user_message));
            }
        } catch (JSONException e) {
            result = backendErrorNotification(step_result, username);
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

    private Bundle backendErrorNotification(JSONObject result, String username) {
        Bundle userNotificationBundle = new Bundle();
        if (result.has(ERRORS)) {
            Object baseErrorMessage = result.opt(ERRORS);
            if (baseErrorMessage instanceof JSONObject) {
                try {
                    JSONObject errorMessage = result.getJSONObject(ERRORS);
                    String errorType = errorMessage.keys().next().toString();
                    String message = errorMessage.get(errorType).toString();
                    userNotificationBundle.putString(USER_MESSAGE, message);
                } catch (JSONException | NoSuchElementException | NullPointerException e) {
                    e.printStackTrace();
                }
            } else if (baseErrorMessage instanceof String) {
                try {
                    String errorMessage = result.getString(ERRORS);
                    userNotificationBundle.putString(USER_MESSAGE, errorMessage);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else if (result.has(BACKEND_ERROR_KEY)) {
            try {
                String backendErrorMessage = resources.getString(R.string.error_json_exception_user_message);
                if (result.has(BACKEND_ERROR_MESSAGE)) {
                    backendErrorMessage = resources.getString(R.string.error) + result.getString(BACKEND_ERROR_MESSAGE);
                }
                userNotificationBundle.putString(USER_MESSAGE, backendErrorMessage);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (!username.isEmpty())
            userNotificationBundle.putString(CREDENTIALS_USERNAME, username);
        userNotificationBundle.putBoolean(BROADCAST_RESULT_KEY, false);

        return userNotificationBundle;
    }

    private void sendToReceiverOrBroadcast(ResultReceiver receiver, int resultCode, Bundle resultData, Provider provider) {
        if (resultData == null || resultData == Bundle.EMPTY) {
            resultData = new Bundle();
        }
        resultData.putParcelable(PROVIDER_KEY, provider);
        if (receiver != null) {
            receiver.send(resultCode, resultData);
        } else {
            broadcastEvent(resultCode, resultData);
        }
        handleEventSummaryErrorLog(resultCode);
    }

    private void broadcastEvent(int resultCode , Bundle resultData) {
        Intent intentUpdate = new Intent(BROADCAST_PROVIDER_API_EVENT);
        intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
        intentUpdate.putExtra(BROADCAST_RESULT_CODE, resultCode);
        intentUpdate.putExtra(BROADCAST_RESULT_KEY, resultData);
        serviceCallback.broadcastEvent(intentUpdate);
    }

    private void handleEventSummaryErrorLog(int resultCode) {
        String event = null;
        switch (resultCode) {
            case FAILED_LOGIN:
                event = "login.";
                break;
            case FAILED_SIGNUP:
                event = "signup.";
                break;
            case SUCCESSFUL_LOGOUT:
                event = "logout.";
                break;
            case INCORRECTLY_DOWNLOADED_VPN_CERTIFICATE:
                event = "download of vpn certificate.";
                break;
            case PROVIDER_NOK:
                event = "setup or update provider details.";
                break;
            case INCORRECTLY_DOWNLOADED_EIP_SERVICE:
                event = "update eip-service.json";
                break;
            case INCORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE:
                event = "update invalid vpn certificate.";
                break;
            case INCORRECTLY_DOWNLOADED_GEOIP_JSON:
                event = "download menshen service json.";
                break;
            case TOR_TIMEOUT:
            case TOR_EXCEPTION:
                event = "start tor for censorship circumvention";
                break;
            default:
                break;
        }
        if (event != null) {
            VpnStatus.logWarning("[API] failed provider API event: " + event);
        }
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
        } catch (NullPointerException | JSONException e) {
            e.printStackTrace();
            responseJson = getErrorMessageAsJson(error_json_exception_user_message);
            VpnStatus.logWarning("[API] got null response for request: " + url);
        }
        return responseJson;

    }

    private String requestStringFromServer(@NonNull String url, @NonNull String request_method, String jsonString, @NonNull List<Pair<String, String>> headerArgs, @NonNull OkHttpClient okHttpClient) {
        String plainResponseBody;

        try {

            plainResponseBody = ProviderApiConnector.requestStringFromServer(url, request_method, jsonString, headerArgs, okHttpClient);

        } catch (NullPointerException npe) {
            plainResponseBody = formatErrorMessage(error_json_exception_user_message);
            VpnStatus.logWarning("[API] Null response body for request " + url + ": " + npe.getLocalizedMessage());
        } catch (UnknownHostException | SocketTimeoutException e) {
            plainResponseBody = formatErrorMessage(server_unreachable_message);
            VpnStatus.logWarning("[API] UnknownHostException or SocketTimeoutException for request " + url + ": " + e.getLocalizedMessage());
        } catch (MalformedURLException e) {
            plainResponseBody = formatErrorMessage(malformed_url);
            VpnStatus.logWarning("[API] MalformedURLException for request " + url + ": " + e.getLocalizedMessage());
        } catch (SSLHandshakeException | SSLPeerUnverifiedException e) {
            plainResponseBody = formatErrorMessage(certificate_error);
            VpnStatus.logWarning("[API] SSLHandshakeException or SSLPeerUnverifiedException for request " + url + ": " + e.getLocalizedMessage());
        } catch (ConnectException e) {
            plainResponseBody = formatErrorMessage(service_is_down_error);
            VpnStatus.logWarning("[API] ConnectException for request " + url + ": " + e.getLocalizedMessage());
        } catch (IllegalArgumentException e) {
            plainResponseBody = formatErrorMessage(error_no_such_algorithm_exception_user_message);
            VpnStatus.logWarning("[API] IllegalArgumentException for request " + url + ": " + e.getLocalizedMessage());
        } catch (UnknownServiceException e) {
            //unable to find acceptable protocols - tlsv1.2 not enabled?
            plainResponseBody = formatErrorMessage(error_no_such_algorithm_exception_user_message);
            VpnStatus.logWarning("[API] UnknownServiceException for request " + url + ": " + e.getLocalizedMessage());
        } catch (IOException e) {
            plainResponseBody = formatErrorMessage(error_io_exception_user_message);
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
            setErrorResult(result, server_unreachable_message, null);
        } catch (MalformedURLException e) {
            VpnStatus.logWarning("[API] MalformedURLException during connection check: " + e.getLocalizedMessage());
            setErrorResult(result, malformed_url, null);
        } catch (SSLHandshakeException e) {
            VpnStatus.logWarning("[API] SSLHandshakeException during connection check: " + e.getLocalizedMessage());
            setErrorResult(result, warning_corrupted_provider_cert, ERROR_INVALID_CERTIFICATE.toString());
        } catch (ConnectException e) {
            VpnStatus.logWarning("[API] ConnectException during connection check: " + e.getLocalizedMessage());
            setErrorResult(result, service_is_down_error, null);
        } catch (IllegalArgumentException e) {
            VpnStatus.logWarning("[API] IllegalArgumentException during connection check: " + e.getLocalizedMessage());
            setErrorResult(result, error_no_such_algorithm_exception_user_message, null);
        } catch (UnknownServiceException e) {
            VpnStatus.logWarning("[API] UnknownServiceException during connection check: " + e.getLocalizedMessage());
            //unable to find acceptable protocols - tlsv1.2 not enabled?
            setErrorResult(result, error_no_such_algorithm_exception_user_message, null);
        } catch (IOException e) {
            VpnStatus.logWarning("[API] IOException during connection check: " + e.getLocalizedMessage());
            setErrorResult(result, error_io_exception_user_message, null);
        }

        try {
            if (tries == 0 &&
                    result.containsKey(ERRORS) &&
                    TorStatusObservable.getStatus() == OFF &&
                    startTorProxy()
            ) {
                return canConnect(provider, result, 1);
            }
        } catch (InterruptedException | IllegalStateException | TimeoutException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Downloads a provider.json from a given URL, adding a new provider using the given name.
     *
     * @param task containing a boolean meaning if the provider is custom or not, another boolean meaning if the user completely trusts this provider
     * @return a bundle with a boolean value mapped to a key named BROADCAST_RESULT_KEY, and which is true if the update was successful.
     */
    protected abstract Bundle setUpProvider(Provider provider, Bundle task);

    /**
     * Downloads the eip-service.json from a given URL, and saves eip service capabilities including the offered gateways
     * @return a bundle with a boolean value mapped to a key named BROADCAST_RESULT_KEY, and which is true if the download was successful.
     */
    protected abstract Bundle getAndSetEipServiceJson(Provider provider);

    /**
     * Downloads a new OpenVPN certificate, attaching authenticated cookie for authenticated certificate.
     *
     * @return true if certificate was downloaded correctly, false if provider.json is not present in SharedPreferences, or if the certificate url could not be parsed as a URI, or if there was an SSL error.
     */
    protected abstract Bundle updateVpnCertificate(Provider provider);


    /**
     * Fetches the Geo ip Json, containing a list of gateways sorted by distance from the users current location
     *
     * @param provider
     * @return
     */
    protected abstract Bundle getGeoIPJson(Provider provider);


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

    protected boolean validCertificate(Provider provider, String certString) {
        boolean result = false;
        if (!ConfigHelper.checkErroneousDownload(certString)) {
            ArrayList<X509Certificate> certificates = ConfigHelper.parseX509CertificatesFromString(certString);
            try {
                if (certificates != null) {
                    if (certificates.size() == 1) {
                        JSONObject providerJson = provider.getDefinition();
                        String fingerprint = providerJson.getString(Provider.CA_CERT_FINGERPRINT);
                        String encoding = fingerprint.split(":")[0];
                        String expectedFingerprint = fingerprint.split(":")[1];
                        String realFingerprint = getFingerprintFromCertificate(certificates.get(0), encoding);
                        result = realFingerprint.trim().equalsIgnoreCase(expectedFingerprint.trim());
                    } else {
                        // otherwise we assume the provider is transitioning the CA certs and thus shipping multiple CA certs
                        // in that case we don't do cert pinning
                        result = true;
                    }
                } else {
                    result = false;
                }
            } catch (JSONException | NoSuchAlgorithmException | CertificateEncodingException e) {
                result = false;
            }
        }

        return result;
    }

    protected void getPersistedProviderUpdates(Provider provider) {
        String providerDomain = getDomainFromMainURL(provider.getMainUrlString());
        if (hasUpdatedProviderDetails(providerDomain)) {
            provider.setCaCert(getPersistedProviderCA(providerDomain));
            provider.define(getPersistedProviderDefinition(providerDomain));
            provider.setPrivateKey(getPersistedPrivateKey(providerDomain));
            provider.setVpnCertificate(getPersistedVPNCertificate(providerDomain));
            provider.setProviderApiIp(getPersistedProviderApiIp(providerDomain));
            provider.setProviderIp(getPersistedProviderIp(providerDomain));
            provider.setGeoipUrl(getPersistedGeoIp(providerDomain)); // TODO: do we really need to persist the Geoip URL??
            provider.setLastMotdSeen(getPersistedMotdLastSeen(providerDomain));
            provider.setMotdLastSeenHashes(getPersistedMotdHashes(providerDomain));
            provider.setLastMotdUpdate(getPersistedMotdLastUpdate(providerDomain));
            provider.setMotdJson(getPersistedMotd(providerDomain));
        }
    }

    Bundle validateProviderDetails(Provider provider) {
        Bundle result = new Bundle();
        result.putBoolean(BROADCAST_RESULT_KEY, false);

        if (!provider.hasDefinition()) {
            return result;
        }

        result = validateCertificateForProvider(result, provider);

        //invalid certificate or no certificate
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
            return setErrorResult(result, warning_corrupted_provider_cert, ERROR_INVALID_CERTIFICATE.toString());
        }
        try {
            String encoding = provider.getCertificatePinEncoding();
            String expectedFingerprint = provider.getCertificatePin();

            // Do certificate pinning only if we have 1 cert, otherwise we assume some transitioning of
            // X509 certs, therefore we cannot do cert pinning
            if (certificates.size() == 1) {
                String realFingerprint = getFingerprintFromCertificate(certificates.get(0), encoding);
                if (!realFingerprint.trim().equalsIgnoreCase(expectedFingerprint.trim())) {
                    return setErrorResult(result, warning_corrupted_provider_cert, ERROR_CERTIFICATE_PINNING.toString());
                }
            }
            for (X509Certificate certificate : certificates) {
                certificate.checkValidity();
            }

            if (!canConnect(provider, result)) {
                return result;
            }
        } catch (NoSuchAlgorithmException e ) {
            return setErrorResult(result, error_no_such_algorithm_exception_user_message, null);
        } catch (ArrayIndexOutOfBoundsException e) {
            return setErrorResult(result, warning_corrupted_provider_details, ERROR_CORRUPTED_PROVIDER_JSON.toString());
        } catch (CertificateEncodingException | CertificateNotYetValidException | CertificateExpiredException e) {
            return setErrorResult(result, warning_expired_provider_cert, ERROR_INVALID_CERTIFICATE.toString());
        }

        result.putBoolean(BROADCAST_RESULT_KEY, true);
        return result;
    }

    protected Bundle setErrorResult(Bundle result, String stringJsonErrorMessage) {
        String reasonToFail = pickErrorMessage(stringJsonErrorMessage);
        VpnStatus.logWarning("[API] error: " + reasonToFail);
        result.putString(ERRORS, reasonToFail);
        result.putBoolean(BROADCAST_RESULT_KEY, false);
        return result;
    }

    Bundle setErrorResultAction(Bundle result, String initialAction) {
        JSONObject errorJson = new JSONObject();
        addErrorMessageToJson(errorJson, null, null, initialAction);
        VpnStatus.logWarning("[API] error: " + initialAction + " failed.");
        result.putString(ERRORS, errorJson.toString());
        result.putBoolean(BROADCAST_RESULT_KEY, false);
        return result;
    }

    Bundle setErrorResult(Bundle result, int errorMessageId, String errorId) {
        return setErrorResult(result, errorMessageId, errorId, null);
    }

    Bundle setErrorResult(Bundle result, int errorMessageId, String errorId, String initialAction) {
        JSONObject errorJson = new JSONObject();
        String errorMessage = getProviderFormattedString(resources, errorMessageId);
        addErrorMessageToJson(errorJson, errorMessage, errorId, initialAction);
        VpnStatus.logWarning("[API] error: " + errorMessage);
        result.putString(ERRORS, errorJson.toString());
        result.putBoolean(BROADCAST_RESULT_KEY, false);
        return result;
    }

    protected String getPersistedPrivateKey(String providerDomain) {
        return getFromPersistedProvider(PROVIDER_PRIVATE_KEY, providerDomain, preferences);
    }

    protected String getPersistedVPNCertificate(String providerDomain) {
        return getFromPersistedProvider(PROVIDER_VPN_CERTIFICATE, providerDomain, preferences);
    }

    protected JSONObject getPersistedProviderDefinition(String providerDomain) {
        try {
            return new JSONObject(getFromPersistedProvider(Provider.KEY, providerDomain, preferences));
        } catch (JSONException e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

    protected String getPersistedProviderCA(String providerDomain) {
        return getFromPersistedProvider(CA_CERT, providerDomain, preferences);
    }

    protected String getPersistedProviderApiIp(String providerDomain) {
        return getFromPersistedProvider(PROVIDER_API_IP, providerDomain, preferences);
    }

    protected String getPersistedProviderIp(String providerDomain) {
        return getFromPersistedProvider(PROVIDER_IP, providerDomain, preferences);
    }

    protected String getPersistedGeoIp(String providerDomain) {
        return getFromPersistedProvider(GEOIP_URL, providerDomain, preferences);
    }

    protected JSONObject getPersistedMotd(String providerDomain) {
        try {
            return new JSONObject(getFromPersistedProvider(PROVIDER_MOTD, providerDomain, preferences));
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    protected long getPersistedMotdLastSeen(String providerDomain) {
        return getLongFromPersistedProvider(PROVIDER_MOTD_LAST_SEEN, providerDomain, preferences);
    }

    protected long getPersistedMotdLastUpdate(String providerDomain) {
        return getLongFromPersistedProvider(PROVIDER_MOTD_LAST_UPDATED, providerDomain, preferences);
    }

    protected Set<String> getPersistedMotdHashes(String providerDomain) {
        return getStringSetFromPersistedProvider(PROVIDER_MOTD_HASHES, providerDomain, preferences);
    }


    protected boolean hasUpdatedProviderDetails(String domain) {
        return preferences.contains(Provider.KEY + "." + domain) && preferences.contains(CA_CERT + "." + domain);
    }

    /**
     * Interprets the error message as a JSON object and extract the "errors" keyword pair.
     * If the error message is not a JSON object, then it is returned untouched.
     *
     * @param stringJsonErrorMessage
     * @return final error message
     */
    protected String pickErrorMessage(String stringJsonErrorMessage) {
        String errorMessage = "";
        try {
            JSONObject jsonErrorMessage = new JSONObject(stringJsonErrorMessage);
            errorMessage = jsonErrorMessage.getString(ERRORS);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            errorMessage = stringJsonErrorMessage;
        } catch (NullPointerException e) {
            //do nothing
        }

        return errorMessage;
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

    private boolean logOut(Provider provider) {
        OkHttpClient okHttpClient = clientGenerator.initSelfSignedCAHttpClient(provider.getCaCert(), getProxyPort(), new JSONObject());
        if (okHttpClient == null) {
            return false;
        }

        String deleteUrl = provider.getApiUrlWithVersion() + "/logout";

        if (ProviderApiConnector.delete(okHttpClient, deleteUrl)) {
            LeapSRPSession.setToken("");
            return true;
        }
        return false;
    }

    protected Bundle loadCertificate(Provider provider, String certString) {
        Bundle result = new Bundle();
        if (certString == null) {
            setErrorResult(result, vpn_certificate_is_invalid, null);
            return result;
        }

        try {
            // API returns concatenated cert & key.  Split them for OpenVPN options
            String certificateString = null, keyString = null;
            String[] certAndKey = certString.split("(?<=-\n)");
            for (int i = 0; i < certAndKey.length - 1; i++) {
                if (certAndKey[i].contains("KEY")) {
                    keyString = certAndKey[i++] + certAndKey[i];
                } else if (certAndKey[i].contains("CERTIFICATE")) {
                    certificateString = certAndKey[i++] + certAndKey[i];
                }
            }

            RSAPrivateKey key = parseRsaKeyFromString(keyString);
            keyString = Base64.encodeToString(key.getEncoded(), Base64.DEFAULT);
            provider.setPrivateKey( "-----BEGIN RSA PRIVATE KEY-----\n" + keyString + "-----END RSA PRIVATE KEY-----");

            ArrayList<X509Certificate> certificates = ConfigHelper.parseX509CertificatesFromString(certificateString);
            certificates.get(0).checkValidity();
            certificateString = Base64.encodeToString(certificates.get(0).getEncoded(), Base64.DEFAULT);
            provider.setVpnCertificate( "-----BEGIN CERTIFICATE-----\n" + certificateString + "-----END CERTIFICATE-----");
            result.putBoolean(BROADCAST_RESULT_KEY, true);
        } catch (CertificateException | NullPointerException e) {
            e.printStackTrace();
            setErrorResult(result, vpn_certificate_is_invalid, null);
        }
        return result;
    }
}
