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
		if (!(task = task_for.getBundleExtra(ConfigHelper.downloadJsonFilesBundleExtra)).isEmpty()) {
			String provider_json_url = (String) task.get(ConfigHelper.provider_key);
			String eip_service_json_url = (String) task.get(ConfigHelper.eip_service_key);
			try {
				JSONObject provider_json = getFromProvider(provider_json_url);
				ConfigHelper.saveSharedPref(ConfigHelper.provider_key, provider_json);
				JSONObject eip_service_json = getFromProvider(eip_service_json_url);
				ConfigHelper.saveSharedPref(ConfigHelper.eip_service_key, eip_service_json);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				ConfigHelper.rescueJSONException(e);
			}
		}
	}

	private JSONObject getFromProvider(String json_url) throws IOException, JSONException {
		
		String json_file_content = "";
		
		DefaultHttpClient client = new LeapHttpClient(getApplicationContext());
		HttpGet get = new HttpGet(json_url);
		// Execute the GET call and obtain the response
		HttpResponse getResponse = client.execute(get);
		HttpEntity responseEntity = getResponse.getEntity();
		
		json_file_content = new Scanner(responseEntity.getContent()).useDelimiter("\\A").next();
		
		
		return new JSONObject(json_file_content);
	}

}
