package se.leap.bitmaskclient.base.models;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import se.leap.bitmaskclient.base.utils.BuildConfigHelper;
import se.leap.bitmaskclient.testutils.MockHelper;
import se.leap.bitmaskclient.testutils.TestSetupHelper;

/**
 * Created by cyberta on 12.02.18.
 */

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.P})
public class ProviderTest {

    @Before
    public void setup() {
        BuildConfigHelper buildConfigHelper = MockHelper.mockBuildConfigHelper(false);
    }

    @Test
    public void testEquals_sameFields_returnsTrue() throws Exception {
        Provider p1 = TestSetupHelper.getConfiguredProvider();
        Provider p2 = TestSetupHelper.getConfiguredProvider();
        assertTrue("Providers should be same:", p1.equals(p2));
    }

    @Test
    public void testEquals_differntMainUrl_returnsFalse() throws Exception {
        Provider p1 = TestSetupHelper.getConfiguredProvider();
        Provider p2 = TestSetupHelper.getConfiguredProvider();
        p2.setMainUrl("http://somethingsdiffer.org");
        assertFalse("Providers should be same:", p1.equals(p2));
    }

    @Test
    public void testEquals_differentGeoIpUrl_returnsFalse() throws Exception {
        Provider p1 = TestSetupHelper.getConfiguredProvider();
        Provider p2 = TestSetupHelper.getConfiguredProvider();
        p2.setGeoipUrl(null);
        assertFalse("Providers should be same:", p1.equals(p2));
    }

    // see ProviderManagerTest testing add(...)
    @Test
    public void testEqualsThroughSetContains_differentFields_returnsFalse() throws Exception {
        Provider p1 = TestSetupHelper.getConfiguredProvider();
        Provider p2 = TestSetupHelper.getConfiguredProvider();
        p2.setMainUrl("http://somethingsdiffer.org");
        Provider p3 = new Provider("https://anotherprovider.net");
        Provider p4 = TestSetupHelper.getConfiguredProvider();

        assertEquals(p1, p4);
        assertNotEquals(p1, p2);
        assertNotEquals(p1, p3);

    }

    @Test
    public void testIsPluggableTransportsSupported_Obfs4_returnsTrue() throws Exception {
        Provider p1 = TestSetupHelper.getProvider(
                "https://pt.demo.bitmask.net",
                null,
                null,
                null,
                null,
                null,
                "ptdemo.bitmask.eip-service.json",
                null);
        assertTrue(p1.supportsPluggableTransports());
    }

    @Test
    public void testIsPluggableTransportsSupported_noObfs4_returnsFalse() throws Exception {
        Provider p1 = TestSetupHelper.getProvider(
                null,
                null,
                null,
                null,
                null,
                null,
                "eip-service-two-gateways.json",
                null);
        assertFalse(p1.supportsPluggableTransports());
    }

    @Test
    public void testIsExperimentalPluggableTransportsSupported_Obfs4_returnsFalse() throws Exception {
        Provider p1 = TestSetupHelper.getProvider(
                "https://pt.demo.bitmask.net",
                null,
                null,
                null,
                null,
                null,
                "ptdemo.bitmask.eip-service.json",
                null);
        assertFalse(p1.supportsExperimentalPluggableTransports());
    }

    @Test
    public void testIsExperimentalPluggableTransportsSupported_Obfs4Kcp_returnsTrue() throws Exception {
        Provider p1 = TestSetupHelper.getProvider(
                "https://pt.demo.bitmask.net",
                null,
                null,
                null,
                null,
                null,
                "ptdemo_kcp_gateways.json",
                null);
        assertTrue(p1.supportsExperimentalPluggableTransports());
    }

    @Test
    public void testSupportsPluggableTransports_Obfs4Kcp_obsvpn_returnsTrue() throws Exception {
        BuildConfigHelper helper = MockHelper.mockBuildConfigHelper(true);

        Provider p1 = TestSetupHelper.getProvider(
                "https://pt.demo.bitmask.net",
                null,
                null,
                null,
                null,
                null,
                "ptdemo_only_experimental_transports_gateways.json",
                null);
        assertTrue(p1.supportsPluggableTransports());
    }

}
