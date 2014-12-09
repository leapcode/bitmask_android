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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import de.blinkt.openvpn.activities.LogWindow;
import se.leap.bitmaskclient.eip.Constants;
import se.leap.bitmaskclient.eip.EIP;
import se.leap.bitmaskclient.eip.EipStatus;

/**
 * The main user facing Activity of LEAP Android, consisting of status, controls,
 * and access to preferences.
 * 
 * @author Sean Leonard <meanderingcode@aetherislands.net>
 * @author parmegv
 */
public class Dashboard extends Activity implements LogInDialog.LogInDialogInterface, SignUpDialog.SignUpDialogInterface, ProviderAPIResultReceiver.Receiver {

	protected static final int CONFIGURE_LEAP = 0;
	protected static final int SWITCH_PROVIDER = 1;

    final public static String TAG = Dashboard.class.getSimpleName();
    final public static String SHARED_PREFERENCES = "LEAPPreferences";
    final public static String ACTION_QUIT = "quit";
    public static final String REQUEST_CODE = "request_code";
    public static final String PARAMETERS = "dashboard parameters";
    public static final String START_ON_BOOT = "dashboard start on boot";
    final public static String ON_BOOT = "dashboard on boot";
    public static final String APP_VERSION = "bitmask version";

    private static Context app;
    protected static SharedPreferences preferences;
    private FragmentManagerEnhanced fragment_manager;

    private ProgressBar mProgressBar;
    private TextView status_message;
    public ProviderAPIResultReceiver providerAPI_result_receiver;

    private Provider provider;
    private static boolean authed_eip;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		app = this;		
		
		PRNGFixes.apply();

	    mProgressBar = (ProgressBar) findViewById(R.id.eipProgress);
	    
	    preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
	    fragment_manager = new FragmentManagerEnhanced(getFragmentManager());
	    handleVersion();

