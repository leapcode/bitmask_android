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

import java.util.Iterator;

import se.leap.bitmaskclient.Provider;

import static se.leap.bitmaskclient.Constants.PROVIDER_PRIVATE_KEY;
import static se.leap.bitmaskclient.Constants.PROVIDER_VPN_CERTIFICATE;

public class VpnConfigGenerator {

    private JSONObject general_configuration;
    private JSONObject gateway;
    private JSONObject secrets;

    public final static String TAG = VpnConfigGenerator.class.getSimpleName();
    private final String newLine = System.getProperty("line.separator"); // Platform new line

    public VpnConfigGenerator(JSONObject general_configuration, JSONObject secrets, JSONObject gateway) {
        this.general_configuration = general_configuration;
        this.gateway = gateway;
        this.secrets = secrets;
    }

    public String generate() {
        return
                generalConfiguration()
                        + newLine
                        + gatewayConfiguration()
                        + newLine
                        + secretsConfiguration()
                        + newLine
                        + androidCustomizations();
    }

    private String generalConfiguration() {
        String commonOptions = "";
        try {
            Iterator keys = general_configuration.keys();
            while (keys.hasNext()) {
                String key = keys.next().toString();

                commonOptions += key + " ";
                for (String word : String.valueOf(general_configuration.get(key)).split(" "))
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

    private String gatewayConfiguration() {
        String remotes = "";

        String ipAddressKeyword = "ip_address";
        String remoteKeyword = "remote";
        String portsKeyword = "ports";
        String protocolKeyword = "protocols";
        String capabilitiesKeyword = "capabilities";

        try {
            String ip_address = gateway.getString(ipAddressKeyword);
            JSONObject capabilities = gateway.getJSONObject(capabilitiesKeyword);
            JSONArray ports = capabilities.getJSONArray(portsKeyword);
            for (int i = 0; i < ports.length(); i++) {
                String port_specific_remotes = "";
                int port = ports.getInt(i);
                JSONArray protocols = capabilities.getJSONArray(protocolKeyword);
                for (int j = 0; j < protocols.length(); j++) {
                    String protocol = protocols.optString(j);
                    String new_remote = remoteKeyword + " " + ip_address + " " + port + " " + protocol + newLine;

                    port_specific_remotes += new_remote;
                }
                remotes += port_specific_remotes;
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (remotes.endsWith(newLine)) {
            remotes = remotes.substring(0, remotes.lastIndexOf(newLine));
        }
        return remotes;
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
