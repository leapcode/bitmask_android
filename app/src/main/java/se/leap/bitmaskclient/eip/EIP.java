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
import java.io.*;
import java.security.cert.*;
import java.text.*;
import java.util.*;
import org.json.*;

import de.blinkt.openvpn.*;
import de.blinkt.openvpn.activities.*;
import de.blinkt.openvpn.core.*;
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
    
    private static SharedPreferences preferences;

    private static Context context;
    private static ResultReceiver mReceiver;
    private static boolean mBound = false;
	
    private static int parsedEipSerial;
    private static JSONObject eip_definition = null;
	
    private static Gateway activeGateway = null;
    private static List<Gateway> gateways = new ArrayList<Gateway>();
    ProfileManager profile_manager;
    
    public static VpnStatus.ConnectionStatus lastConnectionStatusLevel;
    public static boolean mIsDisconnecting = false;
    public static boolean mIsStarting = false;

	public EIP(){
		super("LEAPEIP");
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
	public void onDestroy() {

	    mBound = false;
	    
	    super.onDestroy();
	}

	
    @Override
    protected void onHandleIntent(Intent intent) {
	String action = intent.getAction();
	mReceiver = intent.getParcelableExtra(RECEIVER_TAG);
	
	if ( action == ACTION_START_EIP )
	    startEIP();
	else if ( action == ACTION_STOP_EIP )
	    stopEIP();
	else if ( action == ACTION_IS_EIP_RUNNING )
	    isRunning();
	else if ( action == ACTION_UPDATE_EIP_SERVICE )
	    updateEIPService();
	else if ( action == ACTION_CHECK_CERT_VALIDITY )
	    checkCertValidity();
    }
	
    /**
     * Initiates an EIP connection by selecting a gateway and preparing and sending an
     * Intent to {@link se.leap.openvpn.LaunchVPN}.
     * It also sets up early routes.
     */
    private void startEIP() {
        earlyRoutes();
	GatewaySelector gateway_selector = new GatewaySelector(gateways);
	
	activeGateway = gateway_selector.select();
	if(activeGateway != null && activeGateway.getProfile() != null) {
	    mReceiver = EipServiceFragment.getReceiver();
	    launchActiveGateway();
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
	Intent intent = new Intent(this,LaunchVPN.class);
	intent.setAction(Intent.ACTION_MAIN);
	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	intent.putExtra(LaunchVPN.EXTRA_KEY, activeGateway.getProfile().getUUID().toString() );
	intent.putExtra(LaunchVPN.EXTRA_NAME, activeGateway.getProfile().getName() );
	intent.putExtra(LaunchVPN.EXTRA_HIDELOG, true);
	intent.putExtra(RECEIVER_TAG, mReceiver);
	startActivity(intent);
    }
    
    /**
     * Disconnects the EIP connection gracefully through the bound service or forcefully
     * if there is no bound service.  Sends a message to the requesting ResultReceiver.
     */
    private void stopEIP() {
	if(isConnected()) {
	    Intent disconnect_vpn = new Intent(this, DisconnectVPN.class);
	    disconnect_vpn.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    startActivity(disconnect_vpn);
	    mIsDisconnecting = true;
	    lastConnectionStatusLevel = VpnStatus.ConnectionStatus.UNKNOWN_LEVEL; // Wait for the decision of the user
	    Log.d(TAG, "mIsDisconnecting = true");
	}

	tellToReceiver(ACTION_STOP_EIP, Activity.RESULT_OK);
    }
    
    private void tellToReceiver(String action, int resultCode) {	
	if (mReceiver != null){
	    Bundle resultData = new Bundle();
	    resultData.putString(REQUEST_TAG, action);
	    mReceiver.send(resultCode, resultData);
	}
    }
	
    /**
     * Checks the last stored status notified by ics-openvpn
     * Sends <code>Activity.RESULT_CANCELED</code> to the ResultReceiver that made the
     * request if it's not connected, <code>Activity.RESULT_OK</code> otherwise.
     */
	
    private void isRunning() {
	int resultCode = (isConnected()) ?
	    Activity.RESULT_OK :
	    Activity.RESULT_CANCELED;
	tellToReceiver(ACTION_IS_EIP_RUNNING, resultCode);
    }
    
    public static boolean isConnected() {	
	return lastConnectionStatusLevel != null
	    && lastConnectionStatusLevel.equals(VpnStatus.ConnectionStatus.LEVEL_CONNECTED)
	    && !mIsDisconnecting;
    }

    /**
     * Loads eip-service.json from SharedPreferences and calls {@link updateGateways()}
     * to parse gateway definitions.
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
    }
	
    /**
     * Walk the list of gateways defined in eip-service.json and parse them into
     * Gateway objects.
     * TODO Store the Gateways (as Serializable) in SharedPreferences
     */
    private void updateGateways(){
	try {
	    JSONArray gatewaysDefined = eip_definition.getJSONArray("gateways");		
	    for ( int i=0 ; i < gatewaysDefined.length(); i++ ){			
		JSONObject gw = gatewaysDefined.getJSONObject(i);
		if(isOpenVpnGateway(gw)) {
		    addGateway(new Gateway(eip_definition, context, gw));
		}
	    }
	} catch (JSONException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	preferences.edit().putInt(PARSED_SERIAL, eip_definition.optInt(Provider.API_RETURN_SERIAL)).commit();
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
}
