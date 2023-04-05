package se.leap.bitmaskclient.pluggableTransports;

import androidx.annotation.NonNull;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import se.leap.bitmaskclient.base.models.Transport;

public class HoppingConfig {
    final boolean kcp;
    final String proxyAddr;
    final String[] remotes;
    final String[] certs;
    final int portSeed;
    final int portCount;
    final int minHopSeconds;
    final int hopJitter;

    public HoppingConfig(boolean kcp,
                         String proxyAddr,
                         Transport transport,
                         int minHopSeconds,
                         int hopJitter) {
        this.kcp = kcp;
        this.proxyAddr = proxyAddr;
        Transport.Endpoint[] endpoints = transport.getOptions().getEndpoints();
        this.remotes = new String[endpoints.length];
        this.certs = new String[endpoints.length];
        for (int i = 0; i < remotes.length; i++) {
            remotes[i] = endpoints[i].getIp();
            certs[i] = endpoints[i].getCert();
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
