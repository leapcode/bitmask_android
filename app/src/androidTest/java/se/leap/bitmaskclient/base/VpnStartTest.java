package se.leap.bitmaskclient.base;


import static android.content.Context.MODE_PRIVATE;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.DrawerMatchers.isClosed;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withTagValue;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static se.leap.bitmaskclient.base.models.Constants.SHARED_PREFERENCES;
import static utils.CustomInteractions.tryResolve;

import android.content.SharedPreferences;
import android.view.Gravity;

import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import se.leap.bitmaskclient.R;
import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;
import tools.fastlane.screengrab.locale.LocaleTestRule;

@LargeTest
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class VpnStartTest {

    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Rule
    public ActivityScenarioRule<StartActivity> mActivityScenarioRule =
            new ActivityScenarioRule<>(StartActivity.class);

    @Before
    public void setup() {
        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        preferences.edit().clear().commit();
    }

    @Test
    public void test01_vpnStartTest() {
        boolean configurationNeeded = configureProviderIfNeeded();

        ViewInteraction mainButtonStop;
        if (!configurationNeeded) {
            // click on Main on/off button and start VPN
            ViewInteraction mainButton = tryResolve(
                    onView(withId(R.id.main_button)),
                    matches(isDisplayed())
            );

            mainButton.perform(click());
            tryResolve(
                    onView(allOf(
                            withId(R.id.button),
                            withTagValue(is("button_circle_cancel")))),
                    matches(isDisplayed()),
                    2);
            Screengrab.screenshot("VPN_connecting");

            mainButtonStop = tryResolve(
                    onView(allOf(
                            withId(R.id.button),
                            withTagValue(is("button_circle_stop")))),
                    matches(isDisplayed()),
                    20);
            Screengrab.screenshot("VPN_connected");
        } else {
            // on new configurations the VPN is automatically started
            Screengrab.screenshot("VPN_connecting");
            mainButtonStop = tryResolve(
                    onView(allOf(
                                    withId(R.id.button),
                                    withTagValue(is("button_circle_stop")))),
                    matches(isDisplayed()),
                    20);
            Screengrab.screenshot("VPN_connected");
        }

        mainButtonStop.perform(click());
        Screengrab.screenshot("VPN_ask_disconnect");

        onView(withText(android.R.string.yes))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
                .perform(click());
        Screengrab.screenshot("VPN_disconnected");
    }

    @Test
    public void test02_SettingsFragmentScreenshots() {
        onView(withId(R.id.drawer_layout))
                .check(matches(isClosed(Gravity.LEFT))) // Left Drawer should be closed.
                .perform(DrawerActions.open()); // Open Drawer

        Screengrab.screenshot("navigationDrawer");

        // Start the screen of your activity.
        onView(withId(R.id.advancedSettings))
                .perform(click());

        Screengrab.screenshot("settingsFragment");
    }

    @Test
    public void test03_LocationSelectionFragmentScreenshots() {
        onView(withId(R.id.drawer_layout))
                .check(matches(isClosed(Gravity.LEFT))) // Left Drawer should be closed.
                .perform(DrawerActions.open()); // Open Drawer

        onView(withId(R.id.manualGatewaySelection))
                .perform(click());

        Screengrab.screenshot("GatewaySelectionFragment");
    }

    @Test
    public void test04_AppExclusionFragmentScreenshots() {
        onView(withId(R.id.drawer_layout))
                .check(matches(isClosed(Gravity.LEFT))) // Left Drawer should be closed.
                .perform(DrawerActions.open()); // Open Drawer

        onView(withId(R.id.advancedSettings)).perform(click());

        onView(withId(R.id.exclude_apps)).perform(click());

        tryResolve(
                onData(anything()).inAdapterView(withId(android.R.id.list)).atPosition(2),
                matches(isDisplayed()),
                5);

        Screengrab.screenshot("App_Exclusion_Fragment");
    }

    public boolean configureProviderIfNeeded() {
        try {
            DataInteraction linearLayout = tryResolve(onData(hasToString(containsString("riseup.net")))
                            .inAdapterView(withId(R.id.provider_list)),
                    2);
            linearLayout.perform(click());
            return true;
        } catch (NoMatchingViewException e) {
            // it might be that the provider was already configured, so we print the stack
            // trace here and try to continue
            e.printStackTrace();
        }
        return false;
    }
}
