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
package se.leap.bitmaskclient.testutils.BackendMockResponses;

import static se.leap.bitmaskclient.testutils.TestSetupHelper.getInputAsString;

import android.util.Pair;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.List;

import okhttp3.OkHttpClient;
import se.leap.bitmaskclient.providersetup.ProviderApiConnector;

/**
 * Created by cyberta on 10.01.18.
 */

public class NoErrorBackendResponseAPIv4 implements ProviderApiConnector.ProviderApiConnectorInterface {
    @Override
    public boolean delete(OkHttpClient okHttpClient, String deleteUrl) {
        return true;
    }

    @Override
    public boolean canConnect(@NonNull OkHttpClient okHttpClient, String url) throws RuntimeException, IOException {
        return true;
    }

    @Override
    public String requestStringFromServer(@NonNull String url, @NonNull String requestMethod, String jsonString, @NonNull List<Pair<String, String>> headerArgs, @NonNull OkHttpClient okHttpClient) throws RuntimeException, IOException {
        if (url.contains("/provider.json")) {
            //download provider json
            return getInputAsString(getClass().getClassLoader().getResourceAsStream("v4/riseup.net.json"));
        } else if (url.contains("/ca.crt")) {
            //download provider ca cert
            return getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.pem"));
        } else if (url.contains("config/eip-service.json")) {
            // download provider service json containing gateways, locations and openvpn settings
            return getInputAsString(getClass().getClassLoader().getResourceAsStream("v4/riseup.service.json"));
        } else if (url.contains(":9001/json")) {
            // download geoip json, containing a sorted list of gateways
            return getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.geoip.json"));
        } else if (url.contains("/cert")) {
            // download vpn key and cert
            return getInputAsString(getClass().getClassLoader().getResourceAsStream("v4/generictest.cert"));
        }

        return null;
    }
}
