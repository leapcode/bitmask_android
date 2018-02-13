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

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

import de.blinkt.openvpn.LaunchVPN;
import se.leap.bitmaskclient.OnBootReceiver;

import static se.leap.bitmaskclient.Constants.EIP_ACTION_CHECK_CERT_VALIDITY;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_IS_RUNNING;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_START;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_START_ALWAYS_ON_VPN;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_STOP;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_UPDATE;
import static se.leap.bitmaskclient.Constants.EIP_RECEIVER;
import static se.leap.bitmaskclient.Constants.EIP_REQUEST;
import static se.leap.bitmaskclient.Constants.EIP_RESTART_ON_BOOT;
import static se.leap.bitmaskclient.Constants.EIP_TRIGGERED_FROM_UI;
import static se.leap.bitmaskclient.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.Constants.PROVIDER_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.Constants.SHARED_PREFERENCES;

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

    public final static String TAG = EIP.class.getSimpleName();
    public final static String SERVICE_API_PATH = "config/eip-service.json";

    private WeakReference<ResultReceiver> mReceiverRef = new WeakReference<>(null);
    private SharedPreferences preferences;

    private JSONObject eipDefinition;
    private GatewaysManager gatewaysManager = new GatewaysManager();
    private Gateway gateway;

    public EIP() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        eipDefinition = eipDefinitionFromPreferences();
        if (gatewaysManager.isEmpty())
            gatewaysFromPreferences();
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
                startEIP(!intent.hasExtra(EIP_TRIGGERED_FROM_UI));
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
            case EIP_ACTION_UPDATE:
                updateEIPService();
                break;
            case EIP_ACTION_CHECK_CERT_VALIDITY:
                checkCertValidity();
                break;
        }
    }

    /**
     * Initiates an EIP connection by selecting a gateway and preparing and sending an
     * Intent to {@link de.blinkt.openvpn.LaunchVPN}.
     * It also sets up early routes.
     */
    private void startEIP(boolean earlyRoutes) {
        if (!preferences.getBoolean(EIP_RESTART_ON_BOOT, false)){
            preferences.edit().putBoolean(EIP_RESTART_ON_BOOT, true).commit();
        }
        if (gatewaysManager.isEmpty())
            updateEIPService();
        if (!EipStatus.getInstance().isBlockingVpnEstablished() && earlyRoutes)  {
            earlyRoutes();
        }

        gateway = gatewaysManager.select();
        if (gateway != null && gateway.getProfile() != null) {
            launchActiveGateway();
            tellToReceiver(EIP_ACTION_START, Activity.RESULT_OK);
        } else
            tellToReceiver(EIP_ACTION_START, Activity.RESULT_CANCELED);
    }

    /**
     * Tries to start the last used vpn profile when the OS was rebooted and always-on-VPN is enabled.
     * The {@link OnBootReceiver} will care if there is no profile.
     */
    private void startEIPAlwaysOnVpn() {
        Log.d(TAG, "startEIPAlwaysOnVpn vpn");

        if (gatewaysManager.isEmpty())
            updateEIPService();

        gateway = gatewaysManager.select();

        if (gateway != null && gateway.getProfile() != null) {
            Log.d(TAG, "startEIPAlwaysOnVpn eip launch avtive gateway vpn");
            launchActiveGateway();
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

    private void launchActiveGateway() {
        Intent intent = new Intent(this, LaunchVPN.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(LaunchVPN.EXTRA_HIDELOG, true);
        intent.putExtra(LaunchVPN.EXTRA_TEMP_VPN_PROFILE, gateway.getProfile());
        startActivity(intent);
    }

    private void stopEIP() {
        EipStatus eipStatus = EipStatus.getInstance();
        int resultCode = Activity.RESULT_CANCELED;
        if (eipStatus.isConnected() || eipStatus.isConnecting())
            resultCode = Activity.RESULT_OK;

        tellToReceiver(EIP_ACTION_STOP, resultCode);
    }

    /**
     * Checks the last stored status notified by ics-openvpn
     * Sends <code>Activity.RESULT_CANCELED</code> to the ResultReceiver that made the
     * request if it's not connected, <code>Activity.RESULT_OK</code> otherwise.
     */
    private void isRunning() {
        EipStatus eipStatus = EipStatus.getInstance();
        int resultCode = (eipStatus.isConnected()) ?
                Activity.RESULT_OK :
                Activity.RESULT_CANCELED;
        tellToReceiver(EIP_ACTION_IS_RUNNING, resultCode);
    }

    /**
     * Loads eip-service.json from SharedPreferences, delete previous vpn profiles and add new gateways.
     * TODO Implement API call to refresh eip-service.json from the provider
     */
    private void updateEIPService() {
        eipDefinition = eipDefinitionFromPreferences();
        if (eipDefinition.length() > 0)
            updateGateways();
        tellToReceiver(EIP_ACTION_UPDATE, Activity.RESULT_OK);
    }

    private JSONObject eipDefinitionFromPreferences() {
        JSONObject result = new JSONObject();
        try {
            String eipDefinitionString = preferences.getString(PROVIDER_KEY, "");
            if (!eipDefinitionString.isEmpty()) {
                result = new JSONObject(eipDefinitionString);
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    private void updateGateways() {
        gatewaysManager.clearGatewaysAndProfiles();
        gatewaysManager.fromEipServiceJson(eipDefinition);
        gatewaysToPreferences();
    }

    private void gatewaysFromPreferences() {
        String gatewaysString = preferences.getString(Gateway.TAG, "");
        gatewaysManager = new GatewaysManager(this, preferences);
        gatewaysManager.addFromString(gatewaysString);
        preferences.edit().remove(Gateway.TAG).apply();
    }

    private void gatewaysToPreferences() {
        String gateways_string = gatewaysManager.toString();
        preferences.edit().putString(Gateway.TAG, gateways_string).commit();
    }

    private void checkCertValidity() {
        VpnCertificateValidator validator = new VpnCertificateValidator(preferences.getString(PROVIDER_VPN_CERTIFICATE, ""));
        int resultCode = validator.isValid() ?
                Activity.RESULT_OK :
                Activity.RESULT_CANCELED;
        tellToReceiver(EIP_ACTION_CHECK_CERT_VALIDITY, resultCode);
    }

    private void tellToReceiver(String action, int resultCode) {
        Bundle resultData = new Bundle();
        resultData.putString(EIP_REQUEST, action);
        if (mReceiverRef.get() != null) {
            mReceiverRef.get().send(resultCode, resultData);
        }
    }
}
