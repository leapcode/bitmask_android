package se.leap.bitmaskclient.pluggableTransports;

import android.util.Log;

import java.util.Observable;
import java.util.Observer;

import client.Client_;
import de.blinkt.openvpn.core.ConnectionStatus;
import se.leap.bitmaskclient.eip.EipStatus;

public class ObfsVpnClient implements Observer {

    public static final String SOCKS_PORT = "4430";
    public static final String SOCKS_IP = "127.0.0.1";

    private static final String TAG = ObfsVpnClient.class.getSimpleName();
    private boolean noNetwork;
    // TODO: implement error signaling go->java
    private boolean isErrorHandling;

    private final client.Client_ obfsVpnClient;
    private final Object LOCK = new Object();

    public ObfsVpnClient(Obfs4Options options) {
        obfsVpnClient = new Client_(options.udp, SOCKS_IP+":"+SOCKS_PORT, options.cert);
    }

    public void start() {
        synchronized (LOCK) {
            Log.d(TAG, "aquired LOCK");
            new Thread(() -> {
                try {
                    obfsVpnClient.start();
                } catch (Exception e) {
                    e.printStackTrace();
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
                if (isErrorHandling) {
                    isErrorHandling = false;
                    stop();
                    start();
                }
            }
        }
    }
}
