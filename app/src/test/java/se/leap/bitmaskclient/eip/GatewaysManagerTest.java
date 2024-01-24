package se.leap.bitmaskclient.eip;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static de.blinkt.openvpn.core.connection.Connection.TransportType.OBFS4;
import static de.blinkt.openvpn.core.connection.Connection.TransportType.OPENVPN;
import static se.leap.bitmaskclient.base.models.Constants.ALLOW_EXPERIMENTAL_TRANSPORTS;
import static se.leap.bitmaskclient.base.models.Constants.GATEWAYS;
import static se.leap.bitmaskclient.base.models.Constants.PREFERRED_CITY;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_EIP_DEFINITION;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_PRIVATE_KEY;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.base.models.Constants.USE_BRIDGES;
import static se.leap.bitmaskclient.base.models.Provider.CA_CERT;
import static se.leap.bitmaskclient.testutils.TestSetupHelper.getProvider;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import se.leap.bitmaskclient.base.models.Location;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.models.ProviderObservable;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.base.utils.TimezoneHelper;
import se.leap.bitmaskclient.testutils.MockSharedPreferences;
import se.leap.bitmaskclient.testutils.TestSetupHelper;

/**
 * Created by cyberta on 09.10.17.
 */
public class GatewaysManagerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mockContext;

    private SharedPreferences sharedPreferences;
    private JSONObject secrets;

    private TimezoneHelper timezoneHelper;
    ProviderObservable providerObservable;


    @Before
    public void setUp() throws IOException, JSONException {
        timezoneHelper = new TimezoneHelper(() -> -1);
        providerObservable = ProviderObservable.getInstance();

        secrets = new JSONObject(getJsonStringFor("secrets.json"));
        sharedPreferences = new MockSharedPreferences();
        sharedPreferences.edit().
                putString(PROVIDER_PRIVATE_KEY, secrets.getString(PROVIDER_PRIVATE_KEY)).
                putString(CA_CERT, secrets.getString(CA_CERT)).
                putString(PROVIDER_VPN_CERTIFICATE, secrets.getString(PROVIDER_VPN_CERTIFICATE))
                .commit();
        PreferenceHelper preferenceHelper = new PreferenceHelper(sharedPreferences);
    }


    @Test
    public void testGatewayManagerFromCurrentProvider_noProvider_noGateways() {
        providerObservable.updateProvider(null);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);
        assertEquals(0, gatewaysManager.size());
    }

    @Test
    public void testGatewayManagerFromCurrentProvider_threeGateways() {
        providerObservable.updateProvider(getProvider(null, null, null, null,null, null, "ptdemo_three_mixed_gateways.json", null));
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);
        assertEquals(3, gatewaysManager.size());
    }

    @Test
    public void TestGetPosition_VpnProfileExtistingObfs4_returnPositionZero() throws JSONException, ConfigParser.ConfigParseError, IOException {
        Provider provider = getProvider(null, null, null, null, null, null, "ptdemo_three_mixed_gateways.json", null);
        JSONObject eipServiceJson = provider.getEipServiceJson();
        JSONObject gateway1 = eipServiceJson.getJSONArray(GATEWAYS).getJSONObject(0);
        providerObservable.updateProvider(provider);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        VpnConfigGenerator.Configuration configuration = new VpnConfigGenerator.Configuration();
        configuration.apiVersion = 3;
        configuration.remoteGatewayIP = "37.218.247.60";
        VpnConfigGenerator configGenerator = new VpnConfigGenerator(provider.getDefinition(), secrets, gateway1, configuration);
        VpnProfile profile = configGenerator.createProfile(OBFS4);

        assertEquals(0, gatewaysManager.getPosition(profile));
    }

    @Test
    public void TestGetPosition_VpnProfileExtistingOpenvpn_returnPositionZero() throws JSONException, ConfigParser.ConfigParseError, IOException {
        Provider provider = getProvider(null, null, null, null, null, null, "ptdemo_three_mixed_gateways.json", null);
        JSONObject eipServiceJson = provider.getEipServiceJson();
        JSONObject gateway1 = eipServiceJson.getJSONArray(GATEWAYS).getJSONObject(0);
        providerObservable.updateProvider(provider);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        VpnConfigGenerator.Configuration configuration = new VpnConfigGenerator.Configuration();
        configuration.apiVersion = 3;
        configuration.remoteGatewayIP = "37.218.247.60";
        VpnConfigGenerator configGenerator = new VpnConfigGenerator(provider.getDefinition(), secrets, gateway1, configuration);
        VpnProfile profile = configGenerator.createProfile(OPENVPN);

        assertEquals(0, gatewaysManager.getPosition(profile));
    }

    @Test
    public void TestGetPosition_VpnProfileExistingObfs4FromPresortedList_returnsPositionOne() throws JSONException, ConfigParser.ConfigParseError, IOException {
        Provider provider = getProvider(null, null, null, null, null, null, "ptdemo_three_mixed_gateways.json", "ptdemo_three_mixed_gateways.geoip.json");
        JSONObject eipServiceJson = provider.getEipServiceJson();
        JSONObject gateway1 = eipServiceJson.getJSONArray(GATEWAYS).getJSONObject(0);
        providerObservable.updateProvider(provider);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        VpnConfigGenerator.Configuration configuration = new VpnConfigGenerator.Configuration();
        configuration.apiVersion = 3;
        configuration.remoteGatewayIP = "37.218.247.60";
        VpnConfigGenerator configGenerator = new VpnConfigGenerator(provider.getDefinition(), secrets, gateway1, configuration);
        VpnProfile profile = configGenerator.createProfile(OBFS4);

        assertEquals(1, gatewaysManager.getPosition(profile));
    }

    @Test
    public void TestGetPosition_VpnProfileExistingOpenvpnFromPresortedList_returnsPositionOne() throws JSONException, ConfigParser.ConfigParseError, IOException {
        Provider provider = getProvider(null, null, null, null, null, null, "ptdemo_three_mixed_gateways.json", "ptdemo_three_mixed_gateways.geoip.json");
        JSONObject eipServiceJson = provider.getEipServiceJson();
        JSONObject gateway1 = eipServiceJson.getJSONArray(GATEWAYS).getJSONObject(0);
        providerObservable.updateProvider(provider);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        VpnConfigGenerator.Configuration configuration = new VpnConfigGenerator.Configuration();
        configuration.apiVersion = 3;
        configuration.remoteGatewayIP = "37.218.247.60";
        VpnConfigGenerator configGenerator = new VpnConfigGenerator(provider.getDefinition(), secrets, gateway1, configuration);
        VpnProfile profile = configGenerator.createProfile(OPENVPN);

        assertEquals(2, gatewaysManager.getPosition(profile));
    }

    @Test
    public void TestGetPosition_VpnProfileDifferentIp_returnMinusOne() throws JSONException, ConfigParser.ConfigParseError, IOException {
        Provider provider = getProvider(null, null, null, null, null, null, "ptdemo_three_mixed_gateways.json", null);
        JSONObject eipServiceJson = provider.getEipServiceJson();
        JSONObject gateway1 = eipServiceJson.getJSONArray(GATEWAYS).getJSONObject(0);
        providerObservable.updateProvider(provider);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        VpnConfigGenerator.Configuration configuration = new VpnConfigGenerator.Configuration();
        configuration.apiVersion = 3;
        configuration.remoteGatewayIP = "37.218.247.61";
        VpnConfigGenerator configGenerator = new VpnConfigGenerator(provider.getDefinition(), secrets, gateway1, configuration);
        VpnProfile profile = configGenerator.createProfile(OBFS4);

        assertEquals(-1, gatewaysManager.getPosition(profile));
    }

    @Test
    public void TestGetPosition_VpnProfileMoscow_returnOne() throws JSONException, ConfigParser.ConfigParseError, IOException {
        Provider provider = getProvider(null, null, null, null, null, null, "ptdemo_three_mixed_gateways.json", null);
        JSONObject eipServiceJson = provider.getEipServiceJson();
        JSONObject gateway1 = eipServiceJson.getJSONArray(GATEWAYS).getJSONObject(1);
        providerObservable.updateProvider(provider);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        VpnConfigGenerator.Configuration configuration = new VpnConfigGenerator.Configuration();
        configuration.apiVersion = 3;
        configuration.remoteGatewayIP = "3.21.247.89";
        VpnConfigGenerator configGenerator = new VpnConfigGenerator(provider.getDefinition(), secrets, gateway1, configuration);
        VpnProfile profile = configGenerator.createProfile(OBFS4);

        assertEquals(1, gatewaysManager.getPosition(profile));
    }

    @Test
    public void TestSelectN_selectFirstObfs4Connection_returnThirdGateway() {
        Provider provider = getProvider(null, null, null, null, null, null, "ptdemo_two_openvpn_one_pt_gateways.json", null);

        providerObservable.updateProvider(provider);
        sharedPreferences.edit().putBoolean(USE_BRIDGES, true).commit();
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        assertEquals("37.12.247.10", gatewaysManager.select(0).gateway.getRemoteIP());
    }

    @Test
    public void TestSelectN_select_includeExperimentalTransport_DecoupledPortHoppingGW() {
        Provider provider = getProvider(null, null, null, null, null, null, "decoupled_pt_portHopping.eip-service.json", null);

        providerObservable.updateProvider(provider);
        sharedPreferences.edit().
                putBoolean(USE_BRIDGES, true).
                putBoolean(ALLOW_EXPERIMENTAL_TRANSPORTS, true).
                commit();
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);
        ArrayList<String> hosts = new ArrayList<>();
        hosts.add(gatewaysManager.select(0).gateway.getHost());
        hosts.add(gatewaysManager.select(1).gateway.getHost());

        assertTrue(hosts.contains("bridge-nyc1-02.bitmask-dev.leapvpn.net"));
        assertTrue(hosts.contains("bridge-nyc1-01.bitmask-dev.leapvpn.net"));

    }

    @Test
    public void TestSelectN_select_includeExperimentalTransport_DecoupledPortAndIPHoppingGW() {
        Provider provider = getProvider(null, null, null, null, null, null, "decoupled_pt.eip-service.json", null);

        providerObservable.updateProvider(provider);
        sharedPreferences.edit().
                putBoolean(USE_BRIDGES, true).
                putBoolean(ALLOW_EXPERIMENTAL_TRANSPORTS, true).
                commit();
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        ArrayList<String> hosts = new ArrayList<>();
        hosts.add(gatewaysManager.select(0).gateway.getHost());
        hosts.add(gatewaysManager.select(1).gateway.getHost());
        assertTrue(hosts.contains("bridge-nyc1-02.bitmask-dev.leapvpn.net"));
        assertTrue(hosts.contains("bridge-nyc1-01.bitmask-dev.leapvpn.net"));
    }

    @Test
    public void TestSelectN_select_excludeExperimentalTransport_DecoupledPortHoppingGW() {
        Provider provider = getProvider(null, null, null, null, null, null, "decoupled_pt_portHopping.eip-service.json", null);

        providerObservable.updateProvider(provider);
        sharedPreferences.edit().
                putBoolean(USE_BRIDGES, true).
                putBoolean(ALLOW_EXPERIMENTAL_TRANSPORTS, false).
                commit();

        for (int i = 0; i < 1000; i++) {
            GatewaysManager gatewaysManager = new GatewaysManager(mockContext);
            assertEquals("bridge-nyc1-01.bitmask-dev.leapvpn.net", gatewaysManager.select(0).gateway.getHost());
        }
    }

    @Test
    public void TestSelectN_select_excludeExperimentalTransport_DecoupledPortAndIPHoppingGW() {
        Provider provider = getProvider(null, null, null, null, null, null, "decoupled_pt.eip-service.json", null);

        providerObservable.updateProvider(provider);
        sharedPreferences.edit().
                putBoolean(USE_BRIDGES, true).
                putBoolean(ALLOW_EXPERIMENTAL_TRANSPORTS, false).
                commit();
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        assertEquals("bridge-nyc1-01.bitmask-dev.leapvpn.net", gatewaysManager.select(0).gateway.getHost());
        assertNull(gatewaysManager.select(1));
    }

    @Test
    public void TestSelectN_select_excludeExperimentalTransport_InGatewayHoppingPTBridge() {
        Provider provider = getProvider(null, null, null, null, null, null, "ptdemo_obfs4hop_tcp_gateways.json", null);

        providerObservable.updateProvider(provider);
        sharedPreferences.edit().
                putBoolean(USE_BRIDGES, true).
                putBoolean(ALLOW_EXPERIMENTAL_TRANSPORTS, false).
                commit();
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        ArrayList<String> hosts = new ArrayList<>();
        hosts.add(gatewaysManager.select(0).gateway.getHost());
        hosts.add(gatewaysManager.select(1).gateway.getHost());

        assertTrue(hosts.contains("pt.demo.bitmask.net"));
        assertTrue(hosts.contains("manila.bitmask.net"));
    }

    @Test
    public void testSelectN_selectFromPresortedGateways_returnsGatewaysInPresortedOrder() {
        Provider provider = getProvider(null, null, null, null, null, null, "ptdemo_three_mixed_gateways.json", "ptdemo_three_mixed_gateways.geoip.json");

        providerObservable.updateProvider(provider);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        assertEquals("manila.bitmask.net", gatewaysManager.select(0).gateway.getHost());
        assertEquals("moscow.bitmask.net", gatewaysManager.select(1).gateway.getHost());
        assertEquals("pt.demo.bitmask.net", gatewaysManager.select(2).gateway.getHost());
    }

    @Test
    public void testSelectN_selectObfs4FromPresortedGateways_returnsObfs4GatewaysInPresortedOrder() {
        Provider provider = getProvider(null, null, null, null, null, null, "ptdemo_three_mixed_gateways.json", "ptdemo_three_mixed_gateways.geoip.json");

        providerObservable.updateProvider(provider);
        //use openvpn, not pluggable transports
        sharedPreferences.edit().
                putBoolean(USE_BRIDGES, true).
                commit();
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        assertEquals("moscow.bitmask.net", gatewaysManager.select(0).gateway.getHost());
        assertEquals("pt.demo.bitmask.net", gatewaysManager.select(1).gateway.getHost());
        assertNull(gatewaysManager.select(2));
    }


    @Test
    public void testSelectN_selectFromCity_returnsGatewaysInPresortedOrder() {
        Provider provider = getProvider(null, null, null, null, null, null, "v4/riseup_eipservice_for_geoip_v4.json", "v4/riseup_geoip_v4.json");

        providerObservable.updateProvider(provider);
        //use openvpn, not pluggable transports
        sharedPreferences.edit().
                putString(PREFERRED_CITY, "Paris").
                commit();
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        assertEquals("mouette.riseup.net", gatewaysManager.select(0).gateway.getHost());
        assertEquals("hoatzin.riseup.net", gatewaysManager.select(1).gateway.getHost());
        assertEquals("zarapito.riseup.net", gatewaysManager.select(2).gateway.getHost());
    }

    @Test
    public void testSelectN_selectFromCityWithGeoIpServiceV1_returnsGatewaysInPresortedOrder() {
        Provider provider = getProvider(null, null, null, null, null, null, "v4/riseup_eipservice_for_geoip_v4.json", "v4/riseup_geoip_v1.json");

        providerObservable.updateProvider(provider);
        //use openvpn, not pluggable transports
        sharedPreferences.edit().
                putString(PREFERRED_CITY, "Paris").
                commit();
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        assertEquals("mouette.riseup.net", gatewaysManager.select(0).gateway.getHost());
        assertEquals("hoatzin.riseup.net", gatewaysManager.select(1).gateway.getHost());
        assertEquals("zarapito.riseup.net", gatewaysManager.select(2).gateway.getHost());
    }

    @Test
    public void testSelectN_selectFromCityWithTimezoneCalculation_returnsRandomizedGatewaysOfSelectedCity() {
        Provider provider = getProvider(null, null, null, null, null, null, "v4/riseup_eipservice_for_geoip_v4.json", null);

        provider.setGeoIpJson(new JSONObject());
        providerObservable.updateProvider(provider);
        //use openvpn, not pluggable transports
        sharedPreferences.edit().
                putString(PREFERRED_CITY, "Paris").
                commit();
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        assertEquals("Paris", gatewaysManager.select(0).gateway.getName());
        assertEquals("Paris", gatewaysManager.select(1).gateway.getName());
        assertEquals("Paris", gatewaysManager.select(2).gateway.getName());
        assertEquals(null, gatewaysManager.select(3));
    }

    @Test
    public void testSelectN_selectNAndCity_returnsGatewaysInPresortedOrder() {
        Provider provider = getProvider(null, null, null, null, null, null, "v4/riseup_eipservice_for_geoip_v4.json", "v4/riseup_geoip_v4.json");

        providerObservable.updateProvider(provider);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        assertEquals("mouette.riseup.net", gatewaysManager.select(0, "Paris").gateway.getHost());
        assertEquals("hoatzin.riseup.net", gatewaysManager.select(1, "Paris").gateway.getHost());
        assertEquals("zarapito.riseup.net", gatewaysManager.select(2, "Paris").gateway.getHost());
    }

    @Test
    public void testSelectN_selectNAndCityWithGeoIpServiceV1_returnsGatewaysInPresortedOrder() {
        Provider provider = getProvider(null, null, null, null, null, null, "v4/riseup_eipservice_for_geoip_v4.json", "v4/riseup_geoip_v1.json");

        providerObservable.updateProvider(provider);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        assertEquals("mouette.riseup.net", gatewaysManager.select(0, "Paris").gateway.getHost());
        assertEquals("hoatzin.riseup.net", gatewaysManager.select(1, "Paris").gateway.getHost());
        assertEquals("zarapito.riseup.net", gatewaysManager.select(2, "Paris").gateway.getHost());
    }

    @Test
    public void testSelectN_selectNAndCityWithTimezoneCalculation_returnsRandomizedGatewaysOfSelectedCity() {
        Provider provider = getProvider(null, null, null, null, null, null, "v4/riseup_eipservice_for_geoip_v4.json", null);

        provider.setGeoIpJson(new JSONObject());
        providerObservable.updateProvider(provider);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        assertEquals("Paris", gatewaysManager.select(0, "Paris").gateway.getName());
        assertEquals("Paris", gatewaysManager.select(1, "Paris").gateway.getName());
        assertEquals("Paris", gatewaysManager.select(2, "Paris").gateway.getName());
        assertEquals(null, gatewaysManager.select(3, "Paris"));
    }

    @Test
    public void testSelectN_selectFromCityWithTimezoneCalculationCityNotExisting_returnsNull() {
        Provider provider = getProvider(null, null, null, null, null, null, "v4/riseup_eipservice_for_geoip_v4.json", "v4/riseup_geoip_v4.json");

        provider.setGeoIpJson(new JSONObject());
        providerObservable.updateProvider(provider);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);
        assertNull(gatewaysManager.select(0, "Stockholm"));
    }

    @Test
    public void testGetLocations_openvpn() {
        Provider provider = getProvider(null, null, null, null, null, null, "v4/riseup_eipservice_for_geoip_v4.json", "v4/riseup_geoip_v4.json");

        providerObservable.updateProvider(provider);
        sharedPreferences.edit().
                putString(PREFERRED_CITY, "Paris").
                commit();
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);
        List<Location> locations = gatewaysManager.getGatewayLocations();

        assertEquals(3, locations.size());
        for (Location location : locations) {
            if ("Paris".equals(location.getName())) {
                assertEquals(3, location.getNumberOfGateways(OPENVPN));
                // manually calculate average load of paris gateways in "v4/riseup_geoip_v4.json"
                double averageLoad = (0.3 + 0.36 + 0.92) / 3.0;
                assertEquals(averageLoad, location.getAverageLoad(OPENVPN));
            }
        }
    }

    @Test
    public void testGetLocations_obfs4() {
        Provider provider = getProvider(null, null, null, null, null, null, "v4/riseup_eipservice_for_geoip_v4.json", "v4/riseup_geoip_v4.json");

        providerObservable.updateProvider(provider);
        sharedPreferences.edit().putBoolean(USE_BRIDGES, true).commit();
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);
        List<Location> locations = gatewaysManager.getGatewayLocations();

        assertEquals(3, locations.size());
        for (Location location : locations) {
            if ("Montreal".equals(location.getName())) {
                assertEquals(1, location.getNumberOfGateways(OBFS4));
                assertEquals(0.59, location.getAverageLoad(OBFS4));
                assertTrue(location.supportsTransport(OBFS4));
            }
            if ("Paris".equals(location.getName())) {
                // checks that only gateways supporting obfs4 are taken into account
                assertEquals(1, location.getNumberOfGateways(OBFS4));
                assertEquals(0.36, location.getAverageLoad(OBFS4));
                assertTrue(location.supportsTransport(OBFS4));
            }
            if ("Amsterdam".equals(location.getName())) {
                assertFalse(location.supportsTransport(OBFS4));
            }
        }

    }

    @Test
    public void testGetLocations_noMenshen_obfs4_calculateAverageLoadFromTimezoneDistance() {
        Provider provider = getProvider(null, null, null, null, null, null, "v4/riseup_eipservice_for_geoip_v4.json", "v4/riseup_geoip_v1.json");

        providerObservable.updateProvider(provider);
        sharedPreferences.edit().putBoolean(USE_BRIDGES, true).commit();
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);
        List<Location> locations = gatewaysManager.getGatewayLocations();

        assertEquals(3, locations.size());
        for (Location location : locations) {
            if ("Montreal".equals(location.getName())) {
                assertEquals(1, location.getNumberOfGateways(OBFS4));
                assertEquals(1/3.0, location.getAverageLoad(OBFS4));
            }
            if ("Paris".equals(location.getName())) {
                // checks that only gateways supporting obfs4 are taken into account
                assertEquals(1, location.getNumberOfGateways(OBFS4));
                assertEquals(0.25, location.getAverageLoad(OBFS4));
            }
        }
    }

    @Test
    public void testGetLocations_noMenshen_openvpn_calculateAverageLoadFromTimezoneDistance() {
        Provider provider = getProvider(null, null, null, null, null, null, "v4/riseup_eipservice_for_geoip_v4.json", "v4/riseup_geoip_v1.json");

        providerObservable.updateProvider(provider);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);
        List<Location> locations = gatewaysManager.getGatewayLocations();

        assertEquals(3, locations.size());
        for (Location location : locations) {
            if ("Montreal".equals(location.getName())) {
                assertEquals(1, location.getNumberOfGateways(OPENVPN));
                assertEquals(1/3.0, location.getAverageLoad(OPENVPN));
            }
            if ("Paris".equals(location.getName())) {
                // checks that only gateways supporting obfs4 are taken into account
                assertEquals(3, location.getNumberOfGateways(OPENVPN));
                assertEquals(0.25, location.getAverageLoad(OPENVPN));
            }
        }
    }


    @Test
    public void testGetSortedLocations_openvpn() {
        Provider provider = getProvider(null, null, null, null, null, null, "v4/riseup_eipservice_for_geoip_v4.json", "v4/riseup_geoip_v4_bad_obfs4_gateway.json");

        providerObservable.updateProvider(provider);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);
        List<Location> locations = gatewaysManager.getSortedGatewayLocations(OPENVPN);

        assertEquals(3, locations.size());

        /** -> v4/riseup_geoip_v4_bad_obfs4_gateway.json OPENVPN
         * Paris = 0.527
         * 0.36 - zarapito
         * 0.92 - hoazin
         * 0.3 - mouette
         *
         * Montreal = 0.59
         * 0.59 - yal
         *
         * Amsterdam = 0.8
         * 0.8 - redshank
         */
        assertEquals("Paris", locations.get(0).getName());
        assertEquals("Montreal", locations.get(1).getName());
        assertEquals("Amsterdam", locations.get(2).getName());
    }

    @Test
    public void testGetSortedLocations_obfs4() {
        Provider provider = getProvider(null, null, null, null, null, null, "v4/riseup_eipservice_for_geoip_v4.json", "v4/riseup_geoip_v4_bad_obfs4_gateway.json");

        providerObservable.updateProvider(provider);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);
        List<Location> locations = gatewaysManager.getSortedGatewayLocations(OBFS4);

        assertEquals(3, locations.size());

        /** -> v4/riseup_geoip_v4_bad_obfs4_gateway.json OBFS4
         * Paris = 0.92
         * 0.92 - hoazin
         *
         * Montreal = 0.59
         * 0.59 - yal
         *
         * Amsterdam = 0.0 - no obfs4
         * 0.0 - redshank
         */
        assertEquals("Montreal", locations.get(0).getName());
        assertEquals("Paris", locations.get(1).getName());
        assertEquals("Amsterdam", locations.get(2).getName());
    }

    // Currently all pluggable transports are handled the same, there's no UI to select a specific one
    // when requesting a sorted list for any pluggabled transport, a sorted list of all pluiggable transports
    // will be returned
    @Test
    public void testGetSortedLocations_obfs4kcp_generalizedAsPT() {
        Provider provider = getProvider(null, null, null, null, null, null, "v4/riseup_eipservice_for_geoip_v4.json", "v4/riseup_geoip_v4_bad_obfs4_gateway.json");

        providerObservable.updateProvider(provider);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);
        List<Location> locations = gatewaysManager.getSortedGatewayLocations(OBFS4);

        assertEquals(3, locations.size());
    }

    @Test
    public void testgetAverageLoad_isSameForAllTransports() {
        Provider provider = getProvider(null, null, null, null, null, null, "ptdemo_kcp_gateways.json", "ptdemo_kcp_gateways_geoip.json");

        providerObservable.updateProvider(provider);
        sharedPreferences.edit().
                putString(PREFERRED_CITY, "Paris").
                commit();
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        assertEquals(0.3, gatewaysManager.getLocation("Amsterdam").getAverageLoad(OBFS4));
        assertEquals(0.3, gatewaysManager.getLocation("Amsterdam").getAverageLoad(OPENVPN));
    }

    @Test
    public void testGetLoadForLocation_() {
        providerObservable.updateProvider(null);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);
        assertEquals(GatewaysManager.Load.UNKNOWN, gatewaysManager.getLoadForLocation("unknown city", OPENVPN));
    }

    @Test
    public void testGatewayManagerFromCurrentProvider_decoupledBridges_twoGateways() throws IOException, NullPointerException {
        Provider provider = getProvider(null, null, null, null, null, null, "decoupled_pt.eip-service.json", null);
        providerObservable.updateProvider(provider);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);
        assertEquals(2, gatewaysManager.size());
    }

    @Test
    public void testGatewayManagerFromCurrentProvider_decoupledBridgesIncludingExperimental_threeGateways() throws IOException, NullPointerException {
        Provider provider = getProvider(null, null, null, null, null, null, "decoupled_pt.eip-service.json", null);
        providerObservable.updateProvider(provider);
        sharedPreferences.edit().
                putBoolean(USE_BRIDGES, true).
                putBoolean(ALLOW_EXPERIMENTAL_TRANSPORTS, true).
                commit();
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);
        assertEquals(3, gatewaysManager.size());
    }

    private String getJsonStringFor(String filename) throws IOException {
        return TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream(filename));
    }

    private void updateEipServiceJson(String filename) throws IOException {
        String eipServiceJson = getJsonStringFor(filename);
        sharedPreferences.edit().putString(PROVIDER_EIP_DEFINITION, eipServiceJson).commit();
    }
}