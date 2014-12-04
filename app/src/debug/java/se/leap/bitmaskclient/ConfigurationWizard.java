/**
 * Copyright (c) 2013 LEAP Encryption Access Project and contributors
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

import com.pedrogomez.renderers.Renderer;
import com.pedrogomez.renderers.RendererAdapter;

import java.io.*;
import java.net.*;
import java.util.*;

import butterknife.ButterKnife;
import butterknife.InjectView;
import org.jetbrains.annotations.NotNull;
import org.json.*;

import javax.inject.Inject;

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

    @InjectView(R.id.progressbar_configuration_wizard) ProgressBar mProgressBar;
    @InjectView(R.id.progressbar_description) TextView progressbar_description;

    @InjectView(R.id.provider_list) ListView provider_list_view;
    @Inject RendererAdapter<Provider> adapter;

    private ProviderManager provider_manager;
	private ProviderListFragment provider_list_fragment;
	private Intent mConfigState = new Intent();
    private ProviderItem selected_provider;
	
	final public static String TAG = ConfigurationWizard.class.getSimpleName();
	final public static String TYPE_OF_CERTIFICATE = "type_of_certificate";
	final public static String ANON_CERTIFICATE = "anon_certificate";
        final public static String AUTHED_CERTIFICATE = "authed_certificate";

	final protected static String PROVIDER_SET = "PROVIDER SET";
	final protected static String SERVICES_RETRIEVED = "SERVICES RETRIEVED";

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

    private void initProviderList() {
        List<Renderer<Provider>> prototypes = new ArrayList<Renderer<Provider>>();
        prototypes.add(new ProviderRenderer(this));
        ProviderRendererBuilder providerRendererBuilder = new ProviderRendererBuilder(prototypes);
        adapter = new RendererAdapter<Provider>(getLayoutInflater(), providerRendererBuilder, provider_manager);
        provider_list_view.setAdapter(adapter);
    }
    
    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
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
        provider_manager = new ProviderManager(getAssets());

        setUpInitialUI();

        setUpProviderAPIResultReceiver();

        setUpProviderList();

        if(savedInstanceState != null) {
            restoreState(savedInstanceState);
        }
    }

    private void restoreState(Bundle savedInstanceState) {
        progressbar_text = savedInstanceState.getString(PROGRESSBAR_TEXT, "");
        provider_name = savedInstanceState.getString(Provider.NAME, "");
        selected_provider = getProvider(provider_name);
        progress = savedInstanceState.getInt(PROGRESSBAR_NUMBER, -1);
        providerAPI_result_receiver = savedInstanceState.getParcelable(ProviderAPI.RECEIVER_KEY);
        providerAPI_result_receiver.setReceiver(this);
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
        ButterKnife.inject(this);

        hideProgressBar();
    }
    
    private void hideProgressBar() {	
	mProgressBar.setVisibility(ProgressBar.INVISIBLE);
	progressbar_description.setVisibility(TextView.INVISIBLE);
    }

    private void setUpProviderList() {
        initProviderList();
	// provider_list_fragment = ProviderListFragment.newInstance();

	// Bundle arguments = new Bundle();
	// int configuration_wizard_request_code = getIntent().getIntExtra(Dashboard.REQUEST_CODE, -1);
	// if(configuration_wizard_request_code == Dashboard.SWITCH_PROVIDER)
	//     arguments.putBoolean(ProviderListFragment.SHOW_ALL_PROVIDERS, true);

	// provider_list_fragment.setArguments(arguments);

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
	    showDownloadFailedDialog(reason_to_fail);
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
	if(preferences.getString(Provider.KEY, "").isEmpty()) {
	    askDashboardToQuitApp();
	} else {
	    setResult(RESULT_OK);
	}
	super.onBackPressed();
    }
    
    private void askDashboardToQuitApp() {
		Intent ask_quit = new Intent();
		ask_quit.putExtra(Dashboard.ACTION_QUIT, Dashboard.ACTION_QUIT);
		setResult(RESULT_CANCELED, ask_quit);
    }

    private ProviderItem getProvider(String name) {
        for (ProviderItem provider : ProviderListContent.ITEMS) {
            if (provider.name().equalsIgnoreCase(name)) {
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
        for (ProviderItem provider : ProviderListContent.ITEMS) {
            if (provider.name().equalsIgnoreCase(id)) {
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
    
    private String getId(String provider_main_url) {
	try {
	URL provider_url = new URL(provider_main_url);
        for (ProviderItem provider : ProviderListContent.ITEMS) {
            URL aux_provider_url = new URL(provider.providerMainUrl());
            if (isSameURL(provider_url, aux_provider_url)) {
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
        return url.getProtocol().equals(baseUrl.getProtocol()) &&
                url.getHost().equals(baseUrl.getHost()) &&
                url.getPort() == baseUrl.getPort();
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
	 * @param reason_to_fail
	 */
	public void showDownloadFailedDialog(String reason_to_fail) {
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
        try {
            provider_manager.add(new Provider(new URL((provider_main_url))));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
	    autoSelectProvider(provider_main_url, danger_on);
	}
	
	private void autoSelectProvider(String provider_main_url, boolean danger_on) {
		preferences.edit().putBoolean(ProviderItem.DANGER_ON, danger_on).apply();
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
		preferences.edit().remove(Provider.KEY).remove(ProviderItem.DANGER_ON).remove(Constants.ALLOWED_ANON).remove(Constants.KEY).apply();
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
