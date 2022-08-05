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

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.connection.Connection;
import se.leap.bitmaskclient.R;
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
    private HashMap<Connection.TransportType, VpnProfile> vpnProfiles;

    /**
     * Build a gateway object from a JSON OpenVPN gateway definition in eip-service.json
     * and create a VpnProfile belonging to it.
     */
    public Gateway(JSONObject eipDefinition, JSONObject secrets, JSONObject gateway, Context context)
            throws ConfigParser.ConfigParseError, JSONException, IOException {
        this(eipDefinition, secrets, gateway, null, context);
    }

    public Gateway(JSONObject eipDefinition, JSONObject secrets, JSONObject gateway, JSONObject load, Context context)
            throws ConfigParser.ConfigParseError, JSONException, IOException {

        this.gateway = gateway;
        this.secrets = secrets;
        this.load = load;

        apiVersion = getApiVersion(eipDefinition);
        VpnConfigGenerator.Configuration configuration = getProfileConfig(context, eipDefinition, apiVersion);
        generalConfiguration = getGeneralConfiguration(eipDefinition);
        timezone = getTimezone(eipDefinition);
        name = configuration.profileName;
        vpnProfiles = createVPNProfiles(configuration);
    }

    private VpnConfigGenerator.Configuration getProfileConfig(Context context, JSONObject eipDefinition, int apiVersion) {
        VpnConfigGenerator.Configuration config = new VpnConfigGenerator.Configuration();
        config.apiVersion = apiVersion;
        config.preferUDP = getPreferUDP(context);
        config.experimentalTransports = allowExperimentalTransports(context);
        config.excludedApps = getExcludedApps(context);

        config.remoteGatewayIP = config.useObfuscationPinning ? getObfuscationPinningIP(context) : gateway.optString(IP_ADDRESS);
        config.useObfuscationPinning = useObfuscationPinning(context);
        config.profileName = config.useObfuscationPinning ? getObfuscationPinningGatewayLocation(context) : locationAsName(eipDefinition);
        if (config.useObfuscationPinning) {
            config.obfuscationProxyIP = getObfuscationPinningIP(context);
            config.obfuscationProxyPort = getObfuscationPinningPort(context);
            config.obfuscationProxyCert = getObfuscationPinningCert(context);
            config.obfuscationProxyKCP = getObfuscationPinningKCP(context);
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
    private @NonNull HashMap<Connection.TransportType, VpnProfile> createVPNProfiles(VpnConfigGenerator.Configuration profileConfig)
            throws ConfigParser.ConfigParseError, IOException, JSONException {
        VpnConfigGenerator vpnConfigurationGenerator = new VpnConfigGenerator(generalConfiguration, secrets, gateway, profileConfig);
        HashMap<Connection.TransportType, VpnProfile> profiles = vpnConfigurationGenerator.generateVpnProfiles();
        return profiles;
    }

    public String getName() {
        return name;
    }

    public HashMap<Connection.TransportType, VpnProfile> getProfiles() {
        return vpnProfiles;
    }

    public VpnProfile getProfile(Connection.TransportType transportType) {
        return vpnProfiles.get(transportType);
    }

    public boolean supportsTransport(Connection.TransportType transportType) {
        if (transportType == Connection.TransportType.PT) {
            return supportsPluggableTransports();
        }
        return vpnProfiles.get(transportType) != null;
    }

    public HashSet<Connection.TransportType> getSupportedTransports() {
        return new HashSet<>(vpnProfiles.keySet());
    }

    public boolean supportsPluggableTransports() {
        for (Connection.TransportType transportType : vpnProfiles.keySet()) {
            if (transportType.isPluggableTransport() && vpnProfiles.get(transportType) != null) {
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
