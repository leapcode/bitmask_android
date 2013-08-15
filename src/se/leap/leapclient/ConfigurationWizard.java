/**
 * Copyright (c) 2013 LEAP Encryption Access Project and contributers
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
 package se.leap.leapclient;

import java.io.IOException;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import se.leap.leapclient.ProviderAPIResultReceiver.Receiver;
import se.leap.leapclient.ProviderListContent.ProviderItem;
import se.leap.leapclient.R;
import se.leap.openvpn.MainActivity;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

/**
 * Activity that builds and shows the list of known available providers.
 * 
 * It also allows the user to enter custom providers with a button.
 * 
 * @author parmegv
 *
 */
public class ConfigurationWizard extends Activity
implements ProviderListFragment.Callbacks, NewProviderDialog.NewProviderDialogInterface, ProviderDetailFragment.ProviderDetailFragmentInterface, Receiver {

	private ProviderItem mSelectedProvider;
	private ProgressBar mProgressBar;
	private Intent mConfigState = new Intent();

	protected static final String PROVIDER_SET = "PROVIDER SET";
	protected static final String SERVICES_RETRIEVED = "SERVICES RETRIEVED";
    
    public ProviderAPIResultReceiver providerAPI_result_receiver;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.configuration_wizard_activity);
	    mProgressBar = (ProgressBar) findViewById(R.id.progressbar_configuration_wizard);
	    mProgressBar.setVisibility(ProgressBar.INVISIBLE);
        
        providerAPI_result_receiver = new ProviderAPIResultReceiver(new Handler());
        providerAPI_result_receiver.setReceiver(this);
        
        ConfigHelper.setSharedPreferences(getSharedPreferences(ConfigHelper.PREFERENCES_KEY, MODE_PRIVATE));
        
        loadPreseededProviders();
        
        // Only create our fragments if we're not restoring a saved instance
        if ( savedInstanceState == null ){
        	// TODO Some welcome screen?
        	// We will need better flow control when we have more Fragments (e.g. user auth)
        	ProviderListFragment provider_list_fragment = new ProviderListFragment();
        	
        	FragmentManager fragmentManager = getFragmentManager();
        	fragmentManager.beginTransaction()
        		.add(R.id.configuration_wizard_layout, provider_list_fragment, getResources().getString(R.string.provider_list_fragment_tag))
        		.commit();
        }

        // TODO: If exposing deep links into your app, handle intents here.
    }

    private void refreshProviderList(int top_padding) {
    	ProviderListFragment providerList = new ProviderListFragment();
		Bundle top_padding_bundle = new Bundle();
		top_padding_bundle.putInt(getResources().getString(R.string.top_padding), top_padding);
		providerList.setArguments(top_padding_bundle);

		FragmentManager fragmentManager = getFragmentManager();
		fragmentManager.beginTransaction()
		.replace(R.id.configuration_wizard_layout, providerList, getResources().getString(R.string.provider_list_fragment_tag))
		.commit();
    }
    
	@Override
	public void onReceiveResult(int resultCode, Bundle resultData) {
		if(resultCode == ConfigHelper.CORRECTLY_UPDATED_PROVIDER_DOT_JSON) {
			JSONObject provider_json;
			try {
				provider_json = new JSONObject(resultData.getString(ConfigHelper.PROVIDER_KEY));
				boolean danger_on = resultData.getBoolean(ConfigHelper.DANGER_ON);
				ConfigHelper.saveSharedPref(ConfigHelper.PROVIDER_KEY, provider_json);
				ConfigHelper.saveSharedPref(ConfigHelper.DANGER_ON, danger_on);
				ConfigHelper.saveSharedPref(ConfigHelper.ALLOWED_ANON, provider_json.getJSONObject(ConfigHelper.SERVICE_KEY).getBoolean(ConfigHelper.ALLOWED_ANON));
				mConfigState.setAction(PROVIDER_SET);
				
				if(!mProgressBar.isShown())
					startProgressBar();
				mProgressBar.incrementProgressBy(1);
				if(resultData.containsKey(ConfigHelper.PROVIDER_ID))
					mSelectedProvider = getProvider(resultData.getString(ConfigHelper.PROVIDER_ID));

				refreshProviderList(30);
				downloadJSONFiles(mSelectedProvider);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			    mProgressBar.setVisibility(ProgressBar.GONE);
				refreshProviderList(0);
				//Toast.makeText(this, getResources().getString(R.string.config_error_parsing), Toast.LENGTH_LONG);
				setResult(RESULT_CANCELED, mConfigState);
			}
		}
		else if(resultCode == ConfigHelper.INCORRECTLY_UPDATED_PROVIDER_DOT_JSON) {
			refreshProviderList(0);
			mProgressBar.setVisibility(ProgressBar.GONE);
			setResult(RESULT_CANCELED, mConfigState);
		}
		else if(resultCode == ConfigHelper.CORRECTLY_DOWNLOADED_JSON_FILES) {
			mProgressBar.incrementProgressBy(1);
			if (ConfigHelper.getBoolFromSharedPref(ConfigHelper.ALLOWED_ANON)){
				mConfigState.putExtra(SERVICES_RETRIEVED, true);
				downloadAnonCert();
			} else {
				mProgressBar.incrementProgressBy(1);
			    mProgressBar.setVisibility(ProgressBar.GONE);
			    refreshProviderList(0);
				//Toast.makeText(getApplicationContext(), R.string.success, Toast.LENGTH_LONG).show();
				setResult(RESULT_OK);
				showProviderDetails(getCurrentFocus());
			}
		}
		else if(resultCode == ConfigHelper.INCORRECTLY_DOWNLOADED_JSON_FILES) {
			//Toast.makeText(getApplicationContext(), R.string.incorrectly_downloaded_json_files_message, Toast.LENGTH_LONG).show();
			refreshProviderList(0);
			mProgressBar.setVisibility(ProgressBar.GONE);
			setResult(RESULT_CANCELED, mConfigState);
		}
		else if(resultCode == ConfigHelper.CORRECTLY_DOWNLOADED_CERTIFICATE) {
			mProgressBar.incrementProgressBy(1);
		    mProgressBar.setVisibility(ProgressBar.GONE);
		    refreshProviderList(0);
			setResult(RESULT_OK);
			showProviderDetails(getCurrentFocus());
		} else if(resultCode == ConfigHelper.INCORRECTLY_DOWNLOADED_CERTIFICATE) {
			refreshProviderList(0);
			mProgressBar.setVisibility(ProgressBar.GONE);
			//Toast.makeText(getApplicationContext(), R.string.incorrectly_downloaded_certificate_message, Toast.LENGTH_LONG).show();
        	setResult(RESULT_CANCELED, mConfigState);
		}
	}
	
	private void startProgressBar() {
	    FragmentManager fragmentManager = getFragmentManager();
	    fragmentManager.findFragmentByTag(getResources().getString(R.string.provider_list_fragment_tag)).getView().setPadding(8, 30, 8, 0);
	    mProgressBar.setVisibility(ProgressBar.VISIBLE);
	    mProgressBar.setProgress(0);
	    mProgressBar.setMax(3);
	}

	/**
     * Callback method from {@link ProviderListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(String id) {
	    //TODO Code 2 pane view
	    ProviderItem selected_provider = getProvider(id);
	    startProgressBar();
	    mSelectedProvider = selected_provider;
	    saveProviderJson(mSelectedProvider);
    }
    
    @Override
    public void onBackPressed() {
    	try {
			if(ConfigHelper.getJsonFromSharedPref(ConfigHelper.PROVIDER_KEY) == null || ConfigHelper.getJsonFromSharedPref(ConfigHelper.PROVIDER_KEY).length() == 0) {
				askDashboardToQuitApp();
			} else {
				setResult(RESULT_OK);
			}
		} catch (JSONException e) {
			askDashboardToQuitApp();
		}
    	super.onBackPressed();
    }
    
    private void askDashboardToQuitApp() {
		Intent ask_quit = new Intent();
		ask_quit.putExtra(ConfigHelper.QUIT, ConfigHelper.QUIT);
		setResult(RESULT_CANCELED, ask_quit);
    }

    private ProviderItem getProvider(String id) {
	    Iterator<ProviderItem> providers_iterator = ProviderListContent.ITEMS.iterator();
	    while(providers_iterator.hasNext()) {
		    ProviderItem provider = providers_iterator.next();
		    if(provider.id.equalsIgnoreCase(id)) {
			    return provider;
		    }
	    }
	    return null;
    }
	
    /**
     * Loads providers data from url file contained in the project 
     * @return true if the file was read correctly
     */
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

    /**
     * Saves provider.json file associated with provider.
     * 
     * If the provider is custom, the file has already been downloaded so we load it from memory.
     * If not, the file is updated using the provider's URL.
     * @param provider
     */
    private void saveProviderJson(ProviderItem provider) {
    	JSONObject provider_json = new JSONObject();
    	try {
    		if(!provider.custom) {
    			updateProviderDotJson(provider.name, provider.provider_json_url, provider.danger_on);
    		} else {
    			// FIXME!! We should we be updating our seeded providers list at ConfigurationWizard onStart() ?
    			// I think yes, but if so, where does this list live? leap.se, as it's the non-profit project for the software?
    			// If not, we should just be getting names/urls, and fetching the provider.json like in custom entries
    			provider_json = provider.provider_json;
    			ConfigHelper.saveSharedPref(ConfigHelper.PROVIDER_KEY, provider_json);
    			ConfigHelper.saveSharedPref(ConfigHelper.ALLOWED_ANON, provider_json.getJSONObject(ConfigHelper.SERVICE_KEY).getBoolean(ConfigHelper.ALLOWED_ANON));
    			ConfigHelper.saveSharedPref(ConfigHelper.DANGER_ON, provider.danger_on);


    			mProgressBar.incrementProgressBy(1);
    			downloadJSONFiles(mSelectedProvider);
    		}
    	} catch (JSONException e) {
    		setResult(RESULT_CANCELED);
    		finish();
    	}
    }

    /**
     * Asks ProviderAPI to download provider site's certificate and eip-service.json
     * 
     * URLs are fetched from the provider parameter
     * @param provider from which certificate and eip-service.json files are going to be downloaded
     */
	private void downloadJSONFiles(ProviderItem provider) {
		Intent provider_API_command = new Intent(this, ProviderAPI.class);
		
		Bundle parameters = new Bundle();
		
		parameters.putString(ConfigHelper.PROVIDER_KEY, provider.name);
		parameters.putString(ConfigHelper.MAIN_CERT_KEY, provider.cert_json_url);
		parameters.putString(ConfigHelper.EIP_SERVICE_KEY, provider.eip_service_json_url);
		parameters.putBoolean(ConfigHelper.DANGER_ON, provider.danger_on);
		
		provider_API_command.setAction(ProviderAPI.DOWNLOAD_JSON_FILES_BUNDLE_EXTRA);
		provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);
		provider_API_command.putExtra(ConfigHelper.RECEIVER_KEY, providerAPI_result_receiver);

		startService(provider_API_command);
	}
	
	/**
	 * Asks ProviderAPI to download an anonymous (anon) VPN certificate.
	 */
	private void downloadAnonCert() {
		Intent provider_API_command = new Intent(this, ProviderAPI.class);

		Bundle parameters = new Bundle();

		parameters.putString(ConfigHelper.TYPE_OF_CERTIFICATE, ConfigHelper.ANON_CERTIFICATE);

		provider_API_command.setAction(ProviderAPI.DOWNLOAD_CERTIFICATE);
		provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);
		provider_API_command.putExtra(ConfigHelper.RECEIVER_KEY, providerAPI_result_receiver);

		startService(provider_API_command);
	}
	
	/**
	 * Open the new provider dialog
	 * @param view from which the dialog is showed
	 */
	public void addAndSelectNewProvider(View view) {
		FragmentTransaction fragment_transaction = getFragmentManager().beginTransaction();
		Fragment previous_new_provider_dialog = getFragmentManager().findFragmentByTag(ConfigHelper.NEW_PROVIDER_DIALOG);
		if (previous_new_provider_dialog != null) {
			fragment_transaction.remove(previous_new_provider_dialog);
		}
		fragment_transaction.addToBackStack(null);
		
		DialogFragment newFragment = NewProviderDialog.newInstance();
		newFragment.show(fragment_transaction, ConfigHelper.NEW_PROVIDER_DIALOG);
	}
	
	/**
	 * Once selected a provider, this fragment offers the user to log in, 
	 * use it anonymously (if possible) 
	 * or cancel his/her election pressing the back button.
	 * @param view
	 */
	public void showProviderDetails(View view) {
		FragmentTransaction fragment_transaction = getFragmentManager().beginTransaction();
		Fragment previous_provider_details_dialog = getFragmentManager().findFragmentByTag(ConfigHelper.PROVIDER_DETAILS_DIALOG);
		if (previous_provider_details_dialog != null) {
			fragment_transaction.remove(previous_provider_details_dialog);
		}
		fragment_transaction.addToBackStack(null);
		
		DialogFragment newFragment = ProviderDetailFragment.newInstance();
		newFragment.show(fragment_transaction, ConfigHelper.PROVIDER_DETAILS_DIALOG);
	}

	@Override
	public void saveAndSelectProvider(String provider_main_url, boolean danger_on) {
		Intent provider_API_command = new Intent(this, ProviderAPI.class);

		Bundle parameters = new Bundle();
		parameters.putString(ConfigHelper.PROVIDER_MAIN_URL, provider_main_url);
		parameters.putBoolean(ConfigHelper.DANGER_ON, danger_on);

		provider_API_command.setAction(ProviderAPI.DOWNLOAD_NEW_PROVIDER_DOTJSON);
		provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);
		provider_API_command.putExtra(ConfigHelper.RECEIVER_KEY, providerAPI_result_receiver);
		
		startService(provider_API_command);
	}
	
	/**
	 * Asks ProviderAPI to download a new provider.json file
	 * @param provider_name
	 * @param provider_json_url
	 * @param danger_on tells if HTTPS client should bypass certificate errors
	 */
	public void updateProviderDotJson(String provider_name, String provider_json_url, boolean danger_on) {
		Intent provider_API_command = new Intent(this, ProviderAPI.class);

		Bundle parameters = new Bundle();
		parameters.putString(ConfigHelper.PROVIDER_NAME, provider_name);
		parameters.putString(ConfigHelper.PROVIDER_JSON_URL, provider_json_url);
		parameters.putBoolean(ConfigHelper.DANGER_ON, danger_on);

		provider_API_command.setAction(ProviderAPI.UPDATE_PROVIDER_DOTJSON);
		provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);
		provider_API_command.putExtra(ConfigHelper.RECEIVER_KEY, providerAPI_result_receiver);
		
		startService(provider_API_command);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.configuration_wizard_activity, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch (item.getItemId()){
		case R.id.about_leap:
			showAboutFragment(getCurrentFocus());
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	/**
	 * Once selected a provider, this fragment offers the user to log in, 
	 * use it anonymously (if possible) 
	 * or cancel his/her election pressing the back button.
	 * @param view
	 */
	public void showAboutFragment(View view) {
		FragmentTransaction fragment_transaction = getFragmentManager().beginTransaction();
		Fragment previous_about_fragment = getFragmentManager().findFragmentByTag(ConfigHelper.ABOUT_FRAGMENT);
		if (previous_about_fragment == null) {
			fragment_transaction.addToBackStack(null);

			Fragment newFragment = AboutFragment.newInstance();
			fragment_transaction.replace(R.id.configuration_wizard_layout, newFragment, ConfigHelper.ABOUT_FRAGMENT).commit();
		}
	}

	@Override
	public void login() {
		Intent ask_login = new Intent();
		ask_login.putExtra(ConfigHelper.LOG_IN, ConfigHelper.LOG_IN);
		setResult(RESULT_OK, ask_login);
		finish();
	}

	@Override
	public void use_anonymously() {
		setResult(RESULT_OK);
		finish();
	}
}
