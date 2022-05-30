package se.leap.bitmaskclient.pluggableTransports;

import java.io.Serializable;

public class Obfs4Options implements Serializable {
    public String cert;
    public String iatMode;
    public String remoteIP;
    public String remotePort;
    public boolean udp;

    public Obfs4Options(String remoteIP, String remotePort, String cert, String iatMode, boolean udp) {
        this.cert = cert;
        this.iatMode = iatMode;
        this.remoteIP = remoteIP;
        this.remotePort = remotePort;
        this.udp = udp;
    }

}
