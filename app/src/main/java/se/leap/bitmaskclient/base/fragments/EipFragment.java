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
import static se.leap.bitmaskclient.eip.EipSetupObserver.reconnectingWithDifferentGateway;
import static se.leap.bitmaskclient.eip.GatewaysManager.Load.UNKNOWN;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.DOWNLOAD_GEOIP_JSON;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.UPDATE_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.USER_MESSAGE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.vectordrawable.graphics.drawable.Animatable2Compat.AnimationCallback;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import java.util.Observable;
import java.util.Observer;

import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.VpnStatus;
import de.blinkt.openvpn.core.connection.Connection;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.FragmentManagerEnhanced;
import se.leap.bitmaskclient.base.MainActivity;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.models.ProviderObservable;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.base.utils.ViewHelper;
import se.leap.bitmaskclient.base.views.LocationButton;
import se.leap.bitmaskclient.base.views.MainButton;
import se.leap.bitmaskclient.databinding.FEipBinding;
import se.leap.bitmaskclient.eip.EipCommand;
import se.leap.bitmaskclient.eip.EipStatus;
import se.leap.bitmaskclient.eip.GatewaysManager;
import se.leap.bitmaskclient.providersetup.ProviderAPICommand;
import se.leap.bitmaskclient.providersetup.ProviderListActivity;
import se.leap.bitmaskclient.providersetup.activities.CustomProviderSetupActivity;
import se.leap.bitmaskclient.providersetup.activities.LoginActivity;
import se.leap.bitmaskclient.providersetup.models.LeapSRPSession;
import se.leap.bitmaskclient.tor.TorServiceCommand;
import se.leap.bitmaskclient.tor.TorStatusObservable;

public class EipFragment extends Fragment implements Observer {

    public final static String TAG = EipFragment.class.getSimpleName();


    private SharedPreferences preferences;
    private Provider provider;

    AppCompatImageView background;
    AppCompatImageView stateView;
    MainButton mainButton;
    LocationButton locationButton;
    AppCompatTextView mainDescription;
    AppCompatTextView subDescription;

    private EipStatus.EipLevel previousEipLevel = EipStatus.EipLevel.UNKNOWN;
    private EipStatus eipStatus;
    private ProviderObservable providerObservable;
    private TorStatusObservable torStatusObservable;

    private @DrawableRes int pendingAnimationState;
    private GatewaysManager gatewaysManager;

    //---saved Instance -------
    private final String KEY_SHOW_PENDING_START_CANCELLATION = "KEY_SHOW_PENDING_START_CANCELLATION";
    private final String KEY_SHOW_ASK_TO_STOP_EIP = "KEY_SHOW_ASK_TO_STOP_EIP";
    private boolean showPendingStartCancellation = false;
    private boolean showAskToStopEip = false;
    //------------------------
    AlertDialog alertDialog;

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
        eipStatus = EipStatus.getInstance();
        providerObservable = ProviderObservable.getInstance();
        torStatusObservable = TorStatusObservable.getInstance();
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
        FEipBinding binding = FEipBinding.inflate(LayoutInflater.from(getContext()), container, false);
        background = binding.background;
        mainButton = binding.mainButton;
        locationButton = binding.gatewayLocationButton;
        locationButton.setTextColor(R.color.black800);
        mainDescription = binding.mainDescription;
        subDescription = binding.subDescription;
        stateView = binding.stateView;
        ViewHelper.setActionBarTitle(this, R.string.app_name);

        eipStatus.addObserver(this);
        torStatusObservable.addObserver(this);
        providerObservable.addObserver(this);

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

        mainButton.setOnClickListener(v -> {
            handleIcon();
        });
        return binding.getRoot();
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
        Log.d(TAG, "onResume");
        handleNewState();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        if (stateView.getDrawable() instanceof Animatable) {
            Animatable animatedDrawable = (Animatable) stateView.getDrawable();
            if (animatedDrawable.isRunning()) {
                animatedDrawable.stop();
            }
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
        Log.d(TAG, "onDestroyView");

        ViewHelper.setDefaultActivityBarColor(getActivity());
        eipStatus.deleteObserver(this);
        providerObservable.deleteObserver(this);
        torStatusObservable.deleteObserver(this);
        background = null;
        mainButton = null;
        locationButton = null;
        mainDescription = null;
        subDescription = null;
        stateView = null;
    }

    private void saveStatus(boolean restartOnBoot) {
        preferences.edit().putBoolean(EIP_RESTART_ON_BOOT, restartOnBoot).apply();
    }

