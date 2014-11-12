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

import android.content.SharedPreferences;
import android.util.Log;
import java.util.Iterator;
import java.util.Vector;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import se.leap.bitmaskclient.*;

public class VpnConfigGenerator {

    private JSONObject general_configuration;
    private JSONObject gateway;
    
    private static SharedPreferences preferences;
    public final static String TAG = VpnConfigGenerator.class.getSimpleName();
    private final String new_line = System.getProperty("line.separator"); // Platform new line

    public VpnConfigGenerator(SharedPreferences preferences, JSONObject general_configuration, JSONObject gateway) {
	this.general_configuration = general_configuration;
	this.gateway = gateway;
	this.preferences = preferences;
    }
    
    public String generate() {
	return
	    generalConfiguration()
	    + new_line
	    + gatewayConfiguration()
	    + new_line
	    + secretsConfiguration()
	    + new_line
	    + androidCustomizations();
    }

    private String generalConfiguration() {
	String common_options = "";
	try {
	    Iterator keys = general_configuration.keys();
	    Vector<Vector<String>> value = new Vector<Vector<String>>();
	    while ( keys.hasNext() ){
		String key = keys.next().toString();
					
		common_options += key + " ";
		for ( String word : general_configuration.getString(key).split(" ") )
		    common_options += word + " ";
		common_options += new_line;
			
	    }
	} catch (JSONException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	common_options += "client";

	return common_options;
    }
    
    private String gatewayConfiguration() {
	String remotes = "";
		
	String remote = "ip_address";
	String remote_openvpn_keyword = "remote";
	String ports = "ports";
	String protos = "protocols";
	String capabilities = "capabilities";
	String udp = "udp";
		
	try {
	    JSONArray protocolsJSON = gateway.getJSONObject(capabilities).getJSONArray(protos);
	    for ( int i=0; i<protocolsJSON.length(); i++ ) {
		String remote_line = remote_openvpn_keyword;
		remote_line += " " + gateway.getString(remote);
		remote_line += " " + gateway.getJSONObject(capabilities).getJSONArray(ports).optString(0);
		remote_line += " " + protocolsJSON.optString(i);
		if(remote_line.endsWith(udp))
		    remotes = remotes.replaceFirst(remote_openvpn_keyword, remote_line + new_line + remote_openvpn_keyword);
		else
		    remotes += remote_line;
		remotes += new_line;
	    }
	} catch (JSONException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
		
	Log.d(TAG, "remotes = " + remotes);
	return remotes;
    }

    private String secretsConfiguration() {
						    
	String ca = 
	    "<ca>"
	    + new_line
	    + preferences.getString(Provider.CA_CERT, "")
	    + new_line
	    + "</ca>";
		
	String key =
	    "<key>"
	    + new_line
	    + preferences.getString(Constants.PRIVATE_KEY, "")
	    + new_line
	    + "</key>";
		
	String openvpn_cert =
	    "<cert>"
	    + new_line
	    + preferences.getString(Constants.CERTIFICATE, "")
	    + new_line
	    + "</cert>";

	return ca + new_line + key + new_line + openvpn_cert;
    }

    private String androidCustomizations() {
	return
	    "remote-cert-tls server"
	    + new_line
	    + "persist-tun"
	    + new_line
	    + "auth-retry nointeract";
    }
}
