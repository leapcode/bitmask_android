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

import java.util.Observable;

public class TetheringObservable extends Observable {
    private static TetheringObservable instance;

    private TetheringState tetheringState;

    private TetheringObservable() {
        tetheringState = new TetheringState();
    }

    public static TetheringObservable getInstance() {
        if (instance == null) {
            instance = new TetheringObservable();
        }
        return instance;
    }

    public static void allowVpnWifiTethering(boolean enabled) {
        if (getInstance().tetheringState.isVpnWifiTetheringAllowed != enabled) {
            getInstance().tetheringState.isVpnWifiTetheringAllowed = enabled;
            getInstance().setChanged();
            getInstance().notifyObservers();
        }
    }

    public static void allowVpnUsbTethering(boolean enabled) {
        if (getInstance().tetheringState.isUsbTetheringEnabled != enabled) {
            getInstance().tetheringState.isUsbTetheringEnabled = enabled;
            getInstance().setChanged();
            getInstance().notifyObservers();
        }
    }

    public static void allowVpnBluetoothTethering(boolean enabled) {
        if (getInstance().tetheringState.isBluetoothTetheringEnabled != enabled) {
            getInstance().tetheringState.isBluetoothTetheringEnabled = enabled;
            getInstance().setChanged();
            getInstance().notifyObservers();
        }
    }

    static void setWifiTethering(boolean enabled, String address, String interfaceName) {
        if (getInstance().tetheringState.isWifiTetheringEnabled != enabled ||
                !getInstance().tetheringState.wifiInterface.equals(interfaceName) ||
                !getInstance().tetheringState.wifiAddress.equals(address)) {
            getInstance().tetheringState.isWifiTetheringEnabled = enabled;
            getInstance().tetheringState.wifiInterface = interfaceName;
            getInstance().tetheringState.wifiAddress = address;
            if ("".equals(address)) {
                getInstance().tetheringState.lastWifiAddress = address;
            }
            getInstance().setChanged();
            getInstance().notifyObservers();
        }

    }

    static void setUsbTethering(boolean enabled) {
        if (getInstance().tetheringState.isUsbTetheringEnabled != enabled) {
            getInstance().tetheringState.isUsbTetheringEnabled = enabled;
            getInstance().setChanged();
            getInstance().notifyObservers();
        }
    }

    static void setBluetoothTethering(boolean enabled) {
        if (getInstance().tetheringState.isBluetoothTetheringEnabled != enabled) {
            getInstance().tetheringState.isBluetoothTetheringEnabled = enabled;
            getInstance().setChanged();
            getInstance().notifyObservers();
        }
    }

    public boolean isBluetoothTetheringEnabled() {
        return tetheringState.isBluetoothTetheringEnabled;
    }

    public boolean isUsbTetheringEnabled() {
        return tetheringState.isUsbTetheringEnabled;
    }

    public boolean isWifiTetheringEnabled() {
        return tetheringState.isWifiTetheringEnabled;
    }

    public TetheringState getTetheringState() {
        return tetheringState;
    }
}
