package se.leap.leapclient;


import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;

public class ConfigHelper {

	final static String downloadJsonFilesBundleExtra = "downloadJSONFiles";
	final static String provider_key = "provider";
	final static String eip_service_key = "eip";

	static void saveSharedPref(String shared_preferences_key,
			JSONObject content) {
		
		SharedPreferences.Editor shared_preferences_editor = ProviderListActivity.shared_preferences
				.edit();
		shared_preferences_editor.putString(shared_preferences_key,
				content.toString());
		shared_preferences_editor.commit();
		System.out.println("Shared preferences updated: "
				+ ProviderListActivity.shared_preferences.getString(
						shared_preferences_key, "Default"));

	}
	
	static void rescueJSONException(JSONException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}
