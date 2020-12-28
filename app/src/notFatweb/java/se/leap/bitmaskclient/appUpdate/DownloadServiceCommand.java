package se.leap.bitmaskclient.appUpdate;

import android.content.Context;

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
}
