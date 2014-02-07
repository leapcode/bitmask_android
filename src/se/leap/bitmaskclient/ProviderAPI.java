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
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
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
    private static boolean last_danger_on = false;
    
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
	
    public static boolean lastDangerOn() {
    	return last_danger_on;
    }
    
	private String formatErrorMessage(final int toast_string_id) {
		return "{ \"" + ERRORS + "\" : \""+getResources().getString(toast_string_id)+"\" }";
	}

	@Override
	protected void onHandleIntent(Intent command) {
		final ResultReceiver receiver = command.getParcelableExtra(RECEIVER_KEY);
		String action = command.getAction();
		Bundle parameters = command.getBundleExtra(PARAMETERS);
		
		if(action.equalsIgnoreCase(SET_UP_PROVIDER)) {
			Bundle result = setUpProvider(parameters);
			if(result.getBoolean(RESULT_KEY)) {
				receiver.send(PROVIDER_OK, Bundle.EMPTY);
			} else { 
				receiver.send(PROVIDER_NOK, result);
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

			SRPParameters params = new SRPParameters(new BigInteger(ConfigHelper.NG_1024, 16).toByteArray(), ConfigHelper.G.toByteArray(), BigInteger.ZERO.toByteArray(), "SHA-256");
			LeapSRPSession client = new LeapSRPSession(username, password, params);
			byte[] A = client.exponential();
			broadcast_progress(progress++);
			try {
				JSONObject saltAndB = sendAToSRPServer(authentication_server, username, new BigInteger(1, A).toString(16));
				if(saltAndB.length() > 0) {
					String salt = saltAndB.getString(LeapSRPSession.SALT);
					broadcast_progress(progress++);
					byte[] Bbytes = new BigInteger(saltAndB.getString("B"), 16).toByteArray();
					byte[] M1 = client.response(new BigInteger(salt, 16).toByteArray(), Bbytes);
					if(M1 != null) {
					broadcast_progress(progress++);
					JSONObject session_idAndM2 = sendM1ToSRPServer(authentication_server, username, M1);
					  if(session_idAndM2.has(LeapSRPSession.M2) && client.verify((byte[])session_idAndM2.get(LeapSRPSession.M2))) {
						  session_id_bundle.putBoolean(RESULT_KEY, true);
						  broadcast_progress(progress++);
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
					broadcast_progress(progress++);
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
			CookieHandler.setDefault(null); // we don't need cookies anymore
			String token = json_response.getString(LeapSRPSession.TOKEN);
			LeapSRPSession.setToken(token);
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
	private Bundle setUpProvider(Bundle task) {
//<<<<<<< HEAD
//		Bundle result = new Bundle();
//		int progress = 0;
//		boolean danger_on = task.getBoolean(ProviderItem.DANGER_ON);
//		String provider_main_url = task.getString(Provider.MAIN_URL);
//		if(downloadCACert(provider_main_url, danger_on)) {
//			broadcast_progress(progress++);
//			result.putBoolean(RESULT_KEY, true);
//			if(getAndSetProviderJson(provider_main_url)) {
//				broadcast_progress(progress++);
//				if(getAndSetEipServiceJson())
//					broadcast_progress(progress++);
//			}
//		}
//		return result;
//	}
//	
//	
//=======
		int progress = 0;
		Bundle current_download = new Bundle();
		
		if(task != null && task.containsKey(ProviderItem.DANGER_ON) && task.containsKey(Provider.MAIN_URL)) {
			last_danger_on = task.getBoolean(ProviderItem.DANGER_ON);
			last_provider_main_url = task.getString(Provider.MAIN_URL);
			CA_CERT_DOWNLOADED = PROVIDER_JSON_DOWNLOADED = EIP_SERVICE_JSON_DOWNLOADED = false;
		}

		if(!CA_CERT_DOWNLOADED)
			current_download = downloadCACert(last_provider_main_url, last_danger_on);
		if(CA_CERT_DOWNLOADED || (current_download.containsKey(RESULT_KEY) && current_download.getBoolean(RESULT_KEY))) {
			broadcast_progress(progress++);
			CA_CERT_DOWNLOADED = true;
			if(!PROVIDER_JSON_DOWNLOADED)
				current_download = getAndSetProviderJson(last_provider_main_url); 
			if(PROVIDER_JSON_DOWNLOADED || (current_download.containsKey(RESULT_KEY) && current_download.getBoolean(RESULT_KEY))) {
				broadcast_progress(progress++);
				PROVIDER_JSON_DOWNLOADED = true;
				current_download = getAndSetEipServiceJson(); 
				if(current_download.containsKey(RESULT_KEY) && current_download.getBoolean(RESULT_KEY)) {
					broadcast_progress(progress++);
					EIP_SERVICE_JSON_DOWNLOADED = true;
				}
			}
		}
		
		return current_download;
	}
	
	private Bundle downloadCACert(String provider_main_url, boolean danger_on) {
		Bundle result = new Bundle();
		String cert_string = downloadWithCommercialCA(provider_main_url + "/ca.crt", danger_on);
		if(validCertificate(cert_string)) {
			ConfigHelper.saveSharedPref(Provider.CA_CERT, cert_string);
			result.putBoolean(RESULT_KEY, true);
		} else {
			String reason_to_fail = pickErrorMessage(cert_string);
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

		String provider_dot_json_string = downloadWithProviderCA(provider_main_url + "/provider.json", true);

		try {
			JSONObject provider_json = new JSONObject(provider_dot_json_string);
			String name = provider_json.getString(Provider.NAME);
			//TODO setProviderName(name);
			
			ConfigHelper.saveSharedPref(Provider.KEY, provider_json);
			ConfigHelper.saveSharedPref(EIP.ALLOWED_ANON, provider_json.getJSONObject(Provider.SERVICE).getBoolean(EIP.ALLOWED_ANON));

			result.putBoolean(RESULT_KEY, true);
		} catch (JSONException e) {
			//TODO Error message should be contained in that provider_dot_json_string
			String reason_to_fail = pickErrorMessage(provider_dot_json_string);
			result.putString(ERRORS, reason_to_fail);
			result.putBoolean(RESULT_KEY, false);
		}
		return result;
	}


	
	public static boolean providerJsonDownloaded() {
		return PROVIDER_JSON_DOWNLOADED;
	}

	private Bundle getAndSetEipServiceJson() {
		Bundle result = new Bundle();
		String eip_service_json_string = "";
		try {
			JSONObject provider_json = ConfigHelper.getJsonFromSharedPref(Provider.KEY);
			String eip_service_url = provider_json.getString(Provider.API_URL) +  "/" + provider_json.getString(Provider.API_VERSION) + "/" + EIP.SERVICE_API_PATH;
			eip_service_json_string = downloadWithProviderCA(eip_service_url, true);
			JSONObject eip_service_json = new JSONObject(eip_service_json_string);
			eip_service_json.getInt(Provider.API_RETURN_SERIAL);

			ConfigHelper.saveSharedPref(EIP.KEY, eip_service_json);

			result.putBoolean(RESULT_KEY, true);
		} catch (JSONException e) {
			String reason_to_fail = pickErrorMessage(eip_service_json_string);
			result.putString(ERRORS, reason_to_fail);
			result.putBoolean(RESULT_KEY, false);
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
			if(!LeapSRPSession.getToken().isEmpty())
				url_connection.addRequestProperty(LeapSRPSession.TOKEN, LeapSRPSession.getToken());
			json_file_content = new Scanner(url_connection.getInputStream()).useDelimiter("\\A").next();
		} catch (MalformedURLException e) {
			json_file_content = formatErrorMessage(R.string.malformed_url);
		} catch(SocketTimeoutException e) {
			json_file_content = formatErrorMessage(R.string.server_is_down_message);
		} catch (IOException e) {
			if(provider_url != null) {
				json_file_content = downloadWithProviderCA(string_url, danger_on);
			} else {
				json_file_content = formatErrorMessage(R.string.certificate_error);
			}
		} catch (Exception e) {
			if(provider_url != null && danger_on) {
				json_file_content = downloadWithProviderCA(string_url, danger_on);
			}
		}

		return json_file_content;
	}

	/**
	 * Tries to download the contents of the provided url using not commercially validated CA certificate from chosen provider. 
	 * @param url as a string
	 * @param danger_on true to download CA certificate in case it has not been downloaded.
	 * @return an empty string if it fails, the url content if not. 
	 */
	private String downloadWithProviderCA(String url_string, boolean danger_on) {
		String json_file_content = "";

		try {
			URL url = new URL(url_string);
			// Tell the URLConnection to use a SocketFactory from our SSLContext
			HttpsURLConnection urlConnection =
					(HttpsURLConnection)url.openConnection();
			urlConnection.setSSLSocketFactory(getProviderSSLSocketFactory());
			if(!LeapSRPSession.getToken().isEmpty())
				urlConnection.addRequestProperty(LeapSRPSession.TOKEN, LeapSRPSession.getToken());
			json_file_content = new Scanner(urlConnection.getInputStream()).useDelimiter("\\A").next();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			json_file_content = formatErrorMessage(R.string.server_is_down_message);
		} catch (IOException e) {
			// The downloaded certificate doesn't validate our https connection.
			if(danger_on) {
				json_file_content = downloadWithoutCA(url_string);
			} else {
				json_file_content = formatErrorMessage(R.string.certificate_error);
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
			string = formatErrorMessage(R.string.server_is_down_message);
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
			String provider_main_url = provider_json.getString(Provider.API_URL);
			URL new_cert_string_url = new URL(provider_main_url + "/" + provider_json.getString(Provider.API_VERSION) + "/" + EIP.CERTIFICATE);

			boolean danger_on = ConfigHelper.getBoolFromSharedPref(ProviderItem.DANGER_ON);

			String cert_string = downloadWithProviderCA(new_cert_string_url.toString(), danger_on);

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
