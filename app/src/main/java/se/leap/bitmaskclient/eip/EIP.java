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
package se.leap.bitmaskclient.eip;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.annotation.WorkerThread;
import androidx.core.app.JobIntentService;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Closeable;
import java.lang.ref.WeakReference;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import de.blinkt.openvpn.LaunchVPN;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.IOpenVPNServiceInternal;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.Preferences;
import de.blinkt.openvpn.core.VpnStatus;
import de.blinkt.openvpn.core.connection.Connection;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.OnBootReceiver;
import se.leap.bitmaskclient.base.models.ProviderObservable;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static de.blinkt.openvpn.core.connection.Connection.TransportType.OBFS4;
import static de.blinkt.openvpn.core.connection.Connection.TransportType.OPENVPN;
import static se.leap.bitmaskclient.R.string.vpn_certificate_is_invalid;
import static se.leap.bitmaskclient.R.string.warning_client_parsing_error_gateways;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_GATEWAY_SETUP_OBSERVER_EVENT;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_KEY;
import static se.leap.bitmaskclient.base.models.Constants.CLEARLOG;
import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_CHECK_CERT_VALIDITY;
import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_IS_RUNNING;
import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_LAUNCH_VPN;
import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_START;
import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_START_ALWAYS_ON_VPN;
import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_START_BLOCKING_VPN;
import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_STOP;
import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_STOP_BLOCKING_VPN;
import static se.leap.bitmaskclient.base.models.Constants.EIP_EARLY_ROUTES;
import static se.leap.bitmaskclient.base.models.Constants.EIP_N_CLOSEST_GATEWAY;
import static se.leap.bitmaskclient.base.models.Constants.EIP_RECEIVER;
import static se.leap.bitmaskclient.base.models.Constants.EIP_RESTART_ON_BOOT;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_PROFILE;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.base.models.Constants.SHARED_PREFERENCES;
import static se.leap.bitmaskclient.base.utils.ConfigHelper.ensureNotOnMainThread;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getPreferredCity;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getUsePluggableTransports;
import static se.leap.bitmaskclient.eip.EIP.EIPErrors.ERROR_INVALID_PROFILE;
import static se.leap.bitmaskclient.eip.EIP.EIPErrors.ERROR_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.eip.EIP.EIPErrors.ERROR_VPN_PREPARE;
import static se.leap.bitmaskclient.eip.EIP.EIPErrors.NO_MORE_GATEWAYS;
import static se.leap.bitmaskclient.eip.EipResultBroadcast.tellToReceiverOrBroadcast;

/**
 * EIP is the abstract base class for interacting with and managing the Encrypted
 * Internet Proxy connection.  Connections are started, stopped, and queried through
 * this Service.
 * Contains logic for parsing eip-service.json from the provider, configuring and selecting
 * gateways, and controlling {@link de.blinkt.openvpn.core.OpenVPNService} connections.
 *
 * @author Sean Leonard <meanderingcode@aetherislands.net>
 * @author Parménides GV <parmegv@sdf.org>
 */
public final class EIP extends JobIntentService implements Observer {


    public final static String TAG = EIP.class.getSimpleName(),
            SERVICE_API_PATH = "config/eip-service.json",
            ERRORS = "errors",
            ERRORID = "errorId";

    private volatile SharedPreferences preferences;
    private volatile EipStatus eipStatus;
    // Service connection to OpenVpnService, shared between threads
    private volatile OpenVpnServiceConnection openVpnServiceConnection;
    private WeakReference<ResultReceiver> mResultRef = new WeakReference<>(null);
    private volatile VoidVpnServiceConnection voidVpnServiceConnection;

    /**
     * Unique job ID for this service.
     */
    static final int JOB_ID = 1312;

    public enum EIPErrors {
        UNKNOWN,
        ERROR_INVALID_VPN_CERTIFICATE,
        NO_MORE_GATEWAYS,
        ERROR_VPN_PREPARE,
        ERROR_INVALID_PROFILE
    }

