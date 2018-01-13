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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pedrogomez.renderers.Renderer;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.InjectView;
import butterknife.OnItemClick;
import se.leap.bitmaskclient.fragments.AboutFragment;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static se.leap.bitmaskclient.Constants.APP_ACTION_QUIT;
import static se.leap.bitmaskclient.Constants.PROVIDER_ALLOW_ANONYMOUS;
import static se.leap.bitmaskclient.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.Constants.SHARED_PREFERENCES;
import static se.leap.bitmaskclient.ProviderAPI.ERRORS;

/**
 * abstract base Activity that builds and shows the list of known available providers.
 * The implementation of BaseConfigurationWizard differ in that they may or may not allow to bypass
 * secure download mechanisms including certificate validation.
 * <p/>
 * It also allows the user to enter custom providers with a button.
 *
 * @author parmegv
 * @author cyberta
 */

public abstract class BaseConfigurationWizard extends ButterKnifeActivity
        implements NewProviderDialog.NewProviderDialogInterface, DownloadFailedDialog.DownloadFailedDialogInterface, ProviderAPIResultReceiver.Receiver {
    @InjectView(R.id.progressbar_configuration_wizard)
    protected ProgressBar mProgressBar;
    @InjectView(R.id.progressbar_description)
    protected TextView progressbarDescription;

    @InjectView(R.id.provider_list)
    protected ListView providerListView;
    @Inject
    protected ProviderListAdapter adapter;

    private ProviderManager providerManager;
    protected Intent mConfigState = new Intent(PROVIDER_NOT_SET);
    protected Provider selectedProvider;

    final public static String TAG = ConfigurationWizard.class.getSimpleName();

    final protected static String PROVIDER_NOT_SET = "PROVIDER NOT SET";
    final protected static String SETTING_UP_PROVIDER = "PROVIDER GETS SET";
    final private static  String PENDING_SHOW_PROVIDER_DETAILS = "PROVIDER DETAILS SHOWN";
    final private static String PENDING_SHOW_FAILED_DIALOG = "SHOW FAILED DIALOG";
    final private static String REASON_TO_FAIL = "REASON TO FAIL";
    final protected static String PROVIDER_SET = "PROVIDER SET";
    final protected static String SERVICES_RETRIEVED = "SERVICES RETRIEVED";

    final private static String PROGRESSBAR_TEXT = TAG + "PROGRESSBAR_TEXT";
    final private static String PROGRESSBAR_NUMBER = TAG + "PROGRESSBAR_NUMBER";

    public ProviderAPIResultReceiver providerAPIResultReceiver;
    private BaseConfigurationWizard.providerAPIBroadcastReceiverUpdate providerAPIBroadcastReceiverUpdate;

    protected static SharedPreferences preferences;
    FragmentManagerEnhanced fragmentManager;
    //TODO: add some states (values for progressbarText) about ongoing setup or remove that field
    private String progressbarText = "";



    public abstract void retrySetUpProvider();

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
        if (mProgressBar != null)
            outState.putInt(PROGRESSBAR_NUMBER, mProgressBar.getProgress());
        if (progressbarDescription != null)
            outState.putString(PROGRESSBAR_TEXT, progressbarDescription.getText().toString());
        outState.putParcelable(Provider.KEY, selectedProvider);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        fragmentManager = new FragmentManagerEnhanced(getSupportFragmentManager());
        providerManager = ProviderManager.getInstance(getAssets(), getExternalFilesDir(null));

        setUpInitialUI();

        initProviderList();

        if (savedInstanceState != null)
            restoreState(savedInstanceState);
        setUpProviderAPIResultReceiver();
    }

    private void restoreState(Bundle savedInstanceState) {
        progressbarText = savedInstanceState.getString(PROGRESSBAR_TEXT, "");
        selectedProvider = savedInstanceState.getParcelable(Provider.KEY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (SETTING_UP_PROVIDER.equals(mConfigState.getAction())) {
            cancelAndShowAllProviders();
        } else if (PENDING_SHOW_PROVIDER_DETAILS.equals(mConfigState.getAction())) {
            showProviderDetails();
        } else if (PENDING_SHOW_FAILED_DIALOG.equals(mConfigState.getAction())) {
            showDownloadFailedDialog(mConfigState.getStringExtra(REASON_TO_FAIL));
        }
    }

    private void setUpInitialUI() {
        setContentView(R.layout.configuration_wizard_activity);

        hideProgressBar();
    }

    private void hideProgressBar() {
        //needs to be "INVISIBLE" instead of GONE b/c the progressbarDescription gets translated
        // by the height of mProgressbar (and the height of the first list item)
        mProgressBar.setVisibility(INVISIBLE);
        progressbarDescription.setVisibility(INVISIBLE);
        mProgressBar.setProgress(0);
    }

    protected void showProgressBar() {
        mProgressBar.setVisibility(VISIBLE);
        progressbarDescription.setVisibility(VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (providerAPIBroadcastReceiverUpdate != null)
            unregisterReceiver(providerAPIBroadcastReceiverUpdate);
    }

    private void setUpProviderAPIResultReceiver() {
        providerAPIResultReceiver = new ProviderAPIResultReceiver(new Handler(), this);
        providerAPIBroadcastReceiverUpdate = new providerAPIBroadcastReceiverUpdate();

        IntentFilter update_intent_filter = new IntentFilter(ProviderAPI.UPDATE_PROGRESSBAR);
        update_intent_filter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(providerAPIBroadcastReceiverUpdate, update_intent_filter);
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        if (resultCode == ProviderAPI.PROVIDER_OK) {
            try {
                String providerJsonString = preferences.getString(Provider.KEY, "");
                if (!providerJsonString.isEmpty())
                    selectedProvider.define(new JSONObject(providerJsonString));
                String caCert = preferences.getString(Provider.CA_CERT, "");
                selectedProvider.setCACert(caCert);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (preferences.getBoolean(PROVIDER_ALLOW_ANONYMOUS, false)) {
                mConfigState.putExtra(SERVICES_RETRIEVED, true);

                downloadVpnCertificate();
            } else {
                mProgressBar.incrementProgressBy(1);
                hideProgressBar();

                showProviderDetails();
            }
        } else if (resultCode == ProviderAPI.PROVIDER_NOK) {
            mConfigState.setAction(PROVIDER_NOT_SET);
            preferences.edit().remove(Provider.KEY).apply();

            setResult(RESULT_CANCELED, mConfigState);

            String reason_to_fail = resultData.getString(ERRORS);
            showDownloadFailedDialog(reason_to_fail);
        } else if (resultCode == ProviderAPI.CORRECTLY_DOWNLOADED_CERTIFICATE) {
            mProgressBar.incrementProgressBy(1);
            hideProgressBar();
            showProviderDetails();
        } else if (resultCode == ProviderAPI.INCORRECTLY_DOWNLOADED_CERTIFICATE) {
            mConfigState.setAction(PROVIDER_NOT_SET);
            hideProgressBar();
            setResult(RESULT_CANCELED, mConfigState);
        } else if (resultCode == AboutFragment.VIEWED) {
            // Do nothing, right now
            // I need this for CW to wait for the About activity to end before going back to Dashboard.
        }
    }

    @OnItemClick(R.id.provider_list)
    void onItemSelected(int position) {
        if (SETTING_UP_PROVIDER.equals(mConfigState.getAction()) ||
                PENDING_SHOW_PROVIDER_DETAILS.equals(mConfigState.getAction()) ||
                PENDING_SHOW_FAILED_DIALOG.equals(mConfigState.getAction())) {
            return;
        }

        //TODO Code 2 pane view
        mConfigState.setAction(SETTING_UP_PROVIDER);
        selectedProvider = adapter.getItem(position);
        onItemSelectedUi();
        onItemSelectedLogic();
    }

    protected void onItemSelectedUi() {
        adapter.hideAllBut(adapter.indexOf(selectedProvider));
        startProgressBar();
    }

    @Override
    public void onBackPressed() {
        if (SETTING_UP_PROVIDER.equals(mConfigState.getAction()) ||
            PENDING_SHOW_PROVIDER_DETAILS.equals(mConfigState.getAction()) ||
                PENDING_SHOW_FAILED_DIALOG.equals(mConfigState.getAction())) {
            stopSettingUpProvider();
        } else {
            askDashboardToQuitApp();
            super.onBackPressed();
        }
    }

    private void stopSettingUpProvider() {
        ProviderAPI.stop();
        mProgressBar.setVisibility(GONE);
        mProgressBar.setProgress(0);
        progressbarDescription.setVisibility(GONE);

        cancelSettingUpProvider();
    }

    @Override
    public void cancelSettingUpProvider() {
        hideProgressBar();
        mConfigState.setAction(PROVIDER_NOT_SET);
        adapter.showAllProviders();
        preferences.edit().remove(Provider.KEY).remove(PROVIDER_ALLOW_ANONYMOUS).remove(PROVIDER_KEY).apply();
    }

    @Override
    public void updateProviderDetails() {
        mConfigState.setAction(SETTING_UP_PROVIDER);
        Intent providerAPICommand = new Intent(this, ProviderAPI.class);

        providerAPICommand.setAction(ProviderAPI.UPDATE_PROVIDER_DETAILS);
        providerAPICommand.putExtra(ProviderAPI.RECEIVER_KEY, providerAPIResultReceiver);
        Bundle parameters = new Bundle();
        parameters.putString(Provider.MAIN_URL, selectedProvider.getMainUrl().toString());
        providerAPICommand.putExtra(ProviderAPI.PARAMETERS, parameters);

        startService(providerAPICommand);
    }

    private void askDashboardToQuitApp() {
        Intent askQuit = new Intent();
        askQuit.putExtra(APP_ACTION_QUIT, APP_ACTION_QUIT);
        setResult(RESULT_CANCELED, askQuit);
    }

    private void startProgressBar() {
        showProgressBar();
        mProgressBar.setProgress(0);
        mProgressBar.setMax(3);

        int measured_height = listItemHeight();
        mProgressBar.setTranslationY(measured_height);
        progressbarDescription.setTranslationY(measured_height + mProgressBar.getHeight());
    }

    private int listItemHeight() {
        View listItem = adapter.getView(0, null, providerListView);
        listItem.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT));
        WindowManager wm = (WindowManager) getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        int screenWidth = display.getWidth(); // deprecated

        int listViewWidth = screenWidth - 10 - 10;
        int widthSpec = View.MeasureSpec.makeMeasureSpec(listViewWidth,
                View.MeasureSpec.AT_MOST);
        listItem.measure(widthSpec, 0);

        return listItem.getMeasuredHeight();
    }

    /**
     * Asks ProviderApiService to download an anonymous (anon) VPN certificate.
     */
    private void downloadVpnCertificate() {
        Intent providerAPICommand = new Intent(this, ProviderAPI.class);

        providerAPICommand.setAction(ProviderAPI.DOWNLOAD_CERTIFICATE);
        providerAPICommand.putExtra(ProviderAPI.RECEIVER_KEY, providerAPIResultReceiver);

        startService(providerAPICommand);
    }

    /**
     * Open the new provider dialog
     */
    public void addAndSelectNewProvider() {
        FragmentTransaction fragmentTransaction = fragmentManager.removePreviousFragment(NewProviderDialog.TAG);
        new NewProviderDialog().show(fragmentTransaction, NewProviderDialog.TAG);
    }

    /**
     * Open the new provider dialog with data
     */
    public void addAndSelectNewProvider(String main_url) {
        FragmentTransaction fragmentTransaction = fragmentManager.removePreviousFragment(NewProviderDialog.TAG);

        DialogFragment newFragment = new NewProviderDialog();
        Bundle data = new Bundle();
        data.putString(Provider.MAIN_URL, main_url);
        newFragment.setArguments(data);
        newFragment.show(fragmentTransaction, NewProviderDialog.TAG);
    }

    /**
     * Shows an error dialog, if configuring of a provider failed.
     *
     * @param reasonToFail
     */
    public void showDownloadFailedDialog(String reasonToFail) {
        try {
            FragmentTransaction fragmentTransaction = fragmentManager.removePreviousFragment(DownloadFailedDialog.TAG);
            DialogFragment newFragment;
            try {
                JSONObject errorJson = new JSONObject(reasonToFail);
                newFragment = DownloadFailedDialog.newInstance(errorJson);
            } catch (JSONException e) {
                e.printStackTrace();
                newFragment = DownloadFailedDialog.newInstance(reasonToFail);
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
        Intent intent = new Intent(this, ProviderDetailActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.configuration_wizard_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about_leap:
                startActivityForResult(new Intent(this, AboutFragment.class), 0);
                return true;
            case R.id.new_provider:
                addAndSelectNewProvider();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void cancelAndShowAllProviders() {
        mConfigState.setAction(PROVIDER_NOT_SET);
        selectedProvider = null;
        adapter.showAllProviders();
    }

    public class providerAPIBroadcastReceiverUpdate extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int update = intent.getIntExtra(ProviderAPI.CURRENT_PROGRESS, 0);
            mProgressBar.setProgress(update);
        }
    }
}
