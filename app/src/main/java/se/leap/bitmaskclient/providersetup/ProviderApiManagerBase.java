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

import static se.leap.bitmaskclient.base.utils.CertificateHelper.getFingerprintFromCertificate;

import android.content.Intent;
import android.content.res.Resources;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.utils.ConfigHelper;

/**
 * Implements the logic of the http api calls. The methods of this class needs to be called from
 * a background thread.
 */

public abstract class ProviderApiManagerBase {

    private final static String TAG = ProviderApiManagerBase.class.getName();
    public static final String PROXY_HOST = "127.0.0.1";
    public static final String SOCKS_PROXY_SCHEME = "socks5://";

    public interface ProviderApiServiceCallback {
        void broadcastEvent(Intent intent);
        boolean startTorService() throws InterruptedException, IllegalStateException, TimeoutException;
        void stopTorService();
        int getTorHttpTunnelPort();
        int getTorSocksProxyPort();
        boolean hasNetworkConnection();
        void saveProvider(Provider p);
    }

    protected final ProviderApiServiceCallback serviceCallback;

    protected Resources resources;

    protected ProviderApiEventSender eventSender;
    protected ProviderApiTorHandler torHandler;

    ProviderApiManagerBase(Resources resources, ProviderApiServiceCallback callback) {
        this.resources = resources;
        this.serviceCallback = callback;
        this.eventSender = new ProviderApiEventSender(resources, serviceCallback);
        this.torHandler = new ProviderApiTorHandler(callback);
    }

    void resetProviderDetails(Provider provider) {
        provider.reset();
    }

    protected boolean isValidJson(String jsonString) {
        try {
            new JSONObject(jsonString);
            return true;
        } catch(JSONException e) {
            return false;
        } catch(NullPointerException e) {
            e.printStackTrace();
            return false;
        }
    }

    protected boolean validCertificate(Provider provider, String certString) {
        boolean result = false;
        if (!ConfigHelper.checkErroneousDownload(certString)) {
            ArrayList<X509Certificate> certificates = ConfigHelper.parseX509CertificatesFromString(certString);
            try {
                if (certificates != null) {
                    if (certificates.size() == 1) {
                        JSONObject providerJson = provider.getDefinition();
                        String fingerprint = providerJson.getString(Provider.CA_CERT_FINGERPRINT);
                        String encoding = fingerprint.split(":")[0];
                        String expectedFingerprint = fingerprint.split(":")[1];
                        String realFingerprint = getFingerprintFromCertificate(certificates.get(0), encoding);
                        result = realFingerprint.trim().equalsIgnoreCase(expectedFingerprint.trim());
                    } else {
                        // otherwise we assume the provider is transitioning the CA certs and thus shipping multiple CA certs
                        // in that case we don't do cert pinning
                        result = true;
                    }
                } else {
                    result = false;
                }
            } catch (JSONException | NoSuchAlgorithmException | CertificateEncodingException e) {
                result = false;
            }
        }

        return result;
    }

   }
