package de.blinkt.openvpn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static de.blinkt.openvpn.core.connection.Connection.TransportType.OBFS4;
import static de.blinkt.openvpn.core.connection.Connection.TransportType.OBFS4_HOP;
import static de.blinkt.openvpn.core.connection.Connection.TransportType.OPENVPN;
import static se.leap.bitmaskclient.base.models.Constants.UDP;

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
import se.leap.bitmaskclient.base.models.Transport;
import se.leap.bitmaskclient.base.utils.BuildConfigHelper;
import se.leap.bitmaskclient.pluggableTransports.models.Obfs4Options;

@RunWith(PowerMockRunner.class)
@PrepareForTest({UUID.class, BuildConfigHelper.class})
public class VpnProfileTest {

    private static final String OPENVPNCONNECTION_PROFILE   = "{\"mCipher\":\"\",\"mProfileVersion\":7,\"mLastUsed\":0,\"mCheckRemoteCN\":true,\"mVerb\":\"1\",\"mRemoteRandom\":false,\"mRoutenopull\":false,\"mConnectRetry\":\"2\",\"mAllowedAppsVpn\":[],\"mUserEditable\":true,\"mUseUdp\":true,\"mAllowedAppsVpnAreDisallowed\":true,\"mDNS1\":\"8.8.8.8\",\"mDNS2\":\"8.8.4.4\",\"mUseCustomConfig\":false,\"mUseFloat\":false,\"mUseDefaultRoute\":true,\"mConnectRetryMaxTime\":\"300\",\"mNobind\":true,\"mVersion\":0,\"mConnectRetryMax\":\"-1\",\"mOverrideDNS\":false,\"mAuth\":\"\",\"mTunMtu\":0,\"mPassword\":\"\",\"mTLSAuthDirection\":\"\",\"mKeyPassword\":\"\",\"mCustomConfigOptions\":\"\",\"mName\":\"mockProfile\",\"mExpectTLSCert\":false,\"mUsername\":\"\",\"mAllowLocalLAN\":false,\"mDataCiphers\":\"\",\"mSearchDomain\":\"blinkt.de\",\"mTemporaryProfile\":false,\"mUseTLSAuth\":false,\"mRemoteCN\":\"\",\"mCustomRoutesv6\":\"\",\"mPersistTun\":false,\"mX509AuthType\":3,\"mUuid\":\"9d295ca2-3789-48dd-996e-f731dbf50fdc\",\"mServerName\":\"openvpn.example.com\",\"mMssFix\":0,\"mPushPeerInfo\":false,\"mAuthenticationType\":2,\"mBlockUnusedAddressFamilies\":true,\"mServerPort\":\"1194\",\"mUseDefaultRoutev6\":true,\"mConnections\":[{\"mCustomConfiguration\":\"\",\"mUseUdp\":false,\"mServerName\":\"openvpn.example.com\",\"mProxyType\":\"NONE\",\"mProxyPort\":\"8080\",\"mUseCustomConfig\":false,\"mConnectTimeout\":0,\"mProxyName\":\"proxy.example.com\",\"mUseProxyAuth\":false,\"ConnectionAdapter.META_TYPE\":\"de.blinkt.openvpn.core.connection.OpenvpnConnection\",\"mServerPort\":\"1194\",\"mEnabled\":true}],\"mUseLzo\":false,\"mTransportType\":1,\"mAllowAppVpnBypass\":false,\"mUsePull\":true,\"mUseRandomHostname\":false,\"mAuthRetry\":0}";
    private static final String OBFS4CONNECTION_PROFILE = "{\"mCipher\":\"\",\"mProfileVersion\":7,\"mLastUsed\":0,\"mCheckRemoteCN\":true,\"mVerb\":\"1\",\"mRemoteRandom\":false,\"mRoutenopull\":false,\"mConnectRetry\":\"2\",\"mAllowedAppsVpn\":[],\"mUserEditable\":true,\"mUseUdp\":true,\"mAllowedAppsVpnAreDisallowed\":true,\"mDNS1\":\"8.8.8.8\",\"mDNS2\":\"8.8.4.4\",\"mUseCustomConfig\":false,\"mUseFloat\":false,\"mUseDefaultRoute\":true,\"mConnectRetryMaxTime\":\"300\",\"mNobind\":true,\"mVersion\":0,\"mConnectRetryMax\":\"-1\",\"mOverrideDNS\":false,\"mAuth\":\"\",\"mTunMtu\":0,\"mPassword\":\"\",\"mTLSAuthDirection\":\"\",\"mKeyPassword\":\"\",\"mCustomConfigOptions\":\"\",\"mName\":\"mockProfile\",\"mExpectTLSCert\":false,\"mUsername\":\"\",\"mAllowLocalLAN\":false,\"mDataCiphers\":\"\",\"mSearchDomain\":\"blinkt.de\",\"mTemporaryProfile\":false,\"mUseTLSAuth\":false,\"mRemoteCN\":\"\",\"mCustomRoutesv6\":\"\",\"mPersistTun\":false,\"mX509AuthType\":3,\"mUuid\":\"9d295ca2-3789-48dd-996e-f731dbf50fdc\",\"mServerName\":\"openvpn.example.com\",\"mMssFix\":0,\"mPushPeerInfo\":false,\"mAuthenticationType\":2,\"mBlockUnusedAddressFamilies\":true,\"mServerPort\":\"1194\",\"mUseDefaultRoutev6\":true,\"mConnections\":[{\"mCustomConfiguration\":\"\",\"mServerName\":\"127.0.0.1\",\"mProxyType\":\"NONE\",\"mConnectTimeout\":0,\"mServerPort\":\"8080\",\"mUseUdp\":true,\"mProxyPort\":\"\",\"mUseCustomConfig\":false,\"options\":{\"bridgeIP\":\"192.168.0.1\",\"transport\":{\"options\":{\"portCount\":0,\"iatMode\":\"0\",\"cert\":\"CERT\",\"experimental\":false,\"portSeed\":0},\"type\":\"obfs4\",\"protocols\":[\"tcp\"],\"ports\":[\"1234\"]}},\"mProxyName\":\"\",\"mUseProxyAuth\":false,\"ConnectionAdapter.META_TYPE\":\"de.blinkt.openvpn.core.connection.Obfs4Connection\",\"mEnabled\":true}],\"mUseLzo\":false,\"mTransportType\":2,\"mAllowAppVpnBypass\":false,\"mUsePull\":true,\"mUseRandomHostname\":false,\"mAuthRetry\":0}";
    private static final String OBFS4CONNECTION_PROFILE_OBFSVPN = "{\"mCipher\":\"\",\"mProfileVersion\":7,\"mLastUsed\":0,\"mCheckRemoteCN\":true,\"mVerb\":\"1\",\"mRemoteRandom\":false,\"mRoutenopull\":false,\"mConnectRetry\":\"2\",\"mAllowedAppsVpn\":[],\"mUserEditable\":true,\"mUseUdp\":true,\"mAllowedAppsVpnAreDisallowed\":true,\"mDNS1\":\"8.8.8.8\",\"mDNS2\":\"8.8.4.4\",\"mUseCustomConfig\":false,\"mUseFloat\":false,\"mUseDefaultRoute\":true,\"mConnectRetryMaxTime\":\"300\",\"mNobind\":true,\"mVersion\":0,\"mConnectRetryMax\":\"-1\",\"mOverrideDNS\":false,\"mAuth\":\"\",\"mTunMtu\":0,\"mPassword\":\"\",\"mTLSAuthDirection\":\"\",\"mKeyPassword\":\"\",\"mCustomConfigOptions\":\"\",\"mName\":\"mockProfile\",\"mExpectTLSCert\":false,\"mUsername\":\"\",\"mAllowLocalLAN\":false,\"mDataCiphers\":\"\",\"mSearchDomain\":\"blinkt.de\",\"mTemporaryProfile\":false,\"mUseTLSAuth\":false,\"mRemoteCN\":\"\",\"mCustomRoutesv6\":\"\",\"mPersistTun\":false,\"mX509AuthType\":3,\"mUuid\":\"9d295ca2-3789-48dd-996e-f731dbf50fdc\",\"mServerName\":\"openvpn.example.com\",\"mMssFix\":0,\"mPushPeerInfo\":false,\"mAuthenticationType\":2,\"mBlockUnusedAddressFamilies\":true,\"mServerPort\":\"1194\",\"mUseDefaultRoutev6\":true,\"mConnections\":[{\"mCustomConfiguration\":\"\",\"mServerName\":\"127.0.0.1\",\"mProxyType\":\"NONE\",\"mConnectTimeout\":0,\"mServerPort\":\"8080\",\"mUseUdp\":true,\"mProxyPort\":\"\",\"mUseCustomConfig\":false,\"options\":{\"bridgeIP\":\"192.168.0.1\",\"transport\":{\"options\":{\"portCount\":0,\"iatMode\":\"1\",\"cert\":\"CERT\",\"experimental\":false,\"portSeed\":0},\"type\":\"obfs4\",\"protocols\":[\"tcp\"],\"ports\":[\"1234\"]}},\"mProxyName\":\"\",\"mUseProxyAuth\":false,\"ConnectionAdapter.META_TYPE\":\"de.blinkt.openvpn.core.connection.Obfs4Connection\",\"mEnabled\":true}],\"mUseLzo\":false,\"mTransportType\":2,\"mAllowAppVpnBypass\":false,\"mUsePull\":true,\"mUseRandomHostname\":false,\"mAuthRetry\":0}";
    private static final String OBFS4CONNECTION_PROFILE_OBFSVPN_KCP = "{\"mCipher\":\"\",\"mProfileVersion\":7,\"mLastUsed\":0,\"mCheckRemoteCN\":true,\"mVerb\":\"1\",\"mRemoteRandom\":false,\"mRoutenopull\":false,\"mConnectRetry\":\"2\",\"mAllowedAppsVpn\":[],\"mUserEditable\":true,\"mUseUdp\":true,\"mAllowedAppsVpnAreDisallowed\":true,\"mDNS1\":\"8.8.8.8\",\"mDNS2\":\"8.8.4.4\",\"mUseCustomConfig\":false,\"mUseFloat\":false,\"mUseDefaultRoute\":true,\"mConnectRetryMaxTime\":\"300\",\"mNobind\":true,\"mVersion\":0,\"mConnectRetryMax\":\"-1\",\"mOverrideDNS\":false,\"mAuth\":\"\",\"mTunMtu\":0,\"mPassword\":\"\",\"mTLSAuthDirection\":\"\",\"mKeyPassword\":\"\",\"mCustomConfigOptions\":\"\",\"mName\":\"mockProfile\",\"mExpectTLSCert\":false,\"mUsername\":\"\",\"mAllowLocalLAN\":false,\"mDataCiphers\":\"\",\"mSearchDomain\":\"blinkt.de\",\"mTemporaryProfile\":false,\"mUseTLSAuth\":false,\"mRemoteCN\":\"\",\"mCustomRoutesv6\":\"\",\"mPersistTun\":false,\"mX509AuthType\":3,\"mUuid\":\"9d295ca2-3789-48dd-996e-f731dbf50fdc\",\"mServerName\":\"openvpn.example.com\",\"mMssFix\":0,\"mPushPeerInfo\":false,\"mAuthenticationType\":2,\"mBlockUnusedAddressFamilies\":true,\"mServerPort\":\"1194\",\"mUseDefaultRoutev6\":true,\"mConnections\":[{\"mCustomConfiguration\":\"\",\"mServerName\":\"127.0.0.1\",\"mProxyType\":\"NONE\",\"mConnectTimeout\":0,\"mServerPort\":\"8080\",\"mUseUdp\":true,\"mProxyPort\":\"\",\"mUseCustomConfig\":false,\"options\":{\"bridgeIP\":\"192.168.0.1\",\"transport\":{\"options\":{\"portCount\":0,\"iatMode\":\"1\",\"cert\":\"CERT\",\"experimental\":false,\"portSeed\":0},\"type\":\"obfs4\",\"protocols\":[\"kcp\"],\"ports\":[\"1234\"]}},\"mProxyName\":\"\",\"mUseProxyAuth\":false,\"ConnectionAdapter.META_TYPE\":\"de.blinkt.openvpn.core.connection.Obfs4Connection\",\"mEnabled\":true}],\"mUseLzo\":false,\"mTransportType\":2,\"mAllowAppVpnBypass\":false,\"mUsePull\":true,\"mUseRandomHostname\":false,\"mAuthRetry\":0}\n";

