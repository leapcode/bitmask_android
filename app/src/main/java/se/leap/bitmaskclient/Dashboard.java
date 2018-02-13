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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

import butterknife.InjectView;
import de.blinkt.openvpn.core.VpnStatus;
import se.leap.bitmaskclient.eip.EipCommand;
import se.leap.bitmaskclient.fragments.AboutFragment;
import se.leap.bitmaskclient.userstatus.User;
import se.leap.bitmaskclient.userstatus.UserStatusFragment;

import static se.leap.bitmaskclient.Constants.APP_ACTION_CONFIGURE_ALWAYS_ON_PROFILE;
import static se.leap.bitmaskclient.Constants.APP_ACTION_QUIT;
import static se.leap.bitmaskclient.Constants.EIP_IS_ALWAYS_ON;
import static se.leap.bitmaskclient.Constants.EIP_RESTART_ON_BOOT;
import static se.leap.bitmaskclient.Constants.PROVIDER_CONFIGURED;
import static se.leap.bitmaskclient.Constants.PROVIDER_EIP_DEFINITION;
import static se.leap.bitmaskclient.Constants.REQUEST_CODE_CONFIGURE_LEAP;
import static se.leap.bitmaskclient.Constants.REQUEST_CODE_KEY;
import static se.leap.bitmaskclient.Constants.REQUEST_CODE_SWITCH_PROVIDER;
import static se.leap.bitmaskclient.Constants.SHARED_PREFERENCES;

/**
 * The main user facing Activity of Bitmask Android, consisting of status, controls,
 * and access to preferences.
 *
 * @author Sean Leonard <meanderingcode@aetherislands.net>
 * @author parmegv
 */
public class Dashboard extends ButterKnifeActivity {

    public static final String TAG = Dashboard.class.getSimpleName();

    /**
     * When "Disconnect" is clicked from the notification this extra gets added to the calling intent.
     */
    public static final String ACTION_ASK_TO_CANCEL_VPN = "ask to cancel vpn";
    /**
     * if always-on feature is enabled, but there's no provider configured the EIP Service
     * adds this intent extra. Constants.APP_ACTION_CONFIGURE_ALWAYS_ON_PROFILE
     * serves to start the Configuration Wizard on top of the Dashboard Activity.
     */

    protected static SharedPreferences preferences;
    private static FragmentManagerEnhanced fragment_manager;

    @InjectView(R.id.providerName)
    TextView provider_name;

    private EipFragment eip_fragment;
    private UserStatusFragment user_status_fragment;

    private static Provider provider = new Provider();
    private boolean handledVersion;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        fragment_manager = new FragmentManagerEnhanced(getSupportFragmentManager());


        if (!handledVersion) {
            handleVersion();
            handledVersion = true;
        }

        // initialize app necessities
        VpnStatus.initLogCache(getApplicationContext().getCacheDir());
        User.init(getString(R.string.default_username));

        prepareEIP(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleVpnCancellation(getIntent());
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
                preferences.getBoolean(PROVIDER_CONFIGURED, false);

    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        outState.putParcelable(Provider.KEY, provider);
        super.onSaveInstanceState(outState);
    }

