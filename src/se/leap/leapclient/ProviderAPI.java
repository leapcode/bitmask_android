package se.leap.leapclient;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
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
		Bundle task ;
		System.out.println("onHandleIntent called");
		if(!(task = task_for.getBundleExtra("downloadJSONFiles")).isEmpty())
		{
			String provider_key = "provider";
			String eip_service_key = "eip";
			String provider_json_url = (String) task.get(provider_key);
			String eip_service_json_url = (String) task.get(eip_service_key);
			try {
				getAndParseSharedPref(provider_key, provider_json_url);
				getAndParseSharedPref(eip_service_key, eip_service_json_url);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void getAndParseSharedPref(String shared_preferences_key,
			String json_url) throws IOException {
		URL url = new URL(json_url);
		HttpURLConnection urlConnection = (HttpURLConnection) url
				.openConnection();
		try {
			InputStream in = new BufferedInputStream(
					urlConnection.getInputStream());
			String json_file_content = new Scanner(in).useDelimiter("\\A")
					.next();

			SharedPreferences.Editor shared_preferences_editor = ProviderListActivity.shared_preferences
					.edit();
			shared_preferences_editor.putString(shared_preferences_key,
					json_file_content);
			shared_preferences_editor.commit();
			System.out.println("Shared preferences updated: " + ProviderListActivity.shared_preferences.getString(shared_preferences_key, "Default"));
		} finally {
			urlConnection.disconnect();
		}

	}

}
