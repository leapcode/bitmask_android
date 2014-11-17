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
	
    private static OVPNGateway activeGateway = null;
    
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
	GatewaySelector gateway_selector = new GatewaySelector(eip_definition);
	String selected_gateway = gateway_selector.select();
	
	activeGateway = new OVPNGateway(selected_gateway);
	if(activeGateway != null && activeGateway.mVpnProfile != null) {
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
	intent.putExtra(LaunchVPN.EXTRA_KEY, activeGateway.mVpnProfile.getUUID().toString() );
	intent.putExtra(LaunchVPN.EXTRA_NAME, activeGateway.mVpnProfile.getName() );
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
	ProfileManager vpl = ProfileManager.getInstance(context);
	Collection<VpnProfile> profiles = vpl.getProfiles();
	profiles.removeAll(profiles);
    }
	
	/**
	 * Walk the list of gateways defined in eip-service.json and parse them into
	 * OVPNGateway objects.
	 * TODO Store the OVPNGateways (as Serializable) in SharedPreferences
	 */
    private void updateGateways(){
	JSONArray gatewaysDefined = null;
	try {
	    gatewaysDefined = eip_definition.getJSONArray("gateways");		
	    for ( int i=0 ; i < gatewaysDefined.length(); i++ ){			
		JSONObject gw = null;			
		gw = gatewaysDefined.getJSONObject(i);
			
		if ( gw.getJSONObject("capabilities").getJSONArray("transport").toString().contains("openvpn") )
		    new OVPNGateway(gw);
	    }
	} catch (JSONException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	preferences.edit().putInt(PARSED_SERIAL, eip_definition.optInt(Provider.API_RETURN_SERIAL)).commit();
    }

    private void checkCertValidity() {
	VpnCertificateValidator validator = new VpnCertificateValidator();
	int resultCode = validator.isValid(preferences.getString(CERTIFICATE, "")) ?
	    Activity.RESULT_OK :
	    Activity.RESULT_CANCELED;
	tellToReceiver(ACTION_CHECK_CERT_VALIDITY, resultCode);
    }

	/**
	 * OVPNGateway provides objects defining gateways and their options and metadata.
	 * Each instance contains a VpnProfile for OpenVPN specific data and member
	 * variables describing capabilities and location
	 * 
	 * @author Sean Leonard <meanderingcode@aetherislands.net>
	 */
	private class OVPNGateway {
		
		private String TAG = "OVPNGateway";
		
		private String mName;
		private VpnProfile mVpnProfile;
		private JSONObject mGateway;
		private HashMap<String,Vector<Vector<String>>> options = new HashMap<String, Vector<Vector<String>>>();

		
		/**
		 * Attempts to retrieve a VpnProfile by name and build an OVPNGateway around it.
		 * FIXME This needs to become a findGatewayByName() method
		 * 
		 * @param name The hostname of the gateway to inflate
		 */
		private OVPNGateway(String name){
			mName = name;
			
			this.loadVpnProfile();
		}
		
		private void loadVpnProfile() {
			ProfileManager vpl = ProfileManager.getInstance(context);
			try {
				if ( mName == null )
					mVpnProfile = vpl.getProfiles().iterator().next();
				else
					mVpnProfile = vpl.getProfileByName(mName);
			} catch (NoSuchElementException e) {
				updateEIPService();
				this.loadVpnProfile();	// FIXME catch infinite loops
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		/**
		 * Build a gateway object from a JSON OpenVPN gateway definition in eip-service.json
		 * and create a VpnProfile belonging to it.
		 * 
		 * @param gateway The JSON OpenVPN gateway definition to parse
		 */
		protected OVPNGateway(JSONObject gateway){

			mGateway = gateway;
			
			// Currently deletes VpnProfile for host, if there already is one, and builds new
			ProfileManager vpl = ProfileManager.getInstance(context);
			Collection<VpnProfile> profiles = vpl.getProfiles();
			for (Iterator<VpnProfile> it = profiles.iterator(); it.hasNext(); ){
				VpnProfile p = it.next();
				
				if ( p.mName.equalsIgnoreCase( mName ) ) {
				    it.remove();
				    vpl.removeProfile(context, p);
				}
			}
			
			this.createVPNProfile();
			
			vpl.addProfile(mVpnProfile);
			vpl.saveProfile(context, mVpnProfile);
			vpl.saveProfileList(context);
		}
	    
		/**
		 * Create and attach the VpnProfile to our gateway object
		 */
		protected void createVPNProfile(){
			try {
				ConfigParser cp = new ConfigParser();
				
				JSONObject openvpn_configuration = eip_definition.getJSONObject("openvpn_configuration");
				VpnConfigGenerator vpn_configuration_generator = new VpnConfigGenerator(preferences, openvpn_configuration, mGateway);
				String configuration = vpn_configuration_generator.generate();
				
				cp.parseConfig(new StringReader(configuration));
				mVpnProfile = cp.convertProfile();
				mVpnProfile.mName = mName = locationAsName();
				Log.v(TAG,"Created VPNProfile");
				
			} catch (JSONException e) {
			    // TODO Auto-generated catch block
			    e.printStackTrace();
			} catch (ConfigParser.ConfigParseError e) {
				// FIXME We didn't get a VpnProfile!  Error handling! and log level
				Log.v(TAG,"Error creating VPNProfile");
				e.printStackTrace();
			} catch (IOException e) {
				// FIXME We didn't get a VpnProfile!  Error handling! and log level
				Log.v(TAG,"Error creating VPNProfile");
				e.printStackTrace();
			}
		}

	    
	    public String locationAsName() {
		try {
		    return eip_definition.getJSONObject("locations").getJSONObject(mGateway.getString("location")).getString("name");
		} catch (JSONException e) {
		    Log.v(TAG,"Couldn't read gateway name for profile creation! Returning original name = " + mName);
		    e.printStackTrace();
		    return (mName != null) ? mName : "";
		}
	    }
	}
}
