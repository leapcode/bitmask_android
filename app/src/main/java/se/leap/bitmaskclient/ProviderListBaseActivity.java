/**
 * Copyright (c) 2017 LEAP Encryption Access Project and contributors
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package se.leap.bitmaskclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ListView;

import com.pedrogomez.renderers.Renderer;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.InjectView;
import butterknife.OnItemClick;
import se.leap.bitmaskclient.fragments.AboutFragment;

import static se.leap.bitmaskclient.Constants.BROADCAST_PROVIDER_API_EVENT;
import static se.leap.bitmaskclient.Constants.BROADCAST_RESULT_CODE;
import static se.leap.bitmaskclient.Constants.BROADCAST_RESULT_KEY;
import static se.leap.bitmaskclient.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.Constants.REQUEST_CODE_ADD_PROVIDER;
import static se.leap.bitmaskclient.Constants.REQUEST_CODE_CONFIGURE_LEAP;
import static se.leap.bitmaskclient.ProviderAPI.CORRECTLY_DOWNLOADED_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.ProviderAPI.DOWNLOAD_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.ProviderAPI.ERRORS;
import static se.leap.bitmaskclient.ProviderAPI.INCORRECTLY_DOWNLOADED_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.ProviderAPI.PROVIDER_NOK;
import static se.leap.bitmaskclient.ProviderAPI.PROVIDER_OK;
import static se.leap.bitmaskclient.ProviderAPI.PROVIDER_SET_UP;
import static se.leap.bitmaskclient.ProviderAPI.UPDATE_PROVIDER_DETAILS;

/**
 * abstract base Activity that builds and shows the list of known available providers.
 * The implementation of ProviderListBaseActivity differ in that they may or may not allow to bypass
 * secure download mechanisms including certificate validation.
 * <p/>
 * It also allows the user to enter custom providers with a button.
 *
 * @author parmegv
 * @author cyberta
 */

