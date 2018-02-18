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
package se.leap.bitmaskclient.eip;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

import de.blinkt.openvpn.LaunchVPN;
import se.leap.bitmaskclient.OnBootReceiver;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static android.content.Intent.CATEGORY_DEFAULT;
import static se.leap.bitmaskclient.Constants.BROADCAST_EIP_EVENT;
import static se.leap.bitmaskclient.Constants.BROADCAST_RESULT_CODE;
import static se.leap.bitmaskclient.Constants.BROADCAST_RESULT_KEY;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_CHECK_CERT_VALIDITY;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_IS_RUNNING;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_START;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_START_ALWAYS_ON_VPN;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_STOP;
import static se.leap.bitmaskclient.Constants.EIP_EARLY_ROUTES;
import static se.leap.bitmaskclient.Constants.EIP_RECEIVER;
import static se.leap.bitmaskclient.Constants.EIP_REQUEST;
import static se.leap.bitmaskclient.Constants.EIP_RESTART_ON_BOOT;
import static se.leap.bitmaskclient.Constants.PROVIDER_EIP_DEFINITION;
import static se.leap.bitmaskclient.Constants.PROVIDER_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.Constants.SHARED_PREFERENCES;
import static se.leap.bitmaskclient.MainActivityErrorDialog.DOWNLOAD_ERRORS.ERROR_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.R.string.vpn_certificate_is_invalid;

/**
 * EIP is the abstract base class for interacting with and managing the Encrypted
 * Internet Proxy connection.  Connections are started, stopped, and queried through
 * this IntentService.
 * Contains logic for parsing eip-service.json from the provider, configuring and selecting
 * gateways, and controlling {@link de.blinkt.openvpn.core.OpenVPNService} connections.
 *
 * @author Sean Leonard <meanderingcode@aetherislands.net>
 * @author Parménides GV <parmegv@sdf.org>
 */
public final class EIP extends IntentService {

    public final static String TAG = EIP.class.getSimpleName(),
            SERVICE_API_PATH = "config/eip-service.json",
            ERRORS = "errors",
            ERROR_ID = "errorID";

    private WeakReference<ResultReceiver> mReceiverRef = new WeakReference<>(null);
    private SharedPreferences preferences;

    public EIP() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        if (intent.getParcelableExtra(EIP_RECEIVER) != null) {
            mReceiverRef = new WeakReference<>((ResultReceiver) intent.getParcelableExtra(EIP_RECEIVER));
        }

        if (action == null) {
            return;
        }

