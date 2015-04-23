package se.leap.bitmaskclient.test;

import android.test.*;
import android.widget.*;

import com.robotium.solo.*;

import java.io.*;

import se.leap.bitmaskclient.*;

public class testConfigurationWizard extends ActivityInstrumentationTestCase2<ConfigurationWizard> {

    private Solo solo;
    private static int added_providers;
    private boolean executing_from_dashboard = false;

    public testConfigurationWizard() {
        super(ConfigurationWizard.class);
    }

    public testConfigurationWizard(Solo solo) {
        super(ConfigurationWizard.class);
        this.solo = solo;
        executing_from_dashboard = true;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        solo = new Solo(getInstrumentation(), getActivity());
        //ConnectionManager.setMobileDataEnabled(true, solo.getCurrentActivity().getApplicationContext());
    }

    @Override
    protected void tearDown() throws Exception {
        if(!executing_from_dashboard)
            solo.finishOpenedActivities();
        super.tearDown();
    }

    public void testListProviders() {
        assertEquals(solo.getCurrentViews(ListView.class).size(), 1);

        assertEquals("Number of available providers differ", predefinedProviders() + added_providers, shownProviders());
    }

    private int shownProviders() {
        return solo.getCurrentViews(ListView.class).get(0).getCount();
    }

    private int predefinedProviders() {
        int predefined_providers = 0;
        try {
            predefined_providers = solo.getCurrentActivity().getAssets().list("urls").length;
        } catch (IOException e) {
            e.printStackTrace();
            return predefined_providers;
        }

        return predefined_providers;
    }

    public void testSelectProvider() {
        selectProvider("demo.bitmask.net");
    }

    private void selectProvider(String provider) {
        solo.clickOnText(provider);
        waitForProviderDetails();
    }

    private void waitForProviderDetails() {
        String text = solo.getString(R.string.provider_details_fragment_title);
        assertTrue("Provider details dialog did not appear", solo.waitForText(text));
    }

    public void testAddNewProvider() {
        addProvider("calyx.net");
    }

    private void addProvider(String url) {
        boolean is_new_provider = !solo.searchText(url);

        if (is_new_provider)
            added_providers = added_providers + 1;
        solo.clickOnActionBarItem(R.id.new_provider);
        solo.enterText(0, url);
        solo.clickOnCheckBox(0);
        solo.clickOnText(solo.getString(R.string.save));
        waitForProviderDetails();
        solo.goBack();
    }

    public void testShowAbout() {
        showAbout();
    }

    private void showAbout() {
        String text = solo.getString(R.string.about);
        solo.clickOnMenuItem(text);
        assertTrue("Provider details dialog did not appear", solo.waitForActivity(AboutActivity.class));
    }

    protected void toDashboardAnonymously(String provider) {
        selectProvider(provider);
        useAnonymously();
    }

    private void useAnonymously() {
        String text = solo.getString(R.string.use_anonymously_button);
        clickAndWaitForDashboard(text);
    }

    private void clickAndWaitForDashboard(String click_text) {
        solo.clickOnText(click_text);
        assertTrue(solo.waitForActivity(Dashboard.class, 5000));
    }
}
