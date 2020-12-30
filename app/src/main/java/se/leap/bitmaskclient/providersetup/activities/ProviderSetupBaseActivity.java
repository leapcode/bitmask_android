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
package se.leap.bitmaskclient.providersetup.activities;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONException;
import org.json.JSONObject;

import se.leap.bitmaskclient.base.FragmentManagerEnhanced;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.providersetup.ProviderAPICommand;
import se.leap.bitmaskclient.providersetup.ProviderDetailActivity;
import se.leap.bitmaskclient.providersetup.ProviderApiSetupBroadcastReceiver;
import se.leap.bitmaskclient.providersetup.ProviderManager;
import se.leap.bitmaskclient.providersetup.ProviderSetupFailedDialog;
import se.leap.bitmaskclient.providersetup.ProviderSetupInterface;

import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_PROVIDER_API_EVENT;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.base.models.Constants.REQUEST_CODE_CONFIGURE_LEAP;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.DOWNLOAD_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.ERRORS;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.UPDATE_PROVIDER_DETAILS;
import static se.leap.bitmaskclient.providersetup.ProviderSetupInterface.ProviderConfigState.PENDING_SHOW_FAILED_DIALOG;
import static se.leap.bitmaskclient.providersetup.ProviderSetupInterface.ProviderConfigState.PENDING_SHOW_PROVIDER_DETAILS;
import static se.leap.bitmaskclient.providersetup.ProviderSetupInterface.ProviderConfigState.PROVIDER_NOT_SET;
import static se.leap.bitmaskclient.providersetup.ProviderSetupInterface.ProviderConfigState.SETTING_UP_PROVIDER;
import static se.leap.bitmaskclient.providersetup.ProviderSetupInterface.ProviderConfigState.SHOWING_PROVIDER_DETAILS;
import static se.leap.bitmaskclient.providersetup.ProviderSetupInterface.ProviderConfigState.SHOW_FAILED_DIALOG;

/**
 * Created by cyberta on 19.08.18.
 */

public abstract class ProviderSetupBaseActivity extends ConfigWizardBaseActivity implements ProviderSetupInterface, ProviderSetupFailedDialog.DownloadFailedDialogInterface {
    final public static String TAG = "PoviderSetupActivity";
    final private static String ACTIVITY_STATE = "ACTIVITY STATE";
    final private static String REASON_TO_FAIL = "REASON TO FAIL";

    protected ProviderSetupInterface.ProviderConfigState providerConfigState = PROVIDER_NOT_SET;
    private ProviderManager providerManager;
    private FragmentManagerEnhanced fragmentManager;

    private String reasonToFail;
    protected boolean testNewURL;

