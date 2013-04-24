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
import java.util.List;
import java.net.MalformedURLException;
import java.net.URL;
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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
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
		if((task = task_for.getBundleExtra(ConfigHelper.downloadJsonFilesBundleExtra)) != null) {
			if(!downloadJsonFiles(task))
				receiver.send(ConfigHelper.INCORRECTLY_DOWNLOADED_JSON_FILES, Bundle.EMPTY);
			else
				receiver.send(ConfigHelper.CORRECTLY_DOWNLOADED_JSON_FILES, Bundle.EMPTY);
		}
		else if ((task = task_for.getBundleExtra(ConfigHelper.downloadNewProviderDotJSON)) != null) {
			if(downloadNewProviderDotJSON(task))
				receiver.send(ConfigHelper.CUSTOM_PROVIDER_ADDED, Bundle.EMPTY);
			else
				receiver.send(ConfigHelper.INCORRECTLY_DOWNLOADED_JSON_FILES, Bundle.EMPTY);
		}
		else if ((task = task_for.getBundleExtra(ConfigHelper.srpAuth)) != null) {
			if(authenticateBySRP(task))
				receiver.send(ConfigHelper.SRP_AUTHENTICATION_SUCCESSFUL, Bundle.EMPTY);
			else
				receiver.send(ConfigHelper.SRP_AUTHENTICATION_FAILED, Bundle.EMPTY);
		}
		else if ((task = task_for.getBundleExtra(ConfigHelper.logOut)) != null) {
			if(logOut(task))
				receiver.send(ConfigHelper.LOGOUT_SUCCESSFUL, Bundle.EMPTY);
			else
				receiver.send(ConfigHelper.LOGOUT_FAILED, Bundle.EMPTY);
		}
	}

	private boolean downloadJsonFiles(Bundle task) {
		String provider_name = task.getString(ConfigHelper.provider_key);
		String cert_url = task.getString(ConfigHelper.cert_key);
		String eip_service_json_url = task.getString(ConfigHelper.eip_service_key);
		boolean danger_on = task.getBoolean(ConfigHelper.danger_on);
		try {
			String cert_string = getStringFromProvider(cert_url, danger_on);
			ConfigHelper.addTrustedCertificate(provider_name, cert_string);
			JSONObject cert_json = new JSONObject("{ \"certificate\" : \"" + cert_string + "\"}");
			ConfigHelper.saveSharedPref(ConfigHelper.cert_key, cert_json);
			JSONObject eip_service_json = getJSONFromProvider(eip_service_json_url, danger_on);
			ConfigHelper.saveSharedPref(ConfigHelper.eip_service_key, eip_service_json);
			return true;
		} catch (JSONException e) {
			ConfigHelper.rescueJSONException(e);
			return false;
		}
	}

	private boolean registerWithSRP(Bundle task) {
		String username = (String) task.get(ConfigHelper.username_key);
		String password = (String) task.get(ConfigHelper.password_key);
		String authentication_server = (String) task.get(ConfigHelper.api_url_key);
		
		BigInteger ng_1024 = new BigInteger(ConfigHelper.NG_1024, 16);
		BigInteger salt = ng_1024.probablePrime(1024, null);
		byte[] salt_in_bytes = salt.toByteArray();
		
		return false;
	}
	
	private boolean authenticateBySRP(Bundle task) {
		String username = (String) task.get(ConfigHelper.username_key);
		String password = (String) task.get(ConfigHelper.password_key);
		String authentication_server = (String) task.get(ConfigHelper.api_url_key);
		
		String salt = "abcd";

		SRPParameters params = new SRPParameters(new BigInteger(ConfigHelper.NG_1024, 16).toByteArray(), new BigInteger("2").toByteArray(), new BigInteger(salt, 16).toByteArray(), "SHA-256");
		//SRPClientSession client = new SRPClientSession(username, password.toCharArray(), params);
		LeapSRPSession client = new LeapSRPSession(username, password, params);
		byte[] A = client.exponential();
		try {
			JSONObject saltAndB = sendAToSRPServer(authentication_server, username, new BigInteger(1, A).toString(16));
			if(saltAndB.length() > 0) {
				byte[] B = saltAndB.getString("B").getBytes();
				salt = saltAndB.getString("salt");
				params = new SRPParameters(new BigInteger(ConfigHelper.NG_1024, 16).toByteArray(), new BigInteger("2").toByteArray(), new BigInteger(salt, 16).toByteArray(), "SHA-256");
				client = new LeapSRPSession(username, password, params);
				A = client.exponential();
				saltAndB = sendAToSRPServer(authentication_server, username, new BigInteger(1, A).toString(16));
				byte[] Bbytes = new BigInteger(saltAndB.getString("B"), 16).toByteArray();
				byte[] M1 = client.response(Bbytes);
				byte[] M2 = sendM1ToSRPServer(authentication_server, username, M1);
				if( client.verify(M2) == false )
					//throw new SecurityException("Failed to validate server reply: M2 = " + new BigInteger(1, M2).toString(16));
					return false;
				return true;
			}
			else return false;
		} catch (ClientProtocolException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return false;
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return false;
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return false;
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	private JSONObject sendAToSRPServer(String server_url, String username, String clientA) throws ClientProtocolException, IOException, JSONException {
		DefaultHttpClient client = LeapHttpClient.getInstance(getApplicationContext());
		String parameter_chain = "A" + "=" + clientA + "&" + "login" + "=" + username;
		HttpPost post = new HttpPost(server_url + "/sessions.json" + "?" + parameter_chain);
	
		HttpResponse getResponse = client.execute(post);
		HttpEntity responseEntity = getResponse.getEntity();
		String plain_response = new Scanner(responseEntity.getContent()).useDelimiter("\\A").next();
		JSONObject json_response = new JSONObject(plain_response);
		if(!json_response.isNull("errors") || json_response.has("errors")) {
			return new JSONObject();
		}
		List<Cookie> cookies = client.getCookieStore().getCookies();
		if(!cookies.isEmpty()) {
			String session_id = cookies.get(0).getValue();
		}
		return json_response;
	}

	private byte[] sendM1ToSRPServer(String server_url, String username, byte[] m1) throws ClientProtocolException, IOException, JSONException {
		DefaultHttpClient client = LeapHttpClient.getInstance(getApplicationContext());
		String parameter_chain = "client_auth" + "=" + new BigInteger(1, Util.trim(m1)).toString(16);
		HttpPut put = new HttpPut(server_url + "/sessions/" + username +".json" + "?" + parameter_chain);
		HttpContext localContext = new BasicHttpContext();
		localContext.setAttribute(ClientContext.COOKIE_STORE, client.getCookieStore());
		
		HttpResponse getResponse = client.execute(put, localContext);
		HttpEntity responseEntity = getResponse.getEntity();
		String plain_response = new Scanner(responseEntity.getContent()).useDelimiter("\\A").next();
		JSONObject json_response = new JSONObject(plain_response);
		if(!json_response.isNull("errors") || json_response.has("errors")) {
			return new byte[0];
		}
		
		byte[] M2_not_trimmed = new BigInteger(json_response.getString("M2"), 16).toByteArray();
		return Util.trim(M2_not_trimmed);
		//return M2_not_trimmed;
	}

	private boolean downloadNewProviderDotJSON(Bundle task) {
		boolean custom = true;
		boolean danger_on = task.getBoolean(ConfigHelper.danger_on);
		
		String provider_main_url = (String) task.get(ConfigHelper.provider_main_url);
		String provider_name = provider_main_url.replaceFirst("http[s]?://", "").replaceFirst("\\/", "_");
		String provider_json_url = guessURL(provider_main_url);
		JSONObject provider_json = null;
		try {
			provider_json = getJSONFromProvider(provider_json_url, danger_on);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		if(provider_json == null) {
			return false;
		} else {
			String filename = provider_name + "_provider.json".replaceFirst("__", "_");
			ConfigHelper.saveFile(filename, provider_json.toString());
			ConfigHelper.saveSharedPref(ConfigHelper.provider_key, provider_json);

			ProviderListContent.addItem(new ProviderItem(provider_name, provider_json_url, ConfigHelper.openFileInputStream(filename), custom, danger_on));
			return true;
		}
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

	private String guessURL(String provider_main_url) {
		return provider_main_url + "/provider.json";
	}
	
	private String getStringFromProvider(String string_url, boolean danger_on) {
		
		String json_file_content = "";
		
		URL provider_url = null;
		try {
			provider_url = new URL(string_url);
			json_file_content = new Scanner(provider_url.openStream()).useDelimiter("\\A").next();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO SSLHandshakeException
			// This means that we have not added ca.crt to the trusted certificates.
			if(provider_url != null && danger_on) {
				json_file_content = getStringFromProviderWithoutValidate(provider_url);
			}
			//json_file_content = downloadStringFromProviderWithCACertAdded(string_url);
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

			String cert_string = ConfigHelper.getStringFromSharedPref(ConfigHelper.cert_key);
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

	private String getStringFromProvider_2(String string_url) throws IOException {
		
		String json_file_content = "";
		
		DefaultHttpClient client = LeapHttpClient.getInstance(getApplicationContext());
		HttpGet get = new HttpGet(string_url);
		// Execute the GET call and obtain the response
		HttpResponse getResponse = client.execute(get);
		HttpEntity responseEntity = getResponse.getEntity();
		
		json_file_content = new Scanner(responseEntity.getContent()).useDelimiter("\\A").next();
		
		return json_file_content;
	}
	
	private JSONObject getJSONFromProvider(String json_url, boolean danger_on) throws JSONException {
		String json_file_content = getStringFromProvider(json_url, danger_on);
		return new JSONObject(json_file_content);
	}
	
	private boolean logOut(Bundle task) {
		DefaultHttpClient client = LeapHttpClient.getInstance(getApplicationContext());
		int session_id_index = 0;
		//String delete_url = task.getString(ConfigHelper.srp_server_url_key) + "/sessions/" + client.getCookieStore().getCookies().get(0).getValue();
		String delete_url = task.getString(ConfigHelper.api_url_key) + "/logout" + "?authenticity_token=" + client.getCookieStore().getCookies().get(session_id_index).getValue();
		HttpDelete delete = new HttpDelete(delete_url);
		try {
			HttpResponse getResponse = client.execute(delete);
			HttpEntity responseEntity = getResponse.getEntity();
			responseEntity.consumeContent();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
}
