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

import android.app.IntentService;
import android.content.*;
import android.os.*;
import android.util.*;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.*;
import java.security.cert.*;
import java.security.interfaces.RSAPrivateKey;
import java.util.*;
import javax.net.ssl.*;
import org.apache.http.client.ClientProtocolException;
import org.json.*;

import se.leap.bitmaskclient.ProviderListContent.ProviderItem;
import se.leap.bitmaskclient.eip.*;

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
	
    final public static String
    TAG = ProviderAPI.class.getSimpleName(),
    SET_UP_PROVIDER = "setUpProvider",
    DOWNLOAD_NEW_PROVIDER_DOTJSON = "downloadNewProviderDotJSON",
    SIGN_UP = "srpRegister",
    LOG_IN = "srpAuth",
    LOG_OUT = "logOut",
    DOWNLOAD_CERTIFICATE = "downloadUserAuthedCertificate",
    PARAMETERS = "parameters",
    RESULT_KEY = "result",
    RECEIVER_KEY = "receiver",
    ERRORS = "errors",
    UPDATE_PROGRESSBAR = "update_progressbar",
    CURRENT_PROGRESS = "current_progress",
    DOWNLOAD_EIP_SERVICE = TAG + ".DOWNLOAD_EIP_SERVICE"
    ;

    final public static int
            SUCCESSFUL_LOGIN = 3,
    FAILED_LOGIN = 4,
    SUCCESSFUL_SIGNUP = 5,
    FAILED_SIGNUP = 6,
    SUCCESSFUL_LOGOUT = 7,
    LOGOUT_FAILED = 8,
    CORRECTLY_DOWNLOADED_CERTIFICATE = 9,
    INCORRECTLY_DOWNLOADED_CERTIFICATE = 10,
    PROVIDER_OK = 11,
    PROVIDER_NOK = 12,
    CORRECTLY_DOWNLOADED_EIP_SERVICE = 13,
    INCORRECTLY_DOWNLOADED_EIP_SERVICE= 14
    ;

    private static boolean 
    CA_CERT_DOWNLOADED = false,
    PROVIDER_JSON_DOWNLOADED = false,
    EIP_SERVICE_JSON_DOWNLOADED = false
    ;
    
    private static String last_provider_main_url;
    private static boolean last_danger_on = false;
    private static boolean go_ahead = true;
    private static SharedPreferences preferences;
    private static String provider_api_url;
    
    public static void stop() {
    	go_ahead = false;
    }

	public ProviderAPI() {
		super("ProviderAPI");
		Log.v("ClassName", "Provider API");
	}
	
    @Override
    public void onCreate() {
	super.onCreate();
	
	preferences = getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE);
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
        if(provider_api_url == null) {
            try {
                JSONObject provider_json = new JSONObject(preferences.getString(Provider.KEY, "no provider"));
                provider_api_url = provider_json.getString(Provider.API_URL) + "/" + provider_json.getString(Provider.API_VERSION);
                go_ahead = true;
            } catch (JSONException e) {
                go_ahead = false;
            }
        }
		
	if(action.equalsIgnoreCase(SET_UP_PROVIDER)) {
	    Bundle result = setUpProvider(parameters);
	    if(go_ahead) {
		if(result.getBoolean(RESULT_KEY)) {
		    receiver.send(PROVIDER_OK, result);
		} else { 
		    receiver.send(PROVIDER_NOK, result);
		}
	    }
	} else if (action.equalsIgnoreCase(SIGN_UP)) {
	    Bundle result = tryToRegister(parameters);
	    if(result.getBoolean(RESULT_KEY)) {
		receiver.send(SUCCESSFUL_SIGNUP, result);
	    } else {
		receiver.send(FAILED_SIGNUP, result);
	    }
	} else if (action.equalsIgnoreCase(LOG_IN)) {
        UserSessionStatus.updateStatus(UserSessionStatus.SessionStatus.LOGGING_IN);
	    Bundle result = tryToAuthenticate(parameters);
	    if(result.getBoolean(RESULT_KEY)) {
		receiver.send(SUCCESSFUL_LOGIN, result);
		UserSessionStatus.updateStatus(UserSessionStatus.SessionStatus.LOGGED_IN);
	    } else {
		receiver.send(FAILED_LOGIN, result);
		UserSessionStatus.updateStatus(UserSessionStatus.SessionStatus.NOT_LOGGED_IN);
	    }
	} else if (action.equalsIgnoreCase(LOG_OUT)) {
	    UserSessionStatus.updateStatus(UserSessionStatus.SessionStatus.LOGGING_OUT);
	    if(logOut()) {
		receiver.send(SUCCESSFUL_LOGOUT, Bundle.EMPTY);
		UserSessionStatus.updateStatus(UserSessionStatus.SessionStatus.LOGGED_OUT);
	    } else {
		receiver.send(LOGOUT_FAILED, Bundle.EMPTY);
		UserSessionStatus.updateStatus(UserSessionStatus.SessionStatus.DIDNT_LOG_OUT);
	    }
	} else if (action.equalsIgnoreCase(DOWNLOAD_CERTIFICATE)) {
	    if(updateVpnCertificate()) {
		receiver.send(CORRECTLY_DOWNLOADED_CERTIFICATE, Bundle.EMPTY);
	    } else {
		receiver.send(INCORRECTLY_DOWNLOADED_CERTIFICATE, Bundle.EMPTY);
	    }
	} else if(action.equalsIgnoreCase(DOWNLOAD_EIP_SERVICE)) {
            Bundle result = getAndSetEipServiceJson();
            if(result.getBoolean(RESULT_KEY)) {
                receiver.send(CORRECTLY_DOWNLOADED_EIP_SERVICE, result);
            } else {
                receiver.send(INCORRECTLY_DOWNLOADED_EIP_SERVICE, result);
            }
        }
    }

    private Bundle tryToRegister(Bundle task) {
	Bundle result = new Bundle();
	int progress = 0;
		
	String username = User.userName();
	String password = task.getString(SessionDialog.PASSWORD);
	
	if(validUserLoginData(username, password)) {
	    result = register(username, password);
	    broadcastProgress(progress++);
	} else {
	    if(!wellFormedPassword(password)) {
		result.putBoolean(RESULT_KEY, false);
		result.putString(SessionDialog.USERNAME, username);
		result.putBoolean(SessionDialog.PASSWORD_INVALID_LENGTH, true);
	    }
	    if(!validUsername(username)) {
		result.putBoolean(RESULT_KEY, false);
		result.putBoolean(SessionDialog.USERNAME_MISSING, true);
	    }
	}
		
	return result;
    }

    private Bundle register(String username, String password) {	
	LeapSRPSession client = new LeapSRPSession(username, password);
	byte[] salt = client.calculateNewSalt();
	
	BigInteger password_verifier = client.calculateV(username, password, salt);
	
	JSONObject api_result = sendNewUserDataToSRPServer(provider_api_url, username, new BigInteger(1, salt).toString(16), password_verifier.toString(16));
	
	Bundle result = new Bundle();
	if(api_result.has(ERRORS))
	    result = authFailedNotification(api_result, username);
	else {
	    result.putString(SessionDialog.USERNAME, username);
	    result.putString(SessionDialog.PASSWORD, password);
	    result.putBoolean(RESULT_KEY, true);
	}

	return result;
    }
	/**
	 * Starts the authentication process using SRP protocol.
	 * 
	 * @param task containing: username, password and api url. 
	 * @return a bundle with a boolean value mapped to a key named RESULT_KEY, and which is true if authentication was successful. 
	 */
	private Bundle tryToAuthenticate(Bundle task) {
	    Bundle result = new Bundle();
	    int progress = 0;

        String username = User.userName();
	    String password = task.getString(SessionDialog.PASSWORD);
	    if(validUserLoginData(username, password)) {		
		result = authenticate(username, password);
		broadcastProgress(progress++);
	    } else {
		if(!wellFormedPassword(password)) {
		    result.putBoolean(RESULT_KEY, false);
		    result.putString(SessionDialog.USERNAME, username);
		    result.putBoolean(SessionDialog.PASSWORD_INVALID_LENGTH, true);
		}
		if(!validUsername(username)) {
		    result.putBoolean(RESULT_KEY, false);
		    result.putBoolean(SessionDialog.USERNAME_MISSING, true);
		}
	    }
		
	    return result;
	}


    private Bundle authenticate(String username, String password) {
	Bundle result = new Bundle();
	
	LeapSRPSession client = new LeapSRPSession(username, password);
	byte[] A = client.exponential();

	JSONObject step_result = sendAToSRPServer(provider_api_url, username, new BigInteger(1, A).toString(16));
	try {
	    String salt = step_result.getString(LeapSRPSession.SALT);
	    byte[] Bbytes = new BigInteger(step_result.getString("B"), 16).toByteArray();
	    byte[] M1 = client.response(new BigInteger(salt, 16).toByteArray(), Bbytes);
	    if(M1 != null) {
		step_result = sendM1ToSRPServer(provider_api_url, username, M1);
		setTokenIfAvailable(step_result);
		byte[] M2 = new BigInteger(step_result.getString(LeapSRPSession.M2), 16).toByteArray();
		if(client.verify(M2)) {
		    result.putBoolean(RESULT_KEY, true);
		} else {
		    authFailedNotification(step_result, username);
		}
	    } else {
		result.putBoolean(RESULT_KEY, false);
		result.putString(SessionDialog.USERNAME, username);
		result.putString(getResources().getString(R.string.user_message), getResources().getString(R.string.error_srp_math_error_user_message));
	    }
	} catch (JSONException e) {
	    result = authFailedNotification(step_result, username);
	    e.printStackTrace();
	}
	
	return result;
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
	    user_notification_bundle.putString(SessionDialog.USERNAME, username);
	user_notification_bundle.putBoolean(RESULT_KEY, false);

	return user_notification_bundle;
    }
	
	/**
	 * Sets up an intent with the progress value passed as a parameter
	 * and sends it as a broadcast.
	 * @param progress
	 */
	private void broadcastProgress(int progress) {
		Intent intentUpdate = new Intent();
		intentUpdate.setAction(UPDATE_PROGRESSBAR);
		intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
		intentUpdate.putExtra(CURRENT_PROGRESS, progress);
		sendBroadcast(intentUpdate);
	}

	/**
	 * Validates parameters entered by the user to log in
	 * @param username
	 * @param password
	 * @return true if both parameters are present and the entered password length is greater or equal to eight (8).
	 */
	private boolean validUserLoginData(String username, String password) {
		return validUsername(username) && wellFormedPassword(password);
	}

    private boolean validUsername(String username) {
        return username != null && !username.isEmpty();
    }

	/**
	 * Validates a password
	 * @param password
	 * @return true if the entered password length is greater or equal to eight (8).
	 */
	private boolean wellFormedPassword(String password) {
		return password != null && password.length() >= 8;
	}

	/**
	 * Sends an HTTP POST request to the authentication server with the SRP Parameter A.
	 * @param server_url
	 * @param username
	 * @param clientA First SRP parameter sent 
	 * @return response from authentication server
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
	 * @param salt
	 * @param password_verifier   
	 * @return response from authentication server
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
		
		if(task != null && task.containsKey(ProviderItem.DANGER_ON) && task.containsKey(Provider.MAIN_URL)) {
			last_danger_on = task.getBoolean(ProviderItem.DANGER_ON);
			last_provider_main_url = task.getString(Provider.MAIN_URL);
			CA_CERT_DOWNLOADED = PROVIDER_JSON_DOWNLOADED = EIP_SERVICE_JSON_DOWNLOADED = false;
            go_ahead = true;
		}

			if(!PROVIDER_JSON_DOWNLOADED)
				current_download = getAndSetProviderJson(last_provider_main_url, last_danger_on);
			if(PROVIDER_JSON_DOWNLOADED || (current_download.containsKey(RESULT_KEY) && current_download.getBoolean(RESULT_KEY))) {
			    broadcastProgress(progress++);
			    PROVIDER_JSON_DOWNLOADED = true;
			    current_download = downloadCACert(last_danger_on);
			    
			    if(CA_CERT_DOWNLOADED || (current_download.containsKey(RESULT_KEY) && current_download.getBoolean(RESULT_KEY))) {
				broadcastProgress(progress++);
				CA_CERT_DOWNLOADED = true;
				current_download = getAndSetEipServiceJson(); 
				if(current_download.containsKey(RESULT_KEY) && current_download.getBoolean(RESULT_KEY)) {
					broadcastProgress(progress++);
					EIP_SERVICE_JSON_DOWNLOADED = true;
				}
			}
		}
		
		return current_download;
	}
	
	private Bundle downloadCACert(boolean danger_on) {
		Bundle result = new Bundle();
		try {
		    JSONObject provider_json = new JSONObject(preferences.getString(Provider.KEY, ""));
		    String ca_cert_url = provider_json.getString(Provider.CA_CERT_URI);
		    String cert_string = downloadWithCommercialCA(ca_cert_url, danger_on);

		    if(validCertificate(cert_string) && go_ahead) {
			preferences.edit().putString(Provider.CA_CERT, cert_string).commit();
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
	    X509Certificate certificate = ConfigHelper.parseX509CertificateFromString(cert_string);
	    try {
		if(certificate != null) {
		    JSONObject provider_json = new JSONObject(preferences.getString(Provider.KEY, ""));
		    String fingerprint = provider_json.getString(Provider.CA_CERT_FINGERPRINT);
		    String encoding = fingerprint.split(":")[0];
		    String expected_fingerprint = fingerprint.split(":")[1];
		    String real_fingerprint = base64toHex(Base64.encodeToString(
										MessageDigest.getInstance(encoding).digest(certificate.getEncoded()),
										Base64.DEFAULT));

		    result = real_fingerprint.trim().equalsIgnoreCase(expected_fingerprint.trim());
		} else
		    result = false;
	    } catch (JSONException e) {
		result = false;
	    } catch (NoSuchAlgorithmException e) {
		result = false;
	    } catch (CertificateEncodingException e) {
		result = false;
	    }
	}
		
	return result;
    }

    private String base64toHex(String base64_input) {
	byte[] byteArray = Base64.decode(base64_input, Base64.DEFAULT);
	int readBytes = byteArray.length;
	StringBuffer hexData = new StringBuffer();
	int onebyte;
	for (int i=0; i < readBytes; i++) {
	    onebyte = ((0x000000ff & byteArray[i]) | 0xffffff00);
	    hexData.append(Integer.toHexString(onebyte).substring(6));
	}
	return hexData.toString();
    }	
	private Bundle getAndSetProviderJson(String provider_main_url, boolean danger_on) {
		Bundle result = new Bundle();

		if(go_ahead) {
			String provider_dot_json_string = downloadWithCommercialCA(provider_main_url + "/provider.json", danger_on);

			try {
				JSONObject provider_json = new JSONObject(provider_dot_json_string);
				provider_api_url = provider_json.getString(Provider.API_URL) + "/" + provider_json.getString(Provider.API_VERSION);
				String name = provider_json.getString(Provider.NAME);
				//TODO setProviderName(name);

				preferences.edit().putString(Provider.KEY, provider_json.toString()).commit();
				preferences.edit().putBoolean(Constants.ALLOWED_ANON, provider_json.getJSONObject(Provider.SERVICE).getBoolean(Constants.ALLOWED_ANON)).commit();
				preferences.edit().putBoolean(Constants.ALLOWED_REGISTERED, provider_json.getJSONObject(Provider.SERVICE).getBoolean(Constants.ALLOWED_REGISTERED)).commit();

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

	private Bundle getAndSetEipServiceJson() {
		Bundle result = new Bundle();
		String eip_service_json_string = "";
		if(go_ahead) {
			try {
				JSONObject provider_json = new JSONObject(preferences.getString(Provider.KEY, ""));
				String eip_service_url = provider_json.getString(Provider.API_URL) +  "/" + provider_json.getString(Provider.API_VERSION) + "/" + EIP.SERVICE_API_PATH;
				eip_service_json_string = downloadWithProviderCA(eip_service_url, true);
				JSONObject eip_service_json = new JSONObject(eip_service_json_string);
				eip_service_json.getInt(Provider.API_RETURN_SERIAL);

				preferences.edit().putString(Constants.KEY, eip_service_json.toString()).commit();

				result.putBoolean(RESULT_KEY, true);
			} catch (JSONException e) {
				String reason_to_fail = pickErrorMessage(eip_service_json_string);
				result.putString(ERRORS, reason_to_fail);
				result.putBoolean(RESULT_KEY, false);
			}
		}
		return result;
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
		int seconds_of_timeout = 2;
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
			json_file_content = formatErrorMessage(R.string.server_unreachable_message);
		} catch (SSLHandshakeException e) {
			if(provider_url != null) {
			    json_file_content = downloadWithProviderCA(string_url, danger_on);
			} else {
				json_file_content = formatErrorMessage(R.string.certificate_error);
			}
		} catch(ConnectException e) {
		    json_file_content = formatErrorMessage(R.string.service_is_down_error);
		} catch (FileNotFoundException e) {
		    json_file_content = formatErrorMessage(R.string.malformed_url);
		} catch (Exception e) {
			if(provider_url != null && danger_on) {
				json_file_content = downloadWithProviderCA(string_url, danger_on);
			}
		}

		return json_file_content;
	}

	/**
	 * Tries to download the contents of the provided url using not commercially validated CA certificate from chosen provider. 
	 * @param url_string as a string
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
				urlConnection.addRequestProperty(LeapSRPSession.AUTHORIZATION_HEADER, "Token token=" + LeapSRPSession.getToken());
			json_file_content = new Scanner(urlConnection.getInputStream()).useDelimiter("\\A").next();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
		    e.printStackTrace();
			json_file_content = formatErrorMessage(R.string.server_unreachable_message);
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
		} catch (NoSuchElementException e) {
		    e.printStackTrace();
		    json_file_content = formatErrorMessage(R.string.server_unreachable_message);
		}
		return json_file_content;
	}
	
	private javax.net.ssl.SSLSocketFactory getProviderSSLSocketFactory() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, KeyManagementException {
		String provider_cert_string = preferences.getString(Provider.CA_CERT,"");

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
     * @return true if there were no exceptions
     */
    private boolean logOut() {
        String delete_url = provider_api_url + "/logout";

        HttpsURLConnection urlConnection = null;
        int responseCode = 0;
        int progress = 0;
	try {

	    urlConnection = (HttpsURLConnection)new URL(delete_url).openConnection();
	    urlConnection.setRequestMethod("DELETE");
	    urlConnection.setSSLSocketFactory(getProviderSSLSocketFactory());

	    responseCode = urlConnection.getResponseCode();
	    broadcastProgress(progress++);
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
	    try {
		if(urlConnection != null) {
		    responseCode = urlConnection.getResponseCode();
		    if(responseCode == 401) {
			broadcastProgress(progress++);
			LeapSRPSession.setToken("");
			Log.d(TAG, Integer.toString(responseCode));
			return true;
		    }
		}
	    } catch (IOException e1) {
		e1.printStackTrace();
	    }

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
     * @return true if certificate was downloaded correctly, false if provider.json or danger_on flag are not present in SharedPreferences, or if the certificate url could not be parsed as a URI, or if there was an SSL error. 
     */
    private boolean updateVpnCertificate() {
	try {
	    JSONObject provider_json = new JSONObject(preferences.getString(Provider.KEY, ""));
			
	    String provider_main_url = provider_json.getString(Provider.API_URL);
	    URL new_cert_string_url = new URL(provider_main_url + "/" + provider_json.getString(Provider.API_VERSION) + "/" + Constants.CERTIFICATE);

	    boolean danger_on = preferences.getBoolean(ProviderItem.DANGER_ON, false);


	    String cert_string = downloadWithProviderCA(new_cert_string_url.toString(), danger_on);

	    if(cert_string.isEmpty() || ConfigHelper.checkErroneousDownload(cert_string))
		return false;
	    else
		return loadCertificate(cert_string);
	} catch (JSONException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    return false;
	} catch (MalformedURLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    return false;
	} 
    }

    private boolean loadCertificate(String cert_string) {
	try {
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
	    RSAPrivateKey keyCert = ConfigHelper.parseRsaKeyFromString(keyString);
	    keyString = Base64.encodeToString( keyCert.getEncoded(), Base64.DEFAULT );
	    preferences.edit().putString(Constants.PRIVATE_KEY, "-----BEGIN RSA PRIVATE KEY-----\n"+keyString+"-----END RSA PRIVATE KEY-----").commit();

	    X509Certificate certCert = ConfigHelper.parseX509CertificateFromString(certificateString);
	    certificateString = Base64.encodeToString( certCert.getEncoded(), Base64.DEFAULT);

	    preferences.edit().putString(Constants.CERTIFICATE, "-----BEGIN CERTIFICATE-----\n"+certificateString+"-----END CERTIFICATE-----").commit();
						
	    return true;
	} catch (CertificateException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    return false;
	}
    }
}
