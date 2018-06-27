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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.Connection;
import de.blinkt.openvpn.core.ProfileManager;
import se.leap.bitmaskclient.Provider;
import se.leap.bitmaskclient.utils.PreferenceHelper;

import static se.leap.bitmaskclient.Constants.PROVIDER_PRIVATE_KEY;
import static se.leap.bitmaskclient.Constants.PROVIDER_VPN_CERTIFICATE;

/**
 * @author parmegv
 */
public class GatewaysManager {

    private Context context;
    private SharedPreferences preferences;
    private List<Gateway> gateways = new ArrayList<>();
    private ProfileManager profileManager;
    private Type listType = new TypeToken<ArrayList<Gateway>>() {}.getType();

    GatewaysManager(Context context, SharedPreferences preferences) {
        this.context = context;
        this.preferences = preferences;
        profileManager = ProfileManager.getInstance(context);
    }

    /**
     * select closest Gateway
      * @return the closest Gateway
     */
    public Gateway select() {
        GatewaySelector gatewaySelector = new GatewaySelector(gateways);
        return gatewaySelector.select();
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
            JSONArray gatewaysDefined = eipDefinition.getJSONArray("gateways");
            for (int i = 0; i < gatewaysDefined.length(); i++) {
                JSONObject gw = gatewaysDefined.getJSONObject(i);
                if (isOpenVpnGateway(gw)) {
                    JSONObject secrets = secretsConfiguration();
                    Gateway aux = new Gateway(eipDefinition, secrets, gw);
                    if (!containsProfileWithSecrets(aux.getProfile())) {
                        addGateway(aux);
                    }
                }
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * check if a gateway is an OpenVpn gateway
     * @param gateway to check
     * @return true if gateway is an OpenVpn gateway otherwise false
     */
    private boolean isOpenVpnGateway(JSONObject gateway) {
        try {
            String transport = gateway.getJSONObject("capabilities").getJSONArray("transport").toString();
            return transport.contains("openvpn");
        } catch (JSONException e) {
            return false;
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

    private boolean containsProfileWithSecrets(VpnProfile profile) {
        boolean result = false;

        Collection<VpnProfile> profiles = profileManager.getProfiles();
        for (VpnProfile aux : profiles) {
            result = result || sameConnections(profile.mConnections, aux.mConnections)
                    && profile.mClientCertFilename.equalsIgnoreCase(aux.mClientCertFilename)
                    && profile.mClientKeyFilename.equalsIgnoreCase(aux.mClientKeyFilename);
        }
        return result;
    }

    void clearGatewaysAndProfiles() {
        gateways.clear();
        ArrayList<VpnProfile> profiles = new ArrayList<>(profileManager.getProfiles());
        for (VpnProfile profile : profiles) {
            profileManager.removeProfile(context, profile);
        }
    }

    private void addGateway(Gateway gateway) {
        removeDuplicatedGateway(gateway);
        gateways.add(gateway);

        VpnProfile profile = gateway.getProfile();
        profileManager.addProfile(profile);
    }

    private void removeDuplicatedGateway(Gateway gateway) {
        Iterator<Gateway> it = gateways.iterator();
        List<Gateway> gatewaysToRemove = new ArrayList<>();
        while (it.hasNext()) {
            Gateway aux = it.next();
            if (sameConnections(aux.getProfile().mConnections, gateway.getProfile().mConnections)) {
                gatewaysToRemove.add(aux);
            }
        }
        gateways.removeAll(gatewaysToRemove);
        removeDuplicatedProfiles(gateway.getProfile());
    }

    private void removeDuplicatedProfiles(VpnProfile original) {
        Collection<VpnProfile> profiles = profileManager.getProfiles();
        List<VpnProfile> removeList = new ArrayList<>();
        for (VpnProfile aux : profiles) {
            if (sameConnections(original.mConnections, aux.mConnections)) {
                removeList.add(aux);
            }
        }
        for (VpnProfile profile : removeList) {
            profileManager.removeProfile(context, profile);
        }
    }

    /**
     * check if all connections in c1 are also in c2
     * @param c1 array of connections
     * @param c2 array of connections
     * @return true if all connections of c1 exist in c2 and vice versa
     */
    private boolean sameConnections(Connection[] c1, Connection[] c2) {
        int sameConnections = 0;
        for (Connection c1_aux : c1) {
            for (Connection c2_aux : c2)
                if (c2_aux.mServerName.equals(c1_aux.mServerName)) {
                    sameConnections++;
                    break;
                }
        }
        return c1.length == c2.length && c1.length == sameConnections;
    }

    /**
     * read EipServiceJson from preferences and set gateways
     */
    void configureFromPreferences() {
        //TODO: THIS IS A QUICK FIX - it deletes all profiles in ProfileManager, thus it's possible
        // to add all gateways from prefs without duplicates, but this should be refactored.
        clearGatewaysAndProfiles();
        fromEipServiceJson(
                PreferenceHelper.getEipDefinitionFromPreferences(preferences)
        );
    }
}
