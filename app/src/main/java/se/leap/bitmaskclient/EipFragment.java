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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.AppCompatImageView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.Observable;
import java.util.Observer;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.blinkt.openvpn.core.IOpenVPNServiceInternal;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VpnStatus;
import se.leap.bitmaskclient.eip.EipCommand;
import se.leap.bitmaskclient.eip.EipStatus;
import se.leap.bitmaskclient.eip.VoidVpnService;
import se.leap.bitmaskclient.views.VpnStateImage;

import static android.app.Activity.RESULT_OK;
import static android.content.Intent.CATEGORY_DEFAULT;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static de.blinkt.openvpn.core.ConnectionStatus.LEVEL_NONETWORK;
import static se.leap.bitmaskclient.Constants.BROADCAST_EIP_EVENT;
import static se.leap.bitmaskclient.Constants.BROADCAST_PROVIDER_API_EVENT;
import static se.leap.bitmaskclient.Constants.BROADCAST_RESULT_CODE;
import static se.leap.bitmaskclient.Constants.BROADCAST_RESULT_KEY;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_CHECK_CERT_VALIDITY;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_START;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_STOP;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_STOP_BLOCKING_VPN;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_UPDATE;
import static se.leap.bitmaskclient.Constants.EIP_NOTIFICATION;
import static se.leap.bitmaskclient.Constants.EIP_REQUEST;
import static se.leap.bitmaskclient.Constants.EIP_RESTART_ON_BOOT;
import static se.leap.bitmaskclient.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.Constants.REQUEST_CODE_LOG_IN;
import static se.leap.bitmaskclient.Constants.REQUEST_CODE_SWITCH_PROVIDER;
import static se.leap.bitmaskclient.Constants.SHARED_PREFERENCES;
import static se.leap.bitmaskclient.ProviderAPI.CORRECTLY_DOWNLOADED_CERTIFICATE;
import static se.leap.bitmaskclient.ProviderAPI.CORRECTLY_DOWNLOADED_EIP_SERVICE;
import static se.leap.bitmaskclient.ProviderAPI.DOWNLOAD_CERTIFICATE;
import static se.leap.bitmaskclient.ProviderAPI.INCORRECTLY_DOWNLOADED_CERTIFICATE;
import static se.leap.bitmaskclient.ProviderAPI.INCORRECTLY_DOWNLOADED_EIP_SERVICE;

public class EipFragment extends Fragment implements Observer {

    public final static String TAG = EipFragment.class.getSimpleName();

    protected static final String IS_CONNECTED = TAG + ".is_connected";
    public static final String START_EIP_ON_BOOT = "start on boot";
    public static final String ASK_TO_CANCEL_VPN = "ask_to_cancel_vpn";


    private SharedPreferences preferences;
    private Provider provider;

    @InjectView(R.id.background)
    AppCompatImageView background;

    @InjectView(R.id.vpn_state_image)
    VpnStateImage vpnStateImage;

    @InjectView(R.id.vpn_main_button)
    Button mainButton;

    @InjectView(R.id.routed_text)
    TextView routedText;

    @InjectView(R.id.vpn_route)
    TextView vpnRoute;

    private EipStatus eipStatus;
    private boolean wantsToConnect;

