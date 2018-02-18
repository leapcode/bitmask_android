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
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
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
import se.leap.bitmaskclient.eip.EipCommand;
import se.leap.bitmaskclient.eip.EipStatus;
import se.leap.bitmaskclient.eip.VoidVpnService;
import se.leap.bitmaskclient.views.VpnStateImage;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static de.blinkt.openvpn.core.ConnectionStatus.LEVEL_NONETWORK;
import static se.leap.bitmaskclient.Constants.EIP_RESTART_ON_BOOT;
import static se.leap.bitmaskclient.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.Constants.REQUEST_CODE_LOG_IN;
import static se.leap.bitmaskclient.Constants.REQUEST_CODE_SWITCH_PROVIDER;
import static se.leap.bitmaskclient.Constants.SHARED_PREFERENCES;
import static se.leap.bitmaskclient.ProviderAPI.DOWNLOAD_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.ProviderCredentialsBaseActivity.USER_MESSAGE;
import static se.leap.bitmaskclient.R.string.vpn_certificate_user_message;

public class EipFragment extends Fragment implements Observer {

    public final static String TAG = EipFragment.class.getSimpleName();

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

    //---saved Instance -------
    private final static String KEY_SHOW_PENDING_START_CANCELLATION = "KEY_SHOW_PENDING_START_CANCELLATION";
    private final static String KEY_SHOW_ASK_TO_STOP_EIP = "KEY_SHOW_ASK_TO_STOP_EIP";
    private boolean showPendingStartCancellation = false;
    private boolean showAskToStopEip = false;
    //------------------------
    AlertDialog alertDialog;

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
            arguments.remove(ASK_TO_CANCEL_VPN);
            setArguments(arguments);
            askToStopEIP();
        }
        restoreFromSavedInstance(savedInstanceState);
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

        Activity activity = getActivity();
        if (activity != null) {
            getActivity().unbindService(openVpnConnection);
        }
        Log.d(TAG, "broadcast unregistered");
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (showAskToStopEip) {
            outState.putBoolean(KEY_SHOW_ASK_TO_STOP_EIP, true);
            alertDialog.dismiss();
        } else if (showPendingStartCancellation) {
            outState.putBoolean(KEY_SHOW_PENDING_START_CANCELLATION, true);
            alertDialog.dismiss();

        }
    }

    private void restoreFromSavedInstance(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_SHOW_PENDING_START_CANCELLATION)) {
            showPendingStartCancellation = true;
            askPendingStartCancellation();
        } else if (savedInstanceState != null && savedInstanceState.containsKey(KEY_SHOW_ASK_TO_STOP_EIP)) {
            showAskToStopEip = true;
            askToStopEIP();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        eipStatus.deleteObserver(this);
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
            askUserToLogIn(getString(vpn_certificate_user_message));
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
        boolean isLoggedIn = LeapSRPSession.loggedIn();
        return isAllowedRegistered && !isLoggedIn && !eipStatus.isConnecting() && !eipStatus.isConnected();
    }

    private void handleSwitchOff() {
        if (eipStatus.isConnecting()) {
            askPendingStartCancellation();
        } else if (eipStatus.isConnected()) {
            askToStopEIP();
        }
    }

    public void startEipFromScratch() {
        saveStatus(true);
        Context context = getContext();
        if (context == null) {
            Log.e(TAG, "context is null when trying to start VPN");
            return;
        }
        EipCommand.startVPN(context, false);
        vpnStateImage.showProgress();
        routedText.setVisibility(GONE);
        vpnRoute.setVisibility(GONE);
        colorBackgroundALittle();
    }

    protected void stopEipIfPossible() {
        Context context = getContext();
        if (context != null) {
            EipCommand.stopVPN(getContext());
        } else {
            Log.e(TAG, "context is null when trying to stop EIP");
        }
    }

    private void askPendingStartCancellation() {
        Activity activity = getActivity();
        if (activity == null) {
            Log.e(TAG, "activity is null when asking to cancel");
            return;
        }

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
        showPendingStartCancellation = true;
        alertDialog = alertBuilder.setTitle(activity.getString(R.string.eip_cancel_connect_title))
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
                }).setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                showPendingStartCancellation = false;
            }
        }).show();

    }

    protected void askToStopEIP() {
        Activity activity = getActivity();
        if (activity == null) {
            Log.e(TAG, "activity is null when asking to stop EIP");
            return;
        }
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(activity);
        showAskToStopEip = true;
        alertDialog = alertBuilder.setTitle(activity.getString(R.string.eip_cancel_connect_title))
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
                }).setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                showAskToStopEip = false;
            }
        }).show();
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

    private void downloadVpnCertificate() {
        ProviderAPICommand.execute(getContext(), DOWNLOAD_VPN_CERTIFICATE, provider);
    }

    private void askUserToLogIn(String userMessage) {
        Intent intent = new Intent(getContext(), LoginActivity.class);
        intent.putExtra(PROVIDER_KEY, provider);

        if(userMessage != null) {
            intent.putExtra(USER_MESSAGE, userMessage);
        }

        Activity activity = getActivity();
        if (activity != null) {
            activity.startActivityForResult(intent, REQUEST_CODE_LOG_IN);
        }
    }
}
