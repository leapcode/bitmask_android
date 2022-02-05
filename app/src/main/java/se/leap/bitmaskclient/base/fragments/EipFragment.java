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
import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.IOpenVPNServiceInternal;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.VpnStatus;
import de.blinkt.openvpn.core.connection.Connection;
import se.leap.bitmaskclient.BuildConfig;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.FragmentManagerEnhanced;
import se.leap.bitmaskclient.base.MainActivity;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.models.ProviderObservable;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.base.views.LocationButton;
import se.leap.bitmaskclient.base.views.MainButton;
import se.leap.bitmaskclient.eip.EipCommand;
import se.leap.bitmaskclient.eip.EipStatus;
import se.leap.bitmaskclient.eip.GatewaysManager;
import se.leap.bitmaskclient.providersetup.ProviderAPICommand;
import se.leap.bitmaskclient.providersetup.ProviderListActivity;
import se.leap.bitmaskclient.providersetup.activities.CustomProviderSetupActivity;
import se.leap.bitmaskclient.providersetup.activities.LoginActivity;
import se.leap.bitmaskclient.providersetup.models.LeapSRPSession;

import static android.view.View.INVISIBLE;
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
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getPreferredCity;
import static se.leap.bitmaskclient.base.utils.ViewHelper.convertDimensionToPx;
import static se.leap.bitmaskclient.eip.EipSetupObserver.gatewayOrder;
import static se.leap.bitmaskclient.eip.EipSetupObserver.reconnectingWithDifferentGateway;
import static se.leap.bitmaskclient.eip.GatewaysManager.Load.UNKNOWN;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.DOWNLOAD_GEOIP_JSON;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.UPDATE_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.USER_MESSAGE;

public class EipFragment extends Fragment implements Observer {

    public final static String TAG = EipFragment.class.getSimpleName();


    private SharedPreferences preferences;
    private Provider provider;

    @BindView(R.id.background)
    AppCompatImageView background;

    @BindView(R.id.main_button)
    MainButton mainButton;

    @BindView(R.id.gateway_location_button)
    LocationButton locationButton;

    @BindView(R.id.main_description)
    AppCompatTextView mainDescription;

    @BindView(R.id.sub_description)
    AppCompatTextView subDescription;

    private Unbinder unbinder;
    private EipStatus eipStatus;

    private GatewaysManager gatewaysManager;

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

        gatewaysManager = new GatewaysManager(getContext());


    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        eipStatus.addObserver(this);
        View view = inflater.inflate(R.layout.f_eip, container, false);
        unbinder = ButterKnife.bind(this, view);

        try {
            Bundle arguments = getArguments();
            if (arguments != null && arguments.containsKey(ASK_TO_CANCEL_VPN) && arguments.getBoolean(ASK_TO_CANCEL_VPN)) {
                arguments.remove(ASK_TO_CANCEL_VPN);
                setArguments(arguments);
                askToStopEIP();
            }
        } catch (IllegalStateException e) {
            // probably setArguments failed because the fragments state is already saved
            e.printStackTrace();
        }

        restoreFromSavedInstance(savedInstanceState);
        locationButton.setOnClickListener(v -> {
                FragmentManagerEnhanced fragmentManager = new FragmentManagerEnhanced(getActivity().getSupportFragmentManager());
                Fragment fragment = new GatewaySelectionFragment();
                fragmentManager.replace(R.id.main_container, fragment, MainActivity.TAG);
        });
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

