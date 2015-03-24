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

import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringReader;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;

/**
 * Gateway provides objects defining gateways and their metadata.
 * Each instance contains a VpnProfile for OpenVPN specific data and member
 * variables describing capabilities and location (name)
 * 
 * @author Sean Leonard <meanderingcode@aetherislands.net>
 * @author Parménides GV <parmegv@sdf.org>
 */
public class Gateway {
		
    public final static String TAG = Gateway.class.getSimpleName();

    private JSONObject general_configuration;
    private JSONObject secrets;
    private JSONObject gateway;

    private String mName;
    private int timezone;
    private VpnProfile mVpnProfile;
    /**
     * Build a gateway object from a JSON OpenVPN gateway definition in eip-service.json
     * and create a VpnProfile belonging to it.
     */
    public Gateway(JSONObject eip_definition, JSONObject secrets, JSONObject gateway){

	this.gateway = gateway;
        this.secrets = secrets;

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

	    return locations.getJSONObject(gateway.getString("location"));
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

	    VpnConfigGenerator vpn_configuration_generator = new VpnConfigGenerator(general_configuration, secrets, gateway);
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
    public String toString() {
        return new Gson().toJson(this, Gateway.class);
    }
}
