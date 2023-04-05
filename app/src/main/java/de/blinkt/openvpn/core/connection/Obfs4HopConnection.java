package de.blinkt.openvpn.core.connection;

import se.leap.bitmaskclient.pluggableTransports.HoppingObfsVpnClient;
import se.leap.bitmaskclient.pluggableTransports.Obfs4Options;


/**
 * Created by cyberta on 08.03.19.
 */

public class Obfs4HopConnection extends Connection {

    private static final String TAG = Obfs4HopConnection.class.getName();
    private Obfs4Options options;

    public Obfs4HopConnection(Obfs4Options options) {
        setServerName(HoppingObfsVpnClient.IP);
        setServerPort(String.valueOf(HoppingObfsVpnClient.PORT));
        setProxyName("");
        setProxyPort("");
        setProxyType(ProxyType.NONE);


        setUseUdp(true);
        setProxyAuthUser(null);
        setProxyAuthPassword(null);
        setUseProxyAuth(false);
        this.options = options;
    }

    @Override
    public Connection clone() throws CloneNotSupportedException {
        Obfs4HopConnection connection = (Obfs4HopConnection) super.clone();
        connection.options = this.options;
        return connection;
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.OBFS4_HOP;
    }


    public Obfs4Options getObfs4Options() {
        return options;
    }

}
