package se.leap.bitmaskclient.testutils.BackendMockResponses;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.net.UnknownHostException;

import static se.leap.bitmaskclient.testutils.TestSetupHelper.getInputAsString;

public class TorFallbackBackendResponse extends BaseBackendResponse {
    public TorFallbackBackendResponse() throws IOException {
        super();
    }
    int requestAttempt = 0;

    @Override
    public Answer<String> getAnswerForRequestStringFromServer() {
        return new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                String url = (String) invocation.getArguments()[0];

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
        };
    }

    @Override
    public Answer<Boolean> getAnswerForCanConnect() {
        return new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                if (requestAttempt == 0) {
                    requestAttempt++;
                    throw new UnknownHostException("DNS blocked by censor ;)");
                }
                return true;
            }
        };
    }

    @Override
    public Answer<Boolean> getAnswerForDelete() {
        return new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                if (requestAttempt == 0) {
                    requestAttempt++;
                    throw new UnknownHostException("DNS blocked by censor ;)");
                }
                return true;
            }
        };
    }
}
