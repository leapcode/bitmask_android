package se.leap.bitmaskclient.test;

public class TestVpnFragment extends BaseTestDashboardFragment {

    /**
     * This test will fail if Android does not trust VPN connection.
     * I cannot automate that dialog.
     */
    public void testOnOffOpenVpn() {
        vpn_controller.clickVpnButton();
        Screenshot.setTimeToSleep(2);
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

    }

    /**
     * Run only if the trust this app dialog has not been checked.
     * You must pay attention to the screen, because you need to cancel de dialog twice (block vpn and normal vpn)
     */
    public void testOnFailed() {
        /* TODO Do not rely on the Android's vpn trust dialog
        vpn_controller.clickVpnButton();
        assertTrue("Have you checked the trust vpn dialog?", solo.waitForActivity(LogWindow.class));
        solo.goBack();
        assertTrue(vpn_controller.iconShowsDisconnected());
        */
    }

    public void testVpnEveryProvider() {
        checkDemoBitmaskNet();
        checkRiseupNet();
        checkCalyxNet();
    }

    private void checkDemoBitmaskNet() {
        checkProvider("demo.bitmask.net");
    }

    private void checkRiseupNet() {
        checkProvider("riseup.net");
    }

    private void checkCalyxNet() {
        checkProvider("calyx.net");
    }

    private void checkProvider(String provider) {
        changeProviderAndLogIn(provider);
        vpn_controller.sleepSeconds(1);
        vpn_controller.turnVpnOndAndOff();
        vpn_controller.sleepSeconds(1);
    }

    public void testVpnIconIsDisplayed() {
        assertTrue(isShownWithinConfinesOfVisibleScreen(vpn_controller.getVpnWholeIcon()));
    }
    public void testVpnButtonIsDisplayed() {
        assertTrue(isShownWithinConfinesOfVisibleScreen(vpn_controller.getVpnButton()));
    }
}
