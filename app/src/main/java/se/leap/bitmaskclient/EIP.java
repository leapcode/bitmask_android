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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.drm.DrmStore.Action;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;
import de.blinkt.openvpn.LaunchVPN;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.activities.DisconnectVPN;
import de.blinkt.openvpn.core.ConfigParser.ConfigParseError;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.OpenVpnManagementThread;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VpnStatus.ConnectionStatus;
import java.io.IOException;
import java.io.StringReader;
import java.lang.StringBuffer;
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
import se.leap.bitmaskclient.VoidVpnService;

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
	public final static String TAG = "se.leap.bitmaskclient.EIP";

    public final static SimpleDateFormat certificate_date_format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
    
	private static Context context;
	private static ResultReceiver mReceiver;
	private static boolean mBound = false;
	// Used to store actions to "resume" onServiceConnection
	private static String mPending = null;
	
	private static int parsedEipSerial;
	private static JSONObject eipDefinition = null;
	
	private static OVPNGateway activeGateway = null;
    
    protected static ConnectionStatus lastConnectionStatusLevel;
    protected static boolean mIsDisconnecting = false;
    protected static boolean mIsStarting = false;

	public EIP(){
		super("LEAPEIP");
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		context = getApplicationContext();
		
		updateEIPService();
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
		
		if ( action == ACTION_IS_EIP_RUNNING )
			this.isRunning();
		if ( action == ACTION_UPDATE_EIP_SERVICE )
			this.updateEIPService();
		else if ( action == ACTION_START_EIP )
			this.startEIP();
		else if ( action == ACTION_STOP_EIP )
			this.stopEIP();
		else if ( action == ACTION_CHECK_CERT_VALIDITY )
			this.checkCertValidity();
		else if ( action == ACTION_REBUILD_PROFILES ) {
		    this.updateGateways();		       
		}
	}
	
	/**
	 * Checks the last stored status notified by ics-openvpn
	 * Sends <code>Activity.RESULT_CANCELED</code> to the ResultReceiver that made the
	 * request if it's not connected, <code>Activity.RESULT_OK</code> otherwise.
	 */
	
	  private void isRunning() {
	      Bundle resultData = new Bundle();
	      resultData.putString(REQUEST_TAG, ACTION_IS_EIP_RUNNING);
	      int resultCode = Activity.RESULT_CANCELED;
	      boolean is_connected = isConnected();

	      resultCode = (is_connected) ? Activity.RESULT_OK : Activity.RESULT_CANCELED;
                  
	      if (mReceiver != null){
		  mReceiver.send(resultCode, resultData);
	      }

	      Log.d(TAG, "isRunning() = " + is_connected);
	  }
	
	/**
	 * Initiates an EIP connection by selecting a gateway and preparing and sending an
	 * Intent to {@link se.leap.openvpn.LaunchVPN}
	 */
	private void startEIP() {
	    activeGateway = selectGateway();
	    earlyRoutes();
	    if(activeGateway != null && activeGateway.mVpnProfile != null) {
		launchActiveGateway();
	    }
	}

    private void earlyRoutes() {
	VoidVpnService voidVpn = new VoidVpnService();
	voidVpn.setUp(context);
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
	mPending = ACTION_START_EIP;
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

		if (mReceiver != null){
			Bundle resultData = new Bundle();
			resultData.putString(REQUEST_TAG, ACTION_STOP_EIP);
			mReceiver.send(Activity.RESULT_OK, resultData);
		}
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
			eipDefinition = new JSONObject(getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE).getString(KEY, ""));
			parsedEipSerial = getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE).getInt(PARSED_SERIAL, 0);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(parsedEipSerial == 0) {
		    deleteAllVpnProfiles();
		}
		if (eipDefinition != null && eipDefinition.optInt("serial") > parsedEipSerial)
			updateGateways();
	}

    private void deleteAllVpnProfiles() {
	ProfileManager vpl = ProfileManager.getInstance(context);
	VpnProfile[] profiles = (VpnProfile[]) vpl.getProfiles().toArray(new VpnProfile[vpl.getProfiles().size()]);
	for (int current_profile = 0; current_profile < profiles.length; current_profile++){
	    vpl.removeProfile(context, profiles[current_profile]);
	}
    }
	/**
	 * Choose a gateway to connect to based on timezone from system locale data
	 * 
	 * @return The gateway to connect to
	 */
	private OVPNGateway selectGateway() {
		String closestLocation = closestGateway();
		
		JSONArray gateways = null;
		String chosenHost = null;
		try {
			gateways = eipDefinition.getJSONArray("gateways");
			for (int i = 0; i < gateways.length(); i++) {
				JSONObject gw = gateways.getJSONObject(i);
				if ( gw.getString("location").equalsIgnoreCase(closestLocation) || closestLocation.isEmpty()){
					chosenHost = eipDefinition.getJSONObject("locations").getJSONObject(gw.getString("location")).getString("name");
					break;
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return new OVPNGateway(chosenHost);
	}

    private String closestGateway() {
	Calendar cal = Calendar.getInstance();
	int localOffset = cal.get(Calendar.ZONE_OFFSET) / 3600000;
	TreeMap<Integer, Set<String>> offsets = new TreeMap<Integer, Set<String>>();
	JSONObject locationsObjects = null;
	Iterator<String> locations = null;
	try {
	    locationsObjects = eipDefinition.getJSONObject("locations");
	    locations = locationsObjects.keys();
	} catch (JSONException e1) {
	    // TODO Auto-generated catch block
	    e1.printStackTrace();
	}
		
	while (locations.hasNext()) {
	    String locationName = locations.next();
	    JSONObject location = null;
	    try {
		location = locationsObjects.getJSONObject(locationName);
				
		// Distance along the numberline of Prime Meridian centric, assumes UTC-11 through UTC+12
		int dist = Math.abs(localOffset - location.optInt("timezone"));
		// Farther than 12 timezones and it's shorter around the "back"
		if (dist > 12)
		    dist = 12 - (dist -12);  // Well i'll be.  Absolute values make equations do funny things.
				
		Set<String> set = offsets.get(dist);
		if (set == null) set = new HashSet<String>();
		set.add(locationName);
		offsets.put(dist, set);
	    } catch (JSONException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}		
	return offsets.isEmpty() ? "" : offsets.firstEntry().getValue().iterator().next();
    }
	
	/**
	 * Walk the list of gateways defined in eip-service.json and parse them into
	 * OVPNGateway objects.
	 * TODO Store the OVPNGateways (as Serializable) in SharedPreferences
	 */
	private void updateGateways(){
		JSONArray gatewaysDefined = null;
		
		try {
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
		
		getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE).edit().putInt(PARSED_SERIAL, eipDefinition.optInt(Provider.API_RETURN_SERIAL)).commit();
	}

    private void checkCertValidity() {
	String certificate_string = getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE).getString(CERTIFICATE, "");
	if(!certificate_string.isEmpty()) {
	    String date_from_certificate_string = getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE).getString(DATE_FROM_CERTIFICATE, certificate_date_format.format(Calendar.getInstance().getTime()).toString());
	    X509Certificate certificate_x509 = ConfigHelper.parseX509CertificateFromString(certificate_string);

	    Calendar offset_date = Calendar.getInstance();
	    try {
		Date date_from_certificate = certificate_date_format.parse(date_from_certificate_string);
		long difference = Math.abs(date_from_certificate.getTime() - certificate_x509.getNotAfter().getTime())/2;
		long current_date_millis = offset_date.getTimeInMillis();
		offset_date.setTimeInMillis(current_date_millis + difference);
		Log.d(TAG, "certificate not after = " + certificate_x509.getNotAfter());
	    } catch(ParseException e) {
		e.printStackTrace();
	    }
	
	    Bundle result_data = new Bundle();
	    result_data.putString(REQUEST_TAG, ACTION_CHECK_CERT_VALIDITY);
	    try {
		Log.d(TAG, "offset_date = " + offset_date.getTime().toString());
		certificate_x509.checkValidity(offset_date.getTime());
		mReceiver.send(Activity.RESULT_OK, result_data);
		Log.d(TAG, "Valid certificate");
	    } catch(CertificateExpiredException e) {
		mReceiver.send(Activity.RESULT_CANCELED, result_data);
		Log.d(TAG, "Updating certificate");
	    } catch(CertificateNotYetValidException e) {
		mReceiver.send(Activity.RESULT_CANCELED, result_data);
	    }
	}
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
				cp.parseConfig(new StringReader(configFromEipServiceDotJson()));
				cp.parseConfig(new StringReader(caSecretFromSharedPreferences()));
				cp.parseConfig(new StringReader(keySecretFromSharedPreferences()));
				cp.parseConfig(new StringReader(certSecretFromSharedPreferences()));
				cp.parseConfig(new StringReader("remote-cert-tls server"));
				cp.parseConfig(new StringReader("persist-tun"));
				VpnProfile vp = cp.convertProfile();
				//vp.mAuthenticationType=VpnProfile.TYPE_STATICKEYS;
				mVpnProfile = vp;
				mVpnProfile.mName = mName = locationAsName();
				Log.v(TAG,"Created VPNProfile");
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

	    /**
	     * Parses data from eip-service.json to a section of the openvpn config file
	     */
	    private String configFromEipServiceDotJson() {
		String parsed_configuration = "";
		
		String location_key = "location";
		String locations = "locations";

		parsed_configuration += extractCommonOptionsFromEipServiceDotJson();
		parsed_configuration += extractRemotesFromEipServiceDotJson();

		return parsed_configuration;
	    }

	    private String extractCommonOptionsFromEipServiceDotJson() {
		String common_options = "";
		try {
		    String common_options_key = "openvpn_configuration";
		    JSONObject openvpn_configuration = eipDefinition.getJSONObject(common_options_key);
		    Iterator keys = openvpn_configuration.keys();
		    Vector<Vector<String>> value = new Vector<Vector<String>>();
		    while ( keys.hasNext() ){
			String key = keys.next().toString();
					
			common_options += key + " ";
			for ( String word : openvpn_configuration.getString(key).split(" ") )
			    common_options += word + " ";
			common_options += System.getProperty("line.separator");
			
		    }
		} catch (JSONException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}

		common_options += "client" + System.getProperty("line.separator");

		return common_options;
	    }
		
	    
	    private String extractRemotesFromEipServiceDotJson() {
		String remotes = "";
		
		String remote = "ip_address";
		String remote_openvpn_keyword = "remote";
		String ports = "ports";
		String protos = "protocols";
		String capabilities = "capabilities";
		String udp = "udp";
		
		try {
		    JSONArray protocolsJSON = mGateway.getJSONObject(capabilities).getJSONArray(protos);
		    for ( int i=0; i<protocolsJSON.length(); i++ ) {
			String remote_line = remote_openvpn_keyword;
			remote_line += " " + mGateway.getString(remote);
			remote_line += " " + mGateway.getJSONObject(capabilities).getJSONArray(ports).optString(0);
			remote_line += " " + protocolsJSON.optString(i);
			if(remote_line.endsWith(udp))
			    remotes = remotes.replaceFirst(remote_openvpn_keyword, remote_line + System.getProperty("line.separator") + remote_openvpn_keyword);
			else
			    remotes += remote_line;
			remotes += System.getProperty("line.separator");
		    }
		} catch (JSONException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
		
		Log.d(TAG, "remotes = " + remotes);
		return remotes;
	    }

	    private String caSecretFromSharedPreferences() {
		String secret_lines = "";
		SharedPreferences preferences = context.getSharedPreferences(Dashboard.SHARED_PREFERENCES, context.MODE_PRIVATE);
		
		System.getProperty("line.separator");
		secret_lines += "<ca>";
		secret_lines += System.getProperty("line.separator");
		secret_lines += preferences.getString(Provider.CA_CERT, "");
		secret_lines += System.getProperty("line.separator");
		secret_lines += "</ca>";
		
		return secret_lines;
	    }

	    private String keySecretFromSharedPreferences() {
		String secret_lines = "";
		SharedPreferences preferences = context.getSharedPreferences(Dashboard.SHARED_PREFERENCES, context.MODE_PRIVATE);
	
		secret_lines += System.getProperty("line.separator");
		secret_lines +="<key>";
		secret_lines += System.getProperty("line.separator");
		secret_lines += preferences.getString(EIP.PRIVATE_KEY, "");
		secret_lines += System.getProperty("line.separator");
		secret_lines += "</key>";
		secret_lines += System.getProperty("line.separator");

		return secret_lines;
	    }

	    private String certSecretFromSharedPreferences() {
		String secret_lines = "";
		SharedPreferences preferences = context.getSharedPreferences(Dashboard.SHARED_PREFERENCES, context.MODE_PRIVATE);	
		
		secret_lines += System.getProperty("line.separator");
		secret_lines +="<cert>";
		secret_lines += System.getProperty("line.separator");
		secret_lines += preferences.getString(EIP.CERTIFICATE, "");
		secret_lines += System.getProperty("line.separator");
		secret_lines += "</cert>";
		secret_lines += System.getProperty("line.separator");

		return secret_lines;
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
