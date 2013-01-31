package se.leap.leapclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import se.leap.openvpn.AboutFragment;
import se.leap.openvpn.MainActivity;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
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

		// Get our provider
		provider = Provider.getInstance(preferences);
		
		// Set provider name in textview
		providerNameTV = (TextView) findViewById(R.id.providerName);
		providerNameTV.setText(provider.getName());
		providerNameTV.setTextSize(28); // TODO maybe to some calculating, or a marquee?
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
		case R.id.legacy_interface:
			// TODO call se.leap.openvpn.MainActivity
			intent = new Intent(this,MainActivity.class);
			startActivity(intent);
			return true;
		default:
				return super.onOptionsItemSelected(item);
		}
		
	}
	
}
