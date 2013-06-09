package se.leap.leapclient;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import se.leap.openvpn.ConfigParser;
import se.leap.openvpn.ConfigParser.ConfigParseError;
import se.leap.openvpn.LaunchVPN;
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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;

/**
 * @author Sean Leonard <meanderingcode@aetherislands.net>
 *
 */
public final class EIP extends IntentService {
	
	public final static String ACTION_START_EIP = "se.leap.leapclient.START_EIP";
	public final static String ACTION_STOP_EIP = "se.leap.leapclient.STOP_EIP";
	public final static String ACTION_UPDATE_EIP_SERVICE = "se.leap.leapclient.UPDATE_EIP_SERVICE";
	
	public final static String ACTION_IS_EIP_RUNNING = "se.leap.leapclient.IS_RUNNING";
	
	public final static String EIP_NOTIFICATION = "EIP_NOTIFICATION";
	
	private static Context context;
	private static ResultReceiver mReceiver;
	// Binder to OpenVpnService for comm ops
	private static OpenVpnService mVpnService;
	private static boolean mBound = false;
	// Used to store actions to "resume" onServiceConnection
	private static String mPending = null;
	
	// Represents our Provider's eip-service.json
	private static JSONObject eipDefinition = null;
	
	// Our active gateway
	private static OVPNGateway activeGateway = null;

	public EIP(){
		super("LEAPEIP");
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		context = getApplicationContext();
		
		// Inflate our eip-service.json data
		try {
			eipDefinition = ConfigHelper.getJsonFromSharedPref(ConfigHelper.EIP_SERVICE_KEY);
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
		// Get our action from the Intent
		String action = intent.getAction();
		// Get the ResultReceiver, if any
		mReceiver = intent.getParcelableExtra(ConfigHelper.RECEIVER_TAG);
		
		if ( action == ACTION_IS_EIP_RUNNING )
			this.isRunning();
		if ( action == ACTION_UPDATE_EIP_SERVICE )
			this.updateEIPService();
		else if ( action == ACTION_START_EIP )
			this.startEIP();
		else if ( action == ACTION_STOP_EIP )
			this.stopEIP();
	}
	
	private void retreiveVpnService() {
		Intent bindIntent = new Intent(this,OpenVpnService.class);
		bindIntent.setAction(OpenVpnService.RETRIEVE_SERVICE);
		bindService(bindIntent, mVpnServiceConn, 0);
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
				
				if (mPending.equals(ACTION_IS_EIP_RUNNING))
					resultCode = (running) ? Activity.RESULT_OK : Activity.RESULT_CANCELED;
				if (mPending.equals(ACTION_START_EIP))
					resultCode = (running) ? Activity.RESULT_OK : Activity.RESULT_CANCELED;
				else if (mPending.equals(ACTION_STOP_EIP))
					resultCode = (running) ? Activity.RESULT_CANCELED
							: Activity.RESULT_OK;
				Bundle resultData = new Bundle();
				resultData.putString(ConfigHelper.REQUEST_TAG, EIP_NOTIFICATION);
				mReceiver.send(resultCode, resultData);
				
				mPending = null;
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// XXX tell mReceiver!!
			mBound = false;
			
			if (mReceiver != null){
				Bundle resultData = new Bundle();
				resultData.putString(ConfigHelper.REQUEST_TAG, EIP_NOTIFICATION);
				mReceiver.send(Activity.RESULT_CANCELED, resultData);
			}
		}
	
	};
	
	private void isRunning() {
		// TODO I don't like that whatever requested this never receives a response
		//		if OpenVpnService has not been START_SERVICE, though the one place this is used that's okay
		if (mBound) {
			if (mReceiver != null){
				Bundle resultData = new Bundle();
				resultData.putString(ConfigHelper.REQUEST_TAG, ACTION_IS_EIP_RUNNING);
				int resultCode = (mVpnService.isRunning()) ? Activity.RESULT_OK : Activity.RESULT_CANCELED;
				mReceiver.send(resultCode, resultData);
			}
		} else {
			mPending = ACTION_IS_EIP_RUNNING;
			this.retreiveVpnService();
		}
	}

