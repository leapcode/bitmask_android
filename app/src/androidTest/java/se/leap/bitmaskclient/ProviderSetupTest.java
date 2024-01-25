package se.leap.bitmaskclient;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertNotNull;
import static se.leap.bitmaskclient.base.models.Constants.SHARED_ENCRYPTED_PREFERENCES;
import static utils.CustomInteractions.tryResolve;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;

import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import se.leap.bitmaskclient.base.MainActivity;
import se.leap.bitmaskclient.base.StartActivity;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.models.ProviderObservable;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
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

  //  @Rule
  //  public ActivityScenarioRule<SetupActivity> mActivityScenarioRule =
  //          new ActivityScenarioRule<>(SetupActivity.class);

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
        startSetupActivity();
        ProviderSetupUtils.runProviderSetup(device, true, false, InstrumentationRegistry.getInstrumentation().getTargetContext());
    }

    @Test
    public void test02_setupProviderCircumvention() {
        startSetupActivity();
        ProviderSetupUtils.runProviderSetup(device, true, true, InstrumentationRegistry.getInstrumentation().getTargetContext());
    }

    @Test
    public void test03_addManuallyNewProviderScreenshot() {
        if (!"normal".equals(BuildConfig.FLAVOR_branding)) {
            System.out.println("skipping custom provider url test");
            return;
        }
        startSetupActivity();
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        ViewInteraction radioButtonSelection = tryResolve(onView(withText(R.string.add_provider)), matches(isDisplayed()));
        radioButtonSelection.perform(click());
        onView(withId(R.id.edit_customProvider)).perform(replaceText("https://leapvpn.myserver.org"));
        tryResolve(onView(withId(R.id.edit_customProvider)), matches(withText("https://leapvpn.myserver.org")));
        Screengrab.screenshot("setup_custom_provider");
        onView(withId(R.id.setup_next_button)).perform(click());

        onView(withText(context.getString(R.string.use_standard_vpn, context.getString(R.string.app_name)))).perform(click());
        onView(withId(R.id.setup_next_button)).perform(click());

        tryResolve(
                onView(withText(context.getString(R.string.malformed_url, context.getString(R.string.app_name)))),
                matches(isDisplayed()),
                20);
        Screengrab.screenshot("setup_provider_error_dialog");
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
