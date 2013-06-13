package se.leap.leapclient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.jboss.security.Util;
import org.jboss.security.srp.SRPParameters;
import org.json.JSONException;
import org.json.JSONObject;

import se.leap.leapclient.ProviderListContent.ProviderItem;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Base64;
import android.util.Log;

public class ProviderAPI extends IntentService {
	
	public ProviderAPI() {
		super("ProviderAPI");
		Log.v("ClassName", "Provider API");
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
			if(downloadNewProviderDotJSON(task)) {
				receiver.send(ConfigHelper.CUSTOM_PROVIDER_ADDED, Bundle.EMPTY);
			} else {
				receiver.send(ConfigHelper.INCORRECTLY_DOWNLOADED_JSON_FILES, Bundle.EMPTY);
			}
		}
		else if ((task = task_for.getBundleExtra(ConfigHelper.SRP_AUTH)) != null) {
			Bundle session_id_bundle = authenticateBySRP(task);
			if(session_id_bundle.getBoolean(ConfigHelper.RESULT_KEY)) {
				receiver.send(ConfigHelper.SRP_AUTHENTICATION_SUCCESSFUL, session_id_bundle);
			} else {
				receiver.send(ConfigHelper.SRP_AUTHENTICATION_FAILED, Bundle.EMPTY);
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

	private boolean downloadJsonFiles(Bundle task) {
		String cert_url = task.getString(ConfigHelper.MAIN_CERT_KEY);
		String eip_service_json_url = task.getString(ConfigHelper.EIP_SERVICE_KEY);
		boolean danger_on = task.getBoolean(ConfigHelper.DANGER_ON);
		try {
			String cert_string = getStringFromProvider(cert_url, danger_on);
			ConfigHelper.saveSharedPref(ConfigHelper.MAIN_CERT_KEY, cert_string);
			JSONObject eip_service_json = getJSONFromProvider(eip_service_json_url, danger_on);
			ConfigHelper.saveSharedPref(ConfigHelper.EIP_SERVICE_KEY, eip_service_json);
			return true;
		} catch (JSONException e) {
			return false;
		}
	}
	
	private Bundle authenticateBySRP(Bundle task) {
		Bundle session_id_bundle = new Bundle();
		
		String username = (String) task.get(ConfigHelper.USERNAME_KEY);
		String password = (String) task.get(ConfigHelper.PASSWORD_KEY);
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
				if( client.verify((byte[])session_idAndM2.get("M2")) == false ) {
					session_id_bundle.putBoolean(ConfigHelper.RESULT_KEY, false);
				} else {
					session_id_bundle.putBoolean(ConfigHelper.RESULT_KEY, true);
					session_id_bundle.putString(ConfigHelper.SESSION_ID_KEY, session_idAndM2.getString(ConfigHelper.SESSION_ID_KEY));
					session_id_bundle.putString(ConfigHelper.SESSION_ID_COOKIE_KEY, session_idAndM2.getString(ConfigHelper.SESSION_ID_COOKIE_KEY));
				}
			} else {
				session_id_bundle.putBoolean(ConfigHelper.RESULT_KEY, false);
			}
		} catch (ClientProtocolException e) {
			session_id_bundle.putBoolean(ConfigHelper.RESULT_KEY, false);
		} catch (IOException e) {
			session_id_bundle.putBoolean(ConfigHelper.RESULT_KEY, false);
		} catch (JSONException e) {
			session_id_bundle.putBoolean(ConfigHelper.RESULT_KEY, false);
		} catch (NoSuchAlgorithmException e) {
			session_id_bundle.putBoolean(ConfigHelper.RESULT_KEY, false);
		}

		return session_id_bundle;
	}

	private JSONObject sendAToSRPServer(String server_url, String username, String clientA) throws ClientProtocolException, IOException, JSONException {
		HttpPost post = new HttpPost(server_url + "/sessions.json" + "?" + "login=" + username + "&&" + "A=" + clientA);
		return sendToServer(post);
	}

	private JSONObject sendM1ToSRPServer(String server_url, String username, byte[] m1) throws ClientProtocolException, IOException, JSONException {
		HttpPut put = new HttpPut(server_url + "/sessions/" + username +".json" + "?" + "client_auth" + "=" + new BigInteger(1, Util.trim(m1)).toString(16));
		JSONObject json_response = sendToServer(put);

		JSONObject session_idAndM2 = new JSONObject();
		if(json_response.length() > 0) {
			byte[] M2_not_trimmed = new BigInteger(json_response.getString("M2"), 16).toByteArray();
			Cookie session_id_cookie = LeapHttpClient.getInstance(getApplicationContext()).getCookieStore().getCookies().get(0);
			session_idAndM2.put(ConfigHelper.SESSION_ID_COOKIE_KEY, session_id_cookie.getName());
			session_idAndM2.put(ConfigHelper.SESSION_ID_KEY, session_id_cookie.getValue());
			session_idAndM2.put("M2", Util.trim(M2_not_trimmed));
		}
		return session_idAndM2;
	}
	
