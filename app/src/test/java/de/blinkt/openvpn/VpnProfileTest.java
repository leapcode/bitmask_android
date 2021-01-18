package de.blinkt.openvpn;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.UUID;

import de.blinkt.openvpn.core.connection.Obfs4Connection;
import de.blinkt.openvpn.core.connection.OpenvpnConnection;
import se.leap.bitmaskclient.pluggableTransports.Obfs4Options;

import static de.blinkt.openvpn.core.connection.Connection.TransportType.OBFS4;
import static de.blinkt.openvpn.core.connection.Connection.TransportType.OPENVPN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@PrepareForTest({UUID.class})
public class VpnProfileTest {

    private static final String OPENVPNCONNECTION_PROFILE   = "{\"mAuthenticationType\":2,\"mName\":\"mockProfile\",\"mTLSAuthDirection\":\"\",\"mUseLzo\":false,\"mUseTLSAuth\":false,\"mDNS1\":\"8.8.8.8\",\"mDNS2\":\"8.8.4.4\",\"mOverrideDNS\":false,\"mSearchDomain\":\"blinkt.de\",\"mUseDefaultRoute\":true,\"mUsePull\":true,\"mCheckRemoteCN\":true,\"mExpectTLSCert\":false,\"mRemoteCN\":\"\",\"mPassword\":\"\",\"mUsername\":\"\",\"mRoutenopull\":false,\"mUseRandomHostname\":false,\"mUseFloat\":false,\"mUseCustomConfig\":false,\"mCustomConfigOptions\":\"\",\"mVerb\":\"1\",\"mCipher\":\"\",\"mDataCiphers\":\"\",\"mNobind\":true,\"mUseDefaultRoutev6\":true,\"mCustomRoutesv6\":\"\",\"mKeyPassword\":\"\",\"mPersistTun\":false,\"mConnectRetryMax\":\"-1\",\"mConnectRetry\":\"2\",\"mConnectRetryMaxTime\":\"300\",\"mUserEditable\":true,\"mAuth\":\"\",\"mX509AuthType\":3,\"mAllowLocalLAN\":false,\"mMssFix\":0,\"mConnections\":[{\"mServerName\":\"openvpn.example.com\",\"mServerPort\":\"1194\",\"mUseUdp\":false,\"mCustomConfiguration\":\"\",\"mUseCustomConfig\":false,\"mEnabled\":true,\"mConnectTimeout\":0,\"mProxyType\":\"NONE\",\"mProxyName\":\"proxy.example.com\",\"mProxyPort\":\"8080\",\"mUseProxyAuth\":false,\"ConnectionAdapter.META_TYPE\":\"de.blinkt.openvpn.core.connection.OpenvpnConnection\"}],\"mRemoteRandom\":false,\"mAllowedAppsVpn\":[],\"mAllowedAppsVpnAreDisallowed\":true,\"mAllowAppVpnBypass\":false,\"mAuthRetry\":0,\"mTunMtu\":0,\"mPushPeerInfo\":false,\"mVersion\":0,\"mLastUsed\":0,\"mServerName\":\"openvpn.example.com\",\"mServerPort\":\"1194\",\"mUseUdp\":true,\"mTemporaryProfile\":false,\"mUuid\":\"9d295ca2-3789-48dd-996e-f731dbf50fdc\",\"mProfileVersion\":7,\"mBlockUnusedAddressFamilies\":true,\"mUsePluggableTransports\":false}";
    private static final String OBFS4CONNECTION_PROFILE = "{\"mAuthenticationType\":2,\"mName\":\"mockProfile\",\"mTLSAuthDirection\":\"\",\"mUseLzo\":false,\"mUseTLSAuth\":false,\"mDNS1\":\"8.8.8.8\",\"mDNS2\":\"8.8.4.4\",\"mOverrideDNS\":false,\"mSearchDomain\":\"blinkt.de\",\"mUseDefaultRoute\":true,\"mUsePull\":true,\"mCheckRemoteCN\":true,\"mExpectTLSCert\":false,\"mRemoteCN\":\"\",\"mPassword\":\"\",\"mUsername\":\"\",\"mRoutenopull\":false,\"mUseRandomHostname\":false,\"mUseFloat\":false,\"mUseCustomConfig\":false,\"mCustomConfigOptions\":\"\",\"mVerb\":\"1\",\"mCipher\":\"\",\"mDataCiphers\":\"\",\"mNobind\":true,\"mUseDefaultRoutev6\":true,\"mCustomRoutesv6\":\"\",\"mKeyPassword\":\"\",\"mPersistTun\":false,\"mConnectRetryMax\":\"-1\",\"mConnectRetry\":\"2\",\"mConnectRetryMaxTime\":\"300\",\"mUserEditable\":true,\"mAuth\":\"\",\"mX509AuthType\":3,\"mAllowLocalLAN\":false,\"mMssFix\":0,\"mConnections\":[{\"options\":{\"cert\":\"CERT\",\"iatMode\":\"1\",\"remoteIP\":\"192.168.0.1\",\"remotePort\":\"1234\"},\"mServerName\":\"127.0.0.1\",\"mServerPort\":\"4430\",\"mUseUdp\":false,\"mCustomConfiguration\":\"\",\"mUseCustomConfig\":false,\"mEnabled\":true,\"mConnectTimeout\":0,\"mProxyType\":\"NONE\",\"mProxyName\":\"\",\"mProxyPort\":\"\",\"mUseProxyAuth\":false,\"ConnectionAdapter.META_TYPE\":\"de.blinkt.openvpn.core.connection.Obfs4Connection\"}],\"mRemoteRandom\":false,\"mAllowedAppsVpn\":[],\"mAllowedAppsVpnAreDisallowed\":true,\"mAllowAppVpnBypass\":false,\"mAuthRetry\":0,\"mTunMtu\":0,\"mPushPeerInfo\":false,\"mVersion\":0,\"mLastUsed\":0,\"mServerName\":\"openvpn.example.com\",\"mServerPort\":\"1194\",\"mUseUdp\":true,\"mTemporaryProfile\":false,\"mUuid\":\"9d295ca2-3789-48dd-996e-f731dbf50fdc\",\"mProfileVersion\":7,\"mBlockUnusedAddressFamilies\":true,\"mUsePluggableTransports\":true}";
    @Before
    public void setup() {
        mockStatic(UUID.class);
    }

