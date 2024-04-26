/**
 * Copyright (c) 2021 LEAP Encryption Access Project and contributers
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

import static android.app.Activity.RESULT_CANCELED;
import static android.content.Intent.CATEGORY_DEFAULT;
import static de.blinkt.openvpn.core.ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET;
import static de.blinkt.openvpn.core.ConnectionStatus.LEVEL_NOTCONNECTED;
import static se.leap.bitmaskclient.appUpdate.DownloadServiceCommand.CHECK_VERSION_FILE;
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
import static se.leap.bitmaskclient.providersetup.ProviderAPI.CORRECTLY_DOWNLOADED_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.CORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.DELAY;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.DOWNLOAD_MOTD;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INCORRECTLY_DOWNLOADED_EIP_SERVICE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INCORRECTLY_DOWNLOADED_GEOIP_JSON;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INCORRECTLY_DOWNLOADED_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INCORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.PROVIDER_NOK;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.PROVIDER_OK;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.QUIETLY_UPDATE_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.TOR_EXCEPTION;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.TOR_TIMEOUT;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.UPDATE_INVALID_VPN_CERTIFICATE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONObject;
import org.torproject.jni.TorService;

import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.LogItem;
import de.blinkt.openvpn.core.VpnStatus;
import se.leap.bitmaskclient.BuildConfig;
import se.leap.bitmaskclient.appUpdate.DownloadServiceCommand;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.models.ProviderObservable;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.providersetup.ProviderAPI;
import se.leap.bitmaskclient.providersetup.ProviderAPICommand;
import se.leap.bitmaskclient.providersetup.ProviderSetupObservable;
import se.leap.bitmaskclient.tor.TorServiceCommand;
import se.leap.bitmaskclient.tor.TorStatusObservable;

/**
 * Created by cyberta on 05.12.18.
 */
public class EipSetupObserver extends BroadcastReceiver implements VpnStatus.StateListener, VpnStatus.LogListener {

    private static final String TAG = EipSetupObserver.class.getName();

    private static final int UPDATE_CHECK_TIMEOUT = 1000*60*60*24*7;
    private final Context appContext;
    private VpnProfile setupVpnProfile;
    private String observedProfileFromVpnStatus;
    AtomicInteger reconnectTry = new AtomicInteger();
    AtomicBoolean changingGateway = new AtomicBoolean(false);

    AtomicBoolean activityForeground = new AtomicBoolean(false);
    AtomicInteger setupNClosestGateway = new AtomicInteger();
    private Vector<EipSetupListener> listeners = new Vector<>();
    private static EipSetupObserver instance;

    private EipSetupObserver(Context context) {
        this.appContext = context.getApplicationContext();
        IntentFilter updateIntentFilter = new IntentFilter(BROADCAST_GATEWAY_SETUP_OBSERVER_EVENT);
        updateIntentFilter.addAction(BROADCAST_EIP_EVENT);
        updateIntentFilter.addAction(BROADCAST_PROVIDER_API_EVENT);
        updateIntentFilter.addAction(TorService.ACTION_STATUS);
        updateIntentFilter.addAction(TorService.ACTION_ERROR);
        updateIntentFilter.addCategory(CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(context).registerReceiver(this, updateIntentFilter);
        instance = this;
        VpnStatus.addLogListener(this);
    }

    public static void init(Context context) {
        if (instance == null) {
            instance = new EipSetupObserver(context);
        }
    }

    public static boolean reconnectingWithDifferentGateway() {
        return instance.setupNClosestGateway.get() > 0;
    }

    public static int gatewayOrder() {
        return instance.setupNClosestGateway.get();
    }

    public static void setActivityForeground(boolean isForeground) {
        instance.activityForeground.set(isForeground);
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
            case TorService.ACTION_STATUS:
                handleTorStatusEvent(intent);
                break;
            case TorService.ACTION_ERROR:
                handleTorErrorEvent(intent);
                break;
            default:
                break;
        }
    }

    private void handleTorErrorEvent(Intent intent) {
        String error = intent.getStringExtra(Intent.EXTRA_TEXT);
        Log.d(TAG, "handle Tor error event: " + error);
        TorStatusObservable.setLastError(error);
    }

