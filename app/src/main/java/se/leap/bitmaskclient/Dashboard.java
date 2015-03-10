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

import android.annotation.SuppressLint;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.widget.*;

import org.jetbrains.annotations.NotNull;
import org.json.*;
import java.net.*;

import butterknife.*;
import de.blinkt.openvpn.activities.*;
import se.leap.bitmaskclient.eip.*;

/**
 * The main user facing Activity of Bitmask Android, consisting of status, controls,
 * and access to preferences.
 * 
 * @author Sean Leonard <meanderingcode@aetherislands.net>
 * @author parmegv
 */
public class Dashboard extends Activity implements SessionDialog.SessionDialogInterface, ProviderAPIResultReceiver.Receiver {

    protected static final int CONFIGURE_LEAP = 0;
    protected static final int SWITCH_PROVIDER = 1;

    public static final String TAG = Dashboard.class.getSimpleName();
    public static final String SHARED_PREFERENCES = "LEAPPreferences";
    public static final String ACTION_QUIT = "quit";
    public static final String REQUEST_CODE = "request_code";
    public static final String PARAMETERS = "dashboard parameters";
    public static final String START_ON_BOOT = "dashboard start on boot";
    public static final String ON_BOOT = "dashboard on boot";
    public static final String APP_VERSION = "bitmask version";

    private static Context app;
    protected static SharedPreferences preferences;
    private FragmentManagerEnhanced fragment_manager;

    @InjectView(R.id.providerName)
    TextView provider_name;

    EipFragment eip_fragment;
    private Provider provider;
    public ProviderAPIResultReceiver providerAPI_result_receiver;
    private boolean switching_provider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
		
	app = this;

	PRNGFixes.apply();

	preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
	fragment_manager = new FragmentManagerEnhanced(getFragmentManager());
	handleVersion();

