/**
 * Copyright (c) 2020 LEAP Encryption Access Project and contributers
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

package se.leap.bitmaskclient.eip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONObject;

import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import de.blinkt.openvpn.LaunchVPN;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.LogItem;
import de.blinkt.openvpn.core.VpnStatus;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.providersetup.ProviderAPI;
import se.leap.bitmaskclient.providersetup.ProviderAPICommand;
import se.leap.bitmaskclient.base.models.ProviderObservable;
import se.leap.bitmaskclient.appUpdate.DownloadServiceCommand;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;

import static android.app.Activity.RESULT_CANCELED;
import static android.content.Intent.CATEGORY_DEFAULT;
import static de.blinkt.openvpn.core.ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET;
import static de.blinkt.openvpn.core.ConnectionStatus.LEVEL_NOTCONNECTED;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_EIP_EVENT;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_GATEWAY_SETUP_OBSERVER_EVENT;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_PROVIDER_API_EVENT;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_CODE;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_KEY;
import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_LAUNCH_VPN;
import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_PREPARE_VPN;
import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_START;
import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_START_ALWAYS_ON_VPN;
import static se.leap.bitmaskclient.base.models.Constants.EIP_EARLY_ROUTES;
import static se.leap.bitmaskclient.base.models.Constants.EIP_N_CLOSEST_GATEWAY;
import static se.leap.bitmaskclient.base.models.Constants.EIP_REQUEST;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_PROFILE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.CORRECTLY_DOWNLOADED_EIP_SERVICE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.CORRECTLY_DOWNLOADED_GEOIP_JSON;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.CORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INCORRECTLY_DOWNLOADED_GEOIP_JSON;
import static se.leap.bitmaskclient.appUpdate.DownloadServiceCommand.CHECK_VERSION_FILE;

/**
 * Created by cyberta on 05.12.18.
 */
public class EipSetupObserver extends BroadcastReceiver implements VpnStatus.StateListener, VpnStatus.LogListener {

    private static final String TAG = EipSetupObserver.class.getName();

