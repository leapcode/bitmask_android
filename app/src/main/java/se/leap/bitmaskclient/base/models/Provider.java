/**
 * Copyright (c) 2013 LEAP Encryption Access Project and contributers
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
package se.leap.bitmaskclient.base.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import static de.blinkt.openvpn.core.connection.Connection.TransportType.OBFS4;
import static se.leap.bitmaskclient.base.models.Constants.CAPABILITIES;
import static se.leap.bitmaskclient.base.models.Constants.GATEWAYS;
import static se.leap.bitmaskclient.base.models.Constants.LOCATIONS;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_ALLOWED_REGISTERED;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_ALLOW_ANONYMOUS;
import static se.leap.bitmaskclient.base.models.Constants.TRANSPORT;
import static se.leap.bitmaskclient.base.models.Constants.TYPE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.ERRORS;

/**
 * @author Sean Leonard <meanderingcode@aetherislands.net>
 * @author Parménides GV <parmegv@sdf.org>
 */
public final class Provider implements Parcelable {

    private static long EIP_SERVICE_TIMEOUT = 1000 * 60 * 60 * 24 * 3;
    private static long GEOIP_SERVICE_TIMEOUT = 1000 * 60 * 60;
    private JSONObject definition = new JSONObject(); // Represents our Provider's provider.json
    private JSONObject eipServiceJson = new JSONObject();
    private JSONObject geoIpJson = new JSONObject();
    private DefaultedURL mainUrl = new DefaultedURL();
    private DefaultedURL apiUrl = new DefaultedURL();
    private DefaultedURL geoipUrl = new DefaultedURL();
    private String providerIp = ""; // ip of the provider main url
    private String providerApiIp = ""; // ip of the provider api url
    private String certificatePin = "";
    private String certificatePinEncoding = "";
    private String caCert = "";
    private String apiVersion = "";
    private String privateKey = "";
    private String vpnCertificate = "";
    private long lastEipServiceUpdate = 0L;
    private long lastGeoIpUpdate = 0L;

    private boolean allowAnonymous;
    private boolean allowRegistered;

    final public static String
            API_URL = "api_uri",
            API_VERSION = "api_version",
            ALLOW_REGISTRATION = "allow_registration",
            API_RETURN_SERIAL = "serial",
            SERVICE = "service",
            KEY = "provider",
            CA_CERT = "ca_cert",
            CA_CERT_URI = "ca_cert_uri",
            CA_CERT_FINGERPRINT = "ca_cert_fingerprint",
            NAME = "name",
            DESCRIPTION = "description",
            DOMAIN = "domain",
            MAIN_URL = "main_url",
            PROVIDER_IP = "provider_ip",
            PROVIDER_API_IP = "provider_api_ip",
            GEOIP_URL = "geoip_url";

    private static final String API_TERM_NAME = "name";

    public Provider() { }

    public Provider(String mainUrl) {
       this(mainUrl, null);
    }

    public Provider(String mainUrl, String geoipUrl) {
        try {
            this.mainUrl.setUrl(new URL(mainUrl));
        } catch (MalformedURLException e) {
            this.mainUrl = new DefaultedURL();
        }
        setGeoipUrl(geoipUrl);
    }

    public Provider(String mainUrl, String providerIp, String providerApiIp) {
        this(mainUrl, null, providerIp, providerApiIp);
    }

