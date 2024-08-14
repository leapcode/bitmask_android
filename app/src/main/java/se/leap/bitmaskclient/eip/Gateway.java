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
import static se.leap.bitmaskclient.base.models.Constants.LOCATION;
import static se.leap.bitmaskclient.base.models.Constants.LOCATIONS;
import static se.leap.bitmaskclient.base.models.Constants.NAME;
import static se.leap.bitmaskclient.base.models.Constants.OPENVPN_CONFIGURATION;
import static se.leap.bitmaskclient.base.models.Constants.OVERLOAD;
import static se.leap.bitmaskclient.base.models.Constants.TIMEZONE;
import static se.leap.bitmaskclient.base.models.Constants.VERSION;
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
import java.util.Set;
import java.util.Vector;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.connection.Connection;
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
    private JSONObject load;

    // the location of a gateway is its name
    private String name;
    private int timezone;
    private int apiVersion;
    private Vector<VpnProfile> vpnProfiles;

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

        apiVersion = getApiVersion(eipDefinition);
        VpnConfigGenerator.Configuration configuration = getProfileConfig(eipDefinition, apiVersion);
        generalConfiguration = getGeneralConfiguration(eipDefinition);
        timezone = getTimezone(eipDefinition);
        name = configuration.profileName;
        vpnProfiles = createVPNProfiles(configuration);
    }

    private VpnConfigGenerator.Configuration getProfileConfig(JSONObject eipDefinition, int apiVersion) {
        VpnConfigGenerator.Configuration config = new VpnConfigGenerator.Configuration();
        config.apiVersion = apiVersion;
        config.preferUDP = getPreferUDP();
        config.experimentalTransports = allowExperimentalTransports();
        config.excludedApps = getExcludedApps();

        config.remoteGatewayIP = config.useObfuscationPinning ? getObfuscationPinningIP() : gateway.optString(IP_ADDRESS);
        config.useObfuscationPinning = useObfuscationPinning();
        config.profileName = config.useObfuscationPinning ? getObfuscationPinningGatewayLocation() : locationAsName(eipDefinition);
        if (config.useObfuscationPinning) {
            config.obfuscationProxyIP = getObfuscationPinningIP();
            config.obfuscationProxyPort = getObfuscationPinningPort();
            config.obfuscationProxyCert = getObfuscationPinningCert();
            config.obfuscationProxyKCP = getObfuscationPinningKCP();
        }
        return config;
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

    private int getTimezone(JSONObject eipDefinition) {
        JSONObject location = getLocationInfo(eipDefinition);
        return location.optInt(TIMEZONE);
    }

    private int getApiVersion(JSONObject eipDefinition) {
        return eipDefinition.optInt(VERSION);
    }

    public String getRemoteIP() {
        return gateway.optString(IP_ADDRESS);
    }

    public String getHost() {
        return gateway.optString(HOST);
    }

    private String locationAsName(JSONObject eipDefinition) {
        JSONObject location = getLocationInfo(eipDefinition);
        return location.optString(NAME);
    }

    private JSONObject getLocationInfo(JSONObject eipDefinition) {
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
        VpnConfigGenerator vpnConfigurationGenerator = new VpnConfigGenerator(generalConfiguration, secrets, gateway, profileConfig);
        Vector<VpnProfile> profiles = vpnConfigurationGenerator.generateVpnProfiles();
        return profiles;
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

    public HashSet<Connection.TransportType> getSupportedTransports() {
        HashSet<Connection.TransportType> transportTypes = new HashSet<>();
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

}
