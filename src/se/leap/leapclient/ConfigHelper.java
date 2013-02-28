package se.leap.leapclient;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

public class ConfigHelper {

	final static String downloadJsonFilesBundleExtra = "downloadJSONFiles";
	final static String downloadNewProviderDotJSON = "downloadNewProviderDotJSON";
	final static String srpAuth = "srpAuth";
	final static String provider_key = "provider";
	final static String cert_key = "cert";
	final static String eip_service_key = "eip";
	public static final String PREFERENCES_KEY = "LEAPPreferences";
	public static final String user_directory = "leap_android";
	public static String provider_key_url = "provider_main_url";
	final public static String srp_server_url_key = "srp_server_url";
	final public static String username_key = "username";
	final public static String password_key = "password";
	final public static String eip_service_api_path = "/config/eip-service.json";
	
	final public static String NG_1024 = 
			"EEAF0AB9ADB38DD69C33F80AFA8FC5E86072618775FF3C0B9EA2314C9C256576D674DF7496EA81D3383B4813D692C6E0E0D5D8E250B98BE48E495C1D6089DAD15DC7D7B46154D6B6CE8EF4AD69B15D4982559B297BCF1885C529F566660E57EC68EDBC3C05726CC02FD4CBF4976EAA9AFD5138FE8376435B9FC61D2FC0EB06E3";
	final public static BigInteger g = BigInteger.valueOf(2);
	
	final public static int CUSTOM_PROVIDER_ADDED = 0;
	final public static int CORRECTLY_DOWNLOADED_JSON_FILES = 0;
	final public static int INCORRECTLY_DOWNLOADED_JSON_FILES = 0; 

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
		File leap_dir = new File(root.getAbsolutePath() + File.separator + user_directory);
		if (!leap_dir.isDirectory()) {
			leap_dir.mkdir();
		}
		try {
			if (!leap_dir.isDirectory()) {
				throw new IOException(
						"Unable to create directory " + user_directory + ". Maybe the SD card is mounted?");
			}
			File outputFile = new File(leap_dir, filename);
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
			writer.write(content);
			writer.close();
		} catch (IOException e) {
			Log.w("leap_android", e.getMessage(), e);
		}
	}
	
	static FileInputStream openFileInputStream(String filename) {
		FileInputStream input_stream = null;
		File root = Environment.getExternalStorageDirectory();
		File leap_dir = new File(root.getAbsolutePath() + File.separator + user_directory);
		try {
			input_stream = new FileInputStream(leap_dir + File.separator + filename);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return input_stream;
	}
}
