package se.leap.bitmaskclient.pluggableTransports;

import java.io.Serializable;

public class DispatcherOptions implements Serializable {
    public String cert;
    public String iatMode;
    public String remoteIP;
    public String remotePort;

    public DispatcherOptions(String remoteIP, String remotePort, String cert, String iatMode) {
        this.cert = cert;
        this.iatMode = iatMode;
        this.remoteIP = remoteIP;
        this.remotePort = remotePort;
    }

}
