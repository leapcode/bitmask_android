/**
 * Copyright (c) 2013 - 2019 LEAP Encryption Access Project and contributors
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
package se.leap.bitmaskclient.eip;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import se.leap.bitmaskclient.Provider;
import se.leap.bitmaskclient.utils.PreferenceHelper;

import static se.leap.bitmaskclient.Constants.GATEWAYS;
import static se.leap.bitmaskclient.Constants.PROVIDER_PRIVATE_KEY;
import static se.leap.bitmaskclient.Constants.PROVIDER_VPN_CERTIFICATE;

/**
 * @author parmegv
 */
public class GatewaysManager {

    private static final String TAG = GatewaysManager.class.getSimpleName();

    private Context context;
    private SharedPreferences preferences;
    private LinkedHashMap<String, Gateway> gateways = new LinkedHashMap<>();
    private Type listType = new TypeToken<ArrayList<Gateway>>() {}.getType();

    GatewaysManager(Context context, SharedPreferences preferences) {
        this.context = context;
        this.preferences = preferences;
    }

    /**
     * select closest Gateway
      * @return the n closest Gateway
     */
    public Gateway select(int nClosest) {
        GatewaySelector gatewaySelector = new GatewaySelector(new ArrayList<>(gateways.values()));
        return gatewaySelector.select(nClosest);
    }

    /**
     * check if there are no gateways defined
     * @return true if no gateways defined else false
     */
    public boolean isEmpty() {
        return gateways.isEmpty();
    }

    /**
     * @return number of gateways defined in the GatewaysManager
     */
    public int size() {
        return gateways.size();
    }

    @Override
    public String toString() {
        return new Gson().toJson(gateways, listType);
    }

    /**
     * parse gateways from eipDefinition
     * @param eipDefinition eipServiceJson
     */
    void fromEipServiceJson(JSONObject eipDefinition) {
        try {
            JSONArray gatewaysDefined = eipDefinition.getJSONArray(GATEWAYS);
            for (int i = 0; i < gatewaysDefined.length(); i++) {
                JSONObject gw = gatewaysDefined.getJSONObject(i);
                JSONObject secrets = secretsConfiguration();
                Gateway aux = new Gateway(eipDefinition, secrets, gw);
                if (gateways.get(aux.getRemoteIP()) == null) {
                    addGateway(aux);
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private JSONObject secretsConfiguration() {
        JSONObject result = new JSONObject();
        try {
            result.put(Provider.CA_CERT, preferences.getString(Provider.CA_CERT, ""));
            result.put(PROVIDER_PRIVATE_KEY, preferences.getString(PROVIDER_PRIVATE_KEY, ""));
            result.put(PROVIDER_VPN_CERTIFICATE, preferences.getString(PROVIDER_VPN_CERTIFICATE, ""));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }


    void clearGateways() {
        gateways.clear();
    }

    private void addGateway(Gateway gateway) {
        gateways.put(gateway.getRemoteIP(), gateway);
    }

    /**
     * read EipServiceJson from preferences and set gateways
     */
    void configureFromPreferences() {
        fromEipServiceJson(
                PreferenceHelper.getEipDefinitionFromPreferences(preferences)
        );
    }
}