    /**
     * Convenience method for enqueuing work in to this service.
     */
    static void enqueueWork(Context context, Intent work) {
        try {
            enqueueWork(context, EIP.class, JOB_ID, work);
        } catch (IllegalStateException e) {
           e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        eipStatus = EipStatus.getInstance();
        eipStatus.addObserver(this);
        preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        eipStatus.deleteObserver(this);
        if (openVpnServiceConnection != null) {
            openVpnServiceConnection.close();
            openVpnServiceConnection = null;
        }
        if (voidVpnServiceConnection != null) {
            voidVpnServiceConnection.close();
            voidVpnServiceConnection = null;
        }
    }

    /**
     * update eipStatus whenever it changes
     */
    @Override
    public void update(Observable observable, Object data) {
        if (observable instanceof EipStatus) {
            eipStatus = (EipStatus) observable;
        }
    }

    /**
     *
     * @param intent the intent that started this EIP call
     */
    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        final String action = intent.getAction();
        if (action == null) {
            return;
        }

        if (intent.getParcelableExtra(EIP_RECEIVER) != null) {
            mResultRef = new WeakReference<>((ResultReceiver) intent.getParcelableExtra(EIP_RECEIVER));
        }
        int nClosestGateway;
        switch (action) {
            case EIP_ACTION_START:
                boolean earlyRoutes = intent.getBooleanExtra(EIP_EARLY_ROUTES, false);
                nClosestGateway = intent.getIntExtra(EIP_N_CLOSEST_GATEWAY, 0);
                startEIP(earlyRoutes, nClosestGateway);
                break;
            case EIP_ACTION_START_ALWAYS_ON_VPN:
                startEIPAlwaysOnVpn();
                break;
            case EIP_ACTION_STOP:
                stopEIP();
                break;
            case EIP_ACTION_IS_RUNNING:
                isRunning();
                break;
            case EIP_ACTION_CHECK_CERT_VALIDITY:
                checkVPNCertificateValidity();
                break;
            case EIP_ACTION_START_BLOCKING_VPN:
                disconnect();
                earlyRoutes();
                break;
            case EIP_ACTION_LAUNCH_VPN:
                VpnProfile profile = (VpnProfile) intent.getSerializableExtra(PROVIDER_PROFILE);
                launchProfile(profile);
                break;
        }
    }

    /**
     * Initiates an EIP connection by selecting a gateway and preparing and sending an
     * Intent to {@link de.blinkt.openvpn.LaunchVPN}.
     * It also sets up early routes.
     * @param earlyRoutes if true, a void vpn gets set up
     * @param nClosestGateway the gateway that is the n nearest one to the users place
     */
    @SuppressLint("ApplySharedPref")
    private void startEIP(boolean earlyRoutes, int nClosestGateway) {
        Log.d(TAG, "start EIP with early routes: " +  earlyRoutes + " and nClosest Gateway: " + nClosestGateway);
        Bundle result = new Bundle();
        if (!eipStatus.isBlockingVpnEstablished() && earlyRoutes) {
            earlyRoutes(result);
        }

        if (!preferences.getBoolean(EIP_RESTART_ON_BOOT, false)) {
            preferences.edit().putBoolean(EIP_RESTART_ON_BOOT, true).commit();
        }

        if (!isVPNCertificateValid()) {
            setErrorResult(result, vpn_certificate_is_invalid, ERROR_INVALID_VPN_CERTIFICATE.toString());
            tellToReceiverOrBroadcast(this, EIP_ACTION_START, RESULT_CANCELED, result);
            return;
        }

        GatewaysManager gatewaysManager = new GatewaysManager(getApplicationContext());
        if (gatewaysManager.isEmpty()) {
            setErrorResult(result, warning_client_parsing_error_gateways, null);
            tellToReceiverOrBroadcast(this, EIP_ACTION_START, RESULT_CANCELED, result);
            return;
        }

        Gateway gateway = gatewaysManager.select(nClosestGateway);
        launchActiveGateway(gateway, nClosestGateway, result);
        if (result.containsKey(BROADCAST_RESULT_KEY) && !result.getBoolean(BROADCAST_RESULT_KEY)) {
            tellToReceiverOrBroadcast(this, EIP_ACTION_START, RESULT_CANCELED, result);
        } else {
            tellToReceiverOrBroadcast(this, EIP_ACTION_START, RESULT_OK);
        }
    }

