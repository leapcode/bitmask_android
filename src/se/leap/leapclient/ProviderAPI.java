package se.leap.leapclient;

import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Scanner;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.jboss.security.srp.SRPClientSession;
import org.jboss.security.srp.SRPParameters;
import org.json.JSONException;
import org.json.JSONObject;

import se.leap.leapclient.ProviderListContent.ProviderItem;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
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
		System.out.println("onHandleIntent called");
		if((task = task_for.getBundleExtra(ConfigHelper.downloadJsonFilesBundleExtra)) != null) {
			if(!downloadJsonFiles(task))
				receiver.send(ConfigHelper.INCORRECTLY_DOWNLOADED_JSON_FILES, Bundle.EMPTY);
			else
				receiver.send(ConfigHelper.CORRECTLY_DOWNLOADED_JSON_FILES, Bundle.EMPTY);
		}
		else if ((task = task_for.getBundleExtra(ConfigHelper.downloadNewProviderDotJSON)) != null) {
			boolean custom = true;
			String provider_main_url = (String) task.get(ConfigHelper.provider_key_url);
			String provider_name = provider_main_url.replaceFirst("http[s]?://", "").replaceFirst("\\/", "_");
			String provider_json_url = guessURL(provider_main_url);
			try {
				JSONObject provider_json = getJSONFromProvider(provider_json_url);
				String filename = provider_name + "_provider.json".replaceFirst("__", "_");
				ConfigHelper.saveFile(filename, provider_json.toString());
        		ProviderListContent.addItem(new ProviderItem(provider_name, ConfigHelper.openFileInputStream(filename), custom));
        		receiver.send(ConfigHelper.CUSTOM_PROVIDER_ADDED, Bundle.EMPTY);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if ((task = task_for.getBundleExtra(ConfigHelper.srpRegister)) != null) {
			if(!registerWithSRP(task))
				receiver.send(ConfigHelper.SRP_REGISTRATION_FAILED, Bundle.EMPTY);
			else
				receiver.send(ConfigHelper.SRP_REGISTRATION_SUCCESSFUL, Bundle.EMPTY);
		}
		else if ((task = task_for.getBundleExtra(ConfigHelper.srpAuth)) != null) {
			if(!authenticateBySRP(task))
				receiver.send(ConfigHelper.SRP_AUTHENTICATION_FAILED, Bundle.EMPTY);
			else
				receiver.send(ConfigHelper.SRP_AUTHENTICATION_SUCCESSFUL, Bundle.EMPTY);
		}
	}

	private boolean downloadJsonFiles(Bundle task) {
		String cert_url = (String) task.get(ConfigHelper.cert_key);
		String eip_service_json_url = (String) task.get(ConfigHelper.eip_service_key);
		try {
			String cert_string = getStringFromProvider(cert_url);
			JSONObject cert_json = new JSONObject("{ \"certificate\" : \"" + cert_string + "\"}");
			ConfigHelper.saveSharedPref(ConfigHelper.cert_key, cert_json);
			JSONObject eip_service_json = getJSONFromProvider(eip_service_json_url);
			ConfigHelper.saveSharedPref(ConfigHelper.eip_service_key, eip_service_json);
			return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (JSONException e) {
			ConfigHelper.rescueJSONException(e);
			return false;
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private boolean registerWithSRP(Bundle task) {
		String username = (String) task.get(ConfigHelper.username_key);
		String password = (String) task.get(ConfigHelper.password_key);
		String authentication_server = (String) task.get(ConfigHelper.srp_server_url_key);
		
		BigInteger ng_1024 = new BigInteger(ConfigHelper.NG_1024, 16);
		BigInteger salt = ng_1024.probablePrime(1024, null);
		byte[] salt_in_bytes = salt.toByteArray();
		
		return false;
	}
	
	private boolean authenticateBySRP(Bundle task) {
		String username = (String) task.get(ConfigHelper.username_key);
		String password = (String) task.get(ConfigHelper.password_key);
		String authentication_server = (String) task.get(ConfigHelper.srp_server_url_key);
		
		String salt = "abcd";

		SRPParameters params = new SRPParameters(new BigInteger(ConfigHelper.NG_1024, 16).toByteArray(), new BigInteger("2").toByteArray(), new BigInteger(salt, 16).toByteArray(), "SHA-256");
		//SRPClientSession client = new SRPClientSession(username, password.toCharArray(), params);
		LeapSRPSession client = new LeapSRPSession(username, password.toCharArray(), params);
		byte[] A = client.exponential();
		try {
			JSONObject saltAndB = sendAToSRPServer(authentication_server, username, new BigInteger(A).toString(16));
			if(saltAndB.length() > 0) {
				byte[] B = saltAndB.getString("B").getBytes();
				salt = saltAndB.getString("salt");
				params = new SRPParameters(new BigInteger(ConfigHelper.NG_1024, 16).toByteArray(), new BigInteger("2").toByteArray(), new BigInteger(salt, 16).toByteArray(), "SHA-256");
				//client = new SRPClientSession(username, password.toCharArray(), params);
				client = new LeapSRPSession(username, password.toCharArray(), params);
				A = client.exponential();
				saltAndB = sendAToSRPServer(authentication_server, username, new BigInteger(A).toString(16));
				String Bhex = saltAndB.getString("B");
				byte[] M1 = client.response(new BigInteger(Bhex, 16).toByteArray());
				byte[] M2 = sendM1ToSRPServer(authentication_server, username, M1);
				if( client.verify(M2) == false )
					throw new SecurityException("Failed to validate server reply");
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
		String parameter_chain = "client_auth" + "=" + new BigInteger(m1).toString(16);
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
		
		return json_response.getString("M2").getBytes();
	}

	private String guessURL(String provider_main_url) {
		return provider_main_url + "/provider.json";
	}

	private String getStringFromProvider(String string_url) throws IOException {
		
		String json_file_content = "";
		
		DefaultHttpClient client = LeapHttpClient.getInstance(getApplicationContext());
		HttpGet get = new HttpGet(string_url);
		// Execute the GET call and obtain the response
		HttpResponse getResponse = client.execute(get);
		HttpEntity responseEntity = getResponse.getEntity();
		
		json_file_content = new Scanner(responseEntity.getContent()).useDelimiter("\\A").next();
		
		return json_file_content;
	}
	private JSONObject getJSONFromProvider(String json_url) throws IOException, JSONException {
		String json_file_content = getStringFromProvider(json_url);
		return new JSONObject(json_file_content);
	}
}
