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

import org.json.JSONException;
import org.json.JSONObject;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.ProviderAPIResultReceiver.Receiver;
import se.leap.bitmaskclient.SignUpDialog;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The main user facing Activity of LEAP Android, consisting of status, controls,
 * and access to preferences.
 * 
 * @author Sean Leonard <meanderingcode@aetherislands.net>
 * @author parmegv
 */
public class Dashboard extends Activity implements LogInDialog.LogInDialogInterface, SignUpDialog.SignUpDialogInterface, Receiver {

	protected static final int CONFIGURE_LEAP = 0;
	protected static final int SWITCH_PROVIDER = 1;

    final public static String SHARED_PREFERENCES = "LEAPPreferences";
    final public static String ACTION_QUIT = "quit";
    public static final String REQUEST_CODE = "request_code";
    public static final String PARAMETERS = "dashboard parameters";
    public static final String START_ON_BOOT = "dashboard start on boot";
    final public static String ON_BOOT = "dashboard on boot";

	
	private ProgressBar mProgressBar;
	private TextView eipStatus;
	private static Context app;
	private static SharedPreferences preferences;
	private static Provider provider;

	private TextView providerNameTV;

	private boolean authed_eip = false;

    public ProviderAPIResultReceiver providerAPI_result_receiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		app = this;
		
		PRNGFixes.apply();
	//	mProgressBar = (ProgressBar) findViewById(R.id.progressbar_dashboard);
	//    mProgressBar = (ProgressBar) findViewById(R.id.eipProgress);
	//	eipStatus = (TextView) findViewById(R.id.eipStatus);

	    mProgressBar = (ProgressBar) findViewById(R.id.eipProgress);
	    
