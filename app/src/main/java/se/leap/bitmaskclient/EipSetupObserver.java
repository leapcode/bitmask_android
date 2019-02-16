package se.leap.bitmaskclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import de.blinkt.openvpn.LaunchVPN;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.VpnStatus;
import se.leap.bitmaskclient.eip.EipCommand;
import se.leap.bitmaskclient.eip.EipStatus;
import se.leap.bitmaskclient.eip.Gateway;
import se.leap.bitmaskclient.utils.PreferenceHelper;

import static android.app.Activity.RESULT_CANCELED;
import static android.content.Intent.CATEGORY_DEFAULT;
import static de.blinkt.openvpn.core.ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET;
import static de.blinkt.openvpn.core.ConnectionStatus.LEVEL_NOTCONNECTED;
import static se.leap.bitmaskclient.Constants.BROADCAST_EIP_EVENT;
import static se.leap.bitmaskclient.Constants.BROADCAST_GATEWAY_SETUP_OBSERVER_EVENT;
import static se.leap.bitmaskclient.Constants.BROADCAST_PROVIDER_API_EVENT;
import static se.leap.bitmaskclient.Constants.BROADCAST_RESULT_CODE;
import static se.leap.bitmaskclient.Constants.BROADCAST_RESULT_KEY;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_START;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_START_ALWAYS_ON_VPN;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_STOP;
import static se.leap.bitmaskclient.Constants.EIP_REQUEST;
import static se.leap.bitmaskclient.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.Constants.SHARED_PREFERENCES;
import static se.leap.bitmaskclient.ProviderAPI.CORRECTLY_DOWNLOADED_EIP_SERVICE;
import static se.leap.bitmaskclient.ProviderAPI.CORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.utils.PreferenceHelper.getSavedProviderFromSharedPreferences;

/**
 * Created by cyberta on 05.12.18.
 */
class EipSetupObserver extends BroadcastReceiver implements VpnStatus.StateListener {

    private static final String TAG = EipSetupObserver.class.getName();

    //The real timout is 4*2s + 1*4s + 1*8s + 1*16s + 1*32s + 1*64s = 132 s;
    private static final String TIMEOUT = "4";
    private Context context;
    private VpnProfile setupVpnProfile;
    private String observedProfileFromVpnStatus;
    AtomicBoolean changingGateway = new AtomicBoolean(false);
    AtomicInteger setupNClosestGateway = new AtomicInteger();
    AtomicInteger reconnectTry = new AtomicInteger();
    private Vector<EipSetupListener> listeners = new Vector<>();
    private SharedPreferences preferences;
    private static EipSetupObserver instance;

    private EipSetupObserver(Context context, SharedPreferences preferences) {
        this.context = context;
        this.preferences = preferences;
        IntentFilter updateIntentFilter = new IntentFilter(BROADCAST_GATEWAY_SETUP_OBSERVER_EVENT);
        updateIntentFilter.addAction(BROADCAST_EIP_EVENT);
        updateIntentFilter.addAction(BROADCAST_PROVIDER_API_EVENT);
        updateIntentFilter.addCategory(CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(context.getApplicationContext()).registerReceiver(this, updateIntentFilter);
        instance = this;
    }

    public static void init(Context context, SharedPreferences preferences) {
        if (instance == null) {
            instance = new EipSetupObserver(context, preferences);
        }
    }

    public static boolean reconnectingWithDifferentGateway() {
        return instance.setupNClosestGateway.get() > 0;
    }

    public static int connectionRetry() {
        return instance.reconnectTry.get();
    }

    public static int gatewayOrder() {
        return instance.setupNClosestGateway.get();
    }

    public static synchronized void addListener(EipSetupListener listener) {
        if (instance.listeners.contains(listener)) {
            return;
        }
        instance.listeners.add(listener);
    }

    public static synchronized void removeListener(EipSetupListener listener) {
        instance.listeners.remove(listener);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }

        switch (action) {
            case BROADCAST_GATEWAY_SETUP_OBSERVER_EVENT:
                handleGatewaySetupObserverEvent(intent);
                break;
            case BROADCAST_EIP_EVENT:
                handleEipEvent(intent);
                break;
            case BROADCAST_PROVIDER_API_EVENT:
                handleProviderApiEvent(intent);
                break;
            default:
                break;
        }
    }

    private void handleProviderApiEvent(Intent intent) {
        int resultCode = intent.getIntExtra(BROADCAST_RESULT_CODE, RESULT_CANCELED);
        Bundle resultData = intent.getParcelableExtra(BROADCAST_RESULT_KEY);
        if (resultData == null) {
            resultData = Bundle.EMPTY;
        }

        Provider provider;
        switch (resultCode) {
            case CORRECTLY_DOWNLOADED_EIP_SERVICE:
                provider = resultData.getParcelable(PROVIDER_KEY);
                ProviderObservable.getInstance().updateProvider(provider);
                PreferenceHelper.storeProviderInPreferences(preferences, provider);
                if (EipStatus.getInstance().isDisconnected()) {
                    EipCommand.startVPN(context.getApplicationContext(), true);
                }
                break;
            case CORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE:
                provider = resultData.getParcelable(PROVIDER_KEY);
                ProviderObservable.getInstance().updateProvider(provider);
                EipCommand.startVPN(context.getApplicationContext(), true);
                break;
            default:
                break;
        }

        for (EipSetupListener listener : listeners) {
            listener.handleProviderApiEvent(intent);
        }
    }


