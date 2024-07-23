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

import static se.leap.bitmaskclient.R.string.vpn_certificate_is_invalid;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_KEY;
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
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.deleteProviderDetailsFromPreferences;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getFromPersistedProvider;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getLongFromPersistedProvider;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getStringSetFromPersistedProvider;
import static se.leap.bitmaskclient.base.utils.PrivateKeyHelper.ED_25519_KEY_BEGIN;
import static se.leap.bitmaskclient.base.utils.PrivateKeyHelper.ED_25519_KEY_END;
import static se.leap.bitmaskclient.base.utils.PrivateKeyHelper.RSA_KEY_BEGIN;
import static se.leap.bitmaskclient.base.utils.PrivateKeyHelper.RSA_KEY_END;
import static se.leap.bitmaskclient.base.utils.PrivateKeyHelper.parsePrivateKeyFromString;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.utils.ConfigHelper;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.base.utils.PrivateKeyHelper;

/**
 * Implements the logic of the http api calls. The methods of this class needs to be called from
 * a background thread.
 */

public abstract class ProviderApiManagerBase {

    private final static String TAG = ProviderApiManagerBase.class.getName();

    public interface ProviderApiServiceCallback {
        void broadcastEvent(Intent intent);
        boolean startTorService() throws InterruptedException, IllegalStateException, TimeoutException;
        void stopTorService();
        int getTorHttpTunnelPort();
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
        deleteProviderDetailsFromPreferences(provider.getDomain());
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
        String providerDomain = getDomainFromMainURL(provider.getMainUrlString());
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

    protected Bundle loadCertificate(Provider provider, String certString) {
        Bundle result = new Bundle();
        if (certString == null) {
            eventSender.setErrorResult(result, vpn_certificate_is_invalid, null);
            return result;
        }

        try {
            // API returns concatenated cert & key.  Split them for OpenVPN options
            String certificateString = null, keyString = null;
            String[] certAndKey = certString.split("(?<=-\n)");
            for (int i = 0; i < certAndKey.length - 1; i++) {
                if (certAndKey[i].contains("KEY")) {
                    keyString = certAndKey[i++] + certAndKey[i];
                } else if (certAndKey[i].contains("CERTIFICATE")) {
                    certificateString = certAndKey[i++] + certAndKey[i];
                }
            }

            PrivateKey key = parsePrivateKeyFromString(keyString);
            keyString = Base64.encodeToString(key.getEncoded(), Base64.DEFAULT);

            if (key instanceof RSAPrivateKey) {
                provider.setPrivateKeyString(RSA_KEY_BEGIN + keyString + RSA_KEY_END);
            } else {
                provider.setPrivateKeyString(ED_25519_KEY_BEGIN + keyString + ED_25519_KEY_END);
            }

            ArrayList<X509Certificate> certificates = ConfigHelper.parseX509CertificatesFromString(certificateString);
            certificates.get(0).checkValidity();
            certificateString = Base64.encodeToString(certificates.get(0).getEncoded(), Base64.DEFAULT);
            provider.setVpnCertificate( "-----BEGIN CERTIFICATE-----\n" + certificateString + "-----END CERTIFICATE-----");
            result.putBoolean(BROADCAST_RESULT_KEY, true);
        } catch (CertificateException | NullPointerException e) {
            e.printStackTrace();
            eventSender.setErrorResult(result, vpn_certificate_is_invalid, null);
        }
        return result;
    }
}
