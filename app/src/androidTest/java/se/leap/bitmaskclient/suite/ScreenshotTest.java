package se.leap.bitmaskclient.suite;


import androidx.test.filters.LargeTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import se.leap.bitmaskclient.base.ProviderSetupTest;
import se.leap.bitmaskclient.base.VpnStartTest;

@LargeTest
@RunWith(Suite.class)
@Suite.SuiteClasses({
        ProviderSetupTest.class,
        VpnStartTest.class,
})
public class ScreenshotTest {
}
