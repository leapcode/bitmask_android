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

import org.json.*;

import java.util.*;

import se.leap.bitmaskclient.*;

public class VpnConfigGenerator {

    private JSONObject general_configuration;
    private JSONObject gateway;
    private JSONObject secrets;

    public final static String TAG = VpnConfigGenerator.class.getSimpleName();
    private final String new_line = System.getProperty("line.separator"); // Platform new line

    public VpnConfigGenerator(JSONObject general_configuration, JSONObject secrets, JSONObject gateway) {
        this.general_configuration = general_configuration;
        this.gateway = gateway;
        this.secrets = secrets;
    }

    public String generate() {
        return
                generalConfiguration()
                        + new_line
                        + gatewayConfiguration()
                        + new_line
                        + secretsConfiguration()
                        + new_line
                        + androidCustomizations();
    }

    private String generalConfiguration() {
        String common_options = "";
        try {
            Iterator keys = general_configuration.keys();
            while (keys.hasNext()) {
                String key = keys.next().toString();

                common_options += key + " ";
                for (String word : String.valueOf(general_configuration.get(key)).split(" "))
                    common_options += word + " ";
                common_options += new_line;

            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        common_options += "client";

        return common_options;
    }

    private String gatewayConfiguration() {
        String remotes = "";

        String ip_address_keyword = "ip_address";
        String remote_keyword = "remote";
        String ports_keyword = "ports";
        String protocol_keyword = "protocols";
        String capabilities_keyword = "capabilities";
        String udp = "udp";

        try {
            String ip_address = gateway.getString(ip_address_keyword);
            JSONObject capabilities = gateway.getJSONObject(capabilities_keyword);
            JSONArray ports = capabilities.getJSONArray(ports_keyword);
            for (int i = 0; i < ports.length(); i++) {
                String port_specific_remotes = "";
                int port = ports.getInt(i);
                JSONArray protocols = capabilities.getJSONArray(protocol_keyword);
                for (int j = 0; j < protocols.length(); j++) {
                    String protocol = protocols.optString(j);
                    String new_remote = remote_keyword + " " + ip_address + " " + port + " " + protocol + new_line;

                    port_specific_remotes = protocol.equalsIgnoreCase(udp) ?
                            port_specific_remotes.replaceFirst(remote_keyword, new_remote + new_line + remote_keyword) :
                            new_remote;
                }
                remotes += port_specific_remotes;
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return remotes;
    }

    private String secretsConfiguration() {
        try {
            String ca =
                    "<ca>"
                            + new_line
                            + secrets.getString(Provider.CA_CERT)
                            + new_line
                            + "</ca>";

            String key =
                    "<key>"
                            + new_line
                            + secrets.getString(Constants.PRIVATE_KEY)
                            + new_line
                            + "</key>";

            String openvpn_cert =
                    "<cert>"
                            + new_line
                            + secrets.getString(Constants.CERTIFICATE)
                            + new_line
                            + "</cert>";

            return ca + new_line + key + new_line + openvpn_cert;
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }

    private String androidCustomizations() {
        return
                "remote-cert-tls server"
                        + new_line
                        + "persist-tun"
                        + new_line
                        + "auth-retry nointeract";
    }
}
