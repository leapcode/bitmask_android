package se.leap.bitmaskclient.test;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import se.leap.bitmaskclient.MainActivity;
import se.leap.bitmaskclient.Provider;
import se.leap.bitmaskclient.testutils.TestSetupHelper;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerMatchers.isClosed;
import static android.support.test.espresso.contrib.DrawerMatchers.isOpen;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.anything;
import static se.leap.bitmaskclient.Constants.SHARED_PREFERENCES;
import static se.leap.bitmaskclient.MainActivity.ACTION_SHOW_VPN_FRAGMENT;
import static se.leap.bitmaskclient.R.id.aboutLayout;
import static se.leap.bitmaskclient.R.id.accountList;
import static se.leap.bitmaskclient.R.id.configuration_wizard_layout;
import static se.leap.bitmaskclient.R.id.drawer_layout;
import static se.leap.bitmaskclient.R.id.eipServiceFragment;
import static se.leap.bitmaskclient.R.id.log_layout;
import static se.leap.bitmaskclient.R.id.settingsList;

/**
 * Created by cyberta on 19.01.18.
 */

@RunWith(AndroidJUnit4.class)
public class StartActivityDrawerTest {

    Intent intent;
    SharedPreferences preferences;
    SharedPreferences.Editor preferencesEditor;

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(
            MainActivity.class,
            true,
            false); // Activity is not launched immediately

    @Before
    public void setUp() throws IOException {
        intent = new Intent(ACTION_SHOW_VPN_FRAGMENT);
        Context context = getInstrumentation().getTargetContext();

        preferences = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        preferencesEditor = preferences.edit();
        preferencesEditor.putString(Provider.KEY, TestSetupHelper.getInputAsString(InstrumentationRegistry.getContext().getAssets().open("riseup.net.json")))
                .putString(Provider.CA_CERT, TestSetupHelper.getInputAsString(InstrumentationRegistry.getContext().getAssets().open("riseup.net.pem")))
                .commit();

    }


    @Test
    public void testDisplayDrawer_isOpenUntilUserManuallyOpendDrawerOnce() {
        preferencesEditor.putBoolean("navigation_drawer_learned", false).commit();
        mActivityRule.launchActivity(intent);
        onView(withId(drawer_layout)).check(matches(isOpen()));
        onView(withId(drawer_layout)).perform(DrawerActions.close());
        mActivityRule.finishActivity();

        mActivityRule.launchActivity(intent);
        onView(withId(drawer_layout)).check(matches(isOpen()));
        onView(withId(drawer_layout)).perform(DrawerActions.close());
        onView(withId(drawer_layout)).perform(DrawerActions.open());
        mActivityRule.finishActivity();

        mActivityRule.launchActivity(intent);
        onView(withId(drawer_layout)).check(matches(isClosed()));
    }

    @Test
    public void testClickProviderName_closeDrawerAndShowEipFragment() {
        preferencesEditor.putBoolean("navigation_drawer_learned", false).commit();
        mActivityRule.launchActivity(intent);

        onData(anything()).inAdapterView(withId(accountList)).atPosition(0).perform(click());
        onView(withId(drawer_layout)).check(matches(isClosed()));
        onView(withId(eipServiceFragment)).check(matches(isDisplayed()));
    }

    @Test
    public void testClickSwitchProvider_closeDrawerAndShowProviderListView() {
        preferencesEditor.putBoolean("navigation_drawer_learned", false).commit();
        mActivityRule.launchActivity(intent);

        onData(anything()).inAdapterView(withId(settingsList)).atPosition(0).perform(click());
        onView(withId(configuration_wizard_layout)).check(matches(isDisplayed()));
    }

    @Test
    public void testClickLog_closeDrawerAndShowLogFragment() {
        preferencesEditor.putBoolean("navigation_drawer_learned", false).commit();
        mActivityRule.launchActivity(intent);

        onData(anything()).inAdapterView(withId(settingsList)).atPosition(1).perform(click());
        onView(withId(log_layout)).check(matches(isDisplayed()));
    }

    @Test
    public void testClickAbout_closeDrawerAndShowAboutFragment() {
        preferencesEditor.putBoolean("navigation_drawer_learned", false).commit();
        mActivityRule.launchActivity(intent);

        onData(anything()).inAdapterView(withId(settingsList)).atPosition(2).perform(click());
        onView(withId(aboutLayout)).check(matches(isDisplayed()));
    }
}
