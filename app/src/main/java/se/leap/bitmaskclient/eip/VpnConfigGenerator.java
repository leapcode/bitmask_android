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

import static de.blinkt.openvpn.core.connection.Connection.TransportType.OBFS4;
import static de.blinkt.openvpn.core.connection.Connection.TransportType.OBFS4_HOP;
import static de.blinkt.openvpn.core.connection.Connection.TransportType.OPENVPN;
import static se.leap.bitmaskclient.base.models.Constants.KCP;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.base.models.Constants.REMOTE;
import static se.leap.bitmaskclient.base.models.Constants.TCP;
import static se.leap.bitmaskclient.base.models.Constants.UDP;
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
import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.VpnStatus;
import de.blinkt.openvpn.core.connection.Connection.TransportType;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.models.Transport;
import se.leap.bitmaskclient.base.utils.ConfigHelper;
import se.leap.bitmaskclient.pluggableTransports.ObfsvpnClient;
import se.leap.bitmaskclient.pluggableTransports.models.Obfs4Options;

public class VpnConfigGenerator {
    private final JSONObject generalConfiguration;
    private final JSONObject secrets;
    Vector<Transport> transports = new Vector<>();
    private final int apiVersion;
    private final boolean preferUDP;
    private final boolean experimentalTransports;
    private final boolean useObfuscationPinning;
    private final String obfuscationPinningIP;
    private final String obfuscationPinningPort;
    private final String obfuscationPinningCert;
    private final boolean obfuscationPinningKCP;
    private final String remoteGatewayIP;
    private final String remoteGatewayIPv6;
    private final String profileName;
    private final Set<String> excludedApps;


    public final static String TAG = VpnConfigGenerator.class.getSimpleName();
    private final String newLine = System.getProperty("line.separator"); // Platform new line

    public static class Configuration {
        int apiVersion;
        boolean preferUDP;
        boolean experimentalTransports;
        String remoteGatewayIP = "";
        String remoteGatewayIPv6 = "";
        String profileName = "";
        Set<String> excludedApps = null;

        boolean useObfuscationPinning;
        boolean obfuscationProxyKCP;
        String obfuscationProxyIP = "";
        String obfuscationProxyPort = "";
        String obfuscationProxyCert = "";
        Vector<Transport> transports = new Vector<>();

        public static VpnConfigGenerator.Configuration createProfileConfig(Vector<Transport> transports, int apiVersion, String remoteIpAddress, String remoteIpAddressV6, String locationName) {
            VpnConfigGenerator.Configuration config = new VpnConfigGenerator.Configuration();
            config.apiVersion = apiVersion;
            config.preferUDP = getPreferUDP();
            config.experimentalTransports = allowExperimentalTransports();
            config.excludedApps = getExcludedApps();

            config.remoteGatewayIP = config.useObfuscationPinning ? getObfuscationPinningIP() : remoteIpAddress;
            config.remoteGatewayIPv6 = config.useObfuscationPinning ? null : remoteIpAddressV6;
            config.useObfuscationPinning = useObfuscationPinning();
            config.profileName = config.useObfuscationPinning ? getObfuscationPinningGatewayLocation() : locationName;
            if (config.useObfuscationPinning) {
                config.obfuscationProxyIP = getObfuscationPinningIP();
                config.obfuscationProxyPort = getObfuscationPinningPort();
                config.obfuscationProxyCert = getObfuscationPinningCert();
                config.obfuscationProxyKCP = getObfuscationPinningKCP();
            }
            config.transports = transports;
            return config;
        }
    }


    public VpnConfigGenerator(JSONObject generalConfiguration, JSONObject secrets, Configuration config) {
        this.generalConfiguration = generalConfiguration;
        this.secrets = secrets;
        this.apiVersion = config.apiVersion;
        this.preferUDP = config.preferUDP;
        this.experimentalTransports = config.experimentalTransports;
        this.useObfuscationPinning = config.useObfuscationPinning;
        this.obfuscationPinningIP = config.obfuscationProxyIP;
        this.obfuscationPinningPort = config.obfuscationProxyPort;
        this.obfuscationPinningCert = config.obfuscationProxyCert;
        this.obfuscationPinningKCP = config.obfuscationProxyKCP;
        this.remoteGatewayIP = config.remoteGatewayIP;
        this.remoteGatewayIPv6 = config.remoteGatewayIPv6;
        this.transports = config.transports;
        this.profileName = config.profileName;
        this.excludedApps = config.excludedApps;
    }