    private Provider getSavedProviderFromSharedPreferences() {
        Provider provider = new Provider();
        try {
            provider.setMainUrl(new URL(preferences.getString(Provider.MAIN_URL, "")));
            provider.define(new JSONObject(preferences.getString(Provider.KEY, "")));
            provider.setCaCert(preferences.getString(Provider.CA_CERT, ""));
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
                    if (!preferences.getString(PROVIDER_EIP_DEFINITION, "").isEmpty())
                        EipCommand.updateEipService(this);
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
        handleIntentExtras(intent);
    }

    private void handleIntentExtras(Intent intent) {
        if (intent.hasExtra(ACTION_ASK_TO_CANCEL_VPN)) {
            handleVpnCancellation(intent);
        } else if (intent.hasExtra(EIP_RESTART_ON_BOOT)) {
            Log.d(TAG, "Dashboard: EIP_RESTART_ON_BOOT");
            prepareEIP(null);
        } else if (intent.hasExtra(APP_ACTION_CONFIGURE_ALWAYS_ON_PROFILE)) {
            Log.d(TAG, "Dashboard: Constants.APP_ACTION_CONFIGURE_ALWAYS_ON_PROFILE");
            handleConfigureAlwaysOn(getIntent());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CONFIGURE_LEAP || requestCode == REQUEST_CODE_SWITCH_PROVIDER) {
            if (resultCode == RESULT_OK && data.hasExtra(Provider.KEY)) {
                provider = data.getParcelableExtra(Provider.KEY);
                providerToPreferences(provider);

                buildDashboard(false);
                invalidateOptionsMenuOnUiThread();
                //if (data.hasExtra(SessionDialog.TAG)) {
                //    sessionDialog(Bundle.EMPTY);
                //}

            } else if (resultCode == RESULT_CANCELED && data != null && data.hasExtra(APP_ACTION_QUIT)) {
                finish();
            } else
                configErrorDialog();
        }
    }

    private void handleVpnCancellation(Intent intent) {
        if (intent.hasExtra(Dashboard.ACTION_ASK_TO_CANCEL_VPN)) {
            eip_fragment.askToStopEIP();
            intent.removeExtra(ACTION_ASK_TO_CANCEL_VPN);
        }
    }

    private void handleConfigureAlwaysOn(Intent intent) {
            intent.removeExtra(APP_ACTION_CONFIGURE_ALWAYS_ON_PROFILE);
            Log.d(TAG, "start Configuration wizard!");
            startActivityForResult(new Intent(this, ProviderListActivity.class), REQUEST_CODE_CONFIGURE_LEAP);
    }

    private void prepareEIP(Bundle savedInstanceState) {
        boolean provider_exists = previousProviderExists(savedInstanceState);
        if (provider_exists) {
            provider = getProvider(savedInstanceState);
            if(!provider.isConfigured()) {
                configureLeapProvider();
            } else {
                Log.d(TAG, "vpn provider is configured");
                buildDashboard(getIntent().getBooleanExtra(EIP_RESTART_ON_BOOT, false));
                user_status_fragment.restoreSessionStatus(savedInstanceState);
            }
        } else {
            configureLeapProvider();
        }
    }

    private void configureLeapProvider() {
        if (getIntent().hasExtra(APP_ACTION_CONFIGURE_ALWAYS_ON_PROFILE)) {
            getIntent().removeExtra(APP_ACTION_CONFIGURE_ALWAYS_ON_PROFILE);
        }
        startActivityForResult(new Intent(this, ProviderListActivity.class), REQUEST_CODE_CONFIGURE_LEAP);
    }
    @SuppressLint("CommitPrefEdits")
    private void providerToPreferences(Provider provider) {
        preferences.edit().putBoolean(PROVIDER_CONFIGURED, true).
                putString(Provider.MAIN_URL, provider.getMainUrl().toString()).
                putString(Provider.KEY, provider.getDefinition().toString()).apply();
    }

    private void configErrorDialog() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle(getResources().getString(R.string.setup_error_title));
        alertBuilder
                .setMessage(getResources().getString(R.string.setup_error_text))
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.setup_error_configure_button), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent(Dashboard.this, ProviderListActivity.class), REQUEST_CODE_CONFIGURE_LEAP);
                    }
                })
                .setNegativeButton(getResources().getString(R.string.setup_error_close_button), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        preferences.edit().remove(Provider.KEY).remove(PROVIDER_CONFIGURED).apply();
                        finish();
                    }
                })
                .show();
    }

    /**
     * Inflates permanent UI elements of the View and contains logic for what
     * service dependent UI elements to include.
     */
    //TODO: REFACTOR ME! Consider implementing a manager that handles most of EipFragment's logic about handling EIP commands.
    //This way, we could avoid to create UI elements (like fragmentManager.replace(R.id.servicesCollection, eip_fragment, EipFragment.TAG); )
    // just to start services and destroy them afterwards
    private void buildDashboard(boolean hideAndTurnOnEipOnBoot) {
        setContentView(R.layout.dashboard);

        provider_name.setText(provider.getDomain());

        user_status_fragment = new UserStatusFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean(Provider.ALLOW_REGISTRATION, provider.allowsRegistration());
        user_status_fragment.setArguments(bundle);
        fragment_manager.replace(R.id.user_status_fragment, user_status_fragment, UserStatusFragment.TAG);

//        if (provider.hasEIP()) {
//            fragment_manager.removePreviousFragment(EipFragment.TAG);
//            eip_fragment = prepareEipFragment(hideAndTurnOnEipOnBoot);
//            fragment_manager.replace(R.id.servicesCollection, eip_fragment, EipFragment.TAG);
//            if (hideAndTurnOnEipOnBoot) {
//                onBackPressed();
//            }
//        }
    }

    /**
     *
     * @param hideAndTurnOnEipOnBoot Flag that indicates if system intent android.intent.action.BOOT_COMPLETED
     *                               has caused to start Dashboard
     * @return the created VPNFragment
     */
    public EipFragment prepareEipFragment(boolean hideAndTurnOnEipOnBoot) {
        EipFragment eip_fragment = new EipFragment();

        if (hideAndTurnOnEipOnBoot && !isAlwaysOn()) {
            preferences.edit().remove(EIP_RESTART_ON_BOOT).apply();
            Bundle arguments = new Bundle();
            arguments.putBoolean(EipFragment.START_EIP_ON_BOOT, true);
            Log.d(TAG, "set START_EIP_ON_BOOT argument for eip_fragment");
            eip_fragment.setArguments(arguments);

        }
        return eip_fragment;
    }

    /**
     * checks if Android's VPN feature 'always-on' is enabled for Bitmask
     * @return true if 'always-on' is enabled false if not
     */
    private boolean isAlwaysOn() {
        return  preferences.getBoolean(EIP_IS_ALWAYS_ON, false);
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
                if (User.loggedIn()) user_status_fragment.logOut();
                else switchProvider();
                return true;
            case R.id.signup_button:
                //sessionDialog(Bundle.EMPTY);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void showAbout() {
        Intent intent = new Intent(this, AboutFragment.class);
        startActivity(intent);
    }

    public void showLog() {
        LogWindowWrapper log_window_wrapper = LogWindowWrapper.getInstance(this);
        log_window_wrapper.showLog();
    }

    private void switchProvider() {
//        if (provider.hasEIP()) eip_fragment.stopEipIfPossible();

        ConfigHelper.clearDataOfLastProvider(preferences);

        startActivityForResult(new Intent(this, ProviderListActivity.class), REQUEST_CODE_SWITCH_PROVIDER);
    }

    public static Provider getProvider() { return provider; }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        intent.putExtra(REQUEST_CODE_KEY, requestCode);
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
