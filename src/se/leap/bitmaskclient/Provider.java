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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.app.Activity;
import android.content.SharedPreferences;

/**
 * @author Sean Leonard <meanderingcode@aetherislands.net>
 *
 */
public final class Provider implements Serializable {

	private static final long serialVersionUID = 6003835972151761353L;
	
	private static Provider instance = null;
	
	// We'll access our preferences here
	private static SharedPreferences preferences = null;
	// Represents our Provider's provider.json
	private static JSONObject definition = null;

    final public static String
    API_URL = "api_uri",
	API_VERSION = "api_version",
	ALLOW_REGISTRATION = "allow_registration",
	API_RETURN_SERIAL = "serial",
	SERVICE = "service",
	KEY = "provider",
	CA_CERT = "ca_cert",
	NAME = "name",
	DESCRIPTION = "description",
	DOMAIN = "domain",
	MAIN_URL = "main_url",
	DOT_JSON_URL = "provider_json_url"
	;

	// Array of what API versions we understand
	protected static final String[] API_VERSIONS = {"1"};  // I assume we might encounter arbitrary version "numbers"
	// Some API pieces we want to know about
	private static final String API_TERM_SERVICES = "services";
	private static final String API_TERM_NAME = "name";
	private static final String API_TERM_DOMAIN = "domain";
	private static final String API_TERM_DEFAULT_LANGUAGE = "default_language";
	protected static final String[] API_EIP_TYPES = {"openvpn"};

	private static final String PREFS_EIP_NAME = null;


	
	// What, no individual fields?!  We're going to gamble on org.json.JSONObject and JSONArray
	// Supporting multiple API versions will probably break this paradigm,
	// Forcing me to write a real constructor and rewrite getters/setters
	// Also will refactor if i'm instantiating the same local variables all the time
	
	/**
	 * 
	 */
	private Provider() {}

	protected static Provider getInstance(){
		if(instance==null){
			instance = new Provider();
		}
		return instance;
	}

	protected void init(Activity activity) {
		
		// Load our preferences from SharedPreferences
		//   If there's nothing there, we will end up returning a rather empty object
		//   to whoever called getInstance() and they can run the First Run Wizard
		//preferences = context.getgetPreferences(0); // 0 == MODE_PRIVATE, but we don't extend Android's classes...
		
		// Load SharedPreferences
		preferences = activity.getSharedPreferences(Dashboard.SHARED_PREFERENCES,Context.MODE_PRIVATE);
		// Inflate our provider.json data
		try {
			definition = new JSONObject( preferences.getString(Provider.KEY, "") );
		} catch (JSONException e) {
			// TODO: handle exception
			
			// FIXME!!  We want "real" data!!
		}
	}

	protected String getDomain(){
		String domain = "Null";
		try {
			domain = definition.getString(API_TERM_DOMAIN);
		} catch (JSONException e) {
			domain = "Null";
			e.printStackTrace();
		}
		return domain;
	}
	
	protected String getName(){
		// Should we pass the locale in, or query the system here?
		String lang = Locale.getDefault().getLanguage();
		String name = "Null"; // Should it actually /be/ null, for error conditions?
		try {
			name = definition.getJSONObject(API_TERM_NAME).getString(lang);
		} catch (JSONException e) {
			// TODO: Nesting try/catch blocks?  Crazy
			//  Maybe you should actually handle exception?
			try {
				name = definition.getJSONObject(API_TERM_NAME).getString( definition.getString(API_TERM_DEFAULT_LANGUAGE) );
			} catch (JSONException e2) {
				// TODO: Will you handle the exception already?
			}
		}
		
		return name;
	}
	
	protected String getDescription(){
		String lang = Locale.getDefault().getLanguage();
		String desc = null;
		try {
			desc = definition.getJSONObject("description").getString(lang);
		} catch (JSONException e) {
			// TODO: handle exception!!
			try {
				desc = definition.getJSONObject("description").getString( definition.getString("default_language") );
			} catch (JSONException e2) {
				// TODO: i can't believe you're doing it again!
			}
		}
		
		return desc;
	}

	protected boolean hasEIP() {
		JSONArray services = null;
		try {
			services = definition.getJSONArray(API_TERM_SERVICES); // returns ["openvpn"]
		} catch (Exception e) {
			// TODO: handle exception
		}
		for (int i=0;i<API_EIP_TYPES.length+1;i++){
			try {
				// Walk the EIP types array looking for matches in provider's service definitions
				if ( Arrays.asList(API_EIP_TYPES).contains( services.getString(i) ) )
					return true;
			} catch (NullPointerException e){
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return false;
	}
	
	protected String getEIPType() {
		// FIXME!!!!!  We won't always be providing /only/ OpenVPN, will we?
		// This will have to hook into some saved choice of EIP transport
		if ( instance.hasEIP() )
			return "OpenVPN";
		else
			return null;
	}
	
	protected JSONObject getEIP() {
		// FIXME!!!!!  We won't always be providing /only/ OpenVPN, will we?
		// This will have to hook into some saved choice of EIP transport, cluster, gateway
		//   with possible "choose at random" preference
		if ( instance.hasEIP() ){
			// TODO Might need an EIP class, but we've only got OpenVPN type right now,
			// and only one gateway for our only provider...
			// TODO We'll try to load from preferences, have to call ProviderAPI if we've got nothin...
			JSONObject eipObject = null;
			try {
				eipObject = new JSONObject( preferences.getString(PREFS_EIP_NAME, "") );
			} catch (JSONException e) {
				// TODO ConfigHelper.rescueJSON()
				// Still nothing?
				// TODO ProviderAPI.getEIP()
				e.printStackTrace();
			}
			
			return eipObject;
		} else
			return null;
	}
}