	private void startEIP() {
		if (activeGateway==null)
			activeGateway = selectGateway();
		
		Intent intent = new Intent(this,LaunchVPN.class);
		intent.setAction(Intent.ACTION_MAIN);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(LaunchVPN.EXTRA_KEY, activeGateway.mVpnProfile.getUUID().toString() );
		intent.putExtra(LaunchVPN.EXTRA_NAME, activeGateway.mVpnProfile.getName() );
		intent.putExtra(ConfigHelper.RECEIVER_TAG, mReceiver);
		startActivity(intent);
		// Let's give it 2s to get rolling... TODO there really should be a better way to do this, or it's not needed.
		// Read more code in .openvpn package
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Bind OpenVpnService for comm ops
		if (!mBound){
			mPending = ACTION_START_EIP;
			this.retreiveVpnService();
		} else {
			if (mReceiver != null) {
				Bundle resultData = new Bundle();
				resultData.putString(ConfigHelper.REQUEST_TAG, ACTION_START_EIP);
				mReceiver.send(Activity.RESULT_OK, resultData);
			}
		}
	}
	
	private void stopEIP() {
		if (mBound){
			mVpnService.onRevoke();

			/*if (mReceiver != null){
				Bundle resultData = new Bundle();
				resultData.putString(ConfigHelper.REQUEST_TAG, ACTION_STOP_EIP);
				mReceiver.send(Activity.RESULT_OK, resultData);
			}*/
		} else {
			// TODO If OpenVpnService isn't bound, does that really always mean it's not running?
			//		If it's not running, bindService doesn't work w/o START_SERVICE action, so...
			/*mPending = ACTION_STOP_EIP;
			this.retreiveVpnService();*/
		}
		// Remove this if above comes back
		if (mReceiver != null){
			Bundle resultData = new Bundle();
			resultData.putString(ConfigHelper.REQUEST_TAG, ACTION_STOP_EIP);
			mReceiver.send(Activity.RESULT_OK, resultData);
		}
	}

