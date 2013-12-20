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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

/**
 * Stores constants, and implements auxiliary methods used across all LEAP Android classes.
 * 
 * @author parmegv
 * @author MeanderingCode
 *
 */
public class ConfigHelper {
    
    public static SharedPreferences shared_preferences;
    private static KeyStore keystore_trusted;

    final public static String NG_1024 =
    		"eeaf0ab9adb38dd69c33f80afa8fc5e86072618775ff3c0b9ea2314c9c256576d674df7496ea81d3383b4813d692c6e0e0d5d8e250b98be48e495c1d6089dad15dc7d7b46154d6b6ce8ef4ad69b15d4982559b297bcf1885c529f566660e57ec68edbc3c05726cc02fd4cbf4976eaa9afd5138fe8376435b9fc61d2fc0eb06e3";
    final public static BigInteger G = new BigInteger("2");
	
    
    private static boolean checkSharedPrefs() {
    	try {
    		if(shared_preferences == null)
    			shared_preferences = Dashboard.getAppContext().getSharedPreferences(Dashboard.SHARED_PREFERENCES,Context.MODE_PRIVATE);
    	} catch (Exception e) {
    		return false;
    	}
    	
    	return true;
    }
    
    /**
     * Saves a JSON object into class scope Shared Preferences
     * @param shared_preferences_key
     * @param content
     */
	public static void saveSharedPref(String shared_preferences_key, JSONObject content) {

		SharedPreferences.Editor shared_preferences_editor = shared_preferences
				.edit();
		shared_preferences_editor.putString(shared_preferences_key,
				content.toString());
		shared_preferences_editor.commit();
	}

    /**
     * Saves a String object into class scope Shared Preferences
     * @param shared_preferences_key
     * @param content
     */
	public static void saveSharedPref(String shared_preferences_key, String content) {

		SharedPreferences.Editor shared_preferences_editor = shared_preferences
				.edit();
		shared_preferences_editor.putString(shared_preferences_key,
				content);
		shared_preferences_editor.commit();
	}

    /**
     * Saves a boolean object into class scope Shared Preferences
     * @param shared_preferences_key
     * @param content
     */
	public static void saveSharedPref(String shared_preferences_key, boolean content) {

		SharedPreferences.Editor shared_preferences_editor = shared_preferences
				.edit();
		shared_preferences_editor.putBoolean(shared_preferences_key, content);
		shared_preferences_editor.commit();
	}
	
	/**
	 * Saves an int into class scope Shared Preferences
	 * 
	 * @param shared_preferences_key
	 * @param value
	 */
	protected static void saveSharedPref(String shared_preferences_key, int value) {
		SharedPreferences.Editor shared_preferences_editor = shared_preferences.edit();
		shared_preferences_editor.putInt(shared_preferences_key, value).commit();
	}
	
	/**
	 * Gets String object from class scope Shared Preferences
	 * @param shared_preferences_key
	 * @return the string correspondent to the key parameter
	 */
	public static String getStringFromSharedPref(String shared_preferences_key) {
		String content = null;
		content = shared_preferences.getString(shared_preferences_key, "");
		return content;
	}
	
	/**
	 * Gets JSON object from class scope Shared Preferences
	 * @param shared_preferences_key
	 * @return the JSON object correspondent to the key parameter
	 */
	public static JSONObject getJsonFromSharedPref(String shared_preferences_key) throws JSONException {
		JSONObject content = null;
		if ( checkSharedPrefs() ) {
			String json_string = shared_preferences.getString(shared_preferences_key, "");
			content = new JSONObject(json_string);
		}
		
		return content;
	}
	
	/*
	 * This method defaults to false.
	 * If you use this method, be sure to fail-closed on false!
	 * TODO This is obviously less than ideal...solve it!
	 */
	public static boolean getBoolFromSharedPref(String shared_preferences_key) {
		boolean value = false;
		if ( checkSharedPrefs() ) {
			value = shared_preferences.getBoolean(shared_preferences_key, false);
		}
		return value;
	}

