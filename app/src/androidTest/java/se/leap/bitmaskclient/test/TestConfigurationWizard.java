package se.leap.bitmaskclient.test;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.ListView;

import com.robotium.solo.Solo;

import java.io.IOException;

import se.leap.bitmaskclient.AboutActivity;
import se.leap.bitmaskclient.ConfigurationWizard;
import se.leap.bitmaskclient.R;

public class TestConfigurationWizard extends ActivityInstrumentationTestCase2<ConfigurationWizard> {

    private Solo solo;
    private static int added_providers;

    public TestConfigurationWizard() {
        super(ConfigurationWizard.class);
    }


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        solo = new Solo(getInstrumentation(), getActivity());
        Screenshot.initialize(solo);
    }

    @Override
    protected void tearDown() throws Exception {
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
        Screenshot.take("Initial CW");
        selectProvider("demo.bitmask.net");
    }

    private void selectProvider(String provider) {
        solo.clickOnText(provider);
        Screenshot.setTimeToSleep(1);
        Screenshot.take("Configuring provider");
        waitForProviderDetails();
    }

    private void waitForProviderDetails() {
        String text = solo.getString(R.string.provider_details_fragment_title);
        assertTrue("Provider details dialog did not appear", solo.waitForText(text, 1, 60*1000));
        Screenshot.take("Provider details");
    }

    public void testAddNewProvider() {
        //addProvider("calyx.net");
        addProvider("riseup.net");
    }

    private void addProvider(String url) {
        boolean is_new_provider = !solo.searchText(url);

        if (is_new_provider)
            added_providers = added_providers + 1;
        solo.clickOnActionBarItem(R.id.new_provider);
        solo.enterText(0, url);
        if ( BuildConfig.FLAVOR.equals("insecure")) {
            solo.clickOnCheckBox(0);
        }
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

}
