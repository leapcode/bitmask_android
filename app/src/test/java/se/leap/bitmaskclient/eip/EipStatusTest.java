package se.leap.bitmaskclient.eip;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.VpnStatus;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;

import static de.blinkt.openvpn.core.ConnectionStatus.LEVEL_AUTH_FAILED;
import static de.blinkt.openvpn.core.ConnectionStatus.LEVEL_CONNECTED;
import static de.blinkt.openvpn.core.ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET;
import static de.blinkt.openvpn.core.ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED;
import static de.blinkt.openvpn.core.ConnectionStatus.LEVEL_START;
import static de.blinkt.openvpn.core.ConnectionStatus.LEVEL_VPNPAUSED;
import static de.blinkt.openvpn.core.ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT;
import static de.blinkt.openvpn.core.ConnectionStatus.UNKNOWN_LEVEL;
import static de.blinkt.openvpn.core.connection.Connection.TransportType.OPENVPN;
import static junit.framework.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static se.leap.bitmaskclient.eip.EipStatus.EipLevel.CONNECTING;
import static se.leap.bitmaskclient.eip.EipStatus.EipLevel.DISCONNECTED;
import static se.leap.bitmaskclient.eip.EipStatus.EipLevel.UNKNOWN;

/**
 * Created by cyberta on 06.12.17.
 * TODO: Mock AsyncTask
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({PreferenceHelper.class})
public class EipStatusTest {

    EipStatus eipStatus;

    @Before
    public void setUp() {
        eipStatus = EipStatus.getInstance();

    }

    @Test
    public void testUpdateState_LEVEL_CONNECTED() throws Exception {
        eipStatus.updateState("CONNECTED", "", R.string.state_connected, LEVEL_CONNECTED );
        assertTrue("LEVEL_CONNECTED eipLevel", eipStatus.getEipLevel() == EipStatus.EipLevel.CONNECTED);
        assertTrue("LEVEL_CONNECTED level", eipStatus.getLevel() == LEVEL_CONNECTED);
        assertTrue("LEVEL_CONNECTED localizedResId", eipStatus.getLocalizedResId() == R.string.state_connected);
        assertTrue("LEVEL_CONNECTED state", eipStatus.getState().equals("CONNECTED"));
    }

    @Test
    public void testUpdateState_LEVEL_VPNPAUSED_hasPersistentTun() throws Exception {

        mockStatic(PreferenceHelper.class);
        VpnProfile mockVpnProfile = new VpnProfile("mockProfile", OPENVPN);
        mockVpnProfile.mPersistTun = true;
        doNothing().when(PreferenceHelper.class);
        VpnStatus.setLastConnectedVpnProfile(null, mockVpnProfile);
        VpnStatus.updateStateString("SCREENOFF", "", R.string.state_screenoff, LEVEL_VPNPAUSED);
        assertTrue("LEVEL_VPN_PAUSED eipLevel", eipStatus.getEipLevel() == CONNECTING);
        assertTrue("LEVEL_VPN_PAUSED level", eipStatus.getLevel() == LEVEL_VPNPAUSED);
    }

    @Test
    public void testUpdateState_LEVEL_VPNPAUSED_hasNotPersistentTun() throws Exception {

        mockStatic(PreferenceHelper.class);
        VpnProfile mockVpnProfile = new VpnProfile("mockProfile", OPENVPN);
        mockVpnProfile.mPersistTun = false;
        doNothing().when(PreferenceHelper.class);
        VpnStatus.setLastConnectedVpnProfile(null, mockVpnProfile);
        VpnStatus.updateStateString("SCREENOFF", "", R.string.state_screenoff, LEVEL_VPNPAUSED);
        assertTrue("LEVEL_VPN_PAUSED eipLevel", eipStatus.getEipLevel() == DISCONNECTED);
        assertTrue("LEVEL_VPN_PAUSED level", eipStatus.getLevel() == LEVEL_VPNPAUSED);
    }

    @Test
    public void testUpdateState_LEVEL_CONNECTING_SERVER_REPLIED() throws Exception {
        eipStatus.updateState("ADD_ROUTES", "", 0, LEVEL_CONNECTING_SERVER_REPLIED );
        assertTrue("LEVEL_CONNECTING_SERVER_REPLIED eipLevel", eipStatus.getEipLevel() == CONNECTING);
        assertTrue("LEVEL_CONNECTING_SERVER_REPLIED level", eipStatus.getLevel() == LEVEL_CONNECTING_SERVER_REPLIED);
    }

    @Test
    public void testUpdateState_LEVEL_CONNECTING_NO_SERVER_REPLY_YET() throws Exception {
        eipStatus.updateState("ADD_ROUTES", "", 0, LEVEL_CONNECTING_NO_SERVER_REPLY_YET );
        assertTrue("LEVEL_CONNECTING_SERVER_REPLIED eipLevel", eipStatus.getEipLevel() == CONNECTING);
        assertTrue("LEVEL_CONNECTING_SERVER_REPLIED level", eipStatus.getLevel() == LEVEL_CONNECTING_NO_SERVER_REPLY_YET);
    }
    @Test
    public void testUpdateState_LEVEL_START() throws Exception {
        eipStatus.updateState("VPN_GENERATE_CONFIG", "", R.string.building_configration, ConnectionStatus.LEVEL_START);
        assertTrue("LEVEL_START eipLevel", eipStatus.getEipLevel() == CONNECTING);
        assertTrue("LEVEL_START level", eipStatus.getLevel() == LEVEL_START);
    }

    @Test
    public void testUpdateState_LEVEL_WAITING_FOR_USER_INPUT() throws Exception {
        eipStatus.updateState("NEED", "", 0, LEVEL_WAITING_FOR_USER_INPUT);
        assertTrue("LEVEL_WAITING_FOR_USER_INPUT eipLevel", eipStatus.getEipLevel() == CONNECTING);
        assertTrue("LEVEL_WAITING_FOR_USER_INPUT", eipStatus.getLevel() == LEVEL_WAITING_FOR_USER_INPUT);
    }

    @Test
    public void testUpdateState_LEVEL_AUTH_FAILED() throws Exception {
        eipStatus.updateState("AUTH_FAILED", "", R.string.state_auth_failed, ConnectionStatus.LEVEL_AUTH_FAILED);
        assertTrue("AUTH_FAILED eipLevel", eipStatus.getEipLevel() == DISCONNECTED);
        assertTrue("AUTH_FAILED level", eipStatus.getLevel() == LEVEL_AUTH_FAILED);
    }


    @Test
    public void testUpdateState_UNKNOWN_LEVEL() throws Exception {
        eipStatus.updateState("UNKNOWN", "", 0, ConnectionStatus.UNKNOWN_LEVEL);
        assertTrue("UNKNOWN_LEVEL eipLevel", eipStatus.getEipLevel() == UNKNOWN);
        assertTrue("UNKNOWN_LEVEL level", eipStatus.getLevel() == UNKNOWN_LEVEL);
    }

}