	private JSONObject sendToServer(HttpUriRequest request) throws ClientProtocolException, IOException, JSONException {
		DefaultHttpClient client = LeapHttpClient.getInstance(getApplicationContext());
		HttpContext localContext = new BasicHttpContext();
		localContext.setAttribute(ClientContext.COOKIE_STORE, client.getCookieStore());
		
		HttpResponse getResponse = client.execute(request, localContext);
		HttpEntity responseEntity = getResponse.getEntity();
		String plain_response = new Scanner(responseEntity.getContent()).useDelimiter("\\A").next();
		JSONObject json_response = new JSONObject(plain_response);
		if(!json_response.isNull("errors") || json_response.has("errors")) {
			return new JSONObject();
		}

		return json_response;
	}

	private Bundle updateProviderDotJSON(Bundle task) {
		Bundle result = new Bundle();
		boolean custom = task.getBoolean(ConfigHelper.CUSTOM);
		boolean danger_on = task.getBoolean(ConfigHelper.DANGER_ON);
		String provider_json_url = task.getString(ConfigHelper.PROVIDER_JSON_URL);
		String provider_name = task.getString(ConfigHelper.PROVIDER_NAME);
		
		try {
			JSONObject provider_json = getJSONFromProvider(provider_json_url, danger_on);
			if(provider_json == null) {
				result.putBoolean(ConfigHelper.RESULT_KEY, false);
			} else {    			
				ConfigHelper.saveSharedPref(ConfigHelper.ALLOWED_ANON, provider_json.getJSONObject(ConfigHelper.SERVICE_KEY).getBoolean(ConfigHelper.ALLOWED_ANON));

				ProviderListContent.addItem(new ProviderItem(provider_name, provider_json_url, provider_json, custom, danger_on));
				result.putBoolean(ConfigHelper.RESULT_KEY, true);
				result.putString(ConfigHelper.PROVIDER_KEY, provider_json.toString());
				result.putBoolean(ConfigHelper.DANGER_ON, danger_on);
			}
		} catch (JSONException e) {
			result.putBoolean(ConfigHelper.RESULT_KEY, false);
		}
		
		return result;
	}

