/**
 * Copyright (c) 2013, 2014, 2015 LEAP Encryption Access Project and contributers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package se.leap.bitmaskclient.test;

import android.app.*;
import android.content.*;
import android.test.*;
import android.test.suitebuilder.annotation.*;

import org.json.*;

import se.leap.bitmaskclient.*;
import se.leap.bitmaskclient.eip.*;

/**
 * @author parmegv
 */
public class testGatewaysManager extends InstrumentationTestCase {

    GatewaysManager gateways_manager;
    Gateway gateway;
    JSONObject eip_definition;

    FromAssets assets;

    Context context;
    SharedPreferences preferences;

    @Override
    protected void setUp() throws Exception {
        context = getInstrumentation().getContext();
        assets = new FromAssets(context);
        mockGatewaysManager();
        mockRealGateway();
        super.setUp();
    }

    @MediumTest
    public void testFromEipServiceJson() {
        gateways_manager.fromEipServiceJson(eip_definition);
        assertEquals(2, gateways_manager.size());
        gateways_manager.addFromString(gateway.toString());
        assertEquals(2, gateways_manager.size());
    }

    @SmallTest
    public void testAddFromString() {
        gateways_manager.addFromString("");
        gateways_manager.addFromString(gateway.toString());
    }

    @MediumTest
    public void testRemoveDuplicate() {
        gateways_manager.addFromString(gateway.toString());
        assertEquals(1, gateways_manager.size());

        mockArtificialGateway();
        gateways_manager.addFromString(gateway.toString());
        assertEquals(1, gateways_manager.size());
    }

    @MediumTest
    public void testToString() {
        assertEquals("[]", gateways_manager.toString());

        gateways_manager.addFromString(gateway.toString());
        assertEquals("[" + gateway.toString() + "]", gateways_manager.toString());
    }

    @SmallTest
    public void testIsEmpty() {
        assertTrue(gateways_manager.isEmpty());
        gateways_manager.addFromString("");
        assertTrue(gateways_manager.isEmpty());
        gateways_manager.addFromString(gateway.toString());
        assertFalse(gateways_manager.isEmpty());
    }

    private void mockGatewaysManager() {
        context = getInstrumentation().getContext();
        preferences = context.getSharedPreferences(Dashboard.SHARED_PREFERENCES, Activity.MODE_PRIVATE);
        gateways_manager = new GatewaysManager(context, preferences);
    }

    private void mockRealGateway() {
        try {
            eip_definition = new JSONObject(assets.toString(TestConstants.EIP_DEFINITION_FILE));
            JSONObject secrets = new JSONObject(assets.toString(TestConstants.SECRETS_FILE));
            JSONObject gateway = new JSONObject(assets.toString(TestConstants.GATEWAY_FILE));
            this.gateway = new Gateway(eip_definition, secrets, gateway);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mockArtificialGateway() {
        try {
            eip_definition = new JSONObject(assets.toString(TestConstants.EIP_DEFINITION_FILE));
            JSONObject secrets = new JSONObject(assets.toString(TestConstants.SECRETS_FILE).replace("6u6", "7u7"));
            JSONObject gateway = new JSONObject(assets.toString(TestConstants.GATEWAY_FILE));
            this.gateway = new Gateway(eip_definition, secrets, gateway);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
