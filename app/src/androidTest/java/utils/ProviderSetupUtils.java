package utils;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withTagValue;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static utils.CustomInteractions.tryResolve;

import android.content.Context;
import android.net.VpnService;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import org.hamcrest.Matchers;

import se.leap.bitmaskclient.BuildConfig;
import se.leap.bitmaskclient.R;
import tools.fastlane.screengrab.Screengrab;

public class ProviderSetupUtils {

    public static void runProviderSetup(UiDevice device, boolean takeConfigurationScreenshots, boolean useCircumvention, Context targetContext) {
        try {
            // ------- PROVIDER SELECTION --------------
            if ("normal".equals(BuildConfig.FLAVOR_branding)) {
                System.out.println("next: provider selection");
                ViewInteraction radioButtonSelection = tryResolve(onView(withText("Riseup")), matches(isDisplayed()));
                if (takeConfigurationScreenshots) Screengrab.screenshot("provider_selection");
                radioButtonSelection.perform(click());
                // next button click
                onView(withText(R.string.next)).perform(click());
            }

            // ------- CIRCUMVENTION SELECTION --------------
            System.out.println("next: standard/circumvention selection");
            if (useCircumvention) {
                onView(withText(targetContext.getString(R.string.use_circumvention_tech))).perform(click());
            } else {
                onView(withText(targetContext.getString(R.string.use_standard_vpn, targetContext.getString(R.string.app_name)))).perform(click());
            }
            if (takeConfigurationScreenshots) Screengrab.screenshot("circumvention_selection");

            // ------- CONFIGURATION PROGRESS --------------
            System.out.println("next: configuring");
            onView(withText(R.string.next)).perform(click());
            tryResolve(
                    onView(
                            Matchers.allOf(
                                    withText(R.string.configuring_provider),
                                    withId(R.id.tv_title)
                            )
                    ),
                    matches(isDisplayed())
            );
            if (takeConfigurationScreenshots) Screengrab.screenshot("configuring_provider");

            // ------- VPN PERMISSON DIALOG --------------
            boolean showPermissionDialog = false;
            if (VpnService.prepare(getApplicationContext()) != null) {
                showPermissionDialog = true;
                tryResolve(onView(withText(R.string.upcoming_connection_request_description)), matches(isDisplayed()), useCircumvention ? 180 : 20);
                System.out.println("next: next permission request");
                if (takeConfigurationScreenshots) Screengrab.screenshot("vpn_permission_rationale");
                onView(withText(R.string.next)).perform(click());
                UiObject okButton = device.findObject(new UiSelector().packageName("com.android.vpndialogs").resourceId("android:id/button1"));
                okButton.waitForExists(30000);
                okButton.click();
                device.waitForWindowUpdate("com.android.vpndialogs", 1000);
            }

            // ------- START VPN --------------
            System.out.println("next: perform click on VPN button");
            ViewInteraction interaction = tryResolve(onView(withTagValue(Matchers.is("button_setup_circle_custom"))), matches(isDisplayed()), useCircumvention && !showPermissionDialog ? 180 : 20);
            if (takeConfigurationScreenshots) {
                Screengrab.screenshot("all_set_start_vpn");
            } else {
                // we only want to start the VPN in case we're not running the ProviderSetupTest
                interaction.perform(click());
            }
        } catch (NoMatchingViewException e) {
            // it might be that the provider was already configured, so we print the stack
            // trace here and try to continue
            e.printStackTrace();
        } catch (UiObjectNotFoundException | NullPointerException e) {
            e.printStackTrace();
        }
    }
}