    private static final String OBFS4CONNECTION_PROFILE_OBFSVPN_HOP = "{\"mCipher\":\"\",\"mProfileVersion\":7,\"mLastUsed\":0,\"mCheckRemoteCN\":true,\"mVerb\":\"1\",\"mRemoteRandom\":false,\"mRoutenopull\":false,\"mConnectRetry\":\"2\",\"mAllowedAppsVpn\":[],\"mUserEditable\":true,\"mUseUdp\":true,\"mAllowedAppsVpnAreDisallowed\":true,\"mDNS1\":\"8.8.8.8\",\"mDNS2\":\"8.8.4.4\",\"mUseCustomConfig\":false,\"mUseFloat\":false,\"mUseDefaultRoute\":true,\"mConnectRetryMaxTime\":\"300\",\"mNobind\":true,\"mVersion\":0,\"mConnectRetryMax\":\"-1\",\"mOverrideDNS\":false,\"mAuth\":\"\",\"mTunMtu\":0,\"mPassword\":\"\",\"mTLSAuthDirection\":\"\",\"mKeyPassword\":\"\",\"mCustomConfigOptions\":\"\",\"mName\":\"mockProfile\",\"mExpectTLSCert\":false,\"mUsername\":\"\",\"mAllowLocalLAN\":false,\"mDataCiphers\":\"\",\"mSearchDomain\":\"blinkt.de\",\"mTemporaryProfile\":false,\"mUseTLSAuth\":false,\"mRemoteCN\":\"\",\"mCustomRoutesv6\":\"\",\"mPersistTun\":false,\"mX509AuthType\":3,\"mUuid\":\"9d295ca2-3789-48dd-996e-f731dbf50fdc\",\"mServerName\":\"openvpn.example.com\",\"mMssFix\":0,\"mPushPeerInfo\":false,\"mAuthenticationType\":2,\"mBlockUnusedAddressFamilies\":true,\"mServerPort\":\"1194\",\"mUseDefaultRoutev6\":true,\"mConnections\":[{\"mCustomConfiguration\":\"\",\"mServerName\":\"127.0.0.1\",\"mProxyType\":\"NONE\",\"mConnectTimeout\":0,\"mServerPort\":\"8080\",\"mUseUdp\":true,\"mProxyPort\":\"\",\"mUseCustomConfig\":false,\"options\":{\"bridgeIP\":\"192.168.0.1\",\"transport\":{\"options\":{\"portCount\":100,\"endpoints\":[{\"ip\":\"1.1.1.1\",\"cert\":\"CERT1\"},{\"ip\":\"2.2.2.2\",\"cert\":\"CERT2\"}],\"iatMode\":\"1\",\"experimental\":true,\"portSeed\":200},\"type\":\"obfs4\",\"protocols\":[\"tcp\"],\"ports\":[\"1234\"]}},\"mProxyName\":\"\",\"mUseProxyAuth\":false,\"ConnectionAdapter.META_TYPE\":\"de.blinkt.openvpn.core.connection.Obfs4Connection\",\"mEnabled\":true}],\"mUseLzo\":false,\"mTransportType\":3,\"mAllowAppVpnBypass\":false,\"mUsePull\":true,\"mUseRandomHostname\":false,\"mAuthRetry\":0}\n";

