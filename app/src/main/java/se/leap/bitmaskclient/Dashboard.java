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

import android.annotation.*;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;

import org.jetbrains.annotations.*;
import org.json.*;

import java.net.*;

import butterknife.*;
import se.leap.bitmaskclient.eip.*;
import se.leap.bitmaskclient.userstatus.*;

/**
 * The main user facing Activity of Bitmask Android, consisting of status, controls,
 * and access to preferences.
 *
 * @author Sean Leonard <meanderingcode@aetherislands.net>
 * @author parmegv
 */
public class Dashboard extends Activity implements ProviderAPIResultReceiver.Receiver {

    protected static final int CONFIGURE_LEAP = 0;
    protected static final int SWITCH_PROVIDER = 1;

    public static final String TAG = Dashboard.class.getSimpleName();
    public static final String SHARED_PREFERENCES = "LEAPPreferences";
    public static final String ACTION_QUIT = "quit";
    public static final String REQUEST_CODE = "request_code";
    public static final String PARAMETERS = "dashboard parameters";
    public static final String START_ON_BOOT = "dashboard start on boot";
    public static final String ON_BOOT = "dashboard on boot";
    public static final String APP_VERSION = "bitmask version";

    private static Context app;
    protected static SharedPreferences preferences;
    private FragmentManagerEnhanced fragment_manager;

    @InjectView(R.id.providerName)
    TextView provider_name;

    EipFragment eip_fragment;
    UserSessionFragment user_session_fragment;
    private static Provider provider = new Provider();
    public ProviderAPIResultReceiver providerAPI_result_receiver;
    private boolean switching_provider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app = this;

        PRNGFixes.apply();

        preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        fragment_manager = new FragmentManagerEnhanced(getFragmentManager());
        handleVersion();
        User.init();

        ProviderAPICommand.initialize(this);
        providerAPI_result_receiver = new ProviderAPIResultReceiver(new Handler());
        providerAPI_result_receiver.setReceiver(this);

