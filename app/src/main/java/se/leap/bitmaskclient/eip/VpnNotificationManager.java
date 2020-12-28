/**
 * Copyright (c) 2018 LEAP Encryption Access Project and contributers
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
package se.leap.bitmaskclient.eip;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.widget.RemoteViews;

import de.blinkt.openvpn.LaunchVPN;
import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.OpenVPNService;
import se.leap.bitmaskclient.base.MainActivity;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.StartActivity;

import static android.os.Build.VERSION_CODES.O;
import static androidx.core.app.NotificationCompat.PRIORITY_HIGH;
import static androidx.core.app.NotificationCompat.PRIORITY_MAX;
import static androidx.core.app.NotificationCompat.PRIORITY_MIN;
import static android.text.TextUtils.isEmpty;
import static de.blinkt.openvpn.core.ConnectionStatus.LEVEL_NONETWORK;
import static de.blinkt.openvpn.core.ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT;
import static se.leap.bitmaskclient.base.models.Constants.ASK_TO_CANCEL_VPN;
import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_STOP_BLOCKING_VPN;
import static se.leap.bitmaskclient.base.MainActivity.ACTION_SHOW_VPN_FRAGMENT;

/**
 * Created by cyberta on 14.01.18.
 */

public class VpnNotificationManager {

    Context context;
    private VpnServiceCallback vpnServiceCallback;
    private NotificationManager notificationManager;
    private NotificationManagerCompat compatNotificationManager;
    private String[] notificationChannels = {
            OpenVPNService.NOTIFICATION_CHANNEL_NEWSTATUS_ID,
            OpenVPNService.NOTIFICATION_CHANNEL_BG_ID,
            VoidVpnService.NOTIFICATION_CHANNEL_NEWSTATUS_ID};
    private String lastNotificationChannel = "";

    public interface VpnServiceCallback {
        void onNotificationBuild(int notificationId, Notification notification);
        void onNotificationStop();
    }

    public VpnNotificationManager(@NonNull Context context, @NonNull VpnServiceCallback vpnServiceCallback) {
       this.context = context;
       notificationManager =  (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
       compatNotificationManager = NotificationManagerCompat.from(context);
       this.vpnServiceCallback = vpnServiceCallback;
    }

    public void buildVoidVpnNotification(final String msg, String tickerText, ConnectionStatus status) {
        //TODO: implement extra Dashboard.ACTION_ASK_TO_CANCEL_BLOCKING_VPN
        NotificationCompat.Action.Builder actionBuilder = new NotificationCompat.Action.Builder(R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.vpn_button_turn_off_blocking), getStopVoidVpnIntent());

        buildVpnNotification(
                context.getString(R.string.void_vpn_title),
                msg,
                null,
                tickerText,
                status,
                VoidVpnService.NOTIFICATION_CHANNEL_NEWSTATUS_ID,
                PRIORITY_MAX,
                0,
                getMainActivityIntent(),
                actionBuilder.build());
    }

    public void stopNotifications(String notificationChannelNewstatusId) {
        vpnServiceCallback.onNotificationStop();
        compatNotificationManager.cancel(notificationChannelNewstatusId.hashCode());
    }