    private static final String OBFS4CONNECTION_PROFILE_OBFSVPN_HOP_KCP = "{\"mCipher\":\"\",\"mProfileVersion\":7,\"mLastUsed\":0,\"mCheckRemoteCN\":true,\"mVerb\":\"1\",\"mRemoteRandom\":false,\"mRoutenopull\":false,\"mConnectRetry\":\"2\",\"mAllowedAppsVpn\":[],\"mUserEditable\":true,\"mUseUdp\":true,\"mAllowedAppsVpnAreDisallowed\":true,\"mDNS1\":\"8.8.8.8\",\"mDNS2\":\"8.8.4.4\",\"mUseCustomConfig\":false,\"mUseFloat\":false,\"mUseDefaultRoute\":true,\"mConnectRetryMaxTime\":\"300\",\"mNobind\":true,\"mVersion\":0,\"mConnectRetryMax\":\"-1\",\"mOverrideDNS\":false,\"mAuth\":\"\",\"mTunMtu\":0,\"mPassword\":\"\",\"mTLSAuthDirection\":\"\",\"mKeyPassword\":\"\",\"mCustomConfigOptions\":\"\",\"mName\":\"mockProfile\",\"mExpectTLSCert\":false,\"mUsername\":\"\",\"mAllowLocalLAN\":false,\"mDataCiphers\":\"\",\"mSearchDomain\":\"blinkt.de\",\"mTemporaryProfile\":false,\"mUseTLSAuth\":false,\"mRemoteCN\":\"\",\"mCustomRoutesv6\":\"\",\"mPersistTun\":false,\"mX509AuthType\":3,\"mUuid\":\"9d295ca2-3789-48dd-996e-f731dbf50fdc\",\"mServerName\":\"openvpn.example.com\",\"mMssFix\":0,\"mPushPeerInfo\":false,\"mAuthenticationType\":2,\"mBlockUnusedAddressFamilies\":true,\"mServerPort\":\"1194\",\"mUseDefaultRoutev6\":true,\"mConnections\":[{\"mCustomConfiguration\":\"\",\"mServerName\":\"127.0.0.1\",\"mProxyType\":\"NONE\",\"mConnectTimeout\":0,\"mServerPort\":\"8080\",\"mUseUdp\":true,\"mProxyPort\":\"\",\"mUseCustomConfig\":false,\"options\":{\"bridgeIP\":\"192.168.0.1\",\"transport\":{\"options\":{\"portCount\":100,\"endpoints\":[{\"ip\":\"1.1.1.1\",\"cert\":\"CERT1\"},{\"ip\":\"2.2.2.2\",\"cert\":\"CERT2\"}],\"iatMode\":\"1\",\"experimental\":true,\"portSeed\":2500},\"type\":\"obfs4\",\"protocols\":[\"kcp\"],\"ports\":[\"1234\"]}},\"mProxyName\":\"\",\"mUseProxyAuth\":false,\"ConnectionAdapter.META_TYPE\":\"de.blinkt.openvpn.core.connection.Obfs4Connection\",\"mEnabled\":true}],\"mUseLzo\":false,\"mTransportType\":3,\"mAllowAppVpnBypass\":false,\"mUsePull\":true,\"mUseRandomHostname\":false,\"mAuthRetry\":0}\n";

