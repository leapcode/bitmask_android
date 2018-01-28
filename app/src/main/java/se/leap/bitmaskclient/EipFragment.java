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
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.v4.app.Fragment;
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
import se.leap.bitmaskclient.eip.EIP;
import se.leap.bitmaskclient.eip.EipStatus;
import se.leap.bitmaskclient.eip.VoidVpnService;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static de.blinkt.openvpn.core.ConnectionStatus.LEVEL_NONETWORK;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_CHECK_CERT_VALIDITY;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_START;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_STOP;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_STOP_BLOCKING_VPN;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_UPDATE;
import static se.leap.bitmaskclient.Constants.EIP_NOTIFICATION;
import static se.leap.bitmaskclient.Constants.EIP_RECEIVER;
import static se.leap.bitmaskclient.Constants.EIP_REQUEST;
import static se.leap.bitmaskclient.Constants.EIP_RESTART_ON_BOOT;
import static se.leap.bitmaskclient.Constants.PROVIDER_ALLOWED_REGISTERED;
import static se.leap.bitmaskclient.Constants.PROVIDER_ALLOW_ANONYMOUS;
import static se.leap.bitmaskclient.Constants.PROVIDER_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.Constants.SHARED_PREFERENCES;

public class EipFragment extends Fragment implements Observer {

    public static String TAG = EipFragment.class.getSimpleName();

    protected static final String IS_CONNECTED = TAG + ".is_connected";
    public static final String START_EIP_ON_BOOT = "start on boot";

    private SharedPreferences preferences;

    @InjectView(R.id.background)
    AppCompatImageView background;

    @InjectView(R.id.key)
    AppCompatImageView key;

    @InjectView(R.id.cirle)
    AppCompatImageView circle;

    @InjectView(R.id.vpn_main_button)
    Button mainButton;

    @InjectView(R.id.routed_text)
    TextView routedText;

    @InjectView(R.id.vpn_route)
    TextView vpnRoute;

    private EIPReceiver eipReceiver;
    private EipStatus eipStatus;
    private boolean wantsToConnect;

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

