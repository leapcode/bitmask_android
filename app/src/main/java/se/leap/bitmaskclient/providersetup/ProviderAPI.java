/**
 * Copyright (c) 2017 LEAP Encryption Access Project and contributers
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
package se.leap.bitmaskclient.providersetup;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.torproject.jni.TorService;

import java.io.Closeable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import de.blinkt.openvpn.core.NetworkUtils;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.providersetup.connectivity.OkHttpClientGenerator;
import se.leap.bitmaskclient.tor.ClientTransportPlugin;
import se.leap.bitmaskclient.tor.TorNotificationManager;

import static se.leap.bitmaskclient.base.models.Constants.SHARED_PREFERENCES;
import static se.leap.bitmaskclient.base.utils.ConfigHelper.ensureNotOnMainThread;
import static se.leap.bitmaskclient.tor.TorNotificationManager.TOR_SERVICE_NOTIFICATION_ID;

/**
 * Implements HTTP api methods (encapsulated in {{@link ProviderApiManager}})
 * used to manage communications with the provider server.
 * <p/>
 * It's an JobIntentService because it downloads data from the Internet, so it operates in the background.
 *
 * @author parmegv
 * @author MeanderingCode
 * @author cyberta
 */

public class ProviderAPI extends JobIntentService implements ProviderApiManagerBase.ProviderApiServiceCallback {

    /**
     * Unique job ID for this service.
     */
    static final int JOB_ID = 161375;

    final public static String
            TAG = ProviderAPI.class.getSimpleName(),
            SET_UP_PROVIDER = "setUpProvider",
            UPDATE_PROVIDER_DETAILS = "updateProviderDetails",
            DOWNLOAD_GEOIP_JSON = "downloadGeoIpJson",
            SIGN_UP = "srpRegister",
            LOG_IN = "srpAuth",
            LOG_OUT = "logOut",
            DOWNLOAD_VPN_CERTIFICATE = "downloadUserAuthedVPNCertificate",
            UPDATE_INVALID_VPN_CERTIFICATE = "ProviderAPI.UPDATE_INVALID_VPN_CERTIFICATE",
            PARAMETERS = "parameters",
            RECEIVER_KEY = "receiver",
            ERRORS = "errors",
            ERRORID = "errorId",
            BACKEND_ERROR_KEY = "error",
            BACKEND_ERROR_MESSAGE = "message",
            USER_MESSAGE = "userMessage",
            DOWNLOAD_SERVICE_JSON = "ProviderAPI.DOWNLOAD_SERVICE_JSON";

    final public static int
            SUCCESSFUL_LOGIN = 3,
            FAILED_LOGIN = 4,
            SUCCESSFUL_SIGNUP = 5,
            FAILED_SIGNUP = 6,
            SUCCESSFUL_LOGOUT = 7,
            LOGOUT_FAILED = 8,
            CORRECTLY_DOWNLOADED_VPN_CERTIFICATE = 9,
            INCORRECTLY_DOWNLOADED_VPN_CERTIFICATE = 10,
            PROVIDER_OK = 11,
            PROVIDER_NOK = 12,
            CORRECTLY_DOWNLOADED_EIP_SERVICE = 13,
            INCORRECTLY_DOWNLOADED_EIP_SERVICE = 14,
            CORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE = 15,
            INCORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE = 16,
            CORRECTLY_DOWNLOADED_GEOIP_JSON = 17,
            INCORRECTLY_DOWNLOADED_GEOIP_JSON = 18;

    ProviderApiManager providerApiManager;
    private volatile TorServiceConnection torServiceConnection;

    //TODO: refactor me, please!
    //used in insecure flavor only
    @SuppressLint("unused")
    public static boolean lastDangerOn() {
        return ProviderApiManager.lastDangerOn();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        providerApiManager = initApiManager();
    }

    /**
     * Convenience method for enqueuing work in to this service.
     */
    static void enqueueWork(Context context, Intent work) {
        try {
            ProviderAPI.enqueueWork(context, ProviderAPI.class, JOB_ID, work);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onHandleWork(@NonNull Intent command) {
        providerApiManager.handleIntent(command);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (torServiceConnection != null) {
            torServiceConnection.close();
            torServiceConnection = null;
        }
    }

    @Override
    public void broadcastEvent(Intent intent) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public boolean isConnectedToWifi() {
        return NetworkUtils.isConnectedToWifi(getApplicationContext());
    }

    @Override
    public void startTorService() throws InterruptedException, IllegalStateException {
        initTorServiceConnection(this);
        Intent torServiceIntent = new Intent(this, TorService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification notification = TorNotificationManager.buildTorForegroundNotification(getApplicationContext());
            //noinspection NewApi
            getApplicationContext().startForegroundService(torServiceIntent);
            torServiceConnection.torService.startForeground(TOR_SERVICE_NOTIFICATION_ID, notification);
        } else {
            getApplicationContext().startService(torServiceIntent);
        }
    }


    @Override
    public int getTorHttpTunnelPort() {
        try {
            initTorServiceConnection(this);
            if (torServiceConnection != null) {
                int tunnelPort = torServiceConnection.torService.getHttpTunnelPort();
                torServiceConnection.close();
                torServiceConnection = null;
                return tunnelPort;
            }
        } catch (InterruptedException | IllegalStateException e) {
            e.printStackTrace();
        }

        return -1;
    }

    private ProviderApiManager initApiManager() {
        SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        OkHttpClientGenerator clientGenerator = new OkHttpClientGenerator(getResources());
        return new ProviderApiManager(preferences, getResources(), clientGenerator, this);
    }

    /**
     * Assigns a new TorServiceConnection to ProviderAPI's member variable torServiceConnection.
     * Only one thread at a time can create the service connection, that will be shared between threads
     *
     * @throws InterruptedException  thrown if thread gets interrupted
     * @throws IllegalStateException thrown if this method was not called from a background thread
     */
    private void initTorServiceConnection(Context context) throws InterruptedException, IllegalStateException {
        if (PreferenceHelper.getUseBridges(context)) {
            if (torServiceConnection == null) {
                Log.d(TAG, "serviceConnection is still null");
                if (!TorService.hasClientTransportPlugin()) {
                    TorService.setClientTransportPlugin(new ClientTransportPlugin(context.getApplicationContext()));
                }
                torServiceConnection = new TorServiceConnection(context);
            }
        }
    }

    public static class TorServiceConnection implements Closeable {
        private final Context context;
        private ServiceConnection serviceConnection;
        private TorService torService;

        TorServiceConnection(Context context) throws InterruptedException, IllegalStateException {
            this.context = context;
            ensureNotOnMainThread(context);
            Log.d(TAG, "initSynchronizedServiceConnection!");
            initSynchronizedServiceConnection(context);
        }

        @Override
        public void close() {
            context.unbindService(serviceConnection);
        }

        private void initSynchronizedServiceConnection(final Context context) throws InterruptedException {
            final BlockingQueue<TorService> blockingQueue = new LinkedBlockingQueue<>(1);
            this.serviceConnection = new ServiceConnection() {
                volatile boolean mConnectedAtLeastOnce = false;

                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    if (!mConnectedAtLeastOnce) {
                        mConnectedAtLeastOnce = true;
                        try {
                            TorService.LocalBinder binder = (TorService.LocalBinder) service;
                            blockingQueue.put(binder.getService());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    torService = null;
                }
            };
            Intent intent = new Intent(context, TorService.class);
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            torService = blockingQueue.take();
        }

        public TorService getService() {
            return torService;
        }
    }

}
