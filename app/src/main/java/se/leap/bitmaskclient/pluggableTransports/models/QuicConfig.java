package se.leap.bitmaskclient.pluggableTransports.models;

import androidx.annotation.NonNull;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class QuicConfig {
    final boolean enabled;

    public QuicConfig(boolean enabled) {
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
