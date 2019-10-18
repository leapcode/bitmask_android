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

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.VpnStatus;
import de.blinkt.openvpn.core.connection.Connection;
import se.leap.bitmaskclient.Provider;
import se.leap.bitmaskclient.ProviderObservable;
import se.leap.bitmaskclient.utils.PreferenceHelper;

import static de.blinkt.openvpn.core.connection.Connection.TransportType.OBFS4;
import static de.blinkt.openvpn.core.connection.Connection.TransportType.OPENVPN;
import static se.leap.bitmaskclient.Constants.GATEWAYS;
import static se.leap.bitmaskclient.Constants.PROVIDER_PRIVATE_KEY;
import static se.leap.bitmaskclient.Constants.PROVIDER_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.utils.PreferenceHelper.getUsePluggableTransports;

/**
 * @author parmegv
 */
public class GatewaysManager {

    private static final String TAG = GatewaysManager.class.getSimpleName();

    private Context context;
    private SharedPreferences preferences;
    private LinkedHashMap<String, Gateway> gateways = new LinkedHashMap<>();
    private Type listType = new TypeToken<ArrayList<Gateway>>() {}.getType();

    public GatewaysManager(Context context, SharedPreferences preferences) {
        this.preferences = preferences;
        this.context = context;
        configureFromPreferences();
    }

    public GatewaysManager(Context context) {
        configureFromCurrentProvider();
        this.context = context;
    }

    /**
     * select closest Gateway
      * @return the n closest Gateway
     */
    public Gateway select(int nClosest) {
        Connection.TransportType transportType = getUsePluggableTransports(context) ? OBFS4 : OPENVPN;
        GatewaySelector gatewaySelector = new GatewaySelector(new ArrayList<>(gateways.values()));
        Gateway gateway;
        while ((gateway = gatewaySelector.select(nClosest)) != null) {
            if (gateway.getProfile(transportType) != null) {
                return gateway;
            }
            nClosest++;
        }
        return null;
    }

    /**
     * Get position of the gateway from a sorted set (along the distance of the gw to your time zone)
     * @param profile profile belonging to a gateway
     * @return position of the gateway owning to the profile
     */
    public int getPosition(VpnProfile profile) {
        Connection.TransportType transportType = getUsePluggableTransports(context) ? OBFS4 : OPENVPN;
        GatewaySelector gatewaySelector = new GatewaySelector(new ArrayList<>(gateways.values()));
        Gateway gateway;
        int nClosest = 0;
        while ((gateway = gatewaySelector.select(nClosest)) != null) {
            if (profile.equals(gateway.getProfile(transportType))) {
                return nClosest;
            }
            nClosest++;
        }
        return -1;
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
     private void fromEipServiceJson(JSONObject eipDefinition, JSONObject secrets) {
        JSONArray gatewaysDefined = new JSONArray();
        try {
            gatewaysDefined = eipDefinition.getJSONArray(GATEWAYS);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (int i = 0; i < gatewaysDefined.length(); i++) {
            try {
                JSONObject gw = gatewaysDefined.getJSONObject(i);
                Gateway aux = new Gateway(eipDefinition, secrets, gw, this.context);
                if (gateways.get(aux.getRemoteIP()) == null) {
                    addGateway(aux);
                }
            } catch (JSONException | ConfigParser.ConfigParseError | IOException e) {
                e.printStackTrace();
                VpnStatus.logError("Unable to parse gateway config!");
            }
        }
    }

    private JSONObject secretsConfigurationFromPreferences() {
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

    private JSONObject secretsConfigurationFromCurrentProvider() {
        JSONObject result = new JSONObject();
        Provider provider = ProviderObservable.getInstance().getCurrentProvider();

        try {
            result.put(Provider.CA_CERT, provider.getCaCert());
            result.put(PROVIDER_PRIVATE_KEY, provider.getPrivateKey());
            result.put(PROVIDER_VPN_CERTIFICATE, provider.getVpnCertificate());
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
    private void configureFromPreferences() {
        fromEipServiceJson(
                PreferenceHelper.getEipDefinitionFromPreferences(preferences), secretsConfigurationFromPreferences()
        );
    }

    private void configureFromCurrentProvider() {
        try {
            JSONObject json = ProviderObservable.getInstance().getCurrentProvider().getEipServiceJson();
            fromEipServiceJson(json, secretsConfigurationFromCurrentProvider());
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        }

    }
}