    private static final String OBFS4CONNECTION_PROFILE_OBFSVPN_PORTHOPPING = "{\"mCipher\":\"\",\"mProfileVersion\":7,\"mLastUsed\":0,\"mCheckRemoteCN\":true,\"mVerb\":\"1\",\"mRemoteRandom\":false,\"mRoutenopull\":false,\"mConnectRetry\":\"2\",\"mAllowedAppsVpn\":[],\"mUserEditable\":true,\"mUseUdp\":true,\"mAllowedAppsVpnAreDisallowed\":true,\"mDNS1\":\"8.8.8.8\",\"mDNS2\":\"8.8.4.4\",\"mUseCustomConfig\":false,\"mUseFloat\":false,\"mUseDefaultRoute\":true,\"mConnectRetryMaxTime\":\"300\",\"mNobind\":true,\"mVersion\":0,\"mConnectRetryMax\":\"-1\",\"mOverrideDNS\":false,\"mAuth\":\"\",\"mTunMtu\":0,\"mPassword\":\"\",\"mTLSAuthDirection\":\"\",\"mKeyPassword\":\"\",\"mCustomConfigOptions\":\"\",\"mName\":\"mockProfile\",\"mExpectTLSCert\":false,\"mUsername\":\"\",\"mAllowLocalLAN\":false,\"mDataCiphers\":\"\",\"mSearchDomain\":\"blinkt.de\",\"mTemporaryProfile\":false,\"mUseTLSAuth\":false,\"mRemoteCN\":\"\",\"mCustomRoutesv6\":\"\",\"mPersistTun\":false,\"mX509AuthType\":3,\"mUuid\":\"9d295ca2-3789-48dd-996e-f731dbf50fdc\",\"mServerName\":\"openvpn.example.com\",\"mMssFix\":0,\"mPushPeerInfo\":false,\"mAuthenticationType\":2,\"mBlockUnusedAddressFamilies\":true,\"mServerPort\":\"1194\",\"mUseDefaultRoutev6\":true,\"mConnections\":[{\"mCustomConfiguration\":\"\",\"mServerName\":\"127.0.0.1\",\"mProxyType\":\"NONE\",\"mConnectTimeout\":0,\"mServerPort\":\"8080\",\"mUseUdp\":true,\"mProxyPort\":\"\",\"mUseCustomConfig\":false,\"options\":{\"bridgeIP\":\"192.168.0.1\",\"transport\":{\"options\":{\"portCount\":100,\"iatMode\":\"1\",\"cert\":\"CERT\",\"experimental\":true,\"portSeed\":200},\"type\":\"obfs4-hop\",\"protocols\":[\"tcp\"],\"ports\":[\"1234\"]}},\"mProxyName\":\"\",\"mUseProxyAuth\":false,\"ConnectionAdapter.META_TYPE\":\"de.blinkt.openvpn.core.connection.Obfs4Connection\",\"mEnabled\":true}],\"mUseLzo\":false,\"mTransportType\":3,\"mAllowAppVpnBypass\":false,\"mUsePull\":true,\"mUseRandomHostname\":false,\"mAuthRetry\":0}\n";

