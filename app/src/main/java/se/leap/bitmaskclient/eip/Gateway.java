/**
 * Copyright (c) 2013 LEAP Encryption Access Project and contributers
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

import static de.blinkt.openvpn.core.connection.Connection.TransportType.PT;
import static se.leap.bitmaskclient.base.models.Constants.FULLNESS;
import static se.leap.bitmaskclient.base.models.Constants.HOST;
import static se.leap.bitmaskclient.base.models.Constants.IP_ADDRESS;
import static se.leap.bitmaskclient.base.models.Constants.IP_ADDRESS6;
import static se.leap.bitmaskclient.base.models.Constants.LOCATION;
import static se.leap.bitmaskclient.base.models.Constants.LOCATIONS;
import static se.leap.bitmaskclient.base.models.Constants.NAME;
import static se.leap.bitmaskclient.base.models.Constants.OPENVPN_CONFIGURATION;
import static se.leap.bitmaskclient.base.models.Constants.OVERLOAD;
import static se.leap.bitmaskclient.base.models.Constants.TIMEZONE;
import static se.leap.bitmaskclient.base.models.Constants.VERSION;
import static se.leap.bitmaskclient.base.models.Transport.createTransportsFrom;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.allowExperimentalTransports;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getExcludedApps;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getObfuscationPinningCert;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getObfuscationPinningGatewayLocation;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getObfuscationPinningIP;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getObfuscationPinningKCP;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getObfuscationPinningPort;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getPreferUDP;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.useObfuscationPinning;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.connection.Connection;
import io.swagger.client.model.ModelsBridge;
import io.swagger.client.model.ModelsEIPService;
import io.swagger.client.model.ModelsGateway;
import io.swagger.client.model.ModelsLocation;
import se.leap.bitmaskclient.base.models.Transport;
import se.leap.bitmaskclient.base.utils.ConfigHelper;

/**
 * Gateway provides objects defining gateways and their metadata.
 * Each instance contains a VpnProfile for OpenVPN specific data and member
 * variables describing capabilities and location (name)
 *
 * @author Sean Leonard <meanderingcode@aetherislands.net>
 * @author Parménides GV <parmegv@sdf.org>
 * @author cyberta
 */
public class Gateway {

    public final static String TAG = Gateway.class.getSimpleName();

    private JSONObject generalConfiguration;
    private JSONObject secrets;
    private JSONObject gateway;
    private Vector<ModelsGateway> modelsGateways;
    private Vector<ModelsBridge> modelsBridges;
    private JSONObject load;

    // the location of a gateway is its name
    private String name;
    private int timezone;
    private int apiVersion;
    private Vector<VpnProfile> vpnProfiles;
    private String remoteIpAddress;
    private String remoteIpAddressV6;
    private String host;
    private String locationName;

    /**
     * Build a gateway object from a JSON OpenVPN gateway definition in eip-service.json
     * and create a VpnProfile belonging to it.
     */
    public Gateway(JSONObject eipDefinition, JSONObject secrets, JSONObject gateway)
            throws ConfigParser.ConfigParseError, JSONException, IOException {
        this(eipDefinition, secrets, gateway, null);
    }

    public Gateway(JSONObject eipDefinition, JSONObject secrets, JSONObject gateway, JSONObject load)
            throws ConfigParser.ConfigParseError, JSONException, IOException {

        this.gateway = gateway;
        this.secrets = secrets;
        this.load = load;
        this.apiVersion = eipDefinition.optInt(VERSION);
        this.remoteIpAddress = gateway.optString(IP_ADDRESS);
        this.remoteIpAddressV6 = gateway.optString(IP_ADDRESS6);
        this.host = gateway.optString(HOST);
        JSONObject location = getLocationInfo(gateway, eipDefinition);
        this.locationName = location.optString(NAME);
        this.timezone = location.optInt(TIMEZONE);
        VpnConfigGenerator.Configuration configuration = getProfileConfig(Transport.createTransportsFrom(gateway, apiVersion));
        this.generalConfiguration = getGeneralConfiguration(eipDefinition);
        this.name = configuration.profileName;
        this.vpnProfiles = createVPNProfiles(configuration);
    }


