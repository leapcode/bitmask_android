package se.leap.bitmaskclient.appUpdate;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * DownloadServiceCommand is only implemented in Fatweb builds
 *
 */
public class DownloadServiceCommand {

    public final static String
            CHECK_VERSION_FILE = "checkVersionFile",
            DOWNLOAD_UPDATE = "downloadUpdate";


    public static void execute(Context context, String action) {
        // DO NOTHING.
    }

    public static void execute(@NonNull Context context, @NonNull String action, @Nullable Bundle parameters) {
        // DO NOTHING.
    }
}
