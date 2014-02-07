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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import se.leap.bitmaskclient.R;
import se.leap.openvpn.ConfigParser;
import se.leap.openvpn.ConfigParser.ConfigParseError;
import se.leap.openvpn.LaunchVPN;
import se.leap.openvpn.OpenVpnManagementThread;
import se.leap.openvpn.OpenVpnService;
import se.leap.openvpn.OpenVpnService.LocalBinder;
import se.leap.openvpn.ProfileManager;
import se.leap.openvpn.VpnProfile;
import android.app.Activity;
import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.drm.DrmStore.Action;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;

/**
 * EIP is the abstract base class for interacting with and managing the Encrypted
 * Internet Proxy connection.  Connections are started, stopped, and queried through
 * this IntentService.
 * Contains logic for parsing eip-service.json from the provider, configuring and selecting
 * gateways, and controlling {@link .openvpn.OpenVpnService} connections.
 * 
 * @author Sean Leonard <meanderingcode@aetherislands.net>
 */
public final class EIP extends IntentService {
	
	public final static String AUTHED_EIP = "authed eip";
	public final static String ACTION_START_EIP = "se.leap.bitmaskclient.START_EIP";
	public final static String ACTION_STOP_EIP = "se.leap.bitmaskclient.STOP_EIP";
	public final static String ACTION_UPDATE_EIP_SERVICE = "se.leap.bitmaskclient.UPDATE_EIP_SERVICE";
	public final static String ACTION_IS_EIP_RUNNING = "se.leap.bitmaskclient.IS_RUNNING";
	public final static String EIP_NOTIFICATION = "EIP_NOTIFICATION";
	public final static String ALLOWED_ANON = "allow_anonymous";
	public final static String CERTIFICATE = "cert";
	public final static String PRIVATE_KEY = "private_key";
	public final static String KEY = "eip";
	public final static String PARSED_SERIAL = "eip_parsed_serial";
	public final static String SERVICE_API_PATH = "config/eip-service.json";
	public final static String RECEIVER_TAG = "receiverTag";
	public final static String REQUEST_TAG = "requestTag";
	public final static String TAG = "se.leap.bitmaskclient.EIP";
	
	
	private static Context context;
	private static ResultReceiver mReceiver;
	private static OpenVpnService mVpnService;
	private static boolean mBound = false;
	// Used to store actions to "resume" onServiceConnection
	private static String mPending = null;
	
	private static int parsedEipSerial;
	private static JSONObject eipDefinition = null;
	
	private static OVPNGateway activeGateway = null;

	public EIP(){
		super("LEAPEIP");
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		context = getApplicationContext();
		
		try {
			eipDefinition = ConfigHelper.getJsonFromSharedPref(KEY);
			parsedEipSerial = ConfigHelper.getIntFromSharedPref(PARSED_SERIAL);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.retreiveVpnService();
	}
	
	@Override
	public void onDestroy() {
		unbindService(mVpnServiceConn);
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
	}
	
	/**
	 * Sends an Intent to bind OpenVpnService.
	 * Used when OpenVpnService isn't bound but might be running.
	 */
	private boolean retreiveVpnService() {
		Intent bindIntent = new Intent(this,OpenVpnService.class);
		bindIntent.setAction(OpenVpnService.RETRIEVE_SERVICE);
		return bindService(bindIntent, mVpnServiceConn, BIND_AUTO_CREATE);
	}
	
	private static ServiceConnection mVpnServiceConn = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			LocalBinder binder = (LocalBinder) service;
			mVpnService = binder.getService();
			mBound = true;

			if (mReceiver != null && mPending != null) {

				boolean running = mVpnService.isRunning();
				
				int resultCode = Activity.RESULT_CANCELED;
				
				if (mPending.equals(ACTION_IS_EIP_RUNNING)){
					resultCode = (running) ? Activity.RESULT_OK : Activity.RESULT_CANCELED;

				}
				else if (mPending.equals(ACTION_START_EIP)){
					resultCode = (running) ? Activity.RESULT_OK : Activity.RESULT_CANCELED;
				}
				else if (mPending.equals(ACTION_STOP_EIP)){
					resultCode = (running) ? Activity.RESULT_CANCELED
							: Activity.RESULT_OK;
					}
				Bundle resultData = new Bundle();
				resultData.putString(REQUEST_TAG, ACTION_IS_EIP_RUNNING);
				mReceiver.send(resultCode, resultData);
				
				mPending = null;
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mBound = false;
			
			if (mReceiver != null){
				Bundle resultData = new Bundle();
				resultData.putString(REQUEST_TAG, EIP_NOTIFICATION);
				mReceiver.send(Activity.RESULT_CANCELED, resultData);
			}
		}
		
	
	};
	
