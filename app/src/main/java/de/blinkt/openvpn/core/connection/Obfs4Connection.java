package de.blinkt.openvpn.core.connection;

import org.json.JSONObject;

/**
 * Created by cyberta on 08.03.19.
 */

public class Obfs4Connection extends Connection {

    private static final String TAG = Obfs4Connection.class.getName();


    private String mObfs4RemoteProxyName = "";
    private String mObfs4RemoteProxyPort = "";
    private String mObfs4Certificate = "";
    private String mObfs4IatMode = "";

    public Obfs4Connection() {
        setDefaults();
    }

    public Obfs4Connection(Connection connection) {
        mObfs4RemoteProxyName = connection.getServerName();
        setConnectTimeout(connection.getConnectTimeout());
        setCustomConfiguration(connection.getCustomConfiguration());
        setUseCustomConfig(connection.isUseCustomConfig());

        setDefaults();
    }

    private void setDefaults() {
        setUseUdp(false);
        setServerName("127.0.0.1");
        setServerPort("");
        setProxyName("");
        setProxyPort("");
        setProxyAuthUser(null);
        setProxyAuthPassword(null);
        setProxyType(ProxyType.NONE);
        setUseProxyAuth(false);
    }

    public void setTransportOptions(JSONObject jsonObject) {
        mObfs4Certificate = jsonObject.optString("cert");
        mObfs4IatMode = jsonObject.optString("iat-mode");
    }

    @Override
    public Connection clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.OBFS4;
    }

    public String getmObfs4RemoteProxyName() {
        return mObfs4RemoteProxyName;
    }

    public void setObfs4RemoteProxyName(String mObfs4RemoteProxyName) {
        this.mObfs4RemoteProxyName = mObfs4RemoteProxyName;
    }

    public String getmObfs4RemoteProxyPort() {
        return mObfs4RemoteProxyPort;
    }

    public void setObfs4RemoteProxyPort(String mObfs4RemoteProxyPort) {
        this.mObfs4RemoteProxyPort = mObfs4RemoteProxyPort;
    }

    public String getmObfs4Certificate() {
        return mObfs4Certificate;
    }

    public String getmObfs4IatMode() {
        return mObfs4IatMode;
    }

}
