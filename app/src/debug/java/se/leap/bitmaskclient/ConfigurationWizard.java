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

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.util.*;
import org.json.*;

import se.leap.bitmaskclient.DownloadFailedDialog.DownloadFailedDialogInterface;
import se.leap.bitmaskclient.NewProviderDialog.NewProviderDialogInterface;
import se.leap.bitmaskclient.ProviderAPIResultReceiver.Receiver;
import se.leap.bitmaskclient.ProviderDetailFragment.ProviderDetailFragmentInterface;
import se.leap.bitmaskclient.ProviderListContent.ProviderItem;
import se.leap.bitmaskclient.eip.Constants;

/**
 * Activity that builds and shows the list of known available providers.
 * 
 * It also allows the user to enter custom providers with a button.
 * 
 * @author parmegv
 *
 */
public class ConfigurationWizard extends Activity
implements ProviderListFragment.Callbacks, NewProviderDialogInterface, ProviderDetailFragmentInterface, DownloadFailedDialogInterface, Receiver {

	private ProgressBar mProgressBar;
	private TextView progressbar_description;
	private ProviderListFragment provider_list_fragment;
	private Intent mConfigState = new Intent();
    private ProviderItem selected_provider;
	
	final public static String TAG = ConfigurationWizard.class.getSimpleName();
	final public static String TYPE_OF_CERTIFICATE = "type_of_certificate";
	final public static String ANON_CERTIFICATE = "anon_certificate";
        final public static String AUTHED_CERTIFICATE = "authed_certificate";

	final protected static String PROVIDER_SET = "PROVIDER SET";
	final protected static String SERVICES_RETRIEVED = "SERVICES RETRIEVED";
    final protected static String ASSETS_URL_FOLDER = "urls";

    final private static String PROGRESSBAR_TEXT = TAG + "PROGRESSBAR_TEXT";
    final private static String PROGRESSBAR_NUMBER = TAG + "PROGRESSBAR_NUMBER";
    
    public ProviderAPIResultReceiver providerAPI_result_receiver;
    private ProviderAPIBroadcastReceiver_Update providerAPI_broadcast_receiver_update;

    private static SharedPreferences preferences;
    FragmentManagerEnhanced fragment_manager;
    private static boolean setting_up_provider = false;
    private String progressbar_text = "";
    private String provider_name = "";
    private int progress = -1;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if(mProgressBar != null)
            outState.putInt(PROGRESSBAR_NUMBER, mProgressBar.getProgress());
        if(progressbar_description != null)
            outState.putString(PROGRESSBAR_TEXT, progressbar_description.getText().toString());
        if(selected_provider != null)
            outState.putString(Provider.NAME, selected_provider.name());
        outState.putParcelable(ProviderAPI.RECEIVER_KEY, providerAPI_result_receiver);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE);
        fragment_manager = new FragmentManagerEnhanced(getFragmentManager());

        setUpInitialUI();

        loadPreseededProviders();

        setUpProviderAPIResultReceiver();

        setUpProviderList();

        if(savedInstanceState != null) {
            progressbar_text = savedInstanceState.getString(PROGRESSBAR_TEXT, "");
            provider_name = savedInstanceState.getString(Provider.NAME, "");
            selected_provider = getProvider(provider_name);
            progress = savedInstanceState.getInt(PROGRESSBAR_NUMBER, -1);
            providerAPI_result_receiver = savedInstanceState.getParcelable(ProviderAPI.RECEIVER_KEY);
            providerAPI_result_receiver.setReceiver(this);
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if(!progressbar_text.isEmpty() && !provider_name.isEmpty() && progress != -1) {
            progressbar_description.setText(progressbar_text);
            onItemSelectedUi(getProvider(provider_name));
            mProgressBar.setProgress(progress);

            progressbar_text = "";
            provider_name = "";
            progress = -1;
        }
    }

    private void setUpInitialUI() {
        setContentView(R.layout.configuration_wizard_activity);
	hideProgressBar();
    }
    
    private void hideProgressBar() {	
	mProgressBar = (ProgressBar) findViewById(R.id.progressbar_configuration_wizard);
	mProgressBar.setVisibility(ProgressBar.INVISIBLE);
	
	progressbar_description = (TextView) findViewById(R.id.progressbar_description);
	progressbar_description.setVisibility(TextView.INVISIBLE);
    }

    private void setUpProviderList() {
	provider_list_fragment = ProviderListFragment.newInstance();
	
	Bundle arguments = new Bundle();
	int configuration_wizard_request_code = getIntent().getIntExtra(Dashboard.REQUEST_CODE, -1);
	if(configuration_wizard_request_code == Dashboard.SWITCH_PROVIDER)
	    arguments.putBoolean(ProviderListFragment.SHOW_ALL_PROVIDERS, true);
	
	provider_list_fragment.setArguments(arguments);

	putProviderListFragment(provider_list_fragment);
    }

    private void putProviderListFragment(ProviderListFragment fragment) {
	fragment_manager.replace(R.id.configuration_wizard_layout, provider_list_fragment, ProviderListFragment.TAG);
    }

    @Override
    protected void onDestroy() {
	super.onDestroy();
	if(providerAPI_broadcast_receiver_update != null)
	    unregisterReceiver(providerAPI_broadcast_receiver_update);
    }

    private void setUpProviderAPIResultReceiver() {
        providerAPI_result_receiver = new ProviderAPIResultReceiver(new Handler());
        providerAPI_result_receiver.setReceiver(this);
	providerAPI_broadcast_receiver_update = new ProviderAPIBroadcastReceiver_Update();
	
	IntentFilter update_intent_filter = new IntentFilter(ProviderAPI.UPDATE_PROGRESSBAR);
	update_intent_filter.addCategory(Intent.CATEGORY_DEFAULT);
	registerReceiver(providerAPI_broadcast_receiver_update, update_intent_filter);
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
	if(resultCode == ProviderAPI.PROVIDER_OK) {
	    mConfigState.setAction(PROVIDER_SET);

	    if (preferences.getBoolean(Constants.ALLOWED_ANON, false)){
		mConfigState.putExtra(SERVICES_RETRIEVED, true);
		
		downloadAnonCert();
	    } else {
		mProgressBar.incrementProgressBy(1);
		hideProgressBar();
		
		setResult(RESULT_OK);

		showProviderDetails();
	    }
	} else if(resultCode == ProviderAPI.PROVIDER_NOK) {
	    hideProgressBar();
	    preferences.edit().remove(Provider.KEY).apply();
	    setting_up_provider = false;
	    
	    setResult(RESULT_CANCELED, mConfigState);
	    
	    String reason_to_fail = resultData.getString(ProviderAPI.ERRORS);
	    showDownloadFailedDialog(getCurrentFocus(), reason_to_fail);
	}
	else if(resultCode == ProviderAPI.CORRECTLY_DOWNLOADED_CERTIFICATE) {
	    mProgressBar.incrementProgressBy(1);
	    hideProgressBar();

	    showProviderDetails();

	    setResult(RESULT_OK);
	} else if(resultCode == ProviderAPI.INCORRECTLY_DOWNLOADED_CERTIFICATE) {
	    hideProgressBar();
	    
	    setResult(RESULT_CANCELED, mConfigState);
	} else if(resultCode == AboutActivity.VIEWED) {
	    // Do nothing, right now
	    // I need this for CW to wait for the About activity to end before going back to Dashboard.
	}
    }

	/**
     * Callback method from {@link ProviderListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(String id) {
	//TODO Code 2 pane view
        selected_provider = getProvider(id);
        onItemSelectedUi(selected_provider);

	boolean danger_on = true;
	if(preferences.contains(ProviderItem.DANGER_ON))
	    danger_on = preferences.getBoolean(ProviderItem.DANGER_ON, false);
	setUpProvider(selected_provider.providerMainUrl(), danger_on);
    }

    private void onItemSelectedUi(ProviderItem provider) {
        startProgressBar();
        int provider_index = getProviderIndex(provider.name());
        provider_list_fragment.hideAllBut(provider_index);
    }
    
    @Override
    public void onBackPressed() {
    	if(setting_up_provider) {
    		stopSettingUpProvider();
    	} else {
    		usualBackButton();
    	}
    }
    
    private void stopSettingUpProvider() {
	ProviderAPI.stop();
	mProgressBar.setVisibility(ProgressBar.GONE);
	mProgressBar.setProgress(0);
	progressbar_description.setVisibility(TextView.GONE);

	preferences.edit().remove(Provider.KEY).apply();
    	setting_up_provider = false;
	showAllProviders();
    }
    
    private void usualBackButton() {
		try {
			boolean is_provider_set_up = new JSONObject(preferences.getString(Provider.KEY, "no provider")) != null ? true : false;
			boolean is_provider_set_up_truly = new JSONObject(preferences.getString(Provider.KEY, "no provider")).length() != 0 ? true : false;
			if(!is_provider_set_up || !is_provider_set_up_truly) {
				askDashboardToQuitApp();
			} else {
				setResult(RESULT_OK);
			}
		} catch (JSONException e) {
			askDashboardToQuitApp();
			super.onBackPressed();
			e.printStackTrace();
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
	
    private void startProgressBar() {
        mProgressBar.setVisibility(ProgressBar.VISIBLE);
        progressbar_description.setVisibility(TextView.VISIBLE);
        mProgressBar.setProgress(0);
        mProgressBar.setMax(3);

	int measured_height = listItemHeight();
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
		    } else index++;
	    }
	    return index;
    }
    
    private int listItemHeight() {
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
        int widthSpec = View.MeasureSpec.makeMeasureSpec(listViewWidth,
                    View.MeasureSpec.AT_MOST);
        listItem.measure(widthSpec, 0);

        return listItem.getMeasuredHeight();
}
    
    /**
     * Loads providers data from url files contained in the assets folder
     * @return true if the files were correctly read
     */
    private boolean loadPreseededProviders() {
    	boolean loaded_preseeded_providers = true;
	try {
	    //TODO Put that folder in a better place (also inside the "for")	    
	    String[] urls_filepaths = getAssets().list(ASSETS_URL_FOLDER); 
	    for(String url_filepath : urls_filepaths) {
		addNewProviderToList(url_filepath);
	    }
	} catch (IOException e) {
	    loaded_preseeded_providers = false;
	}
	
	return loaded_preseeded_providers;
    }

    private void addNewProviderToList(String url_filepath) {
	String provider_main_url = extractProviderMainUrlFromAssetsFile(ASSETS_URL_FOLDER + "/" + url_filepath);
	if(getId(provider_main_url).isEmpty()) {	    
	    String provider_name = url_filepath.subSequence(0, url_filepath.lastIndexOf(".")).toString();
	    ProviderListContent.addItem(new ProviderItem(provider_name, provider_main_url));
	}
    }

    private String extractProviderMainUrlFromAssetsFile(String filepath) {
	String provider_main_url = "";
	try {	    
	    InputStream input_stream_file_contents = getAssets().open(filepath);
	    byte[] urls_file_bytes = new byte[input_stream_file_contents.available()];
	    input_stream_file_contents.read(urls_file_bytes);
	    String urls_file_content = new String(urls_file_bytes);
	    JSONObject file_contents = new JSONObject(urls_file_content);
	    provider_main_url = file_contents.getString(Provider.MAIN_URL);
	} catch (JSONException e) {
	} catch (IOException e) {
	}
	return provider_main_url;
    }
    
    private String getId(String provider_main_url) {
	try {
	URL provider_url = new URL(provider_main_url);
	Iterator<ProviderItem> providers_iterator = ProviderListContent.ITEMS.iterator();
	while(providers_iterator.hasNext()) {
	    ProviderItem provider = providers_iterator.next();
	    URL aux_provider_url = new URL(provider.providerMainUrl());
	    if(isSameURL(provider_url, aux_provider_url)) {
		return provider.name();
	    }
	}
	} catch (MalformedURLException e) {
	    e.printStackTrace();
	}
	return "";
    }

    /**
     * Checks, whether 2 urls are pointing to the same location.
     *
     * @param url a url
     * @param baseUrl an other url, that should be compared.
     * @return true, if the urls point to the same host and port and use the 
     *         same protocol, false otherwise.
     */
    private boolean isSameURL(final URL url, final URL baseUrl) {
	if (!url.getProtocol().equals(baseUrl.getProtocol())) {
	    return false;
	}
	if (!url.getHost().equals(baseUrl.getHost())) {
	    return false;
	}
	if (url.getPort() != baseUrl.getPort()) {
	    return false;
	}
	return true;
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
	    FragmentTransaction fragment_transaction = fragment_manager.removePreviousFragment(NewProviderDialog.TAG);
	    DialogFragment newFragment = NewProviderDialog.newInstance();
	    newFragment.show(fragment_transaction, NewProviderDialog.TAG);
	}
	
	/**
	 * Open the new provider dialog with data
	 */
	public void addAndSelectNewProvider(String main_url, boolean danger_on) {
	    FragmentTransaction fragment_transaction = fragment_manager.removePreviousFragment(NewProviderDialog.TAG);
	    
	    DialogFragment newFragment = NewProviderDialog.newInstance();
	    Bundle data = new Bundle();
	    data.putString(Provider.MAIN_URL, main_url);
	    data.putBoolean(ProviderItem.DANGER_ON, danger_on);
	    newFragment.setArguments(data);
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
	    FragmentTransaction fragment_transaction = fragment_manager.removePreviousFragment(DownloadFailedDialog.TAG);
		
	    DialogFragment newFragment = DownloadFailedDialog.newInstance(reason_to_fail);
	    newFragment.show(fragment_transaction, DownloadFailedDialog.TAG);
	}

	/**
	 * Once selected a provider, this fragment offers the user to log in, 
	 * use it anonymously (if possible) 
	 * or cancel his/her election pressing the back button.
	 */
	private void showProviderDetails() {
		if(setting_up_provider) {
		    FragmentTransaction fragment_transaction = fragment_manager.removePreviousFragment(ProviderDetailFragment.TAG);

		    DialogFragment newFragment = ProviderDetailFragment.newInstance();
		    newFragment.show(fragment_transaction, ProviderDetailFragment.TAG);
		}
	}

	public void showAndSelectProvider(String provider_main_url, boolean danger_on) {
	    if(getId(provider_main_url).isEmpty())
		showProvider(provider_main_url);
	    autoSelectProvider(provider_main_url, danger_on);
	}
	
	private void showProvider(final String provider_main_url) {
		String provider_name = provider_main_url.replaceFirst("http[s]?://", "").replaceFirst("\\/", "_");
		ProviderItem added_provider = new ProviderItem(provider_name, provider_main_url);
		provider_list_fragment.addItem(added_provider);
	}
	
	private void autoSelectProvider(String provider_main_url, boolean danger_on) {
		preferences.edit().putBoolean(ProviderItem.DANGER_ON, danger_on).commit();
		onItemSelected(getId(provider_main_url));
	}
	
	/**
	 * Asks ProviderAPI to download a new provider.json file
n	 * @param provider_main_url
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
		setting_up_provider = true;
	}

	public void retrySetUpProvider() {
		cancelSettingUpProvider();
		if(!ProviderAPI.caCertDownloaded()) {
			addAndSelectNewProvider(ProviderAPI.lastProviderMainUrl(), ProviderAPI.lastDangerOn());
		} else {
			Intent provider_API_command = new Intent(this, ProviderAPI.class);

			provider_API_command.setAction(ProviderAPI.SET_UP_PROVIDER);
			provider_API_command.putExtra(ProviderAPI.RECEIVER_KEY, providerAPI_result_receiver);

			startService(provider_API_command);
		}
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
		    startActivityForResult(new Intent(this, AboutActivity.class), 0);
			return true;
		case R.id.new_provider:
			addAndSelectNewProvider();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
		
	public void showAllProviders() {
		provider_list_fragment = (ProviderListFragment) fragment_manager.findFragmentByTag(ProviderListFragment.TAG);
		if(provider_list_fragment != null)
			provider_list_fragment.unhideAll();
	}
	
	public void cancelSettingUpProvider() {
		provider_list_fragment = (ProviderListFragment) fragment_manager.findFragmentByTag(ProviderListFragment.TAG);
		if(provider_list_fragment != null && preferences.contains(ProviderItem.DANGER_ON)) {
			provider_list_fragment.removeLastItem();
		}
		preferences.edit().remove(Provider.KEY).remove(ProviderItem.DANGER_ON).remove(Constants.ALLOWED_ANON).remove(Constants.KEY).commit();
	}

	@Override
	public void login() {
		Intent ask_login = new Intent();
		ask_login.putExtra(LogInDialog.TAG, LogInDialog.TAG);
		setResult(RESULT_OK, ask_login);
		setting_up_provider = false;
		finish();
	}

	@Override
	public void use_anonymously() {
		setResult(RESULT_OK);
		setting_up_provider = false;
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
