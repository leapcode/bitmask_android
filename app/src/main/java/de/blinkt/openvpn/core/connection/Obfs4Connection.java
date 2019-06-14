package de.blinkt.openvpn.core.connection;

import se.leap.bitmaskclient.pluggableTransports.DispatcherOptions;

import static se.leap.bitmaskclient.pluggableTransports.Dispatcher.DISPATCHER_IP;
import static se.leap.bitmaskclient.pluggableTransports.Dispatcher.DISPATCHER_PORT;

/**
 * Created by cyberta on 08.03.19.
 */

public class Obfs4Connection extends Connection {

    private static final String TAG = Obfs4Connection.class.getName();
    private DispatcherOptions options;

    public Obfs4Connection(DispatcherOptions options) {
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

    @Deprecated
    public Obfs4Connection() {
        setUseUdp(false);
        setServerName(DISPATCHER_IP);
        setServerPort(DISPATCHER_PORT);
        setProxyName("");
        setProxyPort("");
        setProxyAuthUser(null);
        setProxyAuthPassword(null);
        setProxyType(ProxyType.NONE);
        setUseProxyAuth(false);    }

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


    public DispatcherOptions getDispatcherOptions() {
        return options;
    }

}
