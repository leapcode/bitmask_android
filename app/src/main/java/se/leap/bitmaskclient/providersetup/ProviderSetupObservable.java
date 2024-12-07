package se.leap.bitmaskclient.providersetup;
/**
 * Copyright (c) 2023 LEAP Encryption Access Project and contributors
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

import android.os.Bundle;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import se.leap.bitmaskclient.base.utils.HandlerProvider;
import se.leap.bitmaskclient.base.utils.HandlerProvider.HandlerInterface;
import se.leap.bitmaskclient.tor.TorStatusObservable;

/**
 * This Observable tracks the progress of a started provider bootstrapping attempt.
 * Each required API call us taken into account as well as the state of tor's bootstrapping in case
 * it is used for censorship circumvention.
 */
public class ProviderSetupObservable {

    private static final String TAG = ProviderSetupObservable.class.getSimpleName();

    private int progress = 0;
    private boolean canceled = false;
    public static final int DOWNLOADED_PROVIDER_JSON = 20;
    public static final int DOWNLOADED_CA_CERT = 40;
    public static final int DOWNLOADED_V5_SERVICE_JSON = 40;
    public static final int DOWNLOADED_V5_GATEWAYS = 60;
    public static final int DOWNLOADED_V5_BRIDGES = 80;
    public static final int DOWNLOADED_EIP_SERVICE_JSON = 60;
    public static final int DOWNLOADED_GEOIP_JSON = 80;
    public static final int DOWNLOADED_VPN_CERTIFICATE = 100;

    private static ProviderSetupObservable instance;
    private final PropertyChangeSupport changeSupport;
    public static final String PROPERTY_CHANGE = "ProviderSetupObservable";
    private final HandlerInterface handler;
    private long lastUpdate = 0;
    private int resultCode = 0;
    private Bundle resultData;



    private ProviderSetupObservable() {
        handler = HandlerProvider.get();
        changeSupport = new PropertyChangeSupport(this);
        resultData = new Bundle();
    }

    public void addObserver(PropertyChangeListener propertyChangeListener) {
        changeSupport.addPropertyChangeListener(propertyChangeListener);
    }

    public void deleteObserver(PropertyChangeListener propertyChangeListener) {
        changeSupport.removePropertyChangeListener(propertyChangeListener);
    }


    public static ProviderSetupObservable getInstance() {
        if (instance == null) {
            instance = new ProviderSetupObservable();
        }
        return instance;
    }

    public static void storeLastResult(int resultCode, Bundle resultData) {
        if (getInstance().canceled) {
            return;
        }
        getInstance().resultCode = resultCode;
        getInstance().resultData = resultData;
    }

    public static void updateProgress(int progress) {
        if (getInstance().canceled) {
            return;
        }
        long now = System.currentTimeMillis();
        getInstance().handler.postDelayed(() -> {
            if (TorStatusObservable.isRunning()) {
                getInstance().progress = (TorStatusObservable.getBootstrapProgress() + progress) / 2;
            } else {
                getInstance().progress = progress;
            }

            getInstance().changeSupport.firePropertyChange(PROPERTY_CHANGE, null, getInstance());

        }, now - getInstance().lastUpdate < 500L ? 500L : 0L);
        getInstance().lastUpdate = System.currentTimeMillis() + 500;
    }

    public static void updateTorSetupProgress() {
        if (!TorStatusObservable.isRunning() || getInstance().canceled) {
            return;
        }
        long now = System.currentTimeMillis();
        getInstance().handler.postDelayed(() -> {
            getInstance().progress = (TorStatusObservable.getBootstrapProgress()) / 2;

            getInstance().changeSupport.firePropertyChange(PROPERTY_CHANGE, null, getInstance());
        }, now - getInstance().lastUpdate < 500L ? 500L : 0);
        getInstance().lastUpdate = System.currentTimeMillis() + 500;
    }

    public static int getProgress() {
        return getInstance().progress;
    }

    public static boolean isSetupRunning() {
        return getInstance().progress > 0;
    }

    public static void reset() {
        getInstance().progress = 0;
        getInstance().resultCode = 0;
        getInstance().resultData = new Bundle();
        getInstance().changeSupport.firePropertyChange(PROPERTY_CHANGE, null, getInstance());
    }

    public static void cancel() {
        getInstance().canceled = true;
        reset();
    }

    public static boolean isCanceled() {
        return getInstance().canceled;
    }

    public static void startSetup() {
        getInstance().canceled = false;
        getInstance().resultCode = 0;
        getInstance().progress = 1;
        getInstance().resultData = new Bundle();
    }

    public static int getResultCode() {
        return getInstance().resultCode;
    }

    public static Bundle getResultData() {
        return getInstance().resultData;
    }
}
