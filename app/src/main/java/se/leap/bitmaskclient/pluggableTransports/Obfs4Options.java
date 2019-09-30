package se.leap.bitmaskclient.pluggableTransports;

import java.io.Serializable;

public class Obfs4Options implements Serializable {
    public String cert;
    public String iatMode;
    public String remoteIP;
    public String remotePort;

    public Obfs4Options(String remoteIP, String remotePort, String cert, String iatMode) {
        this.cert = cert;
        this.iatMode = iatMode;
        this.remoteIP = remoteIP;
        this.remotePort = remotePort;
    }

}
