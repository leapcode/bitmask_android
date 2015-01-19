package se.leap.bitmaskclient.test;

import android.content.Context;
import android.test.*;
import com.robotium.solo.*;

import java.io.IOException;

import de.blinkt.openvpn.activities.*;
import se.leap.bitmaskclient.*;

public class testDashboardIntegration extends ActivityInstrumentationTestCase2<Dashboard> {

	private Solo solo;
    private Context context;

	public testDashboardIntegration() {
		super(Dashboard.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
        context = getInstrumentation().getContext();
		solo = new Solo(getInstrumentation(), getActivity());
		ConnectionManager.setMobileDataEnabled(true, context);
        solo.unlockScreen();
        if(solo.searchText(solo.getString(R.string.configuration_wizard_title)))
            new testConfigurationWizard(solo).toDashboard("demo.bitmask.net");
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
	    turningEipOn();
	    
	    solo.clickOnView(solo.getView(R.id.eipSwitch));
	    turningEipOff();
	    
	    solo.clickOnView(solo.getView(R.id.eipSwitch));
	    turningEipOn();
	    
	    solo.clickOnView(solo.getView(R.id.eipSwitch));
	    turningEipOff();

        /*solo.clickOnView(solo.getView(R.id.eipSwitch));
        turningEipOn();
	    
	    turnNetworkOff();
        restartAdbServer(); // This doesn't work
        */
	    
	}

    private void turningEipOn() {
        assertAuthenticating();
        int max_seconds_until_connected = 30;
        assertConnected(max_seconds_until_connected);
        solo.sleep(2*1000);
    }

    private void assertAuthenticating() {
        String message = solo.getString(R.string.state_auth);
        assertTrue(solo.waitForText(message));
    }

    private void assertConnected(int max_seconds_until_connected) {
        String message = solo.getString(R.string.eip_state_connected);
        assertTrue(solo.waitForText(message, 1, max_seconds_until_connected * 1000));
    }

    private void turningEipOff() {
	sayOkToDisconnect();
        assertDisconnected();
	solo.sleep(2*1000);
    }

    private void sayOkToDisconnect() {
	assertTrue(solo.waitForActivity(DisconnectVPN.class));
        String yes = solo.getString(android.R.string.yes);
	solo.clickOnText(yes);
    }

    private void assertDisconnected() {
        String message = solo.getString(R.string.eip_state_not_connected);
        assertTrue(solo.waitForText(message));
    }
    
    private void turnNetworkOff() {
	ConnectionManager.setMobileDataEnabled(false, context);
	if(!solo.waitForText(getActivity().getString(R.string.eip_state_not_connected), 1, 15*1000))
	    fail();
    }

    private void restartAdbServer() {
        runAdbCommand("kill-server");
        runAdbCommand("start-server");
    }
    
    public void testLogInAndOut() {
		long milliseconds_to_log_in = 40 * 1000;
        logIn("parmegvtest1", " S_Zw3'-");
		solo.waitForDialogToClose(milliseconds_to_log_in);
        assertSuccessfulLogin();

        logOut();
	}

    private void logIn(String username, String password) {
        solo.clickOnActionBarItem(R.id.login_button);
        solo.enterText(0, username);
        solo.enterText(1, password);
        solo.clickOnText("Log In");
        solo.waitForDialogToClose();
    }

    private void assertSuccessfulLogin() {
        String message = solo.getString(R.string.succesful_authentication_message);
        assertTrue(solo.waitForText(message));
    }

    private void logOut() {
        solo.clickOnActionBarItem(R.string.logout_button);
        assertTrue(solo.waitForDialogToClose());
    }

    public void testShowAbout() {
        showAbout();
		solo.goBack();
		showAbout();
		solo.goBack();
	}

    private void showAbout() {
        String menu_item = solo.getString(R.string.about);
        solo.clickOnMenuItem(menu_item);

        String text_unique_to_about = solo.getString(R.string.repository_url_text);
        solo.waitForText(text_unique_to_about);
    }

    public void testSwitchProvider() {
  	        solo.clickOnMenuItem(solo.getString(R.string.switch_provider_menu_option));
		solo.waitForActivity(ConfigurationWizard.class);
		solo.goBack();
	}

    /*public void testReboot() {
        runAdbCommand("shell am broadcast -a android.intent.action.BOOT_COMPLETED");
    }*/

    private void runAdbCommand(String adb_command) {
        try {
            String command = "adb " + adb_command;
            Runtime.getRuntime().exec(command).waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
