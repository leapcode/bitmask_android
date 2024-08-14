/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core.connection;

import static de.blinkt.openvpn.core.connection.Connection.TransportType.*;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.gson.annotations.JsonAdapter;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

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

    public enum TransportProtocol {
        UDP("udp"),
        TCP("tcp"),
        KCP("kcp");

        final String protocol;

        TransportProtocol(String transportProtocol) {
            this.protocol = transportProtocol;
        }

        @Override
        public String toString() {
            return protocol;
        }
    }

    // !! Never use valueOf() to instantiate this enum, use fromString() !!
    public enum TransportType {
        OBFS4("obfs4"),
        // dashes are not allowed in enums, so obfs4-hop becomes obfs4Hop -.-
        OBFS4_HOP("obfs4Hop"),
        OPENVPN("openvpn"),

        PT("metaTransport");

        final String transport;

        TransportType(String transportType) {
            this.transport = transportType;
        }

        @Override
        public String toString() {
            if (this == OBFS4_HOP) {
                return "obfs4-hop";
            }
            return transport;
        }

        public int toInt() {
            switch (this) {
                case PT:
                    return 0;
                case OPENVPN:
                    return 1;
                case OBFS4:
                    return 2;
                case OBFS4_HOP:
                    return 3;
                default:
                    return -1;
            }
        }

        public static TransportType fromString(String value) {
            switch (value) {
                case "obfs4":
                    return OBFS4;
                case "obfs4-hop":
                    return OBFS4_HOP;
                case "metaTransport":
                    return PT;
                case "openvpn":
                    return OPENVPN;
                default:
                    throw new IllegalArgumentException(value + " is not a valid value for TransportType.");
            }
        }

        public static TransportType fromInt(int value) {
            switch (value) {
                case 0:
                    return PT;
                case 1:
                    return OPENVPN;
                case 2:
                    return OBFS4;
                case 3:
                    return OBFS4_HOP;
                default:
                    return null;
            }
        }

        public boolean isPluggableTransport() {
            return this == OBFS4 || this == OBFS4_HOP || this == PT;
        }

        public TransportType getMetaType() {
            if (this == OBFS4 || this == OBFS4_HOP || this == PT) {
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

    public abstract @NonNull TransportType getTransportType();


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Connection that)) return false;

        if (mUseUdp != that.mUseUdp) return false;
        if (mUseCustomConfig != that.mUseCustomConfig) return false;
        if (mEnabled != that.mEnabled) return false;
        if (mConnectTimeout != that.mConnectTimeout) return false;
        if (mUseProxyAuth != that.mUseProxyAuth) return false;
        if (!Objects.equals(mServerName, that.mServerName))
            return false;
        if (!Objects.equals(mServerPort, that.mServerPort))
            return false;
        if (!Objects.equals(mCustomConfiguration, that.mCustomConfiguration))
            return false;
        if (mProxyType != that.mProxyType) return false;
        if (!Objects.equals(mProxyName, that.mProxyName))
            return false;
        if (!Objects.equals(mProxyPort, that.mProxyPort))
            return false;
        if (!Objects.equals(mProxyAuthUser, that.mProxyAuthUser))
            return false;
        if (getTransportType() != that.getTransportType()) {
            return false;
        }
        return Objects.equals(mProxyAuthPassword, that.mProxyAuthPassword);
    }

    @Override
    public int hashCode() {
        int result = mServerName != null ? mServerName.hashCode() : 0;
        result = 31 * result + (mServerPort != null ? mServerPort.hashCode() : 0);
        result = 31 * result + (mUseUdp ? 1 : 0);
        result = 31 * result + (mCustomConfiguration != null ? mCustomConfiguration.hashCode() : 0);
        result = 31 * result + (mUseCustomConfig ? 1 : 0);
        result = 31 * result + (mEnabled ? 1 : 0);
        result = 31 * result + mConnectTimeout;
        result = 31 * result + (mProxyType != null ? mProxyType.hashCode() : 0);
        result = 31 * result + (mProxyName != null ? mProxyName.hashCode() : 0);
        result = 31 * result + (mProxyPort != null ? mProxyPort.hashCode() : 0);
        result = 31 * result + (mUseProxyAuth ? 1 : 0);
        result = 31 * result + (mProxyAuthUser != null ? mProxyAuthUser.hashCode() : 0);
        result = 31 * result + (mProxyAuthPassword != null ? mProxyAuthPassword.hashCode() : 0);
        result = 31 * result + getTransportType().toInt();
        return result;
    }
}
