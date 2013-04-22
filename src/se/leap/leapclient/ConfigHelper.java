package se.leap.leapclient;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

public class ConfigHelper {
    
    public static SharedPreferences shared_preferences;
    private static KeyStore keystore_trusted;

	final static String downloadJsonFilesBundleExtra = "downloadJSONFiles";
	final static String downloadNewProviderDotJSON = "downloadNewProviderDotJSON";
	final public static String logInDialog = "logInDialog";
	final public static String newProviderDialog = "logInDialog";
	final public static String srpRegister = "srpRegister";
	final public static String srpAuth = "srpAuth";
	public static String logIn = "logIn";
	public static String logOut = "logOut";
	final public static String resultKey = "result";
	final static String provider_key = "provider";
	final static String cert_key = "cert";
	final static String eip_service_key = "eip";
	public static final String PREFERENCES_KEY = "LEAPPreferences";
	public static final String user_directory = "leap_android";
	public static String provider_main_url = "provider_main_url";
	final public static String api_url_key = "srp_server_url";
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
	final public static int SRP_REGISTRATION_SUCCESSFUL = 5;
	final public static int SRP_REGISTRATION_FAILED = 6;
	final public static int LOGOUT_SUCCESSFUL = 7;
	final public static int LOGOUT_FAILED = 8;

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

	public static void addTrustedCertificate(String provider, InputStream inputStream) {
		CertificateFactory cf;
		try {
			cf = CertificateFactory.getInstance("X.509");
			X509Certificate cert =
					(X509Certificate)cf.generateCertificate(inputStream);
			keystore_trusted.setCertificateEntry(provider, cert);
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void addTrustedCertificate(String provider, String certificate) {
		String filename_to_save = provider + "_certificate.cer";
		saveFile(filename_to_save, certificate);
		CertificateFactory cf;
		try {
			cf = CertificateFactory.getInstance("X.509");
			X509Certificate cert =
					(X509Certificate)cf.generateCertificate(openFileInputStream(filename_to_save));
			keystore_trusted.setCertificateEntry(provider, cert);
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static KeyStore getKeystore() {
		return keystore_trusted;
	}

	public static void getNewKeystore(InputStream leap_keystore) {
		try {
			keystore_trusted = KeyStore.getInstance("BKS");
			try {
				// Initialize the keystore with the provided trusted certificates
				// Also provide the password of the keystore
				keystore_trusted.load(leap_keystore, "uer92jf".toCharArray());
				//keystore_trusted.load(null, null);
			} finally {
				leap_keystore.close();
			}
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static int getSrpAuthenticationFailed() {
		return SRP_AUTHENTICATION_FAILED;
	}static String extractProviderName(String provider_main_url) {
		
		return null;
	}
}
