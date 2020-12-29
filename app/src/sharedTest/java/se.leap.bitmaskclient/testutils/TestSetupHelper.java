/**
 * Copyright (c) 2018 LEAP Encryption Access Project and contributers
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

package se.leap.bitmaskclient.testutils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import se.leap.bitmaskclient.base.models.Provider;

/**
 * Created by cyberta on 08.10.17.
 */

public class TestSetupHelper {

    public static String getInputAsString(InputStream fileAsInputStream) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(fileAsInputStream));
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();
        while (line != null) {
            sb.append(line);
            line = br.readLine();
            if (line != null) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }


    public static Provider getConfiguredProvider() throws IOException, JSONException {
        return getProvider(null,  null, null, null, null, null, null, null);
    }

    public static Provider getProvider(String domain, String geoipUrl, String providerIp, String providerApiIp, String caCertFile, String providerJson, String eipServiceJson, String geoIpJson) {
        if (domain == null)
            domain = "https://riseup.net";
        if (geoipUrl == null)
            geoipUrl = "https://api.black.riseup.net:9001/json";
        if (providerIp == null) {
            providerIp = "";
        }
        if (providerApiIp == null) {
            providerApiIp = "";
        }
        if (caCertFile == null)
            caCertFile = "riseup.net.pem";
        if (providerJson == null)
            providerJson = "riseup.net.json";
        if (eipServiceJson == null) {
            eipServiceJson = "riseup.service.json";
        }
        if (geoIpJson == null) {
            geoIpJson = "riseup.geoip.json";
        }

        try {
            Provider p = new Provider(
                    domain,
                    geoipUrl,
                    providerIp,
                    providerApiIp,
                    getInputAsString(TestSetupHelper.class.getClassLoader().getResourceAsStream(caCertFile)),
                    getInputAsString(TestSetupHelper.class.getClassLoader().getResourceAsStream(providerJson))

            );
            JSONObject eipServiceJsonObject = new JSONObject(
                    getInputAsString(TestSetupHelper.class.getClassLoader().getResourceAsStream(eipServiceJson)));
            p.setEipServiceJson(eipServiceJsonObject);

            JSONObject geoIpJsonObject = new JSONObject(
                    getInputAsString(TestSetupHelper.class.getClassLoader().getResourceAsStream(geoIpJson)));
            p.setGeoIpJson(geoIpJsonObject);
            return p;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

}