    public Vector<VpnProfile> generateVpnProfiles() throws
            ConfigParser.ConfigParseError,
            NumberFormatException {
        Vector<VpnProfile> profiles = new Vector<>();

        for (Transport transport : transports){
            if (transport.getTransportType().isPluggableTransport()) {
                Transport.Options transportOptions = transport.getOptions();
                if (!experimentalTransports && transportOptions != null && transportOptions.isExperimental()) {
                    continue;
                }
            } else if (transport.getTransportType() == OPENVPN && useObfuscationPinning) {
                continue;
            }
            try {
                profiles.add(createProfile(transport));
            } catch (ConfigParser.ConfigParseError | NumberFormatException | JSONException | IOException e) {
                e.printStackTrace();
            }
        }

        if (profiles.isEmpty()) {
            throw new ConfigParser.ConfigParseError("No supported transports detected.");
        }
        return profiles;
    }

    private String getConfigurationString(Transport transport) {
        return generalConfiguration()
                + newLine
                + gatewayConfiguration(transport)
                + newLine
                + androidCustomizations()
                + newLine
                + secretsConfiguration();
    }

    @VisibleForTesting
    protected VpnProfile createProfile(Transport transport) throws IOException, ConfigParser.ConfigParseError, JSONException {
        TransportType transportType = transport.getTransportType();
        String configuration = getConfigurationString(transport);
        ConfigParser icsOpenvpnConfigParser = new ConfigParser();
        icsOpenvpnConfigParser.parseConfig(new StringReader(configuration));
        if (transportType == OBFS4 || transportType == OBFS4_HOP) {
            icsOpenvpnConfigParser.setObfs4Options(getObfs4Options(transport));
        }

        VpnProfile profile = icsOpenvpnConfigParser.convertProfile(transportType);
        profile.mName = profileName;
        profile.mGatewayIp = remoteGatewayIP;
        if (excludedApps != null) {
            profile.mAllowedAppsVpn = new HashSet<>(excludedApps);
        }
        return profile;
    }

    private Obfs4Options getObfs4Options(Transport transport) throws JSONException, NullPointerException {
        String ip = remoteGatewayIP;
        if (useObfuscationPinning) {
            transport = new Transport(OBFS4.toString(),
                    new String[]{obfuscationPinningKCP ? KCP : TCP},
                    new String[]{obfuscationPinningPort},
                    obfuscationPinningCert);
            ip = obfuscationPinningIP;
        }
        return new Obfs4Options(ip, transport);
    }

