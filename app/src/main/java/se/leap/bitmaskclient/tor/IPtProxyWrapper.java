package se.leap.bitmaskclient.tor;

import IPtProxy.IPtProxy;

public class IPtProxyWrapper implements IPtProxyInterface {
    @Override
    public void setStateLocation(String location) {
        IPtProxy.setStateLocation(location);
    }

    @Override
    public String getStateLocation() {
        return IPtProxy.getStateLocation();
    }

    @Override
    public long snowflakePort() {
        return IPtProxy.snowflakePort();
    }

    @Override
    public long startSnowflake(String ice, String url, String front, String logFile, boolean logToStateDir, boolean keepLocalAddresses, boolean unsafeLogging, long maxPeers) {
        return IPtProxy.startSnowflake(ice, url, front, logFile, logToStateDir, keepLocalAddresses, unsafeLogging, maxPeers);
    }

    @Override
    public void startSnowflakeProxy(long capacity, String broker, String relay, String stun, String logFile, boolean keepLocalAddresses, boolean unsafeLogging) {
        IPtProxy.startSnowflakeProxy(capacity, broker, relay, stun, logFile, keepLocalAddresses, unsafeLogging);
    }

    @Override
    public void stopSnowflake() {
        IPtProxy.stopSnowflake();
    }

    @Override
    public void stopSnowflakeProxy() {
        IPtProxy.stopSnowflakeProxy();
    }
}
