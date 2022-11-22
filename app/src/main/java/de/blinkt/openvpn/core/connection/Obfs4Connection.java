package de.blinkt.openvpn.core.connection;

import static se.leap.bitmaskclient.base.utils.ConfigHelper.ObfsVpnHelper.useObfsVpn;
import static se.leap.bitmaskclient.pluggableTransports.ShapeshifterClient.DISPATCHER_IP;
import static se.leap.bitmaskclient.pluggableTransports.ShapeshifterClient.DISPATCHER_PORT;

import se.leap.bitmaskclient.pluggableTransports.Obfs4Options;
import se.leap.bitmaskclient.pluggableTransports.ObfsVpnClient;


/**
 * Created by cyberta on 08.03.19.
 */

public class Obfs4Connection extends Connection {

    private static final String TAG = Obfs4Connection.class.getName();
    private Obfs4Options options;

    public Obfs4Connection(Obfs4Options options) {
        if (useObfsVpn()) {
            setServerName(options.remoteIP);
            setServerPort(options.remotePort);
            setProxyName(ObfsVpnClient.SOCKS_IP);
            setProxyPort(String.valueOf(ObfsVpnClient.SOCKS_PORT.get()));
            setProxyType(ProxyType.SOCKS5);
        } else {
            setServerName(DISPATCHER_IP);
            setServerPort(DISPATCHER_PORT);
            setProxyName("");
            setProxyPort("");
            setProxyType(ProxyType.NONE);
        }
        // while udp/kcp might be used on the wire,
        // we don't use udp for openvpn in case of a obfs4 connection
        setUseUdp(false);
        setProxyAuthUser(null);
        setProxyAuthPassword(null);
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
