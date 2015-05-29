package se.leap.bitmaskclient.test;

import de.blinkt.openvpn.activities.LogWindow;

public class testVpnFragment extends BaseTestDashboard {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Screenshot.initialize(solo);
    }

    /**
     * This test will fail if Android does not trust VPN connection.
     * I cannot automate that dialog.
     */
    public void testOnOffOpenVpn() {
        Screenshot.take("Initial UI");
        vpn_controller.clickVpnButton();
        Screenshot.setTimeToSleep(5);
        Screenshot.takeWithSleep("Turning VPN on");
        vpn_controller.turningEipOn();
        Screenshot.setTimeToSleep(0.5);
        Screenshot.takeWithSleep("VPN turned on");

        vpn_controller.clickVpnButton();
        vpn_controller.turningEipOff();
        Screenshot.take("VPN turned off");

        vpn_controller.clickVpnButton();
        vpn_controller.turningEipOn();

        vpn_controller.clickVpnButton();
        vpn_controller.turningEipOff();

        /*clickVpnButton();;
        turningEipOn();

	    turnNetworkOff();
        restartAdbServer(); // This doesn't work
        */

    }

    /**
     * Run only if the trust this app dialog has not been checked.
     * You must pay attention to the screen, because you need to cancel de dialog twice (block vpn and normal vpn)
     */
    public void testOnFailed() {
        vpn_controller.clickVpnButton();
        assertTrue("Have you checked the trust vpn dialog?", solo.waitForActivity(LogWindow.class));
        solo.goBack();
        vpn_controller.iconShowsDisconnected();
    }

    public void testVpnEveryProvider() {
        String[] providers = {"demo.bitmask.net", "riseup.net", "calyx.net"};
        for(String provider : providers) {
            changeProviderAndLogIn(provider);
            vpn_controller.sleepSeconds(1);
            vpn_controller.turnVpnOndAndOff(provider);
            vpn_controller.sleepSeconds(1);
        }
    }

    public void testVpnIconIsDisplayed() {
        assertTrue(isShownWithinConfinesOfVisibleScreen(vpn_controller.getVpnWholeIcon()));
    }
    public void testVpnButtonIsDisplayed() {
        assertTrue(isShownWithinConfinesOfVisibleScreen(vpn_controller.getVpnButton()));
    }
}
