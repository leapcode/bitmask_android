package se.leap.leapclient;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONException;
import org.json.JSONObject;

import se.leap.leapclient.ProviderAPIResultReceiver.Receiver;
import se.leap.openvpn.AboutFragment;
import se.leap.openvpn.MainActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class Dashboard extends Activity implements LogInDialog.LogInDialogInterface, Receiver {

	protected static final int CONFIGURE_LEAP = 0;
	
	private static Context app;
	private static SharedPreferences preferences;
	private static Provider provider;

	private TextView providerNameTV;
	private TextView eipTypeTV;

    public ProviderAPIResultReceiver providerAPI_result_receiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		app = this;
		
		setContentView(R.layout.client_dashboard);

		preferences = getSharedPreferences(ConfigHelper.PREFERENCES_KEY,MODE_PRIVATE);
		if(ConfigHelper.shared_preferences == null)
			ConfigHelper.setSharedPreferences(preferences);
		
		// Check if we have preferences, run configuration wizard if not
		// TODO We should do a better check for config that this!
		if (preferences.contains("provider") && preferences.getString(ConfigHelper.PROVIDER_KEY, null) != null)
			buildDashboard();
		else
			startActivityForResult(new Intent(this,ConfigurationWizard.class),CONFIGURE_LEAP);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if ( requestCode == CONFIGURE_LEAP ) {
			if ( resultCode == RESULT_OK ){
				// Configuration done, get our preferences again
				preferences = getSharedPreferences(ConfigHelper.PREFERENCES_KEY,MODE_PRIVATE);
				// Update eip-service local parsing
				startService( new Intent(EIP.ACTION_UPDATE_EIP_SERVICE) );
				
				buildDashboard();
			} else {
				// Something went wrong in configuration
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
							SharedPreferences.Editor prefsEdit = getSharedPreferences(ConfigHelper.PREFERENCES_KEY, MODE_PRIVATE).edit();
							prefsEdit.remove(ConfigHelper.PROVIDER_KEY).commit();
							finish();
						}
					});
			}
		}
	}
	
	private void buildDashboard() {
		// Get our provider
		provider = Provider.getInstance();
		provider.init( this );

		// Set provider name in textview
		providerNameTV = (TextView) findViewById(R.id.providerName);
		providerNameTV.setText(provider.getName());
		providerNameTV.setTextSize(28); // TODO maybe to some calculating, or a marquee?

		if ( provider.hasEIP() /*&& provider.getEIP() != null*/){
			// FIXME let's schedule this, so we're not doing it when we load the app
			startService( new Intent(EIP.ACTION_UPDATE_EIP_SERVICE) );
			serviceItemEIP();
		}
	}

	private void serviceItemEIP() {
		((ViewStub) findViewById(R.id.eipOverviewStub)).inflate();

		// Set our EIP type title
		eipTypeTV = (TextView) findViewById(R.id.eipType);
		eipTypeTV.setText(provider.getEIPType());
		// Show our EIP detail
		View eipDetail = ((RelativeLayout) findViewById(R.id.eipDetail));
		View eipSettings = findViewById(R.id.eipSettings);
		eipSettings.setVisibility(View.GONE); // FIXME too!
		eipDetail.setVisibility(View.VISIBLE);

		// TODO Bind our switch to run our EIP
		// What happens when our VPN stops running?  does it call the listener?
		Switch eipSwitch = (Switch) findViewById(R.id.eipSwitch);
		eipSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					Intent vpnIntent;
					if (isChecked){
						vpnIntent = new Intent(EIP.ACTION_START_EIP);
					} else {
						vpnIntent = new Intent(EIP.ACTION_STOP_EIP);
					}
					startService(vpnIntent);
				}
		});

		//TODO write our info into the view	fragment that will expand with details and a settings button
		// TODO set eip overview subview
		// TODO make eip type clickable, show subview
		// TODO attach vpn status feedback to eip overview view
		// TODO attach settings button to something
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		JSONObject provider_json;
		try {
			provider_json = ConfigHelper.getJsonFromSharedPref(ConfigHelper.PROVIDER_KEY);
			JSONObject service_description = provider_json.getJSONObject(ConfigHelper.SERVICE_KEY);
			if(service_description.getBoolean(ConfigHelper.ALLOW_REGISTRATION_KEY)) {
				menu.findItem(R.id.login_button).setVisible(true);
				menu.findItem(R.id.logout_button).setVisible(true);
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return true;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.client_dashboard, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		Intent intent;
		// Handle item selection
		switch (item.getItemId()){
		case R.id.about_leap:
			// TODO move se.leap.openvpn.AboutFragment into our package
			Fragment aboutFragment = new AboutFragment();
			FragmentTransaction trans = getFragmentManager().beginTransaction();
			trans.replace(R.id.dashboardLayout, aboutFragment);
			trans.addToBackStack(null);
			trans.commit();
			
			//intent = new Intent(this,AboutFragment.class);
			//startActivity(intent);
			return true;
		case R.id.legacy_interface:
			// TODO call se.leap.openvpn.MainActivity
			intent = new Intent(this,MainActivity.class);
			startActivity(intent);
			return true;
		case R.id.login_button:
			View view = ((ViewGroup)findViewById(android.R.id.content)).getChildAt(0);
			logInDialog(view);
			return true;
		case R.id.logout_button:
			logOut();
			return true;
		default:
				return super.onOptionsItemSelected(item);
		}
		
	}
	
	@SuppressWarnings("unused")
	private void toggleOverview() {
		// TODO Expand the one line overview item to show some details
	}

	@Override
	public void authenticate(String username, String password) {
		providerAPI_result_receiver = new ProviderAPIResultReceiver(new Handler());
		providerAPI_result_receiver.setReceiver(this);
		
		Intent provider_API_command = new Intent(this, ProviderAPI.class);

		Bundle method_and_parameters = new Bundle();
		method_and_parameters.putString(ConfigHelper.USERNAME_KEY, username);
		method_and_parameters.putString(ConfigHelper.PASSWORD_KEY, password);

		JSONObject provider_json;
		try {
			provider_json = new JSONObject(preferences.getString(ConfigHelper.PROVIDER_KEY, ""));
			method_and_parameters.putString(ConfigHelper.API_URL_KEY, provider_json.getString(ConfigHelper.API_URL_KEY) + "/" + provider_json.getString(ConfigHelper.API_VERSION_KEY));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		provider_API_command.putExtra(ConfigHelper.SRP_AUTH, method_and_parameters);
		provider_API_command.putExtra(ConfigHelper.RECEIVER_KEY, providerAPI_result_receiver);
		
		startService(provider_API_command);
	}
	
	/**
	 * Asks ProviderAPI to log out.
	 */
	public void logOut() {
		providerAPI_result_receiver = new ProviderAPIResultReceiver(new Handler());
		providerAPI_result_receiver.setReceiver(this);
		Intent provider_API_command = new Intent(this, ProviderAPI.class);

		Bundle method_and_parameters = new Bundle();

		JSONObject provider_json;
		try {
			provider_json = new JSONObject(preferences.getString(ConfigHelper.PROVIDER_KEY, ""));
			method_and_parameters.putString(ConfigHelper.API_URL_KEY, provider_json.getString(ConfigHelper.API_URL_KEY) + "/" + provider_json.getString(ConfigHelper.API_VERSION_KEY));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		provider_API_command.putExtra(ConfigHelper.LOG_OUT, method_and_parameters);
		provider_API_command.putExtra(ConfigHelper.RECEIVER_KEY, providerAPI_result_receiver);
		
		startService(provider_API_command);
	}
	
	/**
	 * Shows the log in dialog.
	 * @param view from which the dialog is created.
	 */
	public void logInDialog(View view) {
		FragmentTransaction fragment_transaction = getFragmentManager().beginTransaction();
	    Fragment previous_log_in_dialog = getFragmentManager().findFragmentByTag(ConfigHelper.LOG_IN_DIALOG);
	    if (previous_log_in_dialog != null) {
	        fragment_transaction.remove(previous_log_in_dialog);
	    }
	    fragment_transaction.addToBackStack(null);

	    DialogFragment newFragment = LogInDialog.newInstance();
	    newFragment.show(fragment_transaction, ConfigHelper.LOG_IN_DIALOG);
	}

	/**
	 * Asks ProviderAPI to download an authenticated OpenVPN certificate.
	 * @param session_id cookie for the server to allow us to download the certificate.
	 */
	private void downloadAuthedUserCertificate(Cookie session_id) {
		providerAPI_result_receiver = new ProviderAPIResultReceiver(new Handler());
		providerAPI_result_receiver.setReceiver(this);
		
		Intent provider_API_command = new Intent(this, ProviderAPI.class);

		Bundle method_and_parameters = new Bundle();
		method_and_parameters.putString(ConfigHelper.TYPE_OF_CERTIFICATE, ConfigHelper.AUTHED_CERTIFICATE);
		method_and_parameters.putString(ConfigHelper.SESSION_ID_COOKIE_KEY, session_id.getName());
		method_and_parameters.putString(ConfigHelper.SESSION_ID_KEY, session_id.getValue());

		provider_API_command.putExtra(ConfigHelper.DOWNLOAD_CERTIFICATE, method_and_parameters);
		provider_API_command.putExtra(ConfigHelper.RECEIVER_KEY, providerAPI_result_receiver);
		
		startService(provider_API_command);
	}

	@Override
	public void onReceiveResult(int resultCode, Bundle resultData) {
		if(resultCode == ConfigHelper.SRP_AUTHENTICATION_SUCCESSFUL){
			String session_id_cookie_key = resultData.getString(ConfigHelper.SESSION_ID_COOKIE_KEY);
			String session_id_string = resultData.getString(ConfigHelper.SESSION_ID_KEY);
			setResult(RESULT_OK);
			Toast.makeText(getApplicationContext(), R.string.succesful_authentication_message, Toast.LENGTH_LONG).show();

			Cookie session_id = new BasicClientCookie(session_id_cookie_key, session_id_string);
			downloadAuthedUserCertificate(session_id);
		} else if(resultCode == ConfigHelper.SRP_AUTHENTICATION_FAILED) {
        	setResult(RESULT_CANCELED);
			Toast.makeText(getApplicationContext(), R.string.authentication_failed_message, Toast.LENGTH_LONG).show();
		} else if(resultCode == ConfigHelper.LOGOUT_SUCCESSFUL) {
			setResult(RESULT_OK);
			Toast.makeText(getApplicationContext(), R.string.successful_log_out_message, Toast.LENGTH_LONG).show();
		} else if(resultCode == ConfigHelper.LOGOUT_FAILED) {
			setResult(RESULT_CANCELED);
			Toast.makeText(getApplicationContext(), R.string.log_out_failed_message, Toast.LENGTH_LONG).show();
		} else if(resultCode == ConfigHelper.CORRECTLY_DOWNLOADED_CERTIFICATE) {
        	setResult(RESULT_CANCELED);
			Toast.makeText(getApplicationContext(), R.string.successful_authed_cert_downloaded_message, Toast.LENGTH_LONG).show();
		} else if(resultCode == ConfigHelper.INCORRECTLY_DOWNLOADED_CERTIFICATE) {
        	setResult(RESULT_CANCELED);
			Toast.makeText(getApplicationContext(), R.string.authed_cert_download_failed_message, Toast.LENGTH_LONG).show();
		}
	}

	// Used for getting Context when outside of a class extending Context
	public static Context getAppContext() {
		return app;
	}
}
