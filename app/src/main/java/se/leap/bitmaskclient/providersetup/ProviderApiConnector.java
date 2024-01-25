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

package se.leap.bitmaskclient.providersetup;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import android.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import javax.net.ssl.SSLHandshakeException;

import de.blinkt.openvpn.core.NativeUtils;
import de.blinkt.openvpn.core.VpnStatus;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by cyberta on 08.01.18.
 */

public class ProviderApiConnector {

    public interface ProviderApiConnectorInterface {
        boolean delete(OkHttpClient okHttpClient, String deleteUrl) throws RuntimeException, IOException;
        boolean canConnect(@NonNull OkHttpClient okHttpClient, String url) throws RuntimeException, IOException;
        String requestStringFromServer(@NonNull String url, @NonNull String requestMethod, String jsonString, @NonNull List<Pair<String, String>> headerArgs, @NonNull OkHttpClient okHttpClient) throws RuntimeException, IOException;

    }

    public static class DefaultProviderApiCpnnector implements ProviderApiConnectorInterface {

        @Override
        public boolean delete(OkHttpClient okHttpClient, String deleteUrl) {
            try {
                Request.Builder requestBuilder = new Request.Builder()
                        .url(deleteUrl)
                        .delete();
                Request request = requestBuilder.build();

                Response response = okHttpClient.newCall(request).execute();
                //response code 401: already logged out
                if (response.isSuccessful() || response.code() == 401) {
                    return true;
                }
            }  catch (IOException | RuntimeException e) {
                return false;
            }

            return false;
        }

        @Override
        public boolean canConnect(@NonNull OkHttpClient okHttpClient, String url) throws RuntimeException, IOException {
            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .method("GET", null);
            Request request = requestBuilder.build();

            Response response = okHttpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                VpnStatus.logWarning("[API] API request failed canConnect(): " + url);
            }
            return response.isSuccessful();
        }

        @Override
        public String requestStringFromServer(@NonNull String url, @NonNull String requestMethod, String jsonString, @NonNull List<Pair<String, String>> headerArgs, @NonNull OkHttpClient okHttpClient) throws RuntimeException, IOException {
            RequestBody jsonBody = jsonString != null ? RequestBody.create(JSON, jsonString) : null;
            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .method(requestMethod, jsonBody);
            for (Pair<String, String> keyValPair : headerArgs) {
                requestBuilder.addHeader(keyValPair.first, keyValPair.second);
            }

            //TODO: move to getHeaderArgs()?
            String locale = Locale.getDefault().getLanguage() + Locale.getDefault().getCountry();
            requestBuilder.addHeader("Accept-Language", locale);
            Request request = requestBuilder.build();

            Response response = okHttpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                VpnStatus.logWarning("[API] API request failed: " + url);
            }

            if (response.body() != null) {
                InputStream inputStream = response.body().byteStream();
                Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
                if (scanner.hasNext()) {
                    String result = scanner.next();
                    response.body().close();
                    return result;
                }
            }
            return null;
        }
    }
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static ProviderApiConnectorInterface instance = new DefaultProviderApiCpnnector();

    @VisibleForTesting
    public ProviderApiConnector(ProviderApiConnectorInterface connectorInterface) {
        if (!NativeUtils.isUnitTest()) {
            throw new IllegalStateException("ProviderApiConnector injected with ProviderApiConnectorInterface outside of an unit test");
        }
        instance = connectorInterface;
    }


    public static boolean delete(OkHttpClient okHttpClient, String deleteUrl) throws RuntimeException, IOException {
      return instance.delete(okHttpClient, deleteUrl);
    }

    public static boolean canConnect(@NonNull OkHttpClient okHttpClient, String url) throws RuntimeException, IOException {
       return instance.canConnect(okHttpClient, url);

    }

    public static String requestStringFromServer(@NonNull String url, @NonNull String requestMethod, String jsonString, @NonNull List<Pair<String, String>> headerArgs, @NonNull OkHttpClient okHttpClient) throws RuntimeException, IOException {
        return instance.requestStringFromServer(url, requestMethod, jsonString, headerArgs, okHttpClient);
    }
}
