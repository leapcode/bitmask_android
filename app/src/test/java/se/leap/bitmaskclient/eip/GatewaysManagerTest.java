package se.leap.bitmaskclient.eip;

import android.content.Context;
import android.content.SharedPreferences;
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

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.connection.Connection;
import se.leap.bitmaskclient.Provider;
import se.leap.bitmaskclient.ProviderObservable;
import se.leap.bitmaskclient.testutils.MockHelper;
import se.leap.bitmaskclient.testutils.MockSharedPreferences;
import se.leap.bitmaskclient.testutils.TestSetupHelper;
import se.leap.bitmaskclient.utils.ConfigHelper;
import se.leap.bitmaskclient.utils.PreferenceHelper;

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
import static se.leap.bitmaskclient.Constants.GATEWAYS;
import static se.leap.bitmaskclient.Constants.PROVIDER_EIP_DEFINITION;
import static se.leap.bitmaskclient.Constants.PROVIDER_PRIVATE_KEY;
import static se.leap.bitmaskclient.Constants.PROVIDER_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.Provider.CA_CERT;
import static se.leap.bitmaskclient.testutils.TestSetupHelper.getProvider;

/**
 * Created by cyberta on 09.10.17.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ProviderObservable.class, Log.class, PreferenceHelper.class, ConfigHelper.class})
public class GatewaysManagerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mockContext;

    private SharedPreferences sharedPreferences;
    private JSONObject secrets;


    @Before
    public void setUp() throws IOException, JSONException {
        mockStatic(Log.class);
        mockStatic(ConfigHelper.class);
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
    public void testFromEipServiceJson_emptyJson() throws Exception {
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext, sharedPreferences);
        assertEquals(0, gatewaysManager.size());
    }

    @Test
    public void testFromEipServiceJson_ignoreGatewaysWithMisconfiguredTransportsWhileAddingValidOnes() throws Exception {
        updateEipServiceJson("ptdemo_misconfigured_mixed_gateways.json");
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext, sharedPreferences);
        assertEquals(1, gatewaysManager.size());
        assertNull(gatewaysManager.select(0).getProfile(OBFS4));
        assertNotNull(gatewaysManager.select(0).getProfile(Connection.TransportType.OPENVPN));
    }

    @Test
    public void testClearGatewaysAndProfiles_resetGateways() throws Exception {
        updateEipServiceJson("eip-service-two-gateways.json");
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext, sharedPreferences);
        assertEquals(2, gatewaysManager.size());
        gatewaysManager.clearGateways();
        assertEquals(0, gatewaysManager.size());
    }

    @Test
    public void testGatewayManagerFromCurrentProvider_noProvider_noGateways() {
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);
        assertEquals(0, gatewaysManager.size());
    }

    @Test
    public void testGatewayManagerFromCurrentProvider_misconfiguredProvider_noGateways() throws IOException, NullPointerException {
        Provider provider = getProvider(null, null, null, "ptdemo_misconfigured_gateway.json");
        MockHelper.mockProviderObserver(provider);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);
        assertEquals(0, gatewaysManager.size());
    }

    @Test
    public void testGatewayManagerFromCurrentProvider_threeGateways() {
        Provider provider = getProvider(null, null, null, "ptdemo_three_mixed_gateways.json");
        MockHelper.mockProviderObserver(provider);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);
        assertEquals(3, gatewaysManager.size());
    }

    @Test
    public void TestGetPosition_VpnProfileExtistingObfs4_returnPositionZero() throws JSONException, ConfigParser.ConfigParseError, IOException {
        Provider provider = getProvider(null, null, null, "ptdemo_three_mixed_gateways.json");
        JSONObject eipServiceJson = provider.getEipServiceJson();
        JSONObject gateway1 = eipServiceJson.getJSONArray(GATEWAYS).getJSONObject(0);
        MockHelper.mockProviderObserver(provider);
        mockStatic(PreferenceHelper.class);
        when(PreferenceHelper.getUsePluggableTransports(any(Context.class))).thenReturn(true);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        VpnConfigGenerator configGenerator = new VpnConfigGenerator(provider.getDefinition(), secrets, gateway1, 3);
        VpnProfile profile = configGenerator.createProfile(OBFS4);
        profile.mGatewayIp = "37.218.247.60";

        assertEquals(0, gatewaysManager.getPosition(profile));
    }

    @Test
    public void TestGetPosition_VpnProfileExtistingOpenvpn_returnPositionZero() throws JSONException, ConfigParser.ConfigParseError, IOException {
        Provider provider = getProvider(null, null, null, "ptdemo_three_mixed_gateways.json");
        JSONObject eipServiceJson = provider.getEipServiceJson();
        JSONObject gateway1 = eipServiceJson.getJSONArray(GATEWAYS).getJSONObject(0);
        MockHelper.mockProviderObserver(provider);
        mockStatic(PreferenceHelper.class);
        when(PreferenceHelper.getUsePluggableTransports(any(Context.class))).thenReturn(false);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        VpnConfigGenerator configGenerator = new VpnConfigGenerator(provider.getDefinition(), secrets, gateway1, 3);
        VpnProfile profile = configGenerator.createProfile(OPENVPN);
        profile.mGatewayIp = "37.218.247.60";

        assertEquals(0, gatewaysManager.getPosition(profile));
    }

    @Test
    public void TestGetPosition_VpnProfileDifferentIp_returnMinusOne() throws JSONException, ConfigParser.ConfigParseError, IOException {
        Provider provider = getProvider(null, null, null, "ptdemo_three_mixed_gateways.json");
        JSONObject eipServiceJson = provider.getEipServiceJson();
        JSONObject gateway1 = eipServiceJson.getJSONArray(GATEWAYS).getJSONObject(0);
        MockHelper.mockProviderObserver(provider);
        mockStatic(PreferenceHelper.class);
        when(PreferenceHelper.getUsePluggableTransports(any(Context.class))).thenReturn(true);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        VpnConfigGenerator configGenerator = new VpnConfigGenerator(provider.getDefinition(), secrets, gateway1, 3);
        VpnProfile profile = configGenerator.createProfile(OBFS4);
        profile.mGatewayIp = "37.218.247.61";

        assertEquals(-1, gatewaysManager.getPosition(profile));
    }

    @Test
    public void TestGetPosition_VpnProfileMoscow_returnOne() throws JSONException, ConfigParser.ConfigParseError, IOException {
        Provider provider = getProvider(null, null, null, "ptdemo_three_mixed_gateways.json");
        JSONObject eipServiceJson = provider.getEipServiceJson();
        JSONObject gateway1 = eipServiceJson.getJSONArray(GATEWAYS).getJSONObject(1);
        MockHelper.mockProviderObserver(provider);
        mockStatic(PreferenceHelper.class);
        when(PreferenceHelper.getUsePluggableTransports(any(Context.class))).thenReturn(true);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        VpnConfigGenerator configGenerator = new VpnConfigGenerator(provider.getDefinition(), secrets, gateway1, 3);
        VpnProfile profile = configGenerator.createProfile(OBFS4);
        profile.mGatewayIp = "3.21.247.89";

        assertEquals(1, gatewaysManager.getPosition(profile));
    }

    @Test
    public void TestSelectN_selectFirstObfs4Connection_returnThirdGateway() throws JSONException, ConfigParser.ConfigParseError, IOException {
        Provider provider = getProvider(null, null, null, "ptdemo_two_openvpn_one_pt_gateways.json");
        JSONObject eipServiceJson = provider.getEipServiceJson();
        JSONObject gateway3 = eipServiceJson.getJSONArray(GATEWAYS).getJSONObject(2);

        MockHelper.mockProviderObserver(provider);
        mockStatic(PreferenceHelper.class);
        when(PreferenceHelper.getUsePluggableTransports(any(Context.class))).thenReturn(true);
        GatewaysManager gatewaysManager = new GatewaysManager(mockContext);

        assertEquals("37.12.247.10", gatewaysManager.select(0).getRemoteIP());
    }

    private String getJsonStringFor(String filename) throws IOException {
        return TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream(filename));
    }

    private void updateEipServiceJson(String filename) throws IOException {
        String eipServiceJson = getJsonStringFor(filename);
        sharedPreferences.edit().putString(PROVIDER_EIP_DEFINITION, eipServiceJson).commit();
    }
}