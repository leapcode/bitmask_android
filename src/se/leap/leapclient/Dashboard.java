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

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONException;
import org.json.JSONObject;

import se.leap.leapclient.ProviderAPIResultReceiver.Receiver;
import se.leap.openvpn.MainActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The main user facing Activity of LEAP Android, consisting of status, controls,
 * and access to preferences.
 * 
 * @author Sean Leonard <meanderingcode@aetherislands.net>
 * @author parmegv
 */
public class Dashboard extends Activity implements LogInDialog.LogInDialogInterface,Receiver {

	protected static final int CONFIGURE_LEAP = 0;

	private static final String TAG_EIP_FRAGMENT = "EIP_DASHBOARD_FRAGMENT";
    final public static String SHARED_PREFERENCES = "LEAPPreferences";
    final public static String ACTION_QUIT = "quit";

	private ProgressDialog mProgressDialog;
	private ProgressBar mProgressBar;
	private ProviderAPIBroadcastReceiver_Update providerAPI_broadcast_receiver_update;
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
		
	    mProgressBar = (ProgressBar) findViewById(R.id.progressbar_dashboard);

	    providerAPI_broadcast_receiver_update = new ProviderAPIBroadcastReceiver_Update();
	    IntentFilter update_intent_filter = new IntentFilter(ProviderAPI.UPDATE_ACTION);
	    update_intent_filter.addCategory(Intent.CATEGORY_DEFAULT);
	    registerReceiver(providerAPI_broadcast_receiver_update, update_intent_filter);
	    
		ConfigHelper.setSharedPreferences(getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE));
		preferences = ConfigHelper.shared_preferences;
		
		authed_eip = ConfigHelper.getBoolFromSharedPref(EIP.AUTHED);
		if (ConfigHelper.getStringFromSharedPref(Provider.KEY).isEmpty())
			startActivityForResult(new Intent(this,ConfigurationWizard.class),CONFIGURE_LEAP);
		else
			buildDashboard();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(providerAPI_broadcast_receiver_update);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if ( requestCode == CONFIGURE_LEAP ) {
			if ( resultCode == RESULT_OK ){
				ConfigHelper.saveSharedPref(EIP.AUTHED, authed_eip);
				startService( new Intent(EIP.ACTION_UPDATE_EIP_SERVICE) );
				buildDashboard();
				if(data != null && data.hasExtra(LogInDialog.VERB)) {
					View view = ((ViewGroup)findViewById(android.R.id.content)).getChildAt(0);
					logInDialog(view, Bundle.EMPTY);
				}
			} else if(resultCode == RESULT_CANCELED && data.hasExtra(ACTION_QUIT)) {
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
	private void buildDashboard() {
		provider = Provider.getInstance();
		provider.init( this );

		setContentView(R.layout.client_dashboard);

		providerNameTV = (TextView) findViewById(R.id.providerName);
		providerNameTV.setText(provider.getDomain());
		providerNameTV.setTextSize(28);

		FragmentManager fragMan = getFragmentManager();
		if ( provider.hasEIP()){
			EipServiceFragment eipFragment = new EipServiceFragment();
			fragMan.beginTransaction().replace(R.id.servicesCollection, eipFragment, TAG_EIP_FRAGMENT).commit();
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		JSONObject provider_json;
		try {
			provider_json = ConfigHelper.getJsonFromSharedPref(Provider.KEY);
			JSONObject service_description = provider_json.getJSONObject(Provider.SERVICE);
			if(service_description.getBoolean(Provider.ALLOW_REGISTRATION)) {
				if(authed_eip) {
					menu.findItem(R.id.login_button).setVisible(false);
					menu.findItem(R.id.logout_button).setVisible(true);
				} else {
					menu.findItem(R.id.login_button).setVisible(true);
					menu.findItem(R.id.logout_button).setVisible(false);
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
			Fragment aboutFragment = new AboutFragment();
			FragmentTransaction trans = getFragmentManager().beginTransaction();
			trans.replace(R.id.dashboardLayout, aboutFragment);
			trans.addToBackStack(null);
			trans.commit();
			return true;
		case R.id.legacy_interface:
			intent = new Intent(this,MainActivity.class);
			startActivity(intent);
			return true;
		case R.id.switch_provider:
			startActivityForResult(new Intent(this,ConfigurationWizard.class),CONFIGURE_LEAP);
			return true;
		case R.id.login_button:
			View view = ((ViewGroup)findViewById(android.R.id.content)).getChildAt(0);
			logInDialog(view, Bundle.EMPTY);
			return true;
		case R.id.logout_button:
			logOut();
			return true;
		default:
				return super.onOptionsItemSelected(item);
		}
		
	}

	@Override
	public void authenticate(String username, String password) {
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
		
		//if(mProgressDialog != null) mProgressDialog.dismiss();
		mProgressBar.setVisibility(ProgressBar.VISIBLE);
		//mProgressDialog = ProgressDialog.show(this, getResources().getString(R.string.authenticating_title), getResources().getString(R.string.authenticating_message), true);
		startService(provider_API_command);
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
		
		if(mProgressDialog != null) mProgressDialog.dismiss();
		mProgressDialog = ProgressDialog.show(this, getResources().getString(R.string.logout_title), getResources().getString(R.string.logout_message), true);
		startService(provider_API_command);
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
			ConfigHelper.saveSharedPref(EIP.AUTHED, authed_eip);
			invalidateOptionsMenu();

        	mProgressBar.setVisibility(ProgressBar.GONE);

        	//Cookie session_id = new BasicClientCookie(session_id_cookie_key, session_id_string);
        	downloadAuthedUserCertificate(/*session_id*/);
		} else if(resultCode == ProviderAPI.SRP_AUTHENTICATION_FAILED) {
        	logInDialog(getCurrentFocus(), resultData);
        	mProgressBar.setVisibility(ProgressBar.GONE);
		} else if(resultCode == ProviderAPI.LOGOUT_SUCCESSFUL) {
			authed_eip = false;
			ConfigHelper.saveSharedPref(EIP.AUTHED, authed_eip);
			invalidateOptionsMenu();
			setResult(RESULT_OK);
			mProgressDialog.dismiss();
		} else if(resultCode == ProviderAPI.LOGOUT_FAILED) {
			setResult(RESULT_CANCELED);
			mProgressDialog.dismiss();
			Toast.makeText(getApplicationContext(), R.string.log_out_failed_message, Toast.LENGTH_LONG).show();
		} else if(resultCode == ProviderAPI.CORRECTLY_DOWNLOADED_CERTIFICATE) {
        	setResult(RESULT_OK);
			mProgressDialog.dismiss();
			Toast.makeText(getApplicationContext(), R.string.successful_authed_cert_downloaded_message, Toast.LENGTH_LONG).show();
		} else if(resultCode == ProviderAPI.INCORRECTLY_DOWNLOADED_CERTIFICATE) {
        	setResult(RESULT_CANCELED);
			mProgressDialog.dismiss();
			Toast.makeText(getApplicationContext(), R.string.authed_cert_download_failed_message, Toast.LENGTH_LONG).show();
		}
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

	public class ProviderAPIBroadcastReceiver_Update extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			int update = intent.getIntExtra(ProviderAPI.UPDATE_DATA, 0);
			mProgressBar.setProgress(update);
		}
	}
}
