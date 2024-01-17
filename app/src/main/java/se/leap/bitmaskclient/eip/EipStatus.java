/**
 * Copyright (c) 2013 LEAP Encryption Access Project and contributers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package se.leap.bitmaskclient.eip;

import static de.blinkt.openvpn.core.ConnectionStatus.LEVEL_NONETWORK;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.LogItem;
import de.blinkt.openvpn.core.VpnStatus;

/**
 * EipStatus is a Singleton that represents a reduced set of a vpn's ConnectionStatus.
 * EipStatus changes it's state (EipLevel) when ConnectionStatus gets updated by OpenVpnService or
 * by VoidVpnService.
 */
public class EipStatus implements VpnStatus.StateListener {
    public static String TAG = EipStatus.class.getSimpleName();
    private static EipStatus currentStatus;

    public enum EipLevel {
        CONNECTING,
        DISCONNECTING,
        CONNECTED,
        DISCONNECTED,
        BLOCKING,
        UNKNOWN
    }

    /**
     * vpnLevel holds the connection status of the openvpn vpn and the traffic blocking
     * void vpn. LEVEL_BLOCKING is set when the latter vpn is up. All other states are set by
     * openvpn.
     */
    private ConnectionStatus vpnLevel = ConnectionStatus.LEVEL_NOTCONNECTED;
    private static EipLevel currentEipLevel = EipLevel.DISCONNECTED;

    private int lastErrorLine = 0;
    private String state, logMessage;
    private int localizedResId;
    private boolean isUpdatingVPNCertificate;

    private final PropertyChangeSupport propertyChange;
    public static final String PROPERTY_CHANGE = "EipStatus";

    public static EipStatus getInstance() {
        if (currentStatus == null) {
            currentStatus = new EipStatus();
            VpnStatus.addStateListener(currentStatus);
        }
        return currentStatus;
    }

    private EipStatus() {
        propertyChange = new PropertyChangeSupport(this);
    }

    @Override
    public void updateState(final String state, final String logmessage, final int localizedResId, final ConnectionStatus level) {
        ConnectionStatus tmp = getInstance().getLevel();
        getInstance().setState(state);
        getInstance().setLogMessage(logmessage);
        getInstance().setLocalizedResId(localizedResId);
        getInstance().setLevel(level);
        getInstance().setEipLevel(level);
        if (tmp != getInstance().getLevel() || "RECONNECTING".equals(state) || "UI_CONNECTING".equals(state)) {
            refresh();
        }
    }

    @Override
    public void setConnectedVPN(String uuid) {
    }

    public boolean isReconnecting() {
        Log.d(TAG, "eip currentVPNStatus : " + getInstance().getState() );
        return "RECONNECTING".equals(getInstance().getState());
    }

    public boolean isVPNRunningWithoutNetwork() {
        return getInstance().getLevel() == LEVEL_NONETWORK &&
                !"NO_PROCESS".equals(getInstance().getState());
    }

    private void setEipLevel(ConnectionStatus level) {
        switch (level) {
            case LEVEL_CONNECTED:
                currentEipLevel = EipLevel.CONNECTED;
                break;
            case LEVEL_VPNPAUSED:
                if (VpnStatus.getLastConnectedVpnProfile() != null && VpnStatus.getLastConnectedVpnProfile().mPersistTun) {
                    //if persistTun is enabled, treat EipLevel as connecting as it *shouldn't* allow passing traffic in the clear...
                    currentEipLevel = EipLevel.CONNECTING;
                } else {
                    //... if persistTun is not enabled, background network traffic will pass in the clear
                    currentEipLevel = EipLevel.DISCONNECTED;
                }
                break;
            case LEVEL_CONNECTING_SERVER_REPLIED:
            case LEVEL_CONNECTING_NO_SERVER_REPLY_YET:
            case LEVEL_WAITING_FOR_USER_INPUT:
            case LEVEL_START:
                currentEipLevel = EipLevel.CONNECTING;
                break;
            case LEVEL_AUTH_FAILED:
            case LEVEL_NOTCONNECTED:
                currentEipLevel = EipLevel.DISCONNECTED;
                break;
            case LEVEL_STOPPING:
                currentEipLevel = EipLevel.DISCONNECTING;
                break;
            case LEVEL_NONETWORK:
            case LEVEL_BLOCKING:
                setEipLevelWithDelay(level);
                break;
            case UNKNOWN_LEVEL:
                currentEipLevel = EipLevel.UNKNOWN; //??
                break;
        }
    }

