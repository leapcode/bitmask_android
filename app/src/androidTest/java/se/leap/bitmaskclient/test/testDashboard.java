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

    public void testUpdateExpiredCertificate() {
        String certificate = "-----BEGIN CERTIFICATE-----" +
                "MIIEnDCCAoSgAwIBAgIRAOBkcbMKR0Jlw+xNalHn7aIwDQYJKoZIhvcNAQELBQAwdTEYMBYGA1UE" +
                "CgwPUmlzZXVwIE5ldHdvcmtzMRswGQYDVQQLDBJodHRwczovL3Jpc2V1cC5uZXQxPDA6BgNVBAMM" +
                "M1Jpc2V1cCBOZXR3b3JrcyBSb290IENBIChjbGllbnQgY2VydGlmaWNhdGVzIG9ubHkhKTAeFw0x" +
                "NDA5MTkwMDAwMDBaFw0xNDExMTkwMDAwMDBaMC0xKzApBgNVBAMMIlVOTElNSVRFRDcwZWhxZG9l" +
                "ZXQ2Z243bmc3eWx3ZWNxeGwwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDdaKQHSwg2" +
                "Q2Uz9t5mae9BfV9Jkk+WSU6jXixsTbtLAr8gvuNcVuI0lKm2zXVqoS8aRCSsCt12vhjU/WBTSv0t" +
                "vwTaT2HQYFQ1GlVUBKssJEUpaVyQKL6LN9BA5ZODBpbhefRIX8z+02afxmNWdnOQfDtLU6nHSQLL" +
                "IUBSmgu+Y2Q3SdIBojIl9Kj0Zt6uZkhtOXZqkwLBiMr+/ukSidpcmNgbAN0eXSfVouaduzsDPQ6M" +
                "eCJTz2lhUvC0/57h5mlkNLzEjyb/pAVTtnK4zdiH6XAuCxU/AkF0yzhaiQWMG0RQb4vEx/UHjkDU" +
                "+K0GDy/qx1BmBB7C4vHLauqSXOs1AgMBAAGjbzBtMB0GA1UdDgQWBBQioBn7DdhjmtBKgQKpx/aW" +
                "XHYkGjALBgNVHQ8EBAMCB4AwEwYDVR0lBAwwCgYIKwYBBQUHAwIwCQYDVR0TBAIwADAfBgNVHSME" +
                "GDAWgBQX9BvV5SoBAU1rol02CikJlmWARjANBgkqhkiG9w0BAQsFAAOCAgEAV7q102FQ62IOX84o" +
                "pPvUL3hJkGtZ5chgQwZhfl2fGtEdeqpU27Hx1jLP9o3n1z9XYaZg/d8xYhpY6Mm4rFl6hA4gk81Z" +
                "yg/A3QeUgIjOsA0Xp+RNB5ACaLjCPUtWNk5brfuelDdFHjl1noC2P3vQ9ErhUna6TKVsxxrueimO" +
                "nc3sV7YMGiVfPC7wEmhERuyhQxftIUHUy2kDCY5QgXtru6IZmc3SP4FcM8LUSC49kqmU9if2GTLo" +
                "wQZmz6T7+N5PIJWIOiDh9PyoojRo7ep9szeIZpzgxcsoE/9ed84tg36JLOWi0GOyrdzVExv0rQQt" +
                "q/NpqAe1mX5XQVbY8nwgaJ8eWIWIXIn+5RB7b+fm5ZFeM4eFyWeDk99bvS8jdH6uQP5WusL55+ft" +
                "ADtESsmBvzUEGqxk5GL4lmmeqE+vsR5TesqGjZ+yH67rR+1+Uy2mhbqJBP0E0LHwWCCPYEVfngHj" +
                "aZkDF1UVQdfc9Amc5u5J5YliWrEG80BNeJF7740Gwx69DHEIhElN+BBeeqLLYIZTKmt28/9iWbKL" +
                "vhCrz/29wLYksL1bXmyHzvzyAcDHPpO9sQrKYiP1mGRDmXJmZU3i3cgeqQFZ8+lr55wcYdMGJOcx" +
                "bz+jL0VkHdnoZdzGzelrAhZtgMtsJ/kgWYRgtFmhpYF1Xtj2MYrpBDxgQck=" +
                "-----END CERTIFICATE-----";

    }
}