	private void updateEIPService() {
		// TODO this will also fetch new eip-service.json
		try {
			eipDefinition = ConfigHelper.getJsonFromSharedPref(ConfigHelper.EIP_SERVICE_KEY);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		updateGateways();
	}
	
	private OVPNGateway selectGateway() {
		// TODO Logic, yay!
		return new OVPNGateway("first");
	}
	
	private void updateGateways(){
		JSONArray gatewaysDefined = null;
		
		// Get our list of gateways
		try {
			gatewaysDefined = eipDefinition.getJSONArray("gateways");
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// Walk the list of gateways and inflate them to VPNProfiles
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
					// We have an openvpn gateway!
					// Now build VPNProfiles and save their UUIDs
					// TODO create multiple profiles for each gateway to allow trying e.g. different ports when connections don't complete
					new OVPNGateway(gw);
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private class OVPNGateway {
		
		// Log tag
		private String TAG = "OVPNGateway";
		
		// The actual VPN Profile object
		private VpnProfile mVpnProfile;
		// Our gateway definition from eip-service.json
		private JSONObject gateway;
		// This holds our OpenVPN options for creating the VPNProfile
		// Options get put here in the form that se.leap.openvpn.ConfigParser wants TODO will be gone w/ rewrite
		private HashMap<String,Vector<Vector<String>>> options = new HashMap<String, Vector<Vector<String>>>();

		
		// Constructor to load a gateway by name
		private OVPNGateway(String name){
			ProfileManager vpl = ProfileManager.getInstance(context);
			
			try {

				// FIXME ha, this got funny..it will get smart once i'm further...
				if ( name == "first" ) {
					name = vpl.getProfiles().iterator().next().mName;
				}
				
				mVpnProfile = vpl.getProfileByName(name);
				
			} catch (NoSuchElementException e) {
				
				// The gateway we were looking for is not in ProfileList!
				updateEIPService();
				
				// TODO prompt user to fix config error
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// Constructor to create a gateway by definition
		protected OVPNGateway(JSONObject gw){
			// TODO We're going to build 1 profile per gateway, but eventually several

			gateway = gw;
			
			// Delete VpnProfile for host, if there already is one
			// FIXME There is a better way to check settings and update them, instead of destroy/rebuild
			// Also, this allows one gateway per hostname entry, so that had better be true from the server!
			// TODO Will we define multiple gateways per host, for variable options?  or change how .openvpn.VpnProfile works?
			ProfileManager vpl = ProfileManager.getInstance(context);
			Collection<VpnProfile> profiles = vpl.getProfiles();
			for (VpnProfile p : profiles){
				try {
					if ( p.mName.contains( gateway.getString("host") ) )
						vpl.removeProfile(context, p);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			// Create options HashMap for se.leap.openvpn.ConfigParser
			this.parseOptions();
			// Now create the VPNProfile
			this.createVPNProfile();
			// Now let's save it in the .openvpn package way
			
			setUniqueProfileName(vpl);
			vpl.addProfile(mVpnProfile);
			vpl.saveProfile(context, mVpnProfile);
			vpl.saveProfileList(context);
		}
		
		private void setUniqueProfileName(ProfileManager vpl) {
			int i=0;

			String newname;
			try {
				newname = gateway.getString("host");
				while(vpl.getProfileByName(newname)!=null) {
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

		// FIXME this whole thing will get rewritten when we modify ConfigParser
		// 			in fact, don't even bother looking, except to debug
		private void parseOptions(){
			// TODO we will want to rewrite se.leap.openvpn.ConfigParser soon to be targeted at our use
			
			// FIXME move these to a common API (& version) definition place, like ProviderAPI or ConfigHelper
			String common_options = "openvpn_configuration";
			String remote = "ip_address";
			String ports = "ports";
			String protos = "protocols";
			String capabilities = "capabilities";
			
			// FIXME Our gateway definition has keys that are not OpenVPN options...
			// We need a hard spec for the eip-service.json and better handling in this class
			// Then we can stop dumping all the "capabilities" key:values into our options for parsing
			
			// Put our common options in
			// FIXME quite ugly.  We don't need the nested vectors, as we're not byte-reading file input, but we haven't rewritten the parser, yet

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
			
			// Now our gateway-specific options
			// to hold 'em the way they're wanted for parsing

			// remote:ip_address
			try {
				arg.add("remote");
				arg.add(gateway.getString(remote));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			args.add((Vector<String>) arg.clone());
			options.put("remote", (Vector<Vector<String>>) args.clone() );
			arg.clear();
			args.clear();
			
			// proto:udp||tcp
			JSONArray protocolsJSON = null;
			arg.add("proto");
			try {
				protocolsJSON = gateway.getJSONObject(capabilities).getJSONArray(protos);
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
			
			
			// Now ports...picking one 'cause i say so'... TODO we should have multiple profiles?...
			String port = null;
			arg.add("port");
			try {
				port = gateway.getJSONObject(capabilities).getJSONArray(ports).optString(0);
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
		
		protected void createVPNProfile(){
			// TODO take data from eip-service.json for openvpn gateway definitions and create VPNProfile for each
			try {
				ConfigParser cp = new ConfigParser();
				cp.setDefinition(options);
				VpnProfile vp = cp.convertProfile();
				mVpnProfile = vp;
				Log.v(TAG,"Created VPNProfile");
			} catch (ConfigParseError e) {
				// FIXME Being here is bad because we didn't get a VpnProfile!
				Log.v(TAG,"Error createing VPNProfile");
				e.printStackTrace();
			}
		}
	}

}