    public void onAttach(Context context) {
        super.onAttach(context);
        downloadEIPServiceConfig();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        eipStatus = EipStatus.getInstance();
        eipStatus.addObserver(this);
        eipReceiver = new EIPReceiver(new Handler());
        preferences = getActivity().getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.eip_service_fragment, container, false);
        ButterKnife.inject(this, view);

        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(START_EIP_ON_BOOT) && arguments.getBoolean(START_EIP_ON_BOOT)) {
            startEipFromScratch();
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        //FIXME: avoid race conditions while checking certificate an logging in at about the same time
        //eipCommand(Constants.EIP_ACTION_CHECK_CERT_VALIDITY);
        handleNewState();
        bindOpenVpnService();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unbindService(openVpnConnection);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
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

    @OnClick(R.id.key)
    void onKeyClick() {
        handleIcon();
    }

    @OnClick(R.id.cirle)
    void onCircleClick() {
        handleIcon();
    }

    void handleIcon() {
        if (eipStatus.isConnected() || eipStatus.isConnecting())
            handleSwitchOff();
        else
            handleSwitchOn();
    }

    private void handleSwitchOn() {
        if (canStartEIP())
            startEipFromScratch();
        else if (canLogInToStartEIP()) {
            wantsToConnect = true;
            Bundle bundle = new Bundle();
            MainActivity.sessionDialog(bundle);
        } else {
            Log.d(TAG, "WHAT IS GOING ON HERE?!");
            // TODO: implement a fallback: check if vpncertificate was not downloaded properly or give
            // a user feedback. A button that does nothing on click is not a good option
        }
    }

    private boolean canStartEIP() {
        boolean certificateExists = !preferences.getString(PROVIDER_VPN_CERTIFICATE, "").isEmpty();
        boolean isAllowedAnon = preferences.getBoolean(PROVIDER_ALLOW_ANONYMOUS, false);
        return (isAllowedAnon || certificateExists) && !eipStatus.isConnected() && !eipStatus.isConnecting();
    }

    private boolean canLogInToStartEIP() {
        boolean isAllowedRegistered = preferences.getBoolean(PROVIDER_ALLOWED_REGISTERED, false);
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
        wantsToConnect = false;
        saveStatus(true);
        eipCommand(EIP_ACTION_START);
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
        //FIXME: no need to start a service here!
        eipCommand(EIP_ACTION_STOP);
    }

    private void downloadEIPServiceConfig() {
        ProviderAPIResultReceiver provider_api_receiver = new ProviderAPIResultReceiver(new Handler(), Dashboard.dashboardReceiver);
        if(eipReceiver != null)
            ProviderAPICommand.execute(Bundle.EMPTY, ProviderAPI.DOWNLOAD_EIP_SERVICE, provider_api_receiver);
    }

    protected void askToStopEIP() {
        Activity activity = getActivity();
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

    protected void updateEipService() {
        eipCommand(EIP_ACTION_UPDATE);
    }

    /**
     * Send a command to EIP
     *
     * @param action A valid String constant from EIP class representing an Intent
     *               filter for the EIP class
     */
    private void eipCommand(String action) {
        Activity activity = getActivity();
        // TODO validate "action"...how do we get the list of intent-filters for a class via Android API?
        Intent vpn_intent = new Intent(activity.getApplicationContext(), EIP.class);
        vpn_intent.setAction(action);
        vpn_intent.putExtra(EIP_RECEIVER, eipReceiver);
        activity.startService(vpn_intent);
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
        if (eipStatus.isConnecting()) {
            mainButton.setText(activity.getString(android.R.string.cancel));
            key.setImageResource(R.drawable.vpn_connecting);
            routedText.setVisibility(GONE);
            vpnRoute.setVisibility(GONE);
            colorBackgroundALittle();
        } else if (eipStatus.isConnected() || isOpenVpnRunningWithoutNetwork()) {
            mainButton.setText(activity.getString(R.string.vpn_button_turn_off));
            key.setImageResource(R.drawable.vpn_connected);
            routedText.setVisibility(VISIBLE);
            vpnRoute.setVisibility(VISIBLE);
            vpnRoute.setText(ConfigHelper.getProviderName(preferences));
            colorBackground();
        } else {
            mainButton.setText(activity.getString(R.string.vpn_button_turn_on));
            key.setImageResource(R.drawable.vpn_disconnected);
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
        Intent intent = new Intent(activity, OpenVPNService.class);
        intent.setAction(OpenVPNService.START_SERVICE);
        activity.bindService(intent, openVpnConnection, Context.BIND_AUTO_CREATE);
    }

    protected class EIPReceiver extends ResultReceiver {

        EIPReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);

            String request = resultData.getString(EIP_REQUEST);

            if (request == null) {
                return;
            }

            switch (request) {
                case EIP_ACTION_START:
                    switch (resultCode) {
                        case Activity.RESULT_OK:
                            break;
                        case Activity.RESULT_CANCELED:
                            break;
                    }
                    break;
                case EIP_ACTION_STOP:
                    switch (resultCode) {
                        case Activity.RESULT_OK:
                            stop();
                            break;
                        case Activity.RESULT_CANCELED:
                            break;
                    }
                    break;
                case EIP_NOTIFICATION:
                    switch (resultCode) {
                        case Activity.RESULT_OK:
                            break;
                        case Activity.RESULT_CANCELED:
                            break;
                    }
                    break;
                case EIP_ACTION_CHECK_CERT_VALIDITY:
                    switch (resultCode) {
                        case Activity.RESULT_OK:
                            break;
                        case Activity.RESULT_CANCELED:
                            Dashboard.downloadVpnCertificate();
                            break;
                    }
                    break;
                case EIP_ACTION_UPDATE:
                    switch (resultCode) {
                        case Activity.RESULT_OK:
                            if (wantsToConnect)
                                startEipFromScratch();
                            break;
                        case Activity.RESULT_CANCELED:
                            handleNewState();
                            break;
                    }
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
        background.setImageAlpha(255);
    }

}
