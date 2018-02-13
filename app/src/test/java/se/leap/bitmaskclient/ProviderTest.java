package se.leap.bitmaskclient;

import org.junit.Test;

import se.leap.bitmaskclient.testutils.TestSetupHelper;

import static org.junit.Assert.assertTrue;

/**
 * Created by cyberta on 12.02.18.
 */
public class ProviderTest {

    @Test
    public void testEquals_sameFields_returnsTrue() throws Exception {
        Provider p1 = TestSetupHelper.getConfiguredProvider();
        Provider p2 = TestSetupHelper.getConfiguredProvider();
        assertTrue("Providers should be same:", p1.equals(p2));
    }

}
