package se.leap.leapclient;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Scanner;

import org.bouncycastle.crypto.agreement.srp.SRP6Client;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.jcajce.provider.digest.Whirlpool.Digest;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
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
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onHandleIntent(Intent task_for) {
		final ResultReceiver receiver = task_for.getParcelableExtra("receiver");
		Bundle task;
		System.out.println("onHandleIntent called");
		if((task = task_for.getBundleExtra(ConfigHelper.downloadJsonFilesBundleExtra)) != null) {
			String cert_url = (String) task.get(ConfigHelper.cert_key);
			String eip_service_json_url = (String) task.get(ConfigHelper.eip_service_key);
			try {
				String cert_string = getStringFromProvider(cert_url);
				JSONObject cert_json = new JSONObject("{ \"certificate\" : \"" + cert_string + "\"}");
				ConfigHelper.saveSharedPref(ConfigHelper.cert_key, cert_json);
				JSONObject eip_service_json = getJSONFromProvider(eip_service_json_url);
				ConfigHelper.saveSharedPref(ConfigHelper.eip_service_key, eip_service_json);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				ConfigHelper.rescueJSONException(e);
			} catch(Exception e) {
				e.printStackTrace();
			}
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
		else if ((task = task_for.getBundleExtra(ConfigHelper.srpAuth)) != null) {
			String username = (String) task.get(ConfigHelper.username_key);
			String password = (String) task.get(ConfigHelper.password_key);
			SRP6Client srp_client = new SRP6Client();
			srp_client.init(new BigInteger(ConfigHelper.NG_1024, 16), ConfigHelper.g, new SHA256Digest(), new SecureRandom());
			// Receive salt from server
			String salt = getSaltFromSRPServer();
			BigInteger A = srp_client.generateClientCredentials(salt.getBytes(), username.getBytes(), password.getBytes());
			//Send A to the server. Doing a http response with cookies?
			//Receive server generated serverB
			//S = calculateSecret(BigInteger serverB)
			//K = H(S)
			//Now the two parties have a shared, strong session key K. To complete authentication, they need to prove to each other that their keys match.
		}
	}

	private String getSaltFromSRPServer() {
		// TODO Auto-generated method stub
		return null;
	}

	private String guessURL(String provider_main_url) {
		return provider_main_url + "/provider.json";
	}

	private String getStringFromProvider(String string_url) throws IOException {
		
		String json_file_content = "";
		
		DefaultHttpClient client = new LeapHttpClient(getApplicationContext());
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
