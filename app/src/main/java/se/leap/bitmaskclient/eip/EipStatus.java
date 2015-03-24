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

import android.util.*;
import android.content.*;

import java.util.*;

import de.blinkt.openvpn.core.*;

public class EipStatus extends Observable implements VpnStatus.StateListener {
    public static String TAG = EipStatus.class.getSimpleName();
    private static EipStatus current_status;

    private static VpnStatus.ConnectionStatus level = VpnStatus.ConnectionStatus.LEVEL_NOTCONNECTED;
    private static boolean wants_to_disconnect = false;

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
    public void updateState(final String state, final String logmessage, final int localizedResId, final VpnStatus.ConnectionStatus level) {
        updateStatus(state, logmessage, localizedResId, level);
        if (isConnected() || isDisconnected()) {
            setConnectedOrDisconnected();
        } else if (isConnecting())
            setConnecting();
        Log.d(TAG, "update state with level " + level);
    }

    private void updateStatus(final String state, final String logmessage, final int localizedResId, final VpnStatus.ConnectionStatus level) {
        current_status = getInstance();
        current_status.setState(state);
        current_status.setLogMessage(logmessage);
        current_status.setLocalizedResId(localizedResId);
        current_status.setLevel(level);
        current_status.setChanged();
    }

    public boolean wantsToDisconnect() {
        return wants_to_disconnect;
    }

    public boolean isConnecting() {
        return
                !isConnected() &&
                        !isDisconnected() &&
                        !isPaused();
    }

    public boolean isConnected() {
        return level == VpnStatus.ConnectionStatus.LEVEL_CONNECTED;
    }

    public boolean isDisconnected() {
        return level == VpnStatus.ConnectionStatus.LEVEL_NOTCONNECTED;
    }

    public boolean isPaused() {
        return level == VpnStatus.ConnectionStatus.LEVEL_VPNPAUSED;
    }

    public void setConnecting() {
        wants_to_disconnect = false;
        current_status.setChanged();
        current_status.notifyObservers();
    }

    public void setConnectedOrDisconnected() {
        Log.d(TAG, "setConnectedOrDisconnected()");
        wants_to_disconnect = false;
        current_status.setChanged();
        current_status.notifyObservers();
    }

    public void setDisconnecting() {
        wants_to_disconnect = false;
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

    public VpnStatus.ConnectionStatus getLevel() {
        return level;
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

    private void setLevel(VpnStatus.ConnectionStatus level) {
        EipStatus.level = level;
    }

    public boolean errorInLast(int lines, Context context) {
        boolean result = false;
        String[] error_keywords = {"error", "ERROR", "fatal", "FATAL"};

        VpnStatus.LogItem[] log = VpnStatus.getlogbuffer();
        String message = "";
        for (int i = 1; i <= lines && log.length > i; i++) {
            message = log[log.length - i].getString(context);
            for (int j = 0; j < error_keywords.length; j++)
                if (message.contains(error_keywords[j]))
                    result = true;
        }
        return result;
    }

    @Override
    public String toString() {
        return "State: " + state + " Level: " + level.toString();
    }

}
