package se.leap.leapclient;

import java.io.IOException;
import java.util.Iterator;
import java.util.Scanner;

import org.json.JSONException;
import org.json.JSONObject;

import se.leap.leapclient.ProviderAPIResultReceiver.Receiver;
import se.leap.leapclient.ProviderListContent.ProviderItem;
import se.leap.leapclient.R;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

public class ConfigurationWizard extends Activity
implements ProviderListFragment.Callbacks, NewProviderDialog.NewProviderDialogInterface, Receiver {

	private ProviderItem mSelectedProvider;
	private ProgressDialog mProgressDialog;
	private Intent mConfigState = new Intent();

	protected static final String PROVIDER_SET = "PROVIDER SET";
	protected static final String SERVICES_RETRIEVED = "SERVICES RETRIEVED";
    
    public ProviderAPIResultReceiver providerAPI_result_receiver;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_configuration_wizard);
        
        providerAPI_result_receiver = new ProviderAPIResultReceiver(new Handler());
        providerAPI_result_receiver.setReceiver(this);
        
        ConfigHelper.setSharedPreferences(getSharedPreferences(ConfigHelper.PREFERENCES_KEY, MODE_PRIVATE));
        
        loadPreseededProviders();
        
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
			if (ConfigHelper.getBoolFromSharedPref(ConfigHelper.ALLOWED_ANON)){
				mProgressDialog.setMessage(getResources().getString(R.string.config_downloading_certificates));
				mConfigState.putExtra(SERVICES_RETRIEVED, true);
				downloadAnonCert();
			} else {
				mProgressDialog.dismiss();
				Toast.makeText(getApplicationContext(), R.string.success, Toast.LENGTH_LONG).show();
				setResult(RESULT_OK);
				finish();
			}
		}
		else if(resultCode == ConfigHelper.INCORRECTLY_DOWNLOADED_JSON_FILES) {
        	Toast.makeText(getApplicationContext(), "You have not entered a LEAP provider URL or it is unavailable", Toast.LENGTH_LONG).show();
        	setResult(RESULT_CANCELED, mConfigState);
        	finish();
		}
		else if(resultCode == ConfigHelper.CORRECTLY_UPDATED_PROVIDER_DOT_JSON) {
			JSONObject provider_json;
			try {
				provider_json = new JSONObject(resultData.getString(ConfigHelper.PROVIDER_KEY));
				boolean danger_on = resultData.getBoolean(ConfigHelper.DANGER_ON);
				ConfigHelper.saveSharedPref(ConfigHelper.PROVIDER_KEY, provider_json);
				ConfigHelper.saveSharedPref(ConfigHelper.DANGER_ON, danger_on);
				ConfigHelper.saveSharedPref(ConfigHelper.ALLOWED_ANON, provider_json.getBoolean(ConfigHelper.ALLOWED_ANON));
				
				mConfigState.setAction(PROVIDER_SET);
				
				mProgressDialog.setMessage(getResources().getString(R.string.config_downloading_services));
				downloadJSONFiles(mSelectedProvider);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

				mProgressDialog.dismiss();
				Toast.makeText(this, getResources().getString(R.string.config_error_parsing), Toast.LENGTH_LONG);
				setResult(RESULT_CANCELED, mConfigState);
				finish();
			}
		}
		else if(resultCode == ConfigHelper.INCORRECTLY_UPDATED_PROVIDER_DOT_JSON) {
			mProgressDialog.dismiss();
			Toast.makeText(getApplicationContext(), "Install a new version of this app.", Toast.LENGTH_LONG).show();
			setResult(RESULT_CANCELED, mConfigState);
			finish();
		}
		else if(resultCode == ConfigHelper.CORRECTLY_DOWNLOADED_CERTIFICATE) {
			mProgressDialog.dismiss();
			Toast.makeText(getApplicationContext(), "Your anon cert has been correctly downloaded", Toast.LENGTH_LONG).show();
			Toast.makeText(getApplicationContext(), R.string.success, Toast.LENGTH_LONG).show();
			//mConfigState.putExtra(CERTIFICATE_RETRIEVED, true); // If this isn't the last step and finish() is moved...
        	setResult(RESULT_OK);
        	finish();
		} else if(resultCode == ConfigHelper.INCORRECTLY_DOWNLOADED_CERTIFICATE) {
			mProgressDialog.dismiss();
			Toast.makeText(getApplicationContext(), "Your anon cert was not downloaded", Toast.LENGTH_LONG).show();
        	setResult(RESULT_CANCELED, mConfigState);
			finish();
		}
	}

	/**
     * Callback method from {@link ProviderListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(String id) {
    	//TODO Code 2 pane view
    	Iterator<ProviderItem> preseeded_providers_iterator = ProviderListContent.ITEMS.iterator();
    	while(preseeded_providers_iterator.hasNext())
    	{
    		ProviderItem provider = preseeded_providers_iterator.next();
    		if(provider.id.equalsIgnoreCase(id))
    		{
    			mProgressDialog = ProgressDialog.show(this, getResources().getString(R.string.config_wait_title), getResources().getString(R.string.config_connecting_provider), true);
    			mSelectedProvider = provider;
    			saveProviderJson(mSelectedProvider);
    		}
    	}
    }
	
    private boolean loadPreseededProviders() {
    	boolean loaded_preseeded_providers = false;
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
	        		ProviderListContent.addItem(new ProviderItem(provider_name, asset_manager.open(url_files_folder + "/" + url_filepath), custom, true)); // By default, it trusts the provider
	        	loaded_preseeded_providers = true;
	        }
		} catch (IOException e) {
			loaded_preseeded_providers = false;
		}
		
		return loaded_preseeded_providers;
	}

    private void saveProviderJson(ProviderItem current_provider_item) {
    	JSONObject provider_json = new JSONObject();
    	try {
    		String provider_contents = "";
    		if(!current_provider_item.custom) {
    			updateProviderDotJson(current_provider_item.name, current_provider_item.provider_json_url, current_provider_item.danger_on);
    		} else {
    			// FIXME!! We should we be updating our seeded providers list at ConfigurationWizard onStart() ?
    			// I think yes, but if so, where does this list live? leap.se, as it's the non-profit project for the software?
    			// If not, we should just be getting names/urls, and fetching the provider.json like in custom entries
    			provider_contents = new Scanner(ConfigHelper.openFileInputStream(current_provider_item.provider_json_filename)).useDelimiter("\\A").next();
    			provider_json = new JSONObject(provider_contents);
    			ConfigHelper.saveSharedPref(ConfigHelper.PROVIDER_KEY, provider_json);
    			ConfigHelper.saveSharedPref(ConfigHelper.ALLOWED_ANON, provider_json.getJSONObject(ConfigHelper.SERVICE_KEY).getBoolean(ConfigHelper.ALLOWED_ANON));
    			ConfigHelper.saveSharedPref(ConfigHelper.DANGER_ON, current_provider_item.danger_on);
    			
    			mProgressDialog.setMessage(getResources().getString(R.string.config_downloading_services));
    			downloadJSONFiles(mSelectedProvider);
    		}
    	} catch (JSONException e) {
    		setResult(RESULT_CANCELED);
    		finish();
    	}
    }

	private void downloadJSONFiles(ProviderItem current_provider_item) {
		Intent provider_API_command = new Intent(this, ProviderAPI.class);
		
		Bundle method_and_parameters = new Bundle();
		
		method_and_parameters.putString(ConfigHelper.PROVIDER_KEY, current_provider_item.name);
		method_and_parameters.putString(ConfigHelper.MAIN_CERT_KEY, current_provider_item.cert_json_url);
		method_and_parameters.putString(ConfigHelper.EIP_SERVICE_KEY, current_provider_item.eip_service_json_url);
		method_and_parameters.putBoolean(ConfigHelper.DANGER_ON, current_provider_item.danger_on);
		
		provider_API_command.putExtra(ConfigHelper.DOWNLOAD_JSON_FILES_BUNDLE_EXTRA, method_and_parameters);
		provider_API_command.putExtra("receiver", providerAPI_result_receiver);

		startService(provider_API_command);
	}
	
	private boolean downloadAnonCert() {
		Intent provider_API_command = new Intent(this, ProviderAPI.class);

		Bundle method_and_parameters = new Bundle();

		method_and_parameters.putString(ConfigHelper.TYPE_OF_CERTIFICATE, ConfigHelper.ANON_CERTIFICATE);

		provider_API_command.putExtra(ConfigHelper.DOWNLOAD_CERTIFICATE, method_and_parameters);
		provider_API_command.putExtra("receiver", providerAPI_result_receiver);

		startService(provider_API_command);
		return true;
	}
	
	public void addNewProvider(View view) {
		FragmentTransaction fragment_transaction = getFragmentManager().beginTransaction();
	    Fragment previous_new_provider_dialog = getFragmentManager().findFragmentByTag(ConfigHelper.NEW_PROVIDER_DIALOG);
	    if (previous_new_provider_dialog != null) {
	        fragment_transaction.remove(previous_new_provider_dialog);
	    }
	    fragment_transaction.addToBackStack(null);

	    DialogFragment newFragment = NewProviderDialog.newInstance();
	    newFragment.show(fragment_transaction, ConfigHelper.NEW_PROVIDER_DIALOG);
	}

	@Override
	public void saveProvider(String provider_main_url, boolean danger_on) {
		Intent provider_API_command = new Intent(this, ProviderAPI.class);

		Bundle method_and_parameters = new Bundle();
		method_and_parameters.putString(ConfigHelper.PROVIDER_MAIN_URL, provider_main_url);
		method_and_parameters.putBoolean(ConfigHelper.DANGER_ON, danger_on);

		provider_API_command.putExtra(ConfigHelper.DOWNLOAD_NEW_PROVIDER_DOTJSON, method_and_parameters);
		provider_API_command.putExtra("receiver", providerAPI_result_receiver);
		
		startService(provider_API_command);
	}
	
	public void updateProviderDotJson(String provider_name, String provider_json_url, boolean danger_on) {
		Intent provider_API_command = new Intent(this, ProviderAPI.class);

		Bundle method_and_parameters = new Bundle();
		method_and_parameters.putString(ConfigHelper.PROVIDER_NAME, provider_name);
		method_and_parameters.putString(ConfigHelper.PROVIDER_JSON_URL, provider_json_url);
		method_and_parameters.putBoolean(ConfigHelper.DANGER_ON, danger_on);

		provider_API_command.putExtra(ConfigHelper.UPDATE_PROVIDER_DOTJSON, method_and_parameters);
		provider_API_command.putExtra("receiver", providerAPI_result_receiver);
		
		startService(provider_API_command);
	}
}
