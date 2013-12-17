package se.leap.bitmaskclient.test;

import se.leap.bitmaskclient.ConfigurationWizard;
import se.leap.bitmaskclient.Dashboard;
import se.leap.bitmaskclient.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.jayway.android.robotium.solo.Solo;

public class testDashboard extends ActivityInstrumentationTestCase2<Dashboard> {

	private Solo solo;
	
	public testDashboard() {
		super(Dashboard.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		solo = new Solo(getInstrumentation(), getActivity());
		setAirplaneMode(false);
	}

	@Override
	protected void tearDown() throws Exception { 
		solo.finishOpenedActivities();
	}
	
	/**
	 * This test will fail if Android does not trust VPN connection.
	 * I cannot automate that dialog.
	 */
	public void testOnOffOpenVpn() {
		solo.clickOnView(solo.getView(R.id.eipSwitch));
		if(!solo.waitForText("Initiating connection"))
			fail();
		if(!solo.waitForText("Authenticating"))
			fail();
		if(!solo.waitForText("Connection Secure", 1, 30*1000))
			fail();
		
		solo.clickOnView(solo.getView(R.id.eipSwitch));
		if(!solo.waitForText("Not running! Connection not secure"))
			fail();
		/* setAirplaneMode isn't working right now.
		setAirplaneMode(true);
		if(!solo.waitForLogMessage("Service state changed"))
			fail();
		
		solo.clickOnView(solo.getView(R.id.eipSwitch));
		if(!solo.waitForText("Initiating connection"))
			fail();
		if(!solo.waitForText("Waiting for usable network"))
			fail();
		*/
	}
	
	public void testLogInAndOut() {
		long miliseconds_to_log_in = 40 * 1000;
		solo.clickOnActionBarItem(R.id.login_button);
		solo.enterText(0, "parmegvtest1");
		solo.enterText(1, " S_Zw3'-");
		solo.clickOnText("Log In");
		solo.waitForDialogToClose();
		solo.waitForDialogToClose(miliseconds_to_log_in);
		if(!solo.waitForText("Your own cert has been correctly downloaded."))
			fail();

		solo.clickOnActionBarItem(R.string.logout_button);
		if(!solo.waitForDialogToClose())
			fail();
	}
	
	public void testShowSettings() {
		solo.clickOnActionBarItem(R.id.menu_settings);
	}
	
	public void testShowAbout() {
		solo.clickOnMenuItem("About");
		solo.waitForText(getActivity().getString(R.string.repository_url_text));
		solo.goBack();
		
		solo.clickOnMenuItem("About");
		solo.waitForText(getActivity().getString(R.string.repository_url_text));
		solo.goBack();
	}
	
	public void testSwitchProvider() {
		solo.clickOnMenuItem("Switch provider");
		solo.waitForActivity(ConfigurationWizard.class);
		solo.goBack();

		solo.clickOnMenuItem("Switch provider");
		solo.waitForActivity(ConfigurationWizard.class);
		solo.goBack();
	}
	
	private void setAirplaneMode(boolean airplane_mode) {
		Context context = solo.getCurrentActivity().getApplicationContext();
	    boolean isEnabled = Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) == 1;
	    Log.d("AirplaneMode", "Service state: " + isEnabled);
	    Settings.System.putInt(context.getContentResolver(),Settings.System.AIRPLANE_MODE_ON, airplane_mode ? 1 : 0);
	    
	    // Post an intent to reload
	    Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
	    intent.putExtra("state", airplane_mode);
	    Log.d("AirplaneMode", "New Service state: " + !isEnabled);
		solo.getCurrentActivity().sendBroadcast(intent);
		
		IntentFilter intentFilter = new IntentFilter("android.intent.action.SERVICE_STATE");
		 
		BroadcastReceiver receiver = new BroadcastReceiver() {
		      @Override
		      public void onReceive(Context context, Intent intent) {
		    	  boolean isEnabled = Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) == 1;
		    	  Log.d("AirplaneMode", "Service state changed: " + isEnabled);
		      }
		};
		
		context.registerReceiver(receiver, intentFilter);
	}
}
