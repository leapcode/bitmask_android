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

import org.json.*;

import de.blinkt.openvpn.*;
import se.leap.bitmaskclient.*;

import static se.leap.bitmaskclient.eip.Constants.*;

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

    public static final int DISCONNECT = 15;

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
        preferences = getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE);
        eip_definition = eipDefinitionFromPreferences();
        if (gateways_manager.isEmpty())
            gatewaysFromPreferences();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        mReceiver = intent.getParcelableExtra(RECEIVER_TAG);

        if (action.equals(ACTION_START_EIP))
            startEIP();
        else if (action.equals(ACTION_STOP_EIP))
            stopEIP();
        else if (action.equals(ACTION_IS_EIP_RUNNING))
            isRunning();
        else if (action.equals(ACTION_UPDATE_EIP_SERVICE))
            updateEIPService();
        else if (action.equals(ACTION_CHECK_CERT_VALIDITY))
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
        earlyRoutes();

        gateway = gateways_manager.select();
        if (gateway != null && gateway.getProfile() != null) {
            mReceiver = VpnFragment.getReceiver();
            launchActiveGateway();
            tellToReceiver(ACTION_START_EIP, Activity.RESULT_OK);
        } else
            tellToReceiver(ACTION_START_EIP, Activity.RESULT_CANCELED);
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

        tellToReceiver(ACTION_STOP_EIP, result_code);
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
        tellToReceiver(ACTION_IS_EIP_RUNNING, resultCode);
    }

    /**
     * Loads eip-service.json from SharedPreferences, delete previous vpn profiles and add new gateways.
     * TODO Implement API call to refresh eip-service.json from the provider
     */
    private void updateEIPService() {
        eip_definition = eipDefinitionFromPreferences();
        if (eip_definition.length() > 0)
            updateGateways();
        tellToReceiver(ACTION_UPDATE_EIP_SERVICE, Activity.RESULT_OK);
    }

    private JSONObject eipDefinitionFromPreferences() {
        JSONObject result = new JSONObject();
        try {
            String eip_definition_string = preferences.getString(KEY, "");
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
        VpnCertificateValidator validator = new VpnCertificateValidator(preferences.getString(CERTIFICATE, ""));
        int resultCode = validator.isValid() ?
                Activity.RESULT_OK :
                Activity.RESULT_CANCELED;
        tellToReceiver(ACTION_CHECK_CERT_VALIDITY, resultCode);
    }

    private void tellToReceiver(String action, int resultCode) {
        if (mReceiver != null) {
            Bundle resultData = new Bundle();
            resultData.putString(REQUEST_TAG, action);
            mReceiver.send(resultCode, resultData);
        }
    }
}
