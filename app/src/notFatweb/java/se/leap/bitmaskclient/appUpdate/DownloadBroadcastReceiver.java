package se.leap.bitmaskclient.appUpdate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * DownloadBroadcastReceiver is only implemented in Fatweb builds
 *
 */
public class DownloadBroadcastReceiver extends BroadcastReceiver {

    public static final String ACTION_DOWNLOAD = "se.leap.bitmaskclient.appUpdate.ACTION_DOWNLOAD";
    @Override
    public void onReceive(Context context, Intent intent) {

    }
}