    public void deleteNotificationChannel(String notificationChannel) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                notificationManager.getNotificationChannel(notificationChannel) != null) {
            notificationManager.deleteNotificationChannel(notificationChannel);
        }
    }

    /**
     * @param msg
     * @param tickerText
     * @param status
     * @param when
     */
    public void buildOpenVpnNotification(String profileName, boolean isObfuscated, String msg, String tickerText, ConnectionStatus status, long when, String notificationChannelNewstatusId) {
        String cancelString;
        CharSequence bigmessage = null;
        String ghostIcon = new String(Character.toChars(0x1f309));

        switch (status) {
            // show cancel if no connection
            case LEVEL_START:
            case LEVEL_NONETWORK:
            case LEVEL_CONNECTING_SERVER_REPLIED:
            case LEVEL_CONNECTING_NO_SERVER_REPLY_YET:
                cancelString = context.getString(R.string.cancel);
                if (isObfuscated && Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                    Spannable spannable = new SpannableString(context.getString(R.string.obfuscated_connection_try));
                    spannable.setSpan(new StyleSpan(Typeface.ITALIC), 0, spannable.length() -1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    bigmessage = TextUtils.concat(spannable, " " + ghostIcon + "\n" + msg);
                }
                break;

            // show disconnect if connection exists
            case LEVEL_CONNECTED:
                if (isObfuscated && Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                    Spannable spannable = new SpannableString(context.getString(R.string.obfuscated_connection));
                    spannable.setSpan(new StyleSpan(Typeface.ITALIC), 0, spannable.length() -1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    bigmessage = TextUtils.concat(spannable, " " + ghostIcon + "\n" + msg);
                }
            default:
                cancelString = context.getString(R.string.cancel_connection);
        }

        if (isObfuscated) {
            msg =  ghostIcon + " " + msg;
        }

        NotificationCompat.Action.Builder actionBuilder = new NotificationCompat.Action.
                Builder(R.drawable.ic_menu_close_clear_cancel, cancelString, getDisconnectIntent());
        String title;
        String appName = context.getString(R.string.app_name);
        if (isEmpty(profileName)) {
            title = appName;
       } else {
            title = context.getString(R.string.notifcation_title_bitmask, appName, profileName);
        }

        PendingIntent contentIntent;
        if (status == LEVEL_WAITING_FOR_USER_INPUT)
            contentIntent = getUserInputIntent(msg);
        else
            contentIntent = getMainActivityIntent();

        int priority;
        if (OpenVPNService.NOTIFICATION_CHANNEL_NEWSTATUS_ID.equals(notificationChannelNewstatusId)) {
            priority = PRIORITY_HIGH;
        } else {
            // background channel
            priority = PRIORITY_MIN;
        }

        buildVpnNotification(
                title,
                msg,
                bigmessage,
                tickerText,
                status,
                notificationChannelNewstatusId,
                priority,
                when,
                contentIntent,
                actionBuilder.build());
    }


    @TargetApi(O)
    public void createVoidVpnNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        // Connection status change messages
        CharSequence name = context.getString(R.string.channel_name_status);
        NotificationChannel mChannel = new NotificationChannel(VoidVpnService.NOTIFICATION_CHANNEL_NEWSTATUS_ID,
                name, NotificationManager.IMPORTANCE_DEFAULT);

        mChannel.setDescription(context.getString(R.string.channel_description_status));
        mChannel.enableLights(true);

        mChannel.setLightColor(Color.BLUE);
        mChannel.setSound(null, null);
        notificationManager.createNotificationChannel(mChannel);
    }

    @TargetApi(O)
    public void createOpenVpnNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        // Background message
        CharSequence name = context.getString(R.string.channel_name_background);
        NotificationChannel mChannel = new NotificationChannel(OpenVPNService.NOTIFICATION_CHANNEL_BG_ID,
                name, NotificationManager.IMPORTANCE_MIN);

        mChannel.setDescription(context.getString(R.string.channel_description_background));
        mChannel.enableLights(false);

        mChannel.setLightColor(Color.DKGRAY);
        notificationManager.createNotificationChannel(mChannel);

        // Connection status change messages
        name = context.getString(R.string.channel_name_status);
        mChannel = new NotificationChannel(OpenVPNService.NOTIFICATION_CHANNEL_NEWSTATUS_ID,
                name, NotificationManager.IMPORTANCE_DEFAULT);


        mChannel.setDescription(context.getString(R.string.channel_description_status));
        mChannel.enableLights(true);

        mChannel.setLightColor(Color.BLUE);
        mChannel.setSound(null, null);
        notificationManager.createNotificationChannel(mChannel);
    }

    /**
     * @return a custom remote view for notifications for API 16 - 19
     */
    private RemoteViews getKitkatCustomRemoteView(ConnectionStatus status, String title, String message) {
        int iconResource = getIconByConnectionStatus(status);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.v_custom_notification);
        remoteViews.setImageViewResource(R.id.image_icon, iconResource);
        remoteViews.setTextViewText(R.id.message, message);
        remoteViews.setTextViewText(R.id.title, title);

        return remoteViews;
    }

    private void buildVpnNotification(String title, String message, CharSequence bigMessage, String tickerText, ConnectionStatus status, String notificationChannelNewstatusId, int priority, long when, PendingIntent contentIntent, NotificationCompat.Action notificationAction) {
        NotificationCompat.Builder nCompatBuilder = new NotificationCompat.Builder(context, notificationChannelNewstatusId);
        int icon = getIconByConnectionStatus(status);

        // this is a workaround to avoid confusion between the Android's system vpn notification
        // showing a filled out key icon and the bitmask icon indicating a different state.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT &&
                notificationChannelNewstatusId.equals(OpenVPNService.NOTIFICATION_CHANNEL_NEWSTATUS_ID)) {
            if (status != LEVEL_NONETWORK) {
                // removes the icon from the system status bar
                icon = android.R.color.transparent;
                // adds the icon to the notification in the notification drawer
                nCompatBuilder.setContent(getKitkatCustomRemoteView(status, title, message));
            }
        } else {
            nCompatBuilder.setStyle(new NotificationCompat.BigTextStyle().
                    setBigContentTitle(title).
                    bigText(bigMessage));
        }
        nCompatBuilder.addAction(notificationAction);
        nCompatBuilder.setContentTitle(title);
        nCompatBuilder.setCategory(NotificationCompat.CATEGORY_SERVICE);
        nCompatBuilder.setLocalOnly(true);
        nCompatBuilder.setContentText(message);
        nCompatBuilder.setOnlyAlertOnce(true);
        nCompatBuilder.setSmallIcon(icon);
        nCompatBuilder.setPriority(priority);
        nCompatBuilder.setOngoing(true);
        nCompatBuilder.setUsesChronometer(true);
        nCompatBuilder.setWhen(when);
        nCompatBuilder.setContentIntent(contentIntent);
        if (!isEmpty(tickerText)) {
            nCompatBuilder.setTicker(tickerText);
        }

        Notification notification = nCompatBuilder.build();
        int notificationId = notificationChannelNewstatusId.hashCode();

        if (!notificationChannelNewstatusId.equals(lastNotificationChannel)) {
            // Cancel old notification
            for (String channel : notificationChannels) {
                stopNotifications(channel);
            }
        }

        compatNotificationManager.notify(notificationId, notification);
        vpnServiceCallback.onNotificationBuild(notificationId, notification);
        lastNotificationChannel = notificationChannelNewstatusId;
    }

    private PendingIntent getMainActivityIntent() {
        Intent startActivity = new Intent(context, StartActivity.class);
        return PendingIntent.getActivity(context, 0, startActivity, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private PendingIntent getStopVoidVpnIntent() {
        Intent stopVoidVpnIntent = new Intent (context, VoidVpnService.class);
        stopVoidVpnIntent.setAction(EIP_ACTION_STOP_BLOCKING_VPN);
        return PendingIntent.getService(context, 0, stopVoidVpnIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private PendingIntent getDisconnectIntent() {
        Intent disconnectVPN = new Intent(context, MainActivity.class);
        disconnectVPN.setAction(ACTION_SHOW_VPN_FRAGMENT);
        disconnectVPN.putExtra(ASK_TO_CANCEL_VPN, true);
        disconnectVPN.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(context, 0, disconnectVPN, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private PendingIntent getUserInputIntent(String needed) {
        Intent intent = new Intent(context, LaunchVPN.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra("need", needed);
        Bundle b = new Bundle();
        b.putString("need", needed);
        PendingIntent pIntent = PendingIntent.getActivity(context, 12, intent, 0);
        return pIntent;
    }

    private int getIconByConnectionStatus(ConnectionStatus level) {
        switch (level) {
            case LEVEL_CONNECTED:
                return R.drawable.ic_stat_vpn;
            case LEVEL_AUTH_FAILED:
            case LEVEL_NONETWORK:
            case LEVEL_NOTCONNECTED:
                return R.drawable.ic_stat_vpn_offline;
            case LEVEL_CONNECTING_NO_SERVER_REPLY_YET:
            case LEVEL_WAITING_FOR_USER_INPUT:
            case LEVEL_CONNECTING_SERVER_REPLIED:
                return R.drawable.ic_stat_vpn_outline;
            case LEVEL_BLOCKING:
                return R.drawable.ic_stat_vpn_blocking;
            case UNKNOWN_LEVEL:
            default:
                return R.drawable.ic_stat_vpn_offline;
        }
    }

}
