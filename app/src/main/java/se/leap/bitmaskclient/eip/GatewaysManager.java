/**
 * Copyright (c) 2013 - 2022 LEAP Encryption Access Project and contributors
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

import static de.blinkt.openvpn.core.connection.Connection.TransportType.OBFS4;
import static de.blinkt.openvpn.core.connection.Connection.TransportType.OBFS4_HOP;
import static de.blinkt.openvpn.core.connection.Connection.TransportType.OPENVPN;
import static de.blinkt.openvpn.core.connection.Connection.TransportType.PT;
import static se.leap.bitmaskclient.base.models.Constants.CERT;
import static se.leap.bitmaskclient.base.models.Constants.GATEWAYS;
import static se.leap.bitmaskclient.base.models.Constants.HOST;
import static se.leap.bitmaskclient.base.models.Constants.IAT_MODE;
import static se.leap.bitmaskclient.base.models.Constants.KCP;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.base.models.Constants.SORTED_GATEWAYS;
import static se.leap.bitmaskclient.base.models.Constants.TCP;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getObfuscationPinningCert;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getObfuscationPinningIP;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getObfuscationPinningKCP;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getObfuscationPinningPort;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getPreferredCity;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getUseBridges;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getUseObfs4;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getUseObfs4Kcp;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getUsePortHopping;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.usesSpecificTunnel;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.VpnStatus;
import de.blinkt.openvpn.core.connection.Connection;
import de.blinkt.openvpn.core.connection.Connection.TransportType;
import io.swagger.client.model.ModelsBridge;
import io.swagger.client.model.ModelsEIPService;
import io.swagger.client.model.ModelsGateway;
import se.leap.bitmaskclient.BuildConfig;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.models.GatewayJson;
import se.leap.bitmaskclient.base.models.Location;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.models.ProviderObservable;
import se.leap.bitmaskclient.base.models.Transport;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;

/**
 * @author parmegv
 */
public class GatewaysManager {

    public enum Load {
        UNKNOWN(0),
        GOOD(0.25),
        AVERAGE(0.75),
        CRITICAL(1.0);

        private final double value;

        Load(double i) {
            value = i;
        }

        public static Load getLoadByValue(double value) {
            if (value == UNKNOWN.value) {
                return UNKNOWN;
            } else if (value <= GOOD.value) {
                return GOOD;
            } else if (value <= AVERAGE.value) {
                return AVERAGE;
            } else if (value <= CRITICAL.value) {
                return CRITICAL;
            } else {
                return UNKNOWN;
            }
        }

        public double getValue() {
            return value;
        }
    }

    private static final String TAG = GatewaysManager.class.getSimpleName();
    public static final String PINNED_OBFUSCATION_PROXY = "pinned.obfuscation.proxy";

    private final Context context;
    private final LinkedHashMap<String, Gateway> gateways = new LinkedHashMap<>();
    private final Type listType = new TypeToken<ArrayList<Gateway>>() {}.getType();
    private final ArrayList<Gateway> presortedList = new ArrayList<>();
    private ArrayList<Location> locations = new ArrayList<>();
    private TransportType selectedTransport;

    GatewaySelector gatewaySelector;


    public GatewaysManager(Context context) {
        this.context = context;
        configureFromCurrentProvider();
    }

    /**
     * selects a VpnProfile of the n closest Gateway or a pinned gateway
     * @return VpnProfile of the n closest Gateway or null if no remaining VpnProfiles available
     */
    public @Nullable VpnProfile selectVpnProfile(int nClosestGateway) {
        if (PreferenceHelper.useObfuscationPinning()) {
            if (nClosestGateway > 2) {
                // no need to try again the pinned proxy, probably configuration error
                return null;
            }
            Gateway gateway = gateways.get(PINNED_OBFUSCATION_PROXY);
            if (gateway == null) {
                return null;
            }
            return gateway.getProfile(OBFS4, null);
        }
        String selectedCity = getPreferredCity();
        return selectVpnProfile(nClosestGateway, selectedCity);
    }

