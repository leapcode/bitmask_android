package se.leap.bitmaskclient.tor;
/**
 * Copyright (c) 2021 LEAP Encryption Access Project and contributors
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

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
import static se.leap.bitmaskclient.tor.TorNotificationManager.TOR_SERVICE_NOTIFICATION_ID;
import static se.leap.bitmaskclient.tor.TorStatusObservable.waitUntil;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.WorkerThread;
import androidx.core.app.ServiceCompat;

import org.torproject.jni.TorService;

import java.util.concurrent.TimeoutException;

import se.leap.bitmaskclient.base.utils.PreferenceHelper;

public class TorServiceCommand {


    private static String TAG = TorServiceCommand.class.getSimpleName();

    // we bind the service before starting it as foreground service so that we avoid startForeground related RemoteExceptions
    @WorkerThread
    public static boolean startTorService(Context context, String action) throws InterruptedException {
        Log.d(TAG, "startTorService");
        try {
            if (TorStatusObservable.isCancelled()) {
                waitUntil(TorServiceCommand::isNotCancelled, 30);
            }
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        TorServiceConnection torServiceConnection = initTorServiceConnection(context);
        Log.d(TAG, "startTorService foreground: " + (torServiceConnection != null));
        boolean startedForeground = false;
        if (torServiceConnection == null) {
            return startedForeground;
        }

        try {
            Intent torServiceIntent = new Intent(context, TorService.class);
            torServiceIntent.setAction(action);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification notification = TorNotificationManager.buildTorForegroundNotification(context.getApplicationContext());
                if (notification == null) {
                    if (torServiceConnection != null) {
                        torServiceConnection.close();
                    }
                    return false;
                }
                //noinspection NewApi
                context.getApplicationContext().startForegroundService(torServiceIntent);
                ServiceCompat.startForeground(torServiceConnection.getService(), TOR_SERVICE_NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                context.getApplicationContext().startService(torServiceIntent);
            }
            startedForeground = true;
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        if (torServiceConnection != null) {
            torServiceConnection.close();
        }

        return startedForeground;
    }

    @WorkerThread
    public static void stopTorService(Context context) {
        if (TorStatusObservable.getStatus() == TorStatusObservable.TorStatus.STOPPING ||
            TorStatusObservable.getStatus() == TorStatusObservable.TorStatus.OFF) {
            return;
        }
        TorStatusObservable.markCancelled();

        try {
            Intent torServiceIntent = new Intent(context, TorService.class);
            torServiceIntent.setAction(TorService.ACTION_STOP);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //noinspection NewApi
                context.getApplicationContext().startService(torServiceIntent);
            } else {
                context.getApplicationContext().startService(torServiceIntent);
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public static void stopTorServiceAsync(Context context) {
        if (!TorStatusObservable.isRunning()) {
            return;
        }
        TorStatusObservable.markCancelled();
        new Thread(() -> stopTorService(context)).start();
    }

    @WorkerThread
    public static int getHttpTunnelPort(Context context) {
        try {
            TorServiceConnection torServiceConnection = initTorServiceConnection(context);
            if (torServiceConnection != null) {
                int tunnelPort = torServiceConnection.getService().getHttpTunnelPort();
                torServiceConnection.close();
                return tunnelPort;
            }
        } catch (InterruptedException | IllegalStateException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @WorkerThread
    public static int getSocksProxyPort(Context context) {
        try {
            TorServiceConnection torServiceConnection = initTorServiceConnection(context);
            if (torServiceConnection != null) {
                int tunnelPort = torServiceConnection.getService().getSocksPort();
                torServiceConnection.close();
                return tunnelPort;
            }
        } catch (InterruptedException | IllegalStateException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private static boolean isNotCancelled() {
        return !TorStatusObservable.isCancelled();
    }


    private static TorServiceConnection initTorServiceConnection(Context context) throws InterruptedException, IllegalStateException {
        Log.d(TAG, "initTorServiceConnection");
        if (PreferenceHelper.getUseSnowflake()) {
            Log.d(TAG, "serviceConnection is still null");
            if (!TorService.hasClientTransportPlugin()) {
                TorService.setClientTransportPlugin(new ClientTransportPlugin(context.getApplicationContext()));
            }
            return new TorServiceConnection(context);
        }
        return null;
    }
}
