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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import org.json.JSONObject;

import static se.leap.bitmaskclient.ProviderSetupFailedDialog.DOWNLOAD_ERRORS.DEFAULT;
import static se.leap.bitmaskclient.ProviderSetupFailedDialog.DOWNLOAD_ERRORS.valueOf;
import static se.leap.bitmaskclient.ProviderAPI.ERRORID;
import static se.leap.bitmaskclient.ProviderAPI.ERRORS;

/**
 * Implements a dialog to show why a download failed.
 *
 * @author parmegv
 */
public class ProviderSetupFailedDialog extends DialogFragment {

    public static String TAG = "downloaded_failed_dialog";
    private String reasonToFail;
    private DOWNLOAD_ERRORS downloadError = DEFAULT;

    private Provider provider;

    public enum DOWNLOAD_ERRORS {
        DEFAULT,
        ERROR_CORRUPTED_PROVIDER_JSON,
        ERROR_INVALID_CERTIFICATE,
        ERROR_CERTIFICATE_PINNING
    }

    /**
     * @return a new instance of this DialogFragment.
     */
    public static DialogFragment newInstance(Provider provider, String reasonToFail) {
        ProviderSetupFailedDialog dialogFragment = new ProviderSetupFailedDialog();
        dialogFragment.reasonToFail = reasonToFail;
        dialogFragment.provider = provider;
        return dialogFragment;
    }

    /**
     * @return a new instance of this DialogFragment.
     */
    public static DialogFragment newInstance(Provider provider, JSONObject errorJson) {
        ProviderSetupFailedDialog dialogFragment = new ProviderSetupFailedDialog();
        dialogFragment.provider = provider;
        try {
            if (errorJson.has(ERRORS)) {
                dialogFragment.reasonToFail = errorJson.getString(ERRORS);
            } else {
                //default error msg
                dialogFragment.reasonToFail = dialogFragment.getString(R.string.error_io_exception_user_message);
            }

            if (errorJson.has(ERRORID)) {
                dialogFragment.downloadError = valueOf(errorJson.getString(ERRORID));
            }
        } catch (Exception e) {
            e.printStackTrace();
            dialogFragment.reasonToFail = dialogFragment.getString(R.string.error_io_exception_user_message);
        }
        return dialogFragment;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(reasonToFail)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                interfaceWithConfigurationWizard.cancelSettingUpProvider();
                dialog.dismiss();
            }
        });
        switch (downloadError) {
            case ERROR_CORRUPTED_PROVIDER_JSON:
                builder.setPositiveButton(R.string.update_provider_details, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                        interfaceWithConfigurationWizard.updateProviderDetails();
                    }
                });
                break;
            case ERROR_CERTIFICATE_PINNING:
            case ERROR_INVALID_CERTIFICATE:
                builder.setPositiveButton(R.string.update_certificate, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                        interfaceWithConfigurationWizard.updateProviderDetails();
                    }
                });
                break;
            default:
                builder.setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dismiss();
                                interfaceWithConfigurationWizard.retrySetUpProvider(provider);
                            }
                        });
                break;
        }

        // Create the AlertDialog object and return it
        return builder.create();
    }

    public interface DownloadFailedDialogInterface {
        void retrySetUpProvider(@NonNull Provider provider);

        void cancelSettingUpProvider();

        void updateProviderDetails();
    }

    DownloadFailedDialogInterface interfaceWithConfigurationWizard;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            interfaceWithConfigurationWizard = (DownloadFailedDialogInterface) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        interfaceWithConfigurationWizard.cancelSettingUpProvider();
        dialog.dismiss();
    }

}
