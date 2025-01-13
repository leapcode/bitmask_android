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

import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_MODELS_BRIDGES;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_MODELS_EIPSERVICE;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_MODELS_GATEWAYS;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_MODELS_PROVIDER;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_MOTD;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_MOTD_HASHES;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_MOTD_LAST_SEEN;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_MOTD_LAST_UPDATED;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_PRIVATE_KEY;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.base.models.Provider.CA_CERT;
import static se.leap.bitmaskclient.base.models.Provider.GEOIP_URL;
import static se.leap.bitmaskclient.base.models.Provider.PROVIDER_API_IP;
import static se.leap.bitmaskclient.base.models.Provider.PROVIDER_IP;
import static se.leap.bitmaskclient.base.utils.CertificateHelper.getFingerprintFromCertificate;
import static se.leap.bitmaskclient.base.utils.ConfigHelper.getDomainFromMainURL;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getFromPersistedProvider;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getLongFromPersistedProvider;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getStringSetFromPersistedProvider;

import android.content.Intent;
import android.content.res.Resources;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.utils.ConfigHelper;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;

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

    protected void getPersistedProviderUpdates(Provider provider) {
        String providerDomain = getDomainFromMainURL(provider.getMainUrl());
        if (providerDomain == null) {
            return;
        }
        if (hasUpdatedProviderDetails(providerDomain)) {
            provider.setCaCert(getPersistedProviderCA(providerDomain));
            provider.define(getPersistedProviderDefinition(providerDomain));
            provider.setPrivateKeyString(getPersistedPrivateKey(providerDomain));
            provider.setVpnCertificate(getPersistedVPNCertificate(providerDomain));
            provider.setProviderApiIp(getPersistedProviderApiIp(providerDomain));
            provider.setProviderIp(getPersistedProviderIp(providerDomain));
            provider.setGeoipUrl(getPersistedGeoIp(providerDomain)); // TODO: do we really need to persist the Geoip URL??
            provider.setLastMotdSeen(getPersistedMotdLastSeen(providerDomain));
            provider.setMotdLastSeenHashes(getPersistedMotdHashes(providerDomain));
            provider.setLastMotdUpdate(getPersistedMotdLastUpdate(providerDomain));
            provider.setMotdJson(getPersistedMotd(providerDomain));
            provider.setModelsProvider(getFromPersistedProvider(PROVIDER_MODELS_PROVIDER, providerDomain));
            provider.setService(getFromPersistedProvider(PROVIDER_MODELS_EIPSERVICE, providerDomain));
            provider.setGateways(getFromPersistedProvider(PROVIDER_MODELS_GATEWAYS, providerDomain));
            provider.setBridges(getFromPersistedProvider(PROVIDER_MODELS_BRIDGES, providerDomain));
        }
    }

    protected String getPersistedPrivateKey(String providerDomain) {
        return getFromPersistedProvider(PROVIDER_PRIVATE_KEY, providerDomain);
    }

    protected String getPersistedVPNCertificate(String providerDomain) {
        return getFromPersistedProvider(PROVIDER_VPN_CERTIFICATE, providerDomain);
    }

    protected JSONObject getPersistedProviderDefinition(String providerDomain) {
        try {
            return new JSONObject(getFromPersistedProvider(Provider.KEY, providerDomain));
        } catch (JSONException e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

    protected String getPersistedProviderCA(String providerDomain) {
        return getFromPersistedProvider(CA_CERT, providerDomain);
    }

    protected String getPersistedProviderApiIp(String providerDomain) {
        return getFromPersistedProvider(PROVIDER_API_IP, providerDomain);
    }

    protected String getPersistedProviderIp(String providerDomain) {
        return getFromPersistedProvider(PROVIDER_IP, providerDomain);
    }

    protected String getPersistedGeoIp(String providerDomain) {
        return getFromPersistedProvider(GEOIP_URL, providerDomain);
    }

    protected JSONObject getPersistedMotd(String providerDomain) {
        try {
            return new JSONObject(getFromPersistedProvider(PROVIDER_MOTD, providerDomain));
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    protected long getPersistedMotdLastSeen(String providerDomain) {
        return getLongFromPersistedProvider(PROVIDER_MOTD_LAST_SEEN, providerDomain);
    }

    protected long getPersistedMotdLastUpdate(String providerDomain) {
        return getLongFromPersistedProvider(PROVIDER_MOTD_LAST_UPDATED, providerDomain);
    }

    protected Set<String> getPersistedMotdHashes(String providerDomain) {
        return getStringSetFromPersistedProvider(PROVIDER_MOTD_HASHES, providerDomain);
    }


    protected boolean hasUpdatedProviderDetails(String domain) {
        return PreferenceHelper.hasKey(Provider.KEY + "." + domain) && PreferenceHelper.hasKey(CA_CERT + "." + domain);
    }

   }