	    preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);

	    authed_eip = preferences.getBoolean(EIP.AUTHED_EIP, false);
		if (preferences.getString(Provider.KEY, "").isEmpty())
			startActivityForResult(new Intent(this,ConfigurationWizard.class),CONFIGURE_LEAP);
		else
		    buildDashboard(getIntent().getBooleanExtra(ON_BOOT, false));
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
		if ( requestCode == CONFIGURE_LEAP || requestCode == SWITCH_PROVIDER) {
		// It should be equivalent: if ( (requestCode == CONFIGURE_LEAP) || (data!= null && data.hasExtra(STOP_FIRST))) {
			if ( resultCode == RESULT_OK ){
				getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE).edit().putInt(EIP.PARSED_SERIAL, 0).commit();
				getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE).edit().putBoolean(EIP.AUTHED_EIP, authed_eip).commit();
				Intent updateEIP = new Intent(getApplicationContext(), EIP.class);
				updateEIP.setAction(EIP.ACTION_UPDATE_EIP_SERVICE);
				startService(updateEIP);
				buildDashboard(false);
				invalidateOptionsMenu();
				if(data != null && data.hasExtra(LogInDialog.VERB)) {
					View view = ((ViewGroup)findViewById(android.R.id.content)).getChildAt(0);
					logInDialog(view, Bundle.EMPTY);
				}
			} else if(resultCode == RESULT_CANCELED && (data == null || data.hasExtra(ACTION_QUIT))) {
				finish();
			} else
				configErrorDialog();
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
					SharedPreferences.Editor prefsEdit = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE).edit();
					prefsEdit.remove(Provider.KEY).commit();
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
		provider = Provider.getInstance();
		provider.init( this );

		setContentView(R.layout.client_dashboard);
	    
		providerNameTV = (TextView) findViewById(R.id.providerName);
		providerNameTV.setText(provider.getDomain());
		providerNameTV.setTextSize(28);
		
	    mProgressBar = (ProgressBar) findViewById(R.id.eipProgress);

		FragmentManager fragMan = getFragmentManager();
		if ( provider.hasEIP()){
			EipServiceFragment eipFragment = new EipServiceFragment();
			if (hide_and_turn_on_eip) {		    
			    getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE).edit().remove(Dashboard.START_ON_BOOT).commit();			
			    Bundle arguments = new Bundle();
			    arguments.putBoolean(EipServiceFragment.START_ON_BOOT, true);
			    eipFragment.setArguments(arguments);
			}
			fragMan.beginTransaction().replace(R.id.servicesCollection, eipFragment, EipServiceFragment.TAG).commit();

			if (hide_and_turn_on_eip) {
			    onBackPressed();
			}
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		JSONObject provider_json;
		try {
			provider_json = new JSONObject(getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE).getString(Provider.KEY, ""));
			JSONObject service_description = provider_json.getJSONObject(Provider.SERVICE);
			
			if(service_description.getBoolean(Provider.ALLOW_REGISTRATION)) {
				if(authed_eip) {
					menu.findItem(R.id.login_button).setVisible(false);
					menu.findItem(R.id.logout_button).setVisible(true);
				} else {
					menu.findItem(R.id.login_button).setVisible(true);
					menu.findItem(R.id.logout_button).setVisible(false);
				}
				menu.findItem(R.id.signup_button).setVisible(true);
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
		case R.id.switch_provider:
			if (Provider.getInstance().hasEIP()){
				if (getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE).getBoolean(EIP.AUTHED_EIP, false)){
					logOut();
				}
				eipStop();
			}
			getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE).edit().remove(Provider.KEY).commit();
			startActivityForResult(new Intent(this,ConfigurationWizard.class), SWITCH_PROVIDER);
			return true;
		case R.id.login_button:
			View view = ((ViewGroup)findViewById(android.R.id.content)).getChildAt(0);
			logInDialog(view, Bundle.EMPTY);
			return true;
		case R.id.logout_button:
			logOut();
			return true;
		case R.id.signup_button:
			signUpDialog(((ViewGroup)findViewById(android.R.id.content)).getChildAt(0), Bundle.EMPTY);
			return true;
		default:
				return super.onOptionsItemSelected(item);
		}
		
	}

	@Override
	public void authenticate(String username, String password) {
	    mProgressBar = (ProgressBar) findViewById(R.id.eipProgress);
		eipStatus = (TextView) findViewById(R.id.eipStatus);
		
		providerAPI_result_receiver = new ProviderAPIResultReceiver(new Handler());
		providerAPI_result_receiver.setReceiver(this);
		
		Intent provider_API_command = new Intent(this, ProviderAPI.class);

		Bundle parameters = new Bundle();
		parameters.putString(LogInDialog.USERNAME, username);
		parameters.putString(LogInDialog.PASSWORD, password);

		JSONObject provider_json;
		try {
			provider_json = new JSONObject(preferences.getString(Provider.KEY, ""));
			parameters.putString(Provider.API_URL, provider_json.getString(Provider.API_URL) + "/" + provider_json.getString(Provider.API_VERSION));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		provider_API_command.setAction(ProviderAPI.SRP_AUTH);
		provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);
		provider_API_command.putExtra(ProviderAPI.RECEIVER_KEY, providerAPI_result_receiver);
		
		mProgressBar.setVisibility(ProgressBar.VISIBLE);
		eipStatus.setText(R.string.authenticating_message);
		//mProgressBar.setMax(4);
		startService(provider_API_command);
	}

    public void cancelAuthedEipOn() {
	EipServiceFragment eipFragment = (EipServiceFragment) getFragmentManager().findFragmentByTag(EipServiceFragment.TAG);
	eipFragment.checkEipSwitch(false);
    }
	
	/**
	 * Asks ProviderAPI to log out.
	 */
	public void logOut() {
		providerAPI_result_receiver = new ProviderAPIResultReceiver(new Handler());
		providerAPI_result_receiver.setReceiver(this);
		Intent provider_API_command = new Intent(this, ProviderAPI.class);

		Bundle parameters = new Bundle();

		JSONObject provider_json;
		try {
			provider_json = new JSONObject(preferences.getString(Provider.KEY, ""));
			parameters.putString(Provider.API_URL, provider_json.getString(Provider.API_URL) + "/" + provider_json.getString(Provider.API_VERSION));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		provider_API_command.setAction(ProviderAPI.LOG_OUT);
		provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);
		provider_API_command.putExtra(ProviderAPI.RECEIVER_KEY, providerAPI_result_receiver);
		
		if(mProgressBar == null) mProgressBar = (ProgressBar) findViewById(R.id.eipProgress);
		mProgressBar.setVisibility(ProgressBar.VISIBLE);
		if(eipStatus == null) eipStatus = (TextView) findViewById(R.id.eipStatus);
		eipStatus.setText(R.string.logout_message);
	//	eipStatus.setText("Starting to logout");
		
		startService(provider_API_command);
		//mProgressBar.setMax(1);

	}
	
	/**
	 * Shows the log in dialog.
	 * @param view from which the dialog is created.
	 */
	public void logInDialog(View view, Bundle resultData) {
		FragmentTransaction fragment_transaction = getFragmentManager().beginTransaction();
	    Fragment previous_log_in_dialog = getFragmentManager().findFragmentByTag(LogInDialog.TAG);
	    if (previous_log_in_dialog != null) {
	        fragment_transaction.remove(previous_log_in_dialog);
	    }
	    fragment_transaction.addToBackStack(null);

	    DialogFragment newFragment = LogInDialog.newInstance();
	    if(resultData != null && !resultData.isEmpty()) {
	    	newFragment.setArguments(resultData);
	    }
	    newFragment.show(fragment_transaction, LogInDialog.TAG);
	}

    	@Override
	public void signUp(String username, String password) {
	    mProgressBar = (ProgressBar) findViewById(R.id.eipProgress);
	    eipStatus = (TextView) findViewById(R.id.eipStatus);
		
	    providerAPI_result_receiver = new ProviderAPIResultReceiver(new Handler());
	    providerAPI_result_receiver.setReceiver(this);
		
	    Intent provider_API_command = new Intent(this, ProviderAPI.class);

	    Bundle parameters = new Bundle();
	    parameters.putString(SignUpDialog.USERNAME, username);
	    parameters.putString(SignUpDialog.PASSWORD, password);

	    JSONObject provider_json;
	    try {
		provider_json = new JSONObject(preferences.getString(Provider.KEY, ""));
		parameters.putString(Provider.API_URL, provider_json.getString(Provider.API_URL) + "/" + provider_json.getString(Provider.API_VERSION));
	    } catch (JSONException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }

	    provider_API_command.setAction(ProviderAPI.SRP_REGISTER);
	    provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);
	    provider_API_command.putExtra(ProviderAPI.RECEIVER_KEY, providerAPI_result_receiver);
		
	    mProgressBar.setVisibility(ProgressBar.VISIBLE);
	    eipStatus.setText(R.string.authenticating_message);
	    //mProgressBar.setMax(4);
	    startService(provider_API_command);
	}
    
	/**
	 * Shows the sign up dialog.
	 * @param view from which the dialog is created.
	 */
	public void signUpDialog(View view, Bundle resultData) {
	    FragmentTransaction fragment_transaction = getFragmentManager().beginTransaction();
	    Fragment previous_sign_up_dialog = getFragmentManager().findFragmentByTag(SignUpDialog.TAG);
	    if (previous_sign_up_dialog != null) {
	        fragment_transaction.remove(previous_sign_up_dialog);
	    }
	    fragment_transaction.addToBackStack(null);

	    DialogFragment newFragment = SignUpDialog.newInstance();
	    if(resultData != null && !resultData.isEmpty()) {
	    	newFragment.setArguments(resultData);
	    }
	    newFragment.show(fragment_transaction, SignUpDialog.TAG);
	}

	/**
	 * Asks ProviderAPI to download an authenticated OpenVPN certificate.
	 * @param session_id cookie for the server to allow us to download the certificate.
	 */
	private void downloadAuthedUserCertificate(/*Cookie session_id*/) {
		providerAPI_result_receiver = new ProviderAPIResultReceiver(new Handler());
		providerAPI_result_receiver.setReceiver(this);
		
		Intent provider_API_command = new Intent(this, ProviderAPI.class);

		Bundle parameters = new Bundle();
		parameters.putString(ConfigurationWizard.TYPE_OF_CERTIFICATE, ConfigurationWizard.AUTHED_CERTIFICATE);
		/*parameters.putString(ConfigHelper.SESSION_ID_COOKIE_KEY, session_id.getName());
		parameters.putString(ConfigHelper.SESSION_ID_KEY, session_id.getValue());*/

		provider_API_command.setAction(ProviderAPI.DOWNLOAD_CERTIFICATE);
		provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);
		provider_API_command.putExtra(ProviderAPI.RECEIVER_KEY, providerAPI_result_receiver);
		
		startService(provider_API_command);
	}

	@Override
	public void onReceiveResult(int resultCode, Bundle resultData) {
		if(resultCode == ProviderAPI.SRP_AUTHENTICATION_SUCCESSFUL){
			String session_id_cookie_key = resultData.getString(ProviderAPI.SESSION_ID_COOKIE_KEY);
			String session_id_string = resultData.getString(ProviderAPI.SESSION_ID_KEY);
			setResult(RESULT_OK);

			authed_eip = true;
			getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE).edit().putBoolean(EIP.AUTHED_EIP, authed_eip).commit();

			invalidateOptionsMenu();
        	mProgressBar.setVisibility(ProgressBar.GONE);
    		changeStatusMessage(resultCode);

        	//Cookie session_id = new BasicClientCookie(session_id_cookie_key, session_id_string);
        	downloadAuthedUserCertificate(/*session_id*/);
		} else if(resultCode == ProviderAPI.SRP_AUTHENTICATION_FAILED) {
        	logInDialog(getCurrentFocus(), resultData);
		} else if(resultCode == ProviderAPI.LOGOUT_SUCCESSFUL) {
			authed_eip = false;
			getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE).edit().putBoolean(EIP.AUTHED_EIP, authed_eip).commit();
			mProgressBar.setVisibility(ProgressBar.GONE);
			mProgressBar.setProgress(0);
			invalidateOptionsMenu();
			setResult(RESULT_OK);
			changeStatusMessage(resultCode);

		} else if(resultCode == ProviderAPI.LOGOUT_FAILED) {
			setResult(RESULT_CANCELED);
			changeStatusMessage(resultCode);
        	mProgressBar.setVisibility(ProgressBar.GONE);
		} else if(resultCode == ProviderAPI.CORRECTLY_DOWNLOADED_CERTIFICATE) {
        	setResult(RESULT_OK);
    		changeStatusMessage(resultCode);
        	mProgressBar.setVisibility(ProgressBar.GONE);
		if(EipServiceFragment.isEipSwitchChecked())
		    eipStart();
		} else if(resultCode == ProviderAPI.INCORRECTLY_DOWNLOADED_CERTIFICATE) {
        	setResult(RESULT_CANCELED);
    		changeStatusMessage(resultCode);
        	mProgressBar.setVisibility(ProgressBar.GONE);
 		}
	}

	private void changeStatusMessage(final int previous_result_code) {
		// TODO Auto-generated method stub
		ResultReceiver eip_status_receiver = new ResultReceiver(new Handler()){
			protected void onReceiveResult(int resultCode, Bundle resultData){
				super.onReceiveResult(resultCode, resultData);
				String request = resultData.getString(EIP.REQUEST_TAG);
				if (request.equalsIgnoreCase(EIP.ACTION_IS_EIP_RUNNING)){					
					if (resultCode == Activity.RESULT_OK){

						switch(previous_result_code){
						case ProviderAPI.SRP_AUTHENTICATION_SUCCESSFUL: eipStatus.setText(R.string.succesful_authentication_message); break;
						case ProviderAPI.SRP_AUTHENTICATION_FAILED: eipStatus.setText(R.string.authentication_failed_message); break;
						case ProviderAPI.CORRECTLY_DOWNLOADED_CERTIFICATE: eipStatus.setText(R.string.authed_secured_status); break;
						case ProviderAPI.INCORRECTLY_DOWNLOADED_CERTIFICATE: eipStatus.setText(R.string.incorrectly_downloaded_certificate_message); break;
						case ProviderAPI.LOGOUT_SUCCESSFUL: eipStatus.setText(R.string.anonymous_secured_status); break;
						case ProviderAPI.LOGOUT_FAILED: eipStatus.setText(R.string.log_out_failed_message); break;
						
						}	
					}
					else if(resultCode == Activity.RESULT_CANCELED){

						switch(previous_result_code){

						case ProviderAPI.SRP_AUTHENTICATION_SUCCESSFUL: eipStatus.setText(R.string.succesful_authentication_message); break;
						case ProviderAPI.SRP_AUTHENTICATION_FAILED: eipStatus.setText(R.string.authentication_failed_message); break;
						case ProviderAPI.CORRECTLY_DOWNLOADED_CERTIFICATE: eipStatus.setText(R.string.future_authed_secured_status); break;
						case ProviderAPI.INCORRECTLY_DOWNLOADED_CERTIFICATE: eipStatus.setText(R.string.incorrectly_downloaded_certificate_message); break;
						case ProviderAPI.LOGOUT_SUCCESSFUL: eipStatus.setText(R.string.future_anonymous_secured_status); break;
						case ProviderAPI.LOGOUT_FAILED: eipStatus.setText(R.string.log_out_failed_message); break;			
						}
					}
				}
					
			}
		};
		eipIsRunning(eip_status_receiver);		
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
	/**
	 * Send a command to EIP
	 * 
	 * @param action	A valid String constant from EIP class representing an Intent
	 * 					filter for the EIP class 
	 */
	private void eipIsRunning(ResultReceiver eip_receiver){
		// TODO validate "action"...how do we get the list of intent-filters for a class via Android API?
		Intent eip_intent = new Intent(this, EIP.class);
		eip_intent.setAction(EIP.ACTION_IS_EIP_RUNNING);
		eip_intent.putExtra(EIP.RECEIVER_TAG, eip_receiver);
		startService(eip_intent);
	}
	
	/**
	 * Send a command to EIP
	 * 	
	 */
	private void eipStop(){
		// TODO validate "action"...how do we get the list of intent-filters for a class via Android API?
		Intent eip_intent = new Intent(this, EIP.class);
		eip_intent.setAction(EIP.ACTION_STOP_EIP);
	//	eip_intent.putExtra(EIP.RECEIVER_TAG, eip_receiver);fi
		startService(eip_intent);
		
	}

    private void eipStart(){
	Intent eip_intent = new Intent(this, EIP.class);
	eip_intent.setAction(EIP.ACTION_START_EIP);
	eip_intent.putExtra(EIP.RECEIVER_TAG, EipServiceFragment.getReceiver());
	startService(eip_intent);

    }
}
