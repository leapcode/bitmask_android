package se.leap.bitmaskclient;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Observable;
import java.util.Observer;

import de.blinkt.openvpn.core.IOpenVPNServiceInternal;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VpnStatus;
import se.leap.bitmaskclient.drawer.NavigationDrawerFragment;
import se.leap.bitmaskclient.eip.EipCommand;
import se.leap.bitmaskclient.eip.EipStatus;
import se.leap.bitmaskclient.eip.VoidVpnService;
import se.leap.bitmaskclient.fragments.LogFragment;

import static android.content.Intent.CATEGORY_DEFAULT;
import static se.leap.bitmaskclient.Constants.BROADCAST_EIP_EVENT;
import static se.leap.bitmaskclient.Constants.BROADCAST_PROVIDER_API_EVENT;
import static se.leap.bitmaskclient.Constants.BROADCAST_RESULT_CODE;
import static se.leap.bitmaskclient.Constants.BROADCAST_RESULT_KEY;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_START;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_STOP;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_STOP_BLOCKING_VPN;
import static se.leap.bitmaskclient.Constants.EIP_REQUEST;
import static se.leap.bitmaskclient.Constants.EIP_RESTART_ON_BOOT;
import static se.leap.bitmaskclient.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.Constants.REQUEST_CODE_CONFIGURE_LEAP;
import static se.leap.bitmaskclient.Constants.REQUEST_CODE_LOG_IN;
import static se.leap.bitmaskclient.Constants.REQUEST_CODE_SWITCH_PROVIDER;
import static se.leap.bitmaskclient.Constants.SHARED_PREFERENCES;
import static se.leap.bitmaskclient.EipFragment.ASK_TO_CANCEL_VPN;
import static se.leap.bitmaskclient.ProviderAPI.CORRECTLY_DOWNLOADED_EIP_SERVICE;
import static se.leap.bitmaskclient.ProviderAPI.CORRECTLY_DOWNLOADED_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.ProviderAPI.ERRORS;
import static se.leap.bitmaskclient.ProviderAPI.INCORRECTLY_DOWNLOADED_EIP_SERVICE;
import static se.leap.bitmaskclient.ProviderAPI.INCORRECTLY_DOWNLOADED_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.ProviderCredentialsBaseActivity.USER_MESSAGE;
import static se.leap.bitmaskclient.R.string.downloading_vpn_certificate_failed;
import static se.leap.bitmaskclient.R.string.vpn_certificate_user_message;


public class MainActivity extends AppCompatActivity implements Observer {

    public final static String TAG = MainActivity.class.getSimpleName();

    private static final String KEY_ACTIVITY_STATE = "key state of activity";
    private static final String DEFAULT_UI_STATE = "default state";
    private static final String SHOW_DIALOG_STATE = "show dialog";
    private static final String REASON_TO_FAIL = "reason to fail";

    private static Provider provider = new Provider();
    private SharedPreferences preferences;
    private EipStatus eipStatus;
    private NavigationDrawerFragment navigationDrawerFragment;
    private MainActivityBroadcastReceiver mainActivityBroadcastReceiver;

