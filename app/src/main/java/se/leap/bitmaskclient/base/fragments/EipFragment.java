/**
 * Copyright (c) 2018 LEAP Encryption Access Project and contributers
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
package se.leap.bitmaskclient.base.fragments;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import java.util.Observable;
import java.util.Observer;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import de.blinkt.openvpn.core.IOpenVPNServiceInternal;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.VpnStatus;
import se.leap.bitmaskclient.providersetup.ProviderListActivity;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.FragmentManagerEnhanced;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.models.ProviderObservable;
import se.leap.bitmaskclient.base.views.VpnStateImage;
import se.leap.bitmaskclient.eip.EipCommand;
import se.leap.bitmaskclient.eip.EipStatus;
import se.leap.bitmaskclient.providersetup.ProviderAPICommand;
import se.leap.bitmaskclient.providersetup.activities.CustomProviderSetupActivity;
import se.leap.bitmaskclient.providersetup.activities.LoginActivity;
import se.leap.bitmaskclient.providersetup.models.LeapSRPSession;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static de.blinkt.openvpn.core.ConnectionStatus.LEVEL_NONETWORK;
import static se.leap.bitmaskclient.R.string.vpn_certificate_user_message;
import static se.leap.bitmaskclient.base.models.Constants.ASK_TO_CANCEL_VPN;
import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_START;
import static se.leap.bitmaskclient.base.models.Constants.EIP_EARLY_ROUTES;
import static se.leap.bitmaskclient.base.models.Constants.EIP_RESTART_ON_BOOT;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.base.models.Constants.REQUEST_CODE_CONFIGURE_LEAP;
import static se.leap.bitmaskclient.base.models.Constants.REQUEST_CODE_LOG_IN;
import static se.leap.bitmaskclient.base.models.Constants.REQUEST_CODE_SWITCH_PROVIDER;
import static se.leap.bitmaskclient.base.models.Constants.SHARED_PREFERENCES;
import static se.leap.bitmaskclient.base.utils.ConfigHelper.isDefaultBitmask;
import static se.leap.bitmaskclient.base.utils.ViewHelper.convertDimensionToPx;
import static se.leap.bitmaskclient.eip.EipSetupObserver.connectionRetry;
import static se.leap.bitmaskclient.eip.EipSetupObserver.gatewayOrder;
import static se.leap.bitmaskclient.eip.EipSetupObserver.reconnectingWithDifferentGateway;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.DOWNLOAD_GEOIP_JSON;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.UPDATE_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.USER_MESSAGE;

public class EipFragment extends Fragment implements Observer {

    public final static String TAG = EipFragment.class.getSimpleName();


    private SharedPreferences preferences;
    private Provider provider;

    @BindView(R.id.background)
    AppCompatImageView background;

    @BindView(R.id.vpn_state_image)
    VpnStateImage vpnStateImage;

    @BindView(R.id.vpn_main_button)
    AppCompatButton mainButton;

    @BindView(R.id.routed_text)
    AppCompatTextView routedText;

    @BindView(R.id.vpn_route)
    AppCompatTextView vpnRoute;

    private Unbinder unbinder;
    private EipStatus eipStatus;

    //---saved Instance -------
    private final String KEY_SHOW_PENDING_START_CANCELLATION = "KEY_SHOW_PENDING_START_CANCELLATION";
    private final String KEY_SHOW_ASK_TO_STOP_EIP = "KEY_SHOW_ASK_TO_STOP_EIP";
    private boolean showPendingStartCancellation = false;
    private boolean showAskToStopEip = false;
    //------------------------
    AlertDialog alertDialog;

    private IOpenVPNServiceInternal mService;
    private ServiceConnection openVpnConnection;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Bundle arguments = getArguments();
        Activity activity = getActivity();
        if (activity != null) {
            if (arguments != null) {
                provider = arguments.getParcelable(PROVIDER_KEY);
                if (provider == null) {
                    handleNoProvider(activity);
                } else {
                    Log.d(TAG, provider.getName() + " configured as provider");
                }
            } else {
                handleNoProvider(activity);
            }
        }
    }

    private void handleNoProvider(Activity activity) {
        if (isDefaultBitmask()) {
            activity.startActivityForResult(new Intent(activity, ProviderListActivity.class), REQUEST_CODE_SWITCH_PROVIDER);
        } else {
            Log.e(TAG, "no provider given - try to reconfigure custom provider");
            startActivityForResult(new Intent(activity, CustomProviderSetupActivity.class), REQUEST_CODE_CONFIGURE_LEAP);

        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        openVpnConnection = new EipFragmentServiceConnection();
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
        View view = inflater.inflate(R.layout.f_eip, container, false);
        unbinder = ButterKnife.bind(this, view);

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
    public void onStart() {
        super.onStart();
        if (DonationReminderDialog.isCallable(getContext())) {
            showDonationReminderDialog();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //FIXME: avoid race conditions while checking certificate an logging in at about the same time
        //eipCommand(Constants.EIP_ACTION_CHECK_CERT_VALIDITY);
        bindOpenVpnService();
        handleNewState();
    }

    @Override
    public void onPause() {
        super.onPause();

        Activity activity = getActivity();
        if (activity != null) {
            getActivity().unbindService(openVpnConnection);
        }
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
        unbinder.unbind();
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
        if (isOpenVpnRunningWithoutNetwork() || eipStatus.isConnected() || eipStatus.isConnecting())
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
            updateInvalidVpnCertificate();
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
        if (isOpenVpnRunningWithoutNetwork() || eipStatus.isConnecting()) {
            askPendingStartCancellation();
        } else if (eipStatus.isConnected()) {
            askToStopEIP();
        }
    }

    private void setMainButtonEnabled(boolean enabled) {
        mainButton.setEnabled(enabled);
        vpnStateImage.setEnabled(enabled);
    }

    public void startEipFromScratch() {
        saveStatus(true);
        Context context = getContext();
        if (context == null) {
            Log.e(TAG, "context is null when trying to start VPN");
            return;
        }
        if (!provider.getGeoipUrl().isDefault() && provider.shouldUpdateGeoIpJson()) {
            Bundle bundle = new Bundle();
            bundle.putBoolean(EIP_ACTION_START, true);
            bundle.putBoolean(EIP_EARLY_ROUTES, false);
            ProviderAPICommand.execute(getContext().getApplicationContext(), DOWNLOAD_GEOIP_JSON, bundle, provider);
        } else {
            EipCommand.startVPN(context.getApplicationContext(), false);
        }
        vpnStateImage.showProgress();
        routedText.setVisibility(GONE);
        vpnRoute.setVisibility(GONE);
        colorBackgroundALittle();
    }

    protected void stopEipIfPossible() {
        Context context = getContext();
        if (context == null) {
            Log.e(TAG, "context is null when trying to stop EIP");
            return;
        }
        EipCommand.stopVPN(context.getApplicationContext());
    }

    private void askPendingStartCancellation() {
        Activity activity = getActivity();
        if (activity == null) {
            Log.e(TAG, "activity is null when asking to cancel");
            return;
        }

        try {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
            showPendingStartCancellation = true;
            alertDialog = alertBuilder.setTitle(activity.getString(R.string.eip_cancel_connect_title))
                    .setMessage(activity.getString(R.string.eip_cancel_connect_text))
                    .setPositiveButton((android.R.string.yes), (dialog, which) -> stopEipIfPossible())
                    .setNegativeButton(activity.getString(android.R.string.no), (dialog, which) -> {
                    }).setOnDismissListener(dialog -> showPendingStartCancellation = false).show();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

    }

    protected void askToStopEIP() {
        Activity activity = getActivity();
        if (activity == null) {
            Log.e(TAG, "activity is null when asking to stop EIP");
            return;
        }
        try {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(activity);
            showAskToStopEip = true;
            alertDialog = alertBuilder.setTitle(activity.getString(R.string.eip_cancel_connect_title))
                    .setMessage(activity.getString(R.string.eip_warning_browser_inconsistency))
                    .setPositiveButton((android.R.string.yes), (dialog, which) -> stopEipIfPossible())
                    .setNegativeButton(activity.getString(android.R.string.no), (dialog, which) -> {
                    }).setOnDismissListener(dialog -> showAskToStopEip = false).show();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void update(Observable observable, Object data) {
        if (observable instanceof EipStatus) {
            eipStatus = (EipStatus) observable;
            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(this::handleNewState);
            } else {
                Log.e("EipFragment", "activity is null");
            }
        } else if (observable instanceof ProviderObservable) {
            provider = ((ProviderObservable) observable).getCurrentProvider();
        }
    }

    private void handleNewState() {
        Activity activity = getActivity();
        if (activity == null) {
            Log.e(TAG, "activity is null while trying to handle new state");
            return;
        }

        //Log.d(TAG, "eip fragment eipStatus state: " + eipStatus.getState() + " - level: " + eipStatus.getLevel() + " - is reconnecting: " + eipStatus.isReconnecting());


        if (eipStatus.isConnecting() ) {
            setMainButtonEnabled(true);
            showConnectingLayout(activity);
            if (eipStatus.isReconnecting()) {
                //Log.d(TAG, "eip show reconnecting toast!");
                //showReconnectToast(activity);
            }
        } else if (eipStatus.isConnected() ) {
            mainButton.setText(activity.getString(R.string.vpn_button_turn_off));
            setMainButtonEnabled(true);
            vpnStateImage.setStateIcon(R.drawable.vpn_connected);
            vpnStateImage.stopProgress(false);
            routedText.setText(R.string.vpn_securely_routed);
            routedText.setVisibility(VISIBLE);
            vpnRoute.setVisibility(VISIBLE);
            setVpnRouteText();
            colorBackground();
        } else if(isOpenVpnRunningWithoutNetwork()){
            mainButton.setText(activity.getString(R.string.vpn_button_turn_off));
            setMainButtonEnabled(true);
            vpnStateImage.setStateIcon(R.drawable.vpn_disconnected);
            vpnStateImage.stopProgress(false);
            routedText.setText(R.string.vpn_securely_routed_no_internet);
            routedText.setVisibility(VISIBLE);
            vpnRoute.setVisibility(VISIBLE);
            setVpnRouteText();
            colorBackgroundALittle();
        } else if (eipStatus.isDisconnected() && reconnectingWithDifferentGateway()) {
            showConnectingLayout(activity);
            // showRetryToast(activity);
        } else if (eipStatus.isDisconnecting()) {
            setMainButtonEnabled(false);
            showDisconnectingLayout(activity);
        } else if (eipStatus.isBlocking()) {
            setMainButtonEnabled(true);
            vpnStateImage.setStateIcon(R.drawable.vpn_blocking);
            vpnStateImage.stopProgress(false);
            routedText.setText(getString(R.string.void_vpn_establish, getString(R.string.app_name)));
            routedText.setVisibility(VISIBLE);
            vpnRoute.setVisibility(GONE);
            colorBackgroundALittle();
        } else {
            mainButton.setText(activity.getString(R.string.vpn_button_turn_on));
            setMainButtonEnabled(true);
            vpnStateImage.setStateIcon(R.drawable.vpn_disconnected);
            vpnStateImage.stopProgress(false);
            routedText.setVisibility(GONE);
            vpnRoute.setVisibility(GONE);
            greyscaleBackground();
        }
    }

    private void showToast(Activity activity, String message, boolean vibrateLong) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast,
                activity.findViewById(R.id.custom_toast_container));

        TextView text = layout.findViewById(R.id.text);
        text.setText(message);

        Vibrator v = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrateLong) {
            v.vibrate(100);
            v.vibrate(200);
        } else {
            v.vibrate(100);
        }

        Toast toast = new Toast(activity.getApplicationContext());
        toast.setGravity(Gravity.BOTTOM, 0, convertDimensionToPx(this.getContext(), R.dimen.stdpadding));
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
    }
    private void showReconnectToast(Activity activity) {
        String message = (String.format("Retry %d of %d before the next closest gateway will be selected.", connectionRetry()+1, 5));
        showToast(activity, message, false);
    }

    private void showRetryToast(Activity activity) {
        int nClosestGateway = gatewayOrder();
        String message = String.format("Server number " + nClosestGateway + " not reachable. Trying next gateway.");
        showToast(activity, message, true );
    }

    private void showConnectingLayout(Context activity) {
        showConnectionTransitionLayout(activity, true);
    }

    private void showDisconnectingLayout(Activity activity) {
        showConnectionTransitionLayout(activity, false);
    }

    private void showConnectionTransitionLayout(Context activity, boolean isConnecting) {
        mainButton.setText(activity.getString(android.R.string.cancel));
        vpnStateImage.setStateIcon(R.drawable.vpn_connecting);
        vpnStateImage.showProgress();
        routedText.setVisibility(GONE);
        vpnRoute.setVisibility(GONE);
        if (isConnecting) {
            colorBackgroundALittle();
        } else {
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

    private void updateInvalidVpnCertificate() {
        ProviderAPICommand.execute(getContext(), UPDATE_INVALID_VPN_CERTIFICATE, provider);
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

    private void setVpnRouteText() {
        String vpnRouteString = provider.getName();
        String profileName = VpnStatus.getLastConnectedVpnName();
        if (!TextUtils.isEmpty(profileName)) {
            vpnRouteString += " (" + profileName + ")";
        }
        vpnRoute.setText(vpnRouteString);
    }

    private class EipFragmentServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mService = IOpenVPNServiceInternal.Stub.asInterface(service);
            handleNewState();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
        }
    }

    public void showDonationReminderDialog() {
        try {
            FragmentTransaction fragmentTransaction = new FragmentManagerEnhanced(
                    getActivity().getSupportFragmentManager()).removePreviousFragment(
                    DonationReminderDialog.TAG);
            DialogFragment newFragment = new DonationReminderDialog();
            newFragment.setCancelable(false);
            newFragment.show(fragmentTransaction, DonationReminderDialog.TAG);
        } catch (IllegalStateException | NullPointerException e) {
            e.printStackTrace();
        }
    }
}
