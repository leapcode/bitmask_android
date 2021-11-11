package se.leap.bitmaskclient.tor;

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

import se.leap.bitmaskclient.providersetup.ProviderAPI;

import static se.leap.bitmaskclient.base.utils.ConfigHelper.ensureNotOnMainThread;

public class TorServiceConnection implements Closeable {
    private static final String TAG = TorServiceConnection.class.getSimpleName();
    private final Context context;
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
