package de.blinkt.openvpn.core.connection;

import se.leap.bitmaskclient.pluggableTransports.ObfsvpnClient;
import se.leap.bitmaskclient.pluggableTransports.models.Obfs4Options;


/**
 * Created by cyberta on 08.03.19.
 */

public class Obfs4Connection extends Connection {

    private static final String TAG = Obfs4Connection.class.getName();
    private Obfs4Options options;

    public Obfs4Connection(Obfs4Options options) {
        setServerName(ObfsvpnClient.IP);
        setServerPort(String.valueOf(ObfsvpnClient.PORT));
        setUseUdp(true);
        setProxyType(ProxyType.NONE);
        setProxyName("");
        setProxyPort("");
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
        return options.transport.getTransportType();
    }


    public Obfs4Options getObfs4Options() {
        return options;
    }

}
