package se.leap.bitmaskclient.base.models;

import androidx.annotation.Nullable;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;

import java.io.Serializable;

import de.blinkt.openvpn.core.connection.Connection;

public class Transport implements Serializable {
    private final String type;
    private final String[] protocols;
    @Nullable
    private final String[] ports;
    @Nullable
    private final Options options;

    public Transport(String type, String[] protocols, String[] ports, String cert) {
        this(type, protocols, ports, new Options(cert, "0"));
    }

    public Transport(String type, String[] protocols, @Nullable String[] ports, @Nullable Options options) {
        this.type = type;
        this.protocols = protocols;
        this.ports = ports;
        this.options = options;
    }

    public String getType() {
        return type;
    }

    public Connection.TransportType getTransportType() {
        return Connection.TransportType.fromString(type);
    }

    public String[] getProtocols() {
        return protocols;
    }

    @Nullable
    public String[] getPorts() {
        return ports;
    }

    @Nullable
    public Options getOptions() {
        return options;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public static Transport fromJson(JSONObject json) {
        GsonBuilder builder = new GsonBuilder();
        return builder.
                setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).
                create().
                fromJson(json.toString(), Transport.class);
    }

    public static class Options implements Serializable {
        @Nullable
        private final String cert;
        @SerializedName("iatMode")
        private final String iatMode;

        @Nullable
        private Endpoint[] endpoints;

        private boolean experimental;

        private int portSeed;

        private int portCount;


        public Options(String cert, String iatMode) {
            this.cert = cert;
            this.iatMode = iatMode;
        }

        public Options(String iatMode, Endpoint[] endpoints, int portSeed, int portCount, boolean experimental) {
            this(iatMode, endpoints, null, portSeed, portCount, experimental);
        }

        public Options(String iatMode, Endpoint[] endpoints, String cert, int portSeed, int portCount, boolean experimental) {
            this.iatMode = iatMode;
            this.endpoints = endpoints;
            this.portSeed = portSeed;
            this.portCount = portCount;
            this.experimental = experimental;
            this.cert = cert;
        }


        @Nullable
        public String getCert() {
            return cert;
        }

        public String getIatMode() {
            return iatMode;
        }

        @Nullable
        public Endpoint[] getEndpoints() {
            return endpoints;
        }

        public boolean isExperimental() {
            return experimental;
        }

        public int getPortSeed() {
            return portSeed;
        }

        public int getPortCount() {
            return portCount;
        }

        @Override
        public String toString() {
            return new Gson().toJson(this);
        }
    }


    public static class Endpoint implements Serializable {
        private final String ip;
        private final String cert;

        public Endpoint(String ip, String cert) {
            this.ip = ip;
            this.cert = cert;
        }

        @Override
        public String toString() {
            return new Gson().toJson(this);
        }

        public String getIp() {
            return ip;
        }

        public String getCert() {
            return cert;
        }
    }

}


