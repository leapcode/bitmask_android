package se.leap.bitmaskclient.base.models;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;

public class GatewayJson {
    private String location;
    @SerializedName(value = "ip_address")
    private String ipAddress;
    @SerializedName(value = "ip_address6")
    private String ipAddress6;
    private String host;
    private Capabilities capabilities;

    public GatewayJson(String location, String ipAddress, String ipAddress6, String host, Capabilities capabilities)  {
        this.location = location;
        this.ipAddress = ipAddress;
        this.ipAddress6 = ipAddress6;
        this.host = host;
        this.capabilities = capabilities;
    }

    @NonNull
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public static GatewayJson fromJson(JSONObject json) {
        GsonBuilder builder = new GsonBuilder();
        return builder.create().fromJson(json.toString(), GatewayJson.class);
    }

    public static class Capabilities {
        private Boolean adblock;
        @SerializedName(value = "filter_dns")
        private Boolean filterDns;
        private Boolean limited;
        private Transport[] transport;
        @SerializedName(value = "user_ips")
        private Boolean userIps;

        public Capabilities(Boolean adblock, Boolean filterDns, Boolean limited, Transport[] transport, Boolean userIps) {
            this.adblock = adblock;
            this.filterDns = filterDns;
            this.limited = limited;
            this.transport = transport;
            this.userIps = userIps;
        }
        @NonNull
        @Override
        public String toString() {
            return new Gson().toJson(this);
        }
    }
}
