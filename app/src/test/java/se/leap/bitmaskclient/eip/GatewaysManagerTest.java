package se.leap.bitmaskclient.eip;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

import se.leap.bitmaskclient.Constants;
import se.leap.bitmaskclient.Provider;
import se.leap.bitmaskclient.testutils.TestSetupHelper;

import static junit.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Created by cyberta on 09.10.17.
 */
@RunWith(MockitoJUnitRunner.class)
public class GatewaysManagerTest {

    private GatewaysManager gatewaysManager;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mockContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SharedPreferences sharedPreferences;

    @Before
    public void setUp() throws IOException, JSONException {


        JSONObject secrets = new JSONObject(getJsonStringFor("secrets.json"));

        when(sharedPreferences.getString(eq(Constants.PROVIDER_PRIVATE_KEY), anyString())).thenReturn(secrets.getString(Constants.PROVIDER_PRIVATE_KEY));
        when(sharedPreferences.getString(eq(Provider.CA_CERT), anyString())).thenReturn(secrets.getString(Provider.CA_CERT));
        when(sharedPreferences.getString(eq(Constants.PROVIDER_VPN_CERTIFICATE), anyString())).thenReturn(secrets.getString(Constants.PROVIDER_VPN_CERTIFICATE));
        when(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences);


        gatewaysManager = new GatewaysManager(mockContext, sharedPreferences);
    }

    @Test
    public void testFromEipServiceJson_emptyJson() throws Exception {
            gatewaysManager.fromEipServiceJson(new JSONObject());
            assertEquals(0, gatewaysManager.size());
    }


    @Test
    public void testFromEipServiceJson_ignoreDuplicateGateways() throws Exception {
        String eipServiceJson = TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("eip-service-two-gateways.json"));
        gatewaysManager.fromEipServiceJson(new JSONObject(eipServiceJson));
        assertEquals(2, gatewaysManager.size());
        eipServiceJson = TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("eip-service-one-gateway.json"));
        gatewaysManager.fromEipServiceJson(new JSONObject(eipServiceJson));
        assertEquals(2, gatewaysManager.size());
    }

    @Test
    public void testClearGatewaysAndProfiles_resetGateways() throws Exception {
        String eipServiceJson = TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("eip-service-two-gateways.json"));
        gatewaysManager.fromEipServiceJson(new JSONObject(eipServiceJson));
        assertEquals(2, gatewaysManager.size());
        gatewaysManager.clearGatewaysAndProfiles();
        assertEquals(0, gatewaysManager.size());
    }

    private String getJsonStringFor(String filename) throws IOException {
        return TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream(filename));
    }

}