        switch (action) {
            case EIP_ACTION_START:
                boolean earlyRoutes = intent.getBooleanExtra(EIP_EARLY_ROUTES, true);
                startEIP(earlyRoutes);
                break;
            case EIP_ACTION_START_ALWAYS_ON_VPN:
                startEIPAlwaysOnVpn();
                break;
            case EIP_ACTION_STOP:
                stopEIP();
                break;
            case EIP_ACTION_IS_RUNNING:
                isRunning();
                break;
            case EIP_ACTION_CHECK_CERT_VALIDITY:
                checkVPNCertificateValidity();
                break;
        }
    }

    /**
     * Initiates an EIP connection by selecting a gateway and preparing and sending an
     * Intent to {@link de.blinkt.openvpn.LaunchVPN}.
     * It also sets up early routes.
     */
    private void startEIP(boolean earlyRoutes) {
        if (!EipStatus.getInstance().isBlockingVpnEstablished() && earlyRoutes)  {
            earlyRoutes();
        }

        Bundle result = new Bundle();

        if (!preferences.getBoolean(EIP_RESTART_ON_BOOT, false)){
            preferences.edit().putBoolean(EIP_RESTART_ON_BOOT, true).commit();
        }

        GatewaysManager gatewaysManager = gatewaysFromPreferences();
        if (!isVPNCertificateValid()){
            setErrorResult(result, vpn_certificate_is_invalid, ERROR_INVALID_VPN_CERTIFICATE.toString());
            tellToReceiverOrBroadcast(EIP_ACTION_START, RESULT_CANCELED, result);
            return;
        }

        Gateway gateway = gatewaysManager.select();
        if (gateway != null && gateway.getProfile() != null) {
            launchActiveGateway(gateway);
            tellToReceiverOrBroadcast(EIP_ACTION_START, RESULT_OK);
        } else
            tellToReceiverOrBroadcast(EIP_ACTION_START, RESULT_CANCELED);
    }

    /**
     * Tries to start the last used vpn profile when the OS was rebooted and always-on-VPN is enabled.
     * The {@link OnBootReceiver} will care if there is no profile.
     */
    private void startEIPAlwaysOnVpn() {
        Log.d(TAG, "startEIPAlwaysOnVpn vpn");

        GatewaysManager gatewaysManager = gatewaysFromPreferences();
        Gateway gateway = gatewaysManager.select();

        if (gateway != null && gateway.getProfile() != null) {
            Log.d(TAG, "startEIPAlwaysOnVpn eip launch avtive gateway vpn");
            launchActiveGateway(gateway);
        } else {
            Log.d(TAG, "startEIPAlwaysOnVpn no active profile available!");
        }
    }

    /**
     * Early routes are routes that block traffic until a new
     * VpnService is started properly.
     */
    private void earlyRoutes() {
        Intent voidVpnLauncher = new Intent(getApplicationContext(), VoidVpnLauncher.class);
        voidVpnLauncher.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(voidVpnLauncher);
    }

    private void launchActiveGateway(Gateway gateway) {
        Intent intent = new Intent(this, LaunchVPN.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(LaunchVPN.EXTRA_HIDELOG, true);
        intent.putExtra(LaunchVPN.EXTRA_TEMP_VPN_PROFILE, gateway.getProfile());
        startActivity(intent);
    }

    private void stopEIP() {
        // TODO stop eip from here if possible...
        EipStatus eipStatus = EipStatus.getInstance();
        int resultCode = RESULT_CANCELED;
        if (eipStatus.isConnected() || eipStatus.isConnecting())
            resultCode = RESULT_OK;

        tellToReceiverOrBroadcast(EIP_ACTION_STOP, resultCode);
    }

    /**
     * Checks the last stored status notified by ics-openvpn
     * Sends <code>Activity.RESULT_CANCELED</code> to the ResultReceiver that made the
     * request if it's not connected, <code>Activity.RESULT_OK</code> otherwise.
     */
    private void isRunning() {
        EipStatus eipStatus = EipStatus.getInstance();
        int resultCode = (eipStatus.isConnected()) ?
                RESULT_OK :
                RESULT_CANCELED;
        tellToReceiverOrBroadcast(EIP_ACTION_IS_RUNNING, resultCode);
    }

    private JSONObject eipDefinitionFromPreferences() {
        JSONObject result = new JSONObject();
        try {
            String eipDefinitionString = preferences.getString(PROVIDER_EIP_DEFINITION, "");
            if (!eipDefinitionString.isEmpty()) {
                result = new JSONObject(eipDefinitionString);
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    private GatewaysManager gatewaysFromPreferences() {
        GatewaysManager gatewaysManager = new GatewaysManager(this, preferences);
        gatewaysManager.fromEipServiceJson(eipDefinitionFromPreferences());
        return gatewaysManager;
    }

    private void checkVPNCertificateValidity() {
        int resultCode = isVPNCertificateValid() ?
                RESULT_OK :
                RESULT_CANCELED;
        tellToReceiverOrBroadcast(EIP_ACTION_CHECK_CERT_VALIDITY, resultCode);
    }

    private boolean isVPNCertificateValid() {
        VpnCertificateValidator validator = new VpnCertificateValidator(preferences.getString(PROVIDER_VPN_CERTIFICATE, ""));
        return validator.isValid();
    }

    private void tellToReceiverOrBroadcast(String action, int resultCode, Bundle resultData) {
        resultData.putString(EIP_REQUEST, action);
        if (mReceiverRef.get() != null) {
            mReceiverRef.get().send(resultCode, resultData);
        } else {
            broadcastEvent(resultCode, resultData);
        }
    }

    private void tellToReceiverOrBroadcast(String action, int resultCode) {
        tellToReceiverOrBroadcast(action, resultCode, new Bundle());
    }

    private void broadcastEvent(int resultCode , Bundle resultData) {
        Intent intentUpdate = new Intent(BROADCAST_EIP_EVENT);
        intentUpdate.addCategory(CATEGORY_DEFAULT);
        intentUpdate.putExtra(BROADCAST_RESULT_CODE, resultCode);
        intentUpdate.putExtra(BROADCAST_RESULT_KEY, resultData);
        Log.d(TAG, "sending broadcast");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intentUpdate);
    }

    Bundle setErrorResult(Bundle result, int errorMessageId, String errorId) {
        JSONObject errorJson = new JSONObject();
        addErrorMessageToJson(errorJson, getResources().getString(errorMessageId), errorId);
        result.putString(ERRORS, errorJson.toString());
        result.putBoolean(BROADCAST_RESULT_KEY, false);
        return result;
    }

    private void addErrorMessageToJson(JSONObject jsonObject, String errorMessage, String errorId) {
        try {
            jsonObject.put(ERRORS, errorMessage);
            jsonObject.put(ERROR_ID, errorId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
