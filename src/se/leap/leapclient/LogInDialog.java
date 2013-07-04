package se.leap.leapclient;

import se.leap.leapclient.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

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

	public AlertDialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View log_in_dialog_view = inflater.inflate(R.layout.log_in_dialog, null);
		
		final EditText username_field = (EditText)log_in_dialog_view.findViewById(R.id.username_entered);
		final EditText password_field = (EditText)log_in_dialog_view.findViewById(R.id.password_entered);
		
		builder.setView(log_in_dialog_view)
			.setPositiveButton(R.string.login_button, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					String username = username_field.getText().toString().trim();
					String password = password_field.getText().toString().trim();
					if(wellFormedPassword(password)) {
						interface_with_Dashboard.authenticate(username, password);
					} else {
						password_field.setText("");
						Toast.makeText(getActivity().getApplicationContext(), R.string.not_valid_password_message, Toast.LENGTH_LONG).show();
					}
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
	 * Validates a password
	 * @param entered_password
	 * @return true if the entered password length is greater or equal to eight (8).
	 */
	private boolean wellFormedPassword(String entered_password) {
		return entered_password.length() >= 8;
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
                    + " must implement NoticeDialogListener");
        }
    }
}
