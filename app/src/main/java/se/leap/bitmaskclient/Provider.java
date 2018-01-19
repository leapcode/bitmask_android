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

import android.os.*;

import com.google.gson.Gson;

import org.json.*;

import java.io.Serializable;
import java.net.*;
import java.util.*;

/**
 * @author Sean Leonard <meanderingcode@aetherislands.net>
 * @author Parménides GV <parmegv@sdf.org>
 */
public final class Provider implements Parcelable {

    private JSONObject definition = new JSONObject(); // Represents our Provider's provider.json
    private DefaultedURL mainUrl = new DefaultedURL();
    private DefaultedURL apiUrl = new DefaultedURL();
    private String certificatePin = "";
    private String certificatePinEncoding = "";
    private String caCert = "";

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
            DOT_JSON_URL = "provider_json_url";

    // Array of what API versions we understand
    protected static final String[] API_VERSIONS = {"1"};  // I assume we might encounter arbitrary version "numbers"
    // Some API pieces we want to know about
    private static final String API_TERM_SERVICES = "services";
    private static final String API_TERM_NAME = "name";
    private static final String API_TERM_DOMAIN = "domain";
    private static final String API_TERM_DEFAULT_LANGUAGE = "default_language";
    protected static final String[] API_EIP_TYPES = {"openvpn"};

    public Provider() { }

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
                definition.length() > 0 &&
                !apiUrl.isDefault() &&
                caCert != null &&
                !caCert.isEmpty();
    }

    protected void setUrl(URL url) {
        mainUrl.setUrl(url);
    }

    protected void define(JSONObject provider_json) {
        definition = provider_json;
        parseDefinition(definition);
    }

    protected JSONObject getDefinition() {
        return definition;
    }

    protected String getDomain() {
        return mainUrl.getDomain();
    }

    protected DefaultedURL getMainUrl() {
        return mainUrl;
    }

    protected DefaultedURL getApiUrl() {
        return apiUrl;
    }

    protected String certificatePin() { return certificatePin; }

    protected boolean hasCertificatePin() {
        return certificatePin != null && !certificatePin.isEmpty();
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
        try {
            JSONArray services = definition.getJSONArray(API_TERM_SERVICES); // returns ["openvpn"]
            for (int i = 0; i < API_EIP_TYPES.length + 1; i++) {
                try {
                    // Walk the EIP types array looking for matches in provider's service definitions
                    if (Arrays.asList(API_EIP_TYPES).contains(services.getString(i)))
                        return true;
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    return false;
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    return false;
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
        return false;
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
        if(mainUrl != null)
            parcel.writeString(mainUrl.toString());
        if (definition != null)
            parcel.writeString(definition.toString());
        if (caCert != null)
            parcel.writeString(caCert);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Provider) {
            Provider p = (Provider) o;
            return p.getDomain().equals(getDomain());
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
            String definitionString = in.readString();
            if (!definitionString.isEmpty()) {
                definition = new JSONObject((definitionString));
                parseDefinition(definition);
            }
            String caCert = in.readString();
            if (!caCert.isEmpty()) {
                this.caCert = caCert;
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
        } catch (JSONException | ArrayIndexOutOfBoundsException | MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void setCACert(String cert) {
        this.caCert = cert;
    }

}