    @Before
    public void setup() {
        mockStatic(UUID.class);
        mockStatic(BuildConfigHelper.class);
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

        Transport transport = new Transport(OBFS4.toString(), new String[]{"tcp"}, new String[]{"1234"}, "CERT");
        mockVpnProfile.mConnections[0] = new Obfs4Connection(new Obfs4Options("192.168.0.1", transport));
        mockVpnProfile.mLastUsed = 0;
        String s = mockVpnProfile.toJson();

        //ignore UUID in comparison -> set it to fixed value
        JSONObject actual = new JSONObject(s);
        actual.put("mUuid", "9d295ca2-3789-48dd-996e-f731dbf50fdc");
        JSONObject expectation = new JSONObject(OBFS4CONNECTION_PROFILE);

        assertEquals(expectation.toString(),actual.toString());
    }

    @Test
    public void toJson_obfs4_obfsvpn() throws JSONException {
        VpnProfile mockVpnProfile = new VpnProfile("mockProfile", OBFS4);
        Transport.Options options = new Transport.Options("CERT", "1");
        Transport transport = new Transport(OBFS4.toString(), new String[]{"tcp"}, new String[]{"1234"}, options);
        mockVpnProfile.mConnections[0] = new Obfs4Connection(new Obfs4Options("192.168.0.1", transport));
        mockVpnProfile.mLastUsed = 0;
        String s = mockVpnProfile.toJson();

        //ignore UUID in comparison -> set it to fixed value
        JSONObject actual = new JSONObject(s);
        actual.put("mUuid", "9d295ca2-3789-48dd-996e-f731dbf50fdc");
        JSONObject expectation = new JSONObject(OBFS4CONNECTION_PROFILE_OBFSVPN);

        assertEquals(expectation.toString(),actual.toString());
    }

