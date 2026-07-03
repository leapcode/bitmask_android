package se.leap.bitmaskclient;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertNotNull;
import static utils.ProviderSetupUtils.isVerboseScreenshots;

import android.Manifest;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Build;
import android.view.Gravity;

import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.espresso.contrib.DrawerMatchers;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObjectNotFoundException;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import de.blinkt.openvpn.core.VpnStatus;
import se.leap.bitmaskclient.base.MainActivity;
import se.leap.bitmaskclient.base.StartActivity;
import se.leap.bitmaskclient.providersetup.activities.SetupActivity;
import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;
import tools.fastlane.screengrab.locale.LocaleTestRule;
import utils.CustomInteractions;
import utils.ProviderSetupUtils;

@LargeTest
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BitmaskTest {

    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Rule
    public GrantPermissionRule notificationPermissionRule = (Build.VERSION.SDK_INT >= 33) ? GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS) : null;

    UiDevice device;
    @Before
    public void setup() {
        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(instrumentation);
    }

    @Test
    public void test01_vpnStartTest() throws InterruptedException, UiObjectNotFoundException {
        startMainActivity();

        ViewInteraction mainButton = null;
        if (!VpnStatus.isVPNActive()) {
            mainButton = CustomInteractions.tryResolve(
                    onView(Matchers.allOf(
                            withId(R.id.button)//,
                            /*ViewMatchers.withTagValue(Matchers.is("button_circle_start"))*/)),
                    ViewAssertions.matches(ViewMatchers.isDisplayed()),
                    20);
            // turn VPN on
            mainButton.perform(ViewActions.click());

            // resolve connecting state
            CustomInteractions.tryResolve(
                    onView(Matchers.allOf(
                            withId(R.id.button),
                            ViewMatchers.withTagValue(Matchers.is("button_circle_cancel")))),
                    ViewAssertions.matches(ViewMatchers.isDisplayed()),
                    3, "attempt to resolve connecting state");
        }

        mainButton = CustomInteractions.tryResolve(
                onView(Matchers.allOf(
                        withId(R.id.button),
                        ViewMatchers.withTagValue(Matchers.is("button_circle_stop")))),
                ViewAssertions.matches(ViewMatchers.isDisplayed()),
                20, "attempt to resolve connected state");
        Screengrab.screenshot("01_VPN_connected");

        // turn VPN off
        mainButton.perform(ViewActions.click());
        if (isVerboseScreenshots()) {
            Screengrab.screenshot("031_VPN_ask_disconnect");
        }

        onView(ViewMatchers.withText(android.R.string.yes))
                .inRoot(RootMatchers.isDialog())
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(ViewActions.click());
        Screengrab.screenshot("03_VPN_disconnected");
    }

    @Test
    public void test02_vpnConnectingStateTest() throws InterruptedException, UiObjectNotFoundException {
        startMainActivity();

        ViewInteraction mainButton = CustomInteractions.tryResolve(
                onView(Matchers.allOf(
                        withId(R.id.button),
                        ViewMatchers.withTagValue(Matchers.is("button_circle_start")))),
                ViewAssertions.matches(ViewMatchers.isDisplayed()),
                20);
        // turn VPN on
        mainButton.perform(ViewActions.click());

        // resolve connecting state
        CustomInteractions.tryResolve(
                onView(Matchers.allOf(
                        withId(R.id.button),
                        ViewMatchers.withTagValue(Matchers.is("button_circle_cancel")))),
                ViewAssertions.matches(ViewMatchers.isDisplayed()),
                3, "attempt to resolve connecting state");
        Screengrab.screenshot("02_VPN_connecting");

        CustomInteractions.tryResolve(
                onView(Matchers.allOf(
                        withId(R.id.button),
                        ViewMatchers.withTagValue(Matchers.is("button_circle_stop")))),
                ViewAssertions.matches(ViewMatchers.isDisplayed()),
                20, "attempt to resolve connected state");
    }

    @Test
    public void test02_SettingsFragmentScreenshots() {
        startMainActivity();
        onView(withId(R.id.drawer_layout))
                .check(ViewAssertions.matches(DrawerMatchers.isClosed(Gravity.LEFT))) // Left Drawer should be closed.
                .perform(DrawerActions.open()); // Open Drawer

        Screengrab.screenshot("05_navigationDrawer");

        // Start the screen of your activity.
        onView(withId(R.id.advancedSettings))
                .perform(ViewActions.click());

        Screengrab.screenshot("06_settingsFragment");
    }

    @Test
    public void test03_LocationSelectionFragmentScreenshots() {
        startMainActivity();
        onView(withId(R.id.drawer_layout))
                .check(ViewAssertions.matches(DrawerMatchers.isClosed(Gravity.LEFT))) // Left Drawer should be closed.
                .perform(DrawerActions.open()); // Open Drawer

        onView(withId(R.id.manualGatewaySelection))
                .perform(ViewActions.click());

        Screengrab.screenshot("07_GatewaySelectionFragment");
    }

    @Test
    public void test04_LanguageSelectionFragmentScreenshots() {
        startMainActivity();
        onView(withId(R.id.drawer_layout))
                .check(ViewAssertions.matches(DrawerMatchers.isClosed(Gravity.LEFT))) // Left Drawer should be closed.
                .perform(DrawerActions.open()); // Open Drawer

        onView(withId(R.id.language_switcher))
                .perform(ViewActions.click());

        Screengrab.screenshot("08_LanguageSelectionFragment");
    }

    @Test
    public void test05_AppExclusionFragmentScreenshots() {
        startMainActivity();
        onView(withId(R.id.drawer_layout))
                .check(ViewAssertions.matches(DrawerMatchers.isClosed(Gravity.LEFT))) // Left Drawer should be closed.
                .perform(DrawerActions.open()); // Open Drawer

        onView(withId(R.id.advancedSettings)).perform(ViewActions.click());
        onView(withId(R.id.exclude_apps)).perform(ViewActions.click());

        CustomInteractions.tryResolve(
                onView(Matchers.allOf(
                        withId(R.id.list),
                        withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
                )).perform(RecyclerViewActions.scrollToPosition(0)),
                ViewAssertions.matches(ViewMatchers.isDisplayed()),
                30, "waiting until app list appears");

        Screengrab.screenshot("09_App_Exclusion_Fragment");
    }

    private void startMainActivity() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor setupActivityMonitor = new Instrumentation.ActivityMonitor(SetupActivity.class.getName(), null, false);
        Instrumentation.ActivityMonitor mainActivityMonitor = new Instrumentation.ActivityMonitor(MainActivity.class.getName(), null, false);
        instrumentation.addMonitor(setupActivityMonitor);
        instrumentation.addMonitor(mainActivityMonitor);
        Intent intent = new Intent(instrumentation.getTargetContext(), StartActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        instrumentation.startActivitySync(intent);
        Activity setupActivity = instrumentation.waitForMonitorWithTimeout(setupActivityMonitor, 1000L);
        if (setupActivity != null) {
            ProviderSetupUtils.runProviderSetup(device, false, false, InstrumentationRegistry.getInstrumentation().getTargetContext());
        }
        Activity mainActivity = instrumentation.waitForMonitorWithTimeout(mainActivityMonitor, 1000L);
        assertNotNull(mainActivity);
    }
}
