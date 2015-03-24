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

import android.content.*;

import com.google.gson.*;
import com.google.gson.reflect.*;

import org.json.*;

import java.lang.reflect.*;
import java.util.*;

import de.blinkt.openvpn.*;
import de.blinkt.openvpn.core.*;
import se.leap.bitmaskclient.*;

/**
 * @author parmegv
 */
public class GatewaysManager {

    private Context context;
    private SharedPreferences preferences;
    private List<Gateway> gateways = new ArrayList<>();
    private ProfileManager profile_manager;
    private Type list_type = new TypeToken<ArrayList<Gateway>>() {
    }.getType();

    public GatewaysManager() {
    }

    public GatewaysManager(Context context, SharedPreferences preferences) {
        this.context = context;
        this.preferences = preferences;
        profile_manager = ProfileManager.getInstance(context);
    }

    public Gateway select() {
        GatewaySelector gateway_selector = new GatewaySelector(gateways);
        return gateway_selector.select();
    }

    public boolean isEmpty() {
        return gateways.isEmpty();
    }

    public int size() {
        return gateways.size();
    }

    public void addFromString(String gateways) {
        List<Gateway> gateways_list = new ArrayList<Gateway>();
        try {
            gateways_list = new Gson().fromJson(gateways, list_type);
        } catch (JsonSyntaxException e) {
            gateways_list.add(new Gson().fromJson(gateways, Gateway.class));
        }

        if (gateways_list != null) {
            for (Gateway gateway : gateways_list)
                addGateway(gateway);
            this.gateways.addAll(gateways_list);
        }
    }

    @Override
    public String toString() {
        return new Gson().toJson(gateways, list_type);
    }

    public void fromEipServiceJson(JSONObject eip_definition) {
        try {
            JSONArray gatewaysDefined = eip_definition.getJSONArray("gateways");
            for (int i = 0; i < gatewaysDefined.length(); i++) {
                JSONObject gw = gatewaysDefined.getJSONObject(i);
                if (isOpenVpnGateway(gw)) {
                    JSONObject secrets = secretsConfiguration();
                    Gateway aux = new Gateway(eip_definition, secrets, gw);
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
            result.put(Constants.PRIVATE_KEY, preferences.getString(Constants.PRIVATE_KEY, ""));
            result.put(Constants.CERTIFICATE, preferences.getString(Constants.CERTIFICATE, ""));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    private boolean containsProfileWithSecrets(VpnProfile profile) {
        boolean result = false;

        Collection<VpnProfile> profiles = profile_manager.getProfiles();
        for (VpnProfile aux : profiles) {
            result = result || sameConnections(profile.mConnections, aux.mConnections)
                    && profile.mClientCertFilename.equalsIgnoreCase(aux.mClientCertFilename)
                    && profile.mClientKeyFilename.equalsIgnoreCase(aux.mClientKeyFilename);
        }
        return result;
    }

    private void addGateway(Gateway gateway) {
        removeDuplicatedGateway(gateway);

        gateways.add(gateway);

        VpnProfile profile = gateway.getProfile();
        profile_manager.addProfile(profile);
        //profile_manager.saveProfile(context, profile);
        //profile_manager.saveProfileList(context);
    }

    private void removeDuplicatedGateway(Gateway gateway) {
        Iterator<Gateway> it = gateways.iterator();
        List<Gateway> gateways_to_remove = new ArrayList<>();
        while (it.hasNext()) {
            Gateway aux = it.next();
            if (sameConnections(aux.getProfile().mConnections, gateway.getProfile().mConnections)) {
                gateways_to_remove.add(aux);
            }
        }
        gateways.removeAll(gateways_to_remove);
        removeDuplicatedProfiles(gateway.getProfile());
    }

    private void removeDuplicatedProfiles(VpnProfile original) {
        Collection<VpnProfile> profiles = profile_manager.getProfiles();
        List<VpnProfile> remove_list = new ArrayList<>();
        for (VpnProfile aux : profiles) {
            if (sameConnections(original.mConnections, aux.mConnections))
                remove_list.add(aux);
        }
        for (VpnProfile profile : remove_list)
            profile_manager.removeProfile(context, profile);
    }

    private boolean sameConnections(Connection[] c1, Connection[] c2) {
        int same_connections = 0;
        for (Connection c1_aux : c1) {
            for (Connection c2_aux : c2)
                if (c2_aux.mServerName.equals(c1_aux.mServerName)) {
                    same_connections++;
                    break;
                }
        }
        return c1.length == c2.length && c1.length == same_connections;
    }
}
