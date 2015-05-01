package se.leap.bitmaskclient.test;

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

    public void testVpnEveryProvider() {
        String[] providers = {"demo.bitmask.net", "riseup.net", "calyx.net"};
        for(String provider : providers) {
            changeProvider(provider);
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
