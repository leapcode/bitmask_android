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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.blinkt.openvpn.LaunchVPN;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ProfileManager;
import se.leap.bitmaskclient.Dashboard;
import se.leap.bitmaskclient.EipServiceFragment;

import static se.leap.bitmaskclient.eip.Constants.ACTION_CHECK_CERT_VALIDITY;
import static se.leap.bitmaskclient.eip.Constants.ACTION_IS_EIP_RUNNING;
import static se.leap.bitmaskclient.eip.Constants.ACTION_START_EIP;
import static se.leap.bitmaskclient.eip.Constants.ACTION_STOP_EIP;
import static se.leap.bitmaskclient.eip.Constants.ACTION_UPDATE_EIP_SERVICE;
import static se.leap.bitmaskclient.eip.Constants.CERTIFICATE;
import static se.leap.bitmaskclient.eip.Constants.KEY;
import static se.leap.bitmaskclient.eip.Constants.RECEIVER_TAG;
import static se.leap.bitmaskclient.eip.Constants.REQUEST_TAG;

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
    private static List<Gateway> gateways = new ArrayList<Gateway>();
    private static ProfileManager profile_manager;
    private static Gateway gateway;
    
	public EIP(){
		super(TAG);
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		context = getApplicationContext();
		profile_manager = ProfileManager.getInstance(context);

		preferences = getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE);
		refreshEipDefinition();
	}
	
    @Override
    protected void onHandleIntent(Intent intent) {
	String action = intent.getAction();
	mReceiver = intent.getParcelableExtra(RECEIVER_TAG);
	
	if ( action.equals(ACTION_START_EIP))
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
	if(gateways.isEmpty())
	    updateEIPService();
        earlyRoutes();

        GatewaySelector gateway_selector = new GatewaySelector(gateways);
	gateway = gateway_selector.select();
	if(gateway != null && gateway.getProfile() != null) {
	    mReceiver = EipServiceFragment.getReceiver();
	    launchActiveGateway();
	}
	tellToReceiver(ACTION_START_EIP, Activity.RESULT_OK);
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
	Intent intent = new Intent(this,LaunchVPN.class);
	intent.setAction(Intent.ACTION_MAIN);
	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	intent.putExtra(LaunchVPN.EXTRA_NAME, gateway.getProfile().getName());
	intent.putExtra(LaunchVPN.EXTRA_HIDELOG, true);
	startActivity(intent);
    }

    private void stopEIP() {
	EipStatus eip_status = EipStatus.getInstance();
	Log.d(TAG, "stopEip(): eip is connected? " + eip_status.isConnected());
	int result_code = Activity.RESULT_CANCELED;
	if(eip_status.isConnected())
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
	refreshEipDefinition();
	deleteAllVpnProfiles();
	updateGateways();
	tellToReceiver(ACTION_UPDATE_EIP_SERVICE, Activity.RESULT_OK);
    }

    private void refreshEipDefinition() {
	try {
	    String eip_definition_string = preferences.getString(KEY, "");
	    if(!eip_definition_string.isEmpty()) {
		eip_definition = new JSONObject(eip_definition_string);
	    }
	} catch (JSONException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }
    
    private void deleteAllVpnProfiles() {
	Collection<VpnProfile> profiles = profile_manager.getProfiles();
	profiles.removeAll(profiles);
	gateways.clear();
    }
	
    /**
     * Walk the list of gateways defined in eip-service.json and parse them into
     * Gateway objects.
     * TODO Store the Gateways (as Serializable) in SharedPreferences
     */
    private void updateGateways(){
	try {
        if(eip_definition != null) {
            JSONArray gatewaysDefined = eip_definition.getJSONArray("gateways");
            for (int i = 0; i < gatewaysDefined.length(); i++) {
                JSONObject gw = gatewaysDefined.getJSONObject(i);
                if (isOpenVpnGateway(gw)) {
                    addGateway(new Gateway(eip_definition, context, gw));
                }
            }
        }
	} catch (JSONException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    private boolean isOpenVpnGateway(JSONObject gateway) {
	try {
	    String transport = gateway.getJSONObject("capabilities").getJSONArray("transport").toString();
	    return transport.contains("openvpn");
	} catch (JSONException e) {
	    return false;
	}
    }

    private void addGateway(Gateway gateway) {
	profile_manager.addProfile(gateway.getProfile());
	gateways.add(gateway);
    }

    private void checkCertValidity() {
	VpnCertificateValidator validator = new VpnCertificateValidator();
	int resultCode = validator.isValid(preferences.getString(CERTIFICATE, "")) ?
	    Activity.RESULT_OK :
	    Activity.RESULT_CANCELED;
	tellToReceiver(ACTION_CHECK_CERT_VALIDITY, resultCode);
    }

    private void tellToReceiver(String action, int resultCode) {
        if (mReceiver != null){
            Bundle resultData = new Bundle();
            resultData.putString(REQUEST_TAG, action);
            mReceiver.send(resultCode, resultData);
        }
    }
}
