package de.blinkt.openvpn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static de.blinkt.openvpn.core.connection.Connection.TransportType.OBFS4;
import static de.blinkt.openvpn.core.connection.Connection.TransportType.OBFS4_KCP;
import static de.blinkt.openvpn.core.connection.Connection.TransportType.OPENVPN;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.UUID;

import de.blinkt.openvpn.core.connection.Obfs4Connection;
import de.blinkt.openvpn.core.connection.OpenvpnConnection;
import se.leap.bitmaskclient.base.utils.ConfigHelper;
import se.leap.bitmaskclient.pluggableTransports.Obfs4Options;

@RunWith(PowerMockRunner.class)
@PrepareForTest({UUID.class, ConfigHelper.ObfsVpnHelper.class})
public class VpnProfileTest {

    private static final String OPENVPNCONNECTION_PROFILE   = "{\"mCipher\":\"\",\"mProfileVersion\":7,\"mLastUsed\":0,\"mCheckRemoteCN\":true,\"mVerb\":\"1\",\"mRemoteRandom\":false,\"mRoutenopull\":false,\"mConnectRetry\":\"2\",\"mAllowedAppsVpn\":[],\"mUserEditable\":true,\"mUseUdp\":true,\"mAllowedAppsVpnAreDisallowed\":true,\"mDNS1\":\"8.8.8.8\",\"mDNS2\":\"8.8.4.4\",\"mUseCustomConfig\":false,\"mUseFloat\":false,\"mUseDefaultRoute\":true,\"mConnectRetryMaxTime\":\"300\",\"mNobind\":true,\"mVersion\":0,\"mConnectRetryMax\":\"-1\",\"mOverrideDNS\":false,\"mAuth\":\"\",\"mTunMtu\":0,\"mUseObfs4\":false,\"mPassword\":\"\",\"mTLSAuthDirection\":\"\",\"mKeyPassword\":\"\",\"mUseObfs4Kcp\":false,\"mCustomConfigOptions\":\"\",\"mName\":\"mockProfile\",\"mExpectTLSCert\":false,\"mUsername\":\"\",\"mAllowLocalLAN\":false,\"mDataCiphers\":\"\",\"mSearchDomain\":\"blinkt.de\",\"mTemporaryProfile\":false,\"mUseTLSAuth\":false,\"mRemoteCN\":\"\",\"mCustomRoutesv6\":\"\",\"mPersistTun\":false,\"mX509AuthType\":3,\"mUuid\":\"9d295ca2-3789-48dd-996e-f731dbf50fdc\",\"mServerName\":\"openvpn.example.com\",\"mMssFix\":0,\"mPushPeerInfo\":false,\"mAuthenticationType\":2,\"mBlockUnusedAddressFamilies\":true,\"mServerPort\":\"1194\",\"mUseDefaultRoutev6\":true,\"mConnections\":[{\"mCustomConfiguration\":\"\",\"mUseUdp\":false,\"mServerName\":\"openvpn.example.com\",\"mProxyType\":\"NONE\",\"mProxyPort\":\"8080\",\"mUseCustomConfig\":false,\"mConnectTimeout\":0,\"mProxyName\":\"proxy.example.com\",\"mUseProxyAuth\":false,\"ConnectionAdapter.META_TYPE\":\"de.blinkt.openvpn.core.connection.OpenvpnConnection\",\"mServerPort\":\"1194\",\"mEnabled\":true}],\"mUseLzo\":false,\"mAllowAppVpnBypass\":false,\"mUsePull\":true,\"mUseRandomHostname\":false,\"mAuthRetry\":0}";
    private static final String OBFS4CONNECTION_PROFILE = "{\"mCipher\":\"\",\"mProfileVersion\":7,\"mLastUsed\":0,\"mCheckRemoteCN\":true,\"mVerb\":\"1\",\"mRemoteRandom\":false,\"mRoutenopull\":false,\"mConnectRetry\":\"2\",\"mAllowedAppsVpn\":[],\"mUserEditable\":true,\"mUseUdp\":true,\"mAllowedAppsVpnAreDisallowed\":true,\"mDNS1\":\"8.8.8.8\",\"mDNS2\":\"8.8.4.4\",\"mUseCustomConfig\":false,\"mUseFloat\":false,\"mUseDefaultRoute\":true,\"mConnectRetryMaxTime\":\"300\",\"mNobind\":true,\"mVersion\":0,\"mConnectRetryMax\":\"-1\",\"mOverrideDNS\":false,\"mAuth\":\"\",\"mTunMtu\":0,\"mUseObfs4\":true,\"mPassword\":\"\",\"mTLSAuthDirection\":\"\",\"mKeyPassword\":\"\",\"mUseObfs4Kcp\":false,\"mCustomConfigOptions\":\"\",\"mName\":\"mockProfile\",\"mExpectTLSCert\":false,\"mUsername\":\"\",\"mAllowLocalLAN\":false,\"mDataCiphers\":\"\",\"mSearchDomain\":\"blinkt.de\",\"mTemporaryProfile\":false,\"mUseTLSAuth\":false,\"mRemoteCN\":\"\",\"mCustomRoutesv6\":\"\",\"mPersistTun\":false,\"mX509AuthType\":3,\"mUuid\":\"9d295ca2-3789-48dd-996e-f731dbf50fdc\",\"mServerName\":\"openvpn.example.com\",\"mMssFix\":0,\"mPushPeerInfo\":false,\"mAuthenticationType\":2,\"mBlockUnusedAddressFamilies\":true,\"mServerPort\":\"1194\",\"mUseDefaultRoutev6\":true,\"mConnections\":[{\"mCustomConfiguration\":\"\",\"mServerName\":\"127.0.0.1\",\"mProxyType\":\"NONE\",\"mConnectTimeout\":0,\"mServerPort\":\"4430\",\"mUseUdp\":false,\"mProxyPort\":\"\",\"mUseCustomConfig\":false,\"options\":{\"udp\":false,\"remoteIP\":\"192.168.0.1\",\"iatMode\":\"1\",\"remotePort\":\"1234\",\"cert\":\"CERT\"},\"mProxyName\":\"\",\"mUseProxyAuth\":false,\"ConnectionAdapter.META_TYPE\":\"de.blinkt.openvpn.core.connection.Obfs4Connection\",\"mEnabled\":true}],\"mUseLzo\":false,\"mAllowAppVpnBypass\":false,\"mUsePull\":true,\"mUseRandomHostname\":false,\"mAuthRetry\":0}";
    private static final String OBFS4CONNECTION_PROFILE_OBFSVPN = "{\"mCipher\":\"\",\"mProfileVersion\":7,\"mLastUsed\":0,\"mCheckRemoteCN\":true,\"mVerb\":\"1\",\"mRemoteRandom\":false,\"mRoutenopull\":false,\"mConnectRetry\":\"2\",\"mAllowedAppsVpn\":[],\"mUserEditable\":true,\"mUseUdp\":true,\"mAllowedAppsVpnAreDisallowed\":true,\"mDNS1\":\"8.8.8.8\",\"mDNS2\":\"8.8.4.4\",\"mUseCustomConfig\":false,\"mUseFloat\":false,\"mUseDefaultRoute\":true,\"mConnectRetryMaxTime\":\"300\",\"mNobind\":true,\"mVersion\":0,\"mConnectRetryMax\":\"-1\",\"mOverrideDNS\":false,\"mAuth\":\"\",\"mTunMtu\":0,\"mUseObfs4\":true,\"mPassword\":\"\",\"mTLSAuthDirection\":\"\",\"mKeyPassword\":\"\",\"mUseObfs4Kcp\":false,\"mCustomConfigOptions\":\"\",\"mName\":\"mockProfile\",\"mExpectTLSCert\":false,\"mUsername\":\"\",\"mAllowLocalLAN\":false,\"mDataCiphers\":\"\",\"mSearchDomain\":\"blinkt.de\",\"mTemporaryProfile\":false,\"mUseTLSAuth\":false,\"mRemoteCN\":\"\",\"mCustomRoutesv6\":\"\",\"mPersistTun\":false,\"mX509AuthType\":3,\"mUuid\":\"9d295ca2-3789-48dd-996e-f731dbf50fdc\",\"mServerName\":\"openvpn.example.com\",\"mMssFix\":0,\"mPushPeerInfo\":false,\"mAuthenticationType\":2,\"mBlockUnusedAddressFamilies\":true,\"mServerPort\":\"1194\",\"mUseDefaultRoutev6\":true,\"mConnections\":[{\"mCustomConfiguration\":\"\",\"mServerName\":\"192.168.0.1\",\"mProxyType\":\"SOCKS5\",\"mConnectTimeout\":0,\"mServerPort\":\"1234\",\"mUseUdp\":false,\"mProxyPort\":\"4430\",\"mUseCustomConfig\":false,\"options\":{\"udp\":false,\"remoteIP\":\"192.168.0.1\",\"iatMode\":\"1\",\"remotePort\":\"1234\",\"cert\":\"CERT\"},\"mProxyName\":\"127.0.0.1\",\"mUseProxyAuth\":false,\"ConnectionAdapter.META_TYPE\":\"de.blinkt.openvpn.core.connection.Obfs4Connection\",\"mEnabled\":true}],\"mUseLzo\":false,\"mAllowAppVpnBypass\":false,\"mUsePull\":true,\"mUseRandomHostname\":false,\"mAuthRetry\":0}";
    private static final String OBFS4CONNECTION_PROFILE_OBFSVPN_KCP = "{\"mCipher\":\"\",\"mProfileVersion\":7,\"mLastUsed\":0,\"mCheckRemoteCN\":true,\"mVerb\":\"1\",\"mRemoteRandom\":false,\"mRoutenopull\":false,\"mConnectRetry\":\"2\",\"mAllowedAppsVpn\":[],\"mUserEditable\":true,\"mUseUdp\":true,\"mAllowedAppsVpnAreDisallowed\":true,\"mDNS1\":\"8.8.8.8\",\"mDNS2\":\"8.8.4.4\",\"mUseCustomConfig\":false,\"mUseFloat\":false,\"mUseDefaultRoute\":true,\"mConnectRetryMaxTime\":\"300\",\"mNobind\":true,\"mVersion\":0,\"mConnectRetryMax\":\"-1\",\"mOverrideDNS\":false,\"mAuth\":\"\",\"mTunMtu\":0,\"mUseObfs4\":false,\"mPassword\":\"\",\"mTLSAuthDirection\":\"\",\"mKeyPassword\":\"\",\"mUseObfs4Kcp\":true,\"mCustomConfigOptions\":\"\",\"mName\":\"mockProfile\",\"mExpectTLSCert\":false,\"mUsername\":\"\",\"mAllowLocalLAN\":false,\"mDataCiphers\":\"\",\"mSearchDomain\":\"blinkt.de\",\"mTemporaryProfile\":false,\"mUseTLSAuth\":false,\"mRemoteCN\":\"\",\"mCustomRoutesv6\":\"\",\"mPersistTun\":false,\"mX509AuthType\":3,\"mUuid\":\"9d295ca2-3789-48dd-996e-f731dbf50fdc\",\"mServerName\":\"openvpn.example.com\",\"mMssFix\":0,\"mPushPeerInfo\":false,\"mAuthenticationType\":2,\"mBlockUnusedAddressFamilies\":true,\"mServerPort\":\"1194\",\"mUseDefaultRoutev6\":true,\"mConnections\":[{\"mCustomConfiguration\":\"\",\"mServerName\":\"192.168.0.1\",\"mProxyType\":\"SOCKS5\",\"mConnectTimeout\":0,\"mServerPort\":\"1234\",\"mUseUdp\":false,\"mProxyPort\":\"4430\",\"mUseCustomConfig\":false,\"options\":{\"udp\":true,\"remoteIP\":\"192.168.0.1\",\"iatMode\":\"1\",\"remotePort\":\"1234\",\"cert\":\"CERT\"},\"mProxyName\":\"127.0.0.1\",\"mUseProxyAuth\":false,\"ConnectionAdapter.META_TYPE\":\"de.blinkt.openvpn.core.connection.Obfs4Connection\",\"mEnabled\":true}],\"mUseLzo\":false,\"mAllowAppVpnBypass\":false,\"mUsePull\":true,\"mUseRandomHostname\":false,\"mAuthRetry\":0}\n";