	/**
	 * Attempts to determine if OpenVpnService has an established VPN connection
	 * through the bound ServiceConnection.  If there is no bound service, this
	 * method will attempt to bind a running OpenVpnService and send
	 * <code>Activity.RESULT_CANCELED</code> to the ResultReceiver that made the
	 * request.
	 * Note: If the request to bind OpenVpnService is successful, the ResultReceiver
	 * will be notified in {@link onServiceConnected()}
	 */
	
	  private void isRunning() {
          Bundle resultData = new Bundle();
          resultData.putString(REQUEST_TAG, ACTION_IS_EIP_RUNNING);
          int resultCode = Activity.RESULT_CANCELED;
          if (mBound) {
                  resultCode = (mVpnService.isRunning()) ? Activity.RESULT_OK : Activity.RESULT_CANCELED;
                  
                  if (mReceiver != null){
                          mReceiver.send(resultCode, resultData);
                  }
          } else {
                  mPending = ACTION_IS_EIP_RUNNING;
                 boolean retrieved_vpn_service = retreiveVpnService();
                 try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                 boolean running = false;
                 try {
                	running = mVpnService.isRunning();
                 } catch (NullPointerException e){
                	 e.printStackTrace();
                 }
                 
                 if (retrieved_vpn_service && running && mReceiver != null){
                	  mReceiver.send(Activity.RESULT_OK, resultData);
                  }
                  else{
                	  mReceiver.send(Activity.RESULT_CANCELED, resultData);
                  }
          }
  }
	
	/**
	 * Initiates an EIP connection by selecting a gateway and preparing and sending an
	 * Intent to {@link se.leap.openvpn.LaunchVPN}
	 */
	private void startEIP() {
		activeGateway = selectGateway();
		
		Intent intent = new Intent(this,LaunchVPN.class);
		intent.setAction(Intent.ACTION_MAIN);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(LaunchVPN.EXTRA_KEY, activeGateway.mVpnProfile.getUUID().toString() );
		intent.putExtra(LaunchVPN.EXTRA_NAME, activeGateway.mVpnProfile.getName() );
		intent.putExtra(RECEIVER_TAG, mReceiver);
		startActivity(intent);
		mPending = ACTION_START_EIP;
	}
	
	/**
	 * Disconnects the EIP connection gracefully through the bound service or forcefully
	 * if there is no bound service.  Sends a message to the requesting ResultReceiver.
	 */
	private void stopEIP() {
		if (mBound)
			mVpnService.onRevoke();
		else
			OpenVpnManagementThread.stopOpenVPN();
			
		if (mReceiver != null){
			Bundle resultData = new Bundle();
			resultData.putString(REQUEST_TAG, ACTION_STOP_EIP);
			mReceiver.send(Activity.RESULT_OK, resultData);
		}
	}

