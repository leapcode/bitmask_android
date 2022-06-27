package se.leap.bitmaskclient.pluggableTransports;

import android.util.Log;

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicBoolean;

import client.Client_;
import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.VpnStatus;
import se.leap.bitmaskclient.eip.EipStatus;

public class ObfsVpnClient implements Observer, client.EventLogger {

    public static final String SOCKS_PORT = "4430";
    public static final String SOCKS_IP = "127.0.0.1";

    private static final String TAG = ObfsVpnClient.class.getSimpleName();
    private volatile boolean noNetwork;
    // TODO: implement error signaling go->java
    private AtomicBoolean isErrorHandling = new AtomicBoolean(false);

    private final client.Client_ obfsVpnClient;
    private final Object LOCK = new Object();

    public ObfsVpnClient(Obfs4Options options) {
        obfsVpnClient = new Client_(options.udp, SOCKS_IP+":"+SOCKS_PORT, options.cert);
        obfsVpnClient.setEventLogger(this);
    }

    public void start() {
        synchronized (LOCK) {
            Log.d(TAG, "aquired LOCK");
            new Thread(() -> {
                try {
                    obfsVpnClient.start();
                } catch (Exception e) {
                    e.printStackTrace();
                    VpnStatus.logError("[obfsvpn] " + e.getLocalizedMessage());
                    if (noNetwork) {
                        isErrorHandling.set(true);
                    }
                }
            }).start();

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "returning LOCK after 500 ms");
        }
    }

    public void stop() {
        synchronized (LOCK) {
            Log.d(TAG, "stopping obfsVpnClient...");
            try {
                obfsVpnClient.stop();
                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
                VpnStatus.logError("[obfsvpn] " + e.getLocalizedMessage());
            }
            isErrorHandling.set(false);
            Log.d(TAG, "stopping obfsVpnClient releasing LOCK ...");
        }
    }

    public boolean isStarted() {
        return obfsVpnClient.isStarted();
    }

    @Override
    public void update(Observable observable, Object arg) {
        if (observable instanceof EipStatus) {
            EipStatus status = (EipStatus) observable;
            //This doesn't do anything yet, since the error handling is still missing
            if (status.getLevel() == ConnectionStatus.LEVEL_NONETWORK) {
                noNetwork = true;
            } else {
                noNetwork = false;
                if (isErrorHandling.getAndSet(false)) {
                    stop();
                    start();
                }
            }
        }
    }

    @Override
    public void error(String s) {
        VpnStatus.logError("[obfsvpn] " + s);
    }

    @Override
    public void log(String state, String message) {
        VpnStatus.logDebug("[obfsvpn] " + state + " " + message);
    }

}
