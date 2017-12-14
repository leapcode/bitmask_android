/*
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

import butterknife.ButterKnife;
import butterknife.InjectView;
import se.leap.bitmaskclient.userstatus.SessionDialog;
import se.leap.bitmaskclient.userstatus.User;
import se.leap.bitmaskclient.userstatus.UserStatusFragment;

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
    public static final String ACTION_QUIT = "quit";
    public static final String ACTION_ASK_TO_CANCEL_VPN = "ask to cancel vpn";
    public static final String REQUEST_CODE = "request_code";
    public static final String PARAMETERS = "dashboard parameters";
    public static final String START_ON_BOOT = "dashboard start on boot";
    //FIXME: remove OR FIX ON_BOOT
    public static final String ON_BOOT = "dashboard on boot";

    private static Context app;
    protected static SharedPreferences preferences;
    private FragmentManagerEnhanced fragment_manager;

    @InjectView(R.id.providerName)
    TextView provider_name;

    VpnFragment eip_fragment;
    UserStatusFragment user_status_fragment;
    private static Provider provider = new Provider();
    public ProviderAPIResultReceiver providerAPI_result_receiver;
    private boolean switching_provider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(Constants.SHARED_PREFERENCES, MODE_PRIVATE);
        fragment_manager = new FragmentManagerEnhanced(getFragmentManager());

        providerAPI_result_receiver = new ProviderAPIResultReceiver(new Handler(), this);

        if (app == null) {
            app = this;
            handleVersion();
        }
        boolean provider_exists = previousProviderExists(savedInstanceState);
        if (provider_exists) {
            provider = getProvider(savedInstanceState);
            if(!provider.isConfigured())
                startActivityForResult(new Intent(this, ConfigurationWizard.class), CONFIGURE_LEAP);
            else {
                buildDashboard(getIntent().getBooleanExtra(ON_BOOT, false));
                user_status_fragment.restoreSessionStatus(savedInstanceState);
            }
        } else {
            startActivityForResult(new Intent(this, ConfigurationWizard.class), CONFIGURE_LEAP);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleVPNCancellation(getIntent());
    }

    private boolean previousProviderExists(Bundle savedInstanceState) {
        return providerInSavedInstance(savedInstanceState) || providerInSharedPreferences();
    }

    private Provider getProvider(Bundle savedInstanceState) {
        if(providerInSavedInstance(savedInstanceState))
            provider = savedInstanceState.getParcelable(Provider.KEY);
        else if (providerInSharedPreferences())
            provider = getSavedProviderFromSharedPreferences();
        return provider;
    }

    private boolean providerInSavedInstance(Bundle savedInstanceState) {
        return savedInstanceState != null &&
                savedInstanceState.containsKey(Provider.KEY);
    }

    private boolean providerInSharedPreferences() {
        return preferences != null &&
                preferences.getBoolean(Constants.PROVIDER_CONFIGURED, false);

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

            switch (versionCode) {
                case 91: // 0.6.0 without Bug #5999
                case 101: // 0.8.0
                    if (!preferences.getString(Constants.PROVIDER_KEY, "").isEmpty())
                        eip_fragment.updateEipService();
                    break;
            }
        } catch (NameNotFoundException e) {
            Log.d(TAG, "Handle version didn't find any " + getPackageName() + " package");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleVPNCancellation(intent);
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
        }
    }

    private void handleVPNCancellation(Intent intent) {
        if (intent.hasExtra(Dashboard.ACTION_ASK_TO_CANCEL_VPN)) {
            eip_fragment.askToStopEIP();
            intent.removeExtra(ACTION_ASK_TO_CANCEL_VPN);
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

        user_status_fragment = new UserStatusFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean(Provider.ALLOW_REGISTRATION, provider.allowsRegistration());
        user_status_fragment.setArguments(bundle);
        fragment_manager.replace(R.id.user_status_fragment, user_status_fragment, UserStatusFragment.TAG);

        if (provider.hasEIP()) {
            fragment_manager.removePreviousFragment(VpnFragment.TAG);
            eip_fragment = new VpnFragment();

            if (hide_and_turn_on_eip) {
                //TODO: remove line below if not in use anymore...
                preferences.edit().remove(Dashboard.START_ON_BOOT).apply();
                //FIXME: always start on Boot? Why do we keep shared preferences then?
                Bundle arguments = new Bundle();
                arguments.putBoolean(VpnFragment.START_ON_BOOT, true);
                if (eip_fragment != null) eip_fragment.setArguments(arguments);
            }

            fragment_manager.replace(R.id.servicesCollection, eip_fragment, VpnFragment.TAG);
            if (hide_and_turn_on_eip) {
                onBackPressed();
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (provider.allowsRegistration()) {
            menu.findItem(R.id.signup_button).setVisible(true);
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
                if (User.loggedIn()) user_status_fragment.logOut();
                else switchProvider();
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
        boolean allowed_anon = preferences.getBoolean(Constants.PROVIDER_ALLOW_ANONYMOUS, false);
        if (allowed_anon || is_authenticated)
            ProviderAPICommand.execute(Bundle.EMPTY, ProviderAPI.DOWNLOAD_CERTIFICATE, providerAPI_result_receiver);
        else
            sessionDialog(Bundle.EMPTY);
    }

    public void sessionDialog(Bundle resultData) {
        try {
            FragmentTransaction transaction = fragment_manager.removePreviousFragment(SessionDialog.TAG);
            SessionDialog.getInstance(provider, resultData).show(transaction, SessionDialog.TAG);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
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
            user_status_fragment.logIn(username, password);
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
