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
 package se.leap.leapclient;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.SecureRandom;
import javax.net.ssl.KeyManager;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jboss.security.srp.SRPParameters;
import org.json.JSONException;
import org.json.JSONObject;

import se.leap.leapclient.ProviderListContent.ProviderItem;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

/**
 * Implements HTTP api methods used to manage communications with the provider server.
 * 
 * It's an IntentService because it downloads data from the Internet, so it operates in the background.
 *  
 * @author parmegv
 * @author MeanderingCode
 *
 */
public class ProviderAPI extends IntentService {
	
	private Handler mHandler;

	public ProviderAPI() {
		super("ProviderAPI");
		Log.v("ClassName", "Provider API");
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		mHandler = new Handler();
		CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER) );
	}
	
	private void displayToast(final int toast_string_id) {
		mHandler.post(new Runnable() {
			
			@Override
			public void run() {
	            Toast.makeText(ProviderAPI.this, toast_string_id, Toast.LENGTH_LONG).show();                
			}
		});
	}

	@Override
	protected void onHandleIntent(Intent task_for) {
		final ResultReceiver receiver = task_for.getParcelableExtra("receiver");
		
		Bundle task;
		if((task = task_for.getBundleExtra(ConfigHelper.DOWNLOAD_JSON_FILES_BUNDLE_EXTRA)) != null) {
			if(!downloadJsonFiles(task)) {
				receiver.send(ConfigHelper.INCORRECTLY_DOWNLOADED_JSON_FILES, Bundle.EMPTY);
			} else { 
				receiver.send(ConfigHelper.CORRECTLY_DOWNLOADED_JSON_FILES, Bundle.EMPTY);
			}
		}
		else if ((task = task_for.getBundleExtra(ConfigHelper.UPDATE_PROVIDER_DOTJSON)) != null) {
			Bundle result = updateProviderDotJSON(task);
			if(result.getBoolean(ConfigHelper.RESULT_KEY)) {
				receiver.send(ConfigHelper.CORRECTLY_UPDATED_PROVIDER_DOT_JSON, result);
			} else {
				receiver.send(ConfigHelper.INCORRECTLY_UPDATED_PROVIDER_DOT_JSON, Bundle.EMPTY);
			}
		}
		else if ((task = task_for.getBundleExtra(ConfigHelper.DOWNLOAD_NEW_PROVIDER_DOTJSON)) != null) {
			Bundle result = downloadNewProviderDotJSON(task);
			if(result.getBoolean(ConfigHelper.RESULT_KEY)) {
				receiver.send(ConfigHelper.CORRECTLY_UPDATED_PROVIDER_DOT_JSON, result);
			} else {
				receiver.send(ConfigHelper.INCORRECTLY_DOWNLOADED_JSON_FILES, Bundle.EMPTY);
			}
		}
		else if ((task = task_for.getBundleExtra(ConfigHelper.SRP_AUTH)) != null) {
			Bundle session_id_bundle = authenticateBySRP(task);
			if(session_id_bundle.getBoolean(ConfigHelper.RESULT_KEY)) {
				receiver.send(ConfigHelper.SRP_AUTHENTICATION_SUCCESSFUL, session_id_bundle);
			} else {
				Bundle user_message_bundle = new Bundle();
				String user_message_key = getResources().getString(R.string.user_message);
				user_message_bundle.putString(user_message_key, session_id_bundle.getString(user_message_key));
				receiver.send(ConfigHelper.SRP_AUTHENTICATION_FAILED, user_message_bundle);
			}
		}
		else if ((task = task_for.getBundleExtra(ConfigHelper.LOG_OUT)) != null) {
			if(logOut(task)) {
				receiver.send(ConfigHelper.LOGOUT_SUCCESSFUL, Bundle.EMPTY);
			} else {
				receiver.send(ConfigHelper.LOGOUT_FAILED, Bundle.EMPTY);
			}
		}
		else if ((task = task_for.getBundleExtra(ConfigHelper.DOWNLOAD_CERTIFICATE)) != null) {
			if(getNewCert(task)) {
				receiver.send(ConfigHelper.CORRECTLY_DOWNLOADED_CERTIFICATE, Bundle.EMPTY);
			} else {
				receiver.send(ConfigHelper.INCORRECTLY_DOWNLOADED_CERTIFICATE, Bundle.EMPTY);
			}
		}
	}
	
	/**
	 * Tries to download the contents of the provided url using commercially validated CA certificate from chosen provider.
	 * 
	 * If danger_on flag is true, SSL exceptions will be managed by futher methods that will try to use some bypass methods.
	 * @param string_url
	 * @param danger_on if the user completely trusts this provider
	 * @return
	 */
	private String downloadWithCommercialCA(String string_url, boolean danger_on) {
		
		String json_file_content = "";
		
		URL provider_url = null;
		int seconds_of_timeout = 1;
		try {
			provider_url = new URL(string_url);
			URLConnection url_connection = provider_url.openConnection();
			url_connection.setConnectTimeout(seconds_of_timeout*1000);
			json_file_content = new Scanner(url_connection.getInputStream()).useDelimiter("\\A").next();
		} catch (MalformedURLException e) {
			displayToast(R.string.malformed_url);
		} catch(SocketTimeoutException e) {
			displayToast(R.string.server_is_down_message);
		} catch (IOException e) {
			if(provider_url != null) {
				json_file_content = downloadWithProviderCA(provider_url, danger_on);
			} else {
				displayToast(R.string.certificate_error);
			}
		} catch (Exception e) {
			if(provider_url != null && danger_on) {
				json_file_content = downloadWithProviderCA(provider_url, danger_on);
			}
		}

		return json_file_content;
	}

	/**
	 * Tries to download the contents of the provided url using not commercially validated CA certificate from chosen provider. 
	 * @param url
	 * @param danger_on true to download CA certificate in case it has not been downloaded.
	 * @return an empty string if it fails, the url content if not. 
	 */
	private String downloadWithProviderCA(URL url, boolean danger_on) {
		String json_file_content = "";

		try {
			// Tell the URLConnection to use a SocketFactory from our SSLContext
			HttpsURLConnection urlConnection =
					(HttpsURLConnection)url.openConnection();
			urlConnection.setSSLSocketFactory(ConfigHelper.getProviderSSLSocketFactory());
			json_file_content = new Scanner(urlConnection.getInputStream()).useDelimiter("\\A").next();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			displayToast(R.string.server_is_down_message);
		} catch (IOException e) {
			// The downloaded certificate doesn't validate our https connection.
			if(danger_on) {
				json_file_content = downloadWithoutCA(url);
			} else {
				displayToast(R.string.certificate_error);
			}
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return json_file_content;
	}
	
	/**
	 * Downloads the string that's in the url with any certificate.
	 */
	private String downloadWithoutCA(URL url) {
		String string = "";
		try {

			HostnameVerifier hostnameVerifier = new HostnameVerifier() {
				@Override
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			};
			
			class DefaultTrustManager implements X509TrustManager {

				@Override
					public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

				@Override
					public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

				@Override
					public X509Certificate[] getAcceptedIssuers() {
						return null;
					}
			}

			SSLContext context = SSLContext.getInstance("TLS");
			context.init(new KeyManager[0], new TrustManager[] {new DefaultTrustManager()}, new SecureRandom());

			HttpsURLConnection urlConnection = (HttpsURLConnection)url.openConnection();
			urlConnection.setSSLSocketFactory(context.getSocketFactory());
			urlConnection.setHostnameVerifier(hostnameVerifier);
			string = new Scanner(urlConnection.getInputStream()).useDelimiter("\\A").next();
		} catch (IOException e) {
			// The downloaded certificate doesn't validate our https connection.
			e.printStackTrace();
			displayToast(R.string.certificate_error);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return string;
	}
	
	/**
	 * Executes an HTTP request expecting a JSON response.
	 * @param url
	 * @param request_method
	 * @param parameters
	 * @return response from authentication server
	 * @throws IOException
	 * @throws JSONException
	 * @throws MalformedURLException 
	 * @throws CertificateException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyStoreException 
	 * @throws KeyManagementException 
	 */
	private JSONObject sendToServer(String url, String request_method, Map<String, String> parameters) throws JSONException, MalformedURLException, IOException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
		JSONObject json_response;
		InputStream is = null;
		HttpsURLConnection urlConnection = (HttpsURLConnection)new URL(url).openConnection();
		urlConnection.setRequestMethod(request_method);
		urlConnection.setChunkedStreamingMode(0);
		urlConnection.setSSLSocketFactory(ConfigHelper.getProviderSSLSocketFactory());
		try {
			
			DataOutputStream writer = new DataOutputStream(urlConnection.getOutputStream());
			writer.writeBytes(ConfigHelper.formatHttpParameters(parameters));
			writer.close();

			is = urlConnection.getInputStream();
			String plain_response = new Scanner(is).useDelimiter("\\A").next();
			json_response = new JSONObject(plain_response);
		} finally {
			InputStream error_stream = urlConnection.getErrorStream();
			if(error_stream != null) {
				String error_response = new Scanner(error_stream).useDelimiter("\\A").next();
				urlConnection.disconnect();
				Log.d("Error", error_response);
				json_response = new JSONObject(error_response);
				if(!json_response.isNull(ConfigHelper.ERRORS_KEY) || json_response.has(ConfigHelper.ERRORS_KEY)) {
					return new JSONObject();
				}
			}
		}

		return json_response;
	}

	/**
	 * Tries to guess the provider.json url given the main provider url.
	 * @param provider_main_url
	 * @return the guessed provider.json url
	 */
	private String guessProviderDotJsonURL(String provider_main_url) {
		return provider_main_url + "/provider.json";
	}

	/**
	 * Downloads the main cert and the eip-service.json files given through the task parameter
	 * @param task
	 * @return true if eip-service.json was parsed as a JSON object correctly.
	 */
	private boolean downloadJsonFiles(Bundle task) {
		String cert_url = task.getString(ConfigHelper.MAIN_CERT_KEY);
		String eip_service_json_url = task.getString(ConfigHelper.EIP_SERVICE_KEY);
		boolean danger_on = task.getBoolean(ConfigHelper.DANGER_ON);
		try {
			String cert_string = downloadWithCommercialCA(cert_url, danger_on);
			ConfigHelper.saveSharedPref(ConfigHelper.MAIN_CERT_KEY, cert_string);
			String eip_service_string = downloadWithCommercialCA(eip_service_json_url, danger_on);
			ConfigHelper.saveSharedPref(ConfigHelper.EIP_SERVICE_KEY, new JSONObject(eip_service_string));
			return true;
		} catch (JSONException e) {
			return false;
		}
	}
	
	/**
	 * Starts the authentication process using SRP protocol.
	 * 
	 * @param task containing: username, password and api url. 
	 * @return a bundle with a boolean value mapped to a key named ConfigHelper.RESULT_KEY, and which is true if authentication was successful. 
	 */
	private Bundle authenticateBySRP(Bundle task) {
		Bundle session_id_bundle = new Bundle();
		
		String username = (String) task.get(ConfigHelper.USERNAME_KEY);
		String password = (String) task.get(ConfigHelper.PASSWORD_KEY);
		if(wellFormedPassword(password)) {
			String authentication_server = (String) task.get(ConfigHelper.API_URL_KEY);

			SRPParameters params = new SRPParameters(new BigInteger(ConfigHelper.NG_1024, 16).toByteArray(), ConfigHelper.G.toByteArray(), BigInteger.ZERO.toByteArray(), "SHA-256");
			LeapSRPSession client = new LeapSRPSession(username, password, params);
			byte[] A = client.exponential();
			try {
				JSONObject saltAndB = sendAToSRPServer(authentication_server, username, new BigInteger(1, A).toString(16));
				if(saltAndB.length() > 0) {
					String salt = saltAndB.getString(ConfigHelper.SALT_KEY);
					byte[] Bbytes = new BigInteger(saltAndB.getString("B"), 16).toByteArray();
					byte[] M1 = client.response(new BigInteger(salt, 16).toByteArray(), Bbytes);
					JSONObject session_idAndM2 = sendM1ToSRPServer(authentication_server, username, M1);
					if(session_idAndM2.has("M2") && client.verify((byte[])session_idAndM2.get("M2"))) {
						session_id_bundle.putBoolean(ConfigHelper.RESULT_KEY, true);
					} else {
						session_id_bundle.putBoolean(ConfigHelper.RESULT_KEY, false);
						session_id_bundle.putString(getResources().getString(R.string.user_message), getResources().getString(R.string.error_bad_user_password_user_message));
					}
				} else {
					session_id_bundle.putString(getResources().getString(R.string.user_message), getResources().getString(R.string.error_bad_user_password_user_message));
					session_id_bundle.putBoolean(ConfigHelper.RESULT_KEY, false);
				}
			} catch (IOException e) {
				session_id_bundle.putBoolean(ConfigHelper.RESULT_KEY, false);
				session_id_bundle.putString(getResources().getString(R.string.user_message), getResources().getString(R.string.error_io_exception_user_message));
			} catch (JSONException e) {
				session_id_bundle.putBoolean(ConfigHelper.RESULT_KEY, false);
				session_id_bundle.putString(getResources().getString(R.string.user_message), getResources().getString(R.string.error_json_exception_user_message));
			} catch (NoSuchAlgorithmException e) {
				session_id_bundle.putBoolean(ConfigHelper.RESULT_KEY, false);
				session_id_bundle.putString(getResources().getString(R.string.user_message), getResources().getString(R.string.error_no_such_algorithm_exception_user_message));
			} catch (KeyManagementException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (KeyStoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CertificateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			session_id_bundle.putBoolean(ConfigHelper.RESULT_KEY, false);
			session_id_bundle.putString(getResources().getString(R.string.user_message), getResources().getString(R.string.error_not_valid_password_user_message));
		}
		
		return session_id_bundle;
	}

	/**
	 * Validates a password
	 * @param entered_password
	 * @return true if the entered password length is greater or equal to eight (8).
	 */
	private boolean wellFormedPassword(String entered_password) {
		return entered_password.length() >= 8;
	}

	/**
	 * Sends an HTTP POST request to the authentication server with the SRP Parameter A.
	 * @param server_url
	 * @param username
	 * @param clientA First SRP parameter sent 
	 * @return response from authentication server
	 * @throws IOException
	 * @throws JSONException
	 * @throws CertificateException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyStoreException 
	 * @throws KeyManagementException 
	 */
	private JSONObject sendAToSRPServer(String server_url, String username, String clientA) throws IOException, JSONException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("login", username);
		parameters.put("A", clientA);
		return sendToServer(server_url + "/sessions.json", "POST", parameters);
	}

	/**
	 * Sends an HTTP PUT request to the authentication server with the SRP Parameter M1 (or simply M).
	 * @param server_url
	 * @param username
	 * @param m1 Second SRP parameter sent 
	 * @return response from authentication server
	 * @throws IOException
	 * @throws JSONException
	 * @throws CertificateException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyStoreException 
	 * @throws KeyManagementException 
	 */
	private JSONObject sendM1ToSRPServer(String server_url, String username, byte[] m1) throws IOException, JSONException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("client_auth", new BigInteger(1, ConfigHelper.trim(m1)).toString(16));
		
		//HttpPut put = new HttpPut(server_url + "/sessions/" + username +".json" + "?" + "client_auth" + "=" + new BigInteger(1, ConfigHelper.trim(m1)).toString(16));
		JSONObject json_response = sendToServer(server_url + "/sessions/" + username +".json", "PUT", parameters);

		JSONObject session_idAndM2 = new JSONObject();
		if(json_response.length() > 0) {
			byte[] M2_not_trimmed = new BigInteger(json_response.getString(ConfigHelper.M2_KEY), 16).toByteArray();
			session_idAndM2.put(ConfigHelper.M2_KEY, ConfigHelper.trim(M2_not_trimmed));
		}
		return session_idAndM2;
	}
	
	/**
	 * Downloads a provider.json from a given URL, adding a new provider using the given name.  
	 * @param task containing a boolean meaning if the provider is custom or not, another boolean meaning if the user completely trusts this provider, the provider name and its provider.json url.
	 * @return a bundle with a boolean value mapped to a key named ConfigHelper.RESULT_KEY, and which is true if the update was successful. 
	 */
	private Bundle updateProviderDotJSON(Bundle task) {
		Bundle result = new Bundle();
		//boolean custom = task.getBoolean(ConfigHelper.CUSTOM);
		boolean danger_on = task.getBoolean(ConfigHelper.DANGER_ON);
		String provider_json_url = task.getString(ConfigHelper.PROVIDER_JSON_URL);
		//String provider_name = task.getString(ConfigHelper.PROVIDER_NAME);
		
		try {
			String provider_dot_json_string = downloadWithCommercialCA(provider_json_url, danger_on);
			if(provider_dot_json_string.isEmpty()) {
				result.putBoolean(ConfigHelper.RESULT_KEY, false);
			} else {
				JSONObject provider_json = new JSONObject(provider_dot_json_string);
				ConfigHelper.saveSharedPref(ConfigHelper.ALLOWED_ANON, provider_json.getJSONObject(ConfigHelper.SERVICE_KEY).getBoolean(ConfigHelper.ALLOWED_ANON));

				//ProviderListContent.addItem(new ProviderItem(provider_name, provider_json_url, provider_json, custom, danger_on));
				result.putBoolean(ConfigHelper.RESULT_KEY, true);
				result.putString(ConfigHelper.PROVIDER_KEY, provider_json.toString());
				result.putBoolean(ConfigHelper.DANGER_ON, danger_on);
			}
		} catch (JSONException e) {
			result.putBoolean(ConfigHelper.RESULT_KEY, false);
		}
		
		return result;
	}

	/**
	 * Downloads a custom provider provider.json file
	 * @param task containing a boolean meaning if the user completely trusts this provider, and the provider main url entered in the new custom provider dialog.
	 * @return true if provider.json file was successfully parsed as a JSON object.
	 */
	private Bundle downloadNewProviderDotJSON(Bundle task) {
		Bundle result = new Bundle();
		boolean custom = true;
		boolean danger_on = task.getBoolean(ConfigHelper.DANGER_ON);
		
		String provider_main_url = (String) task.get(ConfigHelper.PROVIDER_MAIN_URL);
		String provider_name = provider_main_url.replaceFirst("http[s]?://", "").replaceFirst("\\/", "_");
		String provider_json_url = guessProviderDotJsonURL(provider_main_url);
		
		String provider_json_string = downloadWithCommercialCA(provider_json_url, danger_on);
		try {
			if(provider_json_string.isEmpty()) {
				result.putBoolean(ConfigHelper.RESULT_KEY, false);
			} else {
				JSONObject provider_json = new JSONObject(provider_json_string);

				ConfigHelper.saveSharedPref(ConfigHelper.PROVIDER_KEY, provider_json);
				ConfigHelper.saveSharedPref(ConfigHelper.DANGER_ON, danger_on);
				ConfigHelper.saveSharedPref(ConfigHelper.ALLOWED_ANON, provider_json.getJSONObject(ConfigHelper.SERVICE_KEY).getBoolean(ConfigHelper.ALLOWED_ANON));
				ProviderItem added_provider = new ProviderItem(provider_name, provider_json_url, provider_json, custom, danger_on);
				ProviderListContent.addItem(added_provider);

				result.putString(ConfigHelper.PROVIDER_ID, added_provider.getId());
				result.putBoolean(ConfigHelper.RESULT_KEY, true);
				result.putString(ConfigHelper.PROVIDER_KEY, provider_json.toString());
				result.putBoolean(ConfigHelper.DANGER_ON, danger_on);
			}
		} catch (JSONException e) {
			result.putBoolean(ConfigHelper.RESULT_KEY, false);
		}
		
		return result;
	}

	/**
	 * Downloads a new OpenVPN certificate.
	 * 
	 * @param task containing the type of the certificate to be downloaded
	 * @return true if certificate was downloaded correctly, false if provider.json or danger_on flag are not present in SharedPreferences, or if the certificate url could not be parsed as a URI, or if there was an SSL error. 
	 */
	private boolean getNewCert(Bundle task) {
		try {
			JSONObject provider_json = ConfigHelper.getJsonFromSharedPref(ConfigHelper.PROVIDER_KEY);
			URL provider_main_url = new URL(provider_json.getString(ConfigHelper.API_URL_KEY));
			String new_cert_string_url = provider_main_url.toString() + "/" + provider_json.getString(ConfigHelper.API_VERSION_KEY) + "/" + ConfigHelper.CERT_KEY;

			boolean danger_on = ConfigHelper.getBoolFromSharedPref(ConfigHelper.DANGER_ON);
			String cert_string = downloadWithCommercialCA(new_cert_string_url, danger_on);
			if(!cert_string.isEmpty()) {
				// API returns concatenated cert & key.  Split them for OpenVPN options
				String certificate = null, key = null;
				String[] certAndKey = cert_string.split("(?<=-\n)");
				for (int i=0; i < certAndKey.length-1; i++){
					if ( certAndKey[i].contains("KEY") )
						key = certAndKey[i++] + certAndKey[i];
					else if ( certAndKey[i].contains("CERTIFICATE") )
						certificate = certAndKey[i++] + certAndKey[i];
				}
				ConfigHelper.saveSharedPref(ConfigHelper.CERT_KEY, certificate);
				ConfigHelper.saveSharedPref(ConfigHelper.KEY_KEY, key);
				return true;
			} else {
				return false;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Logs out from the api url retrieved from the task.
	 * @param task containing api url from which the user will log out
	 * @return true if there were no exceptions
	 */
	private boolean logOut(Bundle task) {
		try {
			String delete_url = task.getString(ConfigHelper.API_URL_KEY) + "/logout";

			HttpsURLConnection urlConnection = (HttpsURLConnection)new URL(delete_url).openConnection();
			urlConnection.setRequestMethod("DELETE");
			urlConnection.setSSLSocketFactory(ConfigHelper.getProviderSSLSocketFactory());

			int responseCode = urlConnection.getResponseCode();
			Log.d("logout", Integer.toString(responseCode));
		} catch (IndexOutOfBoundsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
}
