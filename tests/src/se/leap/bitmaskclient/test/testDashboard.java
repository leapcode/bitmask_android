package se.leap.bitmaskclient.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import com.jayway.android.robotium.solo.Solo;
import se.leap.bitmaskclient.ConfigurationWizard;
import se.leap.bitmaskclient.Dashboard;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.test.ConnectionManager;
import se.leap.openvpn.MainActivity;

public class testDashboard extends ActivityInstrumentationTestCase2<Dashboard> {

	private Solo solo;
	
	public testDashboard() {
		super(Dashboard.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		solo = new Solo(getInstrumentation(), getActivity());
		ConnectionManager.setMobileDataEnabled(true, solo.getCurrentActivity().getApplicationContext());
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

		ConnectionManager.setMobileDataEnabled(false, solo.getCurrentActivity().getApplicationContext());
		
		solo.clickOnView(solo.getView(R.id.eipSwitch));
		if(!solo.waitForText("Initiating connection"))
			fail();
		if(!solo.waitForText("Waiting for usable network"))
			fail();
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
	}
	}
}