    private void handleTorStatusEvent(Intent intent) {
        String status = intent.getStringExtra(TorService.EXTRA_STATUS);
        Log.d(TAG, "handle Tor status event: " + status);
        Integer bootstrap = intent.getIntExtra(TorService.EXTRA_STATUS_DETAIL_BOOTSTRAP, -1);
        String logKey = intent.getStringExtra(TorService.EXTRA_STATUS_DETAIL_LOGKEY);
        TorStatusObservable.updateState(appContext, status, bootstrap, logKey);
        ProviderSetupObservable.updateTorSetupProgress();
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
                PreferenceHelper.storeProviderInPreferences(provider);
                if (EipStatus.getInstance().isDisconnected()) {
                    EipCommand.startVPN(appContext, false);
                }
                break;
            case CORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE:
                provider = resultData.getParcelable(PROVIDER_KEY);
                ProviderObservable.getInstance().updateProvider(provider);
                PreferenceHelper.storeProviderInPreferences(provider);
                EipCommand.startVPN(appContext, false);
                EipStatus.getInstance().setUpdatingVpnCert(false);
                if (TorStatusObservable.isRunning()) {
                    TorServiceCommand.stopTorServiceAsync(appContext);
                }
                break;
            case CORRECTLY_DOWNLOADED_GEOIP_JSON:
                provider = resultData.getParcelable(PROVIDER_KEY);
                ProviderObservable.getInstance().updateProvider(provider);
                PreferenceHelper.storeProviderInPreferences(provider);
                maybeStartEipService(resultData);
                break;
            case INCORRECTLY_DOWNLOADED_GEOIP_JSON:
                maybeStartEipService(resultData);
                break;
            case INCORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE:
                EipStatus.getInstance().setUpdatingVpnCert(false);
                if (TorStatusObservable.isRunning()) {
                    TorServiceCommand.stopTorServiceAsync(appContext);
                }
                break;
            case PROVIDER_NOK:
            case INCORRECTLY_DOWNLOADED_EIP_SERVICE:
            case INCORRECTLY_DOWNLOADED_VPN_CERTIFICATE:
                if (TorStatusObservable.isRunning()) {
                    TorServiceCommand.stopTorServiceAsync(appContext);
                }
                Log.d(TAG, "PROVIDER NOK - FETCH FAILED");
                break;
            case PROVIDER_OK:
                Log.d(TAG, "PROVIDER OK - FETCH SUCCESSFUL");
                //no break, continue with next case
            case CORRECTLY_DOWNLOADED_VPN_CERTIFICATE:
                if (ProviderSetupObservable.isSetupRunning() && !activityForeground.get()) {
                    ProviderSetupObservable.storeLastResult(resultCode, resultData);
                }
                break;
            case TOR_TIMEOUT:
            case TOR_EXCEPTION:
                try {
                    JSONObject jsonObject = new JSONObject(resultData.getString(ProviderAPI.ERRORS));
                    String initialAction = jsonObject.optString(ProviderAPI.INITIAL_ACTION);
                    if (UPDATE_INVALID_VPN_CERTIFICATE.equals(initialAction)) {
                        EipStatus.getInstance().setUpdatingVpnCert(false);
                    }
                } catch (Exception e) {
                    //ignore
                }
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
            EipCommand.startVPN(appContext, earlyRoutes);
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
                            EipCommand.startBlockingVPN(appContext);
                            break;
                        case ERROR_INVALID_PROFILE:
                            selectNextGateway();
                            break;
                        default:
                            finishGatewaySetup(false);
                            EipCommand.stopVPN(appContext);
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
            // mConnections.length is the amount of remotes defined in the current profile
            if (reconnectTry.addAndGet(1) == setupVpnProfile.mConnections.length) {
                Log.e(TAG, "Timeout reached! Try next gateway!");
                VpnStatus.logError("Timeout reached! Try next gateway!");
                selectNextGateway();
            }
        } else if ("NOPROCESS".equals(state) && LEVEL_NOTCONNECTED == level) {
            //??
        } else if ("CONNECTED".equals(state)) {
            //saveLastProfile(context.getApplicationContext(), setupVpnProfile.getUUIDString());
            Provider provider = ProviderObservable.getInstance().getCurrentProvider();
            if (setupNClosestGateway.get() > 0 || provider.shouldUpdateEipServiceJson()) {
                //setupNClostestGateway > 0: at least one failed gateway -> did the provider change it's gateways?
                Bundle parameters = new Bundle();
                parameters.putLong(DELAY, 500);
                ProviderAPICommand.execute(appContext, ProviderAPI.DOWNLOAD_SERVICE_JSON, parameters, provider);
            }

            if (shouldCheckAppUpdate()) {
                Bundle parameters = new Bundle();
                parameters.putLong(DELAY, 500);
                DownloadServiceCommand.execute(appContext, CHECK_VERSION_FILE, parameters);
            }

            if (provider.shouldUpdateVpnCertificate()) {
                Bundle parameters = new Bundle();
                parameters.putLong(DELAY, 500);
                ProviderAPICommand.execute(appContext, QUIETLY_UPDATE_VPN_CERTIFICATE, parameters, provider);
            }

            if (provider.shouldUpdateMotdJson()) {
                Bundle parameters = new Bundle();
                parameters.putLong(DELAY, 500);
                ProviderAPICommand.execute(appContext, DOWNLOAD_MOTD, parameters, provider);
            }
            finishGatewaySetup(false);
        } else if ("TCP_CONNECT".equals(state)) {
            changingGateway.set(false);
        }
    }

    private boolean shouldCheckAppUpdate() {
        return System.currentTimeMillis() - PreferenceHelper.getLastAppUpdateCheck() >= UPDATE_CHECK_TIMEOUT;
    }

    private void selectNextGateway() {
        changingGateway.set(true);
        reconnectTry.set(0);
        EipCommand.startVPN(appContext, false, setupNClosestGateway.get() + 1);
    }

    private void finishGatewaySetup(boolean changingGateway) {
        VpnStatus.removeStateListener(this);
        setupVpnProfile = null;
        setupNClosestGateway.set(0);
        observedProfileFromVpnStatus = null;
        this.changingGateway.set(changingGateway);
        this.reconnectTry.set(0);
        if (TorStatusObservable.isRunning()) {
            TorServiceCommand.stopTorServiceAsync(appContext);
        }
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
            if (BuildConfig.DEBUG) {
                Log.e("ERROR", logItem.getString(appContext));
            }
            switch (logItem.getErrorType()) {
                case SHAPESHIFTER:
                    VpnProfile profile = VpnStatus.getLastConnectedVpnProfile();
                    if (profile == null) {
                        EipCommand.startVPN(appContext, false, 0);
                    } else {
                        GatewaysManager gatewaysManager = new GatewaysManager(appContext);
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