    public Provider(String mainUrl, String geoipUrl, String providerIp, String providerApiIp) {
        try {
            this.mainUrl.setUrl(new URL(mainUrl));
            if (providerIp != null) {
                this.providerIp = providerIp;
            }
            if (providerApiIp != null) {
                this.providerApiIp = providerApiIp;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return;
        }
        setGeoipUrl(geoipUrl);
    }


    public Provider(String mainUrl, String geoipUrl, String providerIp, String providerApiIp, String caCert, String definition) {
        this(mainUrl, geoipUrl, providerIp, providerApiIp);
        if (caCert != null) {
            this.caCert = caCert;
        }
        if (definition != null) {
            try {
                define(new JSONObject(definition));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    public static final Parcelable.Creator<Provider> CREATOR
            = new Parcelable.Creator<Provider>() {
        public Provider createFromParcel(Parcel in) {
            return new Provider(in);
        }

        public Provider[] newArray(int size) {
            return new Provider[size];
        }
    };

    public boolean isConfigured() {
        return !mainUrl.isDefault() &&
                !apiUrl.isDefault() &&
                hasCaCert() &&
                hasDefinition() &&
                hasVpnCertificate() &&
                hasEIP() &&
                hasPrivateKey();
    }

    public boolean supportsPluggableTransports() {
        try {
            JSONArray gatewayJsons = eipServiceJson.getJSONArray(GATEWAYS);
            for (int i = 0; i < gatewayJsons.length(); i++) {
                JSONArray transports = gatewayJsons.getJSONObject(i).
                        getJSONObject(CAPABILITIES).
                        getJSONArray(TRANSPORT);
                for (int j = 0; j < transports.length(); j++) {
                    if (OBFS4.toString().equals(transports.getJSONObject(j).getString(TYPE))) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
           // ignore
        }
       return false;
    }

    public String getIpForHostname(String host) {
        if (host != null) {
            if (host.equals(mainUrl.getUrl().getHost())) {
                return providerIp;
            } else if (host.equals(apiUrl.getUrl().getHost())) {
                return providerApiIp;
            }
        }
        return "";
    }

    public String getProviderApiIp() {
        return this.providerApiIp;
    }

    public void setProviderApiIp(String providerApiIp) {
        if (providerApiIp == null) return;
        this.providerApiIp = providerApiIp;
    }

    public void setProviderIp(String providerIp) {
        if (providerIp == null) return;
        this.providerIp = providerIp;
    }

    public String getProviderIp() {
        return this.providerIp;
    }

    public void setMainUrl(URL url) {
        mainUrl.setUrl(url);
    }

    public void setMainUrl(String url) {
        try {
            mainUrl.setUrl(new URL(url));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public boolean define(JSONObject providerJson) {
        definition = providerJson;
        return parseDefinition(definition);
    }

    public JSONObject getDefinition() {
        return definition;
    }

    public String getDefinitionString() {
        return getDefinition().toString();
    }

    public String getDomain() {
        return mainUrl.getDomain();
    }

    public String getMainUrlString() {
        return getMainUrl().toString();
    }

    public DefaultedURL getMainUrl() {
        return mainUrl;
    }

    protected DefaultedURL getApiUrl() {
        return apiUrl;
    }
 
    public DefaultedURL getGeoipUrl() {
        return geoipUrl;
    }

    public void setGeoipUrl(String url) {
        try {
            this.geoipUrl.setUrl(new URL(url));
        } catch (MalformedURLException e) {
            this.geoipUrl = new DefaultedURL();
        }
    }

    public String getApiUrlWithVersion() {
        return getApiUrlString() + "/" + getApiVersion();
    }


    public String getApiUrlString() {
        return getApiUrl().toString();
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public boolean hasCaCert() {
        return caCert != null && !caCert.isEmpty();
    }

    public boolean hasDefinition() {
        return definition != null && definition.length() > 0;
    }

    public boolean hasGeoIpJson() {
        return geoIpJson != null && geoIpJson.length() > 0;
    }


    public String getCaCert() {
        return caCert;
    }

    public String getName() {
        // Should we pass the locale in, or query the system here?
        String lang = Locale.getDefault().getLanguage();
        String name = "";
        try {
            if (definition != null)
                name = definition.getJSONObject(API_TERM_NAME).getString(lang);
            else throw new JSONException("Provider not defined");
        } catch (JSONException e) {
            try {
                name = definition.getJSONObject(API_TERM_NAME).getString("en");
            } catch (JSONException e2) {
                if (mainUrl != null) {
                    String host = mainUrl.getDomain();
                    name = host.substring(0, host.indexOf("."));
                }
            }
        }

        return name;
    }

    public String getDescription() {
        String lang = Locale.getDefault().getLanguage();
        String desc = null;
        try {
            desc = definition.getJSONObject("description").getString(lang);
        } catch (JSONException e) {
            // TODO: handle exception!!
            try {
                desc = definition.getJSONObject("description").getString(definition.getString("default_language"));
            } catch (JSONException e2) {
                // TODO: i can't believe you're doing it again!
            }
        }

        return desc;
    }

    public boolean hasEIP() {
        return getEipServiceJson() != null && getEipServiceJson().length() > 0
                && !getEipServiceJson().has(ERRORS);
    }

    public boolean hasGatewaysInDifferentLocations() {
        try {
            return getEipServiceJson().getJSONObject(LOCATIONS).length() > 1;
        } catch (NullPointerException | JSONException e) {
            return false;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(getMainUrlString());
        parcel.writeString(getProviderIp());
        parcel.writeString(getProviderApiIp());
        parcel.writeString(getGeoipUrl().toString());
        parcel.writeString(getDefinitionString());
        parcel.writeString(getCaCert());
        parcel.writeString(getEipServiceJsonString());
        parcel.writeString(getGeoIpJsonString());
        parcel.writeString(getPrivateKey());
        parcel.writeString(getVpnCertificate());
        parcel.writeLong(lastEipServiceUpdate);
        parcel.writeLong(lastGeoIpUpdate);
    }


    //TODO: write a test for marshalling!
    private Provider(Parcel in) {
        try {
            mainUrl.setUrl(new URL(in.readString()));
            String tmpString = in.readString();
            if (!tmpString.isEmpty()) {
                providerIp = tmpString;
            }
            tmpString = in.readString();
            if (!tmpString.isEmpty()) {
                providerApiIp = tmpString;
            }
            tmpString = in.readString();
            if (!tmpString.isEmpty()) {
                geoipUrl.setUrl(new URL(tmpString));
            }
            tmpString = in.readString();
            if (!tmpString.isEmpty()) {
                definition = new JSONObject((tmpString));
                parseDefinition(definition);
            }
            tmpString = in.readString();
            if (!tmpString.isEmpty()) {
                this.caCert = tmpString;
            }
            tmpString = in.readString();
            if (!tmpString.isEmpty()) {
                this.setEipServiceJson(new JSONObject(tmpString));
            }
            tmpString = in.readString();
            if (!tmpString.isEmpty()) {
                this.setGeoIpJson(new JSONObject(tmpString));
            }
            tmpString = in.readString();
            if (!tmpString.isEmpty()) {
                this.setPrivateKey(tmpString);
            }
            tmpString = in.readString();
            if (!tmpString.isEmpty()) {
                this.setVpnCertificate(tmpString);
            }
            this.lastEipServiceUpdate = in.readLong();
            this.lastGeoIpUpdate = in.readLong();
        } catch (MalformedURLException | JSONException e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean equals(Object o) {
        if (o instanceof Provider) {
            Provider p = (Provider) o;
            return p.getDomain().equals(getDomain()) &&
            definition.toString().equals(p.getDefinition().toString()) &&
            eipServiceJson.toString().equals(p.getEipServiceJsonString()) &&
            geoIpJson.toString().equals(p.getGeoIpJsonString()) &&
            providerIp.equals(p.getProviderIp()) &&
            providerApiIp.equals(p.getProviderApiIp()) &&
            apiUrl.equals(p.getApiUrl()) &&
            geoipUrl.equals(p.getGeoipUrl()) &&
            certificatePin.equals(p.getCertificatePin()) &&
            certificatePinEncoding.equals(p.getCertificatePinEncoding()) &&
            caCert.equals(p.getCaCert()) &&
            apiVersion.equals(p.getApiVersion()) &&
            privateKey.equals(p.getPrivateKey()) &&
            vpnCertificate.equals(p.getVpnCertificate()) &&
            allowAnonymous == p.allowsAnonymous() &&
            allowRegistered == p.allowsRegistered();
        } else return false;
    }


    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put(Provider.MAIN_URL, mainUrl);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public int hashCode() {
        return getDomain().hashCode();
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    private boolean parseDefinition(JSONObject definition) {
        try {
            String pin =  definition.getString(CA_CERT_FINGERPRINT);
            this.certificatePin = pin.split(":")[1].trim();
            this.certificatePinEncoding = pin.split(":")[0].trim();
            this.apiUrl.setUrl(new URL(definition.getString(API_URL)));
            this.allowAnonymous = definition.getJSONObject(Provider.SERVICE).getBoolean(PROVIDER_ALLOW_ANONYMOUS);
            this.allowRegistered = definition.getJSONObject(Provider.SERVICE).getBoolean(PROVIDER_ALLOWED_REGISTERED);
            this.apiVersion = getDefinition().getString(Provider.API_VERSION);
            return true;
        } catch (JSONException | ArrayIndexOutOfBoundsException | MalformedURLException e) {
            return false;
        }
    }

    public void setCaCert(String cert) {
        this.caCert = cert;
    }

    public boolean allowsAnonymous() {
        return allowAnonymous;
    }

    public boolean allowsRegistered() {
        return allowRegistered;
    }

    public void setLastEipServiceUpdate(long timestamp) {
        lastEipServiceUpdate = timestamp;
    }

    public boolean shouldUpdateEipServiceJson() {
        return System.currentTimeMillis() - lastEipServiceUpdate >= EIP_SERVICE_TIMEOUT;
    }


    public void setLastGeoIpUpdate(long timestamp) {
        lastGeoIpUpdate = timestamp;
    }

    public boolean shouldUpdateGeoIpJson() {
        return System.currentTimeMillis() - lastGeoIpUpdate >= GEOIP_SERVICE_TIMEOUT;
    }


    public boolean setEipServiceJson(JSONObject eipServiceJson) {
        if (eipServiceJson.has(ERRORS)) {
            return false;
        }
        this.eipServiceJson = eipServiceJson;
        return true;
    }

    public boolean setGeoIpJson(JSONObject geoIpJson) {
        if (geoIpJson.has(ERRORS)) {
            return false;
        }
        this.geoIpJson = geoIpJson;
        return true;
    }

    public JSONObject getEipServiceJson() {
        return eipServiceJson;
    }

    public JSONObject getGeoIpJson() {
        return geoIpJson;
    }

    public String getGeoIpJsonString() {
        return geoIpJson.toString();
    }

    public String getEipServiceJsonString() {
        return getEipServiceJson().toString();
    }

    public boolean isDefault() {
        return getMainUrl().isDefault() &&
                getApiUrl().isDefault() &&
                getGeoipUrl().isDefault() &&
                certificatePin.isEmpty() &&
                certificatePinEncoding.isEmpty() &&
                caCert.isEmpty();
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public boolean hasPrivateKey() {
        return privateKey != null && privateKey.length() > 0;
    }

    public String getVpnCertificate() {
        return vpnCertificate;
    }

    public void setVpnCertificate(String vpnCertificate) {
        this.vpnCertificate = vpnCertificate;
    }

    public boolean hasVpnCertificate() {
        return getVpnCertificate() != null && getVpnCertificate().length() >0 ;
    }

    public String getCertificatePin() {
        return certificatePin;
    }

    public String getCertificatePinEncoding() {
        return certificatePinEncoding;
    }

    public String getCaCertFingerprint() {
        return getCertificatePinEncoding() + ":" + getCertificatePin();
    }

    /**
     * resets everything except the main url, the providerIp and the geoip
     * service url (currently preseeded)
     */
    public void reset() {
        definition = new JSONObject();
        eipServiceJson = new JSONObject();
        geoIpJson = new JSONObject();
        apiUrl = new DefaultedURL();
        certificatePin = "";
        certificatePinEncoding = "";
        caCert = "";
        apiVersion = "";
        privateKey = "";
        vpnCertificate = "";
        allowRegistered = false;
        allowAnonymous = false;
        lastGeoIpUpdate = 0L;
        lastEipServiceUpdate = 0L;
    }
}