        restoreProvider(savedInstanceState);
        if (!provider.isConfigured())
            startActivityForResult(new Intent(this, ConfigurationWizard.class), CONFIGURE_LEAP);
        else {
            buildDashboard(getIntent().getBooleanExtra(ON_BOOT, false));
            user_session_fragment.restoreSessionStatus(savedInstanceState);
        }
    }

    private void restoreProvider(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(Provider.KEY))
                provider = savedInstanceState.getParcelable(Provider.KEY);
        }
        if (!provider.isConfigured() && preferences.getBoolean(Constants.PROVIDER_CONFIGURED, false))
            provider = getSavedProviderFromSharedPreferences();
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        outState.putParcelable(Provider.KEY, provider);
        super.onSaveInstanceState(outState);
    }

    private Provider getSavedProviderFromSharedPreferences() {
        Provider provider = new Provider();
        try {
            provider.setUrl(new URL(preferences.getString(Provider.MAIN_URL, "")));
            provider.define(new JSONObject(preferences.getString(Provider.KEY, "")));
        } catch (MalformedURLException | JSONException e) {
            e.printStackTrace();
        }

        return provider;
    }

    private void handleVersion() {
        try {
            int versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            int lastDetectedVersion = preferences.getInt(APP_VERSION, 0);
            preferences.edit().putInt(APP_VERSION, versionCode).apply();

            switch (versionCode) {
                case 91: // 0.6.0 without Bug #5999
                case 101: // 0.8.0
                    if (!preferences.getString(Constants.KEY, "").isEmpty())
                        eip_fragment.updateEipService();
                    break;
            }
        } catch (NameNotFoundException e) {
            Log.d(TAG, "Handle version didn't find any " + getPackageName() + " package");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CONFIGURE_LEAP || requestCode == SWITCH_PROVIDER) {
            if (resultCode == RESULT_OK && data.hasExtra(Provider.KEY)) {
                provider = data.getParcelableExtra(Provider.KEY);
                providerToPreferences(provider);

                buildDashboard(false);
                invalidateOptionsMenuOnUiThread();
                if (data.hasExtra(SessionDialog.TAG)) {
                    sessionDialog(Bundle.EMPTY);
                }

            } else if (resultCode == RESULT_CANCELED && data != null && data.hasExtra(ACTION_QUIT)) {
                finish();
            } else
                configErrorDialog();
        } else if (requestCode == EIP.DISCONNECT) {
            EipStatus.getInstance().setConnectedOrDisconnected();
        }
    }

    @SuppressLint("CommitPrefEdits")
    private void providerToPreferences(Provider provider) {
        preferences.edit().putBoolean(Constants.PROVIDER_CONFIGURED, true).commit();
        preferences.edit().putString(Provider.MAIN_URL, provider.mainUrl().toString()).apply();
        preferences.edit().putString(Provider.KEY, provider.definition().toString()).apply();
    }

    private void configErrorDialog() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getContext());
        alertBuilder.setTitle(getResources().getString(R.string.setup_error_title));
        alertBuilder
                .setMessage(getResources().getString(R.string.setup_error_text))
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.setup_error_configure_button), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent(getContext(), ConfigurationWizard.class), CONFIGURE_LEAP);
                    }
                })
                .setNegativeButton(getResources().getString(R.string.setup_error_close_button), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        preferences.edit().remove(Provider.KEY).remove(Constants.PROVIDER_CONFIGURED).apply();
                        finish();
                    }
                })
                .show();
    }

    /**
     * Inflates permanent UI elements of the View and contains logic for what
     * service dependent UI elements to include.
     */
    private void buildDashboard(boolean hide_and_turn_on_eip) {
        setContentView(R.layout.dashboard);
        ButterKnife.inject(this);

        provider_name.setText(provider.getDomain());
        user_session_fragment = new UserSessionFragment();

        if (provider.hasEIP()) {
            fragment_manager.removePreviousFragment(EipFragment.TAG);
            eip_fragment = new EipFragment();

            if (hide_and_turn_on_eip) {
                preferences.edit().remove(Dashboard.START_ON_BOOT).apply();
                Bundle arguments = new Bundle();
                arguments.putBoolean(EipFragment.START_ON_BOOT, true);
                if (eip_fragment != null) eip_fragment.setArguments(arguments);
            }

            fragment_manager.replace(R.id.servicesCollection, eip_fragment, EipFragment.TAG);
            if (hide_and_turn_on_eip) {
                onBackPressed();
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (provider.allowsRegistration()) {
            menu.findItem(R.id.signup_button).setVisible(true);

            boolean logged_in = User.loggedIn();
            menu.findItem(R.id.login_button).setVisible(!logged_in);
            menu.findItem(R.id.logout_button).setVisible(logged_in);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.client_dashboard, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about_leap:
                showAbout();
                return true;
            case R.id.log_window:
                showLog();
                return true;
            case R.id.switch_provider:
                switching_provider = true;
                if (User.loggedIn()) user_session_fragment.logOut();
                else switchProvider();
                return true;
            case R.id.login_button:
                sessionDialog(Bundle.EMPTY);
                return true;
            case R.id.logout_button:
                user_session_fragment.logOut();
                return true;
            case R.id.signup_button:
                sessionDialog(Bundle.EMPTY);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void showAbout() {
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }

    public void showLog() {
        LogWindowWrapper log_window_wrapper = LogWindowWrapper.getInstance(getContext());
        log_window_wrapper.showLog();
    }

    public void downloadVpnCertificate() {
        boolean is_authenticated = User.loggedIn();
        boolean allowed_anon = preferences.getBoolean(Constants.ALLOWED_ANON, false);
        if (allowed_anon || is_authenticated)
            ProviderAPICommand.execute(Bundle.EMPTY, ProviderAPI.DOWNLOAD_CERTIFICATE, providerAPI_result_receiver);
        else
            sessionDialog(Bundle.EMPTY);
    }

    public void sessionDialog(Bundle resultData) {
        FragmentTransaction transaction = fragment_manager.removePreviousFragment(SessionDialog.TAG);
        SessionDialog.getInstance(provider, resultData).show(transaction, SessionDialog.TAG);
    }

    private void switchProvider() {
        if (provider.hasEIP()) eip_fragment.stopEipIfPossible();

        preferences.edit().clear().apply();
        switching_provider = false;
        startActivityForResult(new Intent(this, ConfigurationWizard.class), SWITCH_PROVIDER);
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        if (resultCode == ProviderAPI.SUCCESSFUL_SIGNUP) {
            String username = resultData.getString(SessionDialog.USERNAME);
            String password = resultData.getString(SessionDialog.PASSWORD);
            user_session_fragment.logIn(username, password);
        } else if (resultCode == ProviderAPI.FAILED_SIGNUP) {
            sessionDialog(resultData);
        } else if (resultCode == ProviderAPI.SUCCESSFUL_LOGIN) {
            downloadVpnCertificate();
        } else if (resultCode == ProviderAPI.FAILED_LOGIN) {
            sessionDialog(resultData);
        } else if (resultCode == ProviderAPI.SUCCESSFUL_LOGOUT) {
            if (switching_provider) switchProvider();
        } else if (resultCode == ProviderAPI.LOGOUT_FAILED) {
            setResult(RESULT_CANCELED);
        } else if (resultCode == ProviderAPI.CORRECTLY_DOWNLOADED_CERTIFICATE) {
            eip_fragment.updateEipService();
            setResult(RESULT_OK);
        } else if (resultCode == ProviderAPI.INCORRECTLY_DOWNLOADED_CERTIFICATE) {
            setResult(RESULT_CANCELED);
        } else if (resultCode == ProviderAPI.CORRECTLY_DOWNLOADED_EIP_SERVICE) {
            eip_fragment.updateEipService();
            setResult(RESULT_OK);
        } else if (resultCode == ProviderAPI.INCORRECTLY_DOWNLOADED_EIP_SERVICE) {
            setResult(RESULT_CANCELED);
        }
    }

    public static Context getContext() {
        return app;
    }

    public static Provider getProvider() { return provider; }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        intent.putExtra(Dashboard.REQUEST_CODE, requestCode);
        super.startActivityForResult(intent, requestCode);
    }

    public void invalidateOptionsMenuOnUiThread() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                invalidateOptionsMenu();
            }
        });
    }
}
