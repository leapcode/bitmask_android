/**
 * Copyright (c) 2018 LEAP Encryption Access Project and contributers
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

import static se.leap.bitmaskclient.MainActivityErrorDialog.DOWNLOAD_ERRORS.*;
import static se.leap.bitmaskclient.ProviderAPI.DOWNLOAD_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.eip.EIP.ERRORS;
import static se.leap.bitmaskclient.eip.EIP.ERROR_ID;

/**
 * Implements a error dialog for the main activity.
 *
 * @author fupduck
 * @author cyberta
 */
public class MainActivityErrorDialog extends DialogFragment {

    public static String TAG = "downloaded_failed_dialog";
    private String reasonToFail;
    private DOWNLOAD_ERRORS downloadError = DEFAULT;

    private MainActivityErrorDialogInterface callbackInterface;
    private Provider provider;

    public enum DOWNLOAD_ERRORS {
        DEFAULT,
        ERROR_INVALID_VPN_CERTIFICATE,
    }

    /**
     * @return a new instance of this DialogFragment.
     */
    public static DialogFragment newInstance(Provider provider, String reasonToFail) {
        MainActivityErrorDialog dialogFragment = new MainActivityErrorDialog();
        dialogFragment.reasonToFail = reasonToFail;
        dialogFragment.provider = provider;
        return dialogFragment;
    }

    /**
     * @return a new instance of this DialogFragment.
     */
    public static DialogFragment newInstance(Provider provider, JSONObject errorJson) {
        MainActivityErrorDialog dialogFragment = new MainActivityErrorDialog();
        dialogFragment.provider = provider;
        try {
            if (errorJson.has(ERRORS)) {
                dialogFragment.reasonToFail = errorJson.getString(ERRORS);
            } else {
                //default error msg
                dialogFragment.reasonToFail = dialogFragment.getString(R.string.error_io_exception_user_message);
            }

            if (errorJson.has(ERROR_ID)) {
                dialogFragment.downloadError = valueOf(errorJson.getString(ERROR_ID));
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
                dialog.dismiss();
                callbackInterface.onDialogDismissed();
            }
        });
        switch (downloadError) {
            case ERROR_INVALID_VPN_CERTIFICATE:
                builder.setPositiveButton(R.string.update_certificate, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                        ProviderAPICommand.execute(getContext(), DOWNLOAD_VPN_CERTIFICATE, provider);
                        callbackInterface.onDialogDismissed();
                    }
                });
                break;
            default:
                builder.setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dismiss();
                                callbackInterface.onDialogDismissed();
                            }
                        });
                break;
        }

        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public void dismiss() {
        super.dismiss();
    }

    public interface MainActivityErrorDialogInterface {
        void onDialogDismissed();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            callbackInterface = (MainActivityErrorDialogInterface) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        callbackInterface.onDialogDismissed();
    }
}
