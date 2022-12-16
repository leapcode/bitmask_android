/**
 * Copyright (c) 2021 LEAP Encryption Access Project and contributers
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
package se.leap.bitmaskclient.base;


import static androidx.appcompat.app.ActionBar.DISPLAY_SHOW_CUSTOM;
import static se.leap.bitmaskclient.R.string.downloading_vpn_certificate_failed;
import static se.leap.bitmaskclient.R.string.vpn_certificate_user_message;
import static se.leap.bitmaskclient.base.models.Constants.ASK_TO_CANCEL_VPN;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_CODE;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_KEY;
import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_LAUNCH_VPN;
import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_PREPARE_VPN;
import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_START;
import static se.leap.bitmaskclient.base.models.Constants.EIP_REQUEST;
import static se.leap.bitmaskclient.base.models.Constants.EXTRA_MOTD_MSG;
import static se.leap.bitmaskclient.base.models.Constants.LOCATION;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.base.models.Constants.REQUEST_CODE_CONFIGURE_LEAP;
import static se.leap.bitmaskclient.base.models.Constants.REQUEST_CODE_LOG_IN;
import static se.leap.bitmaskclient.base.models.Constants.REQUEST_CODE_SWITCH_PROVIDER;
import static se.leap.bitmaskclient.base.models.Constants.SHARED_PREFERENCES;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.storeProviderInPreferences;
import static se.leap.bitmaskclient.eip.EIP.EIPErrors.ERROR_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.eip.EIP.EIPErrors.ERROR_VPN_PREPARE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.ERRORID;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.ERRORS;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INCORRECTLY_DOWNLOADED_EIP_SERVICE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INCORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.TOR_EXCEPTION;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.TOR_TIMEOUT;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.UPDATE_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.USER_MESSAGE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Observable;
import java.util.Observer;

import se.leap.bitmaskclient.BuildConfig;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.fragments.EipFragment;
import se.leap.bitmaskclient.base.fragments.ExcludeAppsFragment;
import se.leap.bitmaskclient.base.fragments.LogFragment;
import se.leap.bitmaskclient.base.fragments.MainActivityErrorDialog;
import se.leap.bitmaskclient.base.fragments.MotdFragment;
import se.leap.bitmaskclient.base.fragments.NavigationDrawerFragment;
import se.leap.bitmaskclient.base.fragments.SettingsFragment;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.models.ProviderObservable;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.base.views.ActionBarTitle;
import se.leap.bitmaskclient.eip.EIP;
import se.leap.bitmaskclient.eip.EipCommand;
import se.leap.bitmaskclient.eip.EipSetupListener;
import se.leap.bitmaskclient.eip.EipSetupObserver;
import se.leap.bitmaskclient.providersetup.ProviderAPI;
import se.leap.bitmaskclient.providersetup.activities.LoginActivity;
import se.leap.bitmaskclient.providersetup.models.LeapSRPSession;

public class MainActivity extends AppCompatActivity implements EipSetupListener, Observer {

    public final static String TAG = MainActivity.class.getSimpleName();

    private Provider provider;
    private SharedPreferences preferences;
    private NavigationDrawerFragment navigationDrawerFragment;

    public final static String ACTION_SHOW_VPN_FRAGMENT = "action_show_vpn_fragment";
    public final static String ACTION_SHOW_LOG_FRAGMENT = "action_show_log_fragment";
    public final static String ACTION_SHOW_DIALOG_FRAGMENT = "action_show_dialog_fragment";
    public final static String ACTION_SHOW_MOTD_FRAGMENT = "action_show_motd_fragment";

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_main);
        setupActionBar();
        navigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        provider = ProviderObservable.getInstance().getCurrentProvider();

        EipSetupObserver.addListener(this);
        // Set up the drawer.
        navigationDrawerFragment.setUp(R.id.navigation_drawer, findViewById(R.id.drawer_layout));
        handleIntentAction(getIntent());
    }

    @Override
    public void onBackPressed() {
        FragmentManagerEnhanced fragmentManagerEnhanced = new FragmentManagerEnhanced(getSupportFragmentManager());
        Fragment fragment = fragmentManagerEnhanced.findFragmentByTag(MainActivity.TAG);
        //FIXME: use proper navigation, this is only a workaround
        if (!(fragment instanceof EipFragment)) {
            Fragment newFragment;
            if (fragment instanceof ExcludeAppsFragment) {
                newFragment = new SettingsFragment();
            } else {
                newFragment = new EipFragment();
                Bundle bundle = new Bundle();
                bundle.putParcelable(PROVIDER_KEY, provider);
                newFragment.setArguments(bundle);
            }
            fragmentManagerEnhanced.replace(R.id.main_container, newFragment, MainActivity.TAG);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntentAction(intent);
    }

    private void handleIntentAction(Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        Fragment fragment = null;
        switch (intent.getAction()) {
            case ACTION_SHOW_VPN_FRAGMENT:
                fragment = new EipFragment();
                Bundle bundle = new Bundle();
                if (intent.hasExtra(ASK_TO_CANCEL_VPN)) {
                    bundle.putBoolean(ASK_TO_CANCEL_VPN, true);
                }
                bundle.putParcelable(PROVIDER_KEY, provider);
                fragment.setArguments(bundle);
                showActionBar();
                break;
            case ACTION_SHOW_MOTD_FRAGMENT:
                fragment = new MotdFragment();
                Bundle motdBundle = new Bundle();
                if (intent.hasExtra(EXTRA_MOTD_MSG)) {
                    motdBundle.putString(EXTRA_MOTD_MSG, intent.getStringExtra(EXTRA_MOTD_MSG));
                }
                fragment.setArguments(motdBundle);
                hideActionBar();
                break;
            case ACTION_SHOW_LOG_FRAGMENT:
                fragment = new LogFragment();
                showActionBar();
                break;
            case ACTION_SHOW_DIALOG_FRAGMENT:
                if (intent.hasExtra(EIP.ERRORID)) {
                    String errorId = intent.getStringExtra(EIP.ERRORID);
                    String error = intent.getStringExtra(EIP.ERRORS);
                    String args = intent.getStringExtra(LOCATION);
                    showMainActivityErrorDialog(error, EIP.EIPErrors.valueOf(errorId), args);
                }
            default:
                break;
        }
        // on layout change / recreation of the activity, we don't want create new Fragments
        // instead the fragments themselves care about recreation and state restoration
        intent.setAction(null);

        if (fragment != null) {
            new FragmentManagerEnhanced(getSupportFragmentManager())
                    .replace(R.id.main_container, fragment, MainActivity.TAG);
        }
    }

    private void setupActionBar() {
        setSupportActionBar(findViewById(R.id.toolbar));
        final ActionBar actionBar = getSupportActionBar();
        Context context = actionBar.getThemedContext();
        actionBar.setDisplayOptions(DISPLAY_SHOW_CUSTOM);

        ActionBarTitle actionBarTitle = new ActionBarTitle(context);
        actionBarTitle.setTitleCaps(BuildConfig.actionbar_capitalize_title);
        actionBarTitle.setTitle(getString(R.string.app_name));

        @ColorInt int titleColor = ContextCompat.getColor(context, R.color.colorActionBarTitleFont);
        actionBarTitle.setTitleTextColor(titleColor);

        actionBarTitle.setCentered(BuildConfig.actionbar_center_title);
        if (BuildConfig.actionbar_center_title) {
            ActionBar.LayoutParams params = new ActionBar.LayoutParams(
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    ActionBar.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER);
            actionBar.setCustomView(actionBarTitle, params);
        } else {
            actionBar.setCustomView(actionBarTitle);
        }
    }

    private void hideActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }

    private void showActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.show();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            return;
        }

        if (resultCode == RESULT_OK && data.hasExtra(Provider.KEY)) {
            provider = data.getParcelableExtra(Provider.KEY);

            if (provider == null) {
                return;
            }

            storeProviderInPreferences(preferences, provider);
            ProviderObservable.getInstance().updateProvider(provider);
            if (!provider.supportsPluggableTransports()) {
                PreferenceHelper.useBridges(this, false);
            }
            navigationDrawerFragment.refresh();

            switch (requestCode) {
                case REQUEST_CODE_SWITCH_PROVIDER:
                    EipCommand.stopVPN(this);
                    EipCommand.startVPN(this, false);
                    break;
                case REQUEST_CODE_CONFIGURE_LEAP:
                    Log.d(TAG, "REQUEST_CODE_CONFIGURE_LEAP - onActivityResult - MainActivity");
                    break;
                case REQUEST_CODE_LOG_IN:
                    EipCommand.startVPN(this, true);
                    break;
            }
        }

        // on switch provider we need to set the EIP fragment
        Fragment fragment = new EipFragment();
        Bundle arguments = new Bundle();
        arguments.putParcelable(PROVIDER_KEY, provider);
        fragment.setArguments(arguments);
        new FragmentManagerEnhanced(getSupportFragmentManager())
                .replace(R.id.main_container, fragment, MainActivity.TAG);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EipSetupObserver.removeListener(this);
    }

    @Override
    public void handleEipEvent(Intent intent) {
        int resultCode = intent.getIntExtra(BROADCAST_RESULT_CODE, RESULT_CANCELED);
        Bundle resultData = intent.getParcelableExtra(BROADCAST_RESULT_KEY);
        if (resultData == null) {
            resultData = Bundle.EMPTY;
        }
        String request = resultData.getString(EIP_REQUEST);

        if (request == null) {
            return;
        }

        switch (request) {
            case EIP_ACTION_START:
                if (resultCode == RESULT_CANCELED) {
                    String error = resultData.getString(ERRORS);
                    if (isInternalErrorHandling(error)) {
                        return;
                    }

                    if (LeapSRPSession.loggedIn() || provider.allowsAnonymous()) {
                        showMainActivityErrorDialog(error);
                    } else if (isInvalidCertificateForLoginOnlyProvider(error)) {
                        askUserToLogIn(getString(vpn_certificate_user_message));
                    }
                }
                break;
            case EIP_ACTION_PREPARE_VPN:
                if (resultCode == RESULT_CANCELED) {
                    String error = resultData.getString(ERRORS);
                    showMainActivityErrorDialog(error, ERROR_VPN_PREPARE);
                }
                break;
            case EIP_ACTION_LAUNCH_VPN:
                if (resultCode == RESULT_CANCELED) {
                    String error = resultData.getString(ERRORS);
                    showMainActivityErrorDialog(error);
                }
                break;
        }
    }

    @Override
    public void handleProviderApiEvent(Intent intent) {
        int resultCode = intent.getIntExtra(BROADCAST_RESULT_CODE, RESULT_CANCELED);

        switch (resultCode) {
            case INCORRECTLY_DOWNLOADED_EIP_SERVICE:
                // TODO CATCH ME IF YOU CAN - WHAT DO WE WANT TO DO?
                break;
            case INCORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE:
                if (LeapSRPSession.loggedIn() || provider.allowsAnonymous()) {
                    showMainActivityErrorDialog(getString(downloading_vpn_certificate_failed));
                } else {
                    askUserToLogIn(getString(vpn_certificate_user_message));
                }
                break;
            case TOR_TIMEOUT:
            case TOR_EXCEPTION:
                try {
                    Bundle resultData = intent.getParcelableExtra(BROADCAST_RESULT_KEY);
                    JSONObject jsonObject = new JSONObject(resultData.getString(ProviderAPI.ERRORS));
                    String initialAction = jsonObject.optString(ProviderAPI.INITIAL_ACTION);
                    if (UPDATE_INVALID_VPN_CERTIFICATE.equals(initialAction)) {
                        showMainActivityErrorDialog(getString(downloading_vpn_certificate_failed));
                    }
                } catch (Exception e) {
                    //ignore
                }
                break;
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof ProviderObservable) {
            this.provider = ((ProviderObservable) o).getCurrentProvider();
        }
    }

    /**
     * Shows an error dialog
     */
    public void showMainActivityErrorDialog(String reasonToFail) {
        try {

            FragmentTransaction fragmentTransaction = new FragmentManagerEnhanced(
                    this.getSupportFragmentManager()).removePreviousFragment(
                            MainActivityErrorDialog.TAG);
            DialogFragment newFragment;
            try {
                JSONObject errorJson = new JSONObject(reasonToFail);
                newFragment = MainActivityErrorDialog.newInstance(provider, errorJson);
            } catch (JSONException e) {
                newFragment = MainActivityErrorDialog.newInstance(provider, reasonToFail);
            }
            newFragment.show(fragmentTransaction, MainActivityErrorDialog.TAG);
        } catch (IllegalStateException | NullPointerException e) {
            e.printStackTrace();
            Log.w(TAG, "error dialog leaked!");
        }
    }

    /**
     * Shows an error dialog
     */
    public void showMainActivityErrorDialog(String reasonToFail, EIP.EIPErrors error, String... args) {
        try {
            FragmentTransaction fragmentTransaction = new FragmentManagerEnhanced(
                    this.getSupportFragmentManager()).removePreviousFragment(
                    MainActivityErrorDialog.TAG);
            DialogFragment newFragment = MainActivityErrorDialog.newInstance(provider, reasonToFail, error, args);
            newFragment.show(fragmentTransaction, MainActivityErrorDialog.TAG);
        } catch (IllegalStateException | NullPointerException e) {
            e.printStackTrace();
            Log.w(TAG, "error dialog leaked!");
        }
    }

    /**
     *
     * @param errorJsonString
     * @return true if errorJson is a valid json and contains only ERRORID but
     * not an ERRORS field containing an error message
     */
    public boolean isInternalErrorHandling(String errorJsonString) {
        try {
            JSONObject errorJson = new JSONObject(errorJsonString);
            return !errorJson.has(ERRORS) && errorJson.has(ERRORID);
        } catch (JSONException | NullPointerException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isInvalidCertificateForLoginOnlyProvider(String errorJsonString) {
        try {
            JSONObject errorJson = new JSONObject(errorJsonString);
            return  ERROR_INVALID_VPN_CERTIFICATE.toString().equals(errorJson.getString(ERRORID)) &&
                    !LeapSRPSession.loggedIn() &&
                    !provider.allowsAnonymous();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void askUserToLogIn(String userMessage) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(PROVIDER_KEY, provider);
        if (userMessage != null) {
            intent.putExtra(USER_MESSAGE, userMessage);
        }
        startActivityForResult(intent, REQUEST_CODE_LOG_IN);
    }
}
