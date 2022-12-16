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

import static se.leap.bitmaskclient.base.utils.ConfigHelper.ensureNotOnMainThread;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.WorkerThread;

import org.torproject.jni.TorService;

import java.io.Closeable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TorServiceConnection implements Closeable {
    private static final String TAG = TorServiceConnection.class.getSimpleName();
    private Context context;
    private ServiceConnection serviceConnection;
    private TorService torService;

    @WorkerThread
    public TorServiceConnection(Context context) throws InterruptedException, IllegalStateException {
        this.context = context;
        ensureNotOnMainThread(context);
        initSynchronizedServiceConnection(context);
    }

    @Override
    public void close() {
        context.unbindService(serviceConnection);
        context = null;
        serviceConnection = null;
        torService = null;
    }

    private void initSynchronizedServiceConnection(final Context context) throws InterruptedException {
        Log.d(TAG, "initSynchronizedServiceConnection");
        final BlockingQueue<TorService> blockingQueue = new LinkedBlockingQueue<>(1);
        this.serviceConnection = new ServiceConnection() {
            volatile boolean mConnectedAtLeastOnce = false;

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                if (!mConnectedAtLeastOnce) {
                    mConnectedAtLeastOnce = true;
                    Log.d(TAG, "onServiceConnected");
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
