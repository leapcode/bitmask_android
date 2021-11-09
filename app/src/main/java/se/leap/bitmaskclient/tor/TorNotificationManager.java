package se.leap.bitmaskclient.tor;
/**
 * Copyright (c) 2021 LEAP Encryption Access Project and contributers
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

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import se.leap.bitmaskclient.R;

public class TorNotificationManager {
    public   final static int TOR_SERVICE_NOTIFICATION_ID = 10;
    static final String NOTIFICATION_CHANNEL_NEWSTATUS_ID = "bitmask_tor_service_news";
    private long lastNotificationTime = 0;
    // debounce timeout in milliseconds
    private final static long NOTIFICATION_DEBOUNCE_TIME = 500;


    public TorNotificationManager() {}


    public static Notification buildTorForegroundNotification(Context context) {
        NotificationManager notificationManager = initNotificationManager(context);
        if (notificationManager == null) {
            return null;
        }
        NotificationCompat.Builder notificationBuilder = initNotificationBuilderDefaults(context);
        return notificationBuilder
                .setSmallIcon(R.drawable.ic_bridge_36)
                .setWhen(System.currentTimeMillis())
                .setContentText(context.getString(R.string.tor_started)).build();
    }

    public void buildTorNotification(Context context, String state, String message, int progress) {
        if (shouldDropNotification()) {
            return;
        }
        NotificationManager notificationManager = initNotificationManager(context);
        if (notificationManager == null) {
            return;
        }
        NotificationCompat.Builder notificationBuilder = initNotificationBuilderDefaults(context);
        notificationBuilder
                .setSmallIcon(R.drawable.ic_bridge_36)
                .setWhen(System.currentTimeMillis())
                .setStyle(new NotificationCompat.BigTextStyle().
                        setBigContentTitle(state).
                        bigText(message))
                .setTicker(message)
                .setContentTitle(state)
                .setOnlyAlertOnce(true)
                .setContentText(message);
        if (progress > 0) {
            notificationBuilder.setProgress(100, progress, false);
        }
        notificationManager.notify(TOR_SERVICE_NOTIFICATION_ID, notificationBuilder.build());
    }

    private boolean shouldDropNotification() {
        long now = System.currentTimeMillis();
        if (now - lastNotificationTime < NOTIFICATION_DEBOUNCE_TIME) {
            return true;
        }
        lastNotificationTime = now;
        return false;
    }


    private static NotificationManager initNotificationManager(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(context, notificationManager);
        }
        return notificationManager;
    }

    @TargetApi(26)
    private static void createNotificationChannel(Context context, NotificationManager notificationManager) {
        String appName = context.getString(R.string.app_name);
        CharSequence name =  context.getString(R.string.channel_name_tor_service, appName);
        String description = context.getString(R.string.channel_description_tor_service, appName);
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_NEWSTATUS_ID,
                name,
                NotificationManager.IMPORTANCE_LOW);
        channel.setSound(null, null);
        channel.setDescription(description);
        notificationManager.createNotificationChannel(channel);
    }

    private static NotificationCompat.Builder initNotificationBuilderDefaults(Context context) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_NEWSTATUS_ID);
        notificationBuilder.
                setDefaults(Notification.DEFAULT_ALL).
                setLocalOnly(true).
                setAutoCancel(false);
        return notificationBuilder;
    }

    public void cancelNotifications(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }
        notificationManager.cancel(TOR_SERVICE_NOTIFICATION_ID);
    }
}
