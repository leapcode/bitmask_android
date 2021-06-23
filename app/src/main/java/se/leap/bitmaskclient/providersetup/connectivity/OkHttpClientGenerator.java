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

package se.leap.bitmaskclient.providersetup.connectivity;

import android.content.res.Resources;
import android.net.LocalSocketAddress;
import android.os.Build;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;

import static android.text.TextUtils.isEmpty;
import static se.leap.bitmaskclient.R.string.certificate_error;
import static se.leap.bitmaskclient.R.string.error_io_exception_user_message;
import static se.leap.bitmaskclient.R.string.error_no_such_algorithm_exception_user_message;
import static se.leap.bitmaskclient.R.string.keyChainAccessError;
import static se.leap.bitmaskclient.R.string.proxy;
import static se.leap.bitmaskclient.R.string.server_unreachable_message;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.ERRORS;
import static se.leap.bitmaskclient.base.utils.ConfigHelper.getProviderFormattedString;

/**
 * Created by cyberta on 08.01.18.
 */

public class OkHttpClientGenerator {

    Resources resources;
    private final static String PROXY_HOST = "127.0.0.1";

    public OkHttpClientGenerator(/*SharedPreferences preferences,*/ Resources resources) {
        this.resources = resources;
    }

    public OkHttpClient initCommercialCAHttpClient(JSONObject initError, int proxyPort) {
        return initHttpClient(initError, null, proxyPort);
    }

    public OkHttpClient initSelfSignedCAHttpClient(String caCert, int proxyPort, JSONObject initError) {
        return initHttpClient(initError, caCert, proxyPort);
    }

    public OkHttpClient init() {
        try {
            return createClient(null, -1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private OkHttpClient initHttpClient(JSONObject initError, String certificate, int proxyPort) {
        if (resources == null) {
            return null;
        }
        try {
            return createClient(certificate, proxyPort);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            // TODO ca cert is invalid - show better error ?!
            addErrorMessageToJson(initError, getProviderFormattedString(resources, certificate_error));
        } catch (IllegalStateException | KeyManagementException | KeyStoreException e) {
            e.printStackTrace();
            addErrorMessageToJson(initError, String.format(resources.getString(keyChainAccessError), e.getLocalizedMessage()));
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            e.printStackTrace();
            addErrorMessageToJson(initError, resources.getString(error_no_such_algorithm_exception_user_message));
        } catch (CertificateException e) {
            e.printStackTrace();
            // TODO ca cert is invalid - show better error ?!
            addErrorMessageToJson(initError, getProviderFormattedString(resources, certificate_error));
        } catch (UnknownHostException e) {
            e.printStackTrace();
            addErrorMessageToJson(initError, resources.getString(server_unreachable_message));
        } catch (IOException e) {
            e.printStackTrace();
            addErrorMessageToJson(initError, resources.getString(error_io_exception_user_message));
        } catch (Exception e) {
            e.printStackTrace();
            // unexpected exception, should never happen
            // only to shorten the method signature createClient(String certificate)
        }
        return null;
    }

    private OkHttpClient createClient(String certificate, int proxyPort) throws Exception {
        TLSCompatSocketFactory sslCompatFactory;
        ConnectionSpec spec = getConnectionSpec();
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

        if (!isEmpty(certificate)) {
            sslCompatFactory = new TLSCompatSocketFactory(certificate);
        } else {
            sslCompatFactory = new TLSCompatSocketFactory();
        }
        sslCompatFactory.initSSLSocketFactory(clientBuilder);
        clientBuilder.cookieJar(getCookieJar())
                .connectionSpecs(Collections.singletonList(spec));
        clientBuilder.dns(new DnsResolver());
        if (proxyPort != -1) {
            clientBuilder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXY_HOST, proxyPort)));
        }
        return clientBuilder.build();
    }



    @NonNull
    private ConnectionSpec getConnectionSpec() {
        ConnectionSpec.Builder connectionSpecbuilder = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3);
        //FIXME: restrict connection further to the following recommended cipher suites for ALL supported API levels
        //figure out how to use bcjsse for that purpose
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
            connectionSpecbuilder.cipherSuites(
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256,
                    CipherSuite.TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256
            );
        return connectionSpecbuilder.build();
    }

    @NonNull
    private CookieJar getCookieJar() {
        return new CookieJar() {
            private final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                cookieStore.put(url.host(), cookies);
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {
                List<Cookie> cookies = cookieStore.get(url.host());
                return cookies != null ? cookies : new ArrayList<Cookie>();
            }
        };
    }

    private void addErrorMessageToJson(JSONObject jsonObject, String errorMessage) {
        try {
            jsonObject.put(ERRORS, errorMessage);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
