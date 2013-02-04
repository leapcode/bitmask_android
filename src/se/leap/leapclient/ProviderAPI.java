package se.leap.leapclient;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

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
		URL url = new URL(json_url);
		String json_file_content = "";
		URLConnection urlConnection = null;
		
		if (url.getProtocol().equalsIgnoreCase("https")) {
			urlConnection = (HttpsURLConnection) url.openConnection();
		} else if (url.getProtocol().equalsIgnoreCase("http")) {
			urlConnection = (HttpURLConnection) url.openConnection();
		}

		try {
			InputStream in = new BufferedInputStream(
					urlConnection.getInputStream());
			json_file_content = new Scanner(in).useDelimiter("\\A").next();
		} finally {
			((HttpURLConnection) urlConnection).disconnect();
		}
		
		return new JSONObject(json_file_content);
	}

}
