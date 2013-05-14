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

    final public static String
    DOWNLOAD_JSON_FILES_BUNDLE_EXTRA = "downloadJSONFiles",	
    UPDATE_PROVIDER_DOTJSON = "updateProviderDotJSON",
    DOWNLOAD_NEW_PROVIDER_DOTJSON = "downloadNewProviderDotJSON",
    LOG_IN_DIALOG = "logInDialog",
    NEW_PROVIDER_DIALOG = "logInDialog",
    SRP_REGISTER = "srpRegister",
    SRP_AUTH = "srpAuth",
    LOG_IN = "logIn",
    LOG_OUT = "logOut",
    DOWNLOAD_CERTIFICATE = "downloadUserAuthedCertificate",
    API_VERSION_KEY = "api_version",
    RESULT_KEY = "result",
    PROVIDER_KEY = "provider",
    SERVICE_KEY = "service",
    ALLOWED_ANON = "allow_anonymous",
    MAIN_CERT_KEY = "main_cert",
    CERT_KEY = "cert",
    EIP_SERVICE_KEY = "eip",
    TYPE_OF_CERTIFICATE = "type_of_certificate",
    ANON_CERTIFICATE = "anon_certificate",
    AUTHED_CERTIFICATE = "authed_certificate",
    SALT_KEY = "salt",
    SESSION_ID_COOKIE_KEY = "session_id_cookie_key",
    SESSION_ID_KEY = "session_id",
    PREFERENCES_KEY = "LEAPPreferences",
    USER_DIRECTORY = "leap_android",
    PROVIDER_NAME = "provider_name",
    PROVIDER_MAIN_URL = "provider_main_url",
    PROVIDER_JSON_URL = "provider_json_url",
    CUSTOM = "custom",
    DANGER_ON = "danger_on",
    API_URL_KEY = "api_uri",
    USERNAME_KEY = "username",
    PASSWORD_KEY = "password",
    ALLOW_REGISTRATION_KEY = "allow_registration",
    EIP_SERVICE_API_PATH = "config/eip-service.json"
    ;
	
    final public static String NG_1024 =
    		"eeaf0ab9adb38dd69c33f80afa8fc5e86072618775ff3c0b9ea2314c9c256576d674df7496ea81d3383b4813d692c6e0e0d5d8e250b98be48e495c1d6089dad15dc7d7b46154d6b6ce8ef4ad69b15d4982559b297bcf1885c529f566660e57ec68edbc3c05726cc02fd4cbf4976eaa9afd5138fe8376435b9fc61d2fc0eb06e3";
    final public static BigInteger G = new BigInteger("2");

    final public static int
    CUSTOM_PROVIDER_ADDED = 0,
    CORRECTLY_DOWNLOADED_JSON_FILES = 1,
    INCORRECTLY_DOWNLOADED_JSON_FILES = 2,
    SRP_AUTHENTICATION_SUCCESSFUL = 3,
    SRP_AUTHENTICATION_FAILED = 4,
    SRP_REGISTRATION_SUCCESSFUL = 5,
    SRP_REGISTRATION_FAILED = 6,
    LOGOUT_SUCCESSFUL = 7,
    LOGOUT_FAILED = 8,
    CORRECTLY_DOWNLOADED_CERTIFICATE = 9,
    INCORRECTLY_DOWNLOADED_CERTIFICATE = 10,
    CORRECTLY_UPDATED_PROVIDER_DOT_JSON = 11,
    INCORRECTLY_UPDATED_PROVIDER_DOT_JSON = 12,
    CORRECTLY_DOWNLOADED_ANON_CERTIFICATE = 13,
    INCORRECTLY_DOWNLOADED_ANON_CERTIFICATE = 14
    ;
	
	public static void saveSharedPref(String shared_preferences_key, JSONObject content) {

		SharedPreferences.Editor shared_preferences_editor = shared_preferences
				.edit();
		shared_preferences_editor.putString(shared_preferences_key,
				content.toString());
		shared_preferences_editor.commit();
	}

	public static String getStringFromSharedPref(String shared_preferences_key) {
		String value = "";
		if(shared_preferences != null) {
			String content = shared_preferences.getString(shared_preferences_key, "");
			try {
				JSONObject json_object = new JSONObject(content);
				value = json_object.toString();
			} catch (JSONException e) {
				value = content;
			}
		}
		return value;
	}

	public static void saveFile(String filename, String content) {
		File root = Environment.getExternalStorageDirectory();
		File leap_dir = new File(root.getAbsolutePath() + File.separator + USER_DIRECTORY);
		if (!leap_dir.isDirectory()) {
			leap_dir.mkdir();
		}
		try {
			if (!leap_dir.isDirectory()) {
				throw new IOException(
						"Unable to create directory " + USER_DIRECTORY + ". Maybe the SD card is mounted?");
			}
			File outputFile = new File(leap_dir, filename);
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
			writer.write(content);
			writer.close();
		} catch (IOException e) {
			Log.w("leap_android", e.getMessage(), e);
		}
	}
	
	public static FileInputStream openFileInputStream(String filename) {
		FileInputStream input_stream = null;
		File root = Environment.getExternalStorageDirectory();
		File leap_dir = new File(root.getAbsolutePath() + File.separator + USER_DIRECTORY);
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
			if(keystore_trusted == null) {
				keystore_trusted = KeyStore.getInstance("BKS");
				keystore_trusted.load(null);
			}
			keystore_trusted.setCertificateEntry(provider, cert);
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static KeyStore getKeystore() {
		return keystore_trusted;
	}
}
