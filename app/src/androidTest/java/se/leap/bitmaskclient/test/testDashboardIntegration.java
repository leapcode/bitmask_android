package se.leap.bitmaskclient.test;

import android.content.*;
import android.graphics.*;
import android.test.*;
import android.view.*;
import android.widget.Button;

import com.robotium.solo.*;

import java.io.*;

import de.blinkt.openvpn.activities.*;
import mbanje.kurt.fabbutton.CircleImageView;
import mbanje.kurt.fabbutton.FabButton;
import mbanje.kurt.fabbutton.ProgressRingView;
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
        Screenshot.initialize(solo);
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
        Screenshot.take("Initial UI");
        clickVpnButton();
        Screenshot.setTimeToSleep(5);
        Screenshot.takeWithSleep("Turning VPN on");
        turningEipOn();
        Screenshot.setTimeToSleep(0.5);
        Screenshot.takeWithSleep("VPN turned on");

        clickVpnButton();
        turningEipOff();
        Screenshot.take("VPN turned off");

        clickVpnButton();
        turningEipOn();

        clickVpnButton();
        turningEipOff();

        /*clickVpnButton();;
        turningEipOn();
	    
	    turnNetworkOff();
        restartAdbServer(); // This doesn't work
        */

    }

    private void clickVpnButton() {
        solo.clickOnView(getVpnButton());
    }

    private Button getVpnButton() {
        return (Button) solo.getView(R.id.vpn_main_button);
    }

    private FabButton getVpnImage() {
        return (FabButton) solo.getView(R.id.vpn_Status_Image);
    }

    private void turningEipOn() {
        assertInProgress();
        int max_seconds_until_connected = 30;

        Condition condition = new Condition() {
            @Override
            public boolean isSatisfied() {
                return iconConnected();
            }
        };
        solo.waitForCondition(condition, max_seconds_until_connected * 1000);
        sleepSeconds(2);
    }

    private void assertInProgress() {
        ProgressRingView a = (ProgressRingView) getVpnImage().findViewById(R.id.fabbutton_ring);
        assertTrue(isShownWithinConfinesOfVisibleScreen(a));
    }

    private boolean iconConnected() {
        CircleImageView a = (CircleImageView) getVpnImage().findViewById(R.id.fabbutton_circle);
        a.setDrawingCacheEnabled(true);
        return a.getDrawingCache().equals(getActivity().getResources().getDrawable(R.drawable.ic_stat_vpn));
    }

    private void turningEipOff() {
        okToBrowserWarning();
        sayOkToDisconnect();

        int max_seconds_until_connected = 1;

        Condition condition = new Condition() {
            @Override
            public boolean isSatisfied() {
                return iconDisconnected();
            }
        };
        solo.waitForCondition(condition, max_seconds_until_connected * 1000);
        sleepSeconds(2);
    }

    private void okToBrowserWarning() {
        solo.waitForDialogToOpen();
        clickYes();
    }

    private void clickYes() {
        String yes = solo.getString(android.R.string.yes);
        solo.clickOnText(yes);
    }

    private void sayOkToDisconnect() {
        assertTrue(solo.waitForActivity(DisconnectVPN.class));
        clickYes();
    }

    private boolean iconDisconnected() {
        CircleImageView a = (CircleImageView) getVpnImage().findViewById(R.id.fabbutton_circle);
        a.setDrawingCacheEnabled(true);
        return a.getDrawingCache().equals(getActivity().getResources().getDrawable(R.drawable.ic_stat_vpn_offline));
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
        changeProvider("demo.bitmask.net");
        clickLogIn();
        logIn("parmegvtest1", " S_Zw3'-");
        assertSuccessfulLogin();

        logOut();
    }

    private void clickLogIn() {
        solo.clickOnView(getLogInButton());
    }

    private View getLogInButton() {
        return solo.getView(R.id.user_session_button);
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
        changeAndTestProvider("demo.bitmask.net");
        changeAndTestProvider("riseup.net");
        changeAndTestProvider("calyx.net");
    }

    private void changeAndTestProvider(String provider) {
        changeProvider(provider);
        sleepSeconds(1);
        clickVpnButton();
        turningEipOn();
        clickVpnButton();
        turningEipOff();
    }

    private void sleepSeconds(int seconds) {
        solo.sleep(seconds * 1000);
    }

    private void changeProvider(String provider) {
        tapSwitchProvider();
        solo.clickOnText(provider);
        useRegistered();
    }

    private void useRegistered() {
        String text = solo.getString(R.string.signup_or_login_button);
        clickAndWaitForDashboard(text);
        logIn("parmegvtest10", "holahola2");
        assertSuccessfulLogin();
    }

    private void clickAndWaitForDashboard(String click_text) {
        solo.clickOnText(click_text);
        assertTrue(solo.waitForActivity(Dashboard.class, 5000));
    }

    public void testVpnIconIsDisplayed() {
        assertTrue(isShownWithinConfinesOfVisibleScreen(getVpnImage()));
    }
    public void testVpnButtonIsDisplayed() {
        assertTrue(isShownWithinConfinesOfVisibleScreen(getVpnButton()));
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
