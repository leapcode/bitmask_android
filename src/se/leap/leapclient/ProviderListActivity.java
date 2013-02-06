package se.leap.leapclient;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

import org.json.JSONException;
import org.json.JSONObject;

import se.leap.leapclient.ProviderListContent;
import se.leap.leapclient.ProviderListContent.ProviderItem;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;


/**
 * An activity representing a list of Providers. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link DashboardActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link ProviderListFragment} and the item details
 * (if present) is a {@link DashboardFragment}.
 * <p>
 * This activity also implements the required
 * {@link ProviderListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class ProviderListActivity extends FragmentActivity
        implements ProviderListFragment.Callbacks {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    
    static SharedPreferences shared_preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_list);

        shared_preferences = getPreferences(MODE_PRIVATE);
        
        loadPreseededProviders();

        // TODO: If exposing deep links into your app, handle intents here.
    }

    private void loadPreseededProviders() {
        AssetManager asset_manager = getAssets();
        String[] urls_filepaths = null;
		try {
			String url_files_folder = "urls";
			//TODO Put that folder in a better place (also inside the "for")
			urls_filepaths = asset_manager.list(url_files_folder); 
			String provider_name = "";
	        for(String url_filepath : urls_filepaths)
	        {
	        	provider_name = url_filepath.subSequence(0, url_filepath.indexOf(".")).toString();
	        	if(ProviderListContent.ITEMS.isEmpty()) //TODO I have to implement a way of checking if a provider new or is already present in that ITEMS list
	        		ProviderListContent.addItem(new ProviderItem(provider_name, asset_manager.open(url_files_folder + "/" + url_filepath)));
	        }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	/**
     * Callback method from {@link ProviderListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(String id) {
        if (mTwoPane) {
            // TODO Hmmm...is this how we should do this?  What if it /is/ two pane?
        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            
        	Iterator<ProviderItem> preseeded_providers_iterator = ProviderListContent.ITEMS.iterator();
        	while(preseeded_providers_iterator.hasNext())
        	{
        		ProviderItem current_provider_item = preseeded_providers_iterator.next();
        		if(current_provider_item.id.equalsIgnoreCase(id))
        		{
        			try {
        				processAssetsFiles(current_provider_item);
        				// TODO ask Provider class to save provider.json, setResult(OK), finish() to ConfigurationWizard
						downloadJSONFiles(current_provider_item);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
        		}
        	}
        	
            Intent dashboardIntent = new Intent(this, Dashboard.class);
            startActivity(dashboardIntent);
        }
    }

	private void processAssetsFiles(ProviderItem current_provider_item) {
		AssetManager assets_manager = getAssets();
		JSONObject provider_json = new JSONObject();
		try {
			String provider_contents = new Scanner(new InputStreamReader(assets_manager.open(current_provider_item.provider_json_assets))).useDelimiter("\\A").next();
			provider_json = new JSONObject(provider_contents);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			ConfigHelper.rescueJSONException(e);
		}
		ConfigHelper.saveSharedPref(ConfigHelper.provider_key, provider_json);
	}

	private void downloadJSONFiles(ProviderItem current_provider_item) throws IOException {
		Intent provider_API_command = new Intent(this, ProviderAPI.class);
		
		Bundle method_and_parameters = new Bundle();
		method_and_parameters.putString(ConfigHelper.cert_key, current_provider_item.cert_json_url);
		method_and_parameters.putString(ConfigHelper.eip_service_key, current_provider_item.eip_service_json_url);
		
		provider_API_command.putExtra(ConfigHelper.downloadJsonFilesBundleExtra, method_and_parameters);
		
		startService(provider_API_command);
		
	}
}
