package se.leap.bitmaskclient.base.models;

import static de.blinkt.openvpn.core.connection.Connection.TransportType.OPENVPN;
import static se.leap.bitmaskclient.base.models.Constants.CAPABILITIES;
import static se.leap.bitmaskclient.base.models.Constants.CERT;
import static se.leap.bitmaskclient.base.models.Constants.IAT_MODE;
import static se.leap.bitmaskclient.base.models.Constants.PORTS;
import static se.leap.bitmaskclient.base.models.Constants.PROTOCOLS;
import static se.leap.bitmaskclient.base.models.Constants.TRANSPORT;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Map;
import java.util.Vector;

import de.blinkt.openvpn.core.connection.Connection;
import io.swagger.client.model.ModelsBridge;
import io.swagger.client.model.ModelsGateway;

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

    public Transport(String type, String[] protocols, @Nullable String[] ports) {
        this.type = type;
        this.protocols = protocols;
        this.ports = ports;
        this.options = null;
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

    public static Transport createTransportFrom(ModelsBridge modelsBridge) {
        if (modelsBridge == null) {
            return null;
        }
        Map<String, Object> options = modelsBridge.getOptions();
        Transport.Options transportOptions = new Transport.Options((String) options.get(CERT), (String) options.get(IAT_MODE));
        Transport transport = new Transport(
                modelsBridge.getType(),
                new String[]{modelsBridge.getTransport()},
                new String[]{String.valueOf(modelsBridge.getPort())},
                transportOptions
        );
        return transport;
    }

    public static Transport createTransportFrom(ModelsGateway modelsGateway) {
        if (modelsGateway == null) {
            return null;
        }
        Transport transport = new Transport(
                modelsGateway.getType(),
                new String[]{modelsGateway.getTransport()},
                new String[]{String.valueOf(modelsGateway.getPort())}
        );
        return transport;
    }


    @NonNull
    public static Vector<Transport> createTransportsFrom(JSONObject gateway, int apiVersion) throws IllegalArgumentException {
        Vector<Transport> transports = new Vector<>();
        try {
            if (apiVersion >= 3) {
                JSONArray supportedTransports = gateway.getJSONObject(CAPABILITIES).getJSONArray(TRANSPORT);
                for (int i = 0; i < supportedTransports.length(); i++) {
                    Transport transport = Transport.fromJson(supportedTransports.getJSONObject(i));
                    transports.add(transport);
                }
            } else {
                JSONObject capabilities =  gateway.getJSONObject(CAPABILITIES);
                JSONArray ports = capabilities.getJSONArray(PORTS);
                JSONArray protocols = capabilities.getJSONArray(PROTOCOLS);
                String[] portArray = new String[ports.length()];
                String[] protocolArray = new String[protocols.length()];
                for (int i = 0; i < ports.length(); i++) {
                    portArray[i] = String.valueOf(ports.get(i));
                }
                for (int i = 0; i < protocols.length(); i++) {
                    protocolArray[i] = protocols.optString(i);
                }
                Transport transport = new Transport(OPENVPN.toString(), protocolArray, portArray);
                transports.add(transport);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException();
            //throw new ConfigParser.ConfigParseError("Api version ("+ apiVersion +") did not match required JSON fields");
        }
        return transports;
    }

    public static Vector<Transport> createTransportsFrom(ModelsBridge modelsBridge) {
        Vector<Transport> transports = new Vector<>();
        transports.add(Transport.createTransportFrom(modelsBridge));
        return transports;
    }

    public static Vector<Transport> createTransportsFrom(ModelsGateway modelsGateway) {
        Vector<Transport> transports = new Vector<>();
        transports.add(Transport.createTransportFrom(modelsGateway));
        return transports;
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


