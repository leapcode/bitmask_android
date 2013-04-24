package se.leap.leapclient;

import org.json.JSONException;
import org.json.JSONObject;

import se.leap.leapclient.ProviderAPIResultReceiver.Receiver;
import se.leap.openvpn.AboutFragment;
import se.leap.openvpn.MainActivity;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class Dashboard extends Activity implements LogInDialog.LogInDialogInterface, Receiver {

	protected static final int CONFIGURE_LEAP = 0;
	
	private static SharedPreferences preferences;
	private static Provider provider;

	private TextView providerNameTV;
	private TextView eipTypeTV;

    public ProviderAPIResultReceiver providerAPI_result_receiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.client_dashboard);

		preferences = getSharedPreferences(ConfigHelper.PREFERENCES_KEY,MODE_PRIVATE);

		// Check if we have preferences, run configuration wizard if not
		// TODO We should do a better check for config that this!
		if (!preferences.contains("provider") )
			startActivityForResult(new Intent(this,ConfigurationWizard.class),CONFIGURE_LEAP);
		else
			buildDashboard();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if ( requestCode == CONFIGURE_LEAP ) {
			if ( resultCode == RESULT_OK ){
				// Configuration done, get our preferences again
				preferences = getSharedPreferences(ConfigHelper.PREFERENCES_KEY,MODE_PRIVATE);
				
				buildDashboard();
			} else {
				// Something went wrong... TODO figure out what
				// TODO Error dialog
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
				
				// TODO Inflate layout fragments for provider's services
				if ( provider.hasEIP() )
					serviceItemEIP();
	}

	private void serviceItemEIP() {
		// FIXME Provider service (eip/openvpn)	
		View eipOverview = ((ViewStub) findViewById(R.id.eipOverviewStub)).inflate();

		// Set our EIP type title
		eipTypeTV = (TextView) findViewById(R.id.eipType);
		eipTypeTV.setText(provider.getEIPType());

		// TODO Bind our switch to run our EIP
		// What happens when our VPN stops running?  does it call the listener?
		Switch eipSwitch = (Switch) findViewById(R.id.eipSwitch);
		eipSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if ( isChecked ){
					//TODO startVPN();
				} else {
					//TODO stopVPN();
				}
			}
		});

		//TODO write our info into the view	fragment that will expand with details and a settings button
		// TODO set eip overview subview
		// TODO make eip type clickable, show subview
		// TODO attach vpn status feedback to eip overview view
		// TODO attach settings button to something
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
		method_and_parameters.putString(ConfigHelper.username_key, username);
		method_and_parameters.putString(ConfigHelper.password_key, password);

		JSONObject provider_json;
		try {
			provider_json = new JSONObject(preferences.getString(ConfigHelper.provider_key, ""));
			method_and_parameters.putString(ConfigHelper.api_url_key, provider_json.getString(ConfigHelper.api_url_key));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		provider_API_command.putExtra(ConfigHelper.srpAuth, method_and_parameters);
		provider_API_command.putExtra("receiver", providerAPI_result_receiver);
		
		startService(provider_API_command);
	}
	
	public void logInDialog(View view) {
		FragmentTransaction fragment_transaction = getFragmentManager().beginTransaction();
	    Fragment previous_log_in_dialog = getFragmentManager().findFragmentByTag(ConfigHelper.logInDialog);
	    if (previous_log_in_dialog != null) {
	        fragment_transaction.remove(previous_log_in_dialog);
	    }
	    fragment_transaction.addToBackStack(null);

	    DialogFragment newFragment = LogInDialog.newInstance();
	    newFragment.show(fragment_transaction, ConfigHelper.logInDialog);
	}

	@Override
	public void onReceiveResult(int resultCode, Bundle resultData) {
		if(resultCode == ConfigHelper.SRP_AUTHENTICATION_SUCCESSFUL){
			setResult(RESULT_OK);
			Toast.makeText(getApplicationContext(), "Authentication succeeded", Toast.LENGTH_LONG).show();
			//TODO What should we do know?
		}
		else if(resultCode == ConfigHelper.SRP_AUTHENTICATION_FAILED) {
        	setResult(RESULT_CANCELED);
			Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_LONG).show();
		}
	}

}
