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

import android.util.Pair;

import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.io.IOException;

import okhttp3.OkHttpClient;
import se.leap.bitmaskclient.providersetup.ProviderApiConnector;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * Created by cyberta on 10.01.18.
 */

public abstract class BaseBackendResponse {

    private Answer<String> answerRequestStringFromServer;
    private Answer<Boolean> answerCanConnect;
    private Answer<Boolean> answerDelete;

    public BaseBackendResponse() throws IOException {
        mockStatic(ProviderApiConnector.class);
        this.answerRequestStringFromServer = getAnswerForRequestStringFromServer();
        this.answerCanConnect = getAnswerForCanConnect();
        this.answerDelete = getAnswerForDelete();

        responseOnRequestStringFromServer();
        responseOnCanConnect();
        responseOnDelete();

    }

    public abstract Answer<String> getAnswerForRequestStringFromServer();
    public abstract Answer<Boolean> getAnswerForCanConnect();
    public abstract Answer<Boolean> getAnswerForDelete();


    public void responseOnRequestStringFromServer() throws IOException, RuntimeException {
        Mockito.when(ProviderApiConnector.requestStringFromServer(anyString(), anyString(), nullable(String.class), ArgumentMatchers.<Pair<String,String>>anyList(), any(OkHttpClient.class))).
                thenAnswer(answerRequestStringFromServer);
    }

    public void responseOnCanConnect() throws IOException, RuntimeException {
        Mockito.when(ProviderApiConnector.canConnect(any(OkHttpClient.class), anyString())).thenAnswer(answerCanConnect);
    }

    public void responseOnDelete() throws IOException, RuntimeException {
        Mockito.when(ProviderApiConnector.delete(any(OkHttpClient.class), anyString())).thenAnswer(answerDelete);
    }

}
