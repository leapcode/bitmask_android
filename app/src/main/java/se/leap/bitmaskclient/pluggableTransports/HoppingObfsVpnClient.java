package se.leap.bitmaskclient.pluggableTransports;

import static de.blinkt.openvpn.core.connection.Connection.TransportProtocol.KCP;

import client.Client;
import client.HopClient;
import de.blinkt.openvpn.core.VpnStatus;
import se.leap.bitmaskclient.base.models.Constants;

public class HoppingObfsVpnClient implements PtClientInterface {

    public static final int PORT = 8080;
    public static final String IP = "127.0.0.1";

    public final HopClient client;

    public HoppingObfsVpnClient(Obfs4Options options) throws IllegalStateException {

        //FIXME: use a different strategy here
        //Basically we would want to track if the more performant transport protocol (KCP?/TCP?) usage was successful
        //if so, we stick to it, otherwise we flip the flag
        boolean kcp = Constants.KCP.equals(options.transport.getProtocols()[0]);

        if (options.transport.getOptions().getEndpoints() == null) {
            throw new IllegalStateException("No Endpoints for hopping pt detected!");
        }

        HoppingConfig hoppingConfig = new HoppingConfig(kcp,IP+":"+PORT, options.transport, 10, 10);
        try {
            client = Client.newFFIHopClient(hoppingConfig.toString());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int start() {
        try {
            client.setEventLogger(this);
            return client.start() ? PORT : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public void stop() {
        try {
            client.stop();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client.setEventLogger(null);
        }
    }

    @Override
    public boolean isStarted() {
        return client.isStarted();
    }

    @Override
    public void error(String s) {
        VpnStatus.logError("[hopping-obfs4] " + s);
    }

    @Override
    public void log(String state, String message) {
        VpnStatus.logDebug("[hopping-obfs4] " + state + ": " + message);
    }
}
