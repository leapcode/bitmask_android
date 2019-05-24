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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.connection.Connection;
import de.blinkt.openvpn.core.connection.Obfs4Connection;
import se.leap.bitmaskclient.Provider;

import static se.leap.bitmaskclient.Constants.CAPABILITIES;
import static se.leap.bitmaskclient.Constants.IP_ADDRESS;
import static se.leap.bitmaskclient.Constants.OPTIONS;
import static se.leap.bitmaskclient.Constants.PORTS;
import static se.leap.bitmaskclient.Constants.PROTOCOLS;
import static se.leap.bitmaskclient.Constants.PROVIDER_PRIVATE_KEY;
import static se.leap.bitmaskclient.Constants.PROVIDER_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.Constants.REMOTE;
import static se.leap.bitmaskclient.Constants.TRANSPORT;
import static se.leap.bitmaskclient.Constants.TYPE;

public class VpnConfigGenerator {

    private JSONObject generalConfiguration;
    private JSONObject gateway;
    private JSONObject secrets;
    private JSONObject obfs4Transport;
    private int apiVersion;

    private ConfigParser icsOpenvpnConfigParser = new ConfigParser();


    public final static String TAG = VpnConfigGenerator.class.getSimpleName();
    private final String newLine = System.getProperty("line.separator"); // Platform new line

    public VpnConfigGenerator(JSONObject generalConfiguration, JSONObject secrets, JSONObject gateway, int apiVersion) {
        this.generalConfiguration = generalConfiguration;
        this.gateway = gateway;
        this.secrets = secrets;
        this.apiVersion = apiVersion;
        checkCapabilities();
    }

    public void checkCapabilities() {

        try {
            switch (apiVersion) {
                case 2:
                    JSONArray supportedTransports = gateway.getJSONObject(CAPABILITIES).getJSONArray(TRANSPORT);
                    for (int i = 0; i < supportedTransports.length(); i++) {
                        JSONObject transport = supportedTransports.getJSONObject(i);
                        if (transport.getString(TYPE).equals("obfs4")) {
                            obfs4Transport = transport;
                        }
                    }
                    break;
                default:
                    break;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public VpnProfile generateVpnProfile() throws IllegalStateException,
            IOException,
            ConfigParser.ConfigParseError,
            CloneNotSupportedException,
            JSONException,
            NumberFormatException {

        VpnProfile profile = createOvpnProfile();
        if (supportsObfs4()) {
            addPluggableTransportConnections(profile);
        }
        return profile;
    }

    private boolean supportsObfs4(){
        return obfs4Transport != null;
    }

    private void addPluggableTransportConnections(VpnProfile profile) throws JSONException, CloneNotSupportedException {
        JSONArray ports = obfs4Transport.getJSONArray(PORTS);
        Connection[] updatedConnections = new Connection[profile.mConnections.length + ports.length()];

        for (int i = 0; i < ports.length(); i++) {
            String port = ports.getString(i);
            Obfs4Connection obfs4Connection = new Obfs4Connection();
            obfs4Connection.setObfs4RemoteProxyName(gateway.getString(IP_ADDRESS));
            obfs4Connection.setObfs4RemoteProxyPort(port);
            obfs4Connection.setTransportOptions(obfs4Transport.optJSONObject(OPTIONS));
            updatedConnections[i] = obfs4Connection;
        }
        int k = 0;
        for (int i = ports.length(); i < updatedConnections.length; i++, k++) {
            updatedConnections[i] = profile.mConnections[k].clone();
        }
        profile.mConnections = updatedConnections;
    }

    private String getConfigurationString() {
        return generalConfiguration()
                        + newLine
                        + ovpnGatewayConfiguration()
                        + newLine
                        + secretsConfiguration()
                        + newLine
                        + androidCustomizations();
    }

    private VpnProfile createOvpnProfile() throws IOException, ConfigParser.ConfigParseError {
        String configuration = getConfigurationString();
        icsOpenvpnConfigParser.parseConfig(new StringReader(configuration));
        return icsOpenvpnConfigParser.convertProfile();
    }

    private String generalConfiguration() {
        String commonOptions = "";
        try {
            Iterator keys = generalConfiguration.keys();
            while (keys.hasNext()) {
                String key = keys.next().toString();

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

    private String ovpnGatewayConfiguration() {
        String remotes = "";

        StringBuilder stringBuilder = new StringBuilder();
        try {
            String ipAddress = gateway.getString(IP_ADDRESS);
            JSONObject capabilities = gateway.getJSONObject(CAPABILITIES);
            JSONArray transports = capabilities.getJSONArray(TRANSPORT);
            switch (apiVersion) {
                default:
                case 1:
                    ovpnGatewayConfigApiv1(stringBuilder, ipAddress, capabilities);
                    break;
                case 2:
                    ovpnGatewayConfigApiv2(stringBuilder, ipAddress, transports);
                    break;
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        remotes = stringBuilder.toString();
        if (remotes.endsWith(newLine)) {
            remotes = remotes.substring(0, remotes.lastIndexOf(newLine));
        }
        return remotes;
    }

    private void ovpnGatewayConfigApiv1(StringBuilder stringBuilder, String ipAddress, JSONObject capabilities) throws JSONException {
        int port;
        String protocol;

        JSONArray ports = capabilities.getJSONArray(PORTS);
        for (int i = 0; i < ports.length(); i++) {
            port = ports.getInt(i);
            JSONArray protocols = capabilities.getJSONArray(PROTOCOLS);
            for (int j = 0; j < protocols.length(); j++) {
                protocol = protocols.optString(j);
                String newRemote = REMOTE + " " + ipAddress + " " + port + " " + protocol + newLine;
                stringBuilder.append(newRemote);
            }
        }
    }

    private void ovpnGatewayConfigApiv2(StringBuilder stringBuilder, String ipAddress, JSONArray transports) throws JSONException {
        String port;
        String protocol;
        for (int i = 0; i < transports.length(); i++) {
            JSONObject transport = transports.getJSONObject(i);
            if (!transport.getString(TYPE).equals("openvpn")) {
                continue;
            }
            JSONArray ports = transport.getJSONArray(PORTS);
            for (int j = 0; j < ports.length(); j++) {
                port = ports.getString(j);
                JSONArray protocols = transport.getJSONArray(PROTOCOLS);
                for (int k = 0; k < protocols.length(); k++) {
                    protocol = protocols.optString(k);
                    String newRemote = REMOTE + " " + ipAddress + " " + port + " " + protocol + newLine;
                    stringBuilder.append(newRemote);
                }
            }
        }
    }


    private String secretsConfiguration() {
        try {
            String ca =
                    "<ca>"
                            + newLine
                            + secrets.getString(Provider.CA_CERT)
                            + newLine
                            + "</ca>";

            String key =
                    "<key>"
                            + newLine
                            + secrets.getString(PROVIDER_PRIVATE_KEY)
                            + newLine
                            + "</key>";

            String openvpnCert =
                    "<cert>"
                            + newLine
                            + secrets.getString(PROVIDER_VPN_CERTIFICATE)
                            + newLine
                            + "</cert>";

            return ca + newLine + key + newLine + openvpnCert;
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
