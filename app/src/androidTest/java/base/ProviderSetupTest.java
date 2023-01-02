package base;


import static android.content.Context.MODE_PRIVATE;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.anything;

import static se.leap.bitmaskclient.base.models.Constants.SHARED_PREFERENCES;

import android.content.SharedPreferences;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.StartActivity;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.providersetup.ProviderListActivity;
import se.leap.bitmaskclient.providersetup.activities.ProviderSetupBaseActivity;
import se.leap.bitmaskclient.testutils.ForceLocaleRule;
import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;
import tools.fastlane.screengrab.locale.LocaleTestRule;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class ProviderSetupTest {

    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Rule
    public ActivityScenarioRule<ProviderListActivity> mActivityScenarioRule =
            new ActivityScenarioRule<>(ProviderListActivity.class);

    @Before
    public void setup() {
        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        preferences.edit().clear().commit();
    }

    @Test
    public void testConfigureRiseupVPNScreenshot() {
        Screengrab.screenshot("configureRiseupVPN_before_button_click");
        onData(anything()).inAdapterView(withId(R.id.provider_list)).atPosition(2).perform(click());
        Screengrab.screenshot("configureRiseupVPN_after_button_click");
    }

    @Test
    public void testaddManuallyNewProviderScreenshot() {
        Screengrab.screenshot("addManuallyNewProvider_before_button_click");
        onData(anything()).inAdapterView(withId(R.id.provider_list)).atPosition(3).perform(click());
        Screengrab.screenshot("addManuallyNewProvider_after_button_click");
    }
}
