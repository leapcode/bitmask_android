package se.leap.bitmaskclient.suite;


import androidx.test.filters.LargeTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import se.leap.bitmaskclient.ProviderSetupTest;
import se.leap.bitmaskclient.base.CustomProviderTest;

@LargeTest
@RunWith(Suite.class)
@Suite.SuiteClasses({
        ProviderSetupTest.class
})
public class ScreenshotTest {
}
