package se.leap.bitmaskclient.base;

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
import static org.hamcrest.Matchers.is;
import static utils.CustomInteractions.tryResolve;

import android.app.Instrumentation;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.view.Gravity;

import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;
import tools.fastlane.screengrab.locale.LocaleTestRule;

@LargeTest
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class ProviderBaseTest {

    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Rule
    public ActivityScenarioRule<StartActivity> mActivityScenarioRule =
            new ActivityScenarioRule<>(StartActivity.class);

    UiDevice device;
    @Before
    public void setup() {
        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());
        SharedPreferences preferences = PreferenceHelper.getSharedPreferences(getApplicationContext());
        preferences.edit().clear().commit();
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(instrumentation);
    }

    @Test
    public void test01_vpnStartTest() throws InterruptedException, UiObjectNotFoundException {
        boolean configurationNeeded = configureProviderIfNeeded();

        ViewInteraction mainButtonStop;
        if (!configurationNeeded) {
            // click on Main on/off button and start VPN
            ViewInteraction mainButton = tryResolve(
                    onView(withId(R.id.main_button)),
                    matches(isDisplayed())
            );

            mainButton.perform(click());
            Thread.sleep(50);
            Screengrab.screenshot("VPN_connecting");

            mainButtonStop = tryResolve(
                    onView(allOf(
                            withId(R.id.button),
                            withTagValue(is("button_circle_stop")))),
                    matches(isDisplayed()),
                    20);
            Screengrab.screenshot("VPN_connected");
        } else {
            // handle VPN permission dialog
            if (VpnService.prepare(getApplicationContext()) != null) {
                UiObject okButton = device.findObject(new UiSelector().packageName("com.android.vpndialogs").resourceId("android:id/button1"));
                okButton.click();
            }
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

    public abstract boolean configureProviderIfNeeded();
}
