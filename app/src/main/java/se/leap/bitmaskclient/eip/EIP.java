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

import android.app.*;
import android.content.*;
import android.os.*;
import android.util.Log;

import org.json.*;

import de.blinkt.openvpn.*;
import se.leap.bitmaskclient.*;

import static se.leap.bitmaskclient.Constants.EIP_ACTION_CHECK_CERT_VALIDITY;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_IS_RUNNING;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_START;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_START_ALWAYS_ON_EIP;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_STOP;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_UPDATE;
import static se.leap.bitmaskclient.Constants.EIP_RECEIVER;
import static se.leap.bitmaskclient.Constants.EIP_REQUEST;
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

    private static Context context;
    private static ResultReceiver mReceiver;
    private static SharedPreferences preferences;

    private static JSONObject eip_definition;
    private static GatewaysManager gateways_manager = new GatewaysManager();
    private static Gateway gateway;

    public EIP() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        eip_definition = eipDefinitionFromPreferences();
        if (gateways_manager.isEmpty())
            gatewaysFromPreferences();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        mReceiver = intent.getParcelableExtra(EIP_RECEIVER);

        if (action.equals(EIP_ACTION_START))
            startEIP();
        else if (action.equals(EIP_ACTION_START_ALWAYS_ON_EIP))
            startAlwaysOnEIP();
        else if (action.equals(EIP_ACTION_STOP))
            stopEIP();
        else if (action.equals(EIP_ACTION_IS_RUNNING))
            isRunning();
        else if (action.equals(EIP_ACTION_UPDATE))
            updateEIPService();
        else if (action.equals(EIP_ACTION_CHECK_CERT_VALIDITY))
            checkCertValidity();
    }

    /**
     * Initiates an EIP connection by selecting a gateway and preparing and sending an
     * Intent to {@link de.blinkt.openvpn.LaunchVPN}.
     * It also sets up early routes.
     */
    private void startEIP() {
        if (gateways_manager.isEmpty())
            updateEIPService();
        if (!EipStatus.getInstance().isBlockingVpnEstablished())  {
            earlyRoutes();
        }

        gateway = gateways_manager.select();
        if (gateway != null && gateway.getProfile() != null) {
            mReceiver = VpnFragment.getReceiver();
            launchActiveGateway();
            tellToReceiver(EIP_ACTION_START, Activity.RESULT_OK);
        } else
            tellToReceiver(EIP_ACTION_START, Activity.RESULT_CANCELED);
    }

    /**
     * Tries to start the last used vpn profile when the OS was rebooted and always-on-VPN is enabled.
     * The {@link OnBootReceiver} will care if there is no profile.
     */
    private void startAlwaysOnEIP() {
        Log.d(TAG, "startAlwaysOnEIP vpn");

        if (gateways_manager.isEmpty())
            updateEIPService();

        gateway = gateways_manager.select();

        if (gateway != null && gateway.getProfile() != null) {
            //mReceiver = VpnFragment.getReceiver();
            Log.d(TAG, "startAlwaysOnEIP eip launch avtive gateway vpn");
            launchActiveGateway();
        } else {
            Log.d(TAG, "startAlwaysOnEIP no active profile available!");
        }
    }

    /**
     * Early routes are routes that block traffic until a new
     * VpnService is started properly.
     */
    private void earlyRoutes() {
        Intent void_vpn_launcher = new Intent(context, VoidVpnLauncher.class);
        void_vpn_launcher.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(void_vpn_launcher);
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
        EipStatus eip_status = EipStatus.getInstance();
        int result_code = Activity.RESULT_CANCELED;
        if (eip_status.isConnected() || eip_status.isConnecting())
            result_code = Activity.RESULT_OK;

        tellToReceiver(EIP_ACTION_STOP, result_code);
    }

    /**
     * Checks the last stored status notified by ics-openvpn
     * Sends <code>Activity.RESULT_CANCELED</code> to the ResultReceiver that made the
     * request if it's not connected, <code>Activity.RESULT_OK</code> otherwise.
     */
    private void isRunning() {
        EipStatus eip_status = EipStatus.getInstance();
        int resultCode = (eip_status.isConnected()) ?
                Activity.RESULT_OK :
                Activity.RESULT_CANCELED;
        tellToReceiver(EIP_ACTION_IS_RUNNING, resultCode);
    }

    /**
     * Loads eip-service.json from SharedPreferences, delete previous vpn profiles and add new gateways.
     * TODO Implement API call to refresh eip-service.json from the provider
     */
    private void updateEIPService() {
        eip_definition = eipDefinitionFromPreferences();
        if (eip_definition.length() > 0)
            updateGateways();
        tellToReceiver(EIP_ACTION_UPDATE, Activity.RESULT_OK);
    }

    private JSONObject eipDefinitionFromPreferences() {
        JSONObject result = new JSONObject();
        try {
            String eip_definition_string = preferences.getString(PROVIDER_KEY, "");
            if (!eip_definition_string.isEmpty()) {
                result = new JSONObject(eip_definition_string);
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    private void updateGateways() {
        gateways_manager.clearGatewaysAndProfiles();
        gateways_manager.fromEipServiceJson(eip_definition);
        gatewaysToPreferences();
    }

    private void gatewaysFromPreferences() {
        String gateways_string = preferences.getString(Gateway.TAG, "");
        gateways_manager = new GatewaysManager(context, preferences);
        gateways_manager.addFromString(gateways_string);
        preferences.edit().remove(Gateway.TAG).apply();
    }

    private void gatewaysToPreferences() {
        String gateways_string = gateways_manager.toString();
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
        if (mReceiver != null) {
            Bundle resultData = new Bundle();
            resultData.putString(EIP_REQUEST, action);
            mReceiver.send(resultCode, resultData);
        }
    }
}