    @Test
    public void toJson_openvpn() throws JSONException {
        VpnProfile mockVpnProfile = new VpnProfile("mockProfile", OPENVPN);
        mockVpnProfile.mConnections[0] = new OpenvpnConnection();
        mockVpnProfile.mConnections[0].setUseUdp(false);
        mockVpnProfile.mLastUsed = 0;
        String s = mockVpnProfile.toJson();

        //ignore UUID in comparison -> set it to fixed value
        JSONObject actual = new JSONObject(s);
        actual.put("mUuid", "9d295ca2-3789-48dd-996e-f731dbf50fdc");
        JSONObject expectation = new JSONObject(OPENVPNCONNECTION_PROFILE);

        assertEquals(expectation.toString(), actual.toString());
    }

    @Test
    public void fromJson_openvpn() {
        VpnProfile mockVpnProfile = VpnProfile.fromJson(OPENVPNCONNECTION_PROFILE);
        assertNotNull(mockVpnProfile);
        assertNotNull(mockVpnProfile.mConnections);
        assertNotNull(mockVpnProfile.mConnections[0]);
        assertFalse(mockVpnProfile.mConnections[0].isUseUdp());
        OpenvpnConnection openvpnConnection = (OpenvpnConnection) mockVpnProfile.mConnections[0];
        assertEquals(openvpnConnection.getTransportType(), OPENVPN);
    }

    @Test
    public void toJson_obfs4() throws JSONException {
        VpnProfile mockVpnProfile = new VpnProfile("mockProfile", OBFS4);
        mockVpnProfile.mConnections[0] = new Obfs4Connection(new Obfs4Options("192.168.0.1", "1234", "CERT", "1"));
        mockVpnProfile.mConnections[0].setUseUdp(false);
        mockVpnProfile.mLastUsed = 0;
        String s = mockVpnProfile.toJson();
        System.out.println(s);

        //ignore UUID in comparison -> set it to fixed value
        JSONObject actual = new JSONObject(s);
        actual.put("mUuid", "9d295ca2-3789-48dd-996e-f731dbf50fdc");
        JSONObject expectation = new JSONObject(OBFS4CONNECTION_PROFILE);

        assertEquals(expectation.toString(),actual.toString());
    }

    @Test
    public void fromJson_obfs4() {
        VpnProfile mockVpnProfile = VpnProfile.fromJson(OBFS4CONNECTION_PROFILE);
        assertNotNull(mockVpnProfile);
        assertNotNull(mockVpnProfile.mConnections);
        assertNotNull(mockVpnProfile.mConnections[0]);
        assertFalse(mockVpnProfile.mConnections[0].isUseUdp());
        Obfs4Connection obfs4Connection = (Obfs4Connection) mockVpnProfile.mConnections[0];
        assertEquals(obfs4Connection.getTransportType(), OBFS4);
        assertEquals(obfs4Connection.getDispatcherOptions().cert, "CERT");
        assertEquals(obfs4Connection.getDispatcherOptions().iatMode, "1");
        assertEquals(obfs4Connection.getDispatcherOptions().remoteIP, "192.168.0.1");
        assertEquals(obfs4Connection.getDispatcherOptions().remotePort, "1234");
    }
}