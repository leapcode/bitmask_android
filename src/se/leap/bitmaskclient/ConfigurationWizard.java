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
 package se.leap.bitmaskclient;

import java.io.IOException;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.ProviderAPIResultReceiver.Receiver;
import se.leap.bitmaskclient.ProviderListContent.ProviderItem;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.view.View.MeasureSpec;
import android.view.WindowManager;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

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

	private ProgressBar mProgressBar;
	private TextView progressbar_description;
	private ProviderListFragment provider_list_fragment;
	private Intent mConfigState = new Intent();
	
	final public static String TAG = "se.leap.bitmaskclient.ConfigurationWizard";
	final public static String TYPE_OF_CERTIFICATE = "type_of_certificate";
	final public static String ANON_CERTIFICATE = "anon_certificate";
	final public static String AUTHED_CERTIFICATE = "authed_certificate";

	final protected static String PROVIDER_SET = "PROVIDER SET";
	final protected static String SERVICES_RETRIEVED = "SERVICES RETRIEVED";
    
    public ProviderAPIResultReceiver providerAPI_result_receiver;
    private ProviderAPIBroadcastReceiver_Update providerAPI_broadcast_receiver_update;

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.configuration_wizard_activity);
	    mProgressBar = (ProgressBar) findViewById(R.id.progressbar_configuration_wizard);
	    mProgressBar.setVisibility(ProgressBar.INVISIBLE);
	    progressbar_description = (TextView) findViewById(R.id.progressbar_description);
	    progressbar_description.setVisibility(TextView.INVISIBLE);
        providerAPI_result_receiver = new ProviderAPIResultReceiver(new Handler());
        providerAPI_result_receiver.setReceiver(this);
	    providerAPI_broadcast_receiver_update = new ProviderAPIBroadcastReceiver_Update();
	    IntentFilter update_intent_filter = new IntentFilter(ProviderAPI.UPDATE_PROGRESSBAR);
	    update_intent_filter.addCategory(Intent.CATEGORY_DEFAULT);
	    registerReceiver(providerAPI_broadcast_receiver_update, update_intent_filter);
	    
        ConfigHelper.setSharedPreferences(getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE));
        
        loadPreseededProviders();
        
        // Only create our fragments if we're not restoring a saved instance
        if ( savedInstanceState == null ){
        	// TODO Some welcome screen?
        	// We will need better flow control when we have more Fragments (e.g. user auth)
        	provider_list_fragment = ProviderListFragment.newInstance();
    		Bundle arguments = new Bundle();
    		int configuration_wizard_request_code = getIntent().getIntExtra(Dashboard.REQUEST_CODE, -1);
    		if(configuration_wizard_request_code == Dashboard.SWITCH_PROVIDER) {
    			arguments.putBoolean(ProviderListFragment.SHOW_ALL_PROVIDERS, true);
    		}
			provider_list_fragment.setArguments(arguments);

    		FragmentManager fragmentManager = getFragmentManager();
    		fragmentManager.beginTransaction()
    		.replace(R.id.configuration_wizard_layout, provider_list_fragment, ProviderListFragment.TAG)
    		.commit();
        }

        // TODO: If exposing deep links into your app, handle intents here.
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(providerAPI_broadcast_receiver_update);
	}

    public void refreshProviderList(int top_padding) {
    	ProviderListFragment new_provider_list_fragment = new ProviderListFragment();
		Bundle top_padding_bundle = new Bundle();
		top_padding_bundle.putInt(getResources().getString(R.string.top_padding), top_padding);
		new_provider_list_fragment.setArguments(top_padding_bundle);

		FragmentManager fragmentManager = getFragmentManager();
		fragmentManager.beginTransaction()
		.replace(R.id.configuration_wizard_layout, new_provider_list_fragment, ProviderListFragment.TAG)
		.commit();
    }
    
	@Override
	public void onReceiveResult(int resultCode, Bundle resultData) {
		if(resultCode == ProviderAPI.PROVIDER_OK) {
				mConfigState.setAction(PROVIDER_SET);

				if (ConfigHelper.getBoolFromSharedPref(EIP.ALLOWED_ANON)){
					mConfigState.putExtra(SERVICES_RETRIEVED, true);
					downloadAnonCert();
				} else {
					mProgressBar.incrementProgressBy(1);
				    mProgressBar.setVisibility(ProgressBar.GONE);
				    progressbar_description.setVisibility(TextView.GONE);
					setResult(RESULT_OK);
					showProviderDetails(getCurrentFocus());
				}
		} else if(resultCode == ProviderAPI.PROVIDER_NOK) {
			//refreshProviderList(0);
			String reason_to_fail = resultData.getString(ProviderAPI.ERRORS);
			showDownloadFailedDialog(getCurrentFocus(), reason_to_fail);
			mProgressBar.setVisibility(ProgressBar.GONE);
			progressbar_description.setVisibility(TextView.GONE);
			ConfigHelper.removeFromSharedPref(Provider.KEY);
			setResult(RESULT_CANCELED, mConfigState);
		}
		else if(resultCode == ProviderAPI.CORRECTLY_DOWNLOADED_CERTIFICATE) {
			mProgressBar.incrementProgressBy(1);
		    mProgressBar.setVisibility(ProgressBar.GONE);
		    progressbar_description.setVisibility(TextView.GONE);
		    //refreshProviderList(0);
			setResult(RESULT_OK);
			showProviderDetails(getCurrentFocus());
		} else if(resultCode == ProviderAPI.INCORRECTLY_DOWNLOADED_CERTIFICATE) {
			//refreshProviderList(0);
			mProgressBar.setVisibility(ProgressBar.GONE);
		    progressbar_description.setVisibility(TextView.GONE);
			//Toast.makeText(getApplicationContext(), R.string.incorrectly_downloaded_certificate_message, Toast.LENGTH_LONG).show();
        	setResult(RESULT_CANCELED, mConfigState);
		}
	}

	/**
     * Callback method from {@link ProviderListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(String id) {
	    //TODO Code 2 pane view
	//	resetOldConnection();
	    ProviderItem selected_provider = getProvider(id);
	    int provider_index = getProviderIndex(id);
	    startProgressBar(provider_index);
	    provider_list_fragment.hideAllBut(provider_index);
	    setUpProvider(selected_provider.providerMainUrl(), true);
    }
    
    @Override
    public void onBackPressed() {
    	try {
			if(ConfigHelper.getJsonFromSharedPref(Provider.KEY) == null || ConfigHelper.getJsonFromSharedPref(Provider.KEY).length() == 0) {
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
		ask_quit.putExtra(Dashboard.ACTION_QUIT, Dashboard.ACTION_QUIT);
		setResult(RESULT_CANCELED, ask_quit);
    }

    private ProviderItem getProvider(String name) {
	    Iterator<ProviderItem> providers_iterator = ProviderListContent.ITEMS.iterator();
	    while(providers_iterator.hasNext()) {
		    ProviderItem provider = providers_iterator.next();
		    if(provider.name().equalsIgnoreCase(name)) {
			    return provider;
		    }
	    }
	    return null;
    }
    
    private String getId(String provider_main_url) {
	    Iterator<ProviderItem> providers_iterator = ProviderListContent.ITEMS.iterator();
	    while(providers_iterator.hasNext()) {
		    ProviderItem provider = providers_iterator.next();
		    if(provider.providerMainUrl().equalsIgnoreCase(provider_main_url)) {
			    return provider.name();
		    }
	    }
	    return "";
    }
	
	private void startProgressBar(int list_item_index) {
	    mProgressBar.setVisibility(ProgressBar.VISIBLE);
	    progressbar_description.setVisibility(TextView.VISIBLE);
	    mProgressBar.setProgress(0);
	    mProgressBar.setMax(3);
	    int measured_height = listItemHeight(list_item_index);
	    mProgressBar.setTranslationY(measured_height);
	    progressbar_description.setTranslationY(measured_height + mProgressBar.getHeight());
	}

    private int getProviderIndex(String id) {
    	int index = 0;
	    Iterator<ProviderItem> providers_iterator = ProviderListContent.ITEMS.iterator();
	    while(providers_iterator.hasNext()) {
		    ProviderItem provider = providers_iterator.next();
		    if(provider.name().equalsIgnoreCase(id)) {
			    break;
		    }
		    index++;
	    }
	    return index;
    }
    
    private int listItemHeight(int list_item_index) {
        ListView provider_list_view = (ListView)findViewById(android.R.id.list);
        ListAdapter provider_list_adapter = provider_list_view.getAdapter();
        View listItem = provider_list_adapter.getView(0, null, provider_list_view);
        listItem.setLayoutParams(new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT));
        WindowManager wm = (WindowManager) getApplicationContext()
                    .getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        int screenWidth = display.getWidth(); // deprecated

        int listViewWidth = screenWidth - 10 - 10;
        int widthSpec = MeasureSpec.makeMeasureSpec(listViewWidth,
                    MeasureSpec.AT_MOST);
        listItem.measure(widthSpec, 0);

        return listItem.getMeasuredHeight();
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
	        		ProviderListContent.addItem(new ProviderItem(provider_name, asset_manager.open(url_files_folder + "/" + url_filepath)));
	        	loaded_preseeded_providers = true;
	        }
		} catch (IOException e) {
			loaded_preseeded_providers = false;
		}
		
		return loaded_preseeded_providers;
	}
	
	/**
	 * Asks ProviderAPI to download an anonymous (anon) VPN certificate.
	 */
	private void downloadAnonCert() {
		Intent provider_API_command = new Intent(this, ProviderAPI.class);

		Bundle parameters = new Bundle();

		parameters.putString(TYPE_OF_CERTIFICATE, ANON_CERTIFICATE);

		provider_API_command.setAction(ProviderAPI.DOWNLOAD_CERTIFICATE);
		provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);
		provider_API_command.putExtra(ProviderAPI.RECEIVER_KEY, providerAPI_result_receiver);

		startService(provider_API_command);
	}
	
	/**
	 * Open the new provider dialog
	 */
	public void addAndSelectNewProvider() {
		FragmentTransaction fragment_transaction = getFragmentManager().beginTransaction();
		Fragment previous_new_provider_dialog = getFragmentManager().findFragmentByTag(NewProviderDialog.TAG);
		if (previous_new_provider_dialog != null) {
			fragment_transaction.remove(previous_new_provider_dialog);
		}
		fragment_transaction.addToBackStack(null);
		
		DialogFragment newFragment = NewProviderDialog.newInstance();
		newFragment.show(fragment_transaction, NewProviderDialog.TAG);
	}
	
	/**
	 * Once selected a provider, this fragment offers the user to log in, 
	 * use it anonymously (if possible) 
	 * or cancel his/her election pressing the back button.
	 * @param view
	 * @param reason_to_fail 
	 */
	public void showDownloadFailedDialog(View view, String reason_to_fail) {
		FragmentTransaction fragment_transaction = getFragmentManager().beginTransaction();
		Fragment previous_provider_details_dialog = getFragmentManager().findFragmentByTag(DownloadFailedDialog.TAG);
		if (previous_provider_details_dialog != null) {
			fragment_transaction.remove(previous_provider_details_dialog);
		}
		fragment_transaction.addToBackStack(null);
		
		DialogFragment newFragment = DownloadFailedDialog.newInstance(reason_to_fail);
		newFragment.show(fragment_transaction, DownloadFailedDialog.TAG);
	}
	
	/**
	 * Once selected a provider, this fragment offers the user to log in, 
	 * use it anonymously (if possible) 
	 * or cancel his/her election pressing the back button.
	 * @param view
	 */
	public void showProviderDetails(View view) {
		FragmentTransaction fragment_transaction = getFragmentManager().beginTransaction();
		Fragment previous_provider_details_dialog = getFragmentManager().findFragmentByTag(ProviderDetailFragment.TAG);
		if (previous_provider_details_dialog != null) {
			fragment_transaction.remove(previous_provider_details_dialog);
		}
		fragment_transaction.addToBackStack(null);
		
		DialogFragment newFragment = ProviderDetailFragment.newInstance();
		newFragment.show(fragment_transaction, ProviderDetailFragment.TAG);
	}

	public void showAndSelectProvider(String provider_main_url, boolean danger_on) {
		showProvider(provider_main_url, danger_on);
		autoSelectProvider(provider_main_url, danger_on);
	}
	
	private void showProvider(final String provider_main_url, final boolean danger_on) {
		String provider_name = provider_main_url.replaceFirst("http[s]?://", "").replaceFirst("\\/", "_");
		ProviderItem added_provider = new ProviderItem(provider_name, provider_main_url);
		provider_list_fragment.addItem(added_provider);
	}
	
	private void autoSelectProvider(String provider_main_url, boolean danger_on) {
		onItemSelected(getId(provider_main_url));
	}
	
	/**
	 * Asks ProviderAPI to download a new provider.json file
	 * @param provider_name
	 * @param provider_main_url
	 * @param danger_on tells if HTTPS client should bypass certificate errors
	 */
	public void setUpProvider(String provider_main_url, boolean danger_on) {
		Intent provider_API_command = new Intent(this, ProviderAPI.class);
		Bundle parameters = new Bundle();
		parameters.putString(Provider.MAIN_URL, provider_main_url);
		parameters.putBoolean(ProviderItem.DANGER_ON, danger_on);

		provider_API_command.setAction(ProviderAPI.SET_UP_PROVIDER);
		provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);
		provider_API_command.putExtra(ProviderAPI.RECEIVER_KEY, providerAPI_result_receiver);

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
			return true;
		case R.id.new_provider:
			addAndSelectNewProvider();
			return true;
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
		Fragment previous_about_fragment = getFragmentManager().findFragmentByTag(AboutFragment.TAG);
		if (previous_about_fragment == null) {
			fragment_transaction.addToBackStack(null);

			Fragment newFragment = AboutFragment.newInstance();
			fragment_transaction.replace(R.id.configuration_wizard_layout, newFragment, AboutFragment.TAG).commit();
		}
	}
		
	public void showAllProviders() {
		provider_list_fragment = (ProviderListFragment) getFragmentManager().findFragmentByTag(ProviderListFragment.TAG);
		if(provider_list_fragment != null)
			provider_list_fragment.unhideAll();
	}

	@Override
	public void login() {
		Intent ask_login = new Intent();
		ask_login.putExtra(LogInDialog.VERB, LogInDialog.VERB);
		setResult(RESULT_OK, ask_login);
		finish();
	}

	@Override
	public void use_anonymously() {
		setResult(RESULT_OK);
		finish();
	}

	public class ProviderAPIBroadcastReceiver_Update extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			int update = intent.getIntExtra(ProviderAPI.CURRENT_PROGRESS, 0);
			mProgressBar.setProgress(update);
		}
	}
}
