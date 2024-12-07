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

import static de.blinkt.openvpn.core.connection.Connection.TransportProtocol.KCP;
import static de.blinkt.openvpn.core.connection.Connection.TransportProtocol.TCP;
import static de.blinkt.openvpn.core.connection.Connection.TransportType.OBFS4;
import static de.blinkt.openvpn.core.connection.Connection.TransportType.OBFS4_HOP;
import static se.leap.bitmaskclient.base.models.Constants.CAPABILITIES;
import static se.leap.bitmaskclient.base.models.Constants.GATEWAYS;
import static se.leap.bitmaskclient.base.models.Constants.LOCATIONS;
import static se.leap.bitmaskclient.base.models.Constants.PROTOCOLS;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_ALLOWED_REGISTERED;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_ALLOW_ANONYMOUS;
import static se.leap.bitmaskclient.base.models.Constants.TRANSPORT;
import static se.leap.bitmaskclient.base.models.Constants.TYPE;
import static se.leap.bitmaskclient.base.utils.ConfigHelper.isDomainName;
import static se.leap.bitmaskclient.base.utils.ConfigHelper.isNetworkUrl;
import static se.leap.bitmaskclient.base.utils.PrivateKeyHelper.parsePrivateKeyFromString;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.ERRORS;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import de.blinkt.openvpn.core.connection.Connection.TransportProtocol;
import de.blinkt.openvpn.core.connection.Connection.TransportType;
import io.swagger.client.JSON;
import io.swagger.client.model.ModelsBridge;
import io.swagger.client.model.ModelsEIPService;
import io.swagger.client.model.ModelsGateway;
import io.swagger.client.model.ModelsProvider;
import motd.IStringCollection;
import motd.Motd;
import se.leap.bitmaskclient.BuildConfig;

/**
 * @author Sean Leonard <meanderingcode@aetherislands.net>
 * @author Parménides GV <parmegv@sdf.org>
 */
public final class Provider implements Parcelable {

    private static final long EIP_SERVICE_TIMEOUT = 1000 * 60 * 60 * 24 * 3;
    private static final long GEOIP_SERVICE_TIMEOUT = 1000 * 60 * 60;
    private static final long MOTD_TIMEOUT = 1000 * 60 * 60 * 24;
    private JSONObject definition = new JSONObject(); // Represents our Provider's provider.json
    private JSONObject eipServiceJson = new JSONObject();
    private JSONObject geoIpJson = new JSONObject();
    private JSONObject motdJson = new JSONObject();
    private String mainUrl = "";
    private String apiUrl = "";
    private String geoipUrl = "";
    private String motdUrl = "";
    private ModelsEIPService modelsEIPService = null;
    private ModelsProvider modelsProvider = null;
    private ModelsGateway[] modelsGateways = null;
    private ModelsBridge[] modelsBridges = null;
    private String domain = "";
    private String providerIp = ""; // ip of the provider main url
    private String providerApiIp = ""; // ip of the provider api url
    private String certificatePin = "";
    private String certificatePinEncoding = "";
    private String caCert = "";
    private int apiVersion = 3;
    private int[] apiVersions = new int[0];
    private String privateKeyString = "";
    private transient PrivateKey privateKey = null;
    private String vpnCertificate = "";
    private long lastEipServiceUpdate = 0L;
    private long lastGeoIpUpdate = 0L;
    private long lastMotdUpdate = 0L;
    private long lastMotdSeen = 0L;
    private Introducer introducer = null;
    private Set<String> lastMotdSeenHashes = new HashSet<>();
    private boolean shouldUpdateVpnCertificate;

    private boolean allowAnonymous;
    private boolean allowRegistered;

    final public static String
            API_URL = "api_uri",
            API_VERSION = "api_version",
            API_VERSIONS = "api_versions",
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
            GEOIP_URL = "geoip_url",
            MOTD_URL = "motd_url";

    private static final String API_TERM_NAME = "name";

