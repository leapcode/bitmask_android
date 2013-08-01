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

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.ClientProtocolException;
import org.jboss.security.srp.SRPParameters;
import org.json.JSONException;
import org.json.JSONObject;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.ProviderListContent.ProviderItem;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Base64;
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

    final public static String
    DOWNLOAD_JSON_FILES_BUNDLE_EXTRA = "downloadJSONFiles",	
    UPDATE_PROVIDER_DOTJSON = "updateProviderDotJSON",
    DOWNLOAD_NEW_PROVIDER_DOTJSON = "downloadNewProviderDotJSON",
    SRP_REGISTER = "srpRegister",
    SRP_AUTH = "srpAuth",
    LOG_OUT = "logOut",
    DOWNLOAD_CERTIFICATE = "downloadUserAuthedCertificate",
    PARAMETERS = "parameters",
    RESULT_KEY = "result",
    RECEIVER_KEY = "receiver",
    SESSION_ID_COOKIE_KEY = "session_id_cookie_key",
    SESSION_ID_KEY = "session_id",
    ERRORS = "errors"
    ;

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
	protected void onHandleIntent(Intent command) {
		final ResultReceiver receiver = command.getParcelableExtra("receiver");
		String action = command.getAction();
		Bundle parameters = command.getBundleExtra(PARAMETERS);
		
		if(action.equalsIgnoreCase(DOWNLOAD_JSON_FILES_BUNDLE_EXTRA)) {
			if(!downloadJsonFiles(parameters)) {
				receiver.send(INCORRECTLY_DOWNLOADED_JSON_FILES, Bundle.EMPTY);
			} else { 
				receiver.send(CORRECTLY_DOWNLOADED_JSON_FILES, Bundle.EMPTY);
			}
		} else if(action.equalsIgnoreCase(UPDATE_PROVIDER_DOTJSON)) {
			Bundle result = updateProviderDotJSON(parameters);
			if(result.getBoolean(RESULT_KEY)) {
				receiver.send(CORRECTLY_UPDATED_PROVIDER_DOT_JSON, result);
			} else { 
				receiver.send(INCORRECTLY_UPDATED_PROVIDER_DOT_JSON, Bundle.EMPTY);
			}
		} else if (action.equalsIgnoreCase(DOWNLOAD_NEW_PROVIDER_DOTJSON)) {
			Bundle result = downloadNewProviderDotJSON(parameters);
			if(result.getBoolean(RESULT_KEY)) {
				receiver.send(CORRECTLY_UPDATED_PROVIDER_DOT_JSON, result);
			} else {
				receiver.send(INCORRECTLY_DOWNLOADED_JSON_FILES, Bundle.EMPTY);
			}
		} else if (action.equalsIgnoreCase(SRP_AUTH)) {
			Bundle session_id_bundle = authenticateBySRP(parameters);
			if(session_id_bundle.getBoolean(RESULT_KEY)) {
				receiver.send(SRP_AUTHENTICATION_SUCCESSFUL, session_id_bundle);
			} else {
				receiver.send(SRP_AUTHENTICATION_FAILED, session_id_bundle);
			}
		} else if (action.equalsIgnoreCase(LOG_OUT)) {
			if(logOut(parameters)) {
				receiver.send(LOGOUT_SUCCESSFUL, Bundle.EMPTY);
			} else {
				receiver.send(LOGOUT_FAILED, Bundle.EMPTY);
			}
		} else if (action.equalsIgnoreCase(DOWNLOAD_CERTIFICATE)) {
			if(getNewCert(parameters)) {
				receiver.send(CORRECTLY_DOWNLOADED_CERTIFICATE, Bundle.EMPTY);
			} else {
				receiver.send(INCORRECTLY_DOWNLOADED_CERTIFICATE, Bundle.EMPTY);
			}
		}
	}

	/**
	 * Downloads the main cert and the eip-service.json files given through the task parameter
	 * @param task
	 * @return true if eip-service.json was parsed as a JSON object correctly.
	 */
	private boolean downloadJsonFiles(Bundle task) {
		String cert_url = task.getString(Provider.CA_CERT);
		String eip_service_json_url = task.getString(EIP.KEY);
		boolean danger_on = task.getBoolean(ProviderItem.DANGER_ON);
		try {
			String cert_string = downloadWithCommercialCA(cert_url, danger_on);
			if(cert_string.isEmpty()) return false;
			X509Certificate certCert = ConfigHelper.parseX509CertificateFromString(cert_string);
			cert_string = Base64.encodeToString( certCert.getEncoded(), Base64.DEFAULT);
			ConfigHelper.saveSharedPref(Provider.CA_CERT, "-----BEGIN CERTIFICATE-----\n"+cert_string+"-----END CERTIFICATE-----");
			
			String eip_service_string = downloadWithCommercialCA(eip_service_json_url, danger_on);
			ConfigHelper.saveSharedPref(EIP.KEY, new JSONObject(eip_service_string));
			
			return true;
		} catch (JSONException e) {
			return false;
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Starts the authentication process using SRP protocol.
	 * 
	 * @param task containing: username, password and api url. 
	 * @return a bundle with a boolean value mapped to a key named RESULT_KEY, and which is true if authentication was successful. 
	 */
	private Bundle authenticateBySRP(Bundle task) {
		Bundle session_id_bundle = new Bundle();
		
		String username = (String) task.get(LogInDialog.USERNAME);
		String password = (String) task.get(LogInDialog.PASSWORD);
		if(validUserLoginData(username, password)) {
			String authentication_server = (String) task.get(Provider.API_URL);

			SRPParameters params = new SRPParameters(new BigInteger(ConfigHelper.NG_1024, 16).toByteArray(), ConfigHelper.G.toByteArray(), BigInteger.ZERO.toByteArray(), "SHA-256");
			LeapSRPSession client = new LeapSRPSession(username, password, params);
			byte[] A = client.exponential();
			try {
				JSONObject saltAndB = sendAToSRPServer(authentication_server, username, new BigInteger(1, A).toString(16));
				if(saltAndB.length() > 0) {
					String salt = saltAndB.getString(LeapSRPSession.SALT);
					byte[] Bbytes = new BigInteger(saltAndB.getString("B"), 16).toByteArray();
					byte[] M1 = client.response(new BigInteger(salt, 16).toByteArray(), Bbytes);
					if(M1 != null) {
					JSONObject session_idAndM2 = sendM1ToSRPServer(authentication_server, username, M1);
					if(session_idAndM2.has(LeapSRPSession.M2) && client.verify((byte[])session_idAndM2.get(LeapSRPSession.M2))) {
						session_id_bundle.putBoolean(RESULT_KEY, true);
					} else {
						session_id_bundle.putBoolean(RESULT_KEY, false);
						session_id_bundle.putString(getResources().getString(R.string.user_message), getResources().getString(R.string.error_bad_user_password_user_message));
						session_id_bundle.putString(LogInDialog.USERNAME, username);
					}
					} else {
						session_id_bundle.putBoolean(RESULT_KEY, false);
						session_id_bundle.putString(LogInDialog.USERNAME, username);
						session_id_bundle.putString(getResources().getString(R.string.user_message), getResources().getString(R.string.error_srp_math_error_user_message));
					}
				} else {
					session_id_bundle.putString(getResources().getString(R.string.user_message), getResources().getString(R.string.error_bad_user_password_user_message));
					session_id_bundle.putString(LogInDialog.USERNAME, username);
					session_id_bundle.putBoolean(RESULT_KEY, false);
				}
			} catch (ClientProtocolException e) {
				session_id_bundle.putBoolean(RESULT_KEY, false);
				session_id_bundle.putString(getResources().getString(R.string.user_message), getResources().getString(R.string.error_client_http_user_message));
				session_id_bundle.putString(LogInDialog.USERNAME, username);
			} catch (IOException e) {
				session_id_bundle.putBoolean(RESULT_KEY, false);
				session_id_bundle.putString(getResources().getString(R.string.user_message), getResources().getString(R.string.error_io_exception_user_message));
				session_id_bundle.putString(LogInDialog.USERNAME, username);
			} catch (JSONException e) {
				session_id_bundle.putBoolean(RESULT_KEY, false);
				session_id_bundle.putString(getResources().getString(R.string.user_message), getResources().getString(R.string.error_json_exception_user_message));
				session_id_bundle.putString(LogInDialog.USERNAME, username);
			} catch (NoSuchAlgorithmException e) {
				session_id_bundle.putBoolean(RESULT_KEY, false);
				session_id_bundle.putString(getResources().getString(R.string.user_message), getResources().getString(R.string.error_no_such_algorithm_exception_user_message));
				session_id_bundle.putString(LogInDialog.USERNAME, username);
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
			if(!wellFormedPassword(password)) {
				session_id_bundle.putBoolean(RESULT_KEY, false);
				session_id_bundle.putString(LogInDialog.USERNAME, username);
				session_id_bundle.putBoolean(LogInDialog.PASSWORD_INVALID_LENGTH, true);
			}
			if(username.isEmpty()) {
				session_id_bundle.putBoolean(RESULT_KEY, false);
				session_id_bundle.putBoolean(LogInDialog.USERNAME_MISSING, true);
			}
		}
		
		return session_id_bundle;
	}

	/**
	 * Validates parameters entered by the user to log in
	 * @param entered_username
	 * @param entered_password
	 * @return true if both parameters are present and the entered password length is greater or equal to eight (8).
	 */
	private boolean validUserLoginData(String entered_username, String entered_password) {
		return !(entered_username.isEmpty()) && wellFormedPassword(entered_password);
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
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws JSONException
	 * @throws CertificateException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyStoreException 
	 * @throws KeyManagementException 
	 */
	private JSONObject sendAToSRPServer(String server_url, String username, String clientA) throws ClientProtocolException, IOException, JSONException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("login", username);
		parameters.put("A", clientA);
		return sendToServer(server_url + "/sessions.json", "POST", parameters);
		
		/*HttpPost post = new HttpPost(server_url + "/sessions.json" + "?" + "login=" + username + "&&" + "A=" + clientA);
		return sendToServer(post);*/
	}

	/**
	 * Sends an HTTP PUT request to the authentication server with the SRP Parameter M1 (or simply M).
	 * @param server_url
	 * @param username
	 * @param m1 Second SRP parameter sent 
	 * @return response from authentication server
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws JSONException
	 * @throws CertificateException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyStoreException 
	 * @throws KeyManagementException 
	 */
	private JSONObject sendM1ToSRPServer(String server_url, String username, byte[] m1) throws ClientProtocolException, IOException, JSONException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("client_auth", new BigInteger(1, ConfigHelper.trim(m1)).toString(16));
		
		//HttpPut put = new HttpPut(server_url + "/sessions/" + username +".json" + "?" + "client_auth" + "=" + new BigInteger(1, ConfigHelper.trim(m1)).toString(16));
		JSONObject json_response = sendToServer(server_url + "/sessions/" + username +".json", "PUT", parameters);

		JSONObject session_idAndM2 = new JSONObject();
		if(json_response.length() > 0) {
			byte[] M2_not_trimmed = new BigInteger(json_response.getString(LeapSRPSession.M2), 16).toByteArray();
			/*Cookie session_id_cookie = LeapHttpClient.getInstance(getApplicationContext()).getCookieStore().getCookies().get(0);
			session_idAndM2.put(ConfigHelper.SESSION_ID_COOKIE_KEY, session_id_cookie.getName());
			session_idAndM2.put(ConfigHelper.SESSION_ID_KEY, session_id_cookie.getValue());*/
			session_idAndM2.put(LeapSRPSession.M2, ConfigHelper.trim(M2_not_trimmed));
		}
		return session_idAndM2;
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
		urlConnection.setSSLSocketFactory(getProviderSSLSocketFactory());
		try {
			
			DataOutputStream writer = new DataOutputStream(urlConnection.getOutputStream());
			writer.writeBytes(formatHttpParameters(parameters));
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
				if(!json_response.isNull(ERRORS) || json_response.has(ERRORS)) {
					return new JSONObject();
				}
			}
		}

		return json_response;
	}
	
	private String formatHttpParameters(Map<String, String> parameters) throws UnsupportedEncodingException	{
	    StringBuilder result = new StringBuilder();
	    boolean first = true;

		Iterator<String> parameter_iterator = parameters.keySet().iterator();
		while(parameter_iterator.hasNext()) {
			if(first)
				first = false;
			else
				result.append("&&");
			
			String key = parameter_iterator.next();
			String value = parameters.get(key);

	        result.append(URLEncoder.encode(key, "UTF-8"));
	        result.append("=");
	        result.append(URLEncoder.encode(value, "UTF-8"));
		}

	    return result.toString();
	}
	/**
	 * Downloads a provider.json from a given URL, adding a new provider using the given name.  
	 * @param task containing a boolean meaning if the provider is custom or not, another boolean meaning if the user completely trusts this provider, the provider name and its provider.json url.
	 * @return a bundle with a boolean value mapped to a key named RESULT_KEY, and which is true if the update was successful. 
	 */
	private Bundle updateProviderDotJSON(Bundle task) {
		Bundle result = new Bundle();
		boolean custom = task.getBoolean(ProviderItem.CUSTOM);
		boolean danger_on = task.getBoolean(ProviderItem.DANGER_ON);
		String provider_json_url = task.getString(Provider.DOT_JSON_URL);
		String provider_name = task.getString(Provider.NAME);
		
		try {
			String provider_dot_json_string = downloadWithCommercialCA(provider_json_url, danger_on);
			if(provider_dot_json_string.isEmpty()) {
				result.putBoolean(RESULT_KEY, false);
			} else {
				JSONObject provider_json = new JSONObject(provider_dot_json_string);
				ConfigHelper.saveSharedPref(EIP.ALLOWED_ANON, provider_json.getJSONObject(Provider.SERVICE).getBoolean(EIP.ALLOWED_ANON));

				//ProviderListContent.addItem(new ProviderItem(provider_name, provider_json_url, provider_json, custom, danger_on));
				result.putBoolean(RESULT_KEY, true);
				result.putString(Provider.KEY, provider_json.toString());
				result.putBoolean(ProviderItem.DANGER_ON, danger_on);
			}
		} catch (JSONException e) {
			result.putBoolean(RESULT_KEY, false);
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
		boolean danger_on = task.getBoolean(ProviderItem.DANGER_ON);
		
		String provider_main_url = (String) task.get(Provider.MAIN_URL);
		String provider_name = provider_main_url.replaceFirst("http[s]?://", "").replaceFirst("\\/", "_");
		String provider_json_url = guessProviderDotJsonURL(provider_main_url);
		
		String provider_json_string = downloadWithCommercialCA(provider_json_url, danger_on);
		try {
			if(provider_json_string.isEmpty()) {
				result.putBoolean(RESULT_KEY, false);
			} else {
				JSONObject provider_json = new JSONObject(provider_json_string);

				ConfigHelper.saveSharedPref(Provider.KEY, provider_json);
				ConfigHelper.saveSharedPref(ProviderItem.DANGER_ON, danger_on);
				ConfigHelper.saveSharedPref(EIP.ALLOWED_ANON, provider_json.getJSONObject(Provider.SERVICE).getBoolean(EIP.ALLOWED_ANON));
				ProviderItem added_provider = new ProviderItem(provider_name, provider_json_url, provider_json, custom, danger_on);
				ProviderListContent.addItem(added_provider);

				result.putString(Provider.NAME, added_provider.getName());
				result.putBoolean(RESULT_KEY, true);
				result.putString(Provider.KEY, provider_json.toString());
				result.putBoolean(ProviderItem.DANGER_ON, danger_on);
			}
		} catch (JSONException e) {
			result.putBoolean(RESULT_KEY, false);
		}
		
		return result;
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
		} catch (FileNotFoundException e) {
			e.printStackTrace();
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
			urlConnection.setSSLSocketFactory(getProviderSSLSocketFactory());
			json_file_content = new Scanner(urlConnection.getInputStream()).useDelimiter("\\A").next();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			displayToast(R.string.server_is_down_message);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
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
	
	private javax.net.ssl.SSLSocketFactory getProviderSSLSocketFactory() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, KeyManagementException {
		String provider_cert_string = ConfigHelper.getStringFromSharedPref(Provider.CA_CERT);

		java.security.cert.Certificate provider_certificate = ConfigHelper.parseX509CertificateFromString(provider_cert_string);

		// Create a KeyStore containing our trusted CAs
		String keyStoreType = KeyStore.getDefaultType();
		KeyStore keyStore = KeyStore.getInstance(keyStoreType);
		keyStore.load(null, null);
		keyStore.setCertificateEntry("provider_ca_certificate", provider_certificate);

		// Create a TrustManager that trusts the CAs in our KeyStore
		String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
		tmf.init(keyStore);

		// Create an SSLContext that uses our TrustManager
		SSLContext context = SSLContext.getInstance("TLS");
		context.init(null, tmf.getTrustManagers(), null);

		return context.getSocketFactory();
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
			System.out.println("String ignoring certificate = " + string);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			displayToast(R.string.server_is_down_message);
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
	 * Tries to guess the provider.json url given the main provider url.
	 * @param provider_main_url
	 * @return the guessed provider.json url
	 */
	private String guessProviderDotJsonURL(String provider_main_url) {
		return provider_main_url + "/provider.json";
	}
	
	/**
	 * Logs out from the api url retrieved from the task.
	 * @param task containing api url from which the user will log out
	 * @return true if there were no exceptions
	 */
	private boolean logOut(Bundle task) {
		try {
			String delete_url = task.getString(Provider.API_URL) + "/logout";

			HttpsURLConnection urlConnection = (HttpsURLConnection)new URL(delete_url).openConnection();
			urlConnection.setRequestMethod("DELETE");
			urlConnection.setSSLSocketFactory(getProviderSSLSocketFactory());

			int responseCode = urlConnection.getResponseCode();
			Log.d("logout", Integer.toString(responseCode));
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
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

	/**
	 * Downloads a new OpenVPN certificate, attaching authenticated cookie for authenticated certificate.
	 * 
	 * @param task containing the type of the certificate to be downloaded
	 * @return true if certificate was downloaded correctly, false if provider.json or danger_on flag are not present in SharedPreferences, or if the certificate url could not be parsed as a URI, or if there was an SSL error. 
	 */
	private boolean getNewCert(Bundle task) {

		try {
			String type_of_certificate = task.getString(ConfigurationWizard.TYPE_OF_CERTIFICATE);
			JSONObject provider_json = ConfigHelper.getJsonFromSharedPref(Provider.KEY);
			URL provider_main_url = new URL(provider_json.getString(Provider.API_URL));
			String new_cert_string_url = provider_main_url.toString() + "/" + provider_json.getString(Provider.API_VERSION) + "/" + EIP.CERTIFICATE;

			boolean danger_on = ConfigHelper.getBoolFromSharedPref(ProviderItem.DANGER_ON);
			String cert_string = downloadWithCommercialCA(new_cert_string_url, danger_on);
			if(!cert_string.isEmpty()) {
				// API returns concatenated cert & key.  Split them for OpenVPN options
				String certificateString = null, keyString = null;
				String[] certAndKey = cert_string.split("(?<=-\n)");
				for (int i=0; i < certAndKey.length-1; i++){
					if ( certAndKey[i].contains("KEY") ) {
						keyString = certAndKey[i++] + certAndKey[i];
					}
					else if ( certAndKey[i].contains("CERTIFICATE") ) {
						certificateString = certAndKey[i++] + certAndKey[i];
					}
				}
				try {
					RSAPrivateKey keyCert = ConfigHelper.parseRsaKeyFromString(keyString);
					keyString = Base64.encodeToString( keyCert.getEncoded(), Base64.DEFAULT );
					ConfigHelper.saveSharedPref(EIP.PRIVATE_KEY, "-----BEGIN RSA PRIVATE KEY-----\n"+keyString+"-----END RSA PRIVATE KEY-----");
					
					X509Certificate certCert = ConfigHelper.parseX509CertificateFromString(certificateString);
					certificateString = Base64.encodeToString( certCert.getEncoded(), Base64.DEFAULT);
					ConfigHelper.saveSharedPref(EIP.CERTIFICATE, "-----BEGIN CERTIFICATE-----\n"+certificateString+"-----END CERTIFICATE-----");
					
					return true;
				} catch (CertificateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return false;
				}
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
		} /*catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}*/
	}
}
