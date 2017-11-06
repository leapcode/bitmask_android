/**
 * Copyright (c) 2013 LEAP Encryption Access Project and contributors
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

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import com.pedrogomez.renderers.*;

import java.net.*;
import java.util.*;

import butterknife.*;

import org.jetbrains.annotations.*;
import org.json.*;

import javax.inject.*;

import se.leap.bitmaskclient.DownloadFailedDialog.DownloadFailedDialogInterface;
import se.leap.bitmaskclient.NewProviderDialog.NewProviderDialogInterface;
import se.leap.bitmaskclient.ProviderAPIResultReceiver.Receiver;
import se.leap.bitmaskclient.ProviderDetailFragment.ProviderDetailFragmentInterface;
import se.leap.bitmaskclient.ProviderListContent.ProviderItem;
import se.leap.bitmaskclient.eip.*;
import se.leap.bitmaskclient.userstatus.SessionDialog;

import static android.view.View.*;

/**
 * Activity that builds and shows the list of known available providers.
 * <p/>
 * It also allows the user to enter custom providers with a button.
 *
 * @author parmegv
 * @author cyberta
 */
public class ConfigurationWizard extends Activity
        implements NewProviderDialogInterface, ProviderDetailFragmentInterface, DownloadFailedDialogInterface, Receiver {

    @InjectView(R.id.progressbar_configuration_wizard)
    ProgressBar mProgressBar;
    @InjectView(R.id.progressbar_description)
    TextView progressbar_description;

    @InjectView(R.id.provider_list)
    ListView provider_list_view;
    @Inject
    ProviderListAdapter adapter;

    private ProviderManager provider_manager;
    private Intent mConfigState = new Intent(PROVIDER_NOT_SET);
    private Provider selected_provider;

    final public static String TAG = ConfigurationWizard.class.getSimpleName();

    final protected static String PROVIDER_NOT_SET = "PROVIDER NOT SET";
    final protected static String SETTING_UP_PROVIDER = "PROVIDER GETS SET";
    private static final String SETTING_UP_PROVIDER_SHOW_PROVIDER_DETILS = "PROVIDER DETAILS SHOWN";
    final protected static String PROVIDER_SET = "PROVIDER SET";
    final protected static String SERVICES_RETRIEVED = "SERVICES RETRIEVED";

    final private static String PROGRESSBAR_TEXT = TAG + "PROGRESSBAR_TEXT";
    final private static String PROGRESSBAR_NUMBER = TAG + "PROGRESSBAR_NUMBER";

    public ProviderAPIResultReceiver providerAPI_result_receiver;
    private ProviderAPIBroadcastReceiver_Update providerAPI_broadcast_receiver_update;

    private static SharedPreferences preferences;
    FragmentManagerEnhanced fragment_manager;
    //TODO: add some states (values for progressbar_text) about ongoing setup or remove that field
    private String progressbar_text = "";

    private void initProviderList() {
        List<Renderer<Provider>> prototypes = new ArrayList<>();
        prototypes.add(new ProviderRenderer(this));
        ProviderRendererBuilder providerRendererBuilder = new ProviderRendererBuilder(prototypes);
        adapter = new ProviderListAdapter(getLayoutInflater(), providerRendererBuilder, provider_manager);
        provider_list_view.setAdapter(adapter);
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        if (mProgressBar != null)
            outState.putInt(PROGRESSBAR_NUMBER, mProgressBar.getProgress());
        if (progressbar_description != null)
            outState.putString(PROGRESSBAR_TEXT, progressbar_description.getText().toString());
        outState.putParcelable(Provider.KEY, selected_provider);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(Dashboard.SHARED_PREFERENCES, MODE_PRIVATE);
        fragment_manager = new FragmentManagerEnhanced(getFragmentManager());
        provider_manager = ProviderManager.getInstance(getAssets(), getExternalFilesDir(null));

        setUpInitialUI();

        initProviderList();

        if (savedInstanceState != null)
            restoreState(savedInstanceState);
        setUpProviderAPIResultReceiver();
    }

    private void restoreState(Bundle savedInstanceState) {
        progressbar_text = savedInstanceState.getString(PROGRESSBAR_TEXT, "");
        selected_provider = savedInstanceState.getParcelable(Provider.KEY);

        if (fragment_manager.findFragmentByTag(ProviderDetailFragment.TAG) == null && SETTING_UP_PROVIDER.equals(mConfigState.getAction())) {
            onItemSelectedUi();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (SETTING_UP_PROVIDER.equals(mConfigState.getAction())) {
            showProgressBar();
            adapter.hideAllBut(adapter.indexOf(selected_provider));
        } else if (SETTING_UP_PROVIDER_SHOW_PROVIDER_DETILS.equals(mConfigState.getAction())) {
            showProviderDetails();
        }
    }

    private void setUpInitialUI() {
        setContentView(R.layout.configuration_wizard_activity);
        ButterKnife.inject(this);

        hideProgressBar();
    }

    private void hideProgressBar() {
        //needs to be "INVISIBLE" instead of GONE b/c the progressbar_description gets translated
        // by the height of mProgressbar (and the height of the first list item)
        mProgressBar.setVisibility(INVISIBLE);
        progressbar_description.setVisibility(INVISIBLE);
    }

    private void showProgressBar() {
        mProgressBar.setVisibility(VISIBLE);
        progressbar_description.setVisibility(VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (providerAPI_broadcast_receiver_update != null)
            unregisterReceiver(providerAPI_broadcast_receiver_update);
    }

    private void setUpProviderAPIResultReceiver() {
        providerAPI_result_receiver = new ProviderAPIResultReceiver(new Handler(), this);
        providerAPI_broadcast_receiver_update = new ProviderAPIBroadcastReceiver_Update();

        IntentFilter update_intent_filter = new IntentFilter(ProviderAPI.UPDATE_PROGRESSBAR);
        update_intent_filter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(providerAPI_broadcast_receiver_update, update_intent_filter);
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        if (resultCode == ProviderAPI.PROVIDER_OK) {
            try {
                String provider_json_string = preferences.getString(Provider.KEY, "");
                if (!provider_json_string.isEmpty())
                    selected_provider.define(new JSONObject(provider_json_string));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (preferences.getBoolean(Constants.ALLOWED_ANON, false)) {
                mConfigState.putExtra(SERVICES_RETRIEVED, true);

                downloadVpnCertificate();
            } else {
                mProgressBar.incrementProgressBy(1);
                hideProgressBar();

                showProviderDetails();
            }
        } else if (resultCode == ProviderAPI.PROVIDER_NOK) {
            mConfigState.setAction(PROVIDER_NOT_SET);
            hideProgressBar();
            preferences.edit().remove(Provider.KEY).apply();

            String reason_to_fail = resultData.getString(ProviderAPI.ERRORS);
            showDownloadFailedDialog(reason_to_fail);
        } else if (resultCode == ProviderAPI.CORRECTLY_DOWNLOADED_CERTIFICATE) {
            mProgressBar.incrementProgressBy(1);
            hideProgressBar();
            showProviderDetails();
        } else if (resultCode == ProviderAPI.INCORRECTLY_DOWNLOADED_CERTIFICATE) {
            mConfigState.setAction(PROVIDER_NOT_SET);
            hideProgressBar();
            setResult(RESULT_CANCELED, mConfigState);
            Toast.makeText(getApplicationContext(), R.string.provider_problem, Toast.LENGTH_LONG).show();
        } else if (resultCode == AboutActivity.VIEWED) {
            // Do nothing, right now
            // I need this for CW to wait for the About activity to end before going back to Dashboard.
        }
    }

    @OnItemClick(R.id.provider_list)
    void onItemSelected(int position) {
        if (SETTING_UP_PROVIDER.equals(mConfigState.getAction()) ||
                SETTING_UP_PROVIDER_SHOW_PROVIDER_DETILS.equals(mConfigState.getAction())) {
            return;
        }

        //TODO Code 2 pane view
        mConfigState.setAction(SETTING_UP_PROVIDER);
        selected_provider = adapter.getItem(position);
        onItemSelectedLogic();
        onItemSelectedUi();
    }

    private void onItemSelectedLogic() {
        boolean danger_on = preferences.getBoolean(ProviderItem.DANGER_ON, true);
        setUpProvider(danger_on);
    }

    private void onItemSelectedUi() {
        adapter.hideAllBut(adapter.indexOf(selected_provider));
        startProgressBar();
    }

    @Override
    public void onBackPressed() {
        if (SETTING_UP_PROVIDER.equals(mConfigState.getAction())) {
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
        progressbar_description.setVisibility(GONE);

        cancelSettingUpProvider();
    }

    public void cancelSettingUpProvider() {
        mConfigState.setAction(PROVIDER_NOT_SET);
        adapter.showAllProviders();
        preferences.edit().remove(Provider.KEY).remove(ProviderItem.DANGER_ON).remove(Constants.ALLOWED_ANON).remove(Constants.KEY).commit();
    }

    private void askDashboardToQuitApp() {
        Intent ask_quit = new Intent();
        ask_quit.putExtra(Dashboard.ACTION_QUIT, Dashboard.ACTION_QUIT);
        setResult(RESULT_CANCELED, ask_quit);
    }

    private void startProgressBar() {
        showProgressBar();
        mProgressBar.setProgress(0);
        mProgressBar.setMax(3);

        int measured_height = listItemHeight();
        mProgressBar.setTranslationY(measured_height);
        progressbar_description.setTranslationY(measured_height + mProgressBar.getHeight());
    }

    private int listItemHeight() {
        View listItem = adapter.getView(0, null, provider_list_view);
        listItem.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT));
        WindowManager wm = (WindowManager) getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        int screenWidth = display.getWidth(); // deprecated

        int listViewWidth = screenWidth - 10 - 10;
        int widthSpec = MeasureSpec.makeMeasureSpec(listViewWidth,
                MeasureSpec.AT_MOST);
        listItem.measure(widthSpec, 0);

        return listItem.getMeasuredHeight();
    }

    /**
     * Asks ProviderAPI to download an anonymous (anon) VPN certificate.
     */
    private void downloadVpnCertificate() {
        Intent provider_API_command = new Intent(this, ProviderAPI.class);

        provider_API_command.setAction(ProviderAPI.DOWNLOAD_CERTIFICATE);
        provider_API_command.putExtra(ProviderAPI.RECEIVER_KEY, providerAPI_result_receiver);
        startService(provider_API_command);
    }

    /**
     * Open the new provider dialog
     */
    public void addAndSelectNewProvider() {
        FragmentTransaction fragment_transaction = fragment_manager.removePreviousFragment(NewProviderDialog.TAG);
        new NewProviderDialog().show(fragment_transaction, NewProviderDialog.TAG);
    }

    /**
     * Open the new provider dialog with data
     */
    public void addAndSelectNewProvider(String main_url, boolean danger_on) {
        FragmentTransaction fragment_transaction = fragment_manager.removePreviousFragment(NewProviderDialog.TAG);

        DialogFragment newFragment = new NewProviderDialog();
        Bundle data = new Bundle();
        data.putString(Provider.MAIN_URL, main_url);
        data.putBoolean(ProviderItem.DANGER_ON, danger_on);
        newFragment.setArguments(data);
        newFragment.show(fragment_transaction, NewProviderDialog.TAG);
    }

    /**
     * Once selected a provider, this fragment offers the user to log in,
     * use it anonymously (if possible)
     * or cancel his/her election pressing the back button.
     *
     * @param reason_to_fail
     */
    public void showDownloadFailedDialog(String reason_to_fail) {
        FragmentTransaction fragment_transaction = fragment_manager.removePreviousFragment(DownloadFailedDialog.TAG);

        DialogFragment newFragment = DownloadFailedDialog.newInstance(reason_to_fail);
        newFragment.show(fragment_transaction, DownloadFailedDialog.TAG);
    }

    /**
     * Once selected a provider, this fragment offers the user to log in,
     * use it anonymously (if possible)
     * or cancel his/her election pressing the back button.
     */
    private void showProviderDetails() {
        mConfigState.setAction(SETTING_UP_PROVIDER_SHOW_PROVIDER_DETILS);
        try {
            FragmentTransaction fragment_transaction = fragment_manager.removePreviousFragment(ProviderDetailFragment.TAG);

            DialogFragment newFragment = ProviderDetailFragment.newInstance();
            newFragment.show(fragment_transaction, ProviderDetailFragment.TAG);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void showAndSelectProvider(String provider_main_url, boolean danger_on) {
        try {
            selected_provider = new Provider(new URL((provider_main_url)));
            adapter.add(selected_provider);
            adapter.saveProviders();
            autoSelectProvider(selected_provider, danger_on);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private void autoSelectProvider(Provider provider, boolean danger_on) {
        preferences.edit().putBoolean(ProviderItem.DANGER_ON, danger_on).apply();
        selected_provider = provider;
        onItemSelectedLogic();
        onItemSelectedUi();
    }

    /**
     * Asks ProviderAPI to download a new provider.json file
     *
     * @param danger_on         tells if HTTPS client should bypass certificate errors
     */
    public void setUpProvider(boolean danger_on) {
        mConfigState.setAction(SETTING_UP_PROVIDER);
        Intent provider_API_command = new Intent(this, ProviderAPI.class);
        Bundle parameters = new Bundle();
        parameters.putString(Provider.MAIN_URL, selected_provider.mainUrl().getUrl().toString());
        parameters.putBoolean(ProviderItem.DANGER_ON, danger_on);
        parameters.putString(Provider.CA_CERT_FINGERPRINT, selected_provider.certificatePin());

        provider_API_command.setAction(ProviderAPI.SET_UP_PROVIDER);
        provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);
        provider_API_command.putExtra(ProviderAPI.RECEIVER_KEY, providerAPI_result_receiver);

        startService(provider_API_command);
    }

    public void retrySetUpProvider() {
        cancelSettingUpProvider();
        if (!ProviderAPI.caCertDownloaded()) {
            addAndSelectNewProvider(ProviderAPI.lastProviderMainUrl(), ProviderAPI.lastDangerOn());
        } else {
            Intent provider_API_command = new Intent(this, ProviderAPI.class);

            provider_API_command.setAction(ProviderAPI.SET_UP_PROVIDER);
            provider_API_command.putExtra(ProviderAPI.RECEIVER_KEY, providerAPI_result_receiver);

            startService(provider_API_command);
        }
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
                startActivityForResult(new Intent(this, AboutActivity.class), 0);
                return true;
            case R.id.new_provider:
                addAndSelectNewProvider();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void cancelAndShowAllProviders() {
        mConfigState.setAction(PROVIDER_NOT_SET);
        selected_provider = null;
        adapter.showAllProviders();
    }

    @Override
    public void login() {
        mConfigState.setAction(PROVIDER_SET);
        Intent ask_login = new Intent();
        ask_login.putExtra(SessionDialog.TAG, SessionDialog.TAG);
        ask_login.putExtra(Provider.KEY, selected_provider);
        setResult(RESULT_OK, ask_login);
        finish();
    }

    @Override
    public void use_anonymously() {
        mConfigState.setAction(PROVIDER_SET);
        Intent pass_provider = new Intent();
        pass_provider.putExtra(Provider.KEY, selected_provider);
        setResult(RESULT_OK, pass_provider);
        finish();
    }

    public class ProviderAPIBroadcastReceiver_Update extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int update = intent.getIntExtra(ProviderAPI.CURRENT_PROGRESS, 0);
            mProgressBar.setProgress(update);
        }
    }
}
