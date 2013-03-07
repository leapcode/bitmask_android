package se.leap.leapclient;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

public class ConfigHelper {
    
    public static SharedPreferences shared_preferences;

	final static String downloadJsonFilesBundleExtra = "downloadJSONFiles";
	final static String downloadNewProviderDotJSON = "downloadNewProviderDotJSON";
	public static final String srpRegister = "srpRegister";
	final public static String srpAuth = "srpAuth";
	final public static String resultKey = "result";
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
			"eeaf0ab9adb38dd69c33f80afa8fc5e86072618775ff3c0b9ea2314c9c256576d674df7496ea81d3383b4813d692c6e0e0d5d8e250b98be48e495c1d6089dad15dc7d7b46154d6b6ce8ef4ad69b15d4982559b297bcf1885c529f566660e57ec68edbc3c05726cc02fd4cbf4976eaa9afd5138fe8376435b9fc61d2fc0eb06e3";
	final public static BigInteger g = BigInteger.valueOf(2);
	
	final public static int CUSTOM_PROVIDER_ADDED = 0;
	final public static int CORRECTLY_DOWNLOADED_JSON_FILES = 1;
	final public static int INCORRECTLY_DOWNLOADED_JSON_FILES = 2;
	final public static int SRP_AUTHENTICATION_SUCCESSFUL = 3;
	final public static int SRP_AUTHENTICATION_FAILED = 4;
	public static final int SRP_REGISTRATION_SUCCESSFUL = 5;
	public static final int SRP_REGISTRATION_FAILED = 6;

	static void saveSharedPref(String shared_preferences_key, JSONObject content) {

		SharedPreferences.Editor shared_preferences_editor = shared_preferences
				.edit();
		shared_preferences_editor.putString(shared_preferences_key,
				content.toString());
		shared_preferences_editor.commit();
		System.out.println("Shared preferences updated: key = "
				+ shared_preferences_key
				+ " Content = "
				+ shared_preferences.getString(
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

	public static void setSharedPreferences(
			SharedPreferences shared_preferences) {
		ConfigHelper.shared_preferences = shared_preferences;
	}
}
