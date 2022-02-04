package de.blinkt.openvpn.core.connection;

import se.leap.bitmaskclient.pluggableTransports.Obfs4Options;

import static se.leap.bitmaskclient.pluggableTransports.Shapeshifter.DISPATCHER_IP;
import static se.leap.bitmaskclient.pluggableTransports.Shapeshifter.DISPATCHER_PORT;


/**
 * Created by cyberta on 08.03.19.
 */

public class Obfs4Connection extends Connection {

    private static final String TAG = Obfs4Connection.class.getName();
    private Obfs4Options options;

    public Obfs4Connection(Obfs4Options options) {
        setUseUdp(false);
        setServerName(DISPATCHER_IP);
        setServerPort(DISPATCHER_PORT);
        setProxyName("");
        setProxyPort("");
        setProxyAuthUser(null);
        setProxyAuthPassword(null);
        setProxyType(ProxyType.NONE);
        setUseProxyAuth(false);
        this.options = options;
    }

    @Override
    public Connection clone() throws CloneNotSupportedException {
        Obfs4Connection connection = (Obfs4Connection) super.clone();
        connection.options = this.options;
        return connection;
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.OBFS4;
    }


    public Obfs4Options getDispatcherOptions() {
        return options;
    }

}
