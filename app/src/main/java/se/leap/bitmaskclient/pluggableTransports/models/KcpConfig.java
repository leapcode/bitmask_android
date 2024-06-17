package se.leap.bitmaskclient.pluggableTransports.models;

import androidx.annotation.NonNull;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class KcpConfig {

    // check OpenVPN's --sndbuf size and --rcvbuf size
    public static final int DEFAULT_KCP_SEND_WINDOW_SIZE = 32;
    public static final int DEFAULT_KCP_RECEIVE_WINDOW_SIZE = 32;
    public static final int DEFAULT_KCP_READ_BUFFER = 16 * 1024 * 1024;
    public static final int DEFAULT_KCP_WRITE_BUFFER = 16 * 1024 * 1024;

    final boolean enabled;
    final int sendWindowSize;
    final int receiveWindowSize;
    final int readBuffer;
    final int writeBuffer;

    public KcpConfig(boolean enabled) {
        this.enabled = enabled;
        this.sendWindowSize = DEFAULT_KCP_SEND_WINDOW_SIZE;
        this.receiveWindowSize = DEFAULT_KCP_RECEIVE_WINDOW_SIZE;
        this.readBuffer = DEFAULT_KCP_READ_BUFFER;
        this.writeBuffer = DEFAULT_KCP_WRITE_BUFFER;
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
