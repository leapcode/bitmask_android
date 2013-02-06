package se.leap.leapclient;

import se.leap.openvpn.AboutFragment;
import se.leap.openvpn.MainActivity;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

public class Dashboard extends Activity {

	private static SharedPreferences preferences;
	private static Provider provider;

	private TextView providerNameTV;
	private TextView eipTypeTV;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.client_dashboard);

		preferences = getPreferences(MODE_PRIVATE);

		// Check if we have preferences, run configuration wizard if not
		// TODO We should do a better check for config that this!
		if (!preferences.contains("provider") )
			startActivity(new Intent(this, ProviderListActivity.class));
		else
			buildDashboard();
	}

	
	private void buildDashboard() {
		// Get our provider
				provider = Provider.getInstance(preferences);
				
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
		default:
				return super.onOptionsItemSelected(item);
		}
		
	}
	
	@SuppressWarnings("unused")
	private void toggleOverview() {
		// TODO Expand the one line overview item to show some details
	}

}