    public Gateway(ModelsEIPService eipService, JSONObject secrets, ModelsGateway modelsGateway, int apiVersion) throws ConfigParser.ConfigParseError, NumberFormatException, JSONException, IOException {
        this.apiVersion = apiVersion;
        generalConfiguration = getGeneralConfiguration(eipService);
        this.secrets = secrets;
        this.modelsGateways = new Vector<>();
        this.modelsBridges = new Vector<>();
        this.modelsGateways.add(modelsGateway);

        this.remoteIpAddress = modelsGateway.getIpAddr();
        this.remoteIpAddressV6 = modelsGateway.getIp6Addr();
        this.host = modelsGateway.getHost();
        ModelsLocation modelsLocation = eipService.getLocations().get(modelsGateway.getLocation());
        if (modelsLocation != null) {
            this.locationName = modelsLocation.getDisplayName();
            this.timezone = Integer.parseInt(modelsLocation.getTimezone());
        } else {
            this.locationName = modelsGateway.getLocation();
        }
        this.apiVersion = apiVersion;
        VpnConfigGenerator.Configuration configuration = getProfileConfig(createTransportsFrom(modelsGateway));
        this.name = configuration.profileName;
        this.vpnProfiles = createVPNProfiles(configuration);
    }

    public Gateway(ModelsEIPService eipService, JSONObject secrets, ModelsBridge modelsBridge, int apiVersion) throws ConfigParser.ConfigParseError, JSONException, IOException {
        this.apiVersion = apiVersion;
        generalConfiguration = getGeneralConfiguration(eipService);
        this.secrets = secrets;
        this.modelsGateways = new Vector<>();
        this.modelsBridges = new Vector<>();
        this.modelsBridges.add(modelsBridge);
        remoteIpAddress = modelsBridge.getIpAddr();
        host = modelsBridge.getHost();
        ModelsLocation modelsLocation = eipService.getLocations().get(modelsBridge.getLocation());
        if (modelsLocation != null) {
            this.locationName = modelsLocation.getDisplayName();
            this.timezone = Integer.parseInt(modelsLocation.getTimezone());
        } else {
            this.locationName = modelsBridge.getLocation();
        }        this.apiVersion = apiVersion;
        VpnConfigGenerator.Configuration configuration = getProfileConfig(Transport.createTransportsFrom(modelsBridge));
        name = configuration.profileName;
        vpnProfiles = createVPNProfiles(configuration);
    }



    private VpnConfigGenerator.Configuration getProfileConfig(Vector<Transport> transports) {
        return VpnConfigGenerator.Configuration.createProfileConfig(transports, apiVersion, remoteIpAddress, remoteIpAddressV6, locationName);
    }

    public void updateLoad(JSONObject load) {
        this.load = load;
    }

