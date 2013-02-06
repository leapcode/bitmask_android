/**
 * 
 */
package se.leap.leapclient;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;

/**
 * @author Sean Leonard <meanderingcode@aetherislands.net>
 *
 */
final class Provider implements Serializable {

	private static final long serialVersionUID = 6003835972151761353L;
	
	private static Provider instance = null;
	
	// We'll access our preferences here
	private static SharedPreferences preferences = null;
	// Represents our Provider's provider.json
	private static JSONObject definition = null;
	

	// Array of what API versions we understand
	protected static final String[] API_VERSIONS = {"1"};  // I assume we might encounter arbitrary version "numbers"
	// Some API pieces we want to know about
	private static final String API_TERM_SERVICES = "services";
	private static final String API_TERM_NAME = "name";
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
	private Provider(SharedPreferences preferences) {
		
		// Load our preferences from SharedPreferences
		//   If there's nothing there, we will end up returning a rather empty object
		//   to whoever called getInstance() and they can run the First Run Wizard
		//preferences = context.getgetPreferences(0); // 0 == MODE_PRIVATE, but we don't extend Android's classes...
		
		// Inflate our provider.json data
		try {
			definition = new JSONObject( preferences.getString("provider", "") );
		} catch (JSONException e) {
			// TODO: handle exception
			
			// FIXME!!  We want "real" data!!
		}
	}

	protected static Provider getInstance(SharedPreferences preferences){
		if(instance==null){
			instance = new Provider(preferences);
		}
		return instance;
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
		for (int i=0;i<API_EIP_TYPES.length;i++){
			try {
				if ( Arrays.asList(API_EIP_TYPES).contains( services.getString(i) ) )
					return true;
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