	    if (provider == null)
		startActivityForResult(new Intent(this,ConfigurationWizard.class),CONFIGURE_LEAP);
	    else
		buildDashboard(getIntent().getBooleanExtra(ON_BOOT, false));
	}

    private void handleVersion() {
	try {
	    int versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
	    int lastDetectedVersion = preferences.getInt(APP_VERSION, 0);
	    preferences.edit().putInt(APP_VERSION, versionCode).apply();
	    Log.d("Dashboard", "detected version code: " + versionCode);
	    Log.d("Dashboard", "last detected version code: " + lastDetectedVersion);

	    switch(versionCode) {
	    case 91: // 0.6.0 without Bug #5999
	    case 101: // 0.8.0
		if(!preferences.getString(Constants.KEY, "").isEmpty()) {
		    Intent rebuildVpnProfiles = new Intent(getApplicationContext(), EIP.class);
		    rebuildVpnProfiles.setAction(Constants.ACTION_UPDATE_EIP_SERVICE);
		    startService(rebuildVpnProfiles);
		}
		break;
	    }
	} catch (NameNotFoundException e) {
        Log.d(TAG, "Handle version didn't find any " + getPackageName() + " package");
	}
    }

	@Override
	protected void onDestroy() {
	    
		super.onDestroy();
	}

    protected void onPause() {
	super.onPause();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
	Log.d(TAG, "onActivityResult: requestCode = " + requestCode);
	if ( requestCode == CONFIGURE_LEAP || requestCode == SWITCH_PROVIDER) {
	    // It should be equivalent: if ( (requestCode == CONFIGURE_LEAP) || (data!= null && data.hasExtra(STOP_FIRST))) {
	    if ( resultCode == RESULT_OK ) {
            preferences.edit().putInt(Constants.PARSED_SERIAL, 0).apply();
            preferences.edit().putBoolean(Constants.AUTHED_EIP, authed_eip).apply();
            updateEipService();

            if (data.hasExtra(Provider.KEY))
                provider = data.getParcelableExtra(Provider.KEY);
            buildDashboard(false);
            invalidateOptionsMenu();
            if (data.hasExtra(LogInDialog.TAG)) {
                logInDialog(Bundle.EMPTY);
            } else if (resultCode == RESULT_CANCELED && data.hasExtra(ACTION_QUIT)) {
                finish();
            }
        } else
		configErrorDialog();
	} else if(requestCode == EIP.DISCONNECT) {
	    EipStatus.getInstance().setConnectedOrDisconnected();
	}
    }

	/**
	 * Dialog shown when encountering a configuration error.  Such errors require
	 * reconfiguring LEAP or aborting the application.
	 */
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
				    preferences.edit().remove(Provider.KEY).apply();
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
		setContentView(R.layout.client_dashboard);
	    
		TextView providerNameTV = (TextView) findViewById(R.id.providerName);
		providerNameTV.setText(provider.getDomain());
		providerNameTV.setTextSize(28);
		
		mProgressBar = (ProgressBar) findViewById(R.id.eipProgress);

		if ( provider.hasEIP()){

            EipServiceFragment eip_fragment = (EipServiceFragment) fragment_manager.findFragmentByTag(EipServiceFragment.TAG);
            if(eip_fragment == null)
                eip_fragment = new EipServiceFragment();

		    if (hide_and_turn_on_eip) {
			preferences.edit().remove(Dashboard.START_ON_BOOT).apply();
			Bundle arguments = new Bundle();
			arguments.putBoolean(EipServiceFragment.START_ON_BOOT, true);
			eip_fragment.setArguments(arguments);
		    }

            fragment_manager.replace(R.id.servicesCollection, eip_fragment, EipServiceFragment.TAG);

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
		boolean authed_eip = !LeapSRPSession.getToken().isEmpty();
		boolean allow_registered_eip = service_description.getBoolean(Provider.ALLOW_REGISTRATION);
		preferences.edit().putBoolean(Constants.ALLOWED_REGISTERED, allow_registered_eip).apply();
		
		if(allow_registered_eip) {
		    if(authed_eip) {
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
			if (provider.hasEIP()){
				if (preferences.getBoolean(Constants.AUTHED_EIP, false)){
					logOut();
				}				
				eipStop();
			}
			preferences.edit().clear().apply();
			startActivityForResult(new Intent(this,ConfigurationWizard.class), SWITCH_PROVIDER);
			return true;
		case R.id.login_button:
			logInDialog(Bundle.EMPTY);
			return true;
		case R.id.logout_button:
			logOut();
			return true;
		case R.id.signup_button:
			signUpDialog(Bundle.EMPTY);
			return true;
		default:
				return super.onOptionsItemSelected(item);
		}
		
	}

    private Intent prepareProviderAPICommand() {	    
	mProgressBar = (ProgressBar) findViewById(R.id.eipProgress);
	status_message = (TextView) findViewById(R.id.status_message);
		
	providerAPI_result_receiver = new ProviderAPIResultReceiver(new Handler());
	providerAPI_result_receiver.setReceiver(this);
		
	Intent command = new Intent(this, ProviderAPI.class);

	command.putExtra(ProviderAPI.RECEIVER_KEY, providerAPI_result_receiver);
	return command;
    }
    
    /**
     * Shows the log in dialog.
     */
    public void logInDialog(Bundle resultData) {
	FragmentTransaction transaction = fragment_manager.removePreviousFragment(LogInDialog.TAG);

	DialogFragment newFragment = LogInDialog.newInstance();
	if(resultData != null && !resultData.isEmpty())
	    newFragment.setArguments(resultData);
	newFragment.show(transaction, LogInDialog.TAG);
    }

    @Override
    public void logIn(String username, String password) {
	Intent provider_API_command = prepareProviderAPICommand();
	Bundle parameters = provider_API_command.getExtras().getBundle(ProviderAPI.PARAMETERS);
	if(parameters == null)
	    parameters = new Bundle();
	    
	parameters.putString(SessionDialogInterface.USERNAME, username);
	parameters.putString(SessionDialogInterface.PASSWORD, password);

	mProgressBar.setVisibility(ProgressBar.VISIBLE);
	status_message.setText(R.string.authenticating_message);

	provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);
	provider_API_command.setAction(ProviderAPI.SRP_AUTH);
	startService(provider_API_command);
    }

    public void cancelLoginOrSignup() {
      EipStatus.getInstance().setConnectedOrDisconnected();
    }
	
    /**
     * Asks ProviderAPI to log out.
     */
    public void logOut() {
	Intent provider_API_command = prepareProviderAPICommand();
	    
	if(mProgressBar == null) mProgressBar = (ProgressBar) findViewById(R.id.eipProgress);
	mProgressBar.setVisibility(ProgressBar.VISIBLE);
	if(status_message == null) status_message = (TextView) findViewById(R.id.status_message);
	status_message.setText(R.string.logout_message);
		
	provider_API_command.setAction(ProviderAPI.LOG_OUT);
	startService(provider_API_command);
    }
    
    /**
     * Shows the sign up dialog.
     */
    public void signUpDialog(Bundle resultData) {
	FragmentTransaction transaction = fragment_manager.removePreviousFragment(SignUpDialog.TAG);

	DialogFragment newFragment = SignUpDialog.newInstance();
	if(resultData != null && !resultData.isEmpty()) {
	    newFragment.setArguments(resultData);
	}
	newFragment.show(transaction, SignUpDialog.TAG);
    }

    @Override
    public void signUp(String username, String password) {
	Intent provider_API_command = prepareProviderAPICommand();
	Bundle parameters = provider_API_command.getExtras().getBundle(ProviderAPI.PARAMETERS);
	if(parameters == null)
	    parameters = new Bundle();
	    
	parameters.putString(SessionDialogInterface.USERNAME, username);
	parameters.putString(SessionDialogInterface.PASSWORD, password);
	
	mProgressBar.setVisibility(ProgressBar.VISIBLE);
	status_message.setText(R.string.signingup_message);
	
	provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);
	provider_API_command.setAction(ProviderAPI.SRP_REGISTER);
	startService(provider_API_command);
    }

	/**
	 * Asks ProviderAPI to download an authenticated OpenVPN certificate.
	 */
	private void downloadAuthedUserCertificate() {
		providerAPI_result_receiver = new ProviderAPIResultReceiver(new Handler());
		providerAPI_result_receiver.setReceiver(this);
		
		Intent provider_API_command = new Intent(this, ProviderAPI.class);

		Bundle parameters = new Bundle();
		parameters.putString(ConfigurationWizard.TYPE_OF_CERTIFICATE, ConfigurationWizard.AUTHED_CERTIFICATE);

		provider_API_command.setAction(ProviderAPI.DOWNLOAD_CERTIFICATE);
		provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);
		provider_API_command.putExtra(ProviderAPI.RECEIVER_KEY, providerAPI_result_receiver);
		
		startService(provider_API_command);
	}

	@Override
	public void onReceiveResult(int resultCode, Bundle resultData) {
	    Log.d(TAG, "onReceiveResult");
	    if(resultCode == ProviderAPI.SRP_REGISTRATION_SUCCESSFUL) {
		String username = resultData.getString(SessionDialogInterface.USERNAME);
		String password = resultData.getString(SessionDialogInterface.PASSWORD);
		logIn(username, password);
	    } else if(resultCode == ProviderAPI.SRP_REGISTRATION_FAILED) {
		changeStatusMessage(resultCode);
		hideProgressBar();
		
		signUpDialog(resultData);
	    } else if(resultCode == ProviderAPI.SRP_AUTHENTICATION_SUCCESSFUL) {
		changeStatusMessage(resultCode);
		hideProgressBar();
		
		invalidateOptionsMenu();
		
		authed_eip = true;
		preferences.edit().putBoolean(Constants.AUTHED_EIP, authed_eip).apply();

        	downloadAuthedUserCertificate();
	    } else if(resultCode == ProviderAPI.SRP_AUTHENTICATION_FAILED) {
		changeStatusMessage(resultCode);
		hideProgressBar();
		
		logInDialog(resultData);
	    } else if(resultCode == ProviderAPI.LOGOUT_SUCCESSFUL) {
		changeStatusMessage(resultCode);
		hideProgressBar();
		
		invalidateOptionsMenu();
		
		authed_eip = false;
		preferences.edit().putBoolean(Constants.AUTHED_EIP, authed_eip).apply();

	    } else if(resultCode == ProviderAPI.LOGOUT_FAILED) {
		changeStatusMessage(resultCode);
		hideProgressBar();
		
		setResult(RESULT_CANCELED);
	    } else if(resultCode == ProviderAPI.CORRECTLY_DOWNLOADED_CERTIFICATE) {
    		changeStatusMessage(resultCode);
		hideProgressBar();
		
        	setResult(RESULT_OK);
		
		updateEipService();
	    } else if(resultCode == ProviderAPI.INCORRECTLY_DOWNLOADED_CERTIFICATE) {
    		changeStatusMessage(resultCode);
		hideProgressBar();
        	setResult(RESULT_CANCELED);
	    }
	}

    private void updateEipService() {
	Intent updateEIP = new Intent(getApplicationContext(), EIP.class);
	updateEIP.setAction(Constants.ACTION_UPDATE_EIP_SERVICE);
	ResultReceiver receiver = new ResultReceiver(new Handler()) {
		protected void onReceiveResult(int resultCode, Bundle resultData) {
		    String request = resultData.getString(Constants.REQUEST_TAG);
		    if(request.equalsIgnoreCase(Constants.ACTION_UPDATE_EIP_SERVICE)) {
			if(resultCode == Activity.RESULT_OK) {
			    if(authed_eip)
				eipStart();
			}
		    }
		}
	    };
	updateEIP.putExtra(Constants.RECEIVER_TAG, receiver);
	startService(updateEIP);
    }

	private void changeStatusMessage(final int previous_result_code) {
		// TODO Auto-generated method stub
		ResultReceiver eip_status_receiver = new ResultReceiver(new Handler()){
			protected void onReceiveResult(int resultCode, Bundle resultData){
				super.onReceiveResult(resultCode, resultData);
				String request = resultData.getString(Constants.REQUEST_TAG);
				if(status_message == null) status_message = (TextView) findViewById(R.id.status_message);
				if (request.equalsIgnoreCase(Constants.ACTION_IS_EIP_RUNNING)){					
					if (resultCode == Activity.RESULT_OK){

						switch(previous_result_code){
						case ProviderAPI.SRP_AUTHENTICATION_SUCCESSFUL: status_message.setText(R.string.succesful_authentication_message); break;
						case ProviderAPI.SRP_AUTHENTICATION_FAILED: status_message.setText(R.string.authentication_failed_message); break;
						case ProviderAPI.CORRECTLY_DOWNLOADED_CERTIFICATE: status_message.setText(R.string.authed_secured_status); break;
						case ProviderAPI.INCORRECTLY_DOWNLOADED_CERTIFICATE: status_message.setText(R.string.incorrectly_downloaded_certificate_message); break;
						case ProviderAPI.LOGOUT_SUCCESSFUL: status_message.setText(R.string.logged_out_message); break;
						case ProviderAPI.LOGOUT_FAILED: status_message.setText(R.string.log_out_failed_message); break;
						
						}	
					}
					else if(resultCode == Activity.RESULT_CANCELED){

						switch(previous_result_code){

						case ProviderAPI.SRP_AUTHENTICATION_SUCCESSFUL: status_message.setText(R.string.succesful_authentication_message); break;
						case ProviderAPI.SRP_AUTHENTICATION_FAILED: status_message.setText(R.string.authentication_failed_message); break;
						case ProviderAPI.SRP_REGISTRATION_FAILED: status_message.setText(R.string.registration_failed_message); break;
						case ProviderAPI.CORRECTLY_DOWNLOADED_CERTIFICATE: break;
						case ProviderAPI.INCORRECTLY_DOWNLOADED_CERTIFICATE: status_message.setText(R.string.incorrectly_downloaded_certificate_message); break;
						case ProviderAPI.LOGOUT_SUCCESSFUL: status_message.setText(R.string.logged_out_message); break;
						case ProviderAPI.LOGOUT_FAILED: status_message.setText(R.string.log_out_failed_message); break;			
						}
					}
				}
					
			}
		};
		eipIsRunning(eip_status_receiver);		
	}

    private void hideProgressBar() {
	if(mProgressBar == null)
	    mProgressBar = (ProgressBar) findViewById(R.id.eipProgress);

	mProgressBar.setProgress(0);
	mProgressBar.setVisibility(ProgressBar.GONE);
    }

	/**
	 * For retrieving the base application Context in classes that don't extend
	 * Android's Activity class
	 * 
	 * @return Application Context as defined by <code>this</code> for Dashboard instance
	 */
	public static Context getAppContext() {
		return app;
	}

	
    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        intent.putExtra(Dashboard.REQUEST_CODE, requestCode);
        super.startActivityForResult(intent, requestCode);
    }

	private void eipIsRunning(ResultReceiver eip_receiver){
		// TODO validate "action"...how do we get the list of intent-filters for a class via Android API?
		Intent eip_intent = new Intent(this, EIP.class);
		eip_intent.setAction(Constants.ACTION_IS_EIP_RUNNING);
		eip_intent.putExtra(Constants.RECEIVER_TAG, eip_receiver);
		startService(eip_intent);
	}
	
    private void eipStop(){
	EipServiceFragment eipFragment = (EipServiceFragment) getFragmentManager().findFragmentByTag(EipServiceFragment.TAG);
	eipFragment.stopEIP();
    }

    private void eipStart() {
	EipServiceFragment eipFragment = (EipServiceFragment) getFragmentManager().findFragmentByTag(EipServiceFragment.TAG);
	eipFragment.startEipFromScratch();
    }

    protected void showProgressBar() {
	if(mProgressBar == null)
	    mProgressBar = (ProgressBar) findViewById(R.id.eipProgress);	    
	mProgressBar.setVisibility(ProgressBar.VISIBLE);
    }
}