    private void handleEipEvent(Intent intent) {
        int resultCode = intent.getIntExtra(BROADCAST_RESULT_CODE, RESULT_CANCELED);
        Bundle result = intent.getBundleExtra(BROADCAST_RESULT_KEY);
        String eipRequest = result.getString(EIP_REQUEST);
        if (eipRequest == null) {
            return;
        }
        switch (eipRequest) {
            case EIP_ACTION_START:
            case EIP_ACTION_START_ALWAYS_ON_VPN:
                if (resultCode == RESULT_CANCELED) {
                    //setup failed
                    finishGatewaySetup(false);
                }
                break;
            default:
                break;
        }

        for (EipSetupListener listener : listeners) {
            listener.handleEipEvent(intent);
        }

    }

    private void handleGatewaySetupObserverEvent(Intent event) {
        if (observedProfileFromVpnStatus != null || setupVpnProfile != null) {
            //finish last setup observation
            Log.d(TAG, "finish last gateway setup");
            finishGatewaySetup(true);
        }

        VpnProfile vpnProfile = (VpnProfile) event.getSerializableExtra(LaunchVPN.EXTRA_TEMP_VPN_PROFILE);
        if (vpnProfile == null) {
            Log.e(TAG, "Tried to setup non existing vpn profile.");
            return;
        }
        setupVpnProfile = vpnProfile;
        setupNClosestGateway.set(event.getIntExtra(Gateway.KEY_N_CLOSEST_GATEWAY, 0));
        Log.d(TAG, "bitmaskapp add state listener");
        VpnStatus.addStateListener(this);

        launchVPN(setupVpnProfile);
    }

    private void launchVPN(VpnProfile vpnProfile) {
        Intent intent = new Intent(context.getApplicationContext(), LaunchVPN.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(LaunchVPN.EXTRA_HIDELOG, true);
        intent.putExtra(LaunchVPN.EXTRA_TEMP_VPN_PROFILE, vpnProfile);
        intent.putExtra(Gateway.KEY_N_CLOSEST_GATEWAY, setupNClosestGateway.get());
        context.startActivity(intent);
    }

    @Override
    public void updateState(String state, String logmessage, int localizedResId, ConnectionStatus level) {
        // VpnStatus.updateStateString("NOPROCESS", "No process running.", R.string.state_noprocess, ConnectionStatus.LEVEL_NOTCONNECTED);

        Log.d(TAG, "vpn status: " + state + " - " + logmessage + " - " + level);
        if (observedProfileFromVpnStatus == null ||
                setupVpnProfile == null) {
            return;
        }
        if (!observedProfileFromVpnStatus.equals(setupVpnProfile.getUUIDString())) {
            Log.d(TAG, "vpn profile to setup and observed profile currently is used differ: " + setupVpnProfile.getUUIDString() + " vs. " + observedProfileFromVpnStatus);
            return;
        }

        if (ConnectionStatus.LEVEL_STOPPING == level) {
            finishGatewaySetup(false);
        } else if ("CONNECTRETRY".equals(state) && LEVEL_CONNECTING_NO_SERVER_REPLY_YET.equals(level)) {
            Log.d(TAG, "trying gateway: " + setupVpnProfile.getName());
            if (TIMEOUT.equals(logmessage)) {
                Log.e(TAG, "Timeout reached! Try next gateway!");
                VpnStatus.logError("Timeout reached! Try next gateway!");
                selectNextGateway();
                return;
            }
            int current = reconnectTry.get();
            reconnectTry.set(current + 1);
        } else if ("NOPROCESS".equals(state) && LEVEL_NOTCONNECTED == level) {
            //??
        } else if ("CONNECTED".equals(state)) {
            //saveLastProfile(context.getApplicationContext(), setupVpnProfile.getUUIDString());
            if (setupNClosestGateway.get() > 0) {
                //at least one failed gateway -> did the provider change it's gateways?
                SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
                Provider provider = getSavedProviderFromSharedPreferences(preferences);
                ProviderAPICommand.execute(context, ProviderAPI.DOWNLOAD_SERVICE_JSON, provider);
            }
            finishGatewaySetup(false);
        } else if ("TCP_CONNECT".equals(state)) {
            changingGateway.set(false);
        }
    }


    private void selectNextGateway() {
        changingGateway.set(true);
        reconnectTry.set(0);
        EipCommand.startVPN(context.getApplicationContext(), false, setupNClosestGateway.get() + 1);
    }

    private void finishGatewaySetup(boolean changingGateway) {
        VpnStatus.removeStateListener(this);
        setupVpnProfile = null;
        setupNClosestGateway.set(0);
        observedProfileFromVpnStatus = null;
        this.changingGateway.set(changingGateway);
        this.reconnectTry.set(0);
    }

    /**
     * gets called as soon as a new VPN is about to launch
     *
     * @param uuid
     */
    @Override
    public void setConnectedVPN(String uuid) {
        observedProfileFromVpnStatus = uuid;
    }
}