    public Provider() { }

    public Provider(Introducer introducer) {
        this("https://" + introducer.getFullyQualifiedDomainName());
        this.introducer = introducer;
    }

    public Provider(String mainUrl) {
        this(mainUrl, null);
        domain = getHostFromUrl(mainUrl);
    }

    public Provider(String mainUrl, String geoipUrl) {
        setMainUrl(mainUrl);
        setGeoipUrl(geoipUrl);
        domain = getHostFromUrl(mainUrl);
    }

    public static Provider createCustomProvider(String mainUrl, String domain, Introducer introducer) {
        Provider p = new Provider(mainUrl);
        p.domain = domain;
        p.introducer = introducer;
        return p;
    }

    public Provider(String mainUrl, String geoipUrl, String motdUrl, String providerIp, String providerApiIp) {
        setMainUrl(mainUrl);
        if (providerIp != null) {
            this.providerIp = providerIp;
        }
        if (providerApiIp != null) {
            this.providerApiIp = providerApiIp;
        }
        setGeoipUrl(geoipUrl);
        setMotdUrl(motdUrl);
    }

    public Provider(String mainUrl, String geoipUrl, String motdUrl, String providerIp, String providerApiIp, String caCert, String definition) {
        this(mainUrl, geoipUrl, motdUrl, providerIp, providerApiIp);
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

    public void setBridges(String bridgesJson) {
        if (bridgesJson == null) {
            this.modelsBridges = null;
            return;
        }
        try {
            this.modelsBridges = JSON.createGson().create().fromJson(bridgesJson, ModelsBridge[].class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
    }

    public ModelsBridge[] getBridges() {
        return this.modelsBridges;
    }

    public String getBridgesJson() {
        return getJsonString(modelsBridges);
    }

    public void setGateways(String gatewaysJson) {
        if (gatewaysJson == null) {
            this.modelsGateways = null;
            return;
        }
        try {
            this.modelsGateways = JSON.createGson().create().fromJson(gatewaysJson, ModelsGateway[].class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
    }

    public ModelsGateway[] getGateways() {
        return modelsGateways;
    }

    public String getGatewaysJson() {
        return getJsonString(modelsGateways);
    }

    public void setService(String serviceJson) {
        if (serviceJson == null) {
            this.modelsEIPService = null;
            return;
        }
        try {
            this.modelsEIPService = JSON.createGson().create().fromJson(serviceJson, ModelsEIPService.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
    }
    public ModelsEIPService getService() {
        return this.modelsEIPService;
    }

    public String getServiceJson() {
        return getJsonString(modelsEIPService);
    }

    public void setModelsProvider(String json) {
        if (json == null) {
            this.modelsProvider = null;
            return;
        }
        try {
            this.modelsProvider = JSON.createGson().create().fromJson(json, ModelsProvider.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
    }

    public String getModelsProviderJson() {
        return getJsonString(modelsProvider);
    }

    private String getJsonString(Object model) {
        if (model == null) {
            return null;
        }
        try {
            return JSON.createGson().create().toJson(model);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isConfigured() {
        if (apiVersion < 5) {
            return !mainUrl.isEmpty() &&
                    !apiUrl.isEmpty() &&
                    hasCaCert() &&
                    hasDefinition() &&
                    hasVpnCertificate() &&
                    hasEIP() &&
                    hasPrivateKey();
        } else {
            return !mainUrl.isEmpty() &&
                    modelsProvider != null &&
                    modelsEIPService != null &&
                    modelsGateways != null &&
                    hasVpnCertificate() &&
                    hasPrivateKey();
        }
    }

    public boolean supportsPluggableTransports() {
        return supportsTransports(new Pair[]{new Pair<>(OBFS4, TCP), new Pair<>(OBFS4, KCP), new Pair<>(OBFS4_HOP, TCP), new Pair<>(OBFS4_HOP, KCP)});
    }

    public boolean supportsExperimentalPluggableTransports() {
        return supportsTransports(new Pair[]{new Pair<>(OBFS4, KCP),  new Pair<>(OBFS4_HOP, TCP), new Pair<>(OBFS4_HOP, KCP)});
    }


    public boolean supportsObfs4() {
        return supportsTransports(new Pair[]{new Pair<>(OBFS4, TCP)});
    }

    public boolean supportsObfs4Kcp() {
        return supportsTransports(new Pair[]{new Pair<>(OBFS4, KCP)});
    }

    public boolean supportsObfs4Hop() {
        return supportsTransports(new Pair[]{new Pair<>(OBFS4_HOP, KCP),new Pair<>(OBFS4_HOP, TCP)});
    }

    private boolean supportsTransports(Pair<TransportType, TransportProtocol>[] transportTypes) {
        if (apiVersion < 5) {
            try {
                JSONArray gatewayJsons = eipServiceJson.getJSONArray(GATEWAYS);
                for (int i = 0; i < gatewayJsons.length(); i++) {
                    JSONArray transports = gatewayJsons.getJSONObject(i).
                            getJSONObject(CAPABILITIES).
                            getJSONArray(TRANSPORT);
                    for (int j = 0; j < transports.length(); j++) {
                        String supportedTransportType = transports.getJSONObject(j).getString(TYPE);
                        JSONArray transportProtocols = transports.getJSONObject(j).getJSONArray(PROTOCOLS);
                        for (Pair<TransportType, TransportProtocol> transportPair : transportTypes) {
                            for (int k = 0; k < transportProtocols.length(); k++) {
                                if (transportPair.first.toString().equals(supportedTransportType) &&
                                        transportPair.second.toString().equals(transportProtocols.getString(k))) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // ignore
            }
        } else {
            if (modelsBridges == null) return false;
            for (ModelsBridge bridge : modelsBridges) {
                for (Pair<TransportType, TransportProtocol> transportPair : transportTypes) {
                    if (transportPair.first.toString().equals(bridge.getType()) &&
                            transportPair.second.toString().equals(bridge.getTransport())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public String getIpForHostname(String host) {
        if (host != null) {
            if (host.equals(getHostFromUrl(mainUrl))) {
                return providerIp;
            } else if (host.equals(getHostFromUrl(apiUrl))) {
                return providerApiIp;
            }
        }
        return "";
    }

    private String getHostFromUrl(String url) {
        try {
            return new URL(url).getHost();
        } catch (MalformedURLException e) {
            return "";
        }
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
        mainUrl = url.toString();
    }

    public void setMainUrl(String url) {
        try {
            if (isNetworkUrl(url)) {
                this.mainUrl = new URL(url).toString();
            } else if (isDomainName(url)){
                this.mainUrl = new URL("https://" + url).toString();
            } else {
                this.mainUrl = "";
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            this.mainUrl = "";
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
        if ((apiVersion < 5 && (domain == null || domain.isEmpty())) ||
                (modelsProvider == null)) {
            return getHostFromUrl(mainUrl);
        }
        if (apiVersion < 5) {
            return domain;
        }
        return modelsProvider.getDomain();
    }

    public String getMainUrl() {
        return mainUrl;
    }

    public String getGeoipUrl() {
        return geoipUrl;
    }

    public void setGeoipUrl(String url) {
        try {
            this.geoipUrl = new URL(url).toString();
        } catch (MalformedURLException e) {
            this.geoipUrl = "";
        }
    }

    public String getMotdUrl() {
        return this.motdUrl;
    }

    public void setMotdUrl(String url) {
        try {
            this.motdUrl = new URL(url).toString();
        } catch (MalformedURLException e) {
            this.motdUrl = "";
        }
    }

    public String getApiUrlWithVersion() {
        return getApiUrl() + "/" + getApiVersion();
    }


    public String getApiUrl() {
        return apiUrl;
    }

    public int getApiVersion() {
        return apiVersion;
    }

    public boolean hasCaCert() {
        return caCert != null && !caCert.isEmpty();
    }

    public boolean hasDefinition() {
        return (definition != null && definition.length() > 0) || (modelsProvider != null);
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
                    String host = getHostFromUrl(mainUrl);
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

    public boolean hasServiceInfo() {
        return modelsEIPService != null;
    }

    public boolean hasGatewaysInDifferentLocations() {
        if (apiVersion >= 5) {
            try {
                return getService().getLocations().size() > 1;
            } catch (NullPointerException e) {
                return false;
            }
        } else {
            try {
                return getEipServiceJson().getJSONObject(LOCATIONS).length() > 1;
            } catch (NullPointerException | JSONException e) {
                return false;
            }
        }

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(getDomain());
        parcel.writeString(getMainUrl());
        parcel.writeString(getProviderIp());
        parcel.writeString(getProviderApiIp());
        parcel.writeString(getGeoipUrl());
        parcel.writeString(getMotdUrl());
        parcel.writeString(getDefinitionString());
        parcel.writeString(getCaCert());
        parcel.writeString(getEipServiceJsonString());
        parcel.writeString(getGeoIpJsonString());
        parcel.writeString(getMotdJsonString());
        parcel.writeString(getPrivateKeyString());
        parcel.writeString(getVpnCertificate());
        parcel.writeLong(lastEipServiceUpdate);
        parcel.writeLong(lastGeoIpUpdate);
        parcel.writeLong(lastMotdUpdate);
        parcel.writeLong(lastMotdSeen);
        parcel.writeStringList(new ArrayList<>(lastMotdSeenHashes));
        parcel.writeInt(shouldUpdateVpnCertificate ? 0 : 1);
        parcel.writeParcelable(introducer, 0);
        if (apiVersion == 5) {
            Gson gson = JSON.createGson().create();
            parcel.writeString(modelsProvider != null ? gson.toJson(modelsProvider) : "");
            parcel.writeString(modelsEIPService != null ? gson.toJson(modelsEIPService) : "");
            parcel.writeString(modelsBridges != null ? gson.toJson(modelsBridges) : "");
            parcel.writeString(modelsGateways != null ? gson.toJson(modelsGateways) : "");
        }
    }


    //TODO: write a test for marshalling!
    private Provider(Parcel in) {
        try {
            domain = in.readString();
            setMainUrl(in.readString());
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
                geoipUrl = new URL(tmpString).toString();
            }
            tmpString = in.readString();
            if (!tmpString.isEmpty()) {
                motdUrl = new URL(tmpString).toString();
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
                this.setMotdJson(new JSONObject(tmpString));
            }
            tmpString = in.readString();
            if (!tmpString.isEmpty()) {
                this.setPrivateKeyString(tmpString);
            }
            tmpString = in.readString();
            if (!tmpString.isEmpty()) {
                this.setVpnCertificate(tmpString);
            }
            this.lastEipServiceUpdate = in.readLong();
            this.lastGeoIpUpdate = in.readLong();
            this.lastMotdUpdate = in.readLong();
            this.lastMotdSeen = in.readLong();
            ArrayList<String> lastMotdSeenHashes = new ArrayList<>();
            in.readStringList(lastMotdSeenHashes);
            this.lastMotdSeenHashes = new HashSet<>(lastMotdSeenHashes);
            this.shouldUpdateVpnCertificate = in.readInt()  == 0;
            this.introducer = in.readParcelable(Introducer.class.getClassLoader());
            if (this.apiVersion == 5) {
                this.setModelsProvider(in.readString());
                this.setService(in.readString());
                this.setBridges(in.readString());
                this.setGateways(in.readString());
            }
        } catch (MalformedURLException | JSONException e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean equals(Object o) {
        if (o instanceof Provider) {
            Provider p = (Provider) o;
            return getDomain().equals(p.getDomain()) &&
                    getHostFromUrl(mainUrl).equals(getHostFromUrl(p.getMainUrl())) &&
                    definition.toString().equals(p.getDefinition().toString()) &&
                    eipServiceJson.toString().equals(p.getEipServiceJsonString()) &&
                    geoIpJson.toString().equals(p.getGeoIpJsonString()) &&
                    motdJson.toString().equals(p.getMotdJsonString()) &&
                    providerIp.equals(p.getProviderIp()) &&
                    providerApiIp.equals(p.getProviderApiIp()) &&
                    apiUrl.equals(p.getApiUrl()) &&
                    geoipUrl.equals(p.getGeoipUrl()) &&
                    motdUrl.equals(p.getMotdUrl()) &&
                    certificatePin.equals(p.getCertificatePin()) &&
                    certificatePinEncoding.equals(p.getCertificatePinEncoding()) &&
                    caCert.equals(p.getCaCert()) &&
                    apiVersion == p.getApiVersion() &&
                    privateKeyString.equals(p.getPrivateKeyString()) &&
                    vpnCertificate.equals(p.getVpnCertificate()) &&
                    allowAnonymous == p.allowsAnonymous() &&
                    allowRegistered == p.allowsRegistered() &&
                    Objects.equals(modelsProvider, p.modelsProvider) &&
                    Objects.equals(modelsEIPService, p.modelsEIPService) &&
                    Arrays.equals(modelsBridges, p.modelsBridges) &&
                    Arrays.equals(modelsGateways, p.modelsGateways);
        } else return false;
    }


    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put(Provider.MAIN_URL, mainUrl);
            json.put(Provider.DOMAIN, domain);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public int hashCode() {
        return getMainUrl().hashCode();
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    private boolean parseDefinition(JSONObject definition) {
        try {
            this.apiVersions = parseApiVersionsArray();
            this.apiVersion = selectPreferredApiVersion();
            this.domain = getDefinition().getString(Provider.DOMAIN);
            String pin =  definition.getString(CA_CERT_FINGERPRINT);
            this.certificatePin = pin.split(":")[1].trim();
            this.certificatePinEncoding = pin.split(":")[0].trim();
            this.apiUrl = new URL(definition.getString(API_URL)).toString();
            JSONObject serviceJSONObject = definition.getJSONObject(Provider.SERVICE);
            if (serviceJSONObject.has(PROVIDER_ALLOW_ANONYMOUS)) {
                this.allowAnonymous = serviceJSONObject.getBoolean(PROVIDER_ALLOW_ANONYMOUS);
            }
            if (serviceJSONObject.has(PROVIDER_ALLOWED_REGISTERED)) {
                this.allowRegistered = serviceJSONObject.getBoolean(PROVIDER_ALLOWED_REGISTERED);
            }
            return true;
        } catch (JSONException | ArrayIndexOutOfBoundsException | MalformedURLException | NullPointerException | NumberFormatException e) {
            return false;
        }
    }

    /**
     @returns latest api version supported by client and server or the version set in 'api_version'
     in case there's not a common supported version
     */
    private int selectPreferredApiVersion() throws JSONException, NumberFormatException {
        if (apiVersions.length == 0) {
            return Integer.parseInt(getDefinition().getString(Provider.API_VERSION));
        }

        // apiVersion is a sorted Array
        for (int i = apiVersions.length -1; i >= 0; i--) {
            if (apiVersions[i] == BuildConfig.preferred_client_api_version ||
                    apiVersions[i] < BuildConfig.preferred_client_api_version) {
                return apiVersions[i];
            }
        }

        return Integer.parseInt(getDefinition().getString(Provider.API_VERSION));
    }

    private int[] parseApiVersionsArray() {
        int[] versionArray = new int[0];
        try {
            JSONArray versions = getDefinition().getJSONArray(Provider.API_VERSIONS);
            versionArray = new int[versions.length()];
            for (int i = 0; i < versions.length(); i++) {
                try {
                    versionArray[i] = Integer.parseInt(versions.getString(i));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException ignore) {
            // this backend doesn't support api_versions yet
        }
        Arrays.sort(versionArray);
        return versionArray;
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

    public void setShouldUpdateVpnCertificate(Boolean update) {
        shouldUpdateVpnCertificate = update;
    }

    public boolean shouldUpdateVpnCertificate() {
        return shouldUpdateVpnCertificate;
    }

    public void setLastMotdSeen(long timestamp) {
        lastMotdSeen = timestamp;
    }

    public long getLastMotdSeen() {
        return lastMotdSeen;
    }

    /**
     * shouldShowMotdSeen
     * @return true if last message of the day was shown more than 24h ago
     */
    public boolean shouldShowMotdSeen() {
        return !motdUrl.isEmpty() && System.currentTimeMillis() - lastMotdSeen >= MOTD_TIMEOUT;
    }

    /**
     * setLastSeenHashes
     * @param hashes hashes of messages of type 'once' that have already been seen
     */
    public void setMotdLastSeenHashes(Set<String> hashes) {
        lastMotdSeenHashes = hashes;
    }

    public Set<String> getMotdLastSeenHashes() {
        return lastMotdSeenHashes;
    }

    /**
     * getLastSeenHashCollection
     * @return go ffi compatible IStringCollection interface of message hashes of type 'once'
     */
    public IStringCollection getMotdLastSeenHashCollection() {
        IStringCollection stringCollection = Motd.newStringCollection();
        for (String hash : lastMotdSeenHashes) {
            stringCollection.add(hash);
        }
        return stringCollection;
    }

    public void setLastMotdUpdate(long timestamp) {
        lastMotdUpdate = timestamp;
    }

    public long getLastMotdUpdate() {
        return lastMotdUpdate;
    }

    public boolean shouldUpdateMotdJson() {
        return !motdUrl.isEmpty() && System.currentTimeMillis() - lastMotdUpdate >= MOTD_TIMEOUT;
    }

    public void setMotdJson(@NonNull JSONObject motdJson) {
        this.motdJson = motdJson;
    }

    public JSONObject getMotdJson() {
        return motdJson;
    }

    public String getMotdJsonString() {
        return motdJson.toString();
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
        return getMainUrl().isEmpty() &&
                getApiUrl().isEmpty() &&
                getGeoipUrl().isEmpty() &&
                certificatePin.isEmpty() &&
                certificatePinEncoding.isEmpty() &&
                caCert.isEmpty();
    }

    public String getPrivateKeyString() {
        return privateKeyString;
    }

    public PrivateKey getPrivateKey() {
        if (privateKey == null) {
            privateKey = parsePrivateKeyFromString(privateKeyString);
        }
        return privateKey;
    }

    public void setPrivateKeyString(String privateKeyString) {
        this.privateKeyString = privateKeyString;
    }

    public boolean hasPrivateKey() {
        return privateKeyString != null && privateKeyString.length() > 0;
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

    public boolean hasIntroducer() {
        return introducer != null;
    }

    public Introducer getIntroducer() {
        return introducer;
    }

    public void setIntroducer(String introducerUrl) throws URISyntaxException {
       this.introducer = Introducer.fromUrl(introducerUrl);
    }

    /**
     * resets everything except the main url, the providerIp and the geoip
     * service url (currently preseeded)
     */
    public void reset() {
        definition = new JSONObject();
        eipServiceJson = new JSONObject();
        geoIpJson = new JSONObject();
        motdJson = new JSONObject();
        apiUrl = "";
        certificatePin = "";
        certificatePinEncoding = "";
        caCert = "";
        apiVersion = 3;
        privateKeyString = "";
        vpnCertificate = "";
        allowRegistered = false;
        allowAnonymous = false;
        lastGeoIpUpdate = 0L;
        lastEipServiceUpdate = 0L;
        modelsProvider = null;
        modelsGateways = null;
        modelsBridges = null;
        modelsEIPService = null;
    }

}
