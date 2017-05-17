package se.leap.bitmaskclient.test;

import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

import com.robotium.solo.*;

import junit.framework.AssertionFailedError;

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

    protected void clickVpnButton() throws IllegalStateException {
        Button button = getVpnButton();
        if(!isVpnButton(button))
            throw new IllegalStateException();
        solo.clickOnView(button);
    }

    protected Button getVpnButton() {
        try {
            View button_view = solo.getView(R.id.vpn_main_button);
            if (button_view != null)
                return (Button) button_view;
            else
                return new Button(solo.getCurrentActivity());
        } catch (AssertionFailedError e) {
            return new Button(solo.getCurrentActivity());
        }
    }

    private boolean isVpnButton(Button button) {
        return !button.getText().toString().isEmpty();
    }

    protected FabButton getVpnWholeIcon() {
        View view = solo.getView(R.id.vpn_Status_Image);
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
        solo.waitForCondition(condition, max_seconds_until_connected * 1000);
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
        return iconEquals(iconConnectedDrawable());
    }

    protected boolean iconShowsDisconnected() {
        return iconEquals(iconDisconnectedDrawable());
    }

    private boolean iconEquals(Drawable drawable) {
        Bitmap inside_icon = getVpnInsideIcon();
        if(inside_icon != null)
            return inside_icon.equals(drawable);
        else
            return false;

    }

    private Drawable iconConnectedDrawable() {
        return getDrawable(R.drawable.ic_stat_vpn);
    }

    private Drawable iconDisconnectedDrawable() {
        return getDrawable(R.drawable.ic_stat_vpn_offline);
    }

    private Drawable getDrawable(int resId) {
        return solo.getCurrentActivity().getResources().getDrawable(resId);
    }

    private Bitmap getVpnInsideIcon() {
        FabButton whole_icon = getVpnWholeIcon();

        CircleImageView a;
        a = whole_icon != null ?
                (CircleImageView) getVpnWholeIcon().findViewById(R.id.fabbutton_circle)
                : new CircleImageView(solo.getCurrentActivity());
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
                return iconShowsDisconnected();
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
