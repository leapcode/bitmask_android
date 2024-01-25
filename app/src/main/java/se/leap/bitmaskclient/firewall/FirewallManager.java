package se.leap.bitmaskclient.firewall;
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

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import de.blinkt.openvpn.core.VpnStatus;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.tethering.TetheringObservable;
import se.leap.bitmaskclient.tethering.TetheringState;

public class FirewallManager implements FirewallCallback, PropertyChangeListener {
    public static String BITMASK_CHAIN = "bitmask_fw";
    public static String BITMASK_FORWARD = "bitmask_forward";
    public static String BITMASK_POSTROUTING = "bitmask_postrouting";
    static final String TAG = FirewallManager.class.getSimpleName();
    private boolean isRunning = false;

    private Context context;

    public FirewallManager(Context context, boolean observeTethering) {
        this.context = context;
        if (observeTethering) {
            TetheringObservable.getInstance().addObserver(this);
        }
    }

    @Override
    public void onFirewallStarted(boolean success) {
        if (success) {
            VpnStatus.logInfo("[FIREWALL] Custom rules established");
        } else {
            VpnStatus.logError("[FIREWALL] Could not establish custom rules.");
        }
    }

    @Override
    public void onFirewallStopped(boolean success) {
        if (success) {
            VpnStatus.logInfo("[FIREWALL] Custom rules deleted");
        } else {
            VpnStatus.logError("[FIREWALL] Could not delete custom rules");
        }
    }

    @Override
    public void onTetheringStarted(boolean success) {
        if (success) {
            VpnStatus.logInfo("[FIREWALL] Rules for tethering enabled");
        } else {
            VpnStatus.logError("[FIREWALL] Could not enable rules for tethering.");
        }
    }

    @Override
    public void onTetheringStopped(boolean success) {
        if (success) {
            VpnStatus.logInfo("[FIREWALL] Rules for tethering successfully disabled");
        } else {
            VpnStatus.logError("[FIREWALL] Could not disable rules for tethering.");
        }
    }

    @Override
    public void onSuRequested(boolean success) {
        if (!success) {
            VpnStatus.logError("[FIREWALL] Root permission needed to execute custom firewall rules.");
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(context.getApplicationContext(), context.getString(R.string.root_permission_error, context.getString(R.string.app_name)), Toast.LENGTH_LONG).show();
            });
            TetheringObservable.allowVpnWifiTethering(false);
            TetheringObservable.allowVpnUsbTethering(false);
            TetheringObservable.allowVpnBluetoothTethering(false);
            PreferenceHelper.allowWifiTethering(false);
            PreferenceHelper.allowUsbTethering(false);
            PreferenceHelper.allowBluetoothTethering(false);
            PreferenceHelper.setUseIPv6Firewall(false);
        }
    }

    public void onDestroy() {
        TetheringObservable.getInstance().deleteObserver(this);
    }


    public void start() {
        if (!isRunning) {
            isRunning = true;
            if (PreferenceHelper.useIpv6Firewall()) {
                startIPv6Firewall();
            }
            TetheringState tetheringState = TetheringObservable.getInstance().getTetheringState();
            if (tetheringState.hasAnyDeviceTetheringEnabled() && tetheringState.hasAnyVpnTetheringAllowed()) {
                startTethering();
            }
        }

    }

    public void stop() {
        isRunning = false;
        if (PreferenceHelper.useIpv6Firewall()) {
            stopIPv6Firewall();
        }
        TetheringState tetheringState = TetheringObservable.getInstance().getTetheringState();
        if (tetheringState.hasAnyDeviceTetheringEnabled() && tetheringState.hasAnyVpnTetheringAllowed()) {
            stopTethering();
        }
    }

    public void startTethering() {
        SetupTetheringTask task = new SetupTetheringTask(this);
        task.execute();
    }

    public void stopTethering() {
        ShutdownTetheringTask task = new ShutdownTetheringTask(this);
        task.execute();
    }

    public void startIPv6Firewall() {
        StartIPv6FirewallTask task = new StartIPv6FirewallTask(this);
        task.execute();
    }

    public void stopIPv6Firewall() {
        ShutdownIPv6FirewallTask task = new ShutdownIPv6FirewallTask(this);
        task.execute();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (TetheringObservable.PROPERTY_CHANGE.equals(evt.getPropertyName())) {
            TetheringObservable observable = (TetheringObservable) evt.getNewValue();
            TetheringState state = observable.getTetheringState();
            if (state.hasAnyVpnTetheringAllowed() && state.hasAnyDeviceTetheringEnabled()) {
                startTethering();
            } else {
                stopTethering();
            }
        }
    }
}
