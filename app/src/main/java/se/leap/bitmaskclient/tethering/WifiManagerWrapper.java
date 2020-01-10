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
package se.leap.bitmaskclient.tethering;

import android.content.Context;
import android.net.wifi.WifiManager;

import java.lang.reflect.Method;

/**
 * This Wrapper allows better Unit testing.
 */
class WifiManagerWrapper {

    private WifiManager wifiManager;

    WifiManagerWrapper(Context context) {
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    boolean isWifiAPEnabled() throws Exception {
        Method method = wifiManager.getClass().getMethod("getWifiApState");
        int tmp = ((Integer) method.invoke(wifiManager));
        return WifiHotspotState.WIFI_AP_STATE_ENABLED.ordinal() == tmp % 10;
    }

}
