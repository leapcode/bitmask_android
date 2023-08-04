package se.leap.bitmaskclient.base;


import static android.content.Context.MODE_PRIVATE;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasToString;
import static se.leap.bitmaskclient.base.models.Constants.SHARED_PREFERENCES;
import static utils.CustomInteractions.tryResolve;

import android.content.SharedPreferences;

import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.providersetup.ProviderListActivity;
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
        SharedPreferences preferences = PreferenceHelper.getSharedPreferences(getApplicationContext());
        preferences.edit().clear().commit();
    }

    @Test
    public void testConfigureRiseupVPNScreenshot() {
        DataInteraction linearLayout = tryResolve(onData(hasToString(containsString("riseup.net")))
                            .inAdapterView(withId(R.id.provider_list)),
                    2);
        Screengrab.screenshot("ProviderListActivity");
        linearLayout.perform(click());
        Screengrab.screenshot("ProviderListActivity_configureRiseup");
    }

    @Test
    public void testaddManuallyNewProviderScreenshot() {
        onData(anything()).inAdapterView(withId(R.id.provider_list)).atPosition(3).perform(click());
        Screengrab.screenshot("ProviderListActivity_addManuallyNewProvider");
    }
}
