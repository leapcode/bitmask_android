package io.swagger.client;

import org.junit.Test;

import java.io.IOException;
import static org.junit.Assert.*;

import static de.blinkt.openvpn.core.connection.Connection.TransportType.OBFS4;

import com.google.gson.JsonSyntaxException;

import de.blinkt.openvpn.core.ConfigParser;
import io.swagger.client.model.ModelsProvider;
import se.leap.bitmaskclient.testutils.TestSetupHelper;

public class JSONTest {

    @Test
    public void testProviderJsonParsing_testBackwardsCompatibility_v4() throws IOException {
        String boblove = TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("v4/riseup.net.json"));
        ModelsProvider p = JSON.createGson().create().fromJson(boblove, ModelsProvider.class);
        assertNotNull(p);
        assertEquals("riseup.net", p.getDomain());
    }

    @Test
    public void testProvidingNull() throws IOException {
        String p = JSON.createGson().create().toJson(null);
        assertEquals("null", p);
    }
}
