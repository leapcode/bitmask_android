package se.leap.bitmaskclient.pluggableTransports.models;

import static org.junit.Assert.*;

import org.junit.Test;

public class KcpConfigTest {

    private static final String KCP_DEFAULTS = "{\"enabled\":true,\"send_window_size\":65535,\"receive_window_size\":65535,\"read_buffer\":16777216,\"write_buffer\":16777216,\"no_delay\":true,\"disable_flow_control\":true,\"interval\":10,\"resend\":2,\"mtu\":1400}";
    @Test
    public void testToString() {
        KcpConfig config = new KcpConfig(true);
        assertEquals(KCP_DEFAULTS, config.toString());
    }
}