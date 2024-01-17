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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class TetheringObservable {
    private static TetheringObservable instance;

    private TetheringState tetheringState;
    private final PropertyChangeSupport changeSupport;
    public static final String PROPERTY_CHANGE = "TetheringObservable";

    private TetheringObservable() {
        tetheringState = new TetheringState();
        changeSupport = new PropertyChangeSupport(this);
    }

    public static TetheringObservable getInstance() {
        if (instance == null) {
            instance = new TetheringObservable();
        }
        return instance;
    }

    public void addObserver(PropertyChangeListener propertyChangeListener) {
        changeSupport.addPropertyChangeListener(propertyChangeListener);
    }

    public void deleteObserver(PropertyChangeListener propertyChangeListener) {
        changeSupport.removePropertyChangeListener(propertyChangeListener);
    }

    public static void allowVpnWifiTethering(boolean enabled) {
        if (getInstance().tetheringState.isVpnWifiTetheringAllowed != enabled) {
            getInstance().tetheringState.isVpnWifiTetheringAllowed = enabled;
            getInstance().notifyObservers();
        }
    }

    public static void allowVpnUsbTethering(boolean enabled) {
        if (getInstance().tetheringState.isVpnUsbTetheringAllowed != enabled) {
            getInstance().tetheringState.isVpnUsbTetheringAllowed = enabled;
            getInstance().notifyObservers();
        }
    }

    public static void allowVpnBluetoothTethering(boolean enabled) {
        if (getInstance().tetheringState.isVpnBluetoothTetheringAllowed != enabled) {
            getInstance().tetheringState.isVpnBluetoothTetheringAllowed = enabled;
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

    private void notifyObservers() {
        changeSupport.firePropertyChange(PROPERTY_CHANGE, null, getInstance());
    }
}
