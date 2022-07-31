package se.leap.bitmaskclient.base.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONObject;

public class Transport {
    private String type;
    private String[] protocols;
    private String[] ports;
    private Options options;

    public Transport(String type, String[] protocols, String[] ports, String cert) {
        this.type = type;
        this.protocols = protocols;
        this.ports = ports;
        this.options = new Options(cert);
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public static Transport fromJson(JSONObject json) {
        GsonBuilder builder = new GsonBuilder();
        return builder.create().fromJson(json.toString(), Transport.class);
    }

    public static class Options {
        private String cert;
        private String iatMode;

        public Options(String cert) {
            this.cert = cert;
            this.iatMode = "0";
        }

        @Override
        public String toString() {
            return new Gson().toJson(this);
        }
    }


}


