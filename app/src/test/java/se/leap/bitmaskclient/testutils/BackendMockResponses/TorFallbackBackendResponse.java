package se.leap.bitmaskclient.testutils.BackendMockResponses;

import static se.leap.bitmaskclient.testutils.TestSetupHelper.getInputAsString;

import android.util.Pair;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

import okhttp3.OkHttpClient;
import se.leap.bitmaskclient.providersetup.ProviderApiConnector;

public class TorFallbackBackendResponse implements ProviderApiConnector.ProviderApiConnectorInterface {
    public TorFallbackBackendResponse() throws IOException {
        super();
    }
    int requestAttempt = 0;

    @Override
    public boolean delete(OkHttpClient okHttpClient, String deleteUrl) throws RuntimeException, IOException {
        if (requestAttempt == 0) {
            requestAttempt++;
            throw new UnknownHostException("DNS blocked by censor ;)");
        }
        return true;
    }

    @Override
    public boolean canConnect(@NonNull OkHttpClient okHttpClient, String url) throws RuntimeException, IOException {
        if (requestAttempt == 0) {
            requestAttempt++;
            throw new UnknownHostException("DNS blocked by censor ;)");
        }
        return true;
    }

    @Override
    public String requestStringFromServer(@NonNull String url, @NonNull String requestMethod, String jsonString, @NonNull List<Pair<String, String>> headerArgs, @NonNull OkHttpClient okHttpClient) throws RuntimeException, IOException {
        if (url.contains("/provider.json")) {
            if (requestAttempt == 0) {
                requestAttempt++;
                throw new UnknownHostException();
            }
            //download provider json
            return getInputAsString(getClass().getClassLoader().getResourceAsStream("v4/riseup.net.json"));
        } else if (url.contains("/ca.crt")) {
            if (requestAttempt == 0) {
                requestAttempt++;
                throw new UnknownHostException("DNS blocked by censor ;)");
            }
            //download provider ca cert
            return getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.pem"));
        } else if (url.contains("config/eip-service.json")) {
            if (requestAttempt == 0) {
                requestAttempt++;
                throw new UnknownHostException("DNS blocked by censor ;)");
            }
            // download provider service json containing gateways, locations and openvpn settings
            return getInputAsString(getClass().getClassLoader().getResourceAsStream("v4/riseup.service.json"));
        } else if (url.contains(":9001/json")) {
            if (requestAttempt == 0) {
                requestAttempt++;
                throw new UnknownHostException("DNS blocked by censor ;)");
            }
            // download geoip json, containing a sorted list of gateways
            return getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.geoip.json"));
        } else if (url.contains("/cert")) {
            if (requestAttempt == 0) {
                requestAttempt++;
                throw new UnknownHostException("DNS blocked by censor ;)");
            }
            // download vpn certificate for authentication
            return getInputAsString(getClass().getClassLoader().getResourceAsStream("v4/riseup.net.cert"));

        }
        return null;
    }
}
