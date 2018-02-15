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
package se.leap.bitmaskclient;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import static se.leap.bitmaskclient.Constants.PROVIDER_ALLOWED_REGISTERED;
import static se.leap.bitmaskclient.Constants.PROVIDER_ALLOW_ANONYMOUS;
import static se.leap.bitmaskclient.ProviderAPI.ERRORS;

/**
 * @author Sean Leonard <meanderingcode@aetherislands.net>
 * @author Parménides GV <parmegv@sdf.org>
 */
public final class Provider implements Parcelable {

    private JSONObject definition = new JSONObject(); // Represents our Provider's provider.json
    private JSONObject eipServiceJson = new JSONObject();
    private DefaultedURL mainUrl = new DefaultedURL();
    private DefaultedURL apiUrl = new DefaultedURL();
    private String certificatePin = "";
    private String certificatePinEncoding = "";
    private String caCert = "";
    private String apiVersion = "";
    private String privateKey = "";
    private String vpnCertificate = "";

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
            MAIN_URL = "main_url";

    private static final String API_TERM_NAME = "name";

    public Provider() { }

    public Provider(String mainUrl) {
        try {
            this.mainUrl.setUrl(new URL(mainUrl));
        } catch (MalformedURLException e) {
            this.mainUrl = new DefaultedURL();
        }
    }

    public Provider(URL mainUrl) {
        this.mainUrl.setUrl(mainUrl);
    }

    public Provider(URL mainUrl, String caCert, String definition) {
        this.mainUrl.setUrl(mainUrl);
        if (caCert != null) {
            this.caCert = caCert;
        }
        if (definition != null) {
            try {
                this.definition = new JSONObject(definition);
                parseDefinition(this.definition);
            } catch (JSONException | NullPointerException e) {
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
       /*
        * fix against "api_uri": "https://calyx.net.malicious.url.net:4430",
        * This method aims to prevent attacks where the provider.json file got manipulated by a third party.
        * The main url should not change.
        */

        try {
            String providerApiUrl = providerJson.getString(Provider.API_URL);
            String providerDomain = providerJson.getString(Provider.DOMAIN);
            if (getMainUrlString().contains(providerDomain) && providerApiUrl.contains(providerDomain  + ":")) {
                definition = providerJson;
                parseDefinition(definition);
                return true;
            } else {
                return false;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    public JSONObject getDefinition() {
        return definition;
    }

    String getDefinitionString() {
        return getDefinition().toString();
    }

    public String getDomain() {
        return mainUrl.getDomain();
    }

    String getMainUrlString() {
        return getMainUrl().toString();
    }

    public DefaultedURL getMainUrl() {
        return mainUrl;
    }

    protected DefaultedURL getApiUrl() {
        return apiUrl;
    }

    protected String getApiUrlWithVersion() {
        return getApiUrlString() + "/" + getApiVersion();
    }


    protected String getApiUrlString() {
        return getApiUrl().toString();
    }

    public String getApiVersion() {
        return apiVersion;
    }

    boolean hasCaCert() {
        return caCert != null && !caCert.isEmpty();
    }

    public boolean hasDefinition() {
        return definition != null && definition.length() > 0;
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

    protected String getDescription() {
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

    protected boolean hasEIP() {
        return getEipServiceJson() != null && getEipServiceJson().length() > 0
                && !getEipServiceJson().has(ERRORS);
    }

    public boolean allowsRegistration() {
        try {
            return definition.getJSONObject(Provider.SERVICE).getBoolean(Provider.ALLOW_REGISTRATION);
        } catch (JSONException e) {
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
        parcel.writeString(getDefinitionString());
        parcel.writeString(getCaCert());
        parcel.writeString(getEipServiceJsonString());
        parcel.writeString(getPrivateKey());
        parcel.writeString(getVpnCertificate());
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Provider) {
            Provider p = (Provider) o;
            return p.getDomain().equals(getDomain()) &&
            definition.toString().equals(p.getDefinition().toString()) &&
            eipServiceJson.toString().equals(p.getEipServiceJson().toString())&&
            mainUrl.equals(p.getMainUrl()) &&
            apiUrl.equals(p.getApiUrl()) &&
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
            //TODO: add other fields here?
            //this is used to save custom providers as json. I guess this doesn't work correctly
            //TODO 2: verify that
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

    //TODO: write a test for marshalling!
    private Provider(Parcel in) {
        try {
            mainUrl.setUrl(new URL(in.readString()));
            String tmpString = in.readString();
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
                this.setPrivateKey(tmpString);
            }
            tmpString = in.readString();
            if (!tmpString.isEmpty()) {
                this.setVpnCertificate(tmpString);
            }
        } catch (MalformedURLException | JSONException e) {
            e.printStackTrace();
        }
    }

    private void parseDefinition(JSONObject definition) {
        try {
            String pin =  definition.getString(CA_CERT_FINGERPRINT);
            this.certificatePin = pin.split(":")[1].trim();
            this.certificatePinEncoding = pin.split(":")[0].trim();
            this.apiUrl.setUrl(new URL(definition.getString(API_URL)));
            this.allowAnonymous = definition.getJSONObject(Provider.SERVICE).getBoolean(PROVIDER_ALLOW_ANONYMOUS);
            this.allowRegistered = definition.getJSONObject(Provider.SERVICE).getBoolean(PROVIDER_ALLOWED_REGISTERED);
            this.apiVersion = getDefinition().getString(Provider.API_VERSION);
        } catch (JSONException | ArrayIndexOutOfBoundsException | MalformedURLException e) {
            e.printStackTrace();
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

    public boolean setEipServiceJson(JSONObject eipServiceJson) {
        if (eipServiceJson.has(ERRORS)) {
            return false;
        }
        this.eipServiceJson = eipServiceJson;
        return true;
    }

    public JSONObject getEipServiceJson() {
        return eipServiceJson;
    }

    public String getEipServiceJsonString() {
        return getEipServiceJson().toString();
    }
    public boolean isDefault() {
        return getMainUrl().isDefault() &&
                getApiUrl().isDefault() &&
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
     * resets everything except the main url
     */
    public void reset() {
        definition = new JSONObject();
        eipServiceJson = new JSONObject();
        apiUrl = new DefaultedURL();
        certificatePin = "";
        certificatePinEncoding = "";
        caCert = "";
        apiVersion = "";
        privateKey = "";
        vpnCertificate = "";
        allowRegistered = false;
        allowAnonymous = false;
    }

}
