package se.leap.bitmaskclient.test;

import android.view.View;
import android.widget.Button;

import com.robotium.solo.Condition;
import com.robotium.solo.Solo;

import de.blinkt.openvpn.activities.DisconnectVPN;
import mbanje.kurt.fabbutton.ProgressRingView;
import se.leap.bitmaskclient.R;

import static junit.framework.Assert.assertTrue;

public class VpnTestController {

    private final Solo solo;

    public VpnTestController(Solo solo) {
        this.solo = solo;
    }

    protected void turnVpnOnAndOff() {
        clickVpnButton();
        turningEipOn();
        clickVpnButton();
        turningEipOff();
    }

    protected void clickVpnButton() throws IllegalStateException {
        Button button = getVpnButton();
        if(!isVpnButton(button))
            throw new IllegalStateException();
        solo.clickOnButton(String.valueOf(button.getText()));
    }

    protected Button getVpnButton() {
        View button_view = solo.getView(R.id.gateway_location_button);
        if (button_view != null)
            return (Button) button_view;
        else
            return null;
    }

    private boolean isVpnButton(Button button) {
        return button != null && !button.getText().toString().isEmpty();
    }

    protected FabButton getVpnWholeIcon() {
        assertTrue(solo.waitForActivity(Dashboard.class, 5 * 1000));

        View view = solo.getView(R.id.vpn_status_image);
        if (view != null)
            return (FabButton) view;
        else
            return null;
    }

    protected void turningEipOn() {
        assertInProgress();
        int max_seconds_until_connected = 120;

        Condition condition = new Condition() {
            @Override
            public boolean isSatisfied() {
                return iconShowsConnected();
            }
        };
        assertTrue("condition iconShowsConnected not fulfilled within " + max_seconds_until_connected + " seconds." , solo.waitForCondition(condition, max_seconds_until_connected * 1000));
        sleepSeconds(2);
    }

    private void assertInProgress() {
        FabButton whole_icon = getVpnWholeIcon();
        ProgressRingView a;
        a = whole_icon != null ?
                (ProgressRingView) getVpnWholeIcon().findViewById(R.id.fabbutton_ring) :
                new ProgressRingView(solo.getCurrentActivity());
        BaseTestDashboardFragment.isShownWithinConfinesOfVisibleScreen(a);
    }

    private boolean iconShowsConnected() {
        View vpnIconView = getVpnWholeIcon();
        return vpnIconView.getTag().equals(R.drawable.ic_stat_vpn);
    }

    protected boolean iconShowsDisconnected() {
        View vpnIconView = getVpnWholeIcon();
        return vpnIconView.getTag().equals(R.drawable.ic_stat_vpn_offline);
    }

    protected void turningEipOff() {
        okToBrowserWarning();
        int max_seconds_until_connected = 120;

         Condition condition = new Condition() {
            @Override
            public boolean isSatisfied() {
                return iconShowsDisconnected();
            }
        };
        assertTrue(solo.waitForCondition(condition, max_seconds_until_connected * 1000));
        sleepSeconds(2);
    }

    private void okToBrowserWarning() {
        assertTrue(solo.waitForDialogToOpen());
        clickYes();
        solo.waitForDialogToClose();
    }

    private void clickYes() {
        String yes = solo.getString(android.R.string.yes);
        solo.clickOnButton(yes);
    }

    private void clickDisconnect() {
        String disconnect = solo.getString(R.string.cancel_connection);
        solo.clickOnButton(disconnect);
    }

    @SuppressWarnings("unused")
    private void sayOkToDisconnect() throws IllegalStateException {
        boolean disconnect_vpn_appeared = solo.waitForActivity(DisconnectVPN.class);
        if(disconnect_vpn_appeared){
            clickDisconnect();
            solo.waitForDialogToClose();
        }
        else throw new IllegalStateException();
    }

    void sleepSeconds(int seconds) {
        solo.sleep(seconds * 1000);
    }
}
