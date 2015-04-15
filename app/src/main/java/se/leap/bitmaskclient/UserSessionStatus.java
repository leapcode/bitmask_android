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
package se.leap.bitmaskclient;

import android.content.res.*;

import java.util.*;

public class UserSessionStatus extends Observable {
    public static String TAG = UserSessionStatus.class.getSimpleName();
    private static UserSessionStatus current_status;
    private static Resources resources;

    public enum SessionStatus {
        LOGGED_IN,
        LOGGED_OUT,
        NOT_LOGGED_IN,
        DIDNT_LOG_OUT,
        LOGGING_IN,
        LOGGING_OUT,
        SIGNING_UP;

        @Override
        public String toString() {
            int id = 0;
            if(this == SessionStatus.LOGGED_IN)
                id = R.string.logged_in_user_status;
            else if(this == SessionStatus.LOGGED_OUT)
                id = R.string.logged_out_user_status;
            else if(this == SessionStatus.NOT_LOGGED_IN)
                id = R.string.not_logged_in_user_status;
            else if(this == SessionStatus.DIDNT_LOG_OUT)
                id = R.string.didnt_log_out_user_status;
            else if(this == SessionStatus.LOGGING_IN)
                id = R.string.logging_in_user_status;
            else if(this == SessionStatus.LOGGING_OUT)
                id = R.string.logging_out_user_status;
            else if(this == SessionStatus.SIGNING_UP)
                id = R.string.signingup_message;

            return resources.getString(id);
        }
    }

    private static SessionStatus session_status = SessionStatus.NOT_LOGGED_IN;

    public static UserSessionStatus getInstance(Resources resources) {
        if (current_status == null) {
            current_status = new UserSessionStatus(resources);
        }
        return current_status;
    }

    private UserSessionStatus(Resources resources) {
        UserSessionStatus.resources = resources;
    }

    private void sessionStatus(SessionStatus session_status) {
        this.session_status = session_status;
    }

    public SessionStatus sessionStatus() {
        return session_status;
    }

    public boolean inProgress() {
        return session_status == SessionStatus.LOGGING_IN
                || session_status == SessionStatus.LOGGING_OUT;
    }

    public static void updateStatus(SessionStatus session_status, Resources resources) {
        current_status = getInstance(resources);
        current_status.sessionStatus(session_status);
        current_status.setChanged();
        current_status.notifyObservers();
    }

    @Override
    public String toString() {
        String user_session_status = User.userName();

        String default_username = resources.getString(R.string.default_user, "");
        if(user_session_status.isEmpty() && !default_username.equalsIgnoreCase("null")) user_session_status = default_username;
        user_session_status += " " + session_status.toString();

        user_session_status = user_session_status.trim();
        if(User.userName().isEmpty())
            user_session_status = capitalize(user_session_status);
        return user_session_status;
    }

    private String capitalize(String to_be_capitalized) {
        return to_be_capitalized.substring(0,1).toUpperCase() + to_be_capitalized.substring(1);
    }
}
