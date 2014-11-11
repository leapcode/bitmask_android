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

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;
import de.blinkt.openvpn.LaunchVPN;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.activities.DisconnectVPN;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.ConfigParser.ConfigParseError;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VpnStatus.ConnectionStatus;
import java.io.IOException;
import java.io.StringReader;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import se.leap.bitmaskclient.Dashboard;
import se.leap.bitmaskclient.Provider;
import se.leap.bitmaskclient.R;

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
	
	public final static String AUTHED_EIP = "authed eip";
	public final static String ACTION_CHECK_CERT_VALIDITY = "se.leap.bitmaskclient.CHECK_CERT_VALIDITY";
	public final static String ACTION_START_EIP = "se.leap.bitmaskclient.START_EIP";
	public final static String ACTION_STOP_EIP = "se.leap.bitmaskclient.STOP_EIP";
	public final static String ACTION_UPDATE_EIP_SERVICE = "se.leap.bitmaskclient.UPDATE_EIP_SERVICE";
	public final static String ACTION_IS_EIP_RUNNING = "se.leap.bitmaskclient.IS_RUNNING";
	public final static String ACTION_REBUILD_PROFILES = "se.leap.bitmaskclient.REBUILD_PROFILES";
	public final static String EIP_NOTIFICATION = "EIP_NOTIFICATION";
    	public final static String STATUS = "eip status";
    	public final static String DATE_FROM_CERTIFICATE = "date from certificate";
	public final static String ALLOWED_ANON = "allow_anonymous";
	public final static String ALLOWED_REGISTERED = "allow_registration";
	public final static String CERTIFICATE = "cert";
	public final static String PRIVATE_KEY = "private_key";
	public final static String KEY = "eip";
	public final static String PARSED_SERIAL = "eip_parsed_serial";
	public final static String SERVICE_API_PATH = "config/eip-service.json";
	public final static String RECEIVER_TAG = "receiverTag";
	public final static String REQUEST_TAG = "requestTag";
    public final static String TAG = EIP.class.getSimpleName();
    private static SharedPreferences preferences;

	private static Context context;
	private static ResultReceiver mReceiver;
	private static boolean mBound = false;
	
	private static int parsedEipSerial;
	private static JSONObject eipDefinition = null;
	
	private static OVPNGateway activeGateway = null;
    
    protected static ConnectionStatus lastConnectionStatusLevel;
    protected static boolean mIsDisconnecting = false;
    protected static boolean mIsStarting = false;

    public static SimpleDateFormat certificate_date_format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
	public EIP(){
		super("LEAPEIP");
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		context = getApplicationContext();

		preferences = getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE);
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
	else if ( action == ACTION_REBUILD_PROFILES )
	    updateGateways();
    }
	
    /**
     * Initiates an EIP connection by selecting a gateway and preparing and sending an
     * Intent to {@link se.leap.openvpn.LaunchVPN}.
     * It also sets up early routes.
     */
    private void startEIP() {
	activeGateway = selectGateway();
	    
	if(activeGateway != null && activeGateway.mVpnProfile != null) {
	    mReceiver = EipServiceFragment.getReceiver();
	    launchActiveGateway();
	}
        earlyRoutes();
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
    
    /**
     * Choose a gateway to connect to based on timezone from system locale data
     * 
     * @return The gateway to connect to
     */
    private OVPNGateway selectGateway() {
	String closest_location = closestGateway();
	String chosen_host = chooseHost(closest_location);

	return new OVPNGateway(chosen_host);
    }

    private String closestGateway() {
	TreeMap<Integer, Set<String>> offsets = calculateOffsets();
	return offsets.isEmpty() ? "" : offsets.firstEntry().getValue().iterator().next();
    }

    private TreeMap<Integer, Set<String>> calculateOffsets() {
	TreeMap<Integer, Set<String>> offsets = new TreeMap<Integer, Set<String>>();
	
	int localOffset = Calendar.getInstance().get(Calendar.ZONE_OFFSET) / 3600000;
	
	JSONObject locations = availableLocations();
	Iterator<String> locations_names = locations.keys();
	while(locations_names.hasNext()) {
	    try {
		String location_name = locations_names.next();
		JSONObject location = locations.getJSONObject(location_name);

		int dist = timezoneDistance(localOffset, location.optInt("timezone"));

		Set<String> set = (offsets.get(dist) != null) ?
		    offsets.get(dist) : new HashSet<String>();
		
		set.add(location_name);
		offsets.put(dist, set);
	    } catch (JSONException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }	    
	}
	
	return offsets;
    }

    private JSONObject availableLocations() {
	JSONObject locations = null;
	try {
	    if(eipDefinition == null) updateEIPService();
	    locations = eipDefinition.getJSONObject("locations");
	} catch (JSONException e1) {
	    // TODO Auto-generated catch block
	    e1.printStackTrace();
	}

	return locations;
    }

    private int timezoneDistance(int local_timezone, int remote_timezone) {
	// Distance along the numberline of Prime Meridian centric, assumes UTC-11 through UTC+12
	int dist = Math.abs(local_timezone - remote_timezone);
	
	// Farther than 12 timezones and it's shorter around the "back"
	if (dist > 12)
	    dist = 12 - (dist -12);  // Well i'll be.  Absolute values make equations do funny things.
	
	return dist;
    }

    private String chooseHost(String location) {
	String chosen_host = "";
	try {
	    JSONArray gateways = eipDefinition.getJSONArray("gateways");
	    for (int i = 0; i < gateways.length(); i++) {
		JSONObject gw = gateways.getJSONObject(i);
		if ( gw.getString("location").equalsIgnoreCase(location) || location.isEmpty()){
		    chosen_host = eipDefinition.getJSONObject("locations").getJSONObject(gw.getString("location")).getString("name");
		    break;
		}
	    }
	} catch (JSONException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	return chosen_host;
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
	    lastConnectionStatusLevel = ConnectionStatus.UNKNOWN_LEVEL; // Wait for the decision of the user
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
	int resultCode = Activity.RESULT_CANCELED;
	boolean is_connected = isConnected();

	resultCode = (is_connected) ? Activity.RESULT_OK : Activity.RESULT_CANCELED;

	tellToReceiver(ACTION_IS_EIP_RUNNING, resultCode);
    }
    
    protected static boolean isConnected() {	
	return lastConnectionStatusLevel != null && lastConnectionStatusLevel.equals(ConnectionStatus.LEVEL_CONNECTED) && !mIsDisconnecting;
    }

	/**
	 * Loads eip-service.json from SharedPreferences and calls {@link updateGateways()}
	 * to parse gateway definitions.
	 * TODO Implement API call to refresh eip-service.json from the provider
	 */
	private void updateEIPService() {
		try {
		    String eip_definition_string = preferences.getString(KEY, "");
		    if(eip_definition_string.isEmpty() == false) {
			eipDefinition = new JSONObject(eip_definition_string);
		    }
		    parsedEipSerial = preferences.getInt(PARSED_SERIAL, 0);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(parsedEipSerial == 0) {
		    deleteAllVpnProfiles();
		}
		if (eipDefinition != null && eipDefinition.optInt("serial") >= parsedEipSerial)
			updateGateways();
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
		    if(eipDefinition == null) updateEIPService();
			gatewaysDefined = eipDefinition.getJSONArray("gateways");		
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
		preferences.edit().putInt(PARSED_SERIAL, eipDefinition.optInt(Provider.API_RETURN_SERIAL)).commit();
	}

    private void checkCertValidity() {
	String certificate = preferences.getString(CERTIFICATE, "");
	checkCertValidity(certificate);
    }

    private void checkCertValidity(String certificate_string) {
	if(!certificate_string.isEmpty()) {
	    X509Certificate certificate = ConfigHelper.parseX509CertificateFromString(certificate_string);
	    
	    Calendar offset_date = calculateOffsetCertificateValidity(certificate);
	    Bundle result = new Bundle();
	    result.putString(REQUEST_TAG, ACTION_CHECK_CERT_VALIDITY);
	    try {
		Log.d(TAG, "offset_date = " + offset_date.getTime().toString());
		certificate.checkValidity(offset_date.getTime());
		mReceiver.send(Activity.RESULT_OK, result);
		Log.d(TAG, "Valid certificate");
	    } catch(CertificateExpiredException e) {
		mReceiver.send(Activity.RESULT_CANCELED, result);
		Log.d(TAG, "Updating certificate");
	    } catch(CertificateNotYetValidException e) {
		mReceiver.send(Activity.RESULT_CANCELED, result);
	    }
	}
    }

    private Calendar calculateOffsetCertificateValidity(X509Certificate certificate) {
	String current_date = certificate_date_format.format(Calendar.getInstance().getTime()).toString();
	    
	String date_string = preferences.getString(DATE_FROM_CERTIFICATE, current_date);

	Calendar offset_date = Calendar.getInstance();
	try {
	    Date date = certificate_date_format.parse(date_string);
	    long difference = Math.abs(date.getTime() - certificate.getNotAfter().getTime())/2;
	    long current_date_millis = offset_date.getTimeInMillis();
	    offset_date.setTimeInMillis(current_date_millis + difference);
	    Log.d(TAG, "certificate not after = " + certificate.getNotAfter());
	} catch(ParseException e) {
	    e.printStackTrace();
	}

	return offset_date;
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
				
				JSONObject openvpn_configuration = eipDefinition.getJSONObject("openvpn_configuration");
				VpnConfigGenerator vpn_configuration_generator = new VpnConfigGenerator(preferences, openvpn_configuration, mGateway);
				String configuration = vpn_configuration_generator.generate();
				
				cp.parseConfig(new StringReader(configuration));
				mVpnProfile = cp.convertProfile();
				mVpnProfile.mName = mName = locationAsName();
				Log.v(TAG,"Created VPNProfile");
				
			} catch (JSONException e) {
			    // TODO Auto-generated catch block
			    e.printStackTrace();
			} catch (ConfigParseError e) {
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
		    return eipDefinition.getJSONObject("locations").getJSONObject(mGateway.getString("location")).getString("name");
		} catch (JSONException e) {
		    Log.v(TAG,"Couldn't read gateway name for profile creation! Returning original name = " + mName);
		    e.printStackTrace();
		    return (mName != null) ? mName : "";
		}
	    }
	}
}
