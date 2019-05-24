package de.blinkt.openvpn.core;

import java.io.Serializable;

/**
 * Created by cyberta on 11.03.19.
 */

public interface ConnectionInterface {

    String getConnectionBlock(boolean isOpenVPN3);
    boolean usesExtraProxyOptions();
    boolean isOnlyRemote();
     int getTimeout();
}