    private static final int UPDATE_CHECK_TIMEOUT = 1000*60*60*24*7;
    private Context context;
    private VpnProfile setupVpnProfile;
    private String observedProfileFromVpnStatus;
    AtomicBoolean changingGateway = new AtomicBoolean(false);
    AtomicInteger setupNClosestGateway = new AtomicInteger();
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
        VpnStatus.addLogListener(this);
    }

    public static void init(Context context, SharedPreferences preferences) {
        if (instance == null) {
            instance = new EipSetupObserver(context, preferences);
        }
    }

    public static boolean reconnectingWithDifferentGateway() {
        return instance.setupNClosestGateway.get() > 0;
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
                Log.d(TAG, "correctly updated service json");
                provider = resultData.getParcelable(PROVIDER_KEY);
                ProviderObservable.getInstance().updateProvider(provider);
                PreferenceHelper.storeProviderInPreferences(preferences, provider);
                if (EipStatus.getInstance().isDisconnected()) {
                    EipCommand.startVPN(context.getApplicationContext(), false);
                }
                break;
            case CORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE:
                provider = resultData.getParcelable(PROVIDER_KEY);
                ProviderObservable.getInstance().updateProvider(provider);
                PreferenceHelper.storeProviderInPreferences(preferences, provider);
                EipCommand.startVPN(context.getApplicationContext(), false);
                break;
            case CORRECTLY_DOWNLOADED_GEOIP_JSON:
                provider = resultData.getParcelable(PROVIDER_KEY);
                ProviderObservable.getInstance().updateProvider(provider);
                PreferenceHelper.storeProviderInPreferences(preferences, provider);
                maybeStartEipService(resultData);
                break;
            case INCORRECTLY_DOWNLOADED_GEOIP_JSON:
                maybeStartEipService(resultData);
                break;
            default:
                break;
        }

        for (EipSetupListener listener : listeners) {
            listener.handleProviderApiEvent(intent);
        }
    }

    private void maybeStartEipService(Bundle resultData) {
        if (resultData.getBoolean(EIP_ACTION_START)) {
            boolean earlyRoutes = resultData.getBoolean(EIP_EARLY_ROUTES);
            EipCommand.startVPN(context.getApplicationContext(), earlyRoutes);
        }
    }


    private void handleEipEvent(Intent intent) {
        int resultCode = intent.getIntExtra(BROADCAST_RESULT_CODE, RESULT_CANCELED);
        Bundle result = intent.getBundleExtra(BROADCAST_RESULT_KEY);
        String eipRequest = result.getString(EIP_REQUEST);
        EIP.EIPErrors error = EIP.EIPErrors.UNKNOWN;
        try {
            JSONObject jsonObject = new JSONObject(result.getString(EIP.ERRORS));
            error = EIP.EIPErrors.valueOf(jsonObject.getString(EIP.ERRORID));
        } catch (Exception e) {
            //ignore
        }
        if (eipRequest == null) {
            return;
        }
        switch (eipRequest) {
            case EIP_ACTION_START:
            case EIP_ACTION_START_ALWAYS_ON_VPN:
                if (resultCode == RESULT_CANCELED) {
                    //setup failed
                    switch (error) {
                        case NO_MORE_GATEWAYS:
                            finishGatewaySetup(false);
                            EipCommand.startBlockingVPN(context.getApplicationContext());
                            break;
                        case ERROR_INVALID_PROFILE:
                            selectNextGateway();
                            break;
                        default:
                            finishGatewaySetup(false);
                            EipCommand.stopVPN(context);
                            EipStatus.refresh();
                    }
                }
                break;
            case EIP_ACTION_PREPARE_VPN:
                if (resultCode == RESULT_CANCELED) {
                    VpnStatus.logError("Error preparing VpnService.");
                    finishGatewaySetup(false);
                    EipStatus.refresh();
                }
                break;
            case EIP_ACTION_LAUNCH_VPN:
                if (resultCode == RESULT_CANCELED) {
                    VpnStatus.logError("Error starting VpnService.");
                    finishGatewaySetup(false);
                    EipStatus.refresh();
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

        VpnProfile vpnProfile = (VpnProfile) event.getSerializableExtra(PROVIDER_PROFILE);
        if (vpnProfile == null) {
            Log.e(TAG, "Tried to setup non existing vpn profile.");
            return;
        }
        setupVpnProfile = vpnProfile;
        setupNClosestGateway.set(event.getIntExtra(EIP_N_CLOSEST_GATEWAY, 0));
        Log.d(TAG, "bitmaskapp add state listener");
        VpnStatus.addStateListener(this);
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
            Log.e(TAG, "Timeout reached! Try next gateway!");
            VpnStatus.logError("Timeout reached! Try next gateway!");
            selectNextGateway();
        } else if ("NOPROCESS".equals(state) && LEVEL_NOTCONNECTED == level) {
            //??
        } else if ("CONNECTED".equals(state)) {
            //saveLastProfile(context.getApplicationContext(), setupVpnProfile.getUUIDString());
            Provider provider = ProviderObservable.getInstance().getCurrentProvider();
            if (setupNClosestGateway.get() > 0 || provider.shouldUpdateEipServiceJson()) {
                //setupNClostestGateway > 0: at least one failed gateway -> did the provider change it's gateways?
                ProviderAPICommand.execute(context, ProviderAPI.DOWNLOAD_SERVICE_JSON, provider);
            }

            if (shouldCheckAppUpdate()) {
                DownloadServiceCommand.execute(context, CHECK_VERSION_FILE);
            }
            finishGatewaySetup(false);
        } else if ("TCP_CONNECT".equals(state)) {
            changingGateway.set(false);
        }
    }

    private boolean shouldCheckAppUpdate() {
        return System.currentTimeMillis() - PreferenceHelper.getLastAppUpdateCheck(context) >= UPDATE_CHECK_TIMEOUT;
    }

    private void selectNextGateway() {
        changingGateway.set(true);
        EipCommand.startVPN(context.getApplicationContext(), false, setupNClosestGateway.get() + 1);
    }

    private void finishGatewaySetup(boolean changingGateway) {
        VpnStatus.removeStateListener(this);
        setupVpnProfile = null;
        setupNClosestGateway.set(0);
        observedProfileFromVpnStatus = null;
        this.changingGateway.set(changingGateway);
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

    @Override
    public void newLog(LogItem logItem) {
        if (logItem.getLogLevel() == VpnStatus.LogLevel.ERROR) {
            switch (logItem.getErrorType()) {
                case SHAPESHIFTER:
                    VpnProfile profile = VpnStatus.getLastConnectedVpnProfile();
                    if (profile == null) {
                        EipCommand.startVPN(context.getApplicationContext(), false, 0);
                    } else {
                        GatewaysManager gatewaysManager = new GatewaysManager(context.getApplicationContext());
                        int position = gatewaysManager.getPosition(profile);
                        setupNClosestGateway.set(position >= 0 ? position : 0);
                        selectNextGateway();
                    }
                    break;
                default:
                    break;

            }
        }
    }
}