    @OnClick(R.id.main_button)
    void onButtonClick() {
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
        locationButton.setEnabled(enabled);
        mainButton.setEnabled(enabled);
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
            ProviderAPICommand.execute(context, DOWNLOAD_GEOIP_JSON, bundle, provider);
        } else {
            EipCommand.startVPN(context, false);
        }
        EipStatus.getInstance().updateState("UI_CONNECTING", "", 0, ConnectionStatus.LEVEL_START);
    }

    protected void stopEipIfPossible() {
        Context context = getContext();
        if (context == null) {
            Log.e(TAG, "context is null when trying to stop EIP");
            return;
        }
        EipCommand.stopVPN(context);
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

        Log.d(TAG, "eip fragment eipStatus state: " + eipStatus.getState() + " - level: " + eipStatus.getLevel() + " - is reconnecting: " + eipStatus.isReconnecting());
        if (eipStatus.isConnecting()) {
            setMainButtonEnabled(true);
            showConnectionTransitionLayout(true);
            locationButton.setText(getString(R.string.eip_status_start_pending));
            locationButton.setLocationLoad(UNKNOWN);
            locationButton.showBridgeIndicator(false);
            locationButton.showRecommendedIndicator(false);
            mainDescription.setText(null);
            subDescription.setText(null);
        } else if (eipStatus.isConnected()) {
            setMainButtonEnabled(true);
            mainButton.updateState(true, false, false);
            Connection.TransportType transportType = PreferenceHelper.getUseBridges(getContext()) ? Connection.TransportType.OBFS4 : Connection.TransportType.OPENVPN;
            locationButton.setLocationLoad(gatewaysManager.getLoadForLocation(VpnStatus.getLastConnectedVpnName(), transportType));
            locationButton.setText(VpnStatus.getLastConnectedVpnName());
            locationButton.showBridgeIndicator(VpnStatus.isUsingBridges());
            locationButton.showRecommendedIndicator(getPreferredCity(getContext())== null);
            mainDescription.setText(R.string.eip_state_connected);
            subDescription.setText(null);
            colorBackground();
        } else if(isOpenVpnRunningWithoutNetwork()) {
            Log.d(TAG, "eip fragment eipStatus - isOpenVpnRunningWithoutNetwork");
            setMainButtonEnabled(true);
            mainButton.updateState(true, false, true);
            locationButton.setText(VpnStatus.getCurrentlyConnectingVpnName());
            locationButton.showBridgeIndicator(VpnStatus.isUsingBridges());
            locationButton.showBridgeIndicator(VpnStatus.isUsingBridges());
            locationButton.showRecommendedIndicator(getPreferredCity(getContext())== null);
            colorBackgroundALittle();
            mainDescription.setText(R.string.eip_state_connected);
            subDescription.setText(R.string.eip_state_no_network);
        } else if (eipStatus.isDisconnected() && reconnectingWithDifferentGateway()) {
            showConnectionTransitionLayout(true);
            // showRetryToast(activity);
            locationButton.setText(getString(R.string.eip_status_start_pending));
            locationButton.setLocationLoad(UNKNOWN);
            locationButton.showBridgeIndicator(false);
            locationButton.showRecommendedIndicator(false);
            mainDescription.setText(null);
            subDescription.setText(R.string.reconnecting);
        } else if (eipStatus.isDisconnecting()) {
            setMainButtonEnabled(false);
            showConnectionTransitionLayout(false);
            mainDescription.setText(R.string.eip_state_insecure);
        } else if (eipStatus.isBlocking()) {
            setMainButtonEnabled(true);
            mainButton.updateState(true, false, true);
            colorBackgroundALittle();
            locationButton.setText(getString(R.string.no_location));
            locationButton.setLocationLoad(UNKNOWN);
            locationButton.showBridgeIndicator(false);
            locationButton.showRecommendedIndicator(false);
            mainDescription.setText(R.string.eip_state_connected);
            subDescription.setText(getString(R.string.eip_state_blocking, getString(R.string.app_name)));
        } else {
            locationButton.setText(activity.getString(R.string.vpn_button_turn_on));
            setMainButtonEnabled(true);
            mainButton.updateState(false, false, false);
            greyscaleBackground();
            locationButton.setLocationLoad(UNKNOWN);
            locationButton.showBridgeIndicator(false);
            String city = getPreferredCity(getContext());
            locationButton.setText(city == null ? getString(R.string.gateway_selection_recommended_location) : city);
            locationButton.showRecommendedIndicator(false);
            mainDescription.setText(R.string.eip_state_insecure);
            subDescription.setText(R.string.connection_not_connected);
        }
    }

    private void showToast(Activity activity, String message, boolean vibrateLong) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast,
                activity.findViewById(R.id.custom_toast_container));

        AppCompatTextView text = layout.findViewById(R.id.text);
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

    private void showRetryToast(Activity activity) {
        int nClosestGateway = gatewayOrder();
        String message = String.format("Server number " + nClosestGateway + " not reachable. Trying next gateway.");
        showToast(activity, message, true );
    }

    private void showConnectionTransitionLayout(boolean isConnecting) {
        mainButton.updateState(true, isConnecting, false);
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
        if (BuildConfig.use_color_filter) {
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0);
            ColorMatrixColorFilter cf = new ColorMatrixColorFilter(matrix);
            background.setColorFilter(cf);
            background.setImageAlpha(255);
        }
    }

    private void colorBackgroundALittle() {
        if (BuildConfig.use_color_filter) {
            background.setColorFilter(null);
            background.setImageAlpha(144);
        }
    }

    private void colorBackground() {
        if (BuildConfig.use_color_filter) {
            background.setColorFilter(null);
            background.setImageAlpha(210);
        }
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
