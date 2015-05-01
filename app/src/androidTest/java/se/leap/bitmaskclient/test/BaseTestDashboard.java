package se.leap.bitmaskclient.test;

import android.content.*;
import android.graphics.*;
import android.test.*;
import android.view.*;

import com.robotium.solo.*;

import se.leap.bitmaskclient.*;

public abstract class BaseTestDashboard extends ActivityInstrumentationTestCase2<Dashboard> {

    Solo solo;
    Context context;
    UserStatusTestController user_status_controller;
    VpnTestController vpn_controller;

    public BaseTestDashboard() { super(Dashboard.class); }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = getInstrumentation().getContext();
        solo = new Solo(getInstrumentation(), getActivity());
        user_status_controller = new UserStatusTestController(solo);
        vpn_controller = new VpnTestController(solo);
        ConnectionManager.setMobileDataEnabled(true, context);
        solo.unlockScreen();
        if (solo.searchText(solo.getString(R.string.configuration_wizard_title)))
            new testConfigurationWizard(solo).toDashboardAnonymously("demo.bitmask.net");
    }

    void changeProviderAndLogIn(String provider) {
        tapSwitchProvider();
        solo.clickOnText(provider);
        useRegistered();
    }

    void tapSwitchProvider() {
        solo.clickOnMenuItem(solo.getString(R.string.switch_provider_menu_option));
        solo.waitForActivity(ConfigurationWizard.class);
    }

    private void useRegistered() {
        String text = solo.getString(R.string.signup_or_login_button);
        clickAndWaitForDashboard(text);
        user_status_controller.logIn("parmegvtest10", "holahola2");
    }

    private void clickAndWaitForDashboard(String click_text) {
        solo.clickOnText(click_text);
        assertTrue(solo.waitForActivity(Dashboard.class, 5000));
    }

    static boolean isShownWithinConfinesOfVisibleScreen(View view) {
        Rect scrollBounds = new Rect();
        view.getHitRect(scrollBounds);
        return view.getLocalVisibleRect(scrollBounds);
    }
}
