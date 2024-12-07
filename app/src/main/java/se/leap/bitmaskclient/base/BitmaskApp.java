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

package se.leap.bitmaskclient.base;

import static android.content.Intent.CATEGORY_DEFAULT;
import static se.leap.bitmaskclient.appUpdate.DownloadBroadcastReceiver.ACTION_DOWNLOAD;
import static se.leap.bitmaskclient.appUpdate.DownloadServiceCommand.CHECK_VERSION_FILE;
import static se.leap.bitmaskclient.appUpdate.DownloadServiceCommand.DOWNLOAD_UPDATE;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_DOWNLOAD_SERVICE_EVENT;
import static se.leap.bitmaskclient.base.utils.ConfigHelper.isCalyxOSWithTetheringSupport;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getSavedProviderFromSharedPreferences;

import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.multidex.MultiDexApplication;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.conscrypt.Conscrypt;

import java.security.Provider;
import java.security.Security;

import se.leap.bitmaskclient.BuildConfig;
import se.leap.bitmaskclient.appUpdate.DownloadBroadcastReceiver;
import se.leap.bitmaskclient.base.models.ProviderObservable;
import se.leap.bitmaskclient.base.utils.PRNGFixes;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.eip.EipSetupObserver;
import se.leap.bitmaskclient.providersetup.ProviderSetupObservable;
import se.leap.bitmaskclient.tethering.TetheringStateManager;
import se.leap.bitmaskclient.tor.TorStatusObservable;

/**
 * Created by cyberta on 24.10.17.
 */

public class BitmaskApp extends MultiDexApplication implements DefaultLifecycleObserver {

    private final static String TAG = BitmaskApp.class.getSimpleName();
    private ProviderObservable providerObservable;
    private DownloadBroadcastReceiver downloadBroadcastReceiver;
    private TorStatusObservable torStatusObservable;

    private ProviderSetupObservable providerSetupObservable;

    private PreferenceHelper preferenceHelper;


    @Override
    public void onCreate() {
        super.onCreate();
        // Normal app init code...*/
        PRNGFixes.apply();
        final Provider provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
        // Replace Android's own BC provider
        if (!provider.getClass().equals(BouncyCastleProvider.class)) {
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
            Security.insertProviderAt(new BouncyCastleProvider(), 1);
        }
        Security.insertProviderAt(Conscrypt.newProvider(), 2);

        preferenceHelper = new PreferenceHelper(this);
        providerObservable = ProviderObservable.getInstance();
        providerObservable.updateProvider(getSavedProviderFromSharedPreferences());
        torStatusObservable = TorStatusObservable.getInstance();
        providerSetupObservable = ProviderSetupObservable.getInstance();
        EipSetupObserver.init(this);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        if (!isCalyxOSWithTetheringSupport(this)) {
            TetheringStateManager.getInstance().init(this);
        }
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

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onPause(owner);
        EipSetupObserver.setActivityForeground(true);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onResume(owner);
        EipSetupObserver.setActivityForeground(false);
    }
}
