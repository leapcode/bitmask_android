package se.leap.leapclient;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Provider;
import java.security.Security;
import java.util.Scanner;

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
		if((task = task_for.getBundleExtra(ConfigHelper.downloadJsonFilesBundleExtra)) != null) {
			if(downloadJsonFilesBundleExtra(task))
				receiver.send(ConfigHelper.CORRECTLY_DOWNLOADED_JSON_FILES, Bundle.EMPTY);
			else
				receiver.send(ConfigHelper.INCORRECTLY_DOWNLOADED_JSON_FILES, Bundle.EMPTY);
		}
		else if ((task = task_for.getBundleExtra(ConfigHelper.downloadNewProviderDotJSON)) != null) {
			if(downloadNewProviderDotJSON(task))
				receiver.send(ConfigHelper.CUSTOM_PROVIDER_ADDED, Bundle.EMPTY);
			else
				receiver.send(ConfigHelper.INCORRECTLY_DOWNLOADED_JSON_FILES, Bundle.EMPTY);
		}
	}

	private boolean downloadNewProviderDotJSON(Bundle task) {
		boolean custom = true;
		String provider_main_url = (String) task.get(ConfigHelper.provider_key_url);
		String provider_name = provider_main_url.replaceFirst("http[s]?://", "").replaceFirst("\\/", "_");
		String provider_json_url = guessURL(provider_main_url);
		JSONObject provider_json = null;
		try {
			provider_json = getJSONFromProvider(provider_json_url);
		} catch (IOException e) {
			// It could happen that an https site used a certificate not trusted.
			provider_json = downloadNewProviderDotJsonWithoutCert(provider_json_url);
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

			ProviderListContent.addItem(new ProviderItem(provider_name, ConfigHelper.openFileInputStream(filename), custom));
			return true;
		}
	}

	private boolean downloadJsonFilesBundleExtra(Bundle task) {
		String provider_name = (String) task.get(ConfigHelper.provider_key);
		String cert_url = (String) task.get(ConfigHelper.cert_key);
		String eip_service_json_url = (String) task.get(ConfigHelper.eip_service_key);
		try {
			//JSONObject provider_json = new JSONObject("{ \"provider\" : \"" + provider_name + "\"}");
			//ConfigHelper.saveSharedPref(ConfigHelper.provider_key, provider_json);
			
			/*String cert_string = getStringFromProvider(cert_url);
			JSONObject cert_json = new JSONObject("{ \"certificate\" : \"" + cert_string + "\"}");
			ConfigHelper.saveSharedPref(ConfigHelper.cert_key, cert_json);
			ConfigHelper.addTrustedCertificate(provider_name, cert_string);*/
			URL cacert = new URL(cert_url);
			ConfigHelper.addTrustedCertificate(provider_name, cacert.openStream());
			JSONObject eip_service_json = getJSONFromProvider(eip_service_json_url);
			ConfigHelper.saveSharedPref(ConfigHelper.eip_service_key, eip_service_json);
			return true;
		} catch (IOException e) {
			// It could happen that an https site used a certificate not trusted: solved above using URL
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

	private JSONObject downloadNewProviderDotJsonWithoutCert(
			String provider_json_url) {
		JSONObject provider_json = null;
		try {
			URL provider_url = new URL(provider_json_url);
			String provider_json_string = new Scanner(provider_url.openStream()).useDelimiter("\\A").next();
			provider_json = new JSONObject(provider_json_string);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (JSONException e1) {
			e1.printStackTrace();
		}
		return provider_json;
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
