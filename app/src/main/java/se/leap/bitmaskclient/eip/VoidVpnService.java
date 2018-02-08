/**
 * Copyright (c) 2013 LEAP Encryption Access Project and contributers
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

import android.app.Notification;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.VpnStatus;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.VpnNotificationManager;

import static se.leap.bitmaskclient.Constants.EIP_ACTION_START_ALWAYS_ON_VPN;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_START_BLOCKING_VPN;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_STOP_BLOCKING_VPN;
import static se.leap.bitmaskclient.Constants.EIP_IS_ALWAYS_ON;
import static se.leap.bitmaskclient.Constants.SHARED_PREFERENCES;


public class VoidVpnService extends VpnService implements Observer, VpnNotificationManager.VpnServiceCallback {

    static final String TAG = VoidVpnService.class.getSimpleName();
    static ParcelFileDescriptor fd;
    static Thread thread;
    private final int ALWAYS_ON_MIN_API_LEVEL = Build.VERSION_CODES.N;
    private static final String STATE_ESTABLISH = "ESTABLISHVOIDVPN";
    public static final String NOTIFICATION_CHANNEL_NEWSTATUS_ID = "bitmask_void_vpn_news";
    private EipStatus eipStatus;
    private VpnNotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        eipStatus = EipStatus.getInstance();
        eipStatus.addObserver(this);
        notificationManager = new VpnNotificationManager(this, this);
        notificationManager.createVoidVpnNotificationChannel();
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

    private void stop() {
        notificationManager.stopNotifications(NOTIFICATION_CHANNEL_NEWSTATUS_ID);
        notificationManager.deleteNotificationChannel(NOTIFICATION_CHANNEL_NEWSTATUS_ID);
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
        startEIP.setAction(EIP_ACTION_START_ALWAYS_ON_VPN);
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
            notificationManager.buildVoidVpnNotification(
                    getString(eipStatus.getLocalizedResId()),
                    getString(eipStatus.getLocalizedResId()),
                    eipStatus.getLevel());
        } else {
            notificationManager.stopNotifications(NOTIFICATION_CHANNEL_NEWSTATUS_ID);
        }
    }

    @Override
    public void onNotificationBuild(int notificationId, Notification notification) {
        startForeground(notificationId, notification);
    }

    @Override
    public void onNotificationStop() {
        stopForeground(true);
    }

}
