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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import se.leap.bitmaskclient.Provider;

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
        }

        return sb.toString();
    }


    public static Provider getConfiguredProvider() throws IOException, JSONException {
        return getProvider(null, null, null);
    }

    public static Provider getProvider(String domain, String caCertFile, String jsonFile) {
        if (domain == null)
            domain = "https://riseup.net";
        if (caCertFile == null)
            caCertFile = "riseup.net.pem";
        if (jsonFile == null)
            jsonFile = "riseup.net.json";

        try {
            return new Provider(
                    new URL(domain),
                    getInputAsString(TestSetupHelper.class.getClassLoader().getResourceAsStream(caCertFile)),
                    getInputAsString(TestSetupHelper.class.getClassLoader().getResourceAsStream(jsonFile))

            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