    /**
     * Selects a VPN profile, filtered by distance to the user, transportType and
     * optionally by city and transport layer protocol
     * @param nClosestGateway
     * @param city location filter
     * @return VpnProfile of the n closest Gateway or null if no remaining VpnProfiles available
     */
    public @Nullable VpnProfile selectVpnProfile(int nClosestGateway, String city) {
        TransportType[] transportTypes = determineTransportTypes();
        Set<String> obfuscationTransportLayerProtocols = getObfuscationTransportLayerProtocols();
        if (presortedList.size() > 0) {
            return getVpnProfileFromPresortedList(nClosestGateway, transportTypes, obfuscationTransportLayerProtocols, city);
        }

        return getVpnProfileFromTimezoneCalculation(nClosestGateway, transportTypes, obfuscationTransportLayerProtocols, city);
    }

    private TransportType[] determineTransportTypes() {
        if (!getUseBridges()){
            return new TransportType[]{OPENVPN};
        }

        if (getUsePortHopping()) {
            return new TransportType[]{OBFS4_HOP};
        } else if (getUseObfs4() || getUseObfs4Kcp()) {
            return new TransportType[]{OBFS4};
        } else {
            return new TransportType[]{OBFS4, OBFS4_HOP};
        }
    }


    @Nullable
    private static Set<String> getObfuscationTransportLayerProtocols() {
        if (!getUseBridges()) {
            return null;
        }

        if (getUseObfs4()) {
            return Set.of(TCP);
        } else if (getUseObfs4Kcp()) {
            return Set.of(KCP);
        } else {
            // If neither Obf4 nor Obf4Kcp are used, and bridges are enabled,
            // then use both TCP and KCP (based on the original logic).
            return Set.of(TCP, KCP);
        }
    }

    public void updateTransport(TransportType transportType) {
        if (this.selectedTransport == null || transportType != this.selectedTransport) {
            this.selectedTransport = transportType;
            locations.clear();
        }
    }

    public ArrayList<String> getHosts() {
        ArrayList<String> hosts = new ArrayList<>();
        for (Gateway gateway : gateways.values()) {
            hosts.add(gateway.getHost());
        }
        return hosts;
    }


    public String getIpForHost(String gatewayHostName) {
        Gateway gateway = gateways.get(gatewayHostName);
        if (gateway == null) {
            return null;
        }
        return gateway.getRemoteIP();
    }

    public List<Location> getGatewayLocations() {
        return getSortedGatewayLocations(null);
    }

    public List<Location> getSortedGatewayLocations(@Nullable TransportType selectedTransport) {
        if (locations.size() > 0) {
            return locations;
        }

        HashMap<String, Integer> locationNames = new HashMap<>();
        ArrayList<Location> locations = new ArrayList<>();
        String preferredCity = PreferenceHelper.getPreferredCity();
        for (Gateway gateway : gateways.values()) {
            String name = gateway.getName();
            if (name == null) {
                Log.e(TAG, "Gateway without location name found. This should never happen. Provider misconfigured?");
                continue;
            }

            if (!locationNames.containsKey(name)) {
                locationNames.put(name, locations.size());
                Location location = initLocation(name, gateway, preferredCity);
                locations.add(location);
            } else {
                int index = locationNames.get(gateway.getName());
                Location location = locations.get(index);
                updateLocation(location, gateway, PT);
                updateLocation(location, gateway, OPENVPN);
                locations.set(index, location);
            }
        }
        if (selectedTransport != null) {
            Collections.sort(locations, new Location.SortByAverageLoad(selectedTransport));
            this.locations = locations;
        }
        return locations;
    }

