package se.leap.bitmaskclient.test;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.provider.Settings;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import com.jayway.android.robotium.solo.Solo;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import se.leap.bitmaskclient.ConfigurationWizard;
import se.leap.bitmaskclient.Dashboard;
import se.leap.bitmaskclient.R;
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
	}
	
	public void testIcsOpenVpnInterface() {
		solo.clickOnMenuItem("ICS OpenVPN Interface");
		solo.waitForActivity(MainActivity.class);
		
		solo.goBack();
		
		solo.clickOnMenuItem("ICS OpenVPN Interface");
		solo.waitForActivity(MainActivity.class);
	}
	
    private void setAirplaneMode(boolean airplane_mode) {
	Context context = solo.getCurrentActivity().getApplicationContext();
	final ConnectivityManager conman = (ConnectivityManager)  context.getSystemService(Context.CONNECTIVITY_SERVICE);
	try {
	    final Class conmanClass = Class.forName(conman.getClass().getName());
	    final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
	    iConnectivityManagerField.setAccessible(true);
	    final Object iConnectivityManager = iConnectivityManagerField.get(conman);
	    final Class iConnectivityManagerClass =  Class.forName(iConnectivityManager.getClass().getName());
	    final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
	    setMobileDataEnabledMethod.setAccessible(true);

	    setMobileDataEnabledMethod.invoke(iConnectivityManager, !airplane_mode);
	} catch (ClassNotFoundException e) {
	} catch (NoSuchMethodException e) {
	} catch (IllegalAccessException e) {
	} catch (NoSuchFieldException e) {
	} catch (InvocationTargetException e) {
	}
    }
}
