package se.leap.bitmaskclient.test;

import android.content.*;
import android.graphics.*;
import android.test.*;
import android.view.*;
import android.widget.*;

import com.robotium.solo.*;

import java.io.*;

import de.blinkt.openvpn.activities.*;
import mbanje.kurt.fabbutton.FabButton;
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
        if (solo.searchText(solo.getString(R.string.configuration_wizard_title)))
            new testConfigurationWizard(solo).toDashboardAnonymously("demo.bitmask.net");
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
        clickVpnImage();
        turningEipOn();

        clickVpnImage();
        turningEipOff();

        clickVpnImage();
        turningEipOn();

        clickVpnImage();
        turningEipOff();

        /*clickVpnImage();;
        turningEipOn();
	    
	    turnNetworkOff();
        restartAdbServer(); // This doesn't work
        */

    }

    private void clickVpnImage() {
        solo.clickOnView(solo.getView(R.id.vpn_Status_Image));
    }

    private void turningEipOn() {
        assertAuthenticating();
        int max_seconds_until_connected = 30;
        assertConnected(max_seconds_until_connected);
        solo.sleep(2 * 1000);
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
        solo.sleep(2 * 1000);
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
        if (!solo.waitForText(getActivity().getString(R.string.eip_state_not_connected), 1, 15 * 1000))
            fail();
    }

    private void restartAdbServer() {
        runAdbCommand("kill-server");
        runAdbCommand("start-server");
    }

    public void testLogInAndOut() {
        long milliseconds_to_log_in = 40 * 1000;
        solo.clickOnActionBarItem(R.id.login_button);
        logIn("parmegvtest1", " S_Zw3'-");
        solo.waitForDialogToClose(milliseconds_to_log_in);
        assertSuccessfulLogin();

        logOut();
    }

    private void logIn(String username, String password) {
        solo.enterText(0, username);
        solo.enterText(1, password);
        solo.clickOnText(solo.getString(R.string.login_button));
        solo.waitForDialogToClose();
    }

    private void assertSuccessfulLogin() {
        assertTrue(solo.waitForText(solo.getString(R.string.logged_in_user_status)));
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
	tapSwitchProvider();
        solo.goBack();
    }

    private void tapSwitchProvider() {
        solo.clickOnMenuItem(solo.getString(R.string.switch_provider_menu_option));
        solo.waitForActivity(ConfigurationWizard.class);
    }

    public void testEveryProvider() {
        changeProvider("demo.bitmask.net");
        clickVpnImage();
        turningEipOn();
        clickVpnImage();
        turningEipOff();

        changeProvider("riseup.net");
        clickVpnImage();
        turningEipOn();
        clickVpnImage();
        turningEipOff();

        changeProvider("calyx.net");
        clickVpnImage();
        turningEipOn();
        clickVpnImage();
        turningEipOff();
    }

    private void changeProvider(String provider) {
	tapSwitchProvider();
        solo.clickOnText(provider);
	useRegistered();
	solo.waitForText(solo.getString(R.string.downloading_certificate_message));
	assertDisconnected();
    }

    private void useRegistered() {
        String text = solo.getString(R.string.signup_or_login_button);
        clickAndWaitForDashboard(text);
        login();
    }

    private void clickAndWaitForDashboard(String click_text) {
        solo.clickOnText(click_text);
        assertTrue(solo.waitForActivity(Dashboard.class, 5000));
    }

    private void login() {
        long milliseconds_to_log_in = 40 * 1000;
        logIn("parmegvtest10", "holahola2");
        solo.waitForDialogToClose(milliseconds_to_log_in);
        assertSuccessfulLogin();
    }

    public void testVpnIconIsDisplayed() {
        View vpn_status_image = solo.getView(R.id.vpn_Status_Image);
        assertTrue(isShownWithinConfinesOfVisibleScreen(vpn_status_image));
    }

    private boolean isShownWithinConfinesOfVisibleScreen(View view) {
        Rect scrollBounds = new Rect();
        view.getHitRect(scrollBounds);
        return view.getLocalVisibleRect(scrollBounds);
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