    private Location initLocation(String name, Gateway gateway, String preferredCity) {
        HashMap<TransportType, Double> averageLoadMap = new HashMap<>();
        HashMap<TransportType, Integer> numberOfGatewaysMap = new HashMap<>();
        if (gateway.supportsPluggableTransports()) {
            averageLoadMap.put(PT, gateway.getFullness());
            numberOfGatewaysMap.put(PT, 1);
        }
        if (gateway.getSupportedTransports().contains(OPENVPN)) {
            averageLoadMap.put(OPENVPN, gateway.getFullness());
            numberOfGatewaysMap.put(OPENVPN, 1);
        }
        return new Location(
                name,
                averageLoadMap,
                numberOfGatewaysMap,
                name.equals(preferredCity));
    }

    private void updateLocation(Location location, Gateway gateway, Connection.TransportType transportType) {
        if (gateway.supportsTransport(transportType, null)) {
            double averageLoad = location.getAverageLoad(transportType);
            int numberOfGateways = location.getNumberOfGateways(transportType);
            averageLoad = (numberOfGateways * averageLoad + gateway.getFullness()) / (numberOfGateways + 1);
            numberOfGateways++;
            location.setAverageLoad(transportType, averageLoad);
            location.setNumberOfGateways(transportType, numberOfGateways);
        }
    }

    public String getLocationNameForIP(String ip, Context context) {
        for (Gateway gateway : gateways.values()) {
            if (gateway.getRemoteIP().equals(ip)) {
                return gateway.getName();
            }
        }
        return context.getString(R.string.unknown_location);
    }

    @Nullable
    public Location getLocation(String name) {
        List <Location> locations = getGatewayLocations();
        for (Location location : locations) {
            if (location.getName().equals(name)) {
                return location;
            }
        }
        return null;
    }

    public Load getLoadForLocation(@Nullable String name, TransportType transportType) {
        Location location = getLocation(name);
        if (location == null) {
            return Load.UNKNOWN;
        }
        return Load.getLoadByValue(location.getAverageLoad(transportType));
    }

    private VpnProfile getVpnProfileFromTimezoneCalculation(int nClosest, TransportType[] transportTypes, @Nullable Set<String> protocols, @Nullable String city) {
        List<Gateway> list = new ArrayList<>(gateways.values());
        if (gatewaySelector == null) {
            gatewaySelector = new GatewaySelector(list);
        }
        Gateway gateway;
        int found  = 0;
        int i = 0;
        while ((gateway = gatewaySelector.select(i)) != null) {
            for (TransportType transportType : transportTypes) {
                if ((city == null && gateway.supportsTransport(transportType, protocols)) ||
                        (gateway.getName().equals(city) && gateway.supportsTransport(transportType, protocols))) {
                    if (found == nClosest) {
                        return gateway.getProfile(transportType, protocols);
                    }
                    found++;
                }
            }
            i++;
        }
        return null;
    }

    private VpnProfile getVpnProfileFromPresortedList(int nClosest, TransportType[] transportTypes, @Nullable Set<String> protocols, @Nullable String city) {
        int found = 0;
        for (Gateway gateway : presortedList) {
            for (TransportType transportType : transportTypes) {
                if ((city == null && gateway.supportsTransport(transportType, protocols)) ||
                        (gateway.getName().equals(city) && gateway.supportsTransport(transportType, protocols))) {
                    if (found == nClosest) {
                        return gateway.getProfile(transportType, protocols);
                    }
                    found++;
                }
            }
        }
        return null;
    }

    /**
     * Get position of the gateway from a sorted set (along the distance of the gw to your time zone)
     * @param profile profile belonging to a gateway
     * @return position of the gateway owning to the profile
     */
    public int getPosition(VpnProfile profile) {
        if (presortedList.size() > 0) {
            return getPositionFromPresortedList(profile);
        }

        return getPositionFromTimezoneCalculatedList(profile);
    }

    private int getPositionFromPresortedList(VpnProfile profile) {
        int nClosestGateway = 0;
        for (Gateway gateway : presortedList) {
            if (gateway.hasProfile(profile)) {
                return nClosestGateway;
            }
            nClosestGateway++;
        }
        return -1;
    }

