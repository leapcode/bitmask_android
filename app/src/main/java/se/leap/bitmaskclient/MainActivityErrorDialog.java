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

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import org.json.JSONObject;

import se.leap.bitmaskclient.eip.EIP;

import static se.leap.bitmaskclient.ProviderAPI.UPDATE_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.eip.EIP.EIPErrors.UNKNOWN;
import static se.leap.bitmaskclient.eip.EIP.EIPErrors.valueOf;
import static se.leap.bitmaskclient.eip.EIP.ERRORS;
import static se.leap.bitmaskclient.eip.EIP.ERROR_ID;

/**
 * Implements an error dialog for the main activity.
 *
 * @author fupduck
 * @author cyberta
 */
public class MainActivityErrorDialog extends DialogFragment {

    final public static String TAG = "downloaded_failed_dialog";
    final private static String KEY_REASON_TO_FAIL = "key reason to fail";
    final private static String KEY_PROVIDER = "key provider";
    private String reasonToFail;
    private EIP.EIPErrors downloadError = UNKNOWN;

    private Provider provider;

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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        restoreFromSavedInstance(savedInstanceState);
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(reasonToFail)
                .setNegativeButton(R.string.cancel, (dialog, id) -> {
                });
        switch (downloadError) {
            case ERROR_INVALID_VPN_CERTIFICATE:
                builder.setPositiveButton(R.string.update_certificate, (dialog, which) ->
                        ProviderAPICommand.execute(getContext(), UPDATE_INVALID_VPN_CERTIFICATE, provider));
                break;
            default:
                break;
        }

        // Create the AlertDialog object and return it
        return builder.create();
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_REASON_TO_FAIL, reasonToFail);
        outState.putParcelable(KEY_PROVIDER, provider);
    }

    private void restoreFromSavedInstance(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        if (savedInstanceState.containsKey(KEY_PROVIDER)) {
            this.provider = savedInstanceState.getParcelable(KEY_PROVIDER);
        }
        if (savedInstanceState.containsKey(KEY_REASON_TO_FAIL)) {
            this.reasonToFail = savedInstanceState.getString(KEY_REASON_TO_FAIL);
        }
    }

}
