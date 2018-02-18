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
package se.leap.bitmaskclient;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;

import static se.leap.bitmaskclient.Constants.SHARED_PREFERENCES;

/**
 * Implements HTTP api methods (encapsulated in {{@link ProviderApiManager}})
 * used to manage communications with the provider server.
 * <p/>
 * It's an IntentService because it downloads data from the Internet, so it operates in the background.
 *
 * @author parmegv
 * @author MeanderingCode
 * @author cyberta
 */

public class ProviderAPI extends IntentService implements ProviderApiManagerBase.ProviderApiServiceCallback {

    final public static String
            TAG = ProviderAPI.class.getSimpleName(),
            SET_UP_PROVIDER = "setUpProvider",
            UPDATE_PROVIDER_DETAILS = "updateProviderDetails",
            SIGN_UP = "srpRegister",
            LOG_IN = "srpAuth",
            LOG_OUT = "logOut",
            DOWNLOAD_VPN_CERTIFICATE = "downloadUserAuthedVPNCertificate",
            PARAMETERS = "parameters",
            RECEIVER_KEY = "receiver",
            ERRORS = "errors",
            ERRORID = "errorId",
            BACKEND_ERROR_KEY = "error",
            BACKEND_ERROR_MESSAGE = "message",
            DOWNLOAD_SERVICE_JSON = "ProviderAPI.DOWNLOAD_SERVICE_JSON",
            PROVIDER_SET_UP = "ProviderAPI.PROVIDER_SET_UP";

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
            INCORRECTLY_DOWNLOADED_EIP_SERVICE = 14;

    ProviderApiManager providerApiManager;

    public ProviderAPI() {
        super(TAG);
    }

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

    @Override
    public void broadcastEvent(Intent intent) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    protected void onHandleIntent(Intent command) {
        providerApiManager.handleIntent(command);
    }

    private ProviderApiManager initApiManager() {
        SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        OkHttpClientGenerator clientGenerator = new OkHttpClientGenerator(preferences, getResources());
        return new ProviderApiManager(preferences, getResources(), clientGenerator, this);
    }
}
