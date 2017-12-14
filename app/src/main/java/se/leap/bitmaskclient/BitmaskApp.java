package se.leap.bitmaskclient;

import android.annotation.TargetApi;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;

import de.blinkt.openvpn.core.OpenVPNService;

import static android.os.Build.VERSION_CODES.O;

/**
 * Created by cyberta on 24.10.17.
 */

public class BitmaskApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PRNGFixes.apply();
        //TODO: add LeakCanary!
        if (Build.VERSION.SDK_INT >= O)
            createNotificationChannelsForOpenvpn();
    }


    @TargetApi(O)
    private void createNotificationChannelsForOpenvpn() {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Background message
        CharSequence name = getString(R.string.channel_name_background);
        NotificationChannel mChannel = new NotificationChannel(OpenVPNService.NOTIFICATION_CHANNEL_BG_ID,
                name, NotificationManager.IMPORTANCE_MIN);

        mChannel.setDescription(getString(R.string.channel_description_background));
        mChannel.enableLights(false);

        mChannel.setLightColor(Color.DKGRAY);
        mNotificationManager.createNotificationChannel(mChannel);

        // Connection status change messages

        name = getString(R.string.channel_name_status);
        mChannel = new NotificationChannel(OpenVPNService.NOTIFICATION_CHANNEL_NEWSTATUS_ID,
                name, NotificationManager.IMPORTANCE_DEFAULT);


        mChannel.setDescription(getString(R.string.channel_description_status));
        mChannel.enableLights(true);

        mChannel.setLightColor(Color.BLUE);
        mNotificationManager.createNotificationChannel(mChannel);

    }



}