    void handleIcon() {
        if (eipStatus.isVPNRunningWithoutNetwork() || eipStatus.isConnected() || eipStatus.isConnecting() || eipStatus.isUpdatingVpnCert())
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
        if (eipStatus.isVPNRunningWithoutNetwork() || eipStatus.isConnecting() || eipStatus.isUpdatingVpnCert()) {
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
                    .setPositiveButton((android.R.string.yes), (dialog, which) -> {
                        Context context = getContext();
                        if (context != null && eipStatus.isUpdatingVpnCert() &&
                                TorStatusObservable.isRunning()) {
                            TorServiceCommand.stopTorServiceAsync(context.getApplicationContext());
                        }
                        stopEipIfPossible();
                    })
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
            previousEipLevel = eipStatus.getEipLevel();
            eipStatus = (EipStatus) observable;
            handleNewStateOnMain();

        } else if (observable instanceof ProviderObservable) {
            provider = ((ProviderObservable) observable).getCurrentProvider();
        } else if (observable instanceof TorStatusObservable && EipStatus.getInstance().isUpdatingVpnCert()) {
            handleNewStateOnMain();
        }
    }

    private void handleNewStateOnMain() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(this::handleNewState);
        } else {
            Log.e("EipFragment", "activity is null");
        }
    }

    private void setActivityBarColor(@ColorRes int primaryColor, @ColorRes int secondaryColor) {
        ViewHelper.setActivityBarColor(getActivity(), primaryColor, secondaryColor, R.color.actionbar_connectivity_state_text_color_dark);
    }

    private void handleNewState() {
        Activity activity = getActivity();
        if (activity == null) {
            Log.e(TAG, "activity is null while trying to handle new state");
            return;
        }

        Log.d(TAG, "eip fragment eipStatus state: " + eipStatus.getState() + " - level: " + eipStatus.getLevel() + " - is reconnecting: " + eipStatus.isReconnecting());
        if (eipStatus.isUpdatingVpnCert()) {
            setMainButtonEnabled(true);
            String city = getPreferredCity(getContext());
            String locationName = VpnStatus.getCurrentlyConnectingVpnName() != null ?
                    VpnStatus.getCurrentlyConnectingVpnName() :
                    city == null ? getString(R.string.gateway_selection_recommended_location) : city;
            locationButton.setText(locationName);
            locationButton.setLocationLoad(UNKNOWN);
            locationButton.showBridgeIndicator(false);
            locationButton.showRecommendedIndicator(false);
            mainDescription.setText(R.string.eip_status_connecting);
            String torStatus = TorStatusObservable.getStringForCurrentStatus(getContext());
            if (!TextUtils.isEmpty(torStatus)) {
                Spannable spannable = new SpannableString(torStatus);
                spannable.setSpan(new RelativeSizeSpan(0.75f), 0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                subDescription.setText(TextUtils.concat(getString(R.string.updating_certificate_message) + "\n", spannable));
            } else {
                subDescription.setText(getString(R.string.updating_certificate_message));
            }
            background.setImageResource(R.drawable.bg_connecting);
            animateState(R.drawable.state_connecting);
            mainButton.updateState(false, true);
            setActivityBarColor(R.color.bg_connecting_top, R.color.bg_connecting_top_light_transparent);
        } else if (eipStatus.isConnecting()) {
            setMainButtonEnabled(true);
            String city = getPreferredCity(getContext());
            String locationName = VpnStatus.getCurrentlyConnectingVpnName() != null ?
                    VpnStatus.getCurrentlyConnectingVpnName() :
                    city == null ? getString(R.string.gateway_selection_recommended_location) : city;
            locationButton.setText(locationName);
            locationButton.setLocationLoad(UNKNOWN);
            locationButton.showBridgeIndicator(false);
            locationButton.showRecommendedIndicator(false);
            mainDescription.setText(R.string.eip_status_connecting);
            subDescription.setText(null);
            background.setImageResource(R.drawable.bg_connecting);
            animateState(R.drawable.state_connecting);
            mainButton.updateState(false, true);
            setActivityBarColor(R.color.bg_connecting_top, R.color.bg_connecting_top_light_transparent);
        } else if (eipStatus.isConnected()) {
            setMainButtonEnabled(true);
            mainButton.updateState(true, false);
            Connection.TransportType transportType = PreferenceHelper.getUseBridges(getContext()) ? Connection.TransportType.OBFS4 : Connection.TransportType.OPENVPN;
            locationButton.setLocationLoad(PreferenceHelper.useObfuscationPinning(getContext()) ? GatewaysManager.Load.UNKNOWN : gatewaysManager.getLoadForLocation(VpnStatus.getLastConnectedVpnName(), transportType));
            locationButton.setText(VpnStatus.getLastConnectedVpnName());
            locationButton.showBridgeIndicator(VpnStatus.isUsingBridges());
            locationButton.showRecommendedIndicator(getPreferredCity(getContext()) == null);
            mainDescription.setText(R.string.eip_status_secured);
            subDescription.setText(null);
            background.setImageResource(R.drawable.bg_connected);
            animateState(R.drawable.state_connected);
            setActivityBarColor(R.color.bg_running_top, R.color.bg_running_top_light_transparent);
        } else if(eipStatus.isVPNRunningWithoutNetwork()) {
            Log.d(TAG, "eip fragment eipStatus - isOpenVpnRunningWithoutNetwork");
            setMainButtonEnabled(true);
            mainButton.updateState(false, true);
            locationButton.setText(VpnStatus.getCurrentlyConnectingVpnName());
            locationButton.showBridgeIndicator(VpnStatus.isUsingBridges());
            locationButton.showBridgeIndicator(VpnStatus.isUsingBridges());
            locationButton.showRecommendedIndicator(getPreferredCity(getContext())== null);
            mainDescription.setText(R.string.eip_state_connected);
            subDescription.setText(R.string.eip_state_no_network);
            background.setImageResource(R.drawable.bg_connecting);
            animateState(R.drawable.state_connecting);
            setActivityBarColor(R.color.bg_connecting_top, R.color.bg_connecting_top_light_transparent);
        } else if (eipStatus.isDisconnected() && reconnectingWithDifferentGateway()) {
            setMainButtonEnabled(true);
            mainButton.updateState(false, true);
            locationButton.setText(VpnStatus.getCurrentlyConnectingVpnName());
            locationButton.setLocationLoad(UNKNOWN);
            locationButton.showBridgeIndicator(false);
            locationButton.showRecommendedIndicator(false);
            mainDescription.setText(R.string.eip_status_connecting);
            subDescription.setText(R.string.reconnecting);
            background.setImageResource(R.drawable.bg_connecting);
            animateState(R.drawable.state_connecting);
            setActivityBarColor(R.color.bg_connecting_top, R.color.bg_connecting_top_light_transparent);
        } else if (eipStatus.isDisconnecting()) {
            setMainButtonEnabled(false);
            mainButton.updateState(false, false);
            mainDescription.setText(R.string.eip_status_unsecured);
            background.setImageResource(R.drawable.bg_disconnected);
            if (previousEipLevel == EipStatus.EipLevel.CONNECTED) {
                animateState(R.drawable.state_transition_connected_disconnected);
            } else {
                animateState(R.drawable.state_disconnected);
            }
            setActivityBarColor(R.color.bg_disconnected_top, R.color.bg_disconnected_top_light_transparent);
        } else if (eipStatus.isBlocking()) {
            setMainButtonEnabled(true);
            mainButton.updateState(false, true);
            locationButton.setText(R.string.no_location);
            locationButton.setLocationLoad(UNKNOWN);
            locationButton.showBridgeIndicator(false);
            locationButton.showRecommendedIndicator(false);
            mainDescription.setText(R.string.eip_state_connected);
            subDescription.setText(getString(R.string.eip_state_blocking, getString(R.string.app_name)));
            background.setImageResource(R.drawable.bg_connecting);
            animateState(R.drawable.state_connecting);
            setActivityBarColor(R.color.bg_connecting_top, R.color.bg_connecting_top_light_transparent);
        } else {
            locationButton.setText(R.string.vpn_button_turn_on);
            setMainButtonEnabled(true);
            mainButton.updateState(false, false);
            locationButton.setLocationLoad(UNKNOWN);
            locationButton.showBridgeIndicator(false);
            String city = getPreferredCity(getContext());
            locationButton.setText(city == null ? getString(R.string.gateway_selection_recommended_location) : city);
            locationButton.showRecommendedIndicator(false);
            mainDescription.setText(R.string.eip_status_unsecured);
            subDescription.setText(null);
            background.setImageResource(R.drawable.bg_disconnected);

            animateState(R.drawable.state_disconnected);
            setActivityBarColor(R.color.bg_disconnected_top, R.color.bg_disconnected_top_light_transparent);
        }
    }

    private void animateState(@DrawableRes int drawableRes) {
        @DrawableRes int lastDrawableId;
        try {
            lastDrawableId = (int) stateView.getTag();
            if (lastDrawableId == drawableRes) {
                return;
            }

            Drawable lastDrawable = ContextCompat.getDrawable(getContext(), lastDrawableId);
            if (lastDrawable instanceof  Animatable && ((Animatable) lastDrawable).isRunning()) {
                pendingAnimationState = drawableRes;
            }

        } catch (NullPointerException | ClassCastException e) {
            // eat me
        }


        stateView.setImageResource(drawableRes);
        stateView.setTag(drawableRes);
        if (stateView.getDrawable() instanceof Animatable) {
            Animatable animatedDrawable = (Animatable) stateView.getDrawable();
            AnimatedVectorDrawableCompat.registerAnimationCallback(stateView.getDrawable(), new AnimationCallback() {
                @Override
                public void onAnimationEnd(Drawable drawable) {
                    super.onAnimationEnd(drawable);
                    if (!isResumed()) {
                        return;
                    }
                    if (pendingAnimationState != 0) {
                        int newAnimationRes = pendingAnimationState;
                        pendingAnimationState = 0;
                        animateState(newAnimationRes);
                    } else if (drawable instanceof Animatable){
                        ((Animatable) drawable).start();
                    }
                }
            });
            animatedDrawable.start();
        }
    }

    private void updateInvalidVpnCertificate() {
        eipStatus.setUpdatingVpnCert(true);
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