	private boolean downloadNewProviderDotJSON(Bundle task) {
		boolean custom = true;
		boolean danger_on = task.getBoolean(ConfigHelper.DANGER_ON);
		
		String provider_main_url = (String) task.get(ConfigHelper.PROVIDER_MAIN_URL);
		String provider_name = provider_main_url.replaceFirst("http[s]?://", "").replaceFirst("\\/", "_");
		String provider_json_url = guessURL(provider_main_url);
		
		JSONObject provider_json;
		try {
			provider_json = getJSONFromProvider(provider_json_url, danger_on);
			ProviderListContent.addItem(new ProviderItem(provider_name, provider_json_url, provider_json, custom, danger_on));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return true;
	}

	private String getStringFromProviderWithoutValidate(
			URL provider_json_url) {
		
		String json_string = "";
		HostnameVerifier hostnameVerifier = new HostnameVerifier() {
			@Override
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};

		// Tell the URLConnection to use our HostnameVerifier
		try {
			HttpsURLConnection urlConnection =
					(HttpsURLConnection)provider_json_url.openConnection();
			urlConnection.setHostnameVerifier(hostnameVerifier);
			json_string = new Scanner(urlConnection.getInputStream()).useDelimiter("\\A").next();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			json_string = getStringFromProviderWithCACertAdded(provider_json_url);
			//e.printStackTrace();
		}
		return json_string;
	}
	
	private String getStringFromProvider(String string_url, boolean danger_on) {
		
		String json_file_content = "";
		
		URL provider_url = null;
		int seconds_of_timeout = 1;
		try {
			provider_url = new URL(string_url);
			URLConnection url_connection = provider_url.openConnection();
			url_connection.setConnectTimeout(seconds_of_timeout*1000);
			json_file_content = new Scanner(url_connection.getInputStream()).useDelimiter("\\A").next();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(SocketTimeoutException e) {
			return "";
		} catch (IOException e) {
			// TODO SSLHandshakeException
			// This means that we have not added ca.crt to the trusted certificates.
			if(provider_url != null && danger_on) {
				json_file_content = getStringFromProviderWithoutValidate(provider_url);
			}
			//json_file_content = downloadStringFromProviderWithCACertAdded(string_url);
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return json_file_content;
	}

	private String getStringFromProviderWithCACertAdded(URL url) {
		String json_file_content = "";
		
		// Load CAs from an InputStream
		// (could be from a resource or ByteArrayInputStream or ...)
		CertificateFactory cf;
		try {
			cf = CertificateFactory.getInstance("X.509");

			String cert_string = ConfigHelper.getStringFromSharedPref(ConfigHelper.MAIN_CERT_KEY);
			cert_string = cert_string.replaceFirst("-----BEGIN CERTIFICATE-----", "").replaceFirst("-----END CERTIFICATE-----", "").trim();
			byte[] cert_bytes = Base64.decode(cert_string, Base64.DEFAULT);
			InputStream caInput =  new ByteArrayInputStream(cert_bytes);
			java.security.cert.Certificate ca;
			try {
			    ca = cf.generateCertificate(caInput);
			    System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
			} finally {
			    caInput.close();
			}

			// Create a KeyStore containing our trusted CAs
			String keyStoreType = KeyStore.getDefaultType();
			KeyStore keyStore = KeyStore.getInstance(keyStoreType);
			keyStore.load(null, null);
			keyStore.setCertificateEntry("ca", ca);

			// Create a TrustManager that trusts the CAs in our KeyStore
			String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
			tmf.init(keyStore);

			// Create an SSLContext that uses our TrustManager
			SSLContext context = SSLContext.getInstance("TLS");
			context.init(null, tmf.getTrustManagers(), null);

			// Tell the URLConnection to use a SocketFactory from our SSLContext
			HttpsURLConnection urlConnection =
			    (HttpsURLConnection)url.openConnection();
			urlConnection.setSSLSocketFactory(context.getSocketFactory());
			json_file_content = new Scanner(urlConnection.getInputStream()).useDelimiter("\\A").next();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
	
	private JSONObject getJSONFromProvider(String json_url, boolean danger_on) throws JSONException {
		String json_file_content = getStringFromProvider(json_url, danger_on);
		return new JSONObject(json_file_content);
	}

	private String guessURL(String provider_main_url) {
		return provider_main_url + "/provider.json";
	}
	
	private boolean logOut(Bundle task) {
		DefaultHttpClient client = LeapHttpClient.getInstance(getApplicationContext());
		int session_id_index = 0;
		//String delete_url = task.getString(ConfigHelper.srp_server_url_key) + "/sessions/" + client.getCookieStore().getCookies().get(0).getValue();
		try {
			String delete_url = task.getString(ConfigHelper.API_URL_KEY) + "/logout" + "?authenticity_token=" + client.getCookieStore().getCookies().get(session_id_index).getValue();
			HttpDelete delete = new HttpDelete(delete_url);
			HttpResponse getResponse = client.execute(delete);
			HttpEntity responseEntity = getResponse.getEntity();
			responseEntity.consumeContent();
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
		}
		return true;
	}

	private boolean getNewCert(Bundle task) {
		String type_of_certificate = task.getString(ConfigHelper.TYPE_OF_CERTIFICATE);
		try {
			JSONObject provider_json = ConfigHelper.getJsonFromSharedPref(ConfigHelper.PROVIDER_KEY);
			URL provider_main_url = new URL(provider_json.getString(ConfigHelper.API_URL_KEY));
			String new_cert_string_url = provider_main_url.toString() + "/" + provider_json.getString(ConfigHelper.API_VERSION_KEY) + "/" + ConfigHelper.CERT_KEY;

			if(type_of_certificate.equalsIgnoreCase(ConfigHelper.AUTHED_CERTIFICATE)) {
				HttpCookie session_id_cookie = new HttpCookie(task.getString(ConfigHelper.SESSION_ID_COOKIE_KEY), task.getString(ConfigHelper.SESSION_ID_KEY));

				CookieManager cookieManager = new CookieManager();
				cookieManager.getCookieStore().add(provider_main_url.toURI(), session_id_cookie);
				CookieHandler.setDefault(cookieManager);
			}
			
			boolean danger_on = ConfigHelper.getBoolFromSharedPref(ConfigHelper.DANGER_ON);
			String cert_string = getStringFromProvider(new_cert_string_url, danger_on);
			if(!cert_string.isEmpty()) { 
				ConfigHelper.saveSharedPref(ConfigHelper.CERT_KEY, cert_string);
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
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
}
