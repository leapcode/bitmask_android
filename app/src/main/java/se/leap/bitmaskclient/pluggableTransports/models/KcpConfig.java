package se.leap.bitmaskclient.pluggableTransports.models;

import androidx.annotation.NonNull;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

public class KcpConfig {


    final boolean enabled;
    final int sendWindowSize = 65535;
    final int receiveWindowSize = 65535;
    final int readBuffer = 16 * 1024 * 1024;
    final int writeBuffer = 16 * 1024 * 1024;
    final boolean noDelay = true;
    final boolean disableFlowControl = true;
    final int interval  = 10;
    final int resend = 2;
    @SerializedName("mtu")
    final int MTU = 1400;

    public KcpConfig(boolean enabled) {
        this.enabled = enabled;
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