    private IOpenVPNServiceInternal mService;
    private ServiceConnection openVpnConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mService = IOpenVPNServiceInternal.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
        }

    };

    public final static String ACTION_SHOW_VPN_FRAGMENT = "action_show_vpn_fragment";
    public final static String ACTION_SHOW_LOG_FRAGMENT = "action_show_log_fragment";

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        mainActivityBroadcastReceiver = new MainActivityBroadcastReceiver();
        setUpBroadcastReceiver();

        navigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        provider = ConfigHelper.getSavedProviderFromSharedPreferences(preferences);

        // Set up the drawer.
        navigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        eipStatus = EipStatus.getInstance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindOpenVpnService();
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
                break;
            case ACTION_SHOW_LOG_FRAGMENT:
                fragment = new LogFragment();
                break;
            default:
                break;
        }
        // on layout change / recreation of the activity, we don't want create new Fragments
        // instead the fragments themselves care about recreation and state restoration
        intent.setAction(null);

        if (fragment != null) {
            new FragmentManagerEnhanced(getSupportFragmentManager()).beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
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

            ConfigHelper.storeProviderInPreferences(preferences, provider);
            navigationDrawerFragment.refresh();

            switch (requestCode) {
                case REQUEST_CODE_SWITCH_PROVIDER:
                    EipCommand.stopVPN(this);
                    break;
                case REQUEST_CODE_CONFIGURE_LEAP:
                    break;
                case REQUEST_CODE_LOG_IN:
                    EipCommand.startVPN(this);
                    break;
            }
        }

        Fragment fragment = new EipFragment();
        Bundle arguments = new Bundle();
        arguments.putParcelable(PROVIDER_KEY, provider);
        fragment.setArguments(arguments);
        new FragmentManagerEnhanced(getSupportFragmentManager()).beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(openVpnConnection);
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mainActivityBroadcastReceiver);
        mainActivityBroadcastReceiver = null;
        super.onDestroy();
    }


    @Override
    public void update(Observable observable, Object data) {
        if (observable instanceof EipStatus) {
            eipStatus = (EipStatus) observable;
        }
    }

    private void setUpBroadcastReceiver() {
        IntentFilter updateIntentFilter = new IntentFilter(BROADCAST_EIP_EVENT);
        updateIntentFilter.addAction(BROADCAST_PROVIDER_API_EVENT);
        updateIntentFilter.addCategory(CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(this).registerReceiver(mainActivityBroadcastReceiver, updateIntentFilter);
        Log.d(TAG, "broadcast registered");
    }

    private class MainActivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "received Broadcast");

            String action = intent.getAction();
            if (action == null) {
                return;
            }

            int resultCode = intent.getIntExtra(BROADCAST_RESULT_CODE, RESULT_CANCELED);
            Bundle resultData = intent.getParcelableExtra(BROADCAST_RESULT_KEY);
            if (resultData == null) {
                resultData = Bundle.EMPTY;
            }

            switch (action) {
                case BROADCAST_EIP_EVENT:
                    handleEIPEvent(resultCode, resultData);
                    break;
                case BROADCAST_PROVIDER_API_EVENT:
                    handleProviderApiEvent(resultCode, resultData);
                    break;
            }
        }
    }

    private void handleEIPEvent(int resultCode, Bundle resultData) {
        String request = resultData.getString(EIP_REQUEST);

        if (request == null) {
            return;
        }

        switch (request) {
            case EIP_ACTION_START:
                switch (resultCode) {
                    case RESULT_OK:
                        break;
                    case RESULT_CANCELED:
                        String error = resultData.getString(ERRORS);
                        if (LeapSRPSession.loggedIn() || provider.allowsAnonymous()) {
                            showMainActivityErrorDialog(error);
                        } else {
                            askUserToLogIn(getString(vpn_certificate_user_message));
                        }
                        break;
                }
                break;
            case EIP_ACTION_STOP:
                switch (resultCode) {
                    case RESULT_OK:
                        stop();
                        break;
                    case RESULT_CANCELED:
                        break;
                }
                break;
        }
    }

    public void handleProviderApiEvent(int resultCode, Bundle resultData) {
        // TODO call DOWNLOAD_EIP_SERVICES ore remove respective cases
        switch (resultCode) {
            case CORRECTLY_DOWNLOADED_EIP_SERVICE:
                provider = resultData.getParcelable(PROVIDER_KEY);
                EipCommand.startVPN(this);
                break;
            case INCORRECTLY_DOWNLOADED_EIP_SERVICE:
                // TODO CATCH ME IF YOU CAN - WHAT DO WE WANT TO DO?
                break;

            case CORRECTLY_DOWNLOADED_VPN_CERTIFICATE:
                provider = resultData.getParcelable(PROVIDER_KEY);
                ConfigHelper.storeProviderInPreferences(preferences, provider);
                EipCommand.startVPN(this);
                break;
            case INCORRECTLY_DOWNLOADED_VPN_CERTIFICATE:
                if (LeapSRPSession.loggedIn() || provider.allowsAnonymous()) {
                    showMainActivityErrorDialog(getString(downloading_vpn_certificate_failed));
                } else {
                    askUserToLogIn(getString(vpn_certificate_user_message));
                }
                break;
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
                e.printStackTrace();
                newFragment = MainActivityErrorDialog.newInstance(provider, reasonToFail);
            }
            newFragment.show(fragmentTransaction, MainActivityErrorDialog.TAG);
        } catch (IllegalStateException | NullPointerException e) {
            e.printStackTrace();
            Log.w(TAG, "error dialog leaked!");
        }

    }


    private void stop() {
        preferences.edit().putBoolean(EIP_RESTART_ON_BOOT, false).apply();
        if (eipStatus.isBlockingVpnEstablished()) {
            stopBlockingVpn();
        }
        disconnect();
    }

    private void stopBlockingVpn() {
        Log.d(TAG, "stop VoidVpn!");
        Intent stopVoidVpnIntent = new Intent(this, VoidVpnService.class);
        stopVoidVpnIntent.setAction(EIP_ACTION_STOP_BLOCKING_VPN);
        startService(stopVoidVpnIntent);
    }

    private void disconnect() {
        ProfileManager.setConntectedVpnProfileDisconnected(this);
        if (mService != null) {
            try {
                mService.stopVPN(false);
            } catch (RemoteException e) {
                VpnStatus.logException(e);
            }
        }
    }

    private void bindOpenVpnService() {
        Intent intent = new Intent(this, OpenVPNService.class);
        intent.setAction(OpenVPNService.START_SERVICE);
        bindService(intent, openVpnConnection, Context.BIND_AUTO_CREATE);
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
