package se.leap.leapclient;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

public class ConfigHelper {

	final static String downloadJsonFilesBundleExtra = "downloadJSONFiles";
	final static String downloadNewProviderDotJSON = "downloadNewProviderDotJSON";
	final static String provider_key = "provider";
	final static String cert_key = "cert";
	final static String eip_service_key = "eip";
	public static final String PREFERENCES_KEY = "LEAPPreferences";
	public static final String user_directory = "leap_android";
	public static String provider_key_url = "provider_main_url";

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

	static void saveFile(String filename, String content) {
		File root = Environment.getExternalStorageDirectory();
		File outDir = new File(root.getAbsolutePath() + File.separator + user_directory);
		if (!outDir.isDirectory()) {
			outDir.mkdir();
		}
		try {
			if (!outDir.isDirectory()) {
				throw new IOException(
						"Unable to create directory " + user_directory + ". Maybe the SD card is mounted?");
			}
			File outputFile = new File(outDir, filename);
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
			writer.write(content);
			writer.close();
		} catch (IOException e) {
			Log.w("leap_android", e.getMessage(), e);
		}
	}
}
