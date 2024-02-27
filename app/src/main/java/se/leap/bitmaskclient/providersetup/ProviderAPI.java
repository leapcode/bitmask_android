/**
 * Copyright (c) 2017 LEAP Encryption Access Project and contributers
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
package se.leap.bitmaskclient.providersetup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.concurrent.TimeoutException;

import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.providersetup.connectivity.OkHttpClientGenerator;
import se.leap.bitmaskclient.tor.TorServiceCommand;

/**
 * Implements HTTP api methods (encapsulated in {{@link ProviderApiManager}})
 * used to manage communications with the provider server.
 * <p/>
 * It's an JobIntentService because it downloads data from the Internet, so it operates in the background.
 *
 * @author parmegv
 * @author MeanderingCode
 * @author cyberta
 */

public class ProviderAPI extends JobIntentService implements ProviderApiManagerBase.ProviderApiServiceCallback {

    /**
     * Unique job ID for this service.
     */
    static final int JOB_ID = 161375;

    @Deprecated
    final public static String
            UPDATE_PROVIDER_DETAILS = "updateProviderDetails",
            SIGN_UP = "srpRegister",
            LOG_IN = "srpAuth",
            LOG_OUT = "logOut";

    final public static String
            TAG = ProviderAPI.class.getSimpleName(),
            SET_UP_PROVIDER = "setUpProvider",
            DOWNLOAD_GEOIP_JSON = "downloadGeoIpJson",
            DOWNLOAD_MOTD = "downloadMotd",
            // all vpn certificate download commands are used in different scenarios with different error handling
            // command key used for the initial vpn certificate download during the provider setup
            DOWNLOAD_VPN_CERTIFICATE = "downloadUserAuthedVPNCertificate",
            // command key used to update soon expiring but yet valid certificates after connecting to the vpn
            QUIETLY_UPDATE_VPN_CERTIFICATE = "ProviderAPI.QUIETLY_UPDATE_VPN_CERTIFICATE",
            // command key used to update invalid certificates, connecting to the vpn is impossible
            UPDATE_INVALID_VPN_CERTIFICATE = "ProviderAPI.UPDATE_INVALID_VPN_CERTIFICATE",
            PARAMETERS = "parameters",
            DELAY = "delay",
            RECEIVER_KEY = "receiver",
            ERRORS = "errors",
            ERRORID = "errorId",
            INITIAL_ACTION = "initalAction",
            BACKEND_ERROR_KEY = "error",
            BACKEND_ERROR_MESSAGE = "message",
            USER_MESSAGE = "userMessage",
            DOWNLOAD_SERVICE_JSON = "ProviderAPI.DOWNLOAD_SERVICE_JSON";

    final public static int
            SUCCESSFUL_LOGIN = 3,
            FAILED_LOGIN = 4,
            SUCCESSFUL_SIGNUP = 5,
            FAILED_SIGNUP = 6,
            SUCCESSFUL_LOGOUT = 7,
            LOGOUT_FAILED = 8,
            CORRECTLY_DOWNLOADED_VPN_CERTIFICATE = 9,
            INCORRECTLY_DOWNLOADED_VPN_CERTIFICATE = 10,
            PROVIDER_OK = 11,
            PROVIDER_NOK = 12,
            CORRECTLY_DOWNLOADED_EIP_SERVICE = 13,
            INCORRECTLY_DOWNLOADED_EIP_SERVICE = 14,
            CORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE = 15,
            INCORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE = 16,
            CORRECTLY_DOWNLOADED_GEOIP_JSON = 17,
            INCORRECTLY_DOWNLOADED_GEOIP_JSON = 18,
            TOR_TIMEOUT = 19,
            MISSING_NETWORK_CONNECTION = 20,
            TOR_EXCEPTION = 21;

    ProviderApiManager providerApiManager;

    //TODO: refactor me, please!
    //used in insecure flavor only
    @SuppressLint("unused")
    public static boolean lastDangerOn() {
        return ProviderApiManager.lastDangerOn();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        providerApiManager = initApiManager();
    }

    /**
     * Convenience method for enqueuing work in to this service.
     */
    static void enqueueWork(Context context, Intent work) {
        try {
            ProviderAPI.enqueueWork(context, ProviderAPI.class, JOB_ID, work);
        } catch (IllegalStateException | IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onHandleWork(@NonNull Intent command) {
        providerApiManager.handleIntent(command);
    }

    @Override
    public void broadcastEvent(Intent intent) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public boolean startTorService() throws InterruptedException, IllegalStateException, TimeoutException {
        return TorServiceCommand.startTorService(this, null);
    }

    @Override
    public void stopTorService() {
        TorServiceCommand.stopTorService(this);
    }

    @Override
    public int getTorHttpTunnelPort() {
        return TorServiceCommand.getHttpTunnelPort(this);
    }

    @Override
    public boolean hasNetworkConnection() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null &&
                        activeNetwork.isConnected();
            } else {
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
                if (capabilities != null) {
                    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                }
                return false;
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            // we don't know, let's try to fetch data anyways then
            return true;
        }
    }

    @Override
    public void saveProvider(Provider p) {
        ProviderManager pm = ProviderManager.getInstance(this.getAssets());
        pm.add(p);
        pm.saveCustomProviders();
    }


    private ProviderApiManager initApiManager() {
        OkHttpClientGenerator clientGenerator = new OkHttpClientGenerator(getResources());
        return new ProviderApiManager(getResources(), clientGenerator, this);
    }

}
