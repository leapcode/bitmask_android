package se.leap.bitmaskclient.suite;


import androidx.test.filters.LargeTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import se.leap.bitmaskclient.BitmaskTest;
import se.leap.bitmaskclient.ProviderSetupTest;

@LargeTest
@RunWith(Suite.class)
@Suite.SuiteClasses({
        BitmaskTest.class,
        ProviderSetupTest.class,
})
public class ScreenshotTest {
}
