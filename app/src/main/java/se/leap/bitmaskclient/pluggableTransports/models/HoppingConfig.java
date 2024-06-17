package se.leap.bitmaskclient.pluggableTransports.models;

import androidx.annotation.NonNull;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import se.leap.bitmaskclient.base.models.Transport;

public class HoppingConfig {

    /**
     * Enabled       bool     `json:"enabled"`
     * 	Remotes       []string `json:"remotes"`
     * 	Obfs4Certs    []string `json:"obfs4_certs"`
     * 	PortSeed      int64    `json:"port_seed"`
     * 	PortCount     uint     `json:"port_count"`
     * 	MinHopSeconds uint     `json:"min_hop_seconds"`
     * 	HopJitter     uint     `json:"hop_jitter"`
     * }
     */

    final boolean enabled;
    final String proxyAddr;
    final String[] remotes;
    final String[] obfs4Certs;
    final int portSeed;
    final int portCount;
    final int minHopSeconds;
    final int hopJitter;

    public HoppingConfig(boolean enabled,
                         String proxyAddr,
                         Obfs4Options options,
                         int minHopSeconds,
                         int hopJitter) {
        this.enabled = enabled;
        this.proxyAddr = proxyAddr;
        Transport transport = options.transport;
        Transport.Endpoint[] endpoints = transport.getOptions().getEndpoints();
        if (endpoints == null) {
            // only port hopping, we assume the gateway IP as hopping PT's IP
            this.remotes = new String[]{ options.bridgeIP };
            this.obfs4Certs = new String[] { transport.getOptions().getCert() };
        } else {
            // port+ip hopping
            this.remotes = new String[endpoints.length];
            this.obfs4Certs = new String[endpoints.length];
            for (int i = 0; i < remotes.length; i++) {
                remotes[i] = endpoints[i].getIp();
                obfs4Certs[i] = endpoints[i].getCert();
            }
        }
        this.portSeed = transport.getOptions().getPortSeed();
        this.portCount = transport.getOptions().getPortCount();
        this.minHopSeconds = minHopSeconds;
        this.hopJitter = hopJitter;
    }

    @NonNull
    @Override
    public String toString() {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
       return gson.toJson(this);
    }
}