    private String generalConfiguration() {
        String commonOptions = "";
        try {
            Iterator<String> keys = generalConfiguration.keys();
            while (keys.hasNext()) {
                String key = keys.next();

                commonOptions += key + " ";
                for (String word : String.valueOf(generalConfiguration.get(key)).split(" "))
                    commonOptions += word + " ";
                commonOptions += newLine;

            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        commonOptions += "client";

        return commonOptions;
    }

    private String gatewayConfiguration(@NonNull Transport transport) {
        String configs = "";

        StringBuilder stringBuilder = new StringBuilder();
        try {
            switch (apiVersion) {
                case 1, 2 -> gatewayConfigApiv1(transport, stringBuilder, remoteGatewayIP);
                case 3, 4, 5 -> {
                    String[] ipAddresses = (remoteGatewayIPv6 == null || remoteGatewayIPv6.isEmpty()) ?
                            new String[]{remoteGatewayIP} :
                            new String[]{remoteGatewayIPv6, remoteGatewayIP};
                    gatewayConfigMinApiv3(transport, stringBuilder, ipAddresses);
                }
            }
        } catch (JSONException | NullPointerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        configs = stringBuilder.toString();
        if (configs.endsWith(newLine)) {
            configs = configs.substring(0, configs.lastIndexOf(newLine));
        }

        return configs;
    }

    private void gatewayConfigMinApiv3(Transport transport, StringBuilder stringBuilder, String[] ipAddresses) throws JSONException {
        if (transport.getTransportType().isPluggableTransport()) {
            ptGatewayConfigMinApiv3(stringBuilder, ipAddresses, transport);
        } else {
            ovpnGatewayConfigMinApi3(stringBuilder, ipAddresses, transport);
        }
    }

    private @Nullable Transport getTransport(TransportType transportType) {
        for (Transport transport : transports) {
            if (transport.getTransportType() == transportType) {
                return transport;
            }
        }
        return null;
    }

    private void gatewayConfigApiv1(Transport transport, StringBuilder stringBuilder, String ipAddress) throws JSONException {
        if (transport == null || transport.getProtocols() == null || transport.getPorts() == null) {
            VpnStatus.logError("Misconfigured provider: missing details for transport openvpn on gateway " + ipAddress);
            return;
        }
        String[] ports = transport.getPorts();
        String[] protocols = transport.getProtocols();
        for (String port : ports) {
            for (String protocol : protocols) {
                String newRemote = REMOTE + " " + ipAddress + " " + port + " " + protocol + newLine;
                stringBuilder.append(newRemote);
            }
        }
    }

    private void ovpnGatewayConfigMinApi3(StringBuilder stringBuilder, String[] ipAddresses, @Nullable Transport transport) {
        if (transport == null || transport.getProtocols() == null || transport.getPorts() == null) {
            VpnStatus.logError("Misconfigured provider: missing details for transport openvpn on gateway " + ipAddresses[0]);
            return;
        }
        if (preferUDP) {
            StringBuilder udpRemotes = new StringBuilder();
            StringBuilder tcpRemotes = new StringBuilder();
            for (String protocol : transport.getProtocols()) {
                for (String port : transport.getPorts()) {
                    for (String ipAddress : ipAddresses) {
                        String newRemote = REMOTE + " " + ipAddress + " " + port + " " + protocol + newLine;
                        if (UDP.equals(protocol)) {
                            udpRemotes.append(newRemote);
                        } else {
                            tcpRemotes.append(newRemote);
                        }
                    }
                }
            }
            stringBuilder.append(udpRemotes.toString());
            stringBuilder.append(tcpRemotes.toString());
        } else {
            for (String protocol : transport.getProtocols()) {
                for (String port : transport.getPorts()) {
                    for (String ipAddress : ipAddresses) {
                        String newRemote = REMOTE + " " + ipAddress + " " + port + " " + protocol + newLine;
                        stringBuilder.append(newRemote);
                    }
                }
            }
        }
    }

    private boolean isAllowedProtocol(TransportType transportType, String protocol) {
        switch (transportType) {
            case OPENVPN:
                return TCP.equals(protocol) || UDP.equals(protocol);
            case OBFS4_HOP:
            case OBFS4:
                return TCP.equals(protocol) || KCP.equals(protocol);
        }
        return false;
    }

    private void ptGatewayConfigMinApiv3(StringBuilder stringBuilder, String[] ipAddresses, Transport transport) {

        //for now only use ipv4 gateway the syntax route remote_host 255.255.255.255 net_gateway is not yet working
        // https://community.openvpn.net/openvpn/ticket/1161
        /*for (String ipAddress : ipAddresses) {
            String route = "route " + ipAddress + " 255.255.255.255 net_gateway" + newLine;
            stringBuilder.append(route);
        }*/

        if (ipAddresses.length == 0) {
            return;
        }

        // check if at least one address is IPv4, IPv6 is currently not supported for obfs4
        String ipAddress = null;
        for (String address : ipAddresses) {
            if (ConfigHelper.isIPv4(address)) {
                ipAddress = address;
                break;
            }
            VpnStatus.logWarning("Skipping IP address " + address + " while configuring obfs4.");
        }

        if (ipAddress == null) {
            VpnStatus.logError("Misconfigured provider: No matching IPv4 address found to configure obfs4.");
            return;
        }

        if (!openvpnModeSupportsPt(transport, ipAddress) || !hasPTAllowedProtocol(transport, ipAddress)) {
            return;
        }

        TransportType transportType = transport.getTransportType();
        if (transportType == OBFS4 && (transport.getPorts() == null || transport.getPorts().length == 0)) {
            VpnStatus.logError("Misconfigured provider: no ports defined in " + transport.getType() + " transport JSON for gateway " + ipAddress);
            return;
        }

        if (transportType == OBFS4_HOP &&
                (transport.getOptions() == null ||
                        (transport.getOptions().getEndpoints() == null && transport.getOptions().getCert() == null) ||
                        transport.getOptions().getPortCount() == 0)) {
            VpnStatus.logError("Misconfigured provider: missing properties for transport " + transport.getType() + " on gateway " + ipAddress);
            return;
        }

        stringBuilder.append(getRouteString(ipAddress, transport));
        String transparentProxyRemote = REMOTE + " " + ObfsvpnClient.IP + " " + ObfsvpnClient.PORT + " udp" + newLine;
        stringBuilder.append(transparentProxyRemote);
    }

    // TODO: figure out if any of these configs still make sense (
    @Deprecated
    public String getExtraOptions(Transport transport) {
        if (transport.getTransportType() == OBFS4_HOP) {
            return "replay-window 65535" + newLine +
                    "ping-restart 300" + newLine +
                    "tun-mtu 48000" + newLine;
        }
        return "";
    }

    public String getRouteString(String ipAddress, Transport transport) {
        if (useObfuscationPinning) {
            return "route " + obfuscationPinningIP + " 255.255.255.255 net_gateway" + newLine;
        }
        switch (transport.getTransportType()) {
            case OBFS4:
                return "route " + ipAddress + " 255.255.255.255 net_gateway" + newLine;
            case OBFS4_HOP:
                if (transport.getOptions().getEndpoints() != null)  {
                    StringBuilder routes = new StringBuilder();
                    for (Transport.Endpoint endpoint : transport.getOptions().getEndpoints()) {
                        routes.append("route " + endpoint.getIp() + " 255.255.255.255 net_gateway" + newLine);
                    }
                    return routes.toString();
                } else {
                    return "route " + ipAddress + " 255.255.255.255 net_gateway" + newLine;
                }
        }

        return "";
    }

    // With obfsvpn 1.0.0 openvpn is always required to run in UDP to work with any obfs4 based pluggable transport.
    private boolean openvpnModeSupportsPt(Transport transport, String ipAddress) {
        if (useObfuscationPinning) {
            // we don't know if the manually pinned bridge points to a openvpn gateway with the right
            // configuration, so we assume yes
            return true;
        }
        Transport openvpnTransport = getTransport(OPENVPN);
        if (openvpnTransport == null) {
            // the bridge seems to be to be decoupled from the gateway, we can't say if the openvpn gateway
            // will support this PT and hope the admins configured the gateway correctly
            return true;
        }

        String[] protocols = openvpnTransport.getProtocols();
        if (protocols == null) {
            VpnStatus.logError("Misconfigured provider: Protocol array is missing for openvpn gateway " + ipAddress);
            return false;
        }

        String requiredProtocol = UDP;
        for (String protocol : protocols) {
            if (protocol.equals(requiredProtocol)) {
                return true;
            }
        }

        VpnStatus.logError("Provider - client incompatibility: obfuscation protocol " + transport.getTransportType().toString() + " currently only allows OpenVPN in " + requiredProtocol + " mode! Skipping config for ip " + ipAddress);
        return false;
    }

    private boolean hasPTAllowedProtocol(Transport transport, String ipAddress) {
        String[] ptProtocols = transport.getProtocols();
        for (String protocol : ptProtocols) {
            if (isAllowedProtocol(transport.getTransportType(), protocol)) {
                return true;
            } else {
                VpnStatus.logError("Provider - client incompatibility: " + protocol + " is not an allowed transport layer protocol for " + transport.getType());
            }
        }

        VpnStatus.logError("Misconfigured provider: wrong protocol defined in  " + transport.getType() + " transport JSON for gateway " + ipAddress);
        return false;
    }

    private String secretsConfiguration() {
        try {
            String ca =
                    "<ca>"
                            + newLine
                            + secrets.getString(Provider.CA_CERT)
                            + newLine
                            + "</ca>";

            String openvpnCert =
                    "<cert>"
                            + newLine
                            + secrets.getString(PROVIDER_VPN_CERTIFICATE)
                            + newLine
                            + "</cert>";

            return ca + newLine + openvpnCert;
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }

    private String androidCustomizations() {
        return
                "remote-cert-tls server"
                        + newLine
                        + "persist-tun"
                        + newLine
                        + "auth-retry nointeract";
    }
}
