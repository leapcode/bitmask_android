package se.leap.bitmaskclient.eip;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.VpnStatus;
import se.leap.bitmaskclient.Dashboard;
import se.leap.bitmaskclient.R;

import static android.os.Build.VERSION_CODES.O;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_START_ALWAYS_ON_EIP;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_START_BLOCKING_VPN;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_STOP_BLOCKING_VPN;
import static se.leap.bitmaskclient.Constants.EIP_IS_ALWAYS_ON;
import static se.leap.bitmaskclient.Constants.SHARED_PREFERENCES;


public class VoidVpnService extends VpnService implements Observer {

    static final String TAG = VoidVpnService.class.getSimpleName();
    static ParcelFileDescriptor fd;
    static Thread thread;
    private final int ALWAYS_ON_MIN_API_LEVEL = Build.VERSION_CODES.N;
    private static final String STATE_ESTABLISH = "ESTABLISHVOIDVPN";
    public static final String NOTIFICATION_CHANNEL_NEWSTATUS_ID = "bitmask_void_vpn_news";
    private EipStatus eipStatus;
    NotificationManager notificationManager;
    NotificationManagerCompat compatNotificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager =  (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        compatNotificationManager = NotificationManagerCompat.from(this);
        eipStatus = EipStatus.getInstance();
        eipStatus.addObserver(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : "";
        if (action.equals(EIP_ACTION_START_BLOCKING_VPN)) {
            thread = new Thread(new Runnable() {
                public void run() {
                    establishBlockingVpn();
                    SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
                    preferences.edit().putBoolean(EIP_IS_ALWAYS_ON, false).commit();
                    Log.d(TAG, "start blocking vpn profile - always on = false");
                }
            });
            thread.run();
        } else if (action.equals("android.net.VpnService") && Build.VERSION.SDK_INT >= ALWAYS_ON_MIN_API_LEVEL) {
            //only always-on feature triggers this
            thread = new Thread(new Runnable() {
                public void run() {
                    establishBlockingVpn();
                    SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
                    preferences.edit().putBoolean(EIP_IS_ALWAYS_ON, true).commit();
                    requestVpnWithLastSelectedProfile();
                    Log.d(TAG, "start blocking vpn profile - always on = true");
                }
            });
            thread.run();
        } else if (action.equals(EIP_ACTION_STOP_BLOCKING_VPN)) {
            stop();
        }
        return START_STICKY;
    }

    @Override
    public void onRevoke() {
        super.onRevoke();
        closeFd();
    }

    @TargetApi(O)
    private void createNotificationChannel() {

        // Connection status change messages
        CharSequence name = getString(R.string.channel_name_status);
        NotificationChannel mChannel = new NotificationChannel(VoidVpnService.NOTIFICATION_CHANNEL_NEWSTATUS_ID,
                name, NotificationManagerCompat.IMPORTANCE_DEFAULT);

        mChannel.setDescription(getString(R.string.channel_description_status));
        mChannel.enableLights(true);

        mChannel.setLightColor(Color.BLUE);
        notificationManager.createNotificationChannel(mChannel);
    }


    private void stop() {
        stopNotifications();
        if (thread != null) {
            thread.interrupt();
        }
        closeFd();
    }

    public static boolean isRunning() throws NullPointerException {
        return thread.isAlive() && fd != null;
    }

    private static void closeFd() {
        try {
            if (fd != null)
                fd.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Builder prepareBlockingVpnProfile() {
        Builder builder = new Builder();
        builder.setSession("Blocking until running");
        builder.addRoute("0.0.0.0", 1);
        builder.addRoute("192.168.1.0", 24);
        builder.addDnsServer("10.42.0.1");
        builder.addAddress("10.42.0.8", 16);
        return builder;

    }

    private void establishBlockingVpn() {
        try {
            VpnStatus.logInfo(getString(R.string.void_vpn_establish));
            VpnStatus.updateStateString(STATE_ESTABLISH, "",
                    R.string.void_vpn_establish, ConnectionStatus.LEVEL_BLOCKING);
            Builder builder = prepareBlockingVpnProfile();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)  {
                builder.addDisallowedApplication(getPackageName());
            }

            fd = builder.establish();
        } catch (Exception e) {
            // Catch any exception
            e.printStackTrace();
            VpnStatus.logError(R.string.void_vpn_error_establish);
        }
    }

    private void requestVpnWithLastSelectedProfile() {
        Intent startEIP = new Intent(getApplicationContext(), EIP.class);
        startEIP.setAction(EIP_ACTION_START_ALWAYS_ON_EIP);
        getApplicationContext().startService(startEIP);
    }

    @Override
    public void update(Observable observable, Object arg) {
        if (observable instanceof EipStatus) {
            eipStatus = (EipStatus) observable;
        }

        if (thread == null) {
            return;
        }

        if (eipStatus.isBlockingVpnEstablished()) {
            showNotification(getString(eipStatus.getLocalizedResId()),
                    getString(eipStatus.getLocalizedResId()), eipStatus.getLevel());
        } else {
            stopNotifications();
        }
    }

    private void stopNotifications() {
        stopForeground(true);
        compatNotificationManager.cancel(NOTIFICATION_CHANNEL_NEWSTATUS_ID.hashCode());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_NEWSTATUS_ID) != null) {
            notificationManager.deleteNotificationChannel(NOTIFICATION_CHANNEL_NEWSTATUS_ID);
        }
    }

    /**
     * @param msg
     * @param tickerText
     * @param status
     */
    private void showNotification(final String msg, String tickerText, ConnectionStatus status) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        int icon = getIconByConnectionStatus(status);
        NotificationCompat.Builder nCompatBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_NEWSTATUS_ID);

        nCompatBuilder.setContentTitle(getString(R.string.notifcation_title_bitmask, getString(R.string.void_vpn_title)));
        nCompatBuilder.setCategory(NotificationCompat.CATEGORY_SERVICE);
        nCompatBuilder.setLocalOnly(true);
        nCompatBuilder.setContentText(msg);
        nCompatBuilder.setOnlyAlertOnce(true);
        nCompatBuilder.setSmallIcon(icon);
        if (tickerText != null && !tickerText.equals("")) {
            nCompatBuilder.setTicker(tickerText);
        }

        nCompatBuilder.setContentIntent(getDashboardIntent());
        //TODO: implement extra Dashboard.ACTION_ASK_TO_CANCEL_BLOCKING_VPN
        NotificationCompat.Action.Builder builder = new NotificationCompat.Action.Builder(R.drawable.ic_menu_close_clear_cancel, getString(R.string.vpn_button_turn_off_blocking), getStopVoidVpnIntent());
        nCompatBuilder.addAction(builder.build());

        Notification notification = nCompatBuilder.build();
        int notificationId = NOTIFICATION_CHANNEL_NEWSTATUS_ID.hashCode();
        compatNotificationManager.notify(notificationId, notification);
        startForeground(notificationId, notification);
    }

    private PendingIntent getDashboardIntent() {
        Intent startDashboard = new Intent(this, Dashboard.class);
        startDashboard.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(this, 0, startDashboard, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private PendingIntent getStopVoidVpnIntent() {
        Intent stopVoidVpnIntent = new Intent (this, VoidVpnService.class);
        stopVoidVpnIntent.setAction(EIP_ACTION_STOP_BLOCKING_VPN);
        return PendingIntent.getService(this, 0, stopVoidVpnIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    //TODO: replace with getIconByEipLevel(EipLevel level)
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
