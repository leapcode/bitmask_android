package se.leap.bitmaskclient;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertNotNull;
import static se.leap.bitmaskclient.base.models.Constants.SHARED_ENCRYPTED_PREFERENCES;
import static utils.CustomInteractions.tryResolve;
import static utils.ProviderSetupUtils.isVerboseScreenshots;

import android.Manifest;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObjectNotFoundException;

import org.hamcrest.Matchers;
import org.junit.Assume;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import se.leap.bitmaskclient.base.StartActivity;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.models.ProviderObservable;
import se.leap.bitmaskclient.providersetup.activities.SetupActivity;
import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;
import tools.fastlane.screengrab.locale.LocaleTestRule;
import utils.ProviderSetupUtils;

@LargeTest
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProviderSetupTest {

    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Rule
    public GrantPermissionRule notificationPermissionRule = (Build.VERSION.SDK_INT >= 33) ? GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS) : null;

    UiDevice device;

    @Before
    public void setup() {
        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());
        //PreferenceHelper.clear();
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(instrumentation);
        instrumentation.getTargetContext().deleteSharedPreferences(SHARED_ENCRYPTED_PREFERENCES);
        ProviderObservable.getInstance().updateProvider(new Provider());
    }

    @Test
    public void test01_setupProviderDefault() {
        // Assume the branding is not "custom" to skip this test when it is.
        Assume.assumeFalse("custom".equals(BuildConfig.FLAVOR_branding));
        startSetupActivity();
        ProviderSetupUtils.runProviderSelection(true);
    }

    @Test
    public void test02_setupProviderCircumvention() {
        startSetupActivity();
        ProviderSetupUtils.runProviderSelection(false);
        ProviderSetupUtils.runCircumventionSelection(isVerboseScreenshots(), true, false, InstrumentationRegistry.getInstrumentation().getTargetContext());
    }

    @Test
    public void test03_addManuallyNewProviderScreenshot() throws UiObjectNotFoundException {
        if (!"normal".equals(BuildConfig.FLAVOR_branding)) {
            System.out.println("skipping custom provider url test");
            return;
        }
        startSetupActivity();

        ViewInteraction radioButtonSelection = tryResolve(onView(withText(R.string.add_provider)), matches(isDisplayed()));
        radioButtonSelection.perform(click());
        onView(withId(R.id.edit_customProvider)).perform(replaceText("https://leapvpn.myserver.org"));
        tryResolve(onView(withId(R.id.edit_customProvider)), matches(withText("https://leapvpn.myserver.org")));
        Screengrab.screenshot("043_setup_custom_provider");
        Screengrab.screenshot("043_setup_custom_provider");

    }

    @Test
    public void test04_addInviteCode() throws UiObjectNotFoundException {
        if (!"normal".equals(BuildConfig.FLAVOR_branding)) {
            System.out.println("skipping custom provider url test");
            return;
        }
        startSetupActivity();

        ViewInteraction radioButtonSelection = tryResolve(onView(withText(R.string.enter_invite_code)), matches(isDisplayed()));
        radioButtonSelection.perform(click());
        onView(withId(R.id.edit_customProvider)).perform(replaceText("obfsvpnintro://13.12.2.161:443/?cert=faXUc56JJAJP%2B0Gc2zHZX0I2RNXuwR0jz937PrpR%2FNopWkuJFkBQwN%2Bzm4ib%2BXvXxXxXxX&fqdn=mysecret.vpnserver.org&kcp=0&auth=_zYOreyzAh+x79j9Ab0DU3A=="));

        tryResolve(onView(
                Matchers.allOf(
                        withId(R.id.syntax_check_result),
                        withText(R.string.validation_status_success))
                ), matches(isDisplayed()), 3);
        tryResolve(onView(
                Matchers.allOf(
                        withId(R.id.setup_next_button),
                        isEnabled())
        ), matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)), 3);
        Screengrab.screenshot("044_setup_invite_code_provider");
        Screengrab.screenshot("044_setup_invite_code_provider");

    }

    private void startSetupActivity() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor setupActivityMonitor = new Instrumentation.ActivityMonitor(SetupActivity.class.getName(), null, false);
        instrumentation.addMonitor(setupActivityMonitor);
        Intent intent = new Intent(instrumentation.getTargetContext(), StartActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        instrumentation.startActivitySync(intent);
        Activity setupActivity = instrumentation.waitForMonitorWithTimeout(setupActivityMonitor, 1000L);
        assertNotNull(setupActivity);
    }
}
