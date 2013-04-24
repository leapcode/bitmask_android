package se.leap.leapclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class LogInDialog extends DialogFragment {
	
	public interface LogInDialogInterface {
        public void authenticate(String username, String password);
    }

	LogInDialogInterface interface_with_Dashboard;

	public static DialogFragment newInstance() {
		LogInDialog dialog_fragment = new LogInDialog();
		return dialog_fragment;
	}
	
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
        	interface_with_Dashboard = (LogInDialogInterface) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View log_in_dialog_view = inflater.inflate(R.layout.log_in_dialog, null);
		final EditText username_field = (EditText)log_in_dialog_view.findViewById(R.id.username_entered);
		final EditText password_field = (EditText)log_in_dialog_view.findViewById(R.id.password_entered);
		builder.setView(log_in_dialog_view)
			.setPositiveButton(R.string.log_in_button, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					String username = username_field.getText().toString().trim();
					String password = password_field.getText().toString().trim();
					if(validPassword(password)) {
						interface_with_Dashboard.authenticate(username, password);
						Toast.makeText(getActivity().getApplicationContext(), "It seems your URL is well formed", Toast.LENGTH_LONG).show();
					} else {
						password_field.setText("");
						Toast.makeText(getActivity().getApplicationContext(), "It seems your URL is not well formed", Toast.LENGTH_LONG).show();
					}
				}
			})
			.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
		// Create the AlertDialog object and return it
		return builder.create();
	}

	boolean validPassword(String entered_password) {
		return entered_password.length() > 4;
	}
}
