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

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;

import static se.leap.bitmaskclient.testutils.TestSetupHelper.getInputAsString;

/**
 * Created by cyberta on 10.01.18.
 */

public class NoErrorBackendResponseAPIv4 extends BaseBackendResponse {
    public NoErrorBackendResponseAPIv4() throws IOException {
        super();
    }

    @Override
    public Answer<String> getAnswerForRequestStringFromServer() {
        return new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                String url = (String) invocation.getArguments()[0];
                String requestMethod = (String) invocation.getArguments()[1];
                String jsonPayload = (String) invocation.getArguments()[2];

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
                } else if (url.contains("/users.json")) {
                    //create new user
                    //TODO: implement me
                } else if (url.contains("/sessions.json")) {
                    //srp auth: sendAToSRPServer
                    //TODO: implement me
                } else if (url.contains("/sessions/parmegvtest10.json")){
                    //srp auth: sendM1ToSRPServer
                    //TODO: implement me
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
                return true;
            }
        };
    }

    @Override
    public Answer<Boolean> getAnswerForDelete() {
        return new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                return true;
            }
        };
    }

}
