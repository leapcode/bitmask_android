package se.leap.bitmaskclient.pluggableTransports;

import de.blinkt.openvpn.core.connection.Connection;
import de.blinkt.openvpn.core.connection.Obfs4Connection;
import de.blinkt.openvpn.core.connection.Obfs4HopConnection;

public class PtClientBuilder {
    public static PtClientInterface getPtClient(Connection connection) throws IllegalStateException {
        switch (connection.getTransportType()) {
            case OBFS4:
                return new ObfsVpnClient(((Obfs4Connection) connection).getObfs4Options());
            case OBFS4_HOP:
                return new HoppingObfsVpnClient(((Obfs4HopConnection) connection).getObfs4Options());
            default:
                throw new IllegalStateException("Unexpected pluggable transport " + connection.getTransportType());
        }
    }
}