    @Test
    public void toJson_obfs4_obfsvpn_kcp() throws JSONException {

        VpnProfile mockVpnProfile = new VpnProfile("mockProfile", OBFS4);
        Transport.Options options = new Transport.Options("CERT", "1");
        Transport transport = new Transport(OBFS4.toString(), new String[]{"kcp"}, new String[]{"1234"}, options);
        mockVpnProfile.mConnections[0] = new Obfs4Connection(new Obfs4Options("192.168.0.1", transport));
        mockVpnProfile.mLastUsed = 0;
        String s = mockVpnProfile.toJson();

        //ignore UUID in comparison -> set it to fixed value
        JSONObject actual = new JSONObject(s);
        actual.put("mUuid", "9d295ca2-3789-48dd-996e-f731dbf50fdc");
        JSONObject expectation = new JSONObject(OBFS4CONNECTION_PROFILE_OBFSVPN_KCP);

        assertEquals(expectation.toString(),actual.toString());
    }

    @Test
    public void toJson_obfs4hop_kcp() throws JSONException {

        VpnProfile mockVpnProfile = new VpnProfile("mockProfile", OBFS4_HOP);

        Transport.Options options = new Transport.Options("1", new Transport.Endpoint[]{new Transport.Endpoint("1.1.1.1", "CERT1"), new Transport.Endpoint("2.2.2.2", "CERT2")}, 2500, 100, true);
        Transport transport = new Transport(OBFS4.toString(), new String[]{"kcp"}, new String[]{"1234"}, options);
        mockVpnProfile.mConnections[0] = new Obfs4Connection(new Obfs4Options("192.168.0.1", transport));

        mockVpnProfile.mLastUsed = 0;
        String s = mockVpnProfile.toJson();

        //ignore UUID in comparison -> set it to fixed value
        JSONObject actual = new JSONObject(s);
        actual.put("mUuid", "9d295ca2-3789-48dd-996e-f731dbf50fdc");
        JSONObject expectation = new JSONObject(OBFS4CONNECTION_PROFILE_OBFSVPN_HOP_KCP);

        assertEquals(expectation.toString(),actual.toString());
    }

