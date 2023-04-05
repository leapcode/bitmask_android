package se.leap.bitmaskclient.base.models;

import static se.leap.bitmaskclient.base.models.Constants.CAPABILITIES;
import static se.leap.bitmaskclient.base.models.Constants.TRANSPORT;

import junit.framework.TestCase;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import de.blinkt.openvpn.core.connection.Connection;
import se.leap.bitmaskclient.testutils.TestSetupHelper;

public class TransportTest extends TestCase {

    private JSONObject gateway;

    public void test_obfs4_fromJson() throws IOException, JSONException {
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_obfs4hop_tcp_gateways.json"))).getJSONArray("gateways").getJSONObject(2);
        JSONObject obfs4Transport = gateway.getJSONObject(CAPABILITIES).getJSONArray(TRANSPORT).getJSONObject(1);
        Transport transport = Transport.fromJson(obfs4Transport);
        assertEquals("obfs4", transport.getType());
        assertEquals("0", transport.getOptions().getIatMode());
        assertEquals("kcp", transport.getProtocols()[0]);
        assertEquals(1, transport.getProtocols().length);
        assertEquals("23050", transport.getPorts()[0]);
        assertEquals(1, transport.getPorts().length);
        assertEquals("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX1", transport.getOptions().getCert());
        assertNull(transport.getOptions().getEndpoints());
        assertEquals(0, transport.getOptions().getPortCount());
        assertEquals(0, transport.getOptions().getPortSeed());
        assertFalse(transport.getOptions().isExperimental());
    }

    public void test_obfs4hop_fromJson() throws IOException, JSONException {
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_obfs4hop_tcp_gateways.json"))).getJSONArray("gateways").getJSONObject(2);
        JSONObject obfs4Transport = gateway.getJSONObject(CAPABILITIES).getJSONArray(TRANSPORT).getJSONObject(2);
        Transport transport = Transport.fromJson(obfs4Transport);
        assertEquals("obfs4-hop", transport.getType());
        assertEquals(Connection.TransportType.OBFS4_HOP, transport.getTransportType());
        assertEquals("tcp", transport.getProtocols()[0]);
        assertEquals(1, transport.getProtocols().length);
        assertNull(transport.getPorts());
        assertNull(transport.getOptions().getCert());
        assertNotNull(transport.getOptions().getEndpoints());
        assertEquals(2, transport.getOptions().getEndpoints().length);
        assertEquals("CERT1", transport.getOptions().getEndpoints()[0].getCert());
        assertEquals("CERT2", transport.getOptions().getEndpoints()[1].getCert());
        assertEquals("1.1.1.1", transport.getOptions().getEndpoints()[0].getIp());
        assertEquals("2.2.2.2", transport.getOptions().getEndpoints()[1].getIp());
        assertTrue(transport.getOptions().isExperimental());
    }

    public void test_openvpn_fromJson() throws IOException, JSONException {
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_obfs4hop_tcp_gateways.json"))).getJSONArray("gateways").getJSONObject(2);
        JSONObject obfs4Transport = gateway.getJSONObject(CAPABILITIES).getJSONArray(TRANSPORT).getJSONObject(0);
        Transport transport = Transport.fromJson(obfs4Transport);
        assertEquals("openvpn", transport.getType());
        assertEquals(2, transport.getProtocols().length);
        assertEquals("tcp", transport.getProtocols()[0]);
        assertEquals("udp", transport.getProtocols()[1]);
        assertEquals(1, transport.getPorts().length);
        assertEquals("1195", transport.getPorts()[0]);
        assertNull(transport.getOptions());
    }
}