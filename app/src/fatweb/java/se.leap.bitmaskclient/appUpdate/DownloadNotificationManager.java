/**
 * Copyright (c) 2020 LEAP Encryption Access Project and contributers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package se.leap.bitmaskclient.appUpdate;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import se.leap.bitmaskclient.R;

import static android.content.Intent.CATEGORY_DEFAULT;
import static se.leap.bitmaskclient.appUpdate.DownloadBroadcastReceiver.ACTION_DOWNLOAD;

public class DownloadNotificationManager {
    private Context context;
    private  final static int DOWNLOAD_NOTIFICATION_ID = 1;

    public DownloadNotificationManager(@NonNull Context context) {
        this.context = context;
    }

    public void buildDownloadFoundNotification() {
        NotificationManager notificationManager = initNotificationManager();
        if (notificationManager == null) {
            return;
        }
        NotificationCompat.Builder notificationBuilder = initNotificationBuilderDefaults();
        notificationBuilder
                .setSmallIcon(R.drawable.ic_about_36)
                .setWhen(System.currentTimeMillis())
                .setTicker(context.getString(R.string.version_update_title, context.getString(R.string.app_name)))
                .setContentTitle(context.getString(R.string.version_update_title, context.getString(R.string.app_name)))
                .setContentText(context.getString(R.string.version_update_found))
                .setContentIntent(getDownloadIntent());
        notificationManager.notify(DOWNLOAD_NOTIFICATION_ID, notificationBuilder.build());
    }

    public void buildDownloadSuccessfulNotification() {
        NotificationManager notificationManager = initNotificationManager();
        if (notificationManager == null) {
            return;
        }
        NotificationCompat.Builder notificationBuilder = initNotificationBuilderDefaults();
        notificationBuilder
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setWhen(System.currentTimeMillis())
                .setTicker(context.getString(R.string.version_update_title, context.getString(R.string.app_name)))
                .setContentTitle(context.getString(R.string.version_update_download_title, context.getString(R.string.app_name)))
                .setContentText(context.getString(R.string.version_update_download_description))
                .setContentIntent(getInstallIntent());
        notificationManager.notify(DOWNLOAD_NOTIFICATION_ID, notificationBuilder.build());
    }

    public void buildDownloadUpdateProgress(int progress) {
        NotificationManager notificationManager = initNotificationManager();
        if (notificationManager == null) {
            return;
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this.context, DownloadService.NOTIFICATION_CHANNEL_NEWSTATUS_ID);
        notificationBuilder
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(false)
                .setOngoing(true)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(context.getString(R.string.version_update_apk_description, context.getString(R.string.app_name)))
                .setProgress(100, progress, false)
                .setContentIntent(getDownloadIntent());
        notificationManager.notify(DOWNLOAD_NOTIFICATION_ID, notificationBuilder.build());
    }

    private NotificationManager initNotificationManager() {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager);
        }
        return notificationManager;
    }

    @TargetApi(26)
    private void createNotificationChannel(NotificationManager notificationManager) {
        CharSequence name = "Bitmask Updates";
        String description = "Informs about available updates";
        NotificationChannel channel = new NotificationChannel(DownloadService.NOTIFICATION_CHANNEL_NEWSTATUS_ID,
                name,
                NotificationManager.IMPORTANCE_LOW);
        channel.setSound(null, null);
        channel.setDescription(description);
        channel.setLightColor(Color.BLUE);
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        notificationManager.createNotificationChannel(channel);
    }

    private NotificationCompat.Builder initNotificationBuilderDefaults() {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this.context, DownloadService.NOTIFICATION_CHANNEL_NEWSTATUS_ID);
        notificationBuilder.
                setDefaults(Notification.DEFAULT_ALL).
                setAutoCancel(true);
        return notificationBuilder;
    }

    private PendingIntent getDownloadIntent() {
        Intent downloadIntent = new Intent(context, DownloadBroadcastReceiver.class);
        downloadIntent.setAction(ACTION_DOWNLOAD);
        return PendingIntent.getBroadcast(context, 0, downloadIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private PendingIntent getInstallIntent() {
        Intent installIntent = new Intent(context, InstallActivity.class);
        return PendingIntent.getActivity(context, 0, installIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public void cancelNotifications() {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }
        notificationManager.cancel(DOWNLOAD_NOTIFICATION_ID);
    }
}
