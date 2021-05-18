package se.leap.bitmaskclient.eip;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.List;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import se.leap.bitmaskclient.base.models.Location;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.models.ProviderObservable;
import se.leap.bitmaskclient.testutils.MockHelper;
import se.leap.bitmaskclient.testutils.MockSharedPreferences;
import se.leap.bitmaskclient.testutils.TestSetupHelper;
import se.leap.bitmaskclient.base.utils.ConfigHelper;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;

import static de.blinkt.openvpn.core.connection.Connection.TransportType.OBFS4;
import static de.blinkt.openvpn.core.connection.Connection.TransportType.OPENVPN;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static se.leap.bitmaskclient.base.models.Constants.GATEWAYS;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_EIP_DEFINITION;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_PRIVATE_KEY;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.base.models.Provider.CA_CERT;
import static se.leap.bitmaskclient.testutils.MockHelper.mockTextUtils;
import static se.leap.bitmaskclient.testutils.TestSetupHelper.getProvider;

/**
 * Created by cyberta on 09.10.17.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ProviderObservable.class, Log.class, PreferenceHelper.class, ConfigHelper.class, TextUtils.class})
public class GatewaysManagerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mockContext;

    private SharedPreferences sharedPreferences;
    private JSONObject secrets;


    @Before
    public void setUp() throws IOException, JSONException {
        mockStatic(Log.class);
        mockStatic(ConfigHelper.class);
        mockTextUtils();
        when(ConfigHelper.getCurrentTimezone()).thenReturn(-1);
        when(ConfigHelper.stringEqual(anyString(), anyString())).thenCallRealMethod();
        secrets = new JSONObject(getJsonStringFor("secrets.json"));
        sharedPreferences = new MockSharedPreferences();
        sharedPreferences.edit().
                putString(PROVIDER_PRIVATE_KEY, secrets.getString(PROVIDER_PRIVATE_KEY)).
                putString(CA_CERT, secrets.getString(CA_CERT)).
                putString(PROVIDER_VPN_CERTIFICATE, secrets.getString(PROVIDER_VPN_CERTIFICATE))
                .commit();
    }


    @Test
    public void testGatewayManagerFromCurrentProvider_noProvider_noGateways() {
        MockHelper.mockProviderObserver(null);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);
        assertEquals(0, gatewaysManager.size());
    }

    @Test
    public void testGatewayManagerFromCurrentProvider_misconfiguredProvider_noGateways() throws IOException, NullPointerException {
        Provider provider = getProvider(null, null, null, null, null, null, "ptdemo_misconfigured_gateway.json", null);
        MockHelper.mockProviderObserver(provider);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);
        assertEquals(0, gatewaysManager.size());
    }

    @Test
    public void testGatewayManagerFromCurrentProvider_threeGateways() {
        Provider provider = getProvider(null, null, null, null,null, null, "ptdemo_three_mixed_gateways.json", null);
        MockHelper.mockProviderObserver(provider);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);
        assertEquals(3, gatewaysManager.size());
    }

    @Test
    public void TestGetPosition_VpnProfileExtistingObfs4_returnPositionZero() throws JSONException, ConfigParser.ConfigParseError, IOException {
        Provider provider = getProvider(null, null, null, null, null, null, "ptdemo_three_mixed_gateways.json", null);
        JSONObject eipServiceJson = provider.getEipServiceJson();
        JSONObject gateway1 = eipServiceJson.getJSONArray(GATEWAYS).getJSONObject(0);
        MockHelper.mockProviderObserver(provider);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        VpnConfigGenerator configGenerator = new VpnConfigGenerator(provider.getDefinition(), secrets, gateway1, 3);
        VpnProfile profile = configGenerator.createProfile(OBFS4);
        profile.mGatewayIp = "37.218.247.60";

        assertEquals(0, gatewaysManager.getPosition(profile));
    }

    @Test
    public void TestGetPosition_VpnProfileExtistingOpenvpn_returnPositionZero() throws JSONException, ConfigParser.ConfigParseError, IOException {
        Provider provider = getProvider(null, null, null, null, null, null, "ptdemo_three_mixed_gateways.json", null);
        JSONObject eipServiceJson = provider.getEipServiceJson();
        JSONObject gateway1 = eipServiceJson.getJSONArray(GATEWAYS).getJSONObject(0);
        MockHelper.mockProviderObserver(provider);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        VpnConfigGenerator configGenerator = new VpnConfigGenerator(provider.getDefinition(), secrets, gateway1, 3);
        VpnProfile profile = configGenerator.createProfile(OPENVPN);
        profile.mGatewayIp = "37.218.247.60";

        assertEquals(0, gatewaysManager.getPosition(profile));
    }

    @Test
    public void TestGetPosition_VpnProfileExistingObfs4FromPresortedList_returnsPositionOne() throws JSONException, ConfigParser.ConfigParseError, IOException {
        Provider provider = getProvider(null, null, null, null, null, null, "ptdemo_three_mixed_gateways.json", "ptdemo_three_mixed_gateways.geoip.json");
        JSONObject eipServiceJson = provider.getEipServiceJson();
        JSONObject gateway1 = eipServiceJson.getJSONArray(GATEWAYS).getJSONObject(0);
        MockHelper.mockProviderObserver(provider);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        VpnConfigGenerator configGenerator = new VpnConfigGenerator(provider.getDefinition(), secrets, gateway1, 3);
        VpnProfile profile = configGenerator.createProfile(OBFS4);
        profile.mGatewayIp = "37.218.247.60";

        assertEquals(1, gatewaysManager.getPosition(profile));
    }

    @Test
    public void TestGetPosition_VpnProfileExistingOpenvpnFromPresortedList_returnsPositionOne() throws JSONException, ConfigParser.ConfigParseError, IOException {
        Provider provider = getProvider(null, null, null, null, null, null, "ptdemo_three_mixed_gateways.json", "ptdemo_three_mixed_gateways.geoip.json");
        JSONObject eipServiceJson = provider.getEipServiceJson();
        JSONObject gateway1 = eipServiceJson.getJSONArray(GATEWAYS).getJSONObject(0);
        MockHelper.mockProviderObserver(provider);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        VpnConfigGenerator configGenerator = new VpnConfigGenerator(provider.getDefinition(), secrets, gateway1, 3);
        VpnProfile profile = configGenerator.createProfile(OPENVPN);
        profile.mGatewayIp = "37.218.247.60";

        assertEquals(2, gatewaysManager.getPosition(profile));
    }

    @Test
    public void TestGetPosition_VpnProfileDifferentIp_returnMinusOne() throws JSONException, ConfigParser.ConfigParseError, IOException {
        Provider provider = getProvider(null, null, null, null, null, null, "ptdemo_three_mixed_gateways.json", null);
        JSONObject eipServiceJson = provider.getEipServiceJson();
        JSONObject gateway1 = eipServiceJson.getJSONArray(GATEWAYS).getJSONObject(0);
        MockHelper.mockProviderObserver(provider);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        VpnConfigGenerator configGenerator = new VpnConfigGenerator(provider.getDefinition(), secrets, gateway1, 3);
        VpnProfile profile = configGenerator.createProfile(OBFS4);
        profile.mGatewayIp = "37.218.247.61";

        assertEquals(-1, gatewaysManager.getPosition(profile));
    }

    @Test
    public void TestGetPosition_VpnProfileMoscow_returnOne() throws JSONException, ConfigParser.ConfigParseError, IOException {
        Provider provider = getProvider(null, null, null, null, null, null, "ptdemo_three_mixed_gateways.json", null);
        JSONObject eipServiceJson = provider.getEipServiceJson();
        JSONObject gateway1 = eipServiceJson.getJSONArray(GATEWAYS).getJSONObject(1);
        MockHelper.mockProviderObserver(provider);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        VpnConfigGenerator configGenerator = new VpnConfigGenerator(provider.getDefinition(), secrets, gateway1, 3);
        VpnProfile profile = configGenerator.createProfile(OBFS4);
        profile.mGatewayIp = "3.21.247.89";

        assertEquals(1, gatewaysManager.getPosition(profile));
    }

    @Test
    public void TestSelectN_selectFirstObfs4Connection_returnThirdGateway() {
        Provider provider = getProvider(null, null, null, null, null, null, "ptdemo_two_openvpn_one_pt_gateways.json", null);

        MockHelper.mockProviderObserver(provider);
        mockStatic(PreferenceHelper.class);
        when(PreferenceHelper.getUsePluggableTransports(any(Context.class))).thenReturn(true);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        assertEquals("37.12.247.10", gatewaysManager.select(0).getRemoteIP());
    }

    @Test
    public void testSelectN_selectFromPresortedGateways_returnsGatewaysInPresortedOrder() {
        Provider provider = getProvider(null, null, null, null, null, null, "ptdemo_three_mixed_gateways.json", "ptdemo_three_mixed_gateways.geoip.json");

        MockHelper.mockProviderObserver(provider);
        //use openvpn, not pluggable transports
        mockStatic(PreferenceHelper.class);
        when(PreferenceHelper.getUsePluggableTransports(any(Context.class))).thenReturn(false);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        assertEquals("manila.bitmask.net", gatewaysManager.select(0).getHost());
        assertEquals("moscow.bitmask.net", gatewaysManager.select(1).getHost());
        assertEquals("pt.demo.bitmask.net", gatewaysManager.select(2).getHost());
    }

    @Test
    public void testSelectN_selectObfs4FromPresortedGateways_returnsObfs4GatewaysInPresortedOrder() {
        Provider provider = getProvider(null, null, null, null, null, null, "ptdemo_three_mixed_gateways.json", "ptdemo_three_mixed_gateways.geoip.json");

        MockHelper.mockProviderObserver(provider);
        //use openvpn, not pluggable transports
        mockStatic(PreferenceHelper.class);
        when(PreferenceHelper.getUsePluggableTransports(any(Context.class))).thenReturn(true);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        assertEquals("moscow.bitmask.net", gatewaysManager.select(0).getHost());
        assertEquals("pt.demo.bitmask.net", gatewaysManager.select(1).getHost());
        assertNull(gatewaysManager.select(2));
    }


    @Test
    public void testSelectN_selectFromCity_returnsGatewaysInPresortedOrder() {
        Provider provider = getProvider(null, null, null, null, null, null, "v4/riseup_eipservice_for_geoip_v4.json", "v4/riseup_geoip_v4.json");

        MockHelper.mockProviderObserver(provider);
        //use openvpn, not pluggable transports
        mockStatic(PreferenceHelper.class);
        when(PreferenceHelper.getUsePluggableTransports(any(Context.class))).thenReturn(false);
        when(PreferenceHelper.getPreferredCity(any(Context.class))).thenReturn("Paris");
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        assertEquals("mouette.riseup.net", gatewaysManager.select(0).getHost());
        assertEquals("hoatzin.riseup.net", gatewaysManager.select(1).getHost());
        assertEquals("zarapito.riseup.net", gatewaysManager.select(2).getHost());
    }

    @Test
    public void testSelectN_selectFromCityWithGeoIpServiceV1_returnsGatewaysInPresortedOrder() {
        Provider provider = getProvider(null, null, null, null, null, null, "v4/riseup_eipservice_for_geoip_v4.json", "v4/riseup_geoip_v1.json");

        MockHelper.mockProviderObserver(provider);
        //use openvpn, not pluggable transports
        mockStatic(PreferenceHelper.class);
        when(PreferenceHelper.getUsePluggableTransports(any(Context.class))).thenReturn(false);
        when(PreferenceHelper.getPreferredCity(any(Context.class))).thenReturn("Paris");
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        assertEquals("mouette.riseup.net", gatewaysManager.select(0).getHost());
        assertEquals("hoatzin.riseup.net", gatewaysManager.select(1).getHost());
        assertEquals("zarapito.riseup.net", gatewaysManager.select(2).getHost());
    }

    @Test
    public void testSelectN_selectFromCityWithTimezoneCalculation_returnsRandomizedGatewaysOfSelectedCity() {
        Provider provider = getProvider(null, null, null, null, null, null, "v4/riseup_eipservice_for_geoip_v4.json", null);

        provider.setGeoIpJson(new JSONObject());
        MockHelper.mockProviderObserver(provider);
        //use openvpn, not pluggable transports
        mockStatic(PreferenceHelper.class);
        when(PreferenceHelper.getUsePluggableTransports(any(Context.class))).thenReturn(false);
        when(PreferenceHelper.getPreferredCity(any(Context.class))).thenReturn("Paris");
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        assertEquals("Paris", gatewaysManager.select(0).getName());
        assertEquals("Paris", gatewaysManager.select(1).getName());
        assertEquals("Paris", gatewaysManager.select(2).getName());
        assertEquals(null, gatewaysManager.select(3));
    }

    @Test
    public void testSelectN_selectNAndCity_returnsGatewaysInPresortedOrder() {
        Provider provider = getProvider(null, null, null, null, null, null, "v4/riseup_eipservice_for_geoip_v4.json", "v4/riseup_geoip_v4.json");

        MockHelper.mockProviderObserver(provider);
        //use openvpn, not pluggable transports
        mockStatic(PreferenceHelper.class);
        when(PreferenceHelper.getUsePluggableTransports(any(Context.class))).thenReturn(false);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        assertEquals("mouette.riseup.net", gatewaysManager.select(0, "Paris").getHost());
        assertEquals("hoatzin.riseup.net", gatewaysManager.select(1, "Paris").getHost());
        assertEquals("zarapito.riseup.net", gatewaysManager.select(2, "Paris").getHost());
    }

    @Test
    public void testSelectN_selectNAndCityWithGeoIpServiceV1_returnsGatewaysInPresortedOrder() {
        Provider provider = getProvider(null, null, null, null, null, null, "v4/riseup_eipservice_for_geoip_v4.json", "v4/riseup_geoip_v1.json");

        MockHelper.mockProviderObserver(provider);
        //use openvpn, not pluggable transports
        mockStatic(PreferenceHelper.class);
        when(PreferenceHelper.getUsePluggableTransports(any(Context.class))).thenReturn(false);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        assertEquals("mouette.riseup.net", gatewaysManager.select(0, "Paris").getHost());
        assertEquals("hoatzin.riseup.net", gatewaysManager.select(1, "Paris").getHost());
        assertEquals("zarapito.riseup.net", gatewaysManager.select(2, "Paris").getHost());
    }

    @Test
    public void testSelectN_selectNAndCityWithTimezoneCalculation_returnsRandomizedGatewaysOfSelectedCity() {
        Provider provider = getProvider(null, null, null, null, null, null, "v4/riseup_eipservice_for_geoip_v4.json", null);

        provider.setGeoIpJson(new JSONObject());
        MockHelper.mockProviderObserver(provider);
        //use openvpn, not pluggable transports
        mockStatic(PreferenceHelper.class);
        when(PreferenceHelper.getUsePluggableTransports(any(Context.class))).thenReturn(false);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        assertEquals("Paris", gatewaysManager.select(0, "Paris").getName());
        assertEquals("Paris", gatewaysManager.select(1, "Paris").getName());
        assertEquals("Paris", gatewaysManager.select(2, "Paris").getName());
        assertEquals(null, gatewaysManager.select(3, "Paris"));
    }

    @Test
    public void testSelectN_selectFromCityWithTimezoneCalculationCityNotExisting_returnsNull() {
        Provider provider = getProvider(null, null, null, null, null, null, "v4/riseup_eipservice_for_geoip_v4.json", "v4/riseup_geoip_v4.json");

        provider.setGeoIpJson(new JSONObject());
        MockHelper.mockProviderObserver(provider);
        //use openvpn, not pluggable transports
        mockStatic(PreferenceHelper.class);
        when(PreferenceHelper.getUsePluggableTransports(any(Context.class))).thenReturn(false);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);
        assertNull(gatewaysManager.select(0, "Stockholm"));
    }

    @Test
    public void testGetLocations_openvpn() {
        Provider provider = getProvider(null, null, null, null, null, null, "v4/riseup_eipservice_for_geoip_v4.json", "v4/riseup_geoip_v4.json");

        MockHelper.mockProviderObserver(provider);
        mockStatic(PreferenceHelper.class);
        when(PreferenceHelper.getUsePluggableTransports(any(Context.class))).thenReturn(false);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);
        List<Location> locations = gatewaysManager.getGatewayLocations();

        assertEquals(3, locations.size());
        for (Location location : locations) {
            if ("Paris".equals(location.name)) {
                assertEquals(3, location.numberOfGateways);
                // manually calculate average load of paris gateways in "v4/riseup_geoip_v4.json"
                double averageLoad = (0.3 + 0.36 + 0.92) / 3.0;
                assertEquals(averageLoad, location.averageLoad);
            }
        }
    }

    @Test
    public void testGetLocations_obfs4() {
        Provider provider = getProvider(null, null, null, null, null, null, "v4/riseup_eipservice_for_geoip_v4.json", "v4/riseup_geoip_v4.json");

        MockHelper.mockProviderObserver(provider);
        mockStatic(PreferenceHelper.class);
        when(PreferenceHelper.getUsePluggableTransports(any(Context.class))).thenReturn(true);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);
        List<Location> locations = gatewaysManager.getGatewayLocations();

        assertEquals(2, locations.size());
        for (Location location : locations) {
            if ("Montreal".equals(location.name)) {
                assertEquals(1, location.numberOfGateways);
                assertEquals(0.59, location.averageLoad);
            }
            if ("Paris".equals(location.name)) {
                // checks that only gateways supporting obfs4 are taken into account
                assertEquals(1, location.numberOfGateways);
                assertEquals(0.36, location.averageLoad);
            }
        }

    }


    private String getJsonStringFor(String filename) throws IOException {
        return TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream(filename));
    }

    private void updateEipServiceJson(String filename) throws IOException {
        String eipServiceJson = getJsonStringFor(filename);
        sharedPreferences.edit().putString(PROVIDER_EIP_DEFINITION, eipServiceJson).commit();
    }
}