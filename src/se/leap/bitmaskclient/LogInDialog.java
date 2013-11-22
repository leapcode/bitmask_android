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

import se.leap.bitmaskclient.R;
import android.R.color;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.provider.CalendarContract.Colors;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.BounceInterpolator;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Implements the log in dialog, currently without progress dialog.
 * 
 * It returns to the previous fragment when finished, and sends username and password to the authenticate method.
 * 
 * It also notifies the user if the password is not valid. 
 * 
 * @author parmegv
 *
 */
public class LogInDialog extends DialogFragment {

     
	final public static String TAG = "logInDialog";
	final public static String VERB = "log in";
	final public static String USERNAME = "username";
	final public static String PASSWORD = "password";
	final public static String USERNAME_MISSING = "username missing";
	final public static String PASSWORD_INVALID_LENGTH = "password_invalid_length";
    
    
	public AlertDialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View log_in_dialog_view = inflater.inflate(R.layout.log_in_dialog, null);

		final TextView user_message = (TextView)log_in_dialog_view.findViewById(R.id.user_message);
		if(getArguments() != null && getArguments().containsKey(getResources().getString(R.string.user_message))) {
			user_message.setText(getArguments().getString(getResources().getString(R.string.user_message)));
		} else {
			user_message.setVisibility(View.GONE);
		}
		
		final EditText username_field = (EditText)log_in_dialog_view.findViewById(R.id.username_entered);
		if(getArguments() != null && getArguments().containsKey(USERNAME)) {
			String username = getArguments().getString(USERNAME);
			username_field.setText(username);
		}
		if (getArguments() != null && getArguments().containsKey(USERNAME_MISSING)) {
			username_field.setError(getResources().getString(R.string.username_ask));
    	}
		
		final EditText password_field = (EditText)log_in_dialog_view.findViewById(R.id.password_entered);
		if(!username_field.getText().toString().isEmpty() && password_field.isFocusable()) {
			password_field.requestFocus();
		}
		if (getArguments() != null && getArguments().containsKey(PASSWORD_INVALID_LENGTH)) {
			password_field.setError(getResources().getString(R.string.error_not_valid_password_user_message));
    	}

		
		builder.setView(log_in_dialog_view)
			.setPositiveButton(R.string.login_button, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					String username = username_field.getText().toString();
					String password = password_field.getText().toString();
					dialog.dismiss();
					interface_with_Dashboard.authenticate(username, password);
				}
			})
			.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
		
		return builder.create();
	}
	
	/**
	 * Interface used to communicate LogInDialog with Dashboard.
	 * 
	 * @author parmegv
	 *
	 */
	public interface LogInDialogInterface {
		/**
		 * Starts authentication process.
		 * @param username
		 * @param password
		 */
        public void authenticate(String username, String password);
    }

	LogInDialogInterface interface_with_Dashboard;

	/**
	 * @return a new instance of this DialogFragment.
	 */
	public static DialogFragment newInstance() {
		LogInDialog dialog_fragment = new LogInDialog();
		return dialog_fragment;
	}
	
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
        	interface_with_Dashboard = (LogInDialogInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement LogInDialogListener");
        }
    }
}
