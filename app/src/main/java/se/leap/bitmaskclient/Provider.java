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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Locale;

/**
 * @author Sean Leonard <meanderingcode@aetherislands.net>
 *
 */
public final class Provider implements Parcelable {

	private JSONObject definition; // Represents our Provider's provider.json
    private URL main_url;

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
	DOT_JSON_URL = "provider_json_url"
	;

	// Array of what API versions we understand
	protected static final String[] API_VERSIONS = {"1"};  // I assume we might encounter arbitrary version "numbers"
	// Some API pieces we want to know about
	private static final String API_TERM_SERVICES = "services";
	private static final String API_TERM_NAME = "name";
	private static final String API_TERM_DOMAIN = "domain";
	private static final String API_TERM_DEFAULT_LANGUAGE = "default_language";
	protected static final String[] API_EIP_TYPES = {"openvpn"};

	public Provider(URL main_url) {
        this.main_url = main_url;
    }

    public Provider(File provider_file) {

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

    private Provider(Parcel in) {
        try {
            main_url = new URL(in.readString());
            String definition_string = in.readString();
            if(definition_string != null)
                definition = new JSONObject((definition_string));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void define(JSONObject provider_json) {
        definition = provider_json;
    }

    protected JSONObject definition() { return definition; }

	protected String getDomain(){
		return main_url.getHost();
	}

    protected URL mainUrl() {
        return main_url;
    }
	
	protected String getName(){
		// Should we pass the locale in, or query the system here?
		String lang = Locale.getDefault().getLanguage();
		String name = "";
		try {
            if(definition != null)
			    name = definition.getJSONObject(API_TERM_NAME).getString(lang);
            else throw new JSONException("Provider not defined");
		} catch (JSONException e) {
            if(main_url != null) {
                String host = main_url.getHost();
                name = host.substring(0, host.indexOf("."));
            }
		}
		
		return name;
	}
	
	protected String getDescription(){
		String lang = Locale.getDefault().getLanguage();
		String desc = null;
		try {
			desc = definition.getJSONObject("description").getString(lang);
		} catch (JSONException e) {
			// TODO: handle exception!!
			try {
				desc = definition.getJSONObject("description").getString( definition.getString("default_language") );
			} catch (JSONException e2) {
				// TODO: i can't believe you're doing it again!
			}
		}
		
		return desc;
	}

	protected boolean hasEIP() {
		try {
            JSONArray services = definition.getJSONArray(API_TERM_SERVICES); // returns ["openvpn"]
            for (int i=0;i<API_EIP_TYPES.length+1;i++){
                try {
                    // Walk the EIP types array looking for matches in provider's service definitions
                    if ( Arrays.asList(API_EIP_TYPES).contains( services.getString(i) ) )
                        return true;
                } catch (NullPointerException e){
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(main_url.toString());
        if(definition != null)
            parcel.writeString(definition.toString());
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Provider) {
            Provider p = (Provider) o;
            return p.mainUrl().equals(mainUrl());
        } else return false;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put(Provider.MAIN_URL, main_url);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public int hashCode() {
        return main_url.hashCode();
    }
}
