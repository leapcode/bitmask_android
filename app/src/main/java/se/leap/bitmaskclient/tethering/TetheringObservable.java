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

import androidx.annotation.NonNull;

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
        if (getInstance().tetheringState.isVpnUsbTetheringAllowed != enabled) {
            getInstance().tetheringState.isVpnUsbTetheringAllowed = enabled;
            getInstance().setChanged();
            getInstance().notifyObservers();
        }
    }

    public static void allowVpnBluetoothTethering(boolean enabled) {
        if (getInstance().tetheringState.isVpnBluetoothTetheringAllowed != enabled) {
            getInstance().tetheringState.isVpnBluetoothTetheringAllowed = enabled;
            getInstance().setChanged();
            getInstance().notifyObservers();
        }
    }

    static void setWifiTethering(boolean enabled, @NonNull String address, @NonNull String interfaceName) {
        if (getInstance().tetheringState.isWifiTetheringEnabled != enabled ||
                !getInstance().tetheringState.wifiInterface.equals(interfaceName) ||
                !getInstance().tetheringState.wifiAddress.equals(address)) {
            TetheringState state = getInstance().tetheringState;
            state.isWifiTetheringEnabled = enabled;
            state.wifiInterface = interfaceName;
            state.wifiAddress = address;
            state.lastSeenWifiAddress = address.isEmpty() ? state.lastSeenWifiAddress : address;
            state.lastSeenWifiInterface = interfaceName.isEmpty() ? state.lastSeenWifiInterface : interfaceName;
            getInstance().setChanged();
            getInstance().notifyObservers();
        }

    }

    static void setUsbTethering(boolean enabled, @NonNull String address, @NonNull String interfaceName) {
        if (getInstance().tetheringState.isUsbTetheringEnabled != enabled ||
            !getInstance().tetheringState.usbAddress.equals(address) ||
                !getInstance().tetheringState.usbInterface.equals(interfaceName)) {
            TetheringState state = getInstance().tetheringState;
            state.isUsbTetheringEnabled = enabled;
            state.usbAddress = address;
            state.usbInterface = interfaceName;
            state.lastSeenUsbAddress = address.isEmpty() ? state.lastSeenUsbAddress : address;
            state.lastSeenUsbInterface = interfaceName.isEmpty() ? state.lastSeenUsbInterface : interfaceName;
            getInstance().setChanged();
            getInstance().notifyObservers();
        }
    }

    static void setBluetoothTethering(boolean enabled, @NonNull String address, @NonNull String interfaceName) {
        if (getInstance().tetheringState.isBluetoothTetheringEnabled != enabled ||
                !getInstance().tetheringState.bluetoothAddress.equals(address) ||
                !getInstance().tetheringState.bluetoothInterface.equals(interfaceName)) {
            TetheringState state = getInstance().tetheringState;
            state.isBluetoothTetheringEnabled = enabled;
            state.bluetoothAddress = address;
            state.bluetoothInterface = interfaceName;
            state.lastSeenBluetoothAddress = address.isEmpty() ? state.lastSeenBluetoothAddress : address;
            state.lastSeenBluetoothInterface = interfaceName.isEmpty() ? state.lastSeenBluetoothInterface : interfaceName;
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
