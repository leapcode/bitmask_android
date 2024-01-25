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

import javax.net.ssl.SSLHandshakeException;

import okhttp3.OkHttpClient;
import se.leap.bitmaskclient.providersetup.ProviderApiConnector;

/**
 * Created by cyberta on 10.01.18.
 */

public class UpdatedCertificateBackendResponse implements ProviderApiConnector.ProviderApiConnectorInterface {
    static volatile boolean wasCACertCalled = false;

    @Override
    public boolean delete(OkHttpClient okHttpClient, String deleteUrl) throws RuntimeException, IOException {
        if (!wasCACertCalled) {
            throw new SSLHandshakeException("Updated certificate on server side");
        }
        return true;
    }

    @Override
    public boolean canConnect(@NonNull OkHttpClient okHttpClient, String url) throws RuntimeException, IOException {
        if (!wasCACertCalled) {
            throw new SSLHandshakeException("Updated certificate on server side");
        }
        return true;
    }

    @Override
    public String requestStringFromServer(@NonNull String url, @NonNull String requestMethod, String jsonString, @NonNull List<Pair<String, String>> headerArgs, @NonNull OkHttpClient okHttpClient) throws RuntimeException, IOException {
        if (url.contains("/provider.json")) {
            if (!wasCACertCalled) {
                throw new SSLHandshakeException("Updated certificate on server side");
            }
            //download provider json
            return getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.json"));
        } else if (url.contains("/ca.crt")) {
            //download provider ca cert
            wasCACertCalled = true;
            return getInputAsString(getClass().getClassLoader().getResourceAsStream("updated_cert.pem"));
        } else if (url.contains("config/eip-service.json")) {
            // download provider service json containing gateways, locations and openvpn settings
            if (!wasCACertCalled) {
                throw new SSLHandshakeException("Updated certificate on server side");
            }
            return getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.service.json"));
        }

        return null;
    }
}