        provider = getSavedProvider(savedInstanceState);
        if (provider == null || provider.getName().isEmpty())
	    startActivityForResult(new Intent(this,ConfigurationWizard.class),CONFIGURE_LEAP);
	else
	    buildDashboard(getIntent().getBooleanExtra(ON_BOOT, false));
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        if(provider != null)
            outState.putParcelable(Provider.KEY, provider);
        super.onSaveInstanceState(outState);
    }

    private Provider getSavedProvider(Bundle savedInstanceState) {
        Provider provider = null;
        if(savedInstanceState != null)
            provider = savedInstanceState.getParcelable(Provider.KEY);
        else if(preferences.getBoolean(Constants.PROVIDER_CONFIGURED, false))
            provider = getSavedProviderFromSharedPreferences();

        return provider;
    }

    private Provider getSavedProviderFromSharedPreferences() {
        Provider provider = null;
        try {
            provider = new Provider(new URL(preferences.getString(Provider.MAIN_URL, "")));
            provider.define(new JSONObject(preferences.getString(Provider.KEY, "")));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return provider;
    }

    private void handleVersion() {
	try {
	    int versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
	    int lastDetectedVersion = preferences.getInt(APP_VERSION, 0);
	    preferences.edit().putInt(APP_VERSION, versionCode).apply();

	    switch(versionCode) {
	    case 91: // 0.6.0 without Bug #5999
	    case 101: // 0.8.0
		if(!preferences.getString(Constants.KEY, "").isEmpty())
		    eip_fragment.updateEipService();
		break;
	    }
	} catch (NameNotFoundException e) {
	    Log.d(TAG, "Handle version didn't find any " + getPackageName() + " package");
	}
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
	if ( requestCode == CONFIGURE_LEAP || requestCode == SWITCH_PROVIDER) {
	    if ( resultCode == RESULT_OK && data.hasExtra(Provider.KEY)) {
                provider = data.getParcelableExtra(Provider.KEY);
                providerToPreferences(provider);

                buildDashboard(false);
                invalidateOptionsMenu();
                if (data.hasExtra(SessionDialog.TAG)) {
                    sessionDialog(Bundle.EMPTY);
                }

	    } else if (resultCode == RESULT_CANCELED && data.hasExtra(ACTION_QUIT)) {
                finish();
	    } else
		configErrorDialog();
	} else if(requestCode == EIP.DISCONNECT) {
	    EipStatus.getInstance().setConnectedOrDisconnected();
	}
    }

    @SuppressLint("CommitPrefEdits")
    private void providerToPreferences(Provider provider) {
        preferences.edit().putBoolean(Constants.PROVIDER_CONFIGURED, true).commit();
        preferences.edit().putString(Provider.MAIN_URL, provider.mainUrl().toString()).apply();
        preferences.edit().putString(Provider.KEY, provider.definition().toString()).apply();
    }

    private void configErrorDialog() {
	AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getAppContext());
	alertBuilder.setTitle(getResources().getString(R.string.setup_error_title));
	alertBuilder
	    .setMessage(getResources().getString(R.string.setup_error_text))
	    .setCancelable(false)
	    .setPositiveButton(getResources().getString(R.string.setup_error_configure_button), new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
			startActivityForResult(new Intent(getAppContext(),ConfigurationWizard.class),CONFIGURE_LEAP);
		    }
		})
	    .setNegativeButton(getResources().getString(R.string.setup_error_close_button), new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
			preferences.edit().remove(Provider.KEY).remove(Constants.PROVIDER_CONFIGURED).apply();
			finish();
		    }
		})
	    .show();
    }
	
    /**
     * Inflates permanent UI elements of the View and contains logic for what
     * service dependent UI elements to include.
     */
    private void buildDashboard(boolean hide_and_turn_on_eip) {
	setContentView(R.layout.dashboard);
        ButterKnife.inject(this);

	provider_name.setText(provider.getDomain());
	if ( provider.hasEIP()){
            fragment_manager.removePreviousFragment(EipFragment.TAG);
            eip_fragment = new EipFragment();

	    if (hide_and_turn_on_eip) {
		preferences.edit().remove(Dashboard.START_ON_BOOT).apply();
		Bundle arguments = new Bundle();
		arguments.putBoolean(EipFragment.START_ON_BOOT, true);
                if(eip_fragment != null) eip_fragment.setArguments(arguments);
	    }

            fragment_manager.replace(R.id.servicesCollection, eip_fragment, EipFragment.TAG);
	    if (hide_and_turn_on_eip) {
		onBackPressed();
	    }
	}
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
	JSONObject provider_json;
	try {
	    String provider_json_string = preferences.getString(Provider.KEY, "");
	    if(!provider_json_string.isEmpty()) {
		provider_json = new JSONObject(provider_json_string);
		JSONObject service_description = provider_json.getJSONObject(Provider.SERVICE);
		boolean allow_registered_eip = service_description.getBoolean(Provider.ALLOW_REGISTRATION);
		preferences.edit().putBoolean(Constants.ALLOWED_REGISTERED, allow_registered_eip).apply();
		
		if(allow_registered_eip) {
		    if(LeapSRPSession.loggedIn()) {
			menu.findItem(R.id.login_button).setVisible(false);
			menu.findItem(R.id.logout_button).setVisible(true);
		    } else {
			menu.findItem(R.id.login_button).setVisible(true);
			menu.findItem(R.id.logout_button).setVisible(false);
		    }
		    menu.findItem(R.id.signup_button).setVisible(true);
		}
	    }
	} catch (JSONException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	getMenuInflater().inflate(R.menu.client_dashboard, menu);
	return true;
    }
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
	Intent intent;
	switch (item.getItemId()){
	case R.id.about_leap:
	    intent = new Intent(this, AboutActivity.class);
	    startActivity(intent);
	    return true;
	case R.id.log_window:
	    Intent startLW = new Intent(getAppContext(), LogWindow.class);
	    startLW.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
	    startActivity(startLW);
	    return true;
	case R.id.switch_provider:
	    switching_provider = true;
	    if (LeapSRPSession.loggedIn()) logOut();
	    else switchProvider();
	    return true;
	case R.id.login_button:
	    sessionDialog(Bundle.EMPTY);
	    return true;
	case R.id.logout_button:
	    logOut();
	    return true;
	case R.id.signup_button:
	    sessionDialog(Bundle.EMPTY);
	    return true;
	default:
	    return super.onOptionsItemSelected(item);
	}
    }

    @Override
    public void signUp(String username, String password) {
	Bundle parameters = bundleParameters(username, password);
	providerApiCommand(parameters, R.string.signingup_message, ProviderAPI.SRP_REGISTER);
    }

    @Override
    public void logIn(String username, String password) {
	Bundle parameters = bundleParameters(username, password);
	providerApiCommand(parameters, R.string.authenticating_message, ProviderAPI.SRP_AUTH);
    }
	
    public void logOut() {
	providerApiCommand(Bundle.EMPTY, R.string.logout_message, ProviderAPI.LOG_OUT);
    }

    protected void downloadVpnCertificate() {
        boolean is_authenticated = LeapSRPSession.loggedIn();
        boolean allowed_anon = preferences.getBoolean(Constants.ALLOWED_ANON, false);
        if(allowed_anon || is_authenticated)
            providerApiCommand(Bundle.EMPTY, R.string.downloading_certificate_message, ProviderAPI.DOWNLOAD_CERTIFICATE);
        else
            sessionDialog(Bundle.EMPTY);

    }

    private Bundle bundleParameters(String username, String password) {
        Bundle parameters = new Bundle();
	if(!username.isEmpty())
	    parameters.putString(SessionDialog.USERNAME, username);
        if(!password.isEmpty())
	    parameters.putString(SessionDialog.PASSWORD, password);
	return parameters;
    }

    protected void providerApiCommand(Bundle parameters, int progressbar_message_resId, String providerApi_action) {
        if(eip_fragment != null && progressbar_message_resId != 0) {
            eip_fragment.progress_bar.setVisibility(ProgressBar.VISIBLE);
            setStatusMessage(progressbar_message_resId);
        }
	
	Intent command = prepareProviderAPICommand(parameters, providerApi_action);
	startService(command);
    }

    private Intent prepareProviderAPICommand(Bundle parameters, String action) {
	providerAPI_result_receiver = new ProviderAPIResultReceiver(new Handler());
	providerAPI_result_receiver.setReceiver(this);
	
	Intent command = new Intent(this, ProviderAPI.class);
	
	command.putExtra(ProviderAPI.PARAMETERS, parameters);
	command.putExtra(ProviderAPI.RECEIVER_KEY, providerAPI_result_receiver);
	command.setAction(action);
	return command;
    }

    public void cancelLoginOrSignup() {
      EipStatus.getInstance().setConnectedOrDisconnected();
    }
    
    public void sessionDialog(Bundle resultData) {
	
	FragmentTransaction transaction = fragment_manager.removePreviousFragment(SessionDialog.TAG);

	DialogFragment newFragment = new SessionDialog();
	if(resultData != null && !resultData.isEmpty()) {
	    newFragment.setArguments(resultData);
 	}
	newFragment.show(transaction, SessionDialog.TAG);
    }

    private void switchProvider() {
        if (provider.hasEIP()) eip_fragment.askToStopEIP();
	
        preferences.edit().clear().apply();
        switching_provider = false;
        startActivityForResult(new Intent(this, ConfigurationWizard.class), SWITCH_PROVIDER);
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
	Log.d(TAG, "onReceiveResult");
	if(resultCode == ProviderAPI.SUCCESSFUL_SIGNUP) {
	    String username = resultData.getString(SessionDialog.USERNAME);
	    String password = resultData.getString(SessionDialog.PASSWORD);
	    logIn(username, password);
	} else if(resultCode == ProviderAPI.FAILED_SIGNUP) {
	    updateViewHidingProgressBar(resultCode);
	    sessionDialog(resultData);
	} else if(resultCode == ProviderAPI.SUCCESSFUL_LOGIN) {
	    updateViewHidingProgressBar(resultCode);
	    downloadVpnCertificate();
	} else if(resultCode == ProviderAPI.FAILED_LOGIN) {
	    updateViewHidingProgressBar(resultCode);
	    sessionDialog(resultData);
	} else if(resultCode == ProviderAPI.SUCCESSFUL_LOGOUT) {
	    updateViewHidingProgressBar(resultCode);
	    if(switching_provider) switchProvider();
	} else if(resultCode == ProviderAPI.LOGOUT_FAILED) {
	    updateViewHidingProgressBar(resultCode);
	    setResult(RESULT_CANCELED);
	} else if(resultCode == ProviderAPI.CORRECTLY_DOWNLOADED_CERTIFICATE) {
	    updateViewHidingProgressBar(resultCode);
	    eip_fragment.updateEipService();
	    setResult(RESULT_OK);
	} else if(resultCode == ProviderAPI.INCORRECTLY_DOWNLOADED_CERTIFICATE) {
	    updateViewHidingProgressBar(resultCode);
	    setResult(RESULT_CANCELED);
	}
	else if(resultCode == ProviderAPI.CORRECTLY_DOWNLOADED_EIP_SERVICE) {
        eip_fragment.updateEipService();
	    setResult(RESULT_OK);
	} else if(resultCode == ProviderAPI.INCORRECTLY_DOWNLOADED_EIP_SERVICE) {
	    setResult(RESULT_CANCELED);
	}
    }

    private void updateViewHidingProgressBar(int resultCode) {
	changeStatusMessage(resultCode);
	hideProgressBar();
	invalidateOptionsMenu();
    }

    private void changeStatusMessage(final int previous_result_code) {
	ResultReceiver status_receiver = new ResultReceiver(new Handler()){
		protected void onReceiveResult(int resultCode, Bundle resultData){
		    super.onReceiveResult(resultCode, resultData);
		    String request = resultData.getString(Constants.REQUEST_TAG);
		    if (request.equalsIgnoreCase(Constants.ACTION_IS_EIP_RUNNING)){
			if (resultCode == Activity.RESULT_OK){
			    switch(previous_result_code){
			    case ProviderAPI.SUCCESSFUL_LOGIN: setStatusMessage(R.string.succesful_authentication_message); break;
			    case ProviderAPI.FAILED_LOGIN: setStatusMessage(R.string.authentication_failed_message); break;
			    case ProviderAPI.INCORRECTLY_DOWNLOADED_CERTIFICATE: setStatusMessage(R.string.incorrectly_downloaded_certificate_message); break;
			    case ProviderAPI.SUCCESSFUL_LOGOUT: setStatusMessage(R.string.logged_out_message); break;
			    case ProviderAPI.LOGOUT_FAILED: setStatusMessage(R.string.log_out_failed_message); break;
						
			    }	
			}
			else if(resultCode == Activity.RESULT_CANCELED){
			    switch(previous_result_code){
			    case ProviderAPI.SUCCESSFUL_LOGIN: setStatusMessage(R.string.succesful_authentication_message); break;
			    case ProviderAPI.FAILED_LOGIN: setStatusMessage(R.string.authentication_failed_message); break;
			    case ProviderAPI.FAILED_SIGNUP: setStatusMessage(R.string.registration_failed_message); break;
			    case ProviderAPI.CORRECTLY_DOWNLOADED_CERTIFICATE: break;
			    case ProviderAPI.INCORRECTLY_DOWNLOADED_CERTIFICATE: setStatusMessage(R.string.incorrectly_downloaded_certificate_message); break;
			    case ProviderAPI.SUCCESSFUL_LOGOUT: setStatusMessage(R.string.logged_out_message); break;
			    case ProviderAPI.LOGOUT_FAILED: setStatusMessage(R.string.log_out_failed_message); break;			
			    }
			}
		    }
					
		}
	    };
	eipIsRunning(status_receiver);		
    }

    private void setStatusMessage(int string_resId) {
	if(eip_fragment != null && eip_fragment.status_message != null)
	    eip_fragment.status_message.setText(string_resId);
    }

    private void eipIsRunning(ResultReceiver eip_receiver){
	// TODO validate "action"...how do we get the list of intent-filters for a class via Android API?
	Intent intent = new Intent(this, EIP.class);
	intent.setAction(Constants.ACTION_IS_EIP_RUNNING);
	intent.putExtra(Constants.RECEIVER_TAG, eip_receiver);
	startService(intent);
    }

    private void hideProgressBar() {
        if(eip_fragment != null) {
            eip_fragment.progress_bar.setProgress(0);
            eip_fragment.progress_bar.setVisibility(ProgressBar.GONE);
        }
    }

    public static Context getAppContext() {
	return app;
    }
    
    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        intent.putExtra(Dashboard.REQUEST_CODE, requestCode);
        super.startActivityForResult(intent, requestCode);
    }
}