    private ProviderApiSetupBroadcastReceiver providerAPIBroadcastReceiver;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentManager = new FragmentManagerEnhanced(getSupportFragmentManager());
        providerManager = ProviderManager.getInstance(getAssets(), getExternalFilesDir(null));
        setUpProviderAPIResultReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "resuming with ConfigState: " + providerConfigState.toString());
        if (SETTING_UP_PROVIDER == providerConfigState) {
            showProgressBar();
        } else if (PENDING_SHOW_FAILED_DIALOG == providerConfigState) {
            showProgressBar();
            showDownloadFailedDialog();
        } else if (SHOW_FAILED_DIALOG == providerConfigState) {
            showProgressBar();
        } else if (SHOWING_PROVIDER_DETAILS == providerConfigState) {
            cancelSettingUpProvider();
        } else if (PENDING_SHOW_PROVIDER_DETAILS == providerConfigState) {
            showProviderDetails();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (providerAPIBroadcastReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(providerAPIBroadcastReceiver);
        }
        providerAPIBroadcastReceiver = null;
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(ACTIVITY_STATE, providerConfigState.toString());
        outState.putString(REASON_TO_FAIL, reasonToFail);

        super.onSaveInstanceState(outState);
    }

    protected FragmentManagerEnhanced getFragmentManagerEnhanced() {
        return fragmentManager;
    }

    protected ProviderManager getProviderManager() {
        return providerManager;
    }

    protected void setProviderConfigState(ProviderConfigState state) {
        this.providerConfigState = state;
    }

    protected void setProvider(Provider provider) {
        this.provider = provider;
    }

    // --------- ProviderSetupInterface ---v
    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public ProviderConfigState getConfigState() {
        return providerConfigState;
    }

    @Override
    public void handleProviderSetupFailed(Bundle resultData) {
        reasonToFail = resultData.getString(ERRORS);
        showDownloadFailedDialog();
    }

    @Override
    public void handleIncorrectlyDownloadedCertificate() {
        cancelSettingUpProvider();
        setResult(RESULT_CANCELED, new Intent(getConfigState().toString()));
    }

    // -------- DownloadFailedDialogInterface ---v
    @Override
    public void cancelSettingUpProvider() {
        providerConfigState = PROVIDER_NOT_SET;
        provider = null;
        hideProgressBar();
    }

    @Override
    public void updateProviderDetails() {
        providerConfigState = SETTING_UP_PROVIDER;
        ProviderAPICommand.execute(this, UPDATE_PROVIDER_DETAILS, provider);
    }

    protected void restoreState(Bundle savedInstanceState) {
        super.restoreState(savedInstanceState);
        if (savedInstanceState == null) {
            return;
        }
        this.providerConfigState = ProviderSetupInterface.ProviderConfigState.valueOf(savedInstanceState.getString(ACTIVITY_STATE, PROVIDER_NOT_SET.toString()));
        if (savedInstanceState.containsKey(REASON_TO_FAIL)) {
            reasonToFail = savedInstanceState.getString(REASON_TO_FAIL);
        }
    }

    private void setUpProviderAPIResultReceiver() {
        providerAPIBroadcastReceiver = new ProviderApiSetupBroadcastReceiver(this);

        IntentFilter updateIntentFilter = new IntentFilter(BROADCAST_PROVIDER_API_EVENT);
        updateIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(this).registerReceiver(providerAPIBroadcastReceiver, updateIntentFilter);
    }

    /**
     * Asks ProviderApiService to download an anonymous (anon) VPN certificate.
     */
    protected void downloadVpnCertificate() {
        ProviderAPICommand.execute(this, DOWNLOAD_VPN_CERTIFICATE, provider);
    }

    /**
     * Once selected a provider, this fragment offers the user to log in,
     * use it anonymously (if possible)
     * or cancel his/her election pressing the back button.
     */
    public void showProviderDetails() {
        // show only if current activity is shown
        if (isActivityShowing &&
                providerConfigState != SHOWING_PROVIDER_DETAILS) {
            providerConfigState = SHOWING_PROVIDER_DETAILS;
            Intent intent = new Intent(this, ProviderDetailActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.putExtra(PROVIDER_KEY, provider);
            startActivityForResult(intent, REQUEST_CODE_CONFIGURE_LEAP);
        } else {
            providerConfigState = PENDING_SHOW_PROVIDER_DETAILS;
        }
    }

    /**
     * Shows an error dialog, if configuring of a provider failed.
     */
    public void showDownloadFailedDialog() {
        try {
            providerConfigState = SHOW_FAILED_DIALOG;
            FragmentTransaction fragmentTransaction = fragmentManager.removePreviousFragment(ProviderSetupFailedDialog.TAG);
            DialogFragment newFragment;
            try {
                JSONObject errorJson = new JSONObject(reasonToFail);
                newFragment = ProviderSetupFailedDialog.newInstance(provider, errorJson, testNewURL);
            } catch (JSONException e) {
                e.printStackTrace();
                newFragment = ProviderSetupFailedDialog.newInstance(provider, reasonToFail);
            } catch (NullPointerException e) {
                //reasonToFail was null
                return;
            }
            newFragment.show(fragmentTransaction, ProviderSetupFailedDialog.TAG);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            providerConfigState = PENDING_SHOW_FAILED_DIALOG;
        }
    }

}
