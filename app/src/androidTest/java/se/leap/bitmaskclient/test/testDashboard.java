package se.leap.bitmaskclient.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import com.robotium.solo.Solo;

import de.blinkt.openvpn.activities.DisconnectVPN;
import se.leap.bitmaskclient.ConfigurationWizard;
import se.leap.bitmaskclient.Dashboard;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.test.ConnectionManager;

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
	    testEipTurningOn();
	    
	    solo.clickOnView(solo.getView(R.id.eipSwitch));
	    testEipTurningOff();
	    
	    solo.clickOnView(solo.getView(R.id.eipSwitch));
	    testEipTurningOn();
	    
	    solo.clickOnView(solo.getView(R.id.eipSwitch));
	    testEipTurningOff();
	    
	    solo.clickOnView(solo.getView(R.id.eipSwitch));
	    testEipTurningOn();
	    
	    solo.clickOnView(solo.getView(R.id.eipSwitch));
	    testEipTurningOff();

	    solo.clickOnView(solo.getView(R.id.eipSwitch));
	    testEipTurningOn();
	    
	    testEipIsOnNoNetwork();
	    
	}

    private void testEipTurningOn() {
	if(!solo.waitForText(getActivity().getString(R.string.state_auth)))
	    fail();
	if(!solo.waitForText(getActivity().getString(R.string.eip_state_connected), 1, 30*1000))
	    fail();
	solo.sleep(2*1000);
    }

    private void testEipTurningOff() {
	sayOkToDisconnect();
	if(!solo.waitForText(getActivity().getString(R.string.eip_state_not_connected)))
	    fail();
	solo.sleep(2*1000);
    }

    private void sayOkToDisconnect() {
	if(!solo.waitForActivity(DisconnectVPN.class))
	    fail();
	solo.clickOnText(getActivity().getString(android.R.string.yes));
    }
    
    private void testEipIsOnNoNetwork() {
	ConnectionManager.setMobileDataEnabled(false, solo.getCurrentActivity().getApplicationContext());
	if(!solo.waitForText(getActivity().getString(R.string.eip_state_not_connected), 1, 15*1000))
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
		if(!solo.waitForText(getActivity().getString(R.string.succesful_authentication_message)))
			fail();

		solo.clickOnActionBarItem(R.string.logout_button);
		if(!solo.waitForDialogToClose())
			fail();
	}
	
	public void testShowAbout() {
  	        solo.clickOnMenuItem(getActivity().getString(R.string.about));
		solo.waitForText(getActivity().getString(R.string.repository_url_text));
		solo.goBack();
		
		solo.clickOnMenuItem(getActivity().getString(R.string.about));
		solo.waitForText(getActivity().getString(R.string.repository_url_text));
		solo.goBack();
	}
	
	public void testSwitchProvider() {
  	        solo.clickOnMenuItem(getActivity().getString(R.string.switch_provider_menu_option));
		solo.waitForActivity(ConfigurationWizard.class);
		solo.goBack();
	}
}
