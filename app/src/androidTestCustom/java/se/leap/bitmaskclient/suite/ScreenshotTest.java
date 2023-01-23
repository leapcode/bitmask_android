package se.leap.bitmaskclient.suite;


import androidx.test.filters.LargeTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import se.leap.bitmaskclient.base.CustomProviderTest;

@LargeTest
@RunWith(Suite.class)
@Suite.SuiteClasses({
        CustomProviderTest.class
})
public class ScreenshotTest {
}
