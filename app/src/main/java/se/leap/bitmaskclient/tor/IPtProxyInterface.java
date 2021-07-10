package se.leap.bitmaskclient.tor;

public interface IPtProxyInterface {

    /**
     * StateLocation - Override TOR_PT_STATE_LOCATION, which defaults to &#34;$TMPDIR/pt_state&#34;.
     */
    void setStateLocation(String v);

    /**
     * StateLocation - Override TOR_PT_STATE_LOCATION, which defaults to &#34;$TMPDIR/pt_state&#34;.
     */
    String getStateLocation();

    /**
     * SnowflakePort - Port where Snowflake will provide its service.
     Only use this property after calling StartSnowflake! It might have changed after that!
     */
    long snowflakePort();

    /**
     * StartSnowflake - Start the Snowflake client.

     @param ice Comma-separated list of ICE servers.

     @param url URL of signaling broker.

     @param front Front domain.

     @param logFile Name of log file. OPTIONAL

     @param logToStateDir Resolve the log file relative to Tor&#39;s PT state dir.

     @param keepLocalAddresses Keep local LAN address ICE candidates.

     @param unsafeLogging Prevent logs from being scrubbed.

     @param maxPeers Capacity for number of multiplexed WebRTC peers. DEFAULTs to 1 if less than that.

     @return Port number where Snowflake will listen on, if no error happens during start up.
     */
    long startSnowflake(String ice, String url, String front, String logFile, boolean logToStateDir, boolean keepLocalAddresses, boolean unsafeLogging, long maxPeers);

    /**
     * StartSnowflakeProxy - Start the Snowflake proxy.

     @param capacity Maximum concurrent clients. OPTIONAL. Defaults to 10, if 0.

     @param broker Broker URL. OPTIONAL. Defaults to https://snowflake-broker.bamsoftware.com/, if empty.

     @param relay WebSocket relay URL. OPTIONAL. Defaults to wss://snowflake.bamsoftware.com/, if empty.

     @param stun STUN URL. OPTIONAL. Defaults to stun:stun.stunprotocol.org:3478, if empty.

     @param logFile Name of log file. OPTIONAL

     @param keepLocalAddresses Keep local LAN address ICE candidates.

     @param unsafeLogging Prevent logs from being scrubbed.
     */
    void startSnowflakeProxy(long capacity, String broker, String relay, String stun, String logFile, boolean keepLocalAddresses, boolean unsafeLogging);

    /**
     * StopSnowflake - Stop the Snowflake client.
     */
    void stopSnowflake();

    /**
     * StopSnowflakeProxy - Stop the Snowflake proxy.
     */
    void stopSnowflakeProxy();
}