    @Test
    public void toJson_obfs4hop_portHopping() throws JSONException {

        VpnProfile mockVpnProfile = new VpnProfile("mockProfile", OBFS4_HOP);

        Transport.Options options = new Transport.Options("1", null, "CERT",200, 100, true);
        Transport transport = new Transport(OBFS4_HOP.toString(), new String[]{"tcp"}, new String[]{"1234"}, options);
        mockVpnProfile.mConnections[0] = new Obfs4Connection(new Obfs4Options("192.168.0.1", transport));

        mockVpnProfile.mLastUsed = 0;
        String s = mockVpnProfile.toJson();

        //ignore UUID in comparison -> set it to fixed value
        JSONObject actual = new JSONObject(s);
        actual.put("mUuid", "9d295ca2-3789-48dd-996e-f731dbf50fdc");
        JSONObject expectation = new JSONObject(OBFS4CONNECTION_PROFILE_OBFSVPN_PORTHOPPING);

        assertEquals(expectation.toString(),actual.toString());
    }

    @Test
    public void toJson_obfs4hop() throws JSONException {

        VpnProfile mockVpnProfile = new VpnProfile("mockProfile", OBFS4_HOP);
        Transport.Options options = new Transport.Options("1", new Transport.Endpoint[]{new Transport.Endpoint("1.1.1.1", "CERT1"), new Transport.Endpoint("2.2.2.2", "CERT2")}, 200, 100, true);
        Transport transport = new Transport(OBFS4.toString(), new String[]{"tcp"}, new String[]{"1234"}, options);
        mockVpnProfile.mConnections[0] = new Obfs4Connection(new Obfs4Options("192.168.0.1", transport));

        mockVpnProfile.mLastUsed = 0;
        String s = mockVpnProfile.toJson();

        //ignore UUID in comparison -> set it to fixed value
        JSONObject actual = new JSONObject(s);
        actual.put("mUuid", "9d295ca2-3789-48dd-996e-f731dbf50fdc");
        JSONObject expectation = new JSONObject(OBFS4CONNECTION_PROFILE_OBFSVPN_HOP);

        assertEquals(expectation.toString(),actual.toString());
    }

