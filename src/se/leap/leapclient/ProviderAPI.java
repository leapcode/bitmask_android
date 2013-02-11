package se.leap.leapclient;

import java.io.IOException;
import java.util.Scanner;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class ProviderAPI extends IntentService {

	public ProviderAPI() {
		super("ProviderAPI");
		Log.v("ClassName", "Provider API");
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onHandleIntent(Intent task_for) {
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
			String provider_main_url = (String) task.get(ConfigHelper.provider_key_url);
			String provider_json_url = guessURL(provider_main_url);
			try {
				JSONObject provider_json = getJSONFromProvider(provider_json_url);
				String filename = provider_json_url.replaceFirst("http[s]?://", "").replaceFirst("\\/", "_") + "_provider.json".replaceFirst("__", "_");
				ConfigHelper.saveFile(filename, provider_json.toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
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
