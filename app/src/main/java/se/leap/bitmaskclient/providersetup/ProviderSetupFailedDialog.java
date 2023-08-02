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
package se.leap.bitmaskclient.providersetup;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import org.json.JSONObject;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;

import static se.leap.bitmaskclient.providersetup.ProviderAPI.ERRORID;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.ERRORS;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INITIAL_ACTION;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.SET_UP_PROVIDER;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.UPDATE_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.UPDATE_PROVIDER_DETAILS;
import static se.leap.bitmaskclient.providersetup.ProviderSetupFailedDialog.DOWNLOAD_ERRORS.DEFAULT;
import static se.leap.bitmaskclient.providersetup.ProviderSetupFailedDialog.DOWNLOAD_ERRORS.valueOf;

/**
 * Implements a dialog to show why a download failed.
 *
 * @author parmegv
 */
public class ProviderSetupFailedDialog extends DialogFragment {

    public static String TAG = "downloaded_failed_dialog";
    private final static String KEY_PROVIDER = "key_provider";
    private final static String KEY_REASON_TO_FAIL = "key_reason_to_fail";
    private final static String KEY_DOWNLOAD_ERROR = "key_download_error";
    private final static String KEY_INITAL_ACTION = "key_inital_action";
    private String reasonToFail;
    private DOWNLOAD_ERRORS downloadError = DEFAULT;
    private String initialAction;

    private Provider provider;

    /**
     * Represent error types that need different error handling actions
     */
    public enum DOWNLOAD_ERRORS {
        DEFAULT,
        ERROR_CORRUPTED_PROVIDER_JSON,
        ERROR_INVALID_CERTIFICATE,
        ERROR_CERTIFICATE_PINNING,
        ERROR_NEW_URL_NO_VPN_PROVIDER,
        ERROR_TOR_TIMEOUT
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
    public static DialogFragment newInstance(Provider provider, JSONObject errorJson, boolean testNewURL) {
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
            } else if (testNewURL) {
                dialogFragment.downloadError = DOWNLOAD_ERRORS.ERROR_NEW_URL_NO_VPN_PROVIDER;
            }

            if (errorJson.has(INITIAL_ACTION)) {
                dialogFragment.initialAction = errorJson.getString(INITIAL_ACTION);
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
                .setNegativeButton(R.string.cancel, (dialog, id)
                        -> interfaceWithConfigurationWizard.cancelSettingUpProvider());
        switch (downloadError) {
            case ERROR_CORRUPTED_PROVIDER_JSON:
                builder.setPositiveButton(R.string.update_provider_details, (dialog, which)
                        -> interfaceWithConfigurationWizard.updateProviderDetails());
                break;
            case ERROR_CERTIFICATE_PINNING:
            case ERROR_INVALID_CERTIFICATE:
                builder.setPositiveButton(R.string.update_certificate, (dialog, which)
                        -> interfaceWithConfigurationWizard.updateProviderDetails());
                break;
            case ERROR_NEW_URL_NO_VPN_PROVIDER:
                builder.setPositiveButton(R.string.retry, (dialog, id)
                        -> interfaceWithConfigurationWizard.addAndSelectNewProvider(provider.getMainUrlString()));
                break;
            case ERROR_TOR_TIMEOUT:
                builder.setPositiveButton(R.string.retry, (dialog, id) -> {
                    handleTorTimeoutError();
                });
                builder.setNeutralButton(R.string.retry_unobfuscated, ((dialog, id) -> {
                    PreferenceHelper.useSnowflake(false);
                    handleTorTimeoutError();
                }));
            default:
                builder.setPositiveButton(R.string.retry, (dialog, id)
                        -> interfaceWithConfigurationWizard.retrySetUpProvider(provider));
                break;
        }

        // Create the AlertDialog object and return it
        return builder.create();
    }

    private void handleTorTimeoutError() {
        switch (initialAction) {
            case SET_UP_PROVIDER:
            case UPDATE_PROVIDER_DETAILS:
                interfaceWithConfigurationWizard.retrySetUpProvider(provider);
                break;
            case UPDATE_INVALID_VPN_CERTIFICATE:
                ProviderAPICommand.execute(getContext(), UPDATE_INVALID_VPN_CERTIFICATE, provider);
                break;
            default:
                break;
        }
    }

    public interface DownloadFailedDialogInterface {
        void retrySetUpProvider(@NonNull Provider provider);

        void cancelSettingUpProvider();

        void updateProviderDetails();

        void addAndSelectNewProvider(String url);
    }

    DownloadFailedDialogInterface interfaceWithConfigurationWizard;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            interfaceWithConfigurationWizard = (DownloadFailedDialogInterface) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement DownloadFailedDialogInterface");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        interfaceWithConfigurationWizard = null;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        dialog.dismiss();
        interfaceWithConfigurationWizard.cancelSettingUpProvider();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_PROVIDER, provider);
        outState.putString(KEY_REASON_TO_FAIL, reasonToFail);
        outState.putString(KEY_DOWNLOAD_ERROR, downloadError.toString());
        outState.putString(KEY_INITAL_ACTION, initialAction);
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
        if (savedInstanceState.containsKey(KEY_DOWNLOAD_ERROR)) {
            this.downloadError = valueOf(savedInstanceState.getString(KEY_DOWNLOAD_ERROR));
        }
        if (savedInstanceState.containsKey(KEY_INITAL_ACTION)) {
            this.initialAction = savedInstanceState.getString(KEY_INITAL_ACTION);
        }
    }
}