    private EIPFragmentBroadcastReceiver eipFragmentBroadcastReceiver;

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

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Bundle arguments = getArguments();
        Activity activity = getActivity();
        if (activity != null) {
            if (arguments != null) {
                provider = arguments.getParcelable(PROVIDER_KEY);
                if (provider == null) {
                    activity.startActivityForResult(new Intent(activity, ProviderListActivity.class), REQUEST_CODE_SWITCH_PROVIDER);
                } else {
                    Log.d(TAG, provider.getName() + " configured as provider");
                }
            } else {
                Log.e(TAG, "no provider given - starting ProviderListActivity");
                activity.startActivityForResult(new Intent(activity, ProviderListActivity.class), REQUEST_CODE_SWITCH_PROVIDER);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        eipStatus = EipStatus.getInstance();
        eipFragmentBroadcastReceiver = new EIPFragmentBroadcastReceiver();
        Activity activity = getActivity();
        if (activity != null) {
            preferences = getActivity().getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        } else {
            Log.e(TAG, "activity is null in onCreate - no preferences set!");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        eipStatus.addObserver(this);
        View view = inflater.inflate(R.layout.eip_service_fragment, container, false);
        ButterKnife.inject(this, view);

        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(ASK_TO_CANCEL_VPN) && arguments.getBoolean(ASK_TO_CANCEL_VPN)) {
            askToStopEIP();
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        //FIXME: avoid race conditions while checking certificate an logging in at about the same time
        //eipCommand(Constants.EIP_ACTION_CHECK_CERT_VALIDITY);
        setUpBroadcastReceiver();
        handleNewState();
        bindOpenVpnService();
    }

    @Override
    public void onPause() {
        super.onPause();

        Activity activity = getActivity();
        if (activity != null) {
            getActivity().unbindService(openVpnConnection);
            LocalBroadcastManager.getInstance(activity).unregisterReceiver(eipFragmentBroadcastReceiver);
        }
        Log.d(TAG, "broadcast unregistered");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        eipStatus.deleteObserver(this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(IS_CONNECTED, eipStatus.isConnected());
        super.onSaveInstanceState(outState);
    }

    private void saveStatus(boolean restartOnBoot) {
        preferences.edit().putBoolean(EIP_RESTART_ON_BOOT, restartOnBoot).apply();
    }

    @OnClick(R.id.vpn_main_button)
    void onButtonClick() {
        handleIcon();
    }

    @OnClick(R.id.vpn_state_image)
    void onVpnStateImageClick() {
        handleIcon();
    }

    void handleIcon() {
        if (eipStatus.isConnected() || eipStatus.isConnecting())
            handleSwitchOff();
        else
            handleSwitchOn();
    }

    private void handleSwitchOn() {
        Context context = getContext();
        if (context == null) {
            Log.e(TAG, "context is null when switch turning on");
            return;
        }

        if (canStartEIP()) {
            startEipFromScratch();
        } else if (canLogInToStartEIP()) {
            wantsToConnect = true;
            Intent intent = new Intent(getContext(), LoginActivity.class);
            intent.putExtra(PROVIDER_KEY, provider);
            Activity activity = getActivity();
            if (activity != null) {
                activity.startActivityForResult(intent, REQUEST_CODE_LOG_IN);
            }
        } else {
            // provider has no VpnCertificate but user is logged in
            downloadVpnCertificate();
        }
    }

    private boolean canStartEIP() {
        boolean certificateExists = provider.hasVpnCertificate();
        boolean isAllowedAnon = provider.allowsAnonymous();
        return (isAllowedAnon || certificateExists) && !eipStatus.isConnected() && !eipStatus.isConnecting();
    }

    private boolean canLogInToStartEIP() {
        boolean isAllowedRegistered = provider.allowsRegistered();
        boolean isLoggedIn = !LeapSRPSession.getToken().isEmpty();
        return isAllowedRegistered && !isLoggedIn && !eipStatus.isConnecting() && !eipStatus.isConnected();
    }

    private void handleSwitchOff() {
        if (eipStatus.isConnecting()) {
            askPendingStartCancellation();
        } else if (eipStatus.isConnected()) {
            askToStopEIP();
        }
    }

    private void askPendingStartCancellation() {
        Activity activity = getActivity();
        if (activity == null) {
            Log.e(TAG, "activity is null when asking to cancel");
            return;
        }
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
        alertBuilder.setTitle(activity.getString(R.string.eip_cancel_connect_title))
                .setMessage(activity.getString(R.string.eip_cancel_connect_text))
                .setPositiveButton((android.R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        stopEipIfPossible();
                    }
                })
                .setNegativeButton(activity.getString(android.R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();

    }

    public void startEipFromScratch() {
        Context context = getContext();
        if (context == null) {
            Log.e(TAG, "context is null when trying to start VPN");
            return;
        }
        wantsToConnect = false;
        saveStatus(true);
        EipCommand.startVPN(context, false);
        vpnStateImage.showProgress();
        routedText.setVisibility(GONE);
        vpnRoute.setVisibility(GONE);
        colorBackgroundALittle();
    }

    private void stop() {
        saveStatus(false);
        if (eipStatus.isBlockingVpnEstablished()) {
            stopBlockingVpn();
        }
        disconnect();
    }

    private void stopBlockingVpn() {
        Log.d(TAG, "stop VoidVpn!");
        Activity activity = getActivity();
        if (activity == null) {
            // TODO what to do if not stopping void vpn?
            Log.e(TAG, "activity is null when trying to stop blocking vpn");
            return;
        }
        Intent stopVoidVpnIntent = new Intent(activity, VoidVpnService.class);
        stopVoidVpnIntent.setAction(EIP_ACTION_STOP_BLOCKING_VPN);
        activity.startService(stopVoidVpnIntent);
    }

    private void disconnect() {
        ProfileManager.setConntectedVpnProfileDisconnected(getActivity());
        if (mService != null) {
            try {
                mService.stopVPN(false);
            } catch (RemoteException e) {
                VpnStatus.logException(e);
            }
        }
    }

    protected void stopEipIfPossible() {
        Context context = getContext();
        if (context == null) {
            Log.e(TAG, "context is null when trying to stop EIP");
            return;
        }
        EipCommand.stopVPN(getContext());
    }

    protected void askToStopEIP() {
        Activity activity = getActivity();
        if (activity == null) {
            Log.e(TAG, "activity is null when asking to stop EIP");
        }
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(activity);
        alertBuilder.setTitle(activity.getString(R.string.eip_cancel_connect_title))
                .setMessage(activity.getString(R.string.eip_warning_browser_inconsistency))
                .setPositiveButton((android.R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        stopEipIfPossible();
                    }
                })
                .setNegativeButton(activity.getString(android.R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    @Override
    public void update(Observable observable, Object data) {
        if (observable instanceof EipStatus) {
            eipStatus = (EipStatus) observable;
            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        handleNewState();
                    }
                });
            } else {
                Log.e("EipFragment", "activity is null");
            }
        }
    }

    private void handleNewState() {
        Activity activity = getActivity();
        if (activity == null) {
            Log.e(TAG, "activity is null while trying to handle new state");
            return;
        }

        if (eipStatus.isConnecting()) {
            mainButton.setText(activity.getString(android.R.string.cancel));
            vpnStateImage.setStateIcon(R.drawable.vpn_connecting);
            vpnStateImage.showProgress();
            routedText.setVisibility(GONE);
            vpnRoute.setVisibility(GONE);
            colorBackgroundALittle();
        } else if (eipStatus.isConnected() || isOpenVpnRunningWithoutNetwork()) {
            mainButton.setText(activity.getString(R.string.vpn_button_turn_off));
            vpnStateImage.setStateIcon(R.drawable.vpn_connected);
            vpnStateImage.stopProgress(true);
            routedText.setVisibility(VISIBLE);
            vpnRoute.setVisibility(VISIBLE);
            vpnRoute.setText(ConfigHelper.getProviderName(preferences));
            colorBackground();
        } else {
            mainButton.setText(activity.getString(R.string.vpn_button_turn_on));
            vpnStateImage.setStateIcon(R.drawable.vpn_disconnected);
            vpnStateImage.stopProgress(false);
            routedText.setVisibility(GONE);
            vpnRoute.setVisibility(GONE);
            greyscaleBackground();
        }
    }

    private boolean isOpenVpnRunningWithoutNetwork() {
        boolean isRunning = false;
        try {
            isRunning = eipStatus.getLevel() == LEVEL_NONETWORK &&
                    mService.isVpnRunning();
        } catch (Exception e) {
            //eat me
            e.printStackTrace();
        }

        return isRunning;
    }

    private void bindOpenVpnService() {
        Activity activity = getActivity();
        if (activity == null) {
            Log.e(TAG, "activity is null when binding OpenVpn");
            return;
        }

        Intent intent = new Intent(activity, OpenVPNService.class);
        intent.setAction(OpenVPNService.START_SERVICE);
        activity.bindService(intent, openVpnConnection, Context.BIND_AUTO_CREATE);

    }

    private class EIPFragmentBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "received Broadcast");

            String action = intent.getAction();
            if (action == null) {
                return;
            }

            int resultCode = intent.getIntExtra(BROADCAST_RESULT_CODE, -1);
            Bundle resultData = intent.getParcelableExtra(BROADCAST_RESULT_KEY);
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
                    case Activity.RESULT_CANCELED:
                        break;
                }
                break;
            case EIP_ACTION_STOP:
                switch (resultCode) {
                    case RESULT_OK:
                        stop();
                        break;
                    case Activity.RESULT_CANCELED:
                        break;
                }
                break;
            case EIP_NOTIFICATION:
                switch (resultCode) {
                    case RESULT_OK:
                        break;
                    case Activity.RESULT_CANCELED:
                        break;
                }
                break;
            case EIP_ACTION_CHECK_CERT_VALIDITY:
                switch (resultCode) {
                    case RESULT_OK:
                        break;
                    case Activity.RESULT_CANCELED:
                        downloadVpnCertificate();
                        break;
                }
                break;
            case EIP_ACTION_UPDATE:
                switch (resultCode) {
                    case RESULT_OK:
                        if (wantsToConnect)
                            startEipFromScratch();
                        break;
                    case Activity.RESULT_CANCELED:
                        handleNewState();
                        break;
                }
        }
    }

    private void greyscaleBackground() {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);
        ColorMatrixColorFilter cf = new ColorMatrixColorFilter(matrix);
        background.setColorFilter(cf);
        background.setImageAlpha(255);
    }

    private void colorBackgroundALittle() {
        background.setColorFilter(null);
        background.setImageAlpha(144);
    }

    private void colorBackground() {
        background.setColorFilter(null);
        background.setImageAlpha(210);
    }

    public void handleProviderApiEvent(int resultCode, Bundle resultData) {
        Context context = getContext();
        if (context == null) {
            return;
        }

        // TODO call DOWNLOAD_EIP_SERVICES ore remove respective cases
        switch (resultCode) {
            case CORRECTLY_DOWNLOADED_EIP_SERVICE:
                provider = resultData.getParcelable(PROVIDER_KEY);
                EipCommand.updateEipService(context);
                break;
            case INCORRECTLY_DOWNLOADED_EIP_SERVICE:
                //dashboard.setResult(RESULT_CANCELED);
                // TODO CATCH ME IF YOU CAN - WHAT DO WE WANT TO DO?
                break;
            case CORRECTLY_DOWNLOADED_CERTIFICATE:
                startEipFromScratch();
                break;
            case INCORRECTLY_DOWNLOADED_CERTIFICATE:
                // TODO CATCH ME IF YOU CAN - LOGIN?
                break;
        }
    }

    private void downloadVpnCertificate() {
        ProviderAPICommand.execute(getContext(), DOWNLOAD_CERTIFICATE, provider);
    }

    private void setUpBroadcastReceiver() {
        Activity activity = getActivity();
        if (activity != null) {
            IntentFilter updateIntentFilter = new IntentFilter(BROADCAST_EIP_EVENT);
            updateIntentFilter.addAction(BROADCAST_PROVIDER_API_EVENT);
            updateIntentFilter.addCategory(CATEGORY_DEFAULT);
            LocalBroadcastManager.getInstance(activity).registerReceiver(eipFragmentBroadcastReceiver, updateIntentFilter);
            Log.d(TAG, "broadcast registered");
        } else {
            Log.e(TAG, "activity null when setting up broadcast receiver");
        }
    }

}