	/**
	 * Get an int from SharedPreferences
	 * 
	 * @param shared_preferences_key	Key to retrieve
	 * @return	The value for the key or 0
	 */
	protected static int getIntFromSharedPref(String shared_preferences_key) {
		return shared_preferences.getInt(shared_preferences_key, 0);
	}
	
	protected static boolean sharedPrefContainsKey(String shared_preferences_key) {
		return shared_preferences.contains(shared_preferences_key);
	}
	/*
	 * This method defaults to false.
	 * If you use this method, be sure to fail-closed on false!
	 * TODO This is obviously less than ideal...solve it!
	 */
	public static boolean removeFromSharedPref(String shared_preferences_key) {
		SharedPreferences.Editor shared_preferences_editor = shared_preferences
				.edit();
		shared_preferences_editor.remove(shared_preferences_key);
		return shared_preferences_editor.commit();
	}
	
	public static boolean checkErroneousDownload(String downloaded_string) {
		try {
			if(new JSONObject(downloaded_string).has(ProviderAPI.ERRORS) || downloaded_string.isEmpty()) {
				return true;
			} else {
				return false;
			}
		} catch(JSONException e) {
			return false;
		}
	}

	/**
	 *  Treat the input as the MSB representation of a number,
	 *  and lop off leading zero elements.  For efficiency, the
	 *  input is simply returned if no leading zeroes are found.
	 *  
	 *  @param in array to be trimmed
	 */
	public static byte[] trim(byte[] in) {
		if(in.length == 0 || in[0] != 0)
			return in;

		int len = in.length;
		int i = 1;
		while(in[i] == 0 && i < len)
			++i;
		byte[] ret = new byte[len - i];
		System.arraycopy(in, i, ret, 0, len - i);
		return ret;
	}

	/**
	 * Sets class scope Shared Preferences
	 * @param shared_preferences
	 */
	public static void setSharedPreferences(
			SharedPreferences shared_preferences) {
		ConfigHelper.shared_preferences = shared_preferences;
	}
	
	public static X509Certificate parseX509CertificateFromString(String certificate_string) {
		java.security.cert.Certificate certificate = null;
		CertificateFactory cf;
		try {
			cf = CertificateFactory.getInstance("X.509");

			certificate_string = certificate_string.replaceFirst("-----BEGIN CERTIFICATE-----", "").replaceFirst("-----END CERTIFICATE-----", "").trim();
			byte[] cert_bytes = Base64.decode(certificate_string, Base64.DEFAULT);
			InputStream caInput =  new ByteArrayInputStream(cert_bytes);
			try {
				certificate = cf.generateCertificate(caInput);
				System.out.println("ca=" + ((X509Certificate) certificate).getSubjectDN());
			} finally {
				caInput.close();
			}
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			return null;
		}
		
		return (X509Certificate) certificate;
	}
	
	protected static RSAPrivateKey parseRsaKeyFromString(String RsaKeyString) {
		RSAPrivateKey key = null;
		try {
			KeyFactory kf = KeyFactory.getInstance("RSA", "BC");
			
			RsaKeyString = RsaKeyString.replaceFirst("-----BEGIN RSA PRIVATE KEY-----", "").replaceFirst("-----END RSA PRIVATE KEY-----", "");
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec( Base64.decode(RsaKeyString, Base64.DEFAULT) );
			key = (RSAPrivateKey) kf.generatePrivate(keySpec);
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		return key;
	}

	/**
	 * Adds a new X509 certificate given its input stream and its provider name
	 * @param provider used to store the certificate in the keystore
	 * @param inputStream from which X509 certificate must be generated.
	 */
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

	/**
	 * Adds a new X509 certificate given in its string from and using its provider name
	 * @param provider used to store the certificate in the keystore
	 * @param certificate
	 */
	public static void addTrustedCertificate(String provider, String certificate) {

		try {
			X509Certificate cert = ConfigHelper.parseX509CertificateFromString(certificate);
			if(keystore_trusted == null) {
				keystore_trusted = KeyStore.getInstance("BKS");
				keystore_trusted.load(null);
			}
			keystore_trusted.setCertificateEntry(provider, cert);
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
	
	/**
	 * @return class wide keystore
	 */
	public static KeyStore getKeystore() {
		return keystore_trusted;
	}
}
