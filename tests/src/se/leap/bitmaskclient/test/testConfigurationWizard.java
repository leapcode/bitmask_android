package se.leap.bitmaskclient.test;

import java.io.IOException;

import se.leap.bitmaskclient.AboutFragment;
import se.leap.bitmaskclient.ConfigurationWizard;
import se.leap.bitmaskclient.ProviderDetailFragment;
import se.leap.bitmaskclient.R;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.ListView;

import com.jayway.android.robotium.solo.Solo;

public class testConfigurationWizard extends ActivityInstrumentationTestCase2<ConfigurationWizard> {

	private Solo solo;
	private static int added_providers;
	
	public testConfigurationWizard() {
		super(ConfigurationWizard.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		solo = new Solo(getInstrumentation(), getActivity());
	}

	@Override
	protected void tearDown() throws Exception { 
		solo.finishOpenedActivities();
	}
	
	public void testListProviders() throws IOException {
		assertEquals(solo.getCurrentViews(ListView.class).size(), 1);
		
		int number_of_available_providers = solo.getCurrentViews(ListView.class).get(0).getCount();
		
		assertEquals("Number of available providers differ", solo.getCurrentActivity().getAssets().list("urls").length + added_providers, number_of_available_providers);
	}
	
	public void testSelectProvider() {
		solo.clickOnText("bitmask");
		assertTrue("Provider details dialog did not appear", solo.waitForFragmentByTag(ProviderDetailFragment.TAG, 60*1000));
	}
	
	public void testAddNewProvider() {
		solo.clickOnActionBarItem(R.id.new_provider);
		solo.enterText(0, "dev.bitmask.net");
		solo.clickOnCheckBox(0);
		solo.clickOnText(solo.getString(R.string.save));
		added_providers = added_providers+1;
		assertTrue("Provider details dialog did not appear", solo.waitForFragmentByTag(ProviderDetailFragment.TAG, 60*1000));
		solo.goBack();
	}
	
	public void testShowAbout() {
		solo.clickOnMenuItem(solo.getString(R.string.about));
		assertTrue("Provider details dialog did not appear", solo.waitForFragmentByTag(AboutFragment.TAG));
	}
	
	public void testShowSettings() {
		//TODO We still don't have the settings button 
	}
}
