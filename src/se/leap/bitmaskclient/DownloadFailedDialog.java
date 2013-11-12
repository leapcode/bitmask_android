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
import se.leap.bitmaskclient.NewProviderDialog.NewProviderDialogInterface;
import se.leap.bitmaskclient.ProviderListContent.ProviderItem;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Implements a dialog to show why a download failed.
 * 
 * @author parmegv
 *
 */
public class DownloadFailedDialog extends DialogFragment {

	public static String TAG = "downloaded_failed_dialog";
	private String reason_to_fail;
	/**
	 * @return a new instance of this DialogFragment.
	 */
	public static DialogFragment newInstance(String reason_to_fail) {
		DownloadFailedDialog dialog_fragment = new DownloadFailedDialog();
		dialog_fragment.reason_to_fail = reason_to_fail;
		return dialog_fragment;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		builder.setMessage(reason_to_fail)
		.setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dismiss();
				interface_with_ConfigurationWizard.retrySetUpProvider();
			}
		})
		.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				interface_with_ConfigurationWizard.cancelSettingUpProvider();
				dialog.dismiss();
			}
		});

		// Create the AlertDialog object and return it
		return builder.create();
	}

	public interface DownloadFailedDialogInterface {
		public void retrySetUpProvider();
		public void cancelSettingUpProvider();
	}

	DownloadFailedDialogInterface interface_with_ConfigurationWizard;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			interface_with_ConfigurationWizard = (DownloadFailedDialogInterface) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement NoticeDialogListener");
		}
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		interface_with_ConfigurationWizard.cancelSettingUpProvider();
		dialog.dismiss();
	}

}
