package se.leap.bitmaskclient.base;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertNotNull;
import static se.leap.bitmaskclient.base.MainActivity.ACTION_SHOW_VPN_FRAGMENT;
import static utils.ProviderSetupUtils.runProviderSetup;

import android.Manifest;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.Gravity;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.espresso.contrib.DrawerMatchers;
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

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.models.ProviderObservable;
import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;
import tools.fastlane.screengrab.locale.LocaleTestRule;
import utils.CustomInteractions;

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
    public void test03_vpnStartTest() throws InterruptedException, UiObjectNotFoundException {
        startMainActivity();

        Screengrab.screenshot("VPN_connecting");
        ViewInteraction mainButtonStop = CustomInteractions.tryResolve(
                Espresso.onView(Matchers.allOf(
                        ViewMatchers.withId(R.id.button),
                        ViewMatchers.withTagValue(Matchers.is("button_circle_stop")))),
                ViewAssertions.matches(ViewMatchers.isDisplayed()),
                20);
        Screengrab.screenshot("VPN_connected");

        mainButtonStop.perform(ViewActions.click());
        Screengrab.screenshot("VPN_ask_disconnect");

        Espresso.onView(ViewMatchers.withText(android.R.string.yes))
                .inRoot(RootMatchers.isDialog())
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(ViewActions.click());
        Screengrab.screenshot("VPN_disconnected");
    }

    @Test
    public void test04_SettingsFragmentScreenshots() {
        startMainActivity();
        Espresso.onView(ViewMatchers.withId(R.id.drawer_layout))
                .check(ViewAssertions.matches(DrawerMatchers.isClosed(Gravity.LEFT))) // Left Drawer should be closed.
                .perform(DrawerActions.open()); // Open Drawer

        Screengrab.screenshot("navigationDrawer");

        // Start the screen of your activity.
        Espresso.onView(ViewMatchers.withId(R.id.advancedSettings))
                .perform(ViewActions.click());

        Screengrab.screenshot("settingsFragment");
    }

    @Test
    public void test05_LocationSelectionFragmentScreenshots() {
        startMainActivity();
        Espresso.onView(ViewMatchers.withId(R.id.drawer_layout))
                .check(ViewAssertions.matches(DrawerMatchers.isClosed(Gravity.LEFT))) // Left Drawer should be closed.
                .perform(DrawerActions.open()); // Open Drawer

        Espresso.onView(ViewMatchers.withId(R.id.manualGatewaySelection))
                .perform(ViewActions.click());

        Screengrab.screenshot("GatewaySelectionFragment");
    }

    @Test
    public void test06_AppExclusionFragmentScreenshots() {
        startMainActivity();
        Espresso.onView(ViewMatchers.withId(R.id.drawer_layout))
                .check(ViewAssertions.matches(DrawerMatchers.isClosed(Gravity.LEFT))) // Left Drawer should be closed.
                .perform(DrawerActions.open()); // Open Drawer

        Espresso.onView(ViewMatchers.withId(R.id.advancedSettings)).perform(ViewActions.click());

        Espresso.onView(ViewMatchers.withId(R.id.exclude_apps)).perform(ViewActions.click());

        CustomInteractions.tryResolve(
                Espresso.onData(Matchers.anything()).inAdapterView(ViewMatchers.withId(android.R.id.list)).atPosition(2),
                ViewAssertions.matches(ViewMatchers.isDisplayed()),
                5);

        Screengrab.screenshot("App_Exclusion_Fragment");
    }

    private void startMainActivity() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = new Instrumentation.ActivityMonitor(MainActivity.class.getName(), null, false);
        instrumentation.addMonitor(monitor);
        Intent intent = new Intent(instrumentation.getTargetContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(ACTION_SHOW_VPN_FRAGMENT);
        instrumentation.startActivitySync(intent);
        Activity activity = instrumentation.waitForMonitor(monitor);
        assertNotNull(activity);
        configureIfNeeded();
    }

    private void configureIfNeeded() {
        if (ProviderObservable.getInstance().getCurrentProvider().isConfigured()) {
            return;
        }
        final Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        try {
            onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
        } catch (PerformException performException) {
            System.out.println("navigation drawer already opened");
        }
        onView(withText(R.string.switch_provider_menu_option)).perform(click());
        System.out.println("configure Provider... starting SetupActivity");

        runProviderSetup(device, false, false, targetContext);
    }
}
