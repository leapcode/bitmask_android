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
import java.util.*;

import butterknife.*;
import de.blinkt.openvpn.activities.*;
import se.leap.bitmaskclient.eip.*;

/**
 * The main user facing Activity of Bitmask Android, consisting of status, controls,
 * and access to preferences.
 *
 * @author Sean Leonard <meanderingcode@aetherislands.net>
 * @author parmegv
 */
public class Dashboard extends Activity implements SessionDialog.SessionDialogInterface, ProviderAPIResultReceiver.Receiver, Observer {

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
    @InjectView(R.id.user_session_status)
    TextView user_session_status_text_view;
    @InjectView(R.id.user_session_status_progress)
    ProgressBar user_session_status_progress_bar;

    EipFragment eip_fragment;
    private Provider provider;
    private UserSessionStatus user_session_status;
    public ProviderAPIResultReceiver providerAPI_result_receiver;
    private boolean switching_provider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app = this;
        user_session_status = UserSessionStatus.getInstance();
        user_session_status.addObserver(this);

        PRNGFixes.apply();

        preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        fragment_manager = new FragmentManagerEnhanced(getFragmentManager());
        handleVersion();

        restoreProvider(savedInstanceState);
        if (provider == null || provider.getName().isEmpty())
            startActivityForResult(new Intent(this, ConfigurationWizard.class), CONFIGURE_LEAP);
        else {
            buildDashboard(getIntent().getBooleanExtra(ON_BOOT, false));
            restoreSessionStatus(savedInstanceState);
        }
    }

    private void restoreProvider(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(Provider.KEY))
                provider = savedInstanceState.getParcelable(Provider.KEY);
        }
        if (provider == null && preferences.getBoolean(Constants.PROVIDER_CONFIGURED, false))
            provider = getSavedProviderFromSharedPreferences();
    }

    private void restoreSessionStatus(Bundle savedInstanceState) {
        if (savedInstanceState != null)
            if (savedInstanceState.containsKey(UserSessionStatus.TAG)) {
                UserSessionStatus.SessionStatus status = (UserSessionStatus.SessionStatus) savedInstanceState.getSerializable(UserSessionStatus.TAG);
                user_session_status.updateStatus(status);
            }
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        if (provider != null)
            outState.putParcelable(Provider.KEY, provider);
        if (user_session_status_text_view != null && user_session_status_text_view.getVisibility() == TextView.VISIBLE)
            outState.putSerializable(UserSessionStatus.TAG, user_session_status.sessionStatus());

        super.onSaveInstanceState(outState);
    }

    private Provider getSavedProviderFromSharedPreferences() {
        Provider provider = null;
        try {
            provider = new Provider(new URL(preferences.getString(Provider.MAIN_URL, "")));
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
                invalidateOptionsMenu();
                if (data.hasExtra(SessionDialog.TAG)) {
                    sessionDialog(Bundle.EMPTY);
                }

            } else if (resultCode == RESULT_CANCELED && data.hasExtra(ACTION_QUIT)) {
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
        handleNewUserSessionStatus(user_session_status);
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
                if (LeapSRPSession.loggedIn()) logOut();
                else switchProvider();
                return true;
            case R.id.login_button:
                sessionDialog(Bundle.EMPTY);
                return true;
            case R.id.logout_button:
                logOut();
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
        Intent startLW = new Intent(getContext(), LogWindow.class);
        startActivity(startLW);
    }

    @Override
    public void signUp(String username, String password) {
        User.setUserName(username);
        Bundle parameters = bundlePassword(password);
        providerApiCommand(parameters, 0, ProviderAPI.SIGN_UP);
    }

    @Override
    public void logIn(String username, String password) {
        User.setUserName(username);
        Bundle parameters = bundlePassword(password);
        providerApiCommand(parameters, 0, ProviderAPI.LOG_IN);
    }

    public void logOut() {
        providerApiCommand(Bundle.EMPTY, 0, ProviderAPI.LOG_OUT);
    }

    @Override
    public void update(Observable observable, Object data) {
        if (observable instanceof UserSessionStatus) {
            UserSessionStatus status = (UserSessionStatus) observable;
            handleNewUserSessionStatus(status);
        }
    }

    private void handleNewUserSessionStatus(UserSessionStatus status) {
        user_session_status = status;
        if (provider.allowsRegistration()) {
            if (user_session_status.inProgress())
                showUserSessionProgressBar();
            else
                hideUserSessionProgressBar();
            changeSessionStatusMessage(user_session_status.toString());
            invalidateOptionsMenu();
        }
    }

    private void changeSessionStatusMessage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                user_session_status_text_view.setText(message);
            }
        });
    }

    private void showUserSessionProgressBar() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                user_session_status_progress_bar.setVisibility(ProgressBar.VISIBLE);
            }
        });
    }

    private void hideUserSessionProgressBar() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                user_session_status_progress_bar.setVisibility(ProgressBar.GONE);
            }
        });
    }

    protected void downloadVpnCertificate() {
        boolean is_authenticated = LeapSRPSession.loggedIn();
        boolean allowed_anon = preferences.getBoolean(Constants.ALLOWED_ANON, false);
        if (allowed_anon || is_authenticated)
            providerApiCommand(Bundle.EMPTY, R.string.downloading_certificate_message, ProviderAPI.DOWNLOAD_CERTIFICATE);
        else
            sessionDialog(Bundle.EMPTY);

    }

    private Bundle bundlePassword(String password) {
        Bundle parameters = new Bundle();
        if (!password.isEmpty())
            parameters.putString(SessionDialog.PASSWORD, password);
        return parameters;
    }

    protected void providerApiCommand(Bundle parameters, int progressbar_message_resId, String providerApi_action) {
        if (eip_fragment != null && progressbar_message_resId != 0) {
            eip_fragment.progress_bar.setVisibility(ProgressBar.VISIBLE);
            setStatusMessage(progressbar_message_resId);
        }

        Intent command = prepareProviderAPICommand(parameters, providerApi_action);
        startService(command);
    }

    private Intent prepareProviderAPICommand(Bundle parameters, String action) {
        providerAPI_result_receiver = new ProviderAPIResultReceiver(new Handler());
        providerAPI_result_receiver.setReceiver(this);

        Intent command = new Intent(this, ProviderAPI.class);

        command.putExtra(ProviderAPI.PARAMETERS, parameters);
        command.putExtra(ProviderAPI.RECEIVER_KEY, providerAPI_result_receiver);
        command.setAction(action);
        return command;
    }

    public void cancelLoginOrSignup() {
        EipStatus.getInstance().setConnectedOrDisconnected();
    }

    public void sessionDialog(Bundle resultData) {

        FragmentTransaction transaction = fragment_manager.removePreviousFragment(SessionDialog.TAG);

        DialogFragment newFragment = new SessionDialog();
        if (provider.getName().equalsIgnoreCase("riseup")) {
            resultData = resultData == Bundle.EMPTY ? new Bundle() : resultData;
            resultData.putBoolean(SessionDialog.ERRORS.RISEUP_WARNING.toString(), true);
        }
        if (resultData != null && !resultData.isEmpty()) {
            newFragment.setArguments(resultData);
        }
        newFragment.show(transaction, SessionDialog.TAG);
    }

    private void switchProvider() {
        if (provider.hasEIP()) eip_fragment.stopEipIfPossible();

        preferences.edit().clear().apply();
        switching_provider = false;
        startActivityForResult(new Intent(this, ConfigurationWizard.class), SWITCH_PROVIDER);
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        Log.d(TAG, "onReceiveResult");
        if (resultCode == ProviderAPI.SUCCESSFUL_SIGNUP) {
            String username = resultData.getString(SessionDialog.USERNAME);
            String password = resultData.getString(SessionDialog.PASSWORD);
            logIn(username, password);
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
            eip_fragment.handleNewVpnCertificate();
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

    private void setStatusMessage(int string_resId) {
        if (eip_fragment != null && eip_fragment.status_message != null)
            eip_fragment.status_message.setText(string_resId);
    }

    public static Context getContext() {
        return app;
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        intent.putExtra(Dashboard.REQUEST_CODE, requestCode);
        super.startActivityForResult(intent, requestCode);
    }
}
