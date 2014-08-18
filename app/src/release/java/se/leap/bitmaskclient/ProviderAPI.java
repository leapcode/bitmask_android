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
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import javax.net.ssl.SSLHandshakeException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.NoSuchElementException;

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
    SET_UP_PROVIDER = "setUpProvider",
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
    ERRORS = "errors",
    UPDATE_PROGRESSBAR = "update_progressbar",
    CURRENT_PROGRESS = "current_progress",
    TAG = "provider_api_tag"
    ;

    final public static int
    CUSTOM_PROVIDER_ADDED = 0,
    SRP_AUTHENTICATION_SUCCESSFUL = 3,
    SRP_AUTHENTICATION_FAILED = 4,
    SRP_REGISTRATION_SUCCESSFUL = 5,
    SRP_REGISTRATION_FAILED = 6,
    LOGOUT_SUCCESSFUL = 7,
    LOGOUT_FAILED = 8,
    CORRECTLY_DOWNLOADED_CERTIFICATE = 9,
    INCORRECTLY_DOWNLOADED_CERTIFICATE = 10,
    PROVIDER_OK = 11,
    PROVIDER_NOK = 12,
    CORRECTLY_DOWNLOADED_ANON_CERTIFICATE = 13,
    INCORRECTLY_DOWNLOADED_ANON_CERTIFICATE = 14
    ;

    private static boolean 
    CA_CERT_DOWNLOADED = false,
    PROVIDER_JSON_DOWNLOADED = false,
    EIP_SERVICE_JSON_DOWNLOADED = false
    ;
    
    private static String last_provider_main_url;
    private static boolean setting_up_provider = true;
    
    public static void stop() {
    	setting_up_provider = false;
    }

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
	
	public static String lastProviderMainUrl() {
		return last_provider_main_url;
	}
	
	private String formatErrorMessage(final int toast_string_id) {
		return "{ \"" + ERRORS + "\" : \""+getResources().getString(toast_string_id)+"\" }";
	}

	@Override
	protected void onHandleIntent(Intent command) {
		final ResultReceiver receiver = command.getParcelableExtra(RECEIVER_KEY);
		String action = command.getAction();
		Bundle parameters = command.getBundleExtra(PARAMETERS);
		setting_up_provider = true;
		
		if(action.equalsIgnoreCase(SET_UP_PROVIDER)) {
			Bundle result = setUpProvider(parameters);
			if(setting_up_provider) {
				if(result.getBoolean(RESULT_KEY)) {
					receiver.send(PROVIDER_OK, result);
				} else { 
					receiver.send(PROVIDER_NOK, result);
				}
			}
		} else if (action.equalsIgnoreCase(SRP_REGISTER)) {
		    Bundle session_id_bundle = registerWithSRP(parameters);
		    if(session_id_bundle.getBoolean(RESULT_KEY)) {
			receiver.send(SRP_REGISTRATION_SUCCESSFUL, session_id_bundle);
		    } else {
			receiver.send(SRP_REGISTRATION_FAILED, session_id_bundle);
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
		    Log.d(TAG, "action.equalsIgnoreCase(DOWNLOAD_CERTIFICATE)");
				if(updateVpnCertificate()) {
					receiver.send(CORRECTLY_DOWNLOADED_CERTIFICATE, Bundle.EMPTY);
				} else {
					receiver.send(INCORRECTLY_DOWNLOADED_CERTIFICATE, Bundle.EMPTY);
				}
		}
	}

    private Bundle registerWithSRP(Bundle task) {
	Bundle session_id_bundle = new Bundle();
	int progress = 0;
		
	String username = (String) task.get(LogInDialog.USERNAME);
	String password = (String) task.get(LogInDialog.PASSWORD);
	String authentication_server = (String) task.get(Provider.API_URL);
	if(validUserLoginData(username, password)) {
		
	    SRPParameters params = new SRPParameters(new BigInteger(ConfigHelper.NG_1024, 16).toByteArray(), ConfigHelper.G.toByteArray(), BigInteger.ZERO.toByteArray(), "SHA-256");
	    LeapSRPSession client = new LeapSRPSession(username, password, params);
	    byte[] salt = ConfigHelper.trim(client.calculateNewSalt());
	    // byte[] salted_password = client.calculatePasswordHash(username, password, salt);
	    /* Calculate password verifier */
	    BigInteger password_verifier = client.calculateV(username, password, salt);
	    /* Send to the server */
	    JSONObject result = sendNewUserDataToSRPServer(authentication_server, username, new BigInteger(1, salt).toString(16), password_verifier.toString(16));
	    if(result.has(ERRORS))
		session_id_bundle = authFailedNotification(result, username);
	    else {
		session_id_bundle.putString(LogInDialog.USERNAME, username);
		session_id_bundle.putString(LogInDialog.PASSWORD, password);
		session_id_bundle.putBoolean(RESULT_KEY, true);
	    }
	    Log.d(TAG, result.toString());
	    broadcast_progress(progress++);
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
	 * Starts the authentication process using SRP protocol.
	 * 
	 * @param task containing: username, password and api url. 
	 * @return a bundle with a boolean value mapped to a key named RESULT_KEY, and which is true if authentication was successful. 
	 */
	private Bundle authenticateBySRP(Bundle task) {
	    Bundle session_id_bundle = new Bundle();
	    int progress = 0;
		
	    String username = (String) task.get(LogInDialog.USERNAME);
	    String password = (String) task.get(LogInDialog.PASSWORD);
	    if(validUserLoginData(username, password)) {
		
		String authentication_server = (String) task.get(Provider.API_URL);
		JSONObject authentication_step_result = new JSONObject();

		SRPParameters params = new SRPParameters(new BigInteger(ConfigHelper.NG_1024, 16).toByteArray(), ConfigHelper.G.toByteArray(), BigInteger.ZERO.toByteArray(), "SHA-256");
		LeapSRPSession client = new LeapSRPSession(username, password, params);
		byte[] A = client.exponential();
		broadcast_progress(progress++);
		authentication_step_result = sendAToSRPServer(authentication_server, username, new BigInteger(1, A).toString(16));
		try {
		    String salt = authentication_step_result.getString(LeapSRPSession.SALT);
		    broadcast_progress(progress++);
		    byte[] Bbytes = new BigInteger(authentication_step_result.getString("B"), 16).toByteArray();
		    byte[] M1 = client.response(new BigInteger(salt, 16).toByteArray(), Bbytes);
		    if(M1 != null) {
			broadcast_progress(progress++);
			authentication_step_result = sendM1ToSRPServer(authentication_server, username, M1);
			setTokenIfAvailable(authentication_step_result);
			byte[] M2 = new BigInteger(authentication_step_result.getString(LeapSRPSession.M2), 16).toByteArray();
			if(client.verify(M2)) {
			    session_id_bundle.putBoolean(RESULT_KEY, true);
			    broadcast_progress(progress++);
			} else {
			    authFailedNotification(authentication_step_result, username);
			}
		    } else {
			session_id_bundle.putBoolean(RESULT_KEY, false);
			session_id_bundle.putString(LogInDialog.USERNAME, username);
			session_id_bundle.putString(getResources().getString(R.string.user_message), getResources().getString(R.string.error_srp_math_error_user_message));
		    }
		} catch (JSONException e) {
		    session_id_bundle = authFailedNotification(authentication_step_result, username);
		    e.printStackTrace();
		}
		broadcast_progress(progress++);
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

    private boolean setTokenIfAvailable(JSONObject authentication_step_result) {
	try {
	    LeapSRPSession.setToken(authentication_step_result.getString(LeapSRPSession.TOKEN));
	    CookieHandler.setDefault(null); // we don't need cookies anymore
	} catch(JSONException e) { //
	    return false;
	}
	return true;
    }
    
    private Bundle authFailedNotification(JSONObject result, String username) {
	Bundle user_notification_bundle = new Bundle();
	try{
	    JSONObject error_message = result.getJSONObject(ERRORS);
	    String error_type = error_message.keys().next().toString();
	    String message = error_message.get(error_type).toString();
	    user_notification_bundle.putString(getResources().getString(R.string.user_message), message);
	} catch(JSONException e) {}
	
	if(!username.isEmpty())
	    user_notification_bundle.putString(LogInDialog.USERNAME, username);
	user_notification_bundle.putBoolean(RESULT_KEY, false);

	return user_notification_bundle;
    }
	
	/**
	 * Sets up an intent with the progress value passed as a parameter
	 * and sends it as a broadcast.
	 * @param progress
	 */
	private void broadcast_progress(int progress) {
		Intent intentUpdate = new Intent();
		intentUpdate.setAction(UPDATE_PROGRESSBAR);
		intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
		intentUpdate.putExtra(CURRENT_PROGRESS, progress);
		sendBroadcast(intentUpdate);
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
    private JSONObject sendAToSRPServer(String server_url, String username, String clientA) {
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
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws JSONException
	 * @throws CertificateException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyStoreException 
	 * @throws KeyManagementException 
	 */
    private JSONObject sendM1ToSRPServer(String server_url, String username, byte[] m1) {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("client_auth", new BigInteger(1, ConfigHelper.trim(m1)).toString(16));
		return sendToServer(server_url + "/sessions/" + username +".json", "PUT", parameters);
	}

	/**
	 * Sends an HTTP POST request to the api server to register a new user.
	 * @param server_url
	 * @param username
	 * @param salted_password
	 * @param password_verifier   
	 * @return response from authentication server
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws JSONException
	 * @throws CertificateException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyStoreException 
	 * @throws KeyManagementException 
	 */
    private JSONObject sendNewUserDataToSRPServer(String server_url, String username, String salt, String password_verifier) {
	Map<String, String> parameters = new HashMap<String, String>();
	parameters.put("user[login]", username);
	parameters.put("user[password_salt]", salt);
	parameters.put("user[password_verifier]", password_verifier);
	Log.d(TAG, server_url);
	Log.d(TAG, parameters.toString());
	return sendToServer(server_url + "/users.json", "POST", parameters);
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
	private JSONObject sendToServer(String url, String request_method, Map<String, String> parameters) {
	    JSONObject json_response;
	    HttpsURLConnection urlConnection = null;
	    try {
		InputStream is = null;
		urlConnection = (HttpsURLConnection)new URL(url).openConnection();
		urlConnection.setRequestMethod(request_method);
		urlConnection.setChunkedStreamingMode(0);
		urlConnection.setSSLSocketFactory(getProviderSSLSocketFactory());
		
		DataOutputStream writer = new DataOutputStream(urlConnection.getOutputStream());
		writer.writeBytes(formatHttpParameters(parameters));
		writer.close();

		is = urlConnection.getInputStream();
		String plain_response = new Scanner(is).useDelimiter("\\A").next();
		json_response = new JSONObject(plain_response);
	    } catch (ClientProtocolException e) {
		json_response = getErrorMessage(urlConnection);
		e.printStackTrace();
	    } catch (IOException e) {
		json_response = getErrorMessage(urlConnection);
		e.printStackTrace();
	    } catch (JSONException e) {
		json_response = getErrorMessage(urlConnection);
		e.printStackTrace();
	    } catch (NoSuchAlgorithmException e) {
		json_response = getErrorMessage(urlConnection);
		e.printStackTrace();
	    } catch (KeyManagementException e) {
		json_response = getErrorMessage(urlConnection);
		e.printStackTrace();
	    } catch (KeyStoreException e) {
		json_response = getErrorMessage(urlConnection);
		e.printStackTrace();
	    } catch (CertificateException e) {
		json_response = getErrorMessage(urlConnection);
		e.printStackTrace();
	    }

	    return json_response;
	}

    private JSONObject getErrorMessage(HttpsURLConnection urlConnection) {
	JSONObject error_message = new JSONObject();
	if(urlConnection != null) {
	    InputStream error_stream = urlConnection.getErrorStream();
	    if(error_stream != null) {
		String error_response = new Scanner(error_stream).useDelimiter("\\A").next();
		Log.d("Error", error_response);
		try {
		    error_message = new JSONObject(error_response);
		} catch (JSONException e) {
		    Log.d(TAG, e.getMessage());
		    e.printStackTrace();
		}
		urlConnection.disconnect();
	    }
	}
	return error_message;
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
    private Bundle setUpProvider(Bundle task) {
	int progress = 0;
	Bundle current_download = new Bundle();
		
	if(task != null && task.containsKey(Provider.MAIN_URL)) {
	    last_provider_main_url = task.getString(Provider.MAIN_URL);
	    CA_CERT_DOWNLOADED = PROVIDER_JSON_DOWNLOADED = EIP_SERVICE_JSON_DOWNLOADED = false;
	}

	if(!PROVIDER_JSON_DOWNLOADED)
	    current_download = getAndSetProviderJson(last_provider_main_url); 
	if(PROVIDER_JSON_DOWNLOADED || (current_download.containsKey(RESULT_KEY) && current_download.getBoolean(RESULT_KEY))) {
	    broadcast_progress(progress++);
	    PROVIDER_JSON_DOWNLOADED = true;
				
	    if(!CA_CERT_DOWNLOADED)
		current_download = downloadCACert();
	    if(CA_CERT_DOWNLOADED || (current_download.containsKey(RESULT_KEY) && current_download.getBoolean(RESULT_KEY))) {
		broadcast_progress(progress++);
		CA_CERT_DOWNLOADED = true;
		current_download = getAndSetEipServiceJson(); 
		if(current_download.containsKey(RESULT_KEY) && current_download.getBoolean(RESULT_KEY)) {
		    broadcast_progress(progress++);
		    EIP_SERVICE_JSON_DOWNLOADED = true;
		}
	    }
	}
		
	return current_download;
	}
	
	private Bundle downloadCACert() {
		Bundle result = new Bundle();
		try {
		    JSONObject provider_json = new JSONObject(getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE).getString(Provider.KEY, ""));
		    String ca_cert_url = provider_json.getString(Provider.CA_CERT_URI);
		    String cert_string = downloadWithCommercialCA(ca_cert_url);
		    result.putBoolean(RESULT_KEY, true);

		    if(validCertificate(cert_string) && setting_up_provider) {
			getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE).edit().putString(Provider.CA_CERT, cert_string).commit();
			result.putBoolean(RESULT_KEY, true);
		    } else {
			String reason_to_fail = pickErrorMessage(cert_string);
			result.putString(ERRORS, reason_to_fail);
			result.putBoolean(RESULT_KEY, false);
		    }
		} catch (JSONException e) {
		    String reason_to_fail = formatErrorMessage(R.string.malformed_url);
		    result.putString(ERRORS, reason_to_fail);
		    result.putBoolean(RESULT_KEY, false);
		}
		
		return result;
	}
	

	public static boolean caCertDownloaded() {
		return CA_CERT_DOWNLOADED;
	}

	private boolean validCertificate(String cert_string) {
		boolean result = false;
		if(!ConfigHelper.checkErroneousDownload(cert_string)) {
			X509Certificate certCert = ConfigHelper.parseX509CertificateFromString(cert_string);
			try {
				Base64.encodeToString( certCert.getEncoded(), Base64.DEFAULT);
				result = true;
			} catch (CertificateEncodingException e) {
				Log.d(TAG, e.getLocalizedMessage());
			}
		}
		
		return result;
	}
	
	private Bundle getAndSetProviderJson(String provider_main_url) {
		Bundle result = new Bundle();

		if(setting_up_provider) {
			String provider_dot_json_string = downloadWithCommercialCA(provider_main_url + "/provider.json");

			try {
				JSONObject provider_json = new JSONObject(provider_dot_json_string);
				String name = provider_json.getString(Provider.NAME);
				//TODO setProviderName(name);

				getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE).edit().putString(Provider.KEY, provider_json.toString()).commit();
				getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE).edit().putBoolean(EIP.ALLOWED_ANON, provider_json.getJSONObject(Provider.SERVICE).getBoolean(EIP.ALLOWED_ANON)).commit();
				getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE).edit().putBoolean(EIP.ALLOWED_REGISTERED, provider_json.getJSONObject(Provider.SERVICE).getBoolean(EIP.ALLOWED_REGISTERED)).commit();

				result.putBoolean(RESULT_KEY, true);
			} catch (JSONException e) {
				//TODO Error message should be contained in that provider_dot_json_string
				String reason_to_fail = pickErrorMessage(provider_dot_json_string);
				result.putString(ERRORS, reason_to_fail);
				result.putBoolean(RESULT_KEY, false);
			}
		}
		return result;
	}


	
	public static boolean providerJsonDownloaded() {
		return PROVIDER_JSON_DOWNLOADED;
	}

	private Bundle getAndSetEipServiceJson() {
		Bundle result = new Bundle();
		String eip_service_json_string = "";
		if(setting_up_provider) {
			try {
				JSONObject provider_json = new JSONObject(getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE).getString(Provider.KEY, ""));
				String eip_service_url = provider_json.getString(Provider.API_URL) +  "/" + provider_json.getString(Provider.API_VERSION) + "/" + EIP.SERVICE_API_PATH;
				eip_service_json_string = downloadWithProviderCA(eip_service_url);
				JSONObject eip_service_json = new JSONObject(eip_service_json_string);
				eip_service_json.getInt(Provider.API_RETURN_SERIAL);

				getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE).edit().putString(EIP.KEY, eip_service_json.toString()).commit();

				result.putBoolean(RESULT_KEY, true);
			} catch (JSONException e) {
				String reason_to_fail = pickErrorMessage(eip_service_json_string);
				result.putString(ERRORS, reason_to_fail);
				result.putBoolean(RESULT_KEY, false);
			}
		}
		return result;
	}

	public static boolean eipServiceDownloaded() {
		return EIP_SERVICE_JSON_DOWNLOADED;
	}
	
	/**
	 * Interprets the error message as a JSON object and extract the "errors" keyword pair.
	 * If the error message is not a JSON object, then it is returned untouched.
	 * @param string_json_error_message
	 * @return final error message
	 */
	private String pickErrorMessage(String string_json_error_message) {
		String error_message = "";
		try {
			JSONObject json_error_message = new JSONObject(string_json_error_message);
			error_message = json_error_message.getString(ERRORS);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			error_message = string_json_error_message;
		}
		
		return error_message;
	}
	
	/**
	 * Tries to download the contents of the provided url using commercially validated CA certificate from chosen provider.
	 * 
	 * @param string_url
	 * @return
	 */
	private String downloadWithCommercialCA(String string_url) {
		
		String json_file_content = "";
		
		URL provider_url = null;
		int seconds_of_timeout = 1;
		try {
			provider_url = new URL(string_url);
			URLConnection url_connection = provider_url.openConnection();
			url_connection.setConnectTimeout(seconds_of_timeout*1000);
			if(!LeapSRPSession.getToken().isEmpty())
				url_connection.addRequestProperty(LeapSRPSession.AUTHORIZATION_HEADER, "Token token = " + LeapSRPSession.getToken());
			json_file_content = new Scanner(url_connection.getInputStream()).useDelimiter("\\A").next();
		} catch (MalformedURLException e) {
			json_file_content = formatErrorMessage(R.string.malformed_url);
		} catch(SocketTimeoutException e) {
		    e.printStackTrace();
			json_file_content = formatErrorMessage(R.string.server_unreachable_message);
		} catch (SSLHandshakeException e) {
			if(provider_url != null) {
				json_file_content = downloadWithProviderCA(string_url);
			} else {
				json_file_content = formatErrorMessage(R.string.certificate_error);
			}
		} catch(ConnectException e) {
		    json_file_content = formatErrorMessage(R.string.service_is_down_error);
		} catch (FileNotFoundException e) {
		    json_file_content = formatErrorMessage(R.string.malformed_url);
		} catch (Exception e) {
		    e.printStackTrace();
			if(provider_url != null) {
				json_file_content = downloadWithProviderCA(string_url);
			}
		}

		return json_file_content;
	}

	/**
	 * Tries to download the contents of the provided url using not commercially validated CA certificate from chosen provider. 
	 * @param url as a string
	 * @return an empty string if it fails, the url content if not. 
	 */
	private String downloadWithProviderCA(String url_string) {
		String json_file_content = "";

		try {
			URL url = new URL(url_string);
			// Tell the URLConnection to use a SocketFactory from our SSLContext
			HttpsURLConnection urlConnection =
					(HttpsURLConnection)url.openConnection();
			urlConnection.setSSLSocketFactory(getProviderSSLSocketFactory());
			if(!LeapSRPSession.getToken().isEmpty())
				urlConnection.addRequestProperty(LeapSRPSession.AUTHORIZATION_HEADER, "Token token=" + LeapSRPSession.getToken());
			json_file_content = new Scanner(urlConnection.getInputStream()).useDelimiter("\\A").next();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			json_file_content = formatErrorMessage(R.string.server_unreachable_message);
		} catch (IOException e) {
		    // The downloaded certificate doesn't validate our https connection.
		    json_file_content = formatErrorMessage(R.string.certificate_error);
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchElementException e) {
		    json_file_content = formatErrorMessage(R.string.server_unreachable_message);
		}
		return json_file_content;
	}
	
	private javax.net.ssl.SSLSocketFactory getProviderSSLSocketFactory() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, KeyManagementException {
		String provider_cert_string = getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE).getString(Provider.CA_CERT,"");

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
	private String downloadWithoutCA(String url_string) {
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

			URL url = new URL(url_string);
			HttpsURLConnection urlConnection = (HttpsURLConnection)url.openConnection();
			urlConnection.setSSLSocketFactory(context.getSocketFactory());
			urlConnection.setHostnameVerifier(hostnameVerifier);
			string = new Scanner(urlConnection.getInputStream()).useDelimiter("\\A").next();
			System.out.println("String ignoring certificate = " + string);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			string = formatErrorMessage(R.string.malformed_url);
		} catch (IOException e) {
			// The downloaded certificate doesn't validate our https connection.
			e.printStackTrace();
			string = formatErrorMessage(R.string.certificate_error);
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
	 * Logs out from the api url retrieved from the task.
	 * @param task containing api url from which the user will log out
	 * @return true if there were no exceptions
	 */
	private boolean logOut(Bundle task) {
		try {
			String delete_url = task.getString(Provider.API_URL) + "/logout";
			int progress = 0;

			HttpsURLConnection urlConnection = (HttpsURLConnection)new URL(delete_url).openConnection();
			urlConnection.setRequestMethod("DELETE");
			urlConnection.setSSLSocketFactory(getProviderSSLSocketFactory());

			int responseCode = urlConnection.getResponseCode();
			broadcast_progress(progress++);
			LeapSRPSession.setToken("");
			Log.d(TAG, Integer.toString(responseCode));
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

    private boolean updateVpnCertificate() {
	getNewCert();

	getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE).edit().putInt(EIP.PARSED_SERIAL, 0).commit();
	Intent updateEIP = new Intent(getApplicationContext(), EIP.class);
	updateEIP.setAction(EIP.ACTION_UPDATE_EIP_SERVICE);
	startService(updateEIP);

	return true;
    }
    
	/**
	 * Downloads a new OpenVPN certificate, attaching authenticated cookie for authenticated certificate.
	 * 
	 * @return true if certificate was downloaded correctly, false if provider.json is not present in SharedPreferences, or if the certificate url could not be parsed as a URI, or if there was an SSL error. 
	 */
	private boolean getNewCert() {

		try {
			JSONObject provider_json = new JSONObject(getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE).getString(Provider.KEY, ""));
			
			String provider_main_url = provider_json.getString(Provider.API_URL);
			URL new_cert_string_url = new URL(provider_main_url + "/" + provider_json.getString(Provider.API_VERSION) + "/" + EIP.CERTIFICATE);

			String cert_string = downloadWithProviderCA(new_cert_string_url.toString());

			if(!cert_string.isEmpty()) {
				if(ConfigHelper.checkErroneousDownload(cert_string)) {
					String reason_to_fail = provider_json.getString(ERRORS);
					//result.putString(ConfigHelper.ERRORS_KEY, reason_to_fail);
					//result.putBoolean(ConfigHelper.RESULT_KEY, false);
					return false;
				} else {
					
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
						getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE).edit().putString(EIP.PRIVATE_KEY, "-----BEGIN RSA PRIVATE KEY-----\n"+keyString+"-----END RSA PRIVATE KEY-----").commit();

						X509Certificate certCert = ConfigHelper.parseX509CertificateFromString(certificateString);
						certificateString = Base64.encodeToString( certCert.getEncoded(), Base64.DEFAULT);
						getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE).edit().putString(EIP.CERTIFICATE, "-----BEGIN CERTIFICATE-----\n"+certificateString+"-----END CERTIFICATE-----").commit();
						getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE).edit().putString(EIP.DATE_FROM_CERTIFICATE, EIP.certificate_date_format.format(Calendar.getInstance().getTime())).commit();
						return true;
					} catch (CertificateException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return false;
					}
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
