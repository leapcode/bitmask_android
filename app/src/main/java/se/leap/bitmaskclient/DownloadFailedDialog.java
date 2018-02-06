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
import android.app.AlertDialog;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import org.json.JSONObject;

import static se.leap.bitmaskclient.DownloadFailedDialog.DOWNLOAD_ERRORS.DEFAULT;
import static se.leap.bitmaskclient.DownloadFailedDialog.DOWNLOAD_ERRORS.valueOf;
import static se.leap.bitmaskclient.ProviderAPI.ERRORID;
import static se.leap.bitmaskclient.ProviderAPI.ERRORS;

/**
 * Implements a dialog to show why a download failed.
 *
 * @author parmegv
 */
public class DownloadFailedDialog extends DialogFragment {

    public static String TAG = "downloaded_failed_dialog";
    private String reason_to_fail;
    private DOWNLOAD_ERRORS downloadError = DEFAULT;
    public enum DOWNLOAD_ERRORS {
        DEFAULT,
        ERROR_CORRUPTED_PROVIDER_JSON,
        ERROR_INVALID_CERTIFICATE,
        ERROR_CERTIFICATE_PINNING
    }

    /**
     * @return a new instance of this DialogFragment.
     */
    public static DialogFragment newInstance(String reason_to_fail) {
        DownloadFailedDialog dialog_fragment = new DownloadFailedDialog();
        dialog_fragment.reason_to_fail = reason_to_fail;
        return dialog_fragment;
    }

    /**
     * @return a new instance of this DialogFragment.
     */
    public static DialogFragment newInstance(JSONObject errorJson) {
        DownloadFailedDialog dialog_fragment = new DownloadFailedDialog();
        try {
            if (errorJson.has(ERRORS)) {
                dialog_fragment.reason_to_fail = errorJson.getString(ERRORS);
            } else {
                //default error msg
                dialog_fragment.reason_to_fail = dialog_fragment.getString(R.string.error_io_exception_user_message);
            }

            if (errorJson.has(ERRORID)) {
                dialog_fragment.downloadError = valueOf(errorJson.getString(ERRORID));
            }
        } catch (Exception e) {
            e.printStackTrace();
            dialog_fragment.reason_to_fail = dialog_fragment.getString(R.string.error_io_exception_user_message);
        }
        return dialog_fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(reason_to_fail)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                interface_with_ConfigurationWizard.cancelSettingUpProvider();
                dialog.dismiss();
            }
        });
switch (downloadError) {
            case ERROR_CORRUPTED_PROVIDER_JSON:
                builder.setPositiveButton(R.string.update_provider_details, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                        interface_with_ConfigurationWizard.updateProviderDetails();
                    }
                });
                break;
            case ERROR_CERTIFICATE_PINNING:
            case ERROR_INVALID_CERTIFICATE:
                builder.setPositiveButton(R.string.update_certificate, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                        interface_with_ConfigurationWizard.updateProviderDetails();
                    }
                });
                break;
            default:
                builder.setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dismiss();
                                interface_with_ConfigurationWizard.retrySetUpProvider(null);
                            }
                        });
                break;
        }

        // Create the AlertDialog object and return it
        return builder.create();
    }

    public interface DownloadFailedDialogInterface {
        void retrySetUpProvider(Provider provider);

        void cancelSettingUpProvider();

        void updateProviderDetails();
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
