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

import android.content.*;
import android.os.AsyncTask;
import android.support.annotation.VisibleForTesting;

import java.util.*;

import de.blinkt.openvpn.core.*;

/**
 * EipStatus is a Singleton that represents a reduced set of a vpn's ConnectionStatus.
 * EipStatus changes it's state (EipLevel) when ConnectionStatus gets updated by OpenVpnService or
 * by VoidVpnService.
 */
public class EipStatus extends Observable implements VpnStatus.StateListener {
    public static String TAG = EipStatus.class.getSimpleName();
    private static EipStatus current_status;
    public enum EipLevel {
        CONNECTING,
        DISCONNECTING,
        CONNECTED,
        DISCONNECTED,
        BLOCKING,
        UNKNOWN
    }

    /**
     * vpn_level holds the connection status of the openvpn vpn and the traffic blocking
     * void vpn. LEVEL_BLOCKING is set when the latter vpn is up. All other states are set by
     * openvpn.
     */
    private ConnectionStatus vpn_level = ConnectionStatus.LEVEL_NOTCONNECTED;
    private EipLevel current_eip_level = EipLevel.DISCONNECTED;

    int last_error_line = 0;
    private String state, log_message;
    private int localized_res_id;

    public static EipStatus getInstance() {
        if (current_status == null) {
            current_status = new EipStatus();
            VpnStatus.addStateListener(current_status);
        }
        return current_status;
    }

    private EipStatus() {
    }

    @Override
    public void updateState(final String state, final String logmessage, final int localizedResId, final ConnectionStatus level) {
        current_status = getInstance();
        current_status.setState(state);
        current_status.setLogMessage(logmessage);
        current_status.setLocalizedResId(localizedResId);
        current_status.setLevel(level);
        current_status.setEipLevel(level);
    }

    @Override
    public void setConnectedVPN(String uuid) {
    }


    private void setEipLevel(ConnectionStatus level) {
        EipLevel tmp = current_eip_level;
        switch (level) {
            case LEVEL_CONNECTED:
                current_eip_level = EipLevel.CONNECTED;
                break;
            case LEVEL_VPNPAUSED:
                throw new IllegalStateException("Ics-Openvpn's VPNPAUSED state is not supported by Bitmask");
            case LEVEL_CONNECTING_SERVER_REPLIED:
            case LEVEL_CONNECTING_NO_SERVER_REPLY_YET:
            case LEVEL_WAITING_FOR_USER_INPUT:
            case LEVEL_START:
                current_eip_level = EipLevel.CONNECTING;
                break;
            case LEVEL_AUTH_FAILED:
            case LEVEL_NOTCONNECTED:
                current_eip_level = EipLevel.DISCONNECTED;
                break;
            case LEVEL_NONETWORK:
            case LEVEL_BLOCKING:
                setEipLevelWithDelay(level);
                break;
            case UNKNOWN_LEVEL:
                current_eip_level = EipLevel.UNKNOWN; //??
                break;
        }
        if (tmp != current_eip_level) {
            current_status.setChanged();
            current_status.notifyObservers();
        }
    }

    @VisibleForTesting
    EipLevel getEipLevel() {
        return current_eip_level;
    }

    /**
     * This method intends to ignore states that are valid for less than a second.
     * This way flickering UI changes can be avoided
     *
     * @param futureLevel
     */
    private void setEipLevelWithDelay(ConnectionStatus futureLevel) {
        new DelayTask(current_status.getLevel(), futureLevel).execute();
    }

    private class DelayTask extends AsyncTask<Void, Void, Void> {

        private final ConnectionStatus currentLevel;
        private final ConnectionStatus futureLevel;

        public DelayTask(ConnectionStatus currentLevel, ConnectionStatus futureLevel) {
            this.currentLevel = currentLevel;
            this.futureLevel = futureLevel;
        }
        protected Void doInBackground(Void... levels) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
            return null;
        }

        protected void onPostExecute(Void result) {;
            if (currentLevel == current_status.getLevel()) {
                switch (futureLevel) {
                    case LEVEL_NONETWORK:
                        current_eip_level = EipLevel.DISCONNECTED;
                        break;
                    case LEVEL_BLOCKING:
                        current_eip_level = EipLevel.BLOCKING;
                        break;
                    default:
                        break;
                }
                current_status.setChanged();
                current_status.notifyObservers();
            }
        }
    }

    public boolean isConnecting() {
        return current_eip_level == EipLevel.CONNECTING;
    }

    public boolean isConnected() {
        return current_eip_level == EipLevel.CONNECTED;
    }

    /**
     * @return true if current_eip_level is for at least a second {@link EipLevel#BLOCKING}.
     * See {@link #setEipLevelWithDelay(ConnectionStatus)}.
     */
    public boolean isBlocking() {
        return current_eip_level == EipLevel.BLOCKING;
    }

    /**
     *
     * @return true immediately after traffic blocking VoidVpn was established.
     */
    public boolean isBlockingVpnEstablished() {
        return vpn_level == ConnectionStatus.LEVEL_BLOCKING;
    }

    public boolean isDisconnected() {
        return current_eip_level == EipLevel.DISCONNECTED;
    }

    /**
     * ics-openvpn's paused state is not implemented yet
     * @return
     */
    @Deprecated
    public boolean isPaused() {
        return vpn_level == ConnectionStatus.LEVEL_VPNPAUSED;
    }

    public String getState() {
        return state;
    }

    public String getLogMessage() {
        return log_message;
    }

    public int getLocalizedResId() {
        return localized_res_id;
    }

    public ConnectionStatus getLevel() {
        return vpn_level;
    }

    private void setState(String state) {
        this.state = state;
    }

    private void setLogMessage(String log_message) {
        this.log_message = log_message;
    }

    private void setLocalizedResId(int localized_res_id) {
        this.localized_res_id = localized_res_id;
    }

    private void setLevel(ConnectionStatus level) {
        this.vpn_level = level;
    }

    public boolean errorInLast(int lines, Context context) {
        return !lastError(lines, context).isEmpty();
    }

    public String lastError(int lines, Context context) {
        String error = "";

        String[] error_keywords = {"error", "ERROR", "fatal", "FATAL"};

        LogItem[] log = VpnStatus.getlogbuffer();
        if(log.length < last_error_line)
            last_error_line = 0;
        String message = "";
        for (int i = 1; i <= lines && log.length > i; i++) {
            int line = log.length - i;
            LogItem log_item = log[line];
            message = log_item.getString(context);
            for (int j = 0; j < error_keywords.length; j++)
                if (message.contains(error_keywords[j]) && line > last_error_line) {
                    error = message;
                    last_error_line = line;
                }
        }

        return error;
    }

    @Override
    public String toString() {
        return "State: " + state + " Level: " + vpn_level.toString();
    }

}