    @Before
    public void setup() {
        mockStatic(UUID.class);
        mockStatic(ConfigHelper.ObfsVpnHelper.class);
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
        when(ConfigHelper.ObfsVpnHelper.useObfsVpn()).thenReturn(false);

        VpnProfile mockVpnProfile = new VpnProfile("mockProfile", OBFS4);
        mockVpnProfile.mConnections[0] = new Obfs4Connection(new Obfs4Options("192.168.0.1", "1234", "CERT", "1", false));
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
    public void toJson_obfs4_obfsvpn() throws JSONException {
        when(ConfigHelper.ObfsVpnHelper.useObfsVpn()).thenReturn(true);
        VpnProfile mockVpnProfile = new VpnProfile("mockProfile", OBFS4);
        mockVpnProfile.mConnections[0] = new Obfs4Connection(new Obfs4Options("192.168.0.1", "1234", "CERT", "1", false));
        mockVpnProfile.mLastUsed = 0;
        String s = mockVpnProfile.toJson();
        System.out.println(s);

        //ignore UUID in comparison -> set it to fixed value
        JSONObject actual = new JSONObject(s);
        actual.put("mUuid", "9d295ca2-3789-48dd-996e-f731dbf50fdc");
        JSONObject expectation = new JSONObject(OBFS4CONNECTION_PROFILE_OBFSVPN);

        assertEquals(expectation.toString(),actual.toString());
    }

    @Test
    public void toJson_obfs4_obfsvpn_kcp() throws JSONException {
        when(ConfigHelper.ObfsVpnHelper.useObfsVpn()).thenReturn(true);

        VpnProfile mockVpnProfile = new VpnProfile("mockProfile", OBFS4_KCP);
        mockVpnProfile.mConnections[0] = new Obfs4Connection(new Obfs4Options("192.168.0.1", "1234", "CERT", "1", true));
        mockVpnProfile.mLastUsed = 0;
        String s = mockVpnProfile.toJson();
        System.out.println(s);

        //ignore UUID in comparison -> set it to fixed value
        JSONObject actual = new JSONObject(s);
        actual.put("mUuid", "9d295ca2-3789-48dd-996e-f731dbf50fdc");
        JSONObject expectation = new JSONObject(OBFS4CONNECTION_PROFILE_OBFSVPN_KCP);

        assertEquals(expectation.toString(),actual.toString());
    }

    @Test
    public void fromJson_obfs4() {
        when(ConfigHelper.ObfsVpnHelper.useObfsVpn()).thenReturn(false);

        VpnProfile mockVpnProfile = VpnProfile.fromJson(OBFS4CONNECTION_PROFILE);
        assertNotNull(mockVpnProfile);
        assertNotNull(mockVpnProfile.mConnections);
        assertNotNull(mockVpnProfile.mConnections[0]);
        assertFalse(mockVpnProfile.mConnections[0].isUseUdp());
        Obfs4Connection obfs4Connection = (Obfs4Connection) mockVpnProfile.mConnections[0];
        assertEquals(OBFS4, obfs4Connection.getTransportType());
        assertFalse(obfs4Connection.getDispatcherOptions().udp);
        assertEquals("CERT", obfs4Connection.getDispatcherOptions().cert);
        assertEquals("1", obfs4Connection.getDispatcherOptions().iatMode);
        assertEquals("192.168.0.1", obfs4Connection.getDispatcherOptions().remoteIP);
        assertEquals("1234", obfs4Connection.getDispatcherOptions().remotePort);
    }

    @Test
    public void fromJson_obfs4_obfsvpn() {
        when(ConfigHelper.ObfsVpnHelper.useObfsVpn()).thenReturn(true);

        VpnProfile mockVpnProfile = VpnProfile.fromJson(OBFS4CONNECTION_PROFILE_OBFSVPN);
        assertNotNull(mockVpnProfile);
        assertNotNull(mockVpnProfile.mConnections);
        assertNotNull(mockVpnProfile.mConnections[0]);
        assertFalse(mockVpnProfile.mConnections[0].isUseUdp());
        Obfs4Connection obfs4Connection = (Obfs4Connection) mockVpnProfile.mConnections[0];
        assertEquals(OBFS4, obfs4Connection.getTransportType());
        assertFalse(obfs4Connection.getDispatcherOptions().udp);
        assertEquals("CERT", obfs4Connection.getDispatcherOptions().cert);
        assertEquals("1", obfs4Connection.getDispatcherOptions().iatMode);
        assertEquals("192.168.0.1", obfs4Connection.getDispatcherOptions().remoteIP);
        assertEquals("1234", obfs4Connection.getDispatcherOptions().remotePort);
    }

    @Test
    public void fromJson_obfs4_obfsvpn_kcp() {
        when(ConfigHelper.ObfsVpnHelper.useObfsVpn()).thenReturn(true);

        VpnProfile mockVpnProfile = VpnProfile.fromJson(OBFS4CONNECTION_PROFILE_OBFSVPN_KCP);
        assertNotNull(mockVpnProfile);
        assertNotNull(mockVpnProfile.mConnections);
        assertNotNull(mockVpnProfile.mConnections[0]);
        assertFalse(mockVpnProfile.mConnections[0].isUseUdp());
        Obfs4Connection obfs4Connection = (Obfs4Connection) mockVpnProfile.mConnections[0];
        assertEquals(OBFS4, obfs4Connection.getTransportType());
        assertTrue(obfs4Connection.getDispatcherOptions().udp);
        assertEquals("CERT", obfs4Connection.getDispatcherOptions().cert);
        assertEquals("1", obfs4Connection.getDispatcherOptions().iatMode);
        assertEquals("192.168.0.1", obfs4Connection.getDispatcherOptions().remoteIP);
        assertEquals("1234", obfs4Connection.getDispatcherOptions().remotePort);
    }
}