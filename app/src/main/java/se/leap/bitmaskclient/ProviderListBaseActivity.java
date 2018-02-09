/**
 * Copyright (c) 2017 LEAP Encryption Access Project and contributors
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
import android.view.Menu;
import android.widget.ListView;

import com.pedrogomez.renderers.Renderer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.InjectView;
import butterknife.OnItemClick;
import se.leap.bitmaskclient.fragments.AboutFragment;

import static se.leap.bitmaskclient.Constants.APP_ACTION_QUIT;
import static se.leap.bitmaskclient.Constants.BROADCAST_PROVIDER_API_EVENT;
import static se.leap.bitmaskclient.Constants.BROADCAST_RESULT_CODE;
import static se.leap.bitmaskclient.Constants.BROADCAST_RESULT_KEY;
import static se.leap.bitmaskclient.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.Constants.REQUEST_CODE_CONFIGURE_LEAP;
import static se.leap.bitmaskclient.ProviderAPI.CORRECTLY_DOWNLOADED_CERTIFICATE;
import static se.leap.bitmaskclient.ProviderAPI.DOWNLOAD_CERTIFICATE;
import static se.leap.bitmaskclient.ProviderAPI.ERRORS;
import static se.leap.bitmaskclient.ProviderAPI.INCORRECTLY_DOWNLOADED_CERTIFICATE;
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
        implements NewProviderDialog.NewProviderDialogInterface, DownloadFailedDialog.DownloadFailedDialogInterface, ProviderAPIResultReceiver.Receiver {

    @InjectView(R.id.provider_list)
    protected ListView providerListView;
    @Inject
    protected ProviderListAdapter adapter;

    private ProviderManager providerManager;
    protected Intent mConfigState = new Intent(PROVIDER_NOT_SET);

    final public static String TAG = ProviderListActivity.class.getSimpleName();

    final private static String ACTIVITY_STATE = "ACTIVITY STATE";

    final protected static String PROVIDER_NOT_SET = "PROVIDER NOT SET";
    final protected static String SETTING_UP_PROVIDER = "PROVIDER GETS SET";
    final private static  String SHOWING_PROVIDER_DETAILS = "SHOWING PROVIDER DETAILS";
    final private static String PENDING_SHOW_FAILED_DIALOG = "SHOW FAILED DIALOG";
    final private static String REASON_TO_FAIL = "REASON TO FAIL";
    final protected static String SERVICES_RETRIEVED = "SERVICES RETRIEVED";

    public ProviderAPIResultReceiver providerAPIResultReceiver;
    private ProviderAPIBroadcastReceiver providerAPIBroadcastReceiver;

    FragmentManagerEnhanced fragmentManager;

    private boolean isActivityShowing;
    private String reasonToFail;

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
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        outState.putString(ACTIVITY_STATE, mConfigState.getAction());
        outState.putParcelable(PROVIDER_KEY, provider);

        DialogFragment dialogFragment = (DialogFragment) fragmentManager.findFragmentByTag(DownloadFailedDialog.TAG);
        if (dialogFragment != null) {
            outState.putString(REASON_TO_FAIL, reasonToFail);
            dialogFragment.dismiss();
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentManager = new FragmentManagerEnhanced(getSupportFragmentManager());
        providerManager = ProviderManager.getInstance(getAssets(), getExternalFilesDir(null));

        setUpInitialUI();

        initProviderList();

        if (savedInstanceState != null)
            restoreState(savedInstanceState);
    }

    private void restoreState(Bundle savedInstanceState) {

        provider = savedInstanceState.getParcelable(Provider.KEY);
        mConfigState.setAction(savedInstanceState.getString(ACTIVITY_STATE, PROVIDER_NOT_SET));

        reasonToFail = savedInstanceState.getString(REASON_TO_FAIL);
        if(reasonToFail != null) {
            showDownloadFailedDialog();
        }

        if (SETTING_UP_PROVIDER.equals(mConfigState.getAction()) ||
                         PENDING_SHOW_FAILED_DIALOG.equals(mConfigState.getAction())
                ) {
            showProgressBar();
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "resuming with ConfigState: " + mConfigState.getAction());
        super.onResume();
        setUpProviderAPIResultReceiver();
        hideProgressBar();
        isActivityShowing = true;
        if (SETTING_UP_PROVIDER.equals(mConfigState.getAction())) {
            showProgressBar();
            checkProviderSetUp();
        } else if (PENDING_SHOW_FAILED_DIALOG.equals(mConfigState.getAction())) {
            showDownloadFailedDialog();
        } else if (SHOWING_PROVIDER_DETAILS.equals(mConfigState.getAction())) {
            cancelAndShowAllProviders();
        }
    }

    private void setUpInitialUI() {
        setContentView(R.layout.provider_list_activity);
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
        }
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

        if (provider.allowsAnonymous()) {
            mConfigState.putExtra(SERVICES_RETRIEVED, true);
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
        setResult(RESULT_CANCELED, mConfigState);
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
        if (SETTING_UP_PROVIDER.equals(mConfigState.getAction()) ||
                PENDING_SHOW_FAILED_DIALOG.equals(mConfigState.getAction())) {
            return;
        }

        //TODO Code 2 pane view
        provider = adapter.getItem(position);
        if (provider != null && !provider.isDefault()) {
            //TODO Code 2 pane view
            mConfigState.setAction(SETTING_UP_PROVIDER);
            showProgressBar();
            onItemSelectedLogic();
        } else {
            addAndSelectNewProvider();
        }
    }

    @Override
    public void onBackPressed() {
        if (SETTING_UP_PROVIDER.equals(mConfigState.getAction()) ||
                PENDING_SHOW_FAILED_DIALOG.equals(mConfigState.getAction())) {
            stopSettingUpProvider();
        } else {
            askDashboardToQuitApp();
            super.onBackPressed();
        }
    }

    private void stopSettingUpProvider() {
        cancelSettingUpProvider();
    }

    @Override
    public void cancelSettingUpProvider() {
        mConfigState.setAction(PROVIDER_NOT_SET);
        hideProgressBar();
    }

    @Override
    public void updateProviderDetails() {
        ProviderAPICommand.execute(this, UPDATE_PROVIDER_DETAILS, provider);
    }

    public void checkProviderSetUp() {
        ProviderAPICommand.execute(this, PROVIDER_SET_UP, provider, providerAPIResultReceiver);
    }

    private void askDashboardToQuitApp() {
        Intent askQuit = new Intent();
        askQuit.putExtra(APP_ACTION_QUIT, APP_ACTION_QUIT);
        setResult(RESULT_CANCELED, askQuit);
    }

    /**
     * Asks ProviderApiService to download an anonymous (anon) VPN certificate.
     */
    private void downloadVpnCertificate() {
        ProviderAPICommand.execute(this, DOWNLOAD_CERTIFICATE, provider);
    }

    /**
     * Open the new provider dialog
     */
    public void addAndSelectNewProvider() {
        addAndSelectNewProvider(null);
    }

    /**
     * Open the new provider dialog
     * @param mainUrl - the main url of the provider to add - if null add a new provider
     */
    public void addAndSelectNewProvider(@Nullable  String mainUrl) {
        FragmentTransaction fragmentTransaction = fragmentManager.removePreviousFragment(NewProviderDialog.TAG);

        DialogFragment newFragment = new NewProviderDialog();

        if (mainUrl != null) {
            Bundle data = new Bundle();
            data.putString(Provider.MAIN_URL, mainUrl);
            newFragment.setArguments(data);
        }
        newFragment.show(fragmentTransaction, NewProviderDialog.TAG);
    }

    /**
     * Shows an error dialog, if configuring of a provider failed.
     */
    public void showDownloadFailedDialog() {
        try {
            FragmentTransaction fragmentTransaction = fragmentManager.removePreviousFragment(DownloadFailedDialog.TAG);
            DialogFragment newFragment;
            try {
                JSONObject errorJson = new JSONObject(reasonToFail);
                newFragment = DownloadFailedDialog.newInstance(provider, errorJson);
            } catch (JSONException e) {
                e.printStackTrace();
                newFragment = DownloadFailedDialog.newInstance(provider, reasonToFail);
            }
            newFragment.show(fragmentTransaction, DownloadFailedDialog.TAG);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            mConfigState.setAction(PENDING_SHOW_FAILED_DIALOG);
            mConfigState.putExtra(REASON_TO_FAIL, reasonToFail);
        }

    }

    /**
     * Once selected a provider, this fragment offers the user to log in,
     * use it anonymously (if possible)
     * or cancel his/her election pressing the back button.
     *
     *
     */
    public void showProviderDetails() {
        // show only if current activity is shown
        if (isActivityShowing && mConfigState.getAction() != null &&
                !mConfigState.getAction().equalsIgnoreCase(SHOWING_PROVIDER_DETAILS)) {
            mConfigState.setAction(SHOWING_PROVIDER_DETAILS);
            Intent intent = new Intent(this, ProviderDetailActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.putExtra(PROVIDER_KEY, provider);
            startActivityForResult(intent, REQUEST_CODE_CONFIGURE_LEAP);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.configuration_wizard_activity, menu);
        return true;
    }

    public void cancelAndShowAllProviders() {
        mConfigState.setAction(PROVIDER_NOT_SET);
        provider = null;
    }

    public class ProviderAPIBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "received Broadcast");

            String action = intent.getAction();
            if (action == null || !action.equalsIgnoreCase(BROADCAST_PROVIDER_API_EVENT)) {
                return;
            }

            if (mConfigState.getAction() != null &&
                    mConfigState.getAction().equalsIgnoreCase(SETTING_UP_PROVIDER)) {
                int resultCode = intent.getIntExtra(BROADCAST_RESULT_CODE, -1);
                Log.d(TAG, "Broadcast resultCode: " + Integer.toString(resultCode));

                Bundle resultData = intent.getParcelableExtra(BROADCAST_RESULT_KEY);
                Provider handledProvider = resultData.getParcelable(PROVIDER_KEY);

                if (handledProvider != null && handledProvider.getDomain().equalsIgnoreCase(provider.getDomain())) {
                    switch (resultCode) {
                        case PROVIDER_OK:
                            handleProviderSetUp(handledProvider);
                            break;
                        case PROVIDER_NOK:
                            handleProviderSetupFailed(resultData);
                            break;
                        case CORRECTLY_DOWNLOADED_CERTIFICATE:
                            handleCorrectlyDownloadedCertificate(handledProvider);
                            break;
                        case INCORRECTLY_DOWNLOADED_CERTIFICATE:
                            handleIncorrectlyDownloadedCertificate();
                            break;
                    }
                }
            }
        }
    }
}