    private int getPositionFromTimezoneCalculatedList(VpnProfile profile) {
        if (gatewaySelector == null) {
            gatewaySelector = new GatewaySelector(new ArrayList<>(gateways.values()));
        }
        Gateway gateway;
        int nClosestGateway = 0;
        int i = 0;
        while ((gateway = gatewaySelector.select(nClosestGateway)) != null) {
            if (gateway.hasProfile(profile)) {
                return nClosestGateway;
            }
            nClosestGateway++;
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

    public void parseGatewaysV3(Provider provider) {
        try {
            JSONObject eipDefinition = provider.getEipServiceJson();
            JSONObject secrets = secretsConfigurationFromCurrentProvider();
            JSONArray gatewaysDefined = new JSONArray();
            try {
                gatewaysDefined = eipDefinition.getJSONArray(GATEWAYS);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (PreferenceHelper.useObfuscationPinning()) {
                try {
                    Transport[] transports = new Transport[]{
                            new Transport(OBFS4.toString(),
                                    new String[]{getObfuscationPinningKCP() ? "kcp" : "tcp"},
                                    new String[]{getObfuscationPinningPort()},
                                    getObfuscationPinningCert())};
                    GatewayJson.Capabilities capabilities = new GatewayJson.Capabilities(false, false, false, transports, false);
                    GatewayJson gatewayJson = new GatewayJson(context.getString(R.string.unknown_location), getObfuscationPinningIP(

                    ), null, PINNED_OBFUSCATION_PROXY, capabilities);
                    Gateway gateway = new Gateway(eipDefinition, secrets, new JSONObject(gatewayJson.toString()));
                    addGateway(gateway);
                } catch (JSONException | ConfigParser.ConfigParseError | IOException e) {
                    e.printStackTrace();
                }
            } else {
                for (int i = 0; i < gatewaysDefined.length(); i++) {
                    try {
                        JSONObject gw = gatewaysDefined.getJSONObject(i);
                        Gateway aux = new Gateway(eipDefinition, secrets, gw);
                        if (gateways.get(aux.getHost()) == null) {
                            addGateway(aux);
                        }
                    } catch (JSONException | IOException e) {
                        e.printStackTrace();
                        VpnStatus.logError("Unable to parse gateway config!");
                    } catch (ConfigParser.ConfigParseError e) {
                        VpnStatus.logError("Unable to parse gateway config: " + e.getLocalizedMessage());
                    }
                }
            }
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        }

        if (BuildConfig.BUILD_TYPE.equals("debug") && handleGatewayPinning()) {
            return;
        }

        // parse v3 menshen geoIP json variants
        if (hasSortedGatewaysWithLoad(provider)) {
            parseGatewaysWithLoad(provider);
        } else {
            parseSimpleGatewayList(provider);
        }
    }

    public void parseGatewaysV5(Provider provider) {
        ModelsGateway[] modelsGateways = provider.getGateways();
        ModelsBridge[] modelsBridges = provider.getBridges();
        ModelsEIPService modelsEIPService = provider.getService();
        JSONObject secrets = secretsConfigurationFromCurrentProvider();
        int apiVersion = provider.getApiVersion();

        if (PreferenceHelper.useObfuscationPinning()) {
            try {
                ModelsBridge modelsBridge = new ModelsBridge();
                modelsBridge.ipAddr(getObfuscationPinningIP());
                modelsBridge.port(Integer.valueOf(getObfuscationPinningPort()));
                HashMap<String, Object> options = new HashMap<>();
                options.put(CERT, getObfuscationPinningCert());
                options.put(IAT_MODE, "0");
                modelsBridge.options(options);
                modelsBridge.transport(getObfuscationPinningKCP() ? "kcp" : "tcp");
                modelsBridge.type(OBFS4.toString());
                modelsBridge.host(PINNED_OBFUSCATION_PROXY);
                Gateway gateway = new Gateway(modelsEIPService, secrets, modelsBridge, provider.getApiVersion());
                addGateway(gateway);
            } catch (NumberFormatException | ConfigParser.ConfigParseError | JSONException |
                     IOException e) {
                e.printStackTrace();
            }
        } else {
            for (ModelsGateway modelsGateway : modelsGateways) {
                String host = modelsGateway.getHost();
                Gateway gateway = gateways.get(host);
                if (gateway == null) {
                    try {
                        addGateway(new Gateway(modelsEIPService, secrets, modelsGateway, apiVersion));
                    } catch (ConfigParser.ConfigParseError | JSONException | IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    addGateway(gateway.addTransport(Transport.createTransportFrom(modelsGateway)));
                }
            }
            for (ModelsBridge modelsBridge : modelsBridges) {
                String host = modelsBridge.getHost();
                Gateway gateway = gateways.get(host);
                if (gateway == null) {
                    try {
                        addGateway(new Gateway(modelsEIPService, secrets, modelsBridge, apiVersion));
                    } catch (ConfigParser.ConfigParseError | JSONException | IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    addGateway(gateway.addTransport(Transport.createTransportFrom(modelsBridge)));
                }
            }
        }

        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            handleGatewayPinning();
        }
    }

    private void parseSimpleGatewayList(Provider provider) {
        try {
            JSONObject geoIpJson = provider.getGeoIpJson();
            JSONArray gatewaylist = geoIpJson.getJSONArray(GATEWAYS);

            for (int i = 0; i < gatewaylist.length(); i++) {
                try {
                    String key = gatewaylist.getString(i);
                    if (gateways.containsKey(key)) {
                        presortedList.add(gateways.get(key));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (NullPointerException | JSONException npe) {
            Log.d(TAG, "No valid geoip json found: " + npe.getLocalizedMessage());
        }
    }

    private boolean hasSortedGatewaysWithLoad(@Nullable Provider provider) {
        if (provider == null) {
            return false;
        }
        JSONObject geoIpJson = provider.getGeoIpJson();
        return geoIpJson.has(SORTED_GATEWAYS);
    }

    private void parseGatewaysWithLoad(Provider provider) {
        try {
            JSONObject geoIpJson = provider.getGeoIpJson();
            JSONArray gatewaylist = geoIpJson.getJSONArray(SORTED_GATEWAYS);
            for (int i = 0; i < gatewaylist.length(); i++) {
                try {
                    JSONObject load = gatewaylist.getJSONObject(i);
                    String hostName = load.getString(HOST);
                    if (gateways.containsKey(hostName)) {
                        Gateway gateway = gateways.get(hostName);
                        gateway.updateLoad(load);
                        presortedList.add(gateway);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (NullPointerException | JSONException npe) {
            npe.printStackTrace();
        }
    }

    private JSONObject secretsConfigurationFromCurrentProvider() {
        JSONObject result = new JSONObject();
        Provider provider = ProviderObservable.getInstance().getCurrentProvider();
        try {
            result.put(Provider.CA_CERT, provider.getCaCert());
            result.put(PROVIDER_VPN_CERTIFICATE, provider.getVpnCertificate());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    private void addGateway(Gateway gateway) {
        gateways.put(gateway.getHost(), gateway);
    }

    private void configureFromCurrentProvider() {
        Provider provider = ProviderObservable.getInstance().getCurrentProvider();
        if (provider == null) {
            return;
        }
        if (provider.getApiVersion() < 5) {
            parseGatewaysV3(provider);
        } else {
            parseGatewaysV5(provider);
        }
    }

    private boolean handleGatewayPinning() {
        String host = PreferenceHelper.getPinnedGateway();
        if (host == null) {
            return false;
        }
        Gateway gateway = gateways.get(host);
        gateways.clear();
        if (gateway != null) {
            gateways.put(host, gateway);
        }
        return true;
    }

}
