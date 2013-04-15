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

public class NewProviderDialog extends DialogFragment {
	
	public interface NewProviderDialogInterface {
        public void saveProvider(String url_provider);
    }

	NewProviderDialogInterface interface_with_ConfigurationWizard;

	public static DialogFragment newInstance() {
		NewProviderDialog dialog_fragment = new NewProviderDialog();
		return dialog_fragment;
	}
	
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
        	interface_with_ConfigurationWizard = (NewProviderDialogInterface) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View new_provider_dialog_view = inflater.inflate(R.layout.new_provider_dialog, null);
		final EditText url_input_field = (EditText)new_provider_dialog_view.findViewById(R.id.new_provider_url);
		builder.setView(new_provider_dialog_view)
			.setMessage(R.string.introduce_new_provider)
			.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					String entered_url = url_input_field.getText().toString().trim();
					if(validURL(entered_url)) {
						interface_with_ConfigurationWizard.saveProvider(entered_url);
						Toast.makeText(getActivity().getApplicationContext(), "Valid URL", Toast.LENGTH_LONG).show();
					} else {
						url_input_field.setText("");
						Toast.makeText(getActivity().getApplicationContext(), "Not valid URL", Toast.LENGTH_LONG).show();
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

	boolean validURL(String entered_url) {
		return !entered_url.isEmpty() && entered_url.matches("http[s]?://.+") && !entered_url.replaceFirst("http[s]?://", "").isEmpty();
	}
}
