package se.leap.bitmaskclient.firewall;
/**
 * Copyright (c) 2019 LEAP Encryption Access Project and contributers
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

import de.blinkt.openvpn.core.VpnStatus;
import se.leap.bitmaskclient.utils.PreferenceHelper;


public class FirewallManager implements FirewallCallback {
    public static String BITMASK_CHAIN = "bitmask_fw";
    public static String BITMASK_FORWARD = "bitmask_forward";
    public static String BITMASK_POSTROUTING = "bitmask_postrouting";
    static final String TAG = FirewallManager.class.getSimpleName();

    private Context context;

    public FirewallManager(Context context) {
        this.context = context;
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
    public void onTetheringConfigured(boolean success) {
        if (success) {
            VpnStatus.logInfo("[FIREWALL] Rules for tethering configured");
        } else {
            VpnStatus.logError("[FIREWALL] Could not establish rules for tethering ");
        }
    }

    @Override
    public void onSuRequested(boolean success) {
        PreferenceHelper.setSuPermission(context, success);
        if (!success) {
            VpnStatus.logError("[FIREWALL] Root permission needed to execute custom firewall rules.");
        }
    }


    public void startFirewall() {
        StartFirewallTask task = new StartFirewallTask(this);
        task.execute();
    }

    public void shutdownFirewall() {
        ShutdownFirewallTask task = new ShutdownFirewallTask(this);
        task.execute();
    }

}
