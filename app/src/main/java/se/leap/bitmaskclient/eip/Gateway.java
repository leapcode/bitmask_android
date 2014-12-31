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
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Iterator;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.ProfileManager;
import se.leap.bitmaskclient.Dashboard;

/**
 * Gateway provides objects defining gateways and their metadata.
 * Each instance contains a VpnProfile for OpenVPN specific data and member
 * variables describing capabilities and location (name)
 * 
 * @author Sean Leonard <meanderingcode@aetherislands.net>
 * @author Parménides GV <parmegv@sdf.org>
 */
public class Gateway {
		
    private String TAG = Gateway.class.getSimpleName();
		
    private String mName;
    private int timezone;
    private JSONObject general_configuration;
    private Context context;
    private VpnProfile mVpnProfile;
    private JSONObject mGateway;
		
    /**
     * Build a gateway object from a JSON OpenVPN gateway definition in eip-service.json
     * and create a VpnProfile belonging to it.
     * 
     * @param gateway The JSON OpenVPN gateway definition to parse
     */
    protected Gateway(JSONObject eip_definition, Context context, JSONObject gateway){

	mGateway = gateway;
	
	this.context = context;
	general_configuration = getGeneralConfiguration(eip_definition);
	timezone = getTimezone(eip_definition);
	mName = locationAsName(eip_definition);

	mVpnProfile = createVPNProfile();
	mVpnProfile.mName = mName;
    }

    private JSONObject getGeneralConfiguration(JSONObject eip_definition) {
	try {
	    return eip_definition.getJSONObject("openvpn_configuration");
	} catch (JSONException e) {
	    return new JSONObject();
	}
    }

    private int getTimezone(JSONObject eip_definition) {
	JSONObject location = getLocationInfo(eip_definition);
	return location.optInt("timezone");
    }

    private String locationAsName(JSONObject eip_definition) {
	JSONObject location = getLocationInfo(eip_definition);
	return location.optString("name");
    }

    private JSONObject getLocationInfo(JSONObject eip_definition) {
	try {
	    JSONObject locations = eip_definition.getJSONObject("locations");

	    return locations.getJSONObject(mGateway.getString("location"));
	} catch (JSONException e) {
	    return new JSONObject();
	}
    }
	    
    /**
     * Create and attach the VpnProfile to our gateway object
     */
    private VpnProfile createVPNProfile(){
	try {
	    ConfigParser cp = new ConfigParser();

	    SharedPreferences preferences = context.getSharedPreferences(Dashboard.SHARED_PREFERENCES, Activity.MODE_PRIVATE);
	    VpnConfigGenerator vpn_configuration_generator = new VpnConfigGenerator(preferences, general_configuration, mGateway);
	    String configuration = vpn_configuration_generator.generate();
				
	    cp.parseConfig(new StringReader(configuration));
	    return cp.convertProfile();
	} catch (ConfigParser.ConfigParseError e) {
	    // FIXME We didn't get a VpnProfile!  Error handling! and log level
	    Log.v(TAG,"Error creating VPNProfile");
	    e.printStackTrace();
	    return null;
	} catch (IOException e) {
	    // FIXME We didn't get a VpnProfile!  Error handling! and log level
	    Log.v(TAG,"Error creating VPNProfile");
	    e.printStackTrace();
	    return null;
	}
    }

    public String getName() {
	return mName;
    }

    public VpnProfile getProfile() {
	return mVpnProfile;
    }

    public int getTimezone() {
	return timezone;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Gateway) {
            VpnProfile compared_profile = ((Gateway) o).getProfile();
            return compared_profile.mConnections.equals(mVpnProfile.mConnections)
                    && compared_profile.mClientCertFilename != mVpnProfile.mClientCertFilename
                    && compared_profile.mClientKeyFilename != mVpnProfile.mClientKeyFilename;
        }
        else
            return super.equals(o);
    }
}
