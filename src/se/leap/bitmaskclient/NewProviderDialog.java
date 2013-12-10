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

import se.leap.bitmaskclient.ProviderListContent.ProviderItem;
import se.leap.bitmaskclient.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Implements the new custom provider dialog.
 * 
 * @author parmegv
 *
 */
public class NewProviderDialog extends DialogFragment {

    final public static String TAG = "newProviderDialog";
        
	public interface NewProviderDialogInterface {
        public void showAndSelectProvider(String url_provider, boolean danger_on);
    }

	NewProviderDialogInterface interface_with_ConfigurationWizard;

	/**
	 * @return a new instance of this DialogFragment.
	 */
	public static DialogFragment newInstance() {
		NewProviderDialog dialog_fragment = new NewProviderDialog();
		return dialog_fragment;
	}
	
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
        	interface_with_ConfigurationWizard = (NewProviderDialogInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View new_provider_dialog_view = inflater.inflate(R.layout.new_provider_dialog, null);
		final EditText url_input_field = (EditText)new_provider_dialog_view.findViewById(R.id.new_provider_url);
		if(getArguments() != null && getArguments().containsKey(Provider.MAIN_URL)) {
			url_input_field.setText(getArguments().getString(Provider.MAIN_URL));
		}
		final CheckBox danger_checkbox = (CheckBox)new_provider_dialog_view.findViewById(R.id.danger_checkbox);
		if(getArguments() != null && getArguments().containsKey(ProviderItem.DANGER_ON)) {
			danger_checkbox.setActivated(getArguments().getBoolean(ProviderItem.DANGER_ON));
		}
		
		builder.setView(new_provider_dialog_view)
			.setMessage(R.string.introduce_new_provider)
			.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					String entered_url = url_input_field.getText().toString().trim();
					if(!entered_url.startsWith("https://")) {
						if (entered_url.startsWith("http://")){
							entered_url = entered_url.substring("http://".length());
						}
						entered_url = "https://".concat(entered_url);
					}
					boolean danger_on = danger_checkbox.isChecked();
					if(validURL(entered_url)) {
						interface_with_ConfigurationWizard.showAndSelectProvider(entered_url, danger_on);
						Toast.makeText(getActivity().getApplicationContext(), R.string.valid_url_entered, Toast.LENGTH_LONG).show();
					} else {
						url_input_field.setText("");
						danger_checkbox.setChecked(false);
						Toast.makeText(getActivity().getApplicationContext(), R.string.not_valid_url_entered, Toast.LENGTH_LONG).show();;
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

    /**
     * Checks if the entered url is valid or not.
     * @param entered_url
     * @return true if it's not empty nor contains only the protocol.
     */
	boolean validURL(String entered_url) {
		//return !entered_url.isEmpty() && entered_url.matches("http[s]?://.+") && !entered_url.replaceFirst("http[s]?://", "").isEmpty();
		return android.util.Patterns.WEB_URL.matcher(entered_url).matches();
	}
}