    private JSONObject getGeneralConfiguration(JSONObject eipDefinition) {
        try {
            return eipDefinition.getJSONObject(OPENVPN_CONFIGURATION);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    private JSONObject getGeneralConfiguration(ModelsEIPService eipService) {
        JSONObject config = new JSONObject();
        Map<String, Object> openvpnOptions =  eipService.getOpenvpnConfiguration();
        Set<String> keys = openvpnOptions.keySet();
        Iterator<String> i = keys.iterator();
        while (i.hasNext()) {
            try {
                String key = i.next();
                Object o = openvpnOptions.get(key);
                config.put(key, o);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return config;
    }

    public String getRemoteIP() {
        return remoteIpAddress;
    }

    public String getHost() {
        return host;
    }

    private JSONObject getLocationInfo(JSONObject gateway, JSONObject eipDefinition) {
        try {
            JSONObject locations = eipDefinition.getJSONObject(LOCATIONS);

            return locations.getJSONObject(gateway.getString(LOCATION));
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    public boolean hasLoadInfo() {
        return load != null;
    }

    public double getFullness() {
        try {
            return load.getDouble(FULLNESS);
        } catch (JSONException | NullPointerException e) {
            return ConfigHelper.getConnectionQualityFromTimezoneDistance(timezone);
        }
    }

    public boolean isOverloaded() {
        try {
            return load.getBoolean(OVERLOAD);
        } catch (JSONException | NullPointerException e) {
            return false;
        }
    }

    /**
     * Create and attach the VpnProfile to our gateway object
     */
    private @NonNull Vector<VpnProfile> createVPNProfiles(VpnConfigGenerator.Configuration profileConfig)
            throws ConfigParser.ConfigParseError, IOException, JSONException {
        VpnConfigGenerator vpnConfigurationGenerator = new VpnConfigGenerator(generalConfiguration, secrets, profileConfig);
        return vpnConfigurationGenerator.generateVpnProfiles();
    }

    public String getName() {
        return name;
    }

    public Vector<VpnProfile> getProfiles() {
        return vpnProfiles;
    }

    /**
     * Returns a VpnProfile that supports a given transport type and any of the given transport
     * layer protocols (e.g. TCP, KCP). If multiple VpnProfiles fulfill these requirements, a random
     * profile will be chosen. This can currently only occur for obfuscation protocols.
     * @param transportType transport type, e.g. openvpn or obfs4
     * @param obfuscationTransportLayerProtocols Vector of transport layer protocols PTs can be based on
     * @return
     */
    public @Nullable VpnProfile getProfile(Connection.TransportType transportType, @Nullable Set<String> obfuscationTransportLayerProtocols) {
        Vector<VpnProfile> results = new Vector<>();
        for (VpnProfile vpnProfile : vpnProfiles) {
            if (vpnProfile.getTransportType() == transportType) {
                if (!vpnProfile.usePluggableTransports() ||
                        obfuscationTransportLayerProtocols == null ||
                        obfuscationTransportLayerProtocols.contains(vpnProfile.getObfuscationTransportLayerProtocol())) {
                    results.add(vpnProfile);
                }
            }
        }
        if (results.size() == 0) {
            return null;
        }
        int randomIndex = (int) (Math.random() * (results.size()));
        return results.get(randomIndex);
    }

    public boolean hasProfile(VpnProfile profile) {
        return vpnProfiles.contains(profile);
    }

    /**
     * Checks if a transport type is supported by the gateway.
     * In case the transport type is an obfuscation transport, you can pass a Vector of required transport layer protocols.
     * This way you can filter for TCP based obfs4 traffic versus KCP based obfs4 traffic.
     * @param transportType transport type, e.g. openvpn or obfs4
     * @param obfuscationTransportLayerProtocols filters for _any_ of these transport layer protocols (e.g. TCP or KCP) of a given obfuscation transportType, can be omitted if transportType is OPENVPN.
     *
     * @return
     */
    public boolean supportsTransport(Connection.TransportType transportType, @Nullable Set<String> obfuscationTransportLayerProtocols) {
        if (transportType == PT) {
            return supportsPluggableTransports();
        }
        return getProfile(transportType, obfuscationTransportLayerProtocols) != null;
    }

    public Set<Connection.TransportType> getSupportedTransports() {
        Set<Connection.TransportType> transportTypes = new HashSet<>();
        for (VpnProfile p : vpnProfiles) {
            transportTypes.add(p.getTransportType());
        }
        return transportTypes;
    }

    public boolean supportsPluggableTransports() {
        for (VpnProfile profile : vpnProfiles) {
            Connection.TransportType transportType = profile.getTransportType();
            if (transportType.isPluggableTransport()) {
                return true;
            }
        }
        return false;
    }

    public int getTimezone() {
        return timezone;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this, Gateway.class);
    }

    public Gateway addTransport(Transport transport) {
        Vector<Transport> transports = new Vector<>();
        transports.add(transport);
        VpnConfigGenerator.Configuration profileConfig = getProfileConfig(transports);
        try {
            Vector<VpnProfile> profiles = createVPNProfiles(profileConfig);
            vpnProfiles.addAll(profiles);
        } catch (ConfigParser.ConfigParseError | IOException | JSONException e) {
            e.printStackTrace();
        }
        return this;
    }
}
