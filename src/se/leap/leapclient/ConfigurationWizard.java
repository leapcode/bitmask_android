package se.leap.leapclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Scanner;

import org.json.JSONException;
import org.json.JSONObject;

import se.leap.leapclient.ProviderAPIResultReceiver.Receiver;
import se.leap.leapclient.ProviderListContent.ProviderItem;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;


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
public class ConfigurationWizard extends Activity
        implements ProviderListFragment.Callbacks, NewProviderDialog.NewProviderDialogInterface, Receiver {

	
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    public ProviderAPIResultReceiver providerAPI_result_receiver;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_configuration_wizard);
        
        ConfigHelper.setSharedPreferences(getSharedPreferences(ConfigHelper.PREFERENCES_KEY, MODE_PRIVATE));
        
        loadPreseededProviders();
        
        if(ConfigHelper.getKeystore() == null) {
        	InputStream keystore_input_stream = getResources().openRawResource(R.raw.leapkeystore);
        	ConfigHelper.getNewKeystore(keystore_input_stream);
        }
        
        // Only create our fragments if we're not restoring a saved instance
        if ( savedInstanceState == null ){
        	// TODO Some welcome screen?
        	// We will need better flow control when we have more Fragments (e.g. user auth)
        	ProviderListFragment providerList = new ProviderListFragment();
        	
        	FragmentManager fragmentManager = getFragmentManager();
        	fragmentManager.beginTransaction()
        		.add(R.id.configuration_wizard_layout, providerList, "providerlist")
        		.commit();
        }

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
	        	boolean custom = false;
	        	provider_name = url_filepath.subSequence(0, url_filepath.indexOf(".")).toString();
	        	if(ProviderListContent.ITEMS.isEmpty()) //TODO I have to implement a way of checking if a provider new or is already present in that ITEMS list
	        		ProviderListContent.addItem(new ProviderItem(provider_name, asset_manager.open(url_files_folder + "/" + url_filepath), custom));
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
        				saveProviderJson(current_provider_item);
        				downloadJSONFiles(current_provider_item);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
        		}
        	}
        }
    }

	private void saveProviderJson(ProviderItem current_provider_item) {
		AssetManager assets_manager = getAssets();
		JSONObject provider_json = new JSONObject();
		try {
			String provider_contents = "";
			if(!current_provider_item.custom)
				provider_contents = new Scanner(new InputStreamReader(assets_manager.open(current_provider_item.provider_json_filename))).useDelimiter("\\A").next();
			else
				provider_contents = new Scanner(ConfigHelper.openFileInputStream(current_provider_item.provider_json_filename)).useDelimiter("\\A").next();
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
		providerAPI_result_receiver = new ProviderAPIResultReceiver(new Handler());
		providerAPI_result_receiver.setReceiver(this);
		
		Intent provider_API_command = new Intent(this, ProviderAPI.class);
		
		Bundle method_and_parameters = new Bundle();
		
		method_and_parameters.putString(ConfigHelper.provider_key, current_provider_item.name);
		method_and_parameters.putString(ConfigHelper.cert_key, current_provider_item.cert_json_url);
		method_and_parameters.putString(ConfigHelper.eip_service_key, current_provider_item.eip_service_json_url);
		
		provider_API_command.putExtra(ConfigHelper.downloadJsonFilesBundleExtra, method_and_parameters);
		provider_API_command.putExtra("receiver", providerAPI_result_receiver);

		startService(provider_API_command);
	}
	
	public void addNewProvider(View view) {
		FragmentTransaction fragment_transaction = getFragmentManager().beginTransaction();
	    Fragment previous_new_provider_dialog = getFragmentManager().findFragmentByTag("newProviderDialog");
	    if (previous_new_provider_dialog != null) {
	        fragment_transaction.remove(previous_new_provider_dialog);
	    }
	    fragment_transaction.addToBackStack(null);

	    DialogFragment newFragment = NewProviderDialog.newInstance();
	    newFragment.show(fragment_transaction, "newProviderDialog");
	}

	@Override
	public void saveProvider(String provider_url) {
		providerAPI_result_receiver = new ProviderAPIResultReceiver(new Handler());
		providerAPI_result_receiver.setReceiver(this);
		
		Intent provider_API_command = new Intent(this, ProviderAPI.class);

		Bundle method_and_parameters = new Bundle();
		method_and_parameters.putString(ConfigHelper.provider_key_url, provider_url);

		provider_API_command.putExtra(ConfigHelper.downloadNewProviderDotJSON, method_and_parameters);
		provider_API_command.putExtra("receiver", providerAPI_result_receiver);
		
		startService(provider_API_command);
	}

	@Override
	public void onReceiveResult(int resultCode, Bundle resultData) {
		if(resultCode == ConfigHelper.CUSTOM_PROVIDER_ADDED){
			ProviderListFragment providerList = new ProviderListFragment();

			FragmentManager fragmentManager = getFragmentManager();
			fragmentManager.beginTransaction()
				.replace(R.id.configuration_wizard_layout, providerList, "providerlist")
				.commit();
		}
		else if(resultCode == ConfigHelper.CORRECTLY_DOWNLOADED_JSON_FILES) {
        	setResult(RESULT_OK);
        	finish();
		}
		else if(resultCode == ConfigHelper.INCORRECTLY_DOWNLOADED_JSON_FILES) {
        	setResult(RESULT_CANCELED);
        	finish();
		}
	}
}
