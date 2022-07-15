/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core.connection;

import android.text.TextUtils;

import com.google.gson.annotations.JsonAdapter;

import java.io.Serializable;
import java.util.Locale;

@JsonAdapter(ConnectionAdapter.class)
public abstract class Connection implements Serializable, Cloneable {
    private String mServerName = "openvpn.example.com";
    private String mServerPort = "1194";
    private boolean mUseUdp = true;
    private String mCustomConfiguration = "";
    private boolean mUseCustomConfig = false;
    private boolean mEnabled = true;
    private int mConnectTimeout = 0;
    private static final int CONNECTION_DEFAULT_TIMEOUT = 120;
    private ProxyType mProxyType = ProxyType.NONE;
    private String mProxyName = "proxy.example.com";
    private String mProxyPort = "8080";

    private boolean mUseProxyAuth;
    private String mProxyAuthUser = null;
    private String mProxyAuthPassword = null;

    public enum ProxyType {
        NONE,
        HTTP,
        SOCKS5,
        ORBOT
    }

    public enum TransportType {
        OBFS4("obfs4"),
        OPENVPN("openvpn"),
        OBFS4_KCP("obfs4-1"),

        PT("metaTransport");

        String transport;

        TransportType(String transportType) {
            this.transport = transportType;
        }

        @Override
        public String toString() {
            return transport;
        }

        public boolean isPluggableTransport() {
            return this == OBFS4 || this == OBFS4_KCP || this == PT;
        }

        public TransportType getMetaType() {
            if (this == OBFS4 || this == OBFS4_KCP || this == PT) {
                return PT;
            }
            return OPENVPN;
        }
    }


    private static final long serialVersionUID = 92031902903829089L;


    public String getConnectionBlock(boolean isOpenVPN3) {
        String cfg = "";

        // Server Address
        cfg += "remote ";
        cfg += mServerName;
        cfg += " ";
        cfg += mServerPort;
        if (mUseUdp)
            cfg += " udp\n";
        else
            cfg += " tcp-client\n";

        if (mConnectTimeout != 0)
            cfg += String.format(Locale.US, " connect-timeout  %d\n", mConnectTimeout);

        // OpenVPN 2.x manages proxy connection via management interface
        if ((isOpenVPN3 || usesExtraProxyOptions()) && mProxyType == ProxyType.HTTP)
        {
            cfg+=String.format(Locale.US,"http-proxy %s %s\n", mProxyName, mProxyPort);
            if (mUseProxyAuth)
                cfg+=String.format(Locale.US, "<http-proxy-user-pass>\n%s\n%s\n</http-proxy-user-pass>\n", mProxyAuthUser, mProxyAuthPassword);
        }
        if (usesExtraProxyOptions() && mProxyType == ProxyType.SOCKS5) {
            cfg+=String.format(Locale.US,"socks-proxy %s %s\n", mProxyName, mProxyPort);
        }

        if (!TextUtils.isEmpty(mCustomConfiguration) && mUseCustomConfig) {
            cfg += mCustomConfiguration;
            cfg += "\n";
        }


        return cfg;
    }

    public boolean usesExtraProxyOptions() {
        return (mUseCustomConfig && mCustomConfiguration.contains("http-proxy-option "));
    }


    @Override
    public Connection clone() throws CloneNotSupportedException {
        return (Connection) super.clone();
    }

    public boolean isOnlyRemote() {
        return TextUtils.isEmpty(mCustomConfiguration) || !mUseCustomConfig;
    }

    public int getTimeout() {
        if (mConnectTimeout <= 0)
            return CONNECTION_DEFAULT_TIMEOUT;
        else
            return mConnectTimeout;
    }

    public String getServerName() {
        return mServerName;
    }

    public void setServerName(String mServerName) {
        this.mServerName = mServerName;
    }

    public String getServerPort() {
        return mServerPort;
    }

    public void setServerPort(String serverPort) {
        this.mServerPort = serverPort;
    }

    public boolean isUseUdp() {
        return mUseUdp;
    }

    public void setUseUdp(boolean useUdp) {
        this.mUseUdp = useUdp;
    }

    public String getCustomConfiguration() {
        return mCustomConfiguration;
    }

    public void setCustomConfiguration(String customConfiguration) {
        this.mCustomConfiguration = customConfiguration;
    }

    public boolean isUseCustomConfig() {
        return mUseCustomConfig;
    }

    public void setUseCustomConfig(boolean useCustomConfig) {
        this.mUseCustomConfig = useCustomConfig;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.mEnabled = enabled;
    }

    public int getConnectTimeout() {
        return mConnectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.mConnectTimeout = connectTimeout;
    }

    public ProxyType getProxyType() {
        return mProxyType;
    }

    public void setProxyType(ProxyType proxyType) {
        this.mProxyType = proxyType;
    }

    public String getProxyName() {
        return mProxyName;
    }

    public void setProxyName(String proxyName) {
        this.mProxyName = proxyName;
    }

    public String getProxyPort() {
        return mProxyPort;
    }

    public void setProxyPort(String proxyPort) {
        this.mProxyPort = proxyPort;
    }

    public boolean isUseProxyAuth() {
        return mUseProxyAuth;
    }

    public void setUseProxyAuth(boolean useProxyAuth) {
        this.mUseProxyAuth = useProxyAuth;
    }

    public String getProxyAuthUser() {
        return mProxyAuthUser;
    }

    public void setProxyAuthUser(String proxyAuthUser) {
        this.mProxyAuthUser = proxyAuthUser;
    }

    public String getProxyAuthPassword() {
        return mProxyAuthPassword;
    }

    public void setProxyAuthPassword(String proxyAuthPassword) {
        this.mProxyAuthPassword = proxyAuthPassword;
    }

    public abstract TransportType getTransportType();
}
