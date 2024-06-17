package se.leap.bitmaskclient.pluggableTransports.models;

import java.io.Serializable;

import se.leap.bitmaskclient.base.models.Transport;

public class Obfs4Options implements Serializable {
    public String bridgeIP;
    public Transport transport;

    public Obfs4Options(String bridgeIP,
                        Transport transport) {
        this.bridgeIP = bridgeIP;
        this.transport = transport;
    }
}
