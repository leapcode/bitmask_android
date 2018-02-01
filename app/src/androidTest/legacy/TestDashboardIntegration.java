package se.leap.bitmaskclient.test;

import java.io.*;

import se.leap.bitmaskclient.R;

public class TestDashboardIntegration extends BaseTestDashboardFragment {

    @Override
    protected void tearDown() throws Exception {
        solo.finishOpenedActivities();
    }

    public void testSwitchProvider() {
        tapSwitchProvider();
        solo.goBack();
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

    private void turnNetworkOff() {
        ConnectionManager.setMobileDataEnabled(false, context);
        if (!solo.waitForText(getActivity().getString(R.string.eip_state_not_connected), 1, 15 * 1000))
            fail();
    }

    private void restartAdbServer() {
        runAdbCommand("kill-server");
        runAdbCommand("start-server");
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