public abstract class ProviderListBaseActivity extends ConfigWizardBaseActivity
        implements ProviderSetupFailedDialog.DownloadFailedDialogInterface, ProviderAPIResultReceiver.Receiver {

    @InjectView(R.id.provider_list)
    protected ListView providerListView;
    @Inject
    protected ProviderListAdapter adapter;

    private ProviderManager providerManager;
    protected Intent configState = new Intent(PROVIDER_NOT_SET);

    final public static String TAG = ProviderListActivity.class.getSimpleName();

    final private static String ACTIVITY_STATE = "ACTIVITY STATE";

    final protected static String PROVIDER_NOT_SET = "PROVIDER NOT SET";
    final protected static String SETTING_UP_PROVIDER = "PROVIDER GETS SET";
    final private static String SHOWING_PROVIDER_DETAILS = "SHOWING PROVIDER DETAILS";
    final private static String PENDING_SHOW_FAILED_DIALOG = "SHOW FAILED DIALOG PENDING";
    final private static String SHOW_FAILED_DIALOG = "SHOW FAILED DIALOG";
    final private static String REASON_TO_FAIL = "REASON TO FAIL";
    final protected static String SERVICES_RETRIEVED = "SERVICES RETRIEVED";
    final protected static String EXTRAS_KEY_INVALID_URL = "INVALID_URL";

    public ProviderAPIResultReceiver providerAPIResultReceiver;
    private ProviderAPIBroadcastReceiver providerAPIBroadcastReceiver;

    private FragmentManagerEnhanced fragmentManager;

    private boolean isActivityShowing;
    private String reasonToFail;
    private boolean testNewURL;

    public abstract void retrySetUpProvider(@NonNull Provider provider);

    protected abstract void onItemSelectedLogic();

    private void initProviderList() {
        List<Renderer<Provider>> prototypes = new ArrayList<>();
        prototypes.add(new ProviderRenderer(this));
        ProviderRendererBuilder providerRendererBuilder = new ProviderRendererBuilder(prototypes);
        adapter = new ProviderListAdapter(getLayoutInflater(), providerRendererBuilder, providerManager);
        providerListView.setAdapter(adapter);
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        outState.putString(ACTIVITY_STATE, configState.getAction());
        outState.putString(REASON_TO_FAIL, reasonToFail);

        super.onSaveInstanceState(outState);
    }

    protected void restoreState(Bundle savedInstanceState) {
        super.restoreState(savedInstanceState);
        if (savedInstanceState == null) {
            return;
        }
        configState.setAction(savedInstanceState.getString(ACTIVITY_STATE, PROVIDER_NOT_SET));
        if (savedInstanceState.containsKey(REASON_TO_FAIL)) {
            reasonToFail = savedInstanceState.getString(REASON_TO_FAIL);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentManager = new FragmentManagerEnhanced(getSupportFragmentManager());
        providerManager = ProviderManager.getInstance(getAssets(), getExternalFilesDir(null));

        setUpInitialUI();
        initProviderList();
        restoreState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "resuming with ConfigState: " + configState.getAction());
        super.onResume();
        setUpProviderAPIResultReceiver();
        isActivityShowing = true;
        if (SETTING_UP_PROVIDER.equals(configState.getAction())) {
            showProgressBar();
            checkProviderSetUp();
        } else if (PENDING_SHOW_FAILED_DIALOG.equals(configState.getAction())) {
            showProgressBar();
            showDownloadFailedDialog();
        } else if (SHOW_FAILED_DIALOG.equals(configState.getAction())) {
            showProgressBar();
        } else if (SHOWING_PROVIDER_DETAILS.equals(configState.getAction())) {
            cancelSettingUpProvider();
        }
    }

    private void setUpInitialUI() {
        setContentView(R.layout.a_provider_list);
        setProviderHeaderText(R.string.setup_provider);
        hideProgressBar();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityShowing = false;
        if (providerAPIBroadcastReceiver != null)
            LocalBroadcastManager.getInstance(this).unregisterReceiver(providerAPIBroadcastReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        providerAPIResultReceiver = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CONFIGURE_LEAP) {
            if (resultCode == RESULT_OK) {
                setResult(resultCode, data);
                finish();
            }
        } else if (requestCode == REQUEST_CODE_ADD_PROVIDER) {
            if (resultCode == RESULT_OK) {
                testNewURL = true;
                String newUrl = data.getStringExtra(AddProviderActivity.EXTRAS_KEY_NEW_URL);
                this.provider.setMainUrl(newUrl);
                showAndSelectProvider(newUrl);
            }
        }
    }

    public void showAndSelectProvider(String newURL) {
        try {
            provider = new Provider(new URL((newURL)));
            autoSelectProvider();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private void autoSelectProvider() {
        onItemSelectedLogic();
        showProgressBar();
    }


    private void setUpProviderAPIResultReceiver() {
        providerAPIResultReceiver = new ProviderAPIResultReceiver(new Handler(), this);
        providerAPIBroadcastReceiver = new ProviderAPIBroadcastReceiver();

        IntentFilter updateIntentFilter = new IntentFilter(BROADCAST_PROVIDER_API_EVENT);
        updateIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(this).registerReceiver(providerAPIBroadcastReceiver, updateIntentFilter);
    }

    void handleProviderSetUp(Provider handledProvider) {
        this.provider = handledProvider;
        adapter.add(provider);
        adapter.saveProviders();
        if (provider.allowsAnonymous()) {
            configState.putExtra(SERVICES_RETRIEVED, true);
            downloadVpnCertificate();
        } else {
            showProviderDetails();
        }
    }

    void handleProviderSetupFailed(Bundle resultData) {

        reasonToFail = resultData.getString(ERRORS);
        showDownloadFailedDialog();
    }

    void handleCorrectlyDownloadedCertificate(Provider handledProvider) {
        this.provider = handledProvider;
        showProviderDetails();
    }

    void handleIncorrectlyDownloadedCertificate() {
        cancelSettingUpProvider();
        setResult(RESULT_CANCELED, configState);
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        if (resultCode == ProviderAPI.PROVIDER_OK) {
            Provider provider = resultData.getParcelable(PROVIDER_KEY);
            handleProviderSetUp(provider);
        } else if (resultCode == AboutFragment.VIEWED) {
            // Do nothing, right now
            // I need this for CW to wait for the About activity to end before going back to Dashboard.
        }
    }

    @OnItemClick(R.id.provider_list)
    void onItemSelected(int position) {
        if (SETTING_UP_PROVIDER.equals(configState.getAction()) ||
                SHOW_FAILED_DIALOG.equals(configState.getAction())) {
            return;
        }

        //TODO Code 2 pane view
        provider = adapter.getItem(position);
        if (provider != null && !provider.isDefault()) {
            //TODO Code 2 pane view
            configState.setAction(SETTING_UP_PROVIDER);
            showProgressBar();
            onItemSelectedLogic();
        } else {
            addAndSelectNewProvider();
        }
    }

    @Override
    public void onBackPressed() {
        if (SETTING_UP_PROVIDER.equals(configState.getAction()) ||
                SHOW_FAILED_DIALOG.equals(configState.getAction())) {
            stopSettingUpProvider();
        } else {
            super.onBackPressed();
        }
    }

    private void stopSettingUpProvider() {
        cancelSettingUpProvider();
    }

    @Override
    public void cancelSettingUpProvider() {
        configState.setAction(PROVIDER_NOT_SET);
        provider = null;
        hideProgressBar();
    }

    @Override
    public void updateProviderDetails() {
        configState.setAction(SETTING_UP_PROVIDER);
        ProviderAPICommand.execute(this, UPDATE_PROVIDER_DETAILS, provider);
    }

    public void checkProviderSetUp() {
        ProviderAPICommand.execute(this, PROVIDER_SET_UP, provider, providerAPIResultReceiver);
    }

    /**
     * Asks ProviderApiService to download an anonymous (anon) VPN certificate.
     */
    private void downloadVpnCertificate() {
        ProviderAPICommand.execute(this, DOWNLOAD_VPN_CERTIFICATE, provider);
    }


    /**
     * Open the new provider dialog
     */
    public void addAndSelectNewProvider() {
        Intent intent = new Intent(this, AddProviderActivity.class);
        startActivityForResult(intent, REQUEST_CODE_ADD_PROVIDER);
    }

    /**
     * Open the new provider dialog
     */
    @Override
    public void addAndSelectNewProvider(String url) {
        testNewURL = false;
        Intent intent = new Intent(this, AddProviderActivity.class);
        intent.putExtra(EXTRAS_KEY_INVALID_URL, url);
        startActivityForResult(intent, REQUEST_CODE_ADD_PROVIDER);
    }

    /**
     * Shows an error dialog, if configuring of a provider failed.
     */
    public void showDownloadFailedDialog() {
        try {
            configState.setAction(SHOW_FAILED_DIALOG);
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
            configState.setAction(PENDING_SHOW_FAILED_DIALOG);
        }
    }

    /**
     * Once selected a provider, this fragment offers the user to log in,
     * use it anonymously (if possible)
     * or cancel his/her election pressing the back button.
     */
    public void showProviderDetails() {
        // show only if current activity is shown
        if (isActivityShowing && configState.getAction() != null &&
                !configState.getAction().equalsIgnoreCase(SHOWING_PROVIDER_DETAILS)) {
            configState.setAction(SHOWING_PROVIDER_DETAILS);
            Intent intent = new Intent(this, ProviderDetailActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.putExtra(PROVIDER_KEY, provider);
            startActivityForResult(intent, REQUEST_CODE_CONFIGURE_LEAP);
        }
    }

    public class ProviderAPIBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "received Broadcast");

            String action = intent.getAction();
            if (action == null || !action.equalsIgnoreCase(BROADCAST_PROVIDER_API_EVENT)) {
                return;
            }

            if (configState.getAction() != null &&
                    configState.getAction().equalsIgnoreCase(SETTING_UP_PROVIDER)) {
                int resultCode = intent.getIntExtra(BROADCAST_RESULT_CODE, RESULT_CANCELED);
                Log.d(TAG, "Broadcast resultCode: " + Integer.toString(resultCode));

                Bundle resultData = intent.getParcelableExtra(BROADCAST_RESULT_KEY);
                Provider handledProvider = resultData.getParcelable(PROVIDER_KEY);

                if (handledProvider != null && provider != null &&
                        handledProvider.getDomain().equalsIgnoreCase(provider.getDomain())) {
                    switch (resultCode) {
                        case PROVIDER_OK:
                            handleProviderSetUp(handledProvider);
                            break;
                        case PROVIDER_NOK:
                            handleProviderSetupFailed(resultData);
                            break;
                        case CORRECTLY_DOWNLOADED_VPN_CERTIFICATE:
                            handleCorrectlyDownloadedCertificate(handledProvider);
                            break;
                        case INCORRECTLY_DOWNLOADED_VPN_CERTIFICATE:
                            handleIncorrectlyDownloadedCertificate();
                            break;
                    }
                }
            }
        }
    }
}
