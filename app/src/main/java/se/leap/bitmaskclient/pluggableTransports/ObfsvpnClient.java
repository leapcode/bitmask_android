package se.leap.bitmaskclient.pluggableTransports;

import static se.leap.bitmaskclient.base.models.Constants.KCP;
import static se.leap.bitmaskclient.base.models.Constants.QUIC;

import android.util.Log;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import client.Client;
import client.Client_;
import client.EventLogger;
import de.blinkt.openvpn.core.VpnStatus;
import de.blinkt.openvpn.core.connection.Connection;
import se.leap.bitmaskclient.pluggableTransports.models.HoppingConfig;
import se.leap.bitmaskclient.pluggableTransports.models.KcpConfig;
import se.leap.bitmaskclient.pluggableTransports.models.Obfs4Options;
import se.leap.bitmaskclient.pluggableTransports.models.ObfsvpnConfig;
import se.leap.bitmaskclient.pluggableTransports.models.QuicConfig;

public class ObfsvpnClient implements EventLogger {

    public static final int DEFAULT_PORT = 8080;
    public static final String IP = "127.0.0.1";
    private static final String ERROR_BIND = "bind: address already in use";
    private static final String STATE_RUNNING = "RUNNING";
    private final Object LOCK = new Object();
    private final AtomicInteger currentPort = new AtomicInteger(DEFAULT_PORT);
    private CountDownLatch startCallback = null;

    private static final String TAG = ObfsvpnClient.class.getSimpleName();

    public final Client_ client;

    public ObfsvpnClient(Obfs4Options options) throws IllegalStateException {
        // each obfuscation transport has only 1 protocol
        String protocol = options.transport.getProtocols()[0];
        boolean kcpEnabled = KCP.equals(protocol);
        boolean quicEnabled = QUIC.equals(protocol);
        boolean hoppingEnabled = options.transport.getTransportType() == Connection.TransportType.OBFS4_HOP;
        if (!hoppingEnabled && (options.transport.getPorts() == null || options.transport.getPorts().length == 0)) {
            throw new IllegalStateException("obf4 based transport has no bridge ports configured");
        }
        KcpConfig kcpConfig = new KcpConfig(kcpEnabled);
        QuicConfig quicConfig = new QuicConfig(quicEnabled);
        HoppingConfig hoppingConfig = new HoppingConfig(hoppingEnabled,IP+":"+ DEFAULT_PORT, options);
        ObfsvpnConfig obfsvpnConfig = new ObfsvpnConfig(IP+":"+ DEFAULT_PORT, hoppingConfig, kcpConfig, quicConfig, options.bridgeIP, options.transport.getPorts()[0], options.transport.getOptions().getCert() );
        try {
            Log.d(TAG, "create new obfsvpn client: " + obfsvpnConfig);
            client = Client.newFFIClient(obfsvpnConfig.toString());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void start() throws RuntimeException {
        synchronized (LOCK) {
            client.setEventLogger(this);

            // this CountDownLatch stops blocking if:
            // a) obfsvpn changed its state to RUNNING
            // b) an unrecoverable error happened
            final CountDownLatch callback = new CountDownLatch(1);
            this.startCallback = callback;
            AtomicReference<Exception> err = new AtomicReference<>();
            new Thread(() -> {
                try {
                    start(0);
                } catch (RuntimeException e) {
                    // save exception and stop blocking
                    e.printStackTrace();
                    err.set(e);
                    callback.countDown();
                }
            }).start();

            try {
                boolean completedBeforeTimeout = callback.await(35, TimeUnit.SECONDS);
                Exception startException =  err.get();
                this.startCallback = null;
                if (!completedBeforeTimeout) {
                    client.setEventLogger(null);
                    throw new RuntimeException("failed to start obfsvpn: timeout error");
                } else if (startException != null) {
                    client.setEventLogger(null);
                    throw new RuntimeException("failed to start obfsvpn: ", startException);
                }
            } catch (InterruptedException e) {
                this.startCallback = null;
                client.setEventLogger(null);
                throw new RuntimeException("failed to start obfsvpn: ", e);
            }
        }
    }

    private void start(int portOffset) throws RuntimeException {
        currentPort.set(DEFAULT_PORT + portOffset);
        Log.d(TAG, "listen to 127.0.0.1:"+ (currentPort.get()));
        final CountDownLatch errOnStartCDL = new CountDownLatch(1);
        AtomicReference<Exception> err = new AtomicReference<>();
        new Thread(() -> {
            try {
                client.setProxyAddr(IP + ":" + (DEFAULT_PORT+portOffset));
                client.start();
            } catch (Exception e) {
                err.set(e);
                errOnStartCDL.countDown();
            }
        }).start();

        try {
            // wait for 250 ms, in case there is an immediate error due to misconfiguration
            // or bound ports the CountDownLatch is set to 0 and thus the return value of await is true
            boolean receivedErr = errOnStartCDL.await(250, TimeUnit.MILLISECONDS);
            if (receivedErr) {
                Exception e = err.get();
                // attempt to restart the client with a different local proxy port in case
                // there's a port binding error
                if (e != null &&
                        e.getMessage() != null &&
                        e.getMessage().contains(ERROR_BIND) &&
                        portOffset < 10) {
                    start(portOffset + 1);
                    return;
                } else {
                    resetAndThrow(new RuntimeException("Failed to start obfsvpn: " + e));
                }
            }
        } catch (InterruptedException e) {
            resetAndThrow(new RuntimeException(e));
        }
    }

    private void resetAndThrow(RuntimeException e) throws RuntimeException{
        startCallback.countDown();
        startCallback = null;
        client.setEventLogger(null);
        throw e;
    }

    public boolean stop() {
        synchronized (LOCK) {
            try {
                client.stop();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            } finally {
                client.setEventLogger(null);
            }
            return true;
        }
    }

    public int getPort() {
        return currentPort.get();
    }

    public boolean isStarted() {
        return client.isStarted();
    }

    @Override
    public void error(String s) {
        VpnStatus.logError("[obfsvpn-client] error: " + s);
    }

    @Override
    public void log(String state, String message) {
        VpnStatus.logDebug("[obfsvpn-client] " + state + ": " + message);
        CountDownLatch startCallback = this.startCallback;
        if (startCallback != null && STATE_RUNNING.equals(state)) {
            startCallback.countDown();
            this.startCallback = null;
        }
    }
}
