package se.leap.bitmaskclient.pluggableTransports;

import java.io.Serializable;

import se.leap.bitmaskclient.base.models.Transport;

public class Obfs4Options implements Serializable {
    public String gatewayIP;
    public Transport transport;

    public Obfs4Options(String gatewayIP,
                        Transport transport) {
        this.gatewayIP = gatewayIP;
        this.transport = transport;
    }
}
