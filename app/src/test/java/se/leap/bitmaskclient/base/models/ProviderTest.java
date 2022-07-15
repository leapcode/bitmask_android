package se.leap.bitmaskclient.base.models;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashSet;
import java.util.Set;

import se.leap.bitmaskclient.base.utils.ConfigHelper;
import se.leap.bitmaskclient.testutils.TestSetupHelper;

/**
 * Created by cyberta on 12.02.18.
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({ConfigHelper.ObfsVpnHelper.class})
public class ProviderTest {

    @Before
    public void setup() {
        mockStatic(ConfigHelper.ObfsVpnHelper.class);
        when(ConfigHelper.ObfsVpnHelper.useObfsVpn()).thenReturn(false);
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

        Set<Provider> defaultProviders = new HashSet<>();
        defaultProviders.add(p1);
        defaultProviders.add(p2);

        assertTrue(defaultProviders.contains(p1));
        assertTrue(defaultProviders.contains(p2));
        assertFalse(defaultProviders.contains(p3));
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
    public void testSupportsPluggableTransports_Obfs4Kcp_noObsvpn_returnsFalse() throws Exception {
        when(ConfigHelper.ObfsVpnHelper.useObfsVpn()).thenReturn(false);

        Provider p1 = TestSetupHelper.getProvider(
                "https://pt.demo.bitmask.net",
                null,
                null,
                null,
                null,
                null,
                "ptdemo_only_experimental_transports_gateways.json",
                null);
        assertFalse(p1.supportsPluggableTransports());
    }

    @Test
    public void testSupportsPluggableTransports_Obfs4Kcp_obsvpn_returnsTrue() throws Exception {
        when(ConfigHelper.ObfsVpnHelper.useObfsVpn()).thenReturn(true);

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
