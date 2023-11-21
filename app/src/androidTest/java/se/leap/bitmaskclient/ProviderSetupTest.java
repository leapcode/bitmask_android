package se.leap.bitmaskclient;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withTagValue;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static utils.CustomInteractions.tryResolve;

import android.app.Instrumentation;
import android.content.Context;

import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObjectNotFoundException;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runner.manipulation.Ordering;

import se.leap.bitmaskclient.base.fragments.MainActivityErrorDialog;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.providersetup.activities.SetupActivity;
import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;
import tools.fastlane.screengrab.locale.LocaleTestRule;
import utils.ProviderSetupUtils;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class ProviderSetupTest {

    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Rule
    public ActivityScenarioRule<SetupActivity> mActivityScenarioRule =
            new ActivityScenarioRule<>(SetupActivity.class);

    UiDevice device;

    @Before
    public void setup() {
        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());
        //PreferenceHelper.clear();
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(instrumentation);
    }

    @Test
    public void test01_setupProviderDefault() {
        ProviderSetupUtils.runProviderSetup(device, true, false, InstrumentationRegistry.getInstrumentation().getTargetContext());
    }

    @Test
    public void test01_setupProviderCircumvention() {
        ProviderSetupUtils.runProviderSetup(device, true, true, InstrumentationRegistry.getInstrumentation().getTargetContext());
    }

    @Test
    public void testaddManuallyNewProviderScreenshot() {
        if (!"normal".equals(BuildConfig.FLAVOR_branding)) {
            System.out.println("skipping custom provider url test");
            return;
        }
        ViewInteraction radioButtonSelection = tryResolve(onView(withText(R.string.add_provider)), matches(isDisplayed()));
        radioButtonSelection.perform(click());

        onView(withId(R.id.edit_customProvider)).perform(replaceText("https://leapvpn.myserver.org"));
        Screengrab.screenshot("setup_custom_provider");
        onView(withId(R.id.setup_next_button)).perform(click());
        onView(withId(R.id.setup_next_button)).perform(click());
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        tryResolve(
                onView(withText(context.getString(R.string.malformed_url, context.getString(R.string.app_name)))),
                matches(isDisplayed()),
                20);
        Screengrab.screenshot("setup_provider_error_dialog");
    }
}