    /**
     * Tries to start the last used vpn profile when the OS was rebooted and always-on-VPN is enabled.
     * The {@link OnBootReceiver} will care if there is no profile.
     */
    private void startEIPAlwaysOnVpn() {
        GatewaysManager gatewaysManager = new GatewaysManager(getApplicationContext());
        Gateway gateway = gatewaysManager.select(0);
        Bundle result = new Bundle();

        launchActiveGateway(gateway, 0, result);
        if (result.containsKey(BROADCAST_RESULT_KEY) && !result.getBoolean(BROADCAST_RESULT_KEY)){
            VpnStatus.logWarning("ALWAYS-ON VPN: " + getString(R.string.no_vpn_profiles_defined));
        }
    }

    private void earlyRoutes() {
        Bundle result = new Bundle();
        earlyRoutes(result);
        if (result.containsKey(BROADCAST_RESULT_KEY) && !result.getBoolean(BROADCAST_RESULT_KEY)){
            tellToReceiverOrBroadcast(this, EIP_ACTION_START_BLOCKING_VPN, RESULT_CANCELED, result);
        }
    }

    /**
     * Early routes are routes that block traffic until a new
     * VpnService is started properly.
     */
    private void earlyRoutes(Bundle result) {
        Intent blockingIntent = VpnService.prepare(getApplicationContext()); // stops the VPN connection created by another application.
        if (blockingIntent == null) {
            try {
                initVoidVpnServiceConnection();
                Intent voidVpnService = new Intent(getApplicationContext(), VoidVpnService.class);
                voidVpnService.setAction(EIP_ACTION_START_BLOCKING_VPN);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    getApplicationContext().startForegroundService(voidVpnService);
                    voidVpnServiceConnection.getService().startWithForegroundNotification();
                } else {
                    getApplicationContext().startService(voidVpnService);
                }
            } catch (InterruptedException | IllegalStateException e) {
                setErrorResult(result,  R.string.vpn_error_establish, null);
            }
        } else {
            Intent voidVpnLauncher = new Intent(getApplicationContext(), VoidVpnLauncher.class);
            voidVpnLauncher.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(voidVpnLauncher);
        }
    }

    /**
     * starts the VPN and connects to the given gateway
     *
     * @param gateway to connect to
     */
    private void launchActiveGateway(Gateway gateway, int nClosestGateway, Bundle result) {
        VpnProfile profile;
        Connection.TransportType transportType = getUsePluggableTransports(this) ? OBFS4 : OPENVPN;
        if (gateway == null ||
                (profile = gateway.getProfile(transportType)) == null) {
            String preferredLocation = getPreferredCity(getApplicationContext());
            if (preferredLocation != null) {
                setErrorResult(result, NO_MORE_GATEWAYS.toString(), getStringResourceForNoMoreGateways(), getString(R.string.app_name), preferredLocation);
            } else {
                setErrorResult(result, NO_MORE_GATEWAYS.toString(), getStringResourceForNoMoreGateways(), getString(R.string.app_name));
            }
            return;
        }

        Intent intent;
        try {
            intent = VpnService.prepare(getApplicationContext());
        } catch (NullPointerException npe) {
            setErrorResult(result, ERROR_VPN_PREPARE.toString(), R.string.vpn_error_establish);
            return;
        }
        if (intent == null) {
            // vpn has been successfully prepared

            //inform EipSetupObserver about vpn connecting attempt
            Intent setupObserverIntent = new Intent(BROADCAST_GATEWAY_SETUP_OBSERVER_EVENT);
            setupObserverIntent.putExtra(PROVIDER_PROFILE, profile);
            setupObserverIntent.putExtra(EIP_N_CLOSEST_GATEWAY, nClosestGateway);
            LocalBroadcastManager.getInstance(this).sendBroadcast(setupObserverIntent);

            // Check if we need to clear the log
            if (Preferences.getDefaultSharedPreferences(this).getBoolean(CLEARLOG, true))
                VpnStatus.clearLog();

            // check profile configuration
            int vpnok = profile.checkProfile(this);
            if (vpnok != R.string.no_error_found) {
                VpnStatus.logError(R.string.config_error_found);
                VpnStatus.logError(vpnok);
                setErrorResult(result, ERROR_INVALID_PROFILE.toString(), 0);
                return;
            }

            //launch profile
            launchProfile(profile, result);

        } else {
            // vpn permission is missing
            Intent permissionIntent = new Intent(getApplicationContext(), LaunchVPN.class);
            permissionIntent.setAction(Intent.ACTION_MAIN);
            permissionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            permissionIntent.putExtra(PROVIDER_PROFILE, profile);
            permissionIntent.putExtra(EIP_N_CLOSEST_GATEWAY, nClosestGateway);
            startActivity(permissionIntent);
        }
    }


    /**
     * Stop VPN
     * First checks if the OpenVpnConnection is open then
     * terminates EIP if currently connected or connecting
     */
    private void stopEIP() {
        VpnStatus.updateStateString("STOPPING", "STOPPING VPN", R.string.state_exiting, ConnectionStatus.LEVEL_STOPPING);
        int resultCode = stop() ? RESULT_OK : RESULT_CANCELED;
        tellToReceiverOrBroadcast(this, EIP_ACTION_STOP, resultCode);
    }

    /**
     * Checks the last stored status notified by ics-openvpn
     * Sends <code>Activity.RESULT_CANCELED</code> to the ResultReceiver that made the
     * request if it's not connected, <code>Activity.RESULT_OK</code> otherwise.
     */
    private void isRunning() {
        int resultCode = (eipStatus.isConnected()) ?
                RESULT_OK :
                RESULT_CANCELED;
        tellToReceiverOrBroadcast(this, EIP_ACTION_IS_RUNNING, resultCode);
    }

    /**
     * read VPN certificate from preferences and check it
     * broadcast result
     */
    private void checkVPNCertificateValidity() {
        int resultCode = isVPNCertificateValid() ?
                RESULT_OK :
                RESULT_CANCELED;
        tellToReceiverOrBroadcast(this, EIP_ACTION_CHECK_CERT_VALIDITY, resultCode);
    }

    /**
     * read VPN certificate from preferences and check it
     *
     * @return true if VPN certificate is valid false otherwise
     */
    private boolean isVPNCertificateValid() {
        VpnCertificateValidator validator = new VpnCertificateValidator(preferences.getString(PROVIDER_VPN_CERTIFICATE, ""));
        return validator.isValid();
    }

    /**
     * helper function to add error to result bundle
     *
     * @param result         - result of an action
     * @param errorMessageId - id of string resource describing the error
     * @param errorId        - MainActivityErrorDialog DownloadError id
     */
    void setErrorResult(Bundle result, @StringRes int errorMessageId, String errorId) {
        setErrorResult(result, errorId, errorMessageId, (Object[]) null);
    }


    /**
     * helper function to add error to result bundle
     *
     * @param result         - result of an action
     * @param errorMessageId - id of string resource describing the error
     * @param errorId        - MainActivityErrorDialog DownloadError id
     */
    void setErrorResult(Bundle result, String errorId, @StringRes int errorMessageId, Object... args) {
        JSONObject errorJson = new JSONObject();
        try {
            if (errorMessageId != 0) {
                String errorMessage;
                if (args != null) {
                    errorMessage = getResources().getString(errorMessageId, args);
                } else {
                    errorMessage = getResources().getString(errorMessageId);
                }
                VpnStatus.logWarning("[EIP] error: " + errorMessage);
                errorJson.put(ERRORS, errorMessage);
            }
            errorJson.put(ERRORID, errorId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        result.putString(ERRORS, errorJson.toString());
        result.putBoolean(BROADCAST_RESULT_KEY, false);
    }

    /**
     * disable Bitmask starting after phone reboot
     * then stop VPN
     */
    private boolean stop() {
        preferences.edit().putBoolean(EIP_RESTART_ON_BOOT, false).apply();
        if (eipStatus.isBlockingVpnEstablished()) {
            stopBlockingVpn();
        }
        return disconnect();
    }

    /**
     * stop void vpn from blocking internet
     */
    private void stopBlockingVpn() {
        Log.d(TAG, "stop VoidVpn!");
        Intent stopVoidVpnIntent = new Intent(this, VoidVpnService.class);
        stopVoidVpnIntent.setAction(EIP_ACTION_STOP_BLOCKING_VPN);
        startService(stopVoidVpnIntent);
    }

    /**
     * creates a OpenVpnServiceConnection if necessary
     * then terminates OpenVPN
     */
    private boolean disconnect() {
        try {
            initOpenVpnServiceConnection();
        } catch (InterruptedException | IllegalStateException e) {
            return false;
        }

        try {
            return openVpnServiceConnection.getService().stopVPN(false);
        } catch (RemoteException e) {
            VpnStatus.logException(e);
        }
        return false;
    }

    /**
     * binds OpenVPNService to this service, starts it as a foreground service with a profile
     * @param vpnProfile OpenVPN profile used to create a VPN connection
     * @param result Bundle containing information about possible errors
     */
    private void launchProfile(VpnProfile vpnProfile, Bundle result) {
        Intent startVPN = vpnProfile.prepareStartService(getApplicationContext());
        if (startVPN != null) {
            try {
                initOpenVpnServiceConnection();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    //noinspection NewApi
                    getApplicationContext().startForegroundService(startVPN);
                    openVpnServiceConnection.getService().startWithForegroundNotification();
                } else {
                    getApplicationContext().startService(startVPN);
                }
            } catch (InterruptedException | IllegalStateException | RemoteException e) {
                    setErrorResult(result,  R.string.vpn_error_establish, null);
            }
        } else {
            setErrorResult(result,  R.string.vpn_error_establish, null);
        }
    }

    private void launchProfile(VpnProfile vpnProfile) {
        Bundle bundle = new Bundle();
        launchProfile(vpnProfile, bundle);
        if (bundle.containsKey(BROADCAST_RESULT_KEY) && !bundle.getBoolean(BROADCAST_RESULT_KEY)) {
            tellToReceiverOrBroadcast(this.getApplicationContext(), EIP_ACTION_LAUNCH_VPN, RESULT_CANCELED, bundle);
        } else {
            tellToReceiverOrBroadcast(this.getApplicationContext(), EIP_ACTION_LAUNCH_VPN, RESULT_OK);
        }
    }


    private @StringRes int getStringResourceForNoMoreGateways() {
        boolean isManualGatewaySelection = PreferenceHelper.getPreferredCity(getApplicationContext()) != null;
        if (isManualGatewaySelection) {
            return R.string.warning_no_more_gateways_manual_gw_selection;
        } else if (ProviderObservable.getInstance().getCurrentProvider().supportsPluggableTransports()) {
            if (PreferenceHelper.getUsePluggableTransports(getApplicationContext())) {
                return R.string.warning_no_more_gateways_use_ovpn;
            } else {
                return R.string.warning_no_more_gateways_use_pt;
            }
        } else {
            return R.string.warning_no_more_gateways_no_pt;
        }
    }

    /**
     * Assigns a new OpenVpnServiceConnection to EIP's member variable openVpnServiceConnection.
     * Only one thread at a time can create the service connection, that will be shared between threads
     *
     * @throws InterruptedException  thrown if thread gets interrupted
     * @throws IllegalStateException thrown if this method was not called from a background thread
     */
    private void initOpenVpnServiceConnection() throws InterruptedException, IllegalStateException {
        if (openVpnServiceConnection == null) {
            Log.d(TAG, "serviceConnection is still null");
            openVpnServiceConnection = new OpenVpnServiceConnection(this);
        }
    }

    /**
     * Assigns a new VoidVpnServiceConnection to EIP's member variable voidVpnServiceConnection.
     * Only one thread at a time can create the service connection, that will be shared between threads
     *
     * @throws InterruptedException  thrown if thread gets interrupted
     * @throws IllegalStateException thrown if this method was not called from a background thread
     */
    private void initVoidVpnServiceConnection() throws InterruptedException, IllegalStateException {
        if (voidVpnServiceConnection == null) {
            Log.d(TAG, "serviceConnection is still null");
            voidVpnServiceConnection = new VoidVpnServiceConnection(this);
        }
    }

    public static class VoidVpnServiceConnection implements Closeable {
        private final Context context;
        private ServiceConnection serviceConnection;
        private VoidVpnService voidVpnService;

        VoidVpnServiceConnection(Context context) throws InterruptedException, IllegalStateException {
            this.context = context;
            ensureNotOnMainThread(context);
            Log.d(TAG, "initSynchronizedServiceConnection!");
            initSynchronizedServiceConnection(context);
        }

        @Override
        public void close() {
            context.unbindService(serviceConnection);
        }

        private void initSynchronizedServiceConnection(final Context context) throws InterruptedException {
            final BlockingQueue<VoidVpnService> blockingQueue = new LinkedBlockingQueue<>(1);
            this.serviceConnection = new ServiceConnection() {
                volatile boolean mConnectedAtLeastOnce = false;

                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    if (!mConnectedAtLeastOnce) {
                        mConnectedAtLeastOnce = true;
                        try {
                            VoidVpnService.VoidVpnServiceBinder binder = (VoidVpnService.VoidVpnServiceBinder) service;
                            blockingQueue.put(binder.getService());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                }
            };
            Intent intent = new Intent(context, VoidVpnService.class);
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            voidVpnService = blockingQueue.take();
        }

        public VoidVpnService getService() {
            return voidVpnService;
        }
    }

    /**
     * Creates a service connection to OpenVpnService.
     * The constructor blocks until the service is bound to the given Context.
     * Pattern stolen from android.security.KeyChain.java
     */
    @WorkerThread
    public static class OpenVpnServiceConnection implements Closeable {
        private final Context context;
        private ServiceConnection serviceConnection;
        private IOpenVPNServiceInternal service;

        OpenVpnServiceConnection(Context context) throws InterruptedException, IllegalStateException {
            this.context = context;
            ensureNotOnMainThread(context);
            Log.d(TAG, "initSynchronizedServiceConnection!");
            initSynchronizedServiceConnection(context);
        }

        private void initSynchronizedServiceConnection(final Context context) throws InterruptedException {
            final BlockingQueue<IOpenVPNServiceInternal> blockingQueue = new LinkedBlockingQueue<>(1);
            this.serviceConnection = new ServiceConnection() {
                volatile boolean mConnectedAtLeastOnce = false;
                @Override public void onServiceConnected(ComponentName name, IBinder service) {
                    if (!mConnectedAtLeastOnce) {
                        mConnectedAtLeastOnce = true;
                        try {
                            blockingQueue.put(IOpenVPNServiceInternal.Stub.asInterface(service));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                @Override public void onServiceDisconnected(ComponentName name) {
                }
            };

            Intent intent = new Intent(context, OpenVPNService.class);
            intent.setAction(OpenVPNService.START_SERVICE);
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            service = blockingQueue.take();
        }

        @Override public void close() {
            context.unbindService(serviceConnection);
        }

        public IOpenVPNServiceInternal getService() {
            return service;
        }
    }

}
