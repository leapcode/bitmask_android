package se.leap.bitmaskclient.suite;


import androidx.test.filters.LargeTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import se.leap.bitmaskclient.BitmaskTest;
import se.leap.bitmaskclient.ProviderSetupTest;

@LargeTest
@RunWith(Suite.class)
@Suite.SuiteClasses({
        ProviderSetupTest.class,
        BitmaskTest.class,
})
public class ScreenshotTest {
}
