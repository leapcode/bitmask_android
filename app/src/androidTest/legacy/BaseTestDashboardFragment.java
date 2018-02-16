package se.leap.bitmaskclient.test;

import android.content.Context;
import android.graphics.Rect;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;

import com.robotium.solo.Solo;

import se.leap.bitmaskclient.ProviderListActivity;
import se.leap.bitmaskclient.R;

public abstract class BaseTestDashboardFragment extends ActivityInstrumentationTestCase2<Dashboard> {

    Solo solo;
    Context context;
    UserStatusTestController user_status_controller;
    VpnTestController vpn_controller;

    public BaseTestDashboardFragment() { super(Dashboard.class); }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = getInstrumentation().getContext();
        solo = new Solo(getInstrumentation(), getActivity());
        Screenshot.initialize(solo);
        user_status_controller = new UserStatusTestController(solo);
        vpn_controller = new VpnTestController(solo);
        ConnectionManager.setMobileDataEnabled(true, context);
        solo.unlockScreen();
        if (solo.searchText(solo.getString(R.string.configuration_wizard_title))) {
            toDashboardAnonymously("demo.bitmask.net");
        }
    }

    void changeProviderAndLogIn(String provider) {
        tapSwitchProvider();
        solo.clickOnText(provider);
        useRegistered();
    }

    void tapSwitchProvider() {
        solo.clickOnMenuItem(solo.getString(R.string.switch_provider_menu_option));
        solo.waitForActivity(ProviderListActivity.class);
    }

    private void useRegistered() {
        solo.waitForFragmentById(R.id.provider_detail_fragment);
        String text = solo.getString(R.string.signup_or_login_button);
        clickAndWaitForDashboard(text);
        user_status_controller.logIn("parmegvtest10", "holahola2");
    }

    private void clickAndWaitForDashboard(String click_text) {
        solo.clickOnButton(click_text);
        assertTrue(solo.waitForActivity(Dashboard.class, 80 * 1000));
    }

    static boolean isShownWithinConfinesOfVisibleScreen(View view) {
        Rect scrollBounds = new Rect();
        view.getHitRect(scrollBounds);
        return view.getLocalVisibleRect(scrollBounds);
    }


    private void toDashboardAnonymously(String provider) {
        selectProvider(provider);
        useAnonymously();
    }

    private void useAnonymously() {
        String text = solo.getString(R.string.use_anonymously_button);
        clickAndWaitForDashboard(text);
    }


    private void selectProvider(String provider) {
        solo.clickOnText(provider);
        Screenshot.setTimeToSleep(1);
        Screenshot.take("Configuring provider");
        waitForProviderDetails();
    }

    private void waitForProviderDetails() {
        String text = solo.getString(R.string.provider_details_title);
        assertTrue("Provider details dialog did not appear", solo.waitForText(text, 1, 60*1000));
        Screenshot.take("Provider details");
    }



}
