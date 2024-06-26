package se.leap.bitmaskclient.pluggableTransports;

import android.util.Log;

import client.Client;
import client.Client_;
import client.EventLogger;
import de.blinkt.openvpn.core.VpnStatus;
import de.blinkt.openvpn.core.connection.Connection;
import se.leap.bitmaskclient.base.models.Constants;
import se.leap.bitmaskclient.pluggableTransports.models.HoppingConfig;
import se.leap.bitmaskclient.pluggableTransports.models.KcpConfig;
import se.leap.bitmaskclient.pluggableTransports.models.Obfs4Options;
import se.leap.bitmaskclient.pluggableTransports.models.ObfsvpnConfig;

public class ObfsvpnClient implements EventLogger {

    public static final int PORT = 8080;
    public static final String IP = "127.0.0.1";
    private final Object LOCK = new Object();


    private static final String TAG = ObfsvpnClient.class.getSimpleName();

    public final Client_ client;

    public ObfsvpnClient(Obfs4Options options) throws IllegalStateException {

        //FIXME: use a different strategy here
        //Basically we would want to track if the more performant transport protocol (KCP?/TCP?) usage was successful
        //if so, we stick to it, otherwise we flip the flag
        boolean kcpEnabled = Constants.KCP.equals(options.transport.getProtocols()[0]);
        boolean hoppingEnabled = options.transport.getTransportType() == Connection.TransportType.OBFS4_HOP;
        if (!hoppingEnabled && (options.transport.getPorts() == null || options.transport.getPorts().length == 0)) {
            throw new IllegalStateException("obf4 based transport has no bridge ports configured");
        }
        KcpConfig kcpConfig = new KcpConfig(kcpEnabled);
        HoppingConfig hoppingConfig = new HoppingConfig(hoppingEnabled,IP+":"+PORT, options, 10, 10);
        ObfsvpnConfig obfsvpnConfig = new ObfsvpnConfig(IP+":"+PORT, hoppingConfig, kcpConfig, options.bridgeIP, options.transport.getPorts()[0], options.transport.getOptions().getCert() );
        try {
            Log.d(TAG, obfsvpnConfig.toString());
            client = Client.newFFIClient(obfsvpnConfig.toString());
            client.setEventLogger(this);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public int start() {

        synchronized (LOCK) {
            new Thread(() -> {
                try {
                    client.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
            return PORT;
        }
    }

    public void stop() {
        synchronized (LOCK) {
            try {
                client.stop();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                client.setEventLogger(null);
            }
        }
    }

    public boolean isStarted() {
        return client.isStarted();
    }

    @Override
    public void error(String s) {
        VpnStatus.logError("[obfs4-client] " + s);

    }

    @Override
    public void log(String state, String message) {
        VpnStatus.logDebug("[obfs4-client] " + state + ": " + message);
    }
}
