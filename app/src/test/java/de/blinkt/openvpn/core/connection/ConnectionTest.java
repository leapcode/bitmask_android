package de.blinkt.openvpn.core.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static de.blinkt.openvpn.core.connection.Connection.TransportType.OBFS4_HOP;

import org.junit.Test;

public class ConnectionTest {

    @Test
    public void TransportTypeTest_fromString() {
        Connection.TransportType transportType = Connection.TransportType.fromString("obfs4-hop");
        assertEquals(OBFS4_HOP, transportType);
    }

    @Test
    public void TransportTypeTest_toString() {
        assertEquals("obfs4-hop", OBFS4_HOP.toString());
    }

    @Test
    public void TransportTypeTest_valueOf() {
        Exception exception = null;
        try {
            Connection.TransportType.valueOf("obfs4-hop");
        } catch (IllegalArgumentException e) {
            exception = e;
        }
        assertNotNull(exception);
    }

}
