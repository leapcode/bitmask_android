package se.leap.bitmaskclient.eip;

import static org.junit.Assert.*;

import static se.leap.bitmaskclient.base.models.Constants.GATEWAYS;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_PRIVATE_KEY;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.base.models.Provider.CA_CERT;
import static se.leap.bitmaskclient.testutils.TestSetupHelper.getProvider;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.connection.Connection;
import de.blinkt.openvpn.core.connection.Obfs4Connection;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.models.ProviderObservable;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.base.utils.TimezoneHelper;
import se.leap.bitmaskclient.testutils.MockSharedPreferences;
import se.leap.bitmaskclient.testutils.TestSetupHelper;

public class GatewayTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mockContext;

    private SharedPreferences sharedPreferences;

    @Before
    public void setUp() throws IOException, JSONException {
        sharedPreferences = new MockSharedPreferences();

        PreferenceHelper preferenceHelper = new PreferenceHelper(sharedPreferences);
    }


    private Gateway createGatewayFromProvider(int nClosest, @Nullable String eipServiceJsonPath) throws ConfigParser.ConfigParseError, JSONException, IOException {
        Provider provider = getProvider(null, null, null, null, null, null, eipServiceJsonPath, null);
        JSONObject eipServiceJson = provider.getEipServiceJson();

        JSONObject gatewayJson = eipServiceJson.getJSONArray(GATEWAYS).getJSONObject(0);
        JSONObject secrets = new JSONObject();
        try {
            secrets.put(Provider.CA_CERT, provider.getCaCert());
            secrets.put(PROVIDER_VPN_CERTIFICATE, provider.getVpnCertificate());
        } catch (JSONException e) {
            e.printStackTrace();
        }


        return new Gateway(eipServiceJson, secrets, gatewayJson);
    }
    @Test
    public void testGetProfile_OpenVPN_obfuscationProtocols_ignored_OpenVPNfound() throws ConfigParser.ConfigParseError, JSONException, IOException {
        Gateway gateway = createGatewayFromProvider(0, "ptdemo_three_mixed_gateways.json");
        VpnProfile profile = gateway.getProfile(Connection.TransportType.OPENVPN, new HashSet<>(Arrays.asList("invalid")));
        assertNotNull(profile);
    }

    @Test
    public void testGetProfile_obfs4_obfuscationProtocolsTakenIntoAccount_Obfs4Notfound() throws ConfigParser.ConfigParseError, JSONException, IOException {
        Gateway gateway = createGatewayFromProvider(0, "ptdemo_three_mixed_gateways.json");
        VpnProfile profile = gateway.getProfile(Connection.TransportType.OBFS4, new HashSet<>(Arrays.asList("invalid")));
        assertNull(profile);
    }

    @Test
    public void testGetProfile_obfs4_obfuscationProtocolsTakenIntoAccount_Obfs4found() throws ConfigParser.ConfigParseError, JSONException, IOException {
        Gateway gateway = createGatewayFromProvider(0, "ptdemo_three_mixed_gateways.json");
        VpnProfile profile = gateway.getProfile(Connection.TransportType.OBFS4, new HashSet<>(Arrays.asList("tcp")));
        assertNotNull(profile);
    }

    @Test
    public void testGetProfile_obfs4_obfuscationProtocolsTakenIntoAccount_Obfs4KCPfound() throws ConfigParser.ConfigParseError, JSONException, IOException {
        Gateway gateway = createGatewayFromProvider(0, "multiple_pts_per_host_eip-service.json");
        VpnProfile profile = gateway.getProfile(Connection.TransportType.OBFS4, new HashSet<>(Arrays.asList("kcp")));
        assertNotNull(profile);
    }

    @Test
    public void testGetProfile_obfs4_multipleProfiles_randomlySelected() throws ConfigParser.ConfigParseError, JSONException, IOException {
        Gateway gateway = createGatewayFromProvider(0, "multiple_pts_per_host_eip-service.json");
        VpnProfile profile1 = gateway.getProfile(Connection.TransportType.OBFS4, new HashSet<>(Arrays.asList("kcp", "tcp")));
        assertNotNull(profile1);
        assertEquals(1, profile1.mConnections.length);
        assertTrue(profile1.mConnections[0] instanceof Obfs4Connection);
        String[] transportLayerProtocols = ((Obfs4Connection)profile1.mConnections[0]).getObfs4Options().transport.getProtocols();

        boolean profileWithDifferentTransportLayerProtosFound = false;
        for (int i = 0; i < 1000; i++) {
            VpnProfile otherProfile = gateway.getProfile(Connection.TransportType.OBFS4, new HashSet<>(Arrays.asList("kcp", "tcp")));
            String[] otherProtocols =  ((Obfs4Connection)otherProfile.mConnections[0]).getObfs4Options().transport.getProtocols();
            if (!transportLayerProtocols[0].equals(otherProtocols[0])) {
                profileWithDifferentTransportLayerProtosFound = true;
                System.out.println(i + 1 + " attempts");
                break;
            }
        }
        assertTrue(profileWithDifferentTransportLayerProtosFound);
    }

    @Test
    public void testSupportsTransport() throws ConfigParser.ConfigParseError, JSONException, IOException {
        Gateway gateway = createGatewayFromProvider(0, "multiple_pts_per_host_eip-service.json");
        assertFalse(gateway.supportsTransport(Connection.TransportType.OBFS4_HOP, null));
        assertTrue(gateway.supportsTransport(Connection.TransportType.OBFS4, null));
        assertTrue(gateway.supportsTransport(Connection.TransportType.OBFS4, new HashSet<>(Arrays.asList("kcp"))));
        assertTrue(gateway.supportsTransport(Connection.TransportType.OBFS4, new HashSet<>(Arrays.asList("tcp"))));
        assertFalse(gateway.supportsTransport(Connection.TransportType.OBFS4, new HashSet<>(Arrays.asList("invalid"))));
    }

    @Test
    public void testGetSupportedTransports() throws ConfigParser.ConfigParseError, JSONException, IOException {
        Gateway gateway = createGatewayFromProvider(0, "multiple_pts_per_host_eip-service.json");
        assertEquals(2, gateway.getSupportedTransports().size());
        assertTrue(gateway.getSupportedTransports().contains(Connection.TransportType.OBFS4));
        assertTrue(gateway.getSupportedTransports().contains(Connection.TransportType.OPENVPN));
    }

    @Test
    public void testHasProfile() throws ConfigParser.ConfigParseError, JSONException, IOException {
        Gateway gateway = createGatewayFromProvider(0, "multiple_pts_per_host_eip-service.json");
        VpnProfile profile = gateway.getProfiles().get(0);
        String profileString = profile.toJson();
        VpnProfile newProfile = VpnProfile.fromJson(profileString);
        assertTrue(gateway.hasProfile(newProfile));

        newProfile.mGatewayIp = "XXXX";
        assertFalse(gateway.hasProfile(newProfile));

        VpnProfile newProfile2 = VpnProfile.fromJson(profileString);
        newProfile2.mConnections = new Connection[0];
        assertFalse(gateway.hasProfile(newProfile2));
    }


}