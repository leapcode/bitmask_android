package se.leap.bitmaskclient.test;

import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

import com.robotium.solo.*;

import de.blinkt.openvpn.activities.*;
import mbanje.kurt.fabbutton.*;
import se.leap.bitmaskclient.R;

public class VpnTestController {

    private final Solo solo;

    public VpnTestController(Solo solo) {
        this.solo = solo;
    }

    protected void turnVpnOndAndOff(String provider) {
        clickVpnButton();
        turningEipOn();
        clickVpnButton();
        turningEipOff();
    }

    protected void clickVpnButton() {
        solo.clickOnView(getVpnButton());
    }

    protected Button getVpnButton() {
        return (Button) solo.getView(R.id.vpn_main_button);
    }

    protected FabButton getVpnWholeIcon() {
        return (FabButton) solo.getView(R.id.vpn_Status_Image);
    }

    protected void turningEipOn() {
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
        BaseTestDashboard.isShownWithinConfinesOfVisibleScreen(a);
    }

    private boolean iconConnected() {
        return getVpnInsideIcon().equals(getDrawable(R.drawable.ic_stat_vpn));
    }

    private boolean iconDisconnected() {
        return getVpnInsideIcon().equals(getDrawable(R.drawable.ic_stat_vpn_offline));
    }

    private Drawable getDrawable(int resId) {
        return solo.getCurrentActivity().getResources().getDrawable(resId);
    }

    private Bitmap getVpnInsideIcon() {
        CircleImageView a = (CircleImageView) getVpnWholeIcon().findViewById(R.id.fabbutton_circle);
        a.setDrawingCacheEnabled(true);
        return a.getDrawingCache();
    }

    protected void turningEipOff() {
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

    private void sayOkToDisconnect() throws IllegalStateException {
        boolean disconnect_vpn_appeared = solo.waitForActivity(DisconnectVPN.class);
        if(disconnect_vpn_appeared)
            clickYes();
        else throw new IllegalStateException();
    }

    void sleepSeconds(int seconds) {
        solo.sleep(seconds * 1000);
    }
}
