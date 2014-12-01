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

import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;

/**
 * @author parmegv
 */
public abstract class SessionDialogInterface extends DialogFragment {
    final public static String USERNAME = "username";
    final public static String PASSWORD = "password";
    final public static String USERNAME_MISSING = "username missing";
    final public static String PASSWORD_INVALID_LENGTH = "password_invalid_length";

    @Override
    public void onAttach(Activity activity) { super.onAttach(activity); }

    @Override
    public void onCancel(DialogInterface dialog) { super.onCancel(dialog); }
}