	/**
	 * Loads eip-service.json from SharedPreferences and calls {@link updateGateways()}
	 * to parse gateway definitions.
	 * TODO Implement API call to refresh eip-service.json from the provider
	 */
	private void updateEIPService() {
		try {
			eipDefinition = ConfigHelper.getJsonFromSharedPref(EIP.KEY);
			parsedEipSerial = ConfigHelper.getIntFromSharedPref(PARSED_SERIAL);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(parsedEipSerial == 0) {
			// Delete all vpn profiles
			ProfileManager vpl = ProfileManager.getInstance(context);
			Collection<VpnProfile> profiles = vpl.getProfiles();
			for (VpnProfile profile : profiles){
				vpl.removeProfile(context, profile);
			}
		}
		if (eipDefinition.optInt("serial") > parsedEipSerial)
			updateGateways();
	}
	
	/**
	 * Choose a gateway to connect to based on timezone from system locale data
	 * 
	 * @return The gateway to connect to
	 */
	private OVPNGateway selectGateway() {
		// TODO Implement gateway selection logic based on TZ or preferences
		// TODO Implement search through gateways loaded from SharedPreferences
		// TODO Remove String arg constructor in favor of findGatewayByName(String)
		return new OVPNGateway("first");
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
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		for ( int i=0 ; i < gatewaysDefined.length(); i++ ){
			
			JSONObject gw = null;
			
			try {
				gw = gatewaysDefined.getJSONObject(i);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			try {
				if ( gw.getJSONObject("capabilities").getJSONArray("transport").toString().contains("openvpn") ){
					new OVPNGateway(gw);
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		ConfigHelper.saveSharedPref(PARSED_SERIAL, eipDefinition.optInt(Provider.API_RETURN_SERIAL));
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
				if ( mName == "first" ) {
					mName = vpl.getProfiles().iterator().next().mName;
				}
				
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
				try {
					if ( p.mName.contains( gateway.getString("host") ) )
						it.remove();
						vpl.removeProfile(context, p);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			this.parseOptions();
			this.createVPNProfile();
			
			setUniqueProfileName(vpl);
			vpl.addProfile(mVpnProfile);
			vpl.saveProfile(context, mVpnProfile);
			vpl.saveProfileList(context);
		}
		
		/**
		 * Attempts to create a unique profile name from the hostname of the gateway
		 * 
		 * @param profileManager
		 */
		private void setUniqueProfileName(ProfileManager profileManager) {
			int i=0;

			String newname;
			try {
				newname = mGateway.getString("host");
				while(profileManager.getProfileByName(newname)!=null) {
					i++;
					if(i==1)
						newname = getString(R.string.converted_profile);
					else
						newname = getString(R.string.converted_profile_i,i);
				}

				mVpnProfile.mName=newname;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				Log.v(TAG,"Couldn't read gateway name for profile creation!");
				e.printStackTrace();
			}
		}

		/**
		 * FIXME This method is really the outline of the refactoring needed in se.leap.openvpn.ConfigParser 
		 */
		private void parseOptions(){
			
			// FIXME move these to a common API (& version) definition place, like ProviderAPI or ConfigHelper
			String common_options = "openvpn_configuration";
			String remote = "ip_address";
			String ports = "ports";
			String protos = "protocols";
			String capabilities = "capabilities";
			String location_key = "location";
			String locations = "locations";
			
			Vector<String> arg = new Vector<String>();
			Vector<Vector<String>> args = new Vector<Vector<String>>();
			
			try {
				JSONObject def = (JSONObject) eipDefinition.get(common_options);
				Iterator keys = def.keys();
				Vector<Vector<String>> value = new Vector<Vector<String>>();
				while ( keys.hasNext() ){
					String key = keys.next().toString();
					
					arg.add(key);
					for ( String word : def.getString(key).split(" ") )
						arg.add(word);
					value.add( (Vector<String>) arg.clone() );
					options.put(key, (Vector<Vector<String>>) value.clone());
					value.clear();
					arg.clear();
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			try {
				arg.add(remote);
				arg.add(mGateway.getString(remote));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			args.add((Vector<String>) arg.clone());
			options.put("remote", (Vector<Vector<String>>) args.clone() );
			arg.clear();
			args.clear();


			
			try {
				
				arg.add(location_key);
				String locationText = "";
				locationText = eipDefinition.getJSONObject(locations).getJSONObject(mGateway.getString(location_key)).getString("name");		
				arg.add(locationText);

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			args.add((Vector<String>) arg.clone());
			options.put("location", (Vector<Vector<String>>) args.clone() );

			arg.clear();
			args.clear();
			JSONArray protocolsJSON = null;
			arg.add("proto");
			try {
				protocolsJSON = mGateway.getJSONObject(capabilities).getJSONArray(protos);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Vector<String> protocols = new Vector<String>();
			for ( int i=0; i<protocolsJSON.length(); i++ )
				protocols.add(protocolsJSON.optString(i));
			if ( protocols.contains("udp"))
				arg.add("udp");
			else if ( protocols.contains("tcp"))
				arg.add("tcp");
			args.add((Vector<String>) arg.clone());
			options.put("proto", (Vector<Vector<String>>) args.clone());
			arg.clear();
			args.clear();
			
			
			String port = null;
			arg.add("port");
			try {
				port = mGateway.getJSONObject(capabilities).getJSONArray(ports).optString(0);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			arg.add(port);
			args.add((Vector<String>) arg.clone());
			options.put("port", (Vector<Vector<String>>) args.clone());
			args.clear();
			arg.clear();
		}
		
		/**
		 * Create and attach the VpnProfile to our gateway object
		 */
		protected void createVPNProfile(){
			try {
				ConfigParser cp = new ConfigParser();
				cp.setDefinition(options);
				VpnProfile vp = cp.convertProfile();
				mVpnProfile = vp;
				Log.v(TAG,"Created VPNProfile");
			} catch (ConfigParseError e) {
				// FIXME We didn't get a VpnProfile!  Error handling! and log level
				Log.v(TAG,"Error createing VPNProfile");
				e.printStackTrace();
			}
		}
	}

}
