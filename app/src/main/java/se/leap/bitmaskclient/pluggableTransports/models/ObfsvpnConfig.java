package se.leap.bitmaskclient.pluggableTransports.models;

import androidx.annotation.NonNull;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ObfsvpnConfig {

    final String proxyAddr;
    final HoppingConfig hoppingConfig;
    final KcpConfig kcpConfig;
    final String remoteIp;
    final String remotePort;
    final String obfs4Cert;

    public ObfsvpnConfig(String proxyAddress, HoppingConfig hoppingConfig, KcpConfig kcpConfig, String remoteIP, String remotePort, String obfsv4Cert) {
        this.proxyAddr = proxyAddress;
        this.hoppingConfig = hoppingConfig;
        this.kcpConfig = kcpConfig;
        this.remoteIp = remoteIP;
        this.remotePort = remotePort;
        this.obfs4Cert = obfsv4Cert;
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
