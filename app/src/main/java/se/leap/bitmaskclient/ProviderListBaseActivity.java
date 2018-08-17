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
import static se.leap.bitmaskclient.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.Constants.REQUEST_CODE_ADD_PROVIDER;
import static se.leap.bitmaskclient.Constants.REQUEST_CODE_CONFIGURE_LEAP;
import static se.leap.bitmaskclient.ProviderAPI.DOWNLOAD_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.ProviderAPI.ERRORS;
import static se.leap.bitmaskclient.ProviderAPI.PROVIDER_SET_UP;
import static se.leap.bitmaskclient.ProviderAPI.UPDATE_PROVIDER_DETAILS;
import static se.leap.bitmaskclient.ProviderSetupInterface.ProviderConfigState.PENDING_SHOW_FAILED_DIALOG;
import static se.leap.bitmaskclient.ProviderSetupInterface.ProviderConfigState.PROVIDER_NOT_SET;
import static se.leap.bitmaskclient.ProviderSetupInterface.ProviderConfigState.SETTING_UP_PROVIDER;
import static se.leap.bitmaskclient.ProviderSetupInterface.ProviderConfigState.SHOWING_PROVIDER_DETAILS;
import static se.leap.bitmaskclient.ProviderSetupInterface.ProviderConfigState.SHOW_FAILED_DIALOG;

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
        implements ProviderSetupInterface, ProviderSetupFailedDialog.DownloadFailedDialogInterface, ProviderAPIResultReceiver.Receiver {

    @InjectView(R.id.provider_list)
    protected ListView providerListView;
    @Inject
    protected ProviderListAdapter adapter;

    private ProviderManager providerManager;

    final public static String TAG = ProviderListActivity.class.getSimpleName();

    final private static String ACTIVITY_STATE = "ACTIVITY STATE";

    protected ProviderConfigState providerConfigState = PROVIDER_NOT_SET;
    final private static String REASON_TO_FAIL = "REASON TO FAIL";
    final protected static String EXTRAS_KEY_INVALID_URL = "INVALID_URL";

    public ProviderAPIResultReceiver providerAPIResultReceiver;
    private ProviderApiSetupBroadcastReceiver providerAPIBroadcastReceiver;

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
        outState.putString(ACTIVITY_STATE, providerConfigState.toString());
        outState.putString(REASON_TO_FAIL, reasonToFail);

        super.onSaveInstanceState(outState);
    }

    protected void restoreState(Bundle savedInstanceState) {
        super.restoreState(savedInstanceState);
        if (savedInstanceState == null) {
            return;
        }
        this.providerConfigState = ProviderConfigState.valueOf(savedInstanceState.getString(ACTIVITY_STATE, PROVIDER_NOT_SET.toString()));
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
        Log.d(TAG, "resuming with ConfigState: " + providerConfigState.toString());
        super.onResume();
        setUpProviderAPIResultReceiver();
        isActivityShowing = true;
        if (SETTING_UP_PROVIDER == providerConfigState) {
            showProgressBar();
            checkProviderSetUp();
        } else if (PENDING_SHOW_FAILED_DIALOG == providerConfigState) {
            showProgressBar();
            showDownloadFailedDialog();
        } else if (SHOW_FAILED_DIALOG == providerConfigState) {
            showProgressBar();
        } else if (SHOWING_PROVIDER_DETAILS == providerConfigState) {
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
            } else {
                cancelSettingUpProvider();
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
        providerAPIBroadcastReceiver = new ProviderApiSetupBroadcastReceiver(this);

        IntentFilter updateIntentFilter = new IntentFilter(BROADCAST_PROVIDER_API_EVENT);
        updateIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(this).registerReceiver(providerAPIBroadcastReceiver, updateIntentFilter);
    }

    @Override
    public void handleProviderSetUp(Provider handledProvider) {
        this.provider = handledProvider;
        adapter.add(provider);
        adapter.saveProviders();
        if (provider.allowsAnonymous()) {
            //FIXME: providerApiBroadcastReceiver.getConfigState().putExtra(SERVICES_RETRIEVED, true); DEAD CODE???
            downloadVpnCertificate();
        } else {
            showProviderDetails();
        }
    }

    @Override
    public void handleProviderSetupFailed(Bundle resultData) {
        reasonToFail = resultData.getString(ERRORS);
        showDownloadFailedDialog();
    }

    @Override
    public void handleCorrectlyDownloadedCertificate(Provider handledProvider) {
        this.provider = handledProvider;
        showProviderDetails();
    }

    @Override
    public void handleIncorrectlyDownloadedCertificate() {
        cancelSettingUpProvider();
        setResult(RESULT_CANCELED, new Intent(getConfigState().toString()));
    }

    public Provider getProvider() {
        return provider;
    }

    public ProviderConfigState getConfigState() {
        return providerConfigState;
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
        if (SETTING_UP_PROVIDER == getConfigState() ||
                SHOW_FAILED_DIALOG == getConfigState()) {
            return;
        }

        //TODO Code 2 pane view
        provider = adapter.getItem(position);
        if (provider != null && !provider.isDefault()) {
            //TODO Code 2 pane view
            providerConfigState = SETTING_UP_PROVIDER;
            showProgressBar();
            onItemSelectedLogic();
        } else {
            addAndSelectNewProvider();
        }
    }

    @Override
    public void onBackPressed() {
        if (SETTING_UP_PROVIDER == providerConfigState ||
                SHOW_FAILED_DIALOG == providerConfigState) {
            cancelSettingUpProvider();
        } else {
            super.onBackPressed();
        }
    }

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
        }
    }
}
