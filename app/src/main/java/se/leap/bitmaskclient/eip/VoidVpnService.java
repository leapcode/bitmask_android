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

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED;
import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_START_ALWAYS_ON_VPN;
import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_START_BLOCKING_VPN;
import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_STOP_BLOCKING_VPN;
import static se.leap.bitmaskclient.base.utils.ConfigHelper.getProviderFormattedString;

import android.app.Notification;
import android.content.Intent;
import android.net.VpnService;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.system.OsConstants;
import android.util.Log;

import androidx.core.app.ServiceCompat;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.VpnStatus;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;


public class VoidVpnService extends VpnService implements PropertyChangeListener, VpnNotificationManager.VpnServiceCallback {

    static final String TAG = VoidVpnService.class.getSimpleName();
    private ParcelFileDescriptor fd;
    private final int ALWAYS_ON_MIN_API_LEVEL = Build.VERSION_CODES.N;
    private static final String STATE_ESTABLISH = "ESTABLISHVOIDVPN";
    public static final String NOTIFICATION_CHANNEL_NEWSTATUS_ID = "bitmask_void_vpn_news";
    private EipStatus eipStatus;
    private VpnNotificationManager notificationManager;
    private HandlerThread handlerThread;
    private Handler handler;

    private final IBinder binder = new VoidVpnServiceBinder();

    public class VoidVpnServiceBinder extends Binder {
        VoidVpnService getService() {
            // Return this instance of LocalService so clients can call public methods
            return VoidVpnService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }



    @Override
    public void onCreate() {
        super.onCreate();
        eipStatus = EipStatus.getInstance();
        eipStatus.addObserver(this);
        notificationManager = new VpnNotificationManager(this);
        handlerThread = new HandlerThread("VoidVpnServiceHandlerThread", Thread.NORM_PRIORITY);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : "";
        handler.removeCallbacksAndMessages(null);
        if (EIP_ACTION_START_BLOCKING_VPN.equals(action)) {
            handler.post(() -> {
                establishBlockingVpn();
                PreferenceHelper.isAlwaysOnSync(false);
                Log.d(TAG, "start blocking vpn profile - always on = false");
            });
        } else if (intent == null || "android.net.VpnService".equals(action) && Build.VERSION.SDK_INT >= ALWAYS_ON_MIN_API_LEVEL) {
            //only always-on feature triggers this
            startWithForegroundNotification();
            handler.post(() -> {
                establishBlockingVpn();
                PreferenceHelper.isAlwaysOnSync(true);
                requestVpnWithLastSelectedProfile();
                Log.d(TAG, "start blocking vpn profile - always on = true");
            });
        } else if (EIP_ACTION_STOP_BLOCKING_VPN.equals(action)) {
            stop();
        }
        return START_STICKY;
    }

    @Override
    public void onRevoke() {
        super.onRevoke();
        stop();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        notificationManager.cancelAll();
        eipStatus.deleteObserver(this);
    }

    private void stop() {
        handlerThread.interrupt();
        closeFd();
        VpnStatus.updateStateString("NOPROCESS", "BLOCKING VPN STOPPED", R.string.state_noprocess, ConnectionStatus.LEVEL_NOTCONNECTED);
        stopForeground(true);
        stopSelf();
    }

    private void closeFd() {
        try {
            if (fd != null) {
                fd.close();
                fd = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Builder prepareBlockingVpnProfile() {
        Builder builder = new Builder();
        builder.setSession("Blocking until running");
        builder.addRoute("0.0.0.0", 0);
        builder.addRoute("192.168.1.0", 24);
        builder.addDnsServer("10.42.0.1");
        builder.addAddress("10.42.0.8", 16);
        builder.addRoute("::",0);
        builder.addAddress("fc00::", 7);

        allowAllAFFamilies(builder);

        return builder;

    }

    private void allowAllAFFamilies(Builder builder) {
        builder.allowFamily(OsConstants.AF_INET);
        builder.allowFamily(OsConstants.AF_INET6);
    }

    private void establishBlockingVpn() {
        try {
            VpnStatus.logInfo(getProviderFormattedString(getResources(), R.string.void_vpn_establish));
            VpnStatus.updateStateString(STATE_ESTABLISH, "",
                    R.string.void_vpn_establish, ConnectionStatus.LEVEL_BLOCKING);
            Builder builder = prepareBlockingVpnProfile();
            builder.addDisallowedApplication(getPackageName());

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
    public void propertyChange(PropertyChangeEvent evt) {
        if (EipStatus.PROPERTY_CHANGE.equals(evt.getPropertyName())) {
            eipStatus = (EipStatus) evt.getNewValue();
        }
        if (handlerThread.isInterrupted() || !handlerThread.isAlive()) {
            return;
        }

        if (eipStatus.isBlockingVpnEstablished()) {
            String blockingMessage = getProviderFormattedString(getResources(), eipStatus.getLocalizedResId());
            notificationManager.buildVoidVpnNotification(
                    blockingMessage,
                    blockingMessage,
                    eipStatus.getLevel(),
                    null
            );
        } else {
            stopForeground(true);
        }
    }

    @Override
    public void onNotificationBuild(int notificationId, Notification notification) {
        ServiceCompat.startForeground(this, notificationId, notification, FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED);
    }

    public void startWithForegroundNotification() {
        notificationManager.createVoidVpnNotificationChannel();
        String message = getString(R.string.state_disconnected);
        notificationManager.buildVoidVpnNotification(
                message,
                message,
                eipStatus.getLevel(),
                this
        );
    }

}
