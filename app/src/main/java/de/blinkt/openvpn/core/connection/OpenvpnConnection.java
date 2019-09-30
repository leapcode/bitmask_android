package de.blinkt.openvpn.core.connection;

/**
 * Created by cyberta on 11.03.19.
 */

public class OpenvpnConnection extends Connection {

    @Override
    public TransportType getTransportType() {
        return TransportType.OPENVPN;
    }
}
