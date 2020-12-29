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

package se.leap.bitmaskclient.base;

import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.multidex.MultiDexApplication;

import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import se.leap.bitmaskclient.BuildConfig;
import se.leap.bitmaskclient.appUpdate.DownloadBroadcastReceiver;
import se.leap.bitmaskclient.eip.EipSetupObserver;
import se.leap.bitmaskclient.base.models.ProviderObservable;
import se.leap.bitmaskclient.tethering.TetheringStateManager;
import se.leap.bitmaskclient.base.utils.PRNGFixes;

import static android.content.Intent.CATEGORY_DEFAULT;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_DOWNLOAD_SERVICE_EVENT;
import static se.leap.bitmaskclient.base.models.Constants.SHARED_PREFERENCES;
import static se.leap.bitmaskclient.appUpdate.DownloadBroadcastReceiver.ACTION_DOWNLOAD;
import static se.leap.bitmaskclient.appUpdate.DownloadServiceCommand.CHECK_VERSION_FILE;
import static se.leap.bitmaskclient.appUpdate.DownloadServiceCommand.DOWNLOAD_UPDATE;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getSavedProviderFromSharedPreferences;

/**
 * Created by cyberta on 24.10.17.
 */

public class BitmaskApp extends MultiDexApplication {

    private final static String TAG = BitmaskApp.class.getSimpleName();
    private RefWatcher refWatcher;
    private ProviderObservable providerObservable;
    private DownloadBroadcastReceiver downloadBroadcastReceiver;


    @Override
    public void onCreate() {
        super.onCreate();
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        refWatcher = LeakCanary.install(this);
        // Normal app init code...*/
        PRNGFixes.apply();
        SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        providerObservable = ProviderObservable.getInstance();
        providerObservable.updateProvider(getSavedProviderFromSharedPreferences(preferences));
        EipSetupObserver.init(this, preferences);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        TetheringStateManager.getInstance().init(this);
        if (BuildConfig.FLAVOR.contains("Fatweb")) {
            downloadBroadcastReceiver = new DownloadBroadcastReceiver();
            IntentFilter intentFilter = new IntentFilter(BROADCAST_DOWNLOAD_SERVICE_EVENT);
            intentFilter.addAction(ACTION_DOWNLOAD);
            intentFilter.addAction(CHECK_VERSION_FILE);
            intentFilter.addAction(DOWNLOAD_UPDATE);
            intentFilter.addCategory(CATEGORY_DEFAULT);
            LocalBroadcastManager.getInstance(this.getApplicationContext()).registerReceiver(downloadBroadcastReceiver, intentFilter);
        }
    }

    /**
     * Use this method to get a RefWatcher object that checks for memory leaks in the given context.
     * Call refWatcher.watch(this) to check if all references get garbage collected.
     * @param context
     * @return the RefWatcher object
     */
    public static RefWatcher getRefWatcher(Context context) {
        BitmaskApp application = (BitmaskApp) context.getApplicationContext();
        return application.refWatcher;
    }


}
