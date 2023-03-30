package se.leap.bitmaskclient.base;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static utils.CustomInteractions.tryResolve;

import androidx.test.espresso.ViewInteraction;

import org.junit.Test;

import se.leap.bitmaskclient.R;
import tools.fastlane.screengrab.Screengrab;

public class CustomProviderTest extends ProviderBaseTest {

    @Test
    @Override
    public void test01_vpnStartTest() throws InterruptedException {
        ViewInteraction mainButtonStop;
        mainButtonStop = tryResolve(
                onView(withId(R.id.main_button)),
                matches(isDisplayed()),
                30);
        Screengrab.screenshot("VPN_connected");

        mainButtonStop.perform(click());
        Screengrab.screenshot("VPN_ask_disconnect");

        ViewInteraction alertDialogOKbutton = tryResolve(onView(withText(android.R.string.yes))
                .inRoot(isDialog()),
                matches(isDisplayed()));
        alertDialogOKbutton.perform(click());
        Screengrab.screenshot("VPN_disconnected");

        mainButtonStop.perform(click());
        Thread.sleep(50);
        Screengrab.screenshot("VPN_connecting");
    }

    @Override
    public boolean configureProviderIfNeeded() {
        return false;
    }
}