    @Test
    public void fromJson_obfs4() {

        VpnProfile mockVpnProfile = VpnProfile.fromJson(OBFS4CONNECTION_PROFILE);
        assertNotNull(mockVpnProfile);
        assertNotNull(mockVpnProfile.mConnections);
        assertNotNull(mockVpnProfile.mConnections[0]);
        assertTrue(mockVpnProfile.mConnections[0].isUseUdp());
        Obfs4Connection obfs4Connection = (Obfs4Connection) mockVpnProfile.mConnections[0];
        assertEquals(OBFS4, obfs4Connection.getTransportType());
        String[] protocols = obfs4Connection.getObfs4Options().transport.getProtocols();
        for (String protocol : protocols) {
            assertNotEquals(UDP, protocol);
        }
        assertEquals("CERT", obfs4Connection.getObfs4Options().transport.getOptions().getCert());
        assertEquals("0", obfs4Connection.getObfs4Options().transport.getOptions().getIatMode());
        assertEquals("192.168.0.1", obfs4Connection.getObfs4Options().bridgeIP);
        assertEquals("1234", obfs4Connection.getObfs4Options().transport.getPorts()[0]);
        assertEquals(1, obfs4Connection.getObfs4Options().transport.getPorts().length);
    }

    @Test
    public void fromJson_obfs4_obfsvpn() {

        VpnProfile mockVpnProfile = VpnProfile.fromJson(OBFS4CONNECTION_PROFILE_OBFSVPN);
        assertNotNull(mockVpnProfile);
        assertNotEquals(null, mockVpnProfile.mConnections);
        assertNotNull(mockVpnProfile.mConnections[0]);
        assertTrue(mockVpnProfile.mConnections[0].isUseUdp());
        Obfs4Connection obfs4Connection = (Obfs4Connection) mockVpnProfile.mConnections[0];
        assertEquals(OBFS4, obfs4Connection.getTransportType());
        assertEquals("tcp", obfs4Connection.getObfs4Options().transport.getProtocols()[0]);
        assertEquals("CERT", obfs4Connection.getObfs4Options().transport.getOptions().getCert());
        assertEquals("1", obfs4Connection.getObfs4Options().transport.getOptions().getIatMode());
        assertEquals("192.168.0.1", obfs4Connection.getObfs4Options().bridgeIP);
        assertEquals("1234", obfs4Connection.getObfs4Options().transport.getPorts()[0]);
        assertEquals(1, obfs4Connection.getObfs4Options().transport.getPorts().length);
    }

    @Test
    public void fromJson_obfs4_obfsvpn_kcp() {

        VpnProfile mockVpnProfile = VpnProfile.fromJson(OBFS4CONNECTION_PROFILE_OBFSVPN_KCP);
        assertNotNull(mockVpnProfile);
        assertNotNull(mockVpnProfile.mConnections);
        assertNotNull(mockVpnProfile.mConnections[0]);
        assertTrue(mockVpnProfile.mConnections[0].isUseUdp());
        Obfs4Connection obfs4Connection = (Obfs4Connection) mockVpnProfile.mConnections[0];
        assertEquals(OBFS4, obfs4Connection.getTransportType());
        assertEquals("kcp", obfs4Connection.getObfs4Options().transport.getProtocols()[0]);
        assertEquals("CERT", obfs4Connection.getObfs4Options().transport.getOptions().getCert());
        assertEquals("1", obfs4Connection.getObfs4Options().transport.getOptions().getIatMode());
        assertEquals("192.168.0.1", obfs4Connection.getObfs4Options().bridgeIP);
        assertEquals("1234", obfs4Connection.getObfs4Options().transport.getPorts()[0]);
    }
}