    public EipLevel getEipLevel() {
        return currentEipLevel;
    }

    /**
     * This is a debouncing method ignoring states that are valid for less than a second.
     * This way flickering UI changes can be avoided.
     *
     * @param futureLevel
     */
    private void setEipLevelWithDelay(ConnectionStatus futureLevel) {
        new DelayTask(getInstance().getLevel(), futureLevel).execute();
    }

    private static class DelayTask extends AsyncTask<Void, Void, Void> {

        private final ConnectionStatus currentLevel;
        private final ConnectionStatus futureLevel;

        DelayTask(ConnectionStatus currentLevel, ConnectionStatus futureLevel) {
            this.currentLevel = currentLevel;
            this.futureLevel = futureLevel;
        }
        protected Void doInBackground(Void... levels) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
            return null;
        }

        protected void onPostExecute(Void result) {
            if (currentLevel == getInstance().getLevel()) {
                switch (futureLevel) {
                    case LEVEL_NONETWORK:
                        currentEipLevel = EipLevel.DISCONNECTED;
                        break;
                    case LEVEL_BLOCKING:
                        currentEipLevel = EipLevel.BLOCKING;
                        break;
                    default:
                        break;
                }
                refresh();
            }
        }
    }

    public void setUpdatingVpnCert(boolean isUpdating) {
        isUpdatingVPNCertificate = isUpdating;
        refresh();
    }

    public boolean isUpdatingVpnCert() {
        return isUpdatingVPNCertificate;
    }

    public boolean isConnecting() {
        return currentEipLevel == EipLevel.CONNECTING;
    }

    public boolean isConnected() {
        return currentEipLevel == EipLevel.CONNECTED;
    }

    /**
     * @return true if currentEipLevel is for at least a second {@link EipLevel#BLOCKING}.
     * See {@link #setEipLevelWithDelay(ConnectionStatus)}.
     */
    public boolean isBlocking() {
        return currentEipLevel == EipLevel.BLOCKING;
    }

    /**
     *
     * @return true immediately after traffic blocking VoidVpn was established.
     */
    public boolean isBlockingVpnEstablished() {
        return vpnLevel == ConnectionStatus.LEVEL_BLOCKING;
    }

    public boolean isDisconnected() {
        return currentEipLevel == EipLevel.DISCONNECTED;
    }

    public boolean isDisconnecting() {
        return currentEipLevel == EipLevel.DISCONNECTING;
    }

    /**
     * ics-openvpn's paused state is not implemented yet
     * @return true if vpn is paused false if not
     */
    @Deprecated
    public boolean isPaused() {
        return vpnLevel == ConnectionStatus.LEVEL_VPNPAUSED;
    }

    public String getState() {
        return state;
    }

    public String getLogMessage() {
        return logMessage;
    }

    int getLocalizedResId() {
        return localizedResId;
    }

    public ConnectionStatus getLevel() {
        return vpnLevel;
    }

    private void setState(String state) {
        this.state = state;
    }

    private void setLogMessage(String log_message) {
        this.logMessage = log_message;
    }

    private void setLocalizedResId(int localized_res_id) {
        this.localizedResId = localized_res_id;
    }

    private void setLevel(ConnectionStatus level) {
        this.vpnLevel = level;
    }

    public boolean errorInLast(int lines, Context context) {
        return !lastError(lines, context).isEmpty();
    }

    private String lastError(int lines, Context context) {
        String error = "";

        String[] error_keywords = {"error", "ERROR", "fatal", "FATAL"};

        LogItem[] log = VpnStatus.getlogbuffer();
        if(log.length < lastErrorLine)
            lastErrorLine = 0;
        String message;
        for (int i = 1; i <= lines && log.length > i; i++) {
            int line = log.length - i;
            LogItem logItem = log[line];
            message = logItem.getString(context);
            for (String errorKeyword: error_keywords) {
                if (message.contains(errorKeyword) && line > lastErrorLine) {
                    error = message;
                    lastErrorLine = line;
                }
            }
        }

        return error;
    }

    @Override
    public String toString() {
        return "State: " + state + " Level: " + vpnLevel.toString();
    }

    public static void refresh() {
        currentStatus.propertyChange.firePropertyChange(PROPERTY_CHANGE, null, currentStatus);
    }

    public void addObserver(PropertyChangeListener propertyChangeListener) {
        propertyChange.addPropertyChangeListener(propertyChangeListener);
    }

    public void deleteObserver(PropertyChangeListener propertyChangeListener) {
        propertyChange.removePropertyChangeListener(propertyChangeListener);
    }
}
