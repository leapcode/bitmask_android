package se.leap.bitmaskclient.test;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.widget.Button;

import com.robotium.solo.*;

import java.io.*;

import de.blinkt.openvpn.activities.*;
import mbanje.kurt.fabbutton.CircleImageView;
import mbanje.kurt.fabbutton.FabButton;
import mbanje.kurt.fabbutton.ProgressRingView;
import se.leap.bitmaskclient.*;

public class testDashboardIntegration extends BaseTestDashboard {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Screenshot.initialize(solo);
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

    private FabButton getVpnWholeIcon() {
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
        ProgressRingView a = (ProgressRingView) getVpnWholeIcon().findViewById(R.id.fabbutton_ring);
        assertTrue(isShownWithinConfinesOfVisibleScreen(a));
    }

    private boolean iconConnected() {
        return getVpnInsideIcon().equals(getDrawable(R.drawable.ic_stat_vpn));
    }

    private boolean iconDisconnected() {
        return getVpnInsideIcon().equals(getDrawable(R.drawable.ic_stat_vpn_offline));
    }

    private Drawable getDrawable(int resId) {
        return getActivity().getResources().getDrawable(resId);
    }

    private Bitmap getVpnInsideIcon() {
        CircleImageView a = (CircleImageView) getVpnWholeIcon().findViewById(R.id.fabbutton_circle);
        a.setDrawingCacheEnabled(true);
        return a.getDrawingCache();
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

    private void turnNetworkOff() {
        ConnectionManager.setMobileDataEnabled(false, context);
        if (!solo.waitForText(getActivity().getString(R.string.eip_state_not_connected), 1, 15 * 1000))
            fail();
    }

    private void restartAdbServer() {
        runAdbCommand("kill-server");
        runAdbCommand("start-server");
    }

    public void testShowAbout() {
        showAbout();
        solo.goBack();
        showAbout();
        solo.goBack();
    }

    private void showAbout() {
        clickAbout();
        String text_unique_to_about = solo.getString(R.string.repository_url_text);
        solo.waitForText(text_unique_to_about);
    }

    private void clickAbout() {
        String menu_item = solo.getString(R.string.about);
        solo.clickOnMenuItem(menu_item);
    }

    public void testSwitchProvider() {
        tapSwitchProvider();
        solo.goBack();
    }

    public void testVpnEveryProvider() {
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

    public void testVpnIconIsDisplayed() {
        assertTrue(isShownWithinConfinesOfVisibleScreen(getVpnWholeIcon()));
    }
    public void testVpnButtonIsDisplayed() {
        assertTrue(isShownWithinConfinesOfVisibleScreen(getVpnButton()));
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
