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
import android.os.ResultReceiver;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import de.blinkt.openvpn.LaunchVPN;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.Connection;
import de.blinkt.openvpn.core.ProfileManager;
import se.leap.bitmaskclient.Dashboard;
import se.leap.bitmaskclient.EipFragment;
import se.leap.bitmaskclient.Provider;

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
    private static List<Gateway> gateways = new ArrayList<>();
    private static ProfileManager profile_manager;
    private static Gateway gateway;
    
	public EIP(){
		super(TAG);
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		context = getApplicationContext();
        preferences = getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE);

		profile_manager = ProfileManager.getInstance(context);
		eip_definition = eipDefinitionFromPreferences();
        if(gateways.isEmpty())
            gateways = gatewaysFromPreferences();
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
	Log.d(TAG, "Connecting to " + gateway.getProfile().getUUIDString());
	if(gateway != null && gateway.getProfile() != null) {
	    mReceiver = EipFragment.getReceiver();
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
	Intent intent = new Intent(this,LaunchVPN.class);
	intent.setAction(Intent.ACTION_MAIN);
	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	intent.putExtra(LaunchVPN.EXTRA_NAME, gateway.getProfile().getName());
        Log.d(TAG, gateway.getProfile().mClientCertFilename);
        Log.d(TAG, gateway.getProfile().mClientKeyFilename);
	intent.putExtra(LaunchVPN.EXTRA_HIDELOG, true);
	startActivity(intent);
    }

    private void stopEIP() {
	EipStatus eip_status = EipStatus.getInstance();
	Log.d(TAG, "stopEip(): eip is connected? " + eip_status.isConnected());
	int result_code = Activity.RESULT_CANCELED;
	if(eip_status.isConnected() || eip_status.isConnecting())
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
        if(eip_definition != null)
            updateGateways();
	tellToReceiver(ACTION_UPDATE_EIP_SERVICE, Activity.RESULT_OK);
    }

    private JSONObject eipDefinitionFromPreferences() {
	try {
	    String eip_definition_string = preferences.getString(KEY, "");
	    if(!eip_definition_string.isEmpty()) {
		return new JSONObject(eip_definition_string);
	    }
	} catch (JSONException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
        return null;
    }

    private List<Gateway> gatewaysFromPreferences() {
        List<Gateway> result;

        String gateways_string = preferences.getString(Gateway.TAG, "");
        Log.d(TAG, "Recovering gateways: " + gateways_string);
        Type type_list_gateways = new TypeToken<ArrayList<Gateway>>() {}.getType();
        result = gateways_string.isEmpty() ?
                new ArrayList<Gateway>()
                : (List<Gateway>) new Gson().fromJson(gateways_string, type_list_gateways);
	Log.d(TAG, "Gateways from preferences = " + result.size());
        preferences.edit().remove(Gateway.TAG);
        return result;
    }
	
    /**
     * Walk the list of gateways defined in eip-service.json and parse them into
     * Gateway objects.
     */
    private void updateGateways(){
	try {
            JSONArray gatewaysDefined = eip_definition.getJSONArray("gateways");
            for (int i = 0; i < gatewaysDefined.length(); i++) {
                JSONObject gw = gatewaysDefined.getJSONObject(i);
                if (isOpenVpnGateway(gw)) {
                    JSONObject secrets = secretsConfiguration();
                    Gateway aux = new Gateway(eip_definition, secrets, gw);
		    Log.d(TAG, "Possible new gateway: " + aux.getProfile().getUUIDString());
                    if(!containsProfileWithSecrets(aux.getProfile())) {
                        addGateway(aux);
                    }
                }
            }
	    gatewaysToPreferences();
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


    private JSONObject secretsConfiguration() {
        JSONObject result = new JSONObject();
        try {
            result.put(Provider.CA_CERT, preferences.getString(Provider.CA_CERT, ""));
            result.put(Constants.PRIVATE_KEY, preferences.getString(Constants.PRIVATE_KEY, ""));
            result.put(Constants.CERTIFICATE, preferences.getString(Constants.CERTIFICATE, ""));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    private void addGateway(Gateway gateway) {
        VpnProfile profile = gateway.getProfile();
        removeGateway(gateway);

	profile_manager.addProfile(profile);
        profile_manager.saveProfile(context, profile);
        profile_manager.saveProfileList(context);

	gateways.add(gateway);
        Log.d(TAG, "Gateway added: " + gateway.getProfile().getUUIDString());
    }

    private void removeGateway(Gateway gateway) {
        VpnProfile profile = gateway.getProfile();
        removeDuplicatedProfile(profile);
        removeDuplicatedGateway(profile);
    }

    private void removeDuplicatedProfile(VpnProfile original) {
        if(containsProfile(original)) {
            VpnProfile remove = duplicatedProfile(original);
            profile_manager.removeProfile(context, remove);
	    Log.d(TAG, "Removing profile " + remove.getUUIDString());
	}if(containsProfile(original)) removeDuplicatedProfile(original);
    }

    private boolean containsProfile(VpnProfile profile) {
        Collection<VpnProfile> profiles = profile_manager.getProfiles();
        for(VpnProfile aux : profiles) {
            if (sameConnections(profile.mConnections, aux.mConnections)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsProfileWithSecrets(VpnProfile profile) {
	boolean result = false;
	
        if(containsProfile(profile)) {
	    Collection<VpnProfile> profiles = profile_manager.getProfiles();
	    for(VpnProfile aux : profiles) {
		result = result == false ?
		    sameConnections(profile.mConnections, aux.mConnections)
                    && profile.mClientCertFilename.equalsIgnoreCase(aux.mClientCertFilename)
                    && profile.mClientKeyFilename.equalsIgnoreCase(aux.mClientKeyFilename)
		    : true;
	    }
	}
        return result;
    }
    
    private VpnProfile duplicatedProfile(VpnProfile profile) {
        VpnProfile duplicated = null;
        Collection<VpnProfile> profiles = profile_manager.getProfiles();
        for(VpnProfile aux : profiles) {
            if (sameConnections(profile.mConnections, aux.mConnections)) {
                duplicated = aux;
            }
        }
        if(duplicated != null) return duplicated;
        else throw new NoSuchElementException(profile.getName());
    }

    private boolean sameConnections(Connection[] c1, Connection[] c2) {
        int same_connections = 0;
        for(Connection c1_aux : c1) {
            for(Connection c2_aux : c2)
                if(c2_aux.mServerName.equals(c1_aux.mServerName)) {
                    same_connections++;
                    break;
                }
        }
        return c1.length == c2.length && c1.length == same_connections;

    }

    private void removeDuplicatedGateway(VpnProfile profile) {
        Iterator<Gateway> it = gateways.iterator();
        List<Gateway> gateways_to_remove = new ArrayList<>();
        while(it.hasNext()) {
            Gateway aux = it.next();
            if(sameConnections(aux.getProfile().mConnections, profile.mConnections)) {
                gateways_to_remove.add(aux);
		Log.d(TAG, "Removing gateway " + aux.getProfile().getUUIDString());
	    }
        }
        gateways.removeAll(gateways_to_remove);
    }

    private void gatewaysToPreferences() {
        Type type_list_gateways = new TypeToken<List<Gateway>>() {}.getType();
        String gateways_string = new Gson().toJson(gateways, type_list_gateways);
        Log.d(TAG, "Saving gateways: " + gateways_string);
        preferences.edit().putString(Gateway.TAG, gateways_string).apply();
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
