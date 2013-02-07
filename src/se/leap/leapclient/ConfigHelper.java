package se.leap.leapclient;


import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;

public class ConfigHelper {

	final static String downloadJsonFilesBundleExtra = "downloadJSONFiles";
	final static String provider_key = "provider";
	final static String cert_key = "cert";
	final static String eip_service_key = "eip";
	public static final String PREFERENCES_KEY = "LEAPPreferences";

	static void saveSharedPref(String shared_preferences_key, JSONObject content) {

		SharedPreferences.Editor shared_preferences_editor = ConfigurationWizard.shared_preferences
				.edit();
		shared_preferences_editor.putString(shared_preferences_key,
				content.toString());
		shared_preferences_editor.commit();
		System.out.println("Shared preferences updated: key = "
				+ shared_preferences_key
				+ " Content = "
				+ ConfigurationWizard.shared_preferences.getString(
						shared_preferences_key, "Default"));
	}
	
	static void rescueJSONException(JSONException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}
