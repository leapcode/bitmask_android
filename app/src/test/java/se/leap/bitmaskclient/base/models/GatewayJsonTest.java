package se.leap.bitmaskclient.base.models;

import static org.junit.Assert.assertEquals;
import static de.blinkt.openvpn.core.connection.Connection.TransportType.OBFS4;
import static se.leap.bitmaskclient.eip.GatewaysManager.PINNED_OBFUSCATION_PROXY;

import org.junit.Test;

import de.blinkt.openvpn.core.connection.Connection;

public class GatewayJsonTest {

    @Test
    public void testToString() {
        String gatewayJSON = "{\"location\":\"Unknown Location\",\"ip_address\":\"1.2.3.4\",\"host\":\"pinned.obfuscation.proxy\",\"capabilities\":{\"adblock\":false,\"filter_dns\":false,\"limited\":false,\"transport\":[{\"type\":\"obfs4\",\"protocols\":[\"tcp\"],\"ports\":[\"1194\"],\"options\":{\"cert\":\"xxxxxxx\",\"iatMode\":\"0\"}}],\"user_ips\":false}}";

        Connection.TransportType transportType = OBFS4;
        Transport[] transports = new Transport[]{
                new Transport(transportType.toString(),
                        new String[]{"tcp"},
                        new String[]{"1194"},
                        "xxxxxxx")};
        GatewayJson.Capabilities capabilities = new GatewayJson.Capabilities(false, false, false, transports, false);
        GatewayJson gatewayJson = new GatewayJson("Unknown Location", "1.2.3.4", null, PINNED_OBFUSCATION_PROXY, capabilities);

        assertEquals(gatewayJSON, gatewayJson.toString());
    }

}