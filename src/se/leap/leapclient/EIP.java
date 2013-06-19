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
	private static OpenVpnService mVpnService;
	private static boolean mBound = false;
	// Used to store actions to "resume" onServiceConnection
	private static String mPending = null;
	
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
		String action = intent.getAction();
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
			mBound = false;
			
			if (mReceiver != null){
				Bundle resultData = new Bundle();
				resultData.putString(ConfigHelper.REQUEST_TAG, EIP_NOTIFICATION);
				mReceiver.send(Activity.RESULT_CANCELED, resultData);
			}
		}
	
	};
	
	private void isRunning() {
		Bundle resultData = new Bundle();
		resultData.putString(ConfigHelper.REQUEST_TAG, ACTION_IS_EIP_RUNNING);
		int resultCode = Activity.RESULT_CANCELED;
		if (mBound) {
			resultCode = (mVpnService.isRunning()) ? Activity.RESULT_OK : Activity.RESULT_CANCELED;
		} else {
			mPending = ACTION_IS_EIP_RUNNING;
			this.retreiveVpnService();
		}
		
		if (mReceiver != null){
			mReceiver.send(resultCode, resultData);
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
		mPending = ACTION_START_EIP;
	}
	
	private void stopEIP() {
		if (mBound)
			mVpnService.onRevoke();
		else
			OpenVpnManagementThread.stopOpenVPN();
			
		if (mReceiver != null){
			Bundle resultData = new Bundle();
			resultData.putString(ConfigHelper.REQUEST_TAG, ACTION_STOP_EIP);
			mReceiver.send(Activity.RESULT_OK, resultData);
		}
	}

	private void updateEIPService() {
		try {
			eipDefinition = ConfigHelper.getJsonFromSharedPref(ConfigHelper.EIP_SERVICE_KEY);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		updateGateways();
	}
	
	private OVPNGateway selectGateway() {
		// TODO Implement gateway selection logic based on TZ or preferences
		// TODO will also remove "first" from OVPNGateway constructor
		return new OVPNGateway("first");
	}
	
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
	}

	private class OVPNGateway {
		
		private String TAG = "OVPNGateway";
		
		private VpnProfile mVpnProfile;
		private JSONObject mGateway;
		private HashMap<String,Vector<Vector<String>>> options = new HashMap<String, Vector<Vector<String>>>();

		
		// Constructor to load a gateway by name
		private OVPNGateway(String name){
			ProfileManager vpl = ProfileManager.getInstance(context);
			
			try {

				// TODO when implementing gateway selection logic
				if ( name == "first" ) {
					name = vpl.getProfiles().iterator().next().mName;
				}
				
				mVpnProfile = vpl.getProfileByName(name);
				
			} catch (NoSuchElementException e) {
				updateEIPService();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		protected OVPNGateway(JSONObject gateway){

			mGateway = gateway;
			
			// Currently deletes VpnProfile for host, if there already is one, and builds new
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
			
			this.parseOptions();
			this.createVPNProfile();
			
			setUniqueProfileName(vpl);
			vpl.addProfile(mVpnProfile);
			vpl.saveProfile(context, mVpnProfile);
			vpl.saveProfileList(context);
		}
		
		private void setUniqueProfileName(ProfileManager vpl) {
			int i=0;

			String newname;
			try {
				newname = mGateway.getString("host");
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

		private void parseOptions(){
			
			// FIXME move these to a common API (& version) definition place, like ProviderAPI or ConfigHelper
			String common_options = "openvpn_configuration";
			String remote = "ip_address";
			String ports = "ports";
			String protos = "protocols";
			String capabilities = "capabilities";
			
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
				arg.add("remote");
				arg.add(mGateway.getString(remote));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			args.add((Vector<String>) arg.clone());
			options.put("remote", (Vector<Vector<String>>) args.clone() );
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
		
		protected void createVPNProfile(){
			try {
				ConfigParser cp = new ConfigParser();
				cp.setDefinition(options);
				VpnProfile vp = cp.convertProfile();
				mVpnProfile = vp;
				Log.v(TAG,"Created VPNProfile");
			} catch (ConfigParseError e) {
				// FIXME We didn't get a VpnProfile!  Error handling!
				Log.v(TAG,"Error createing VPNProfile");
				e.printStackTrace();
			}
		}
	}

}
