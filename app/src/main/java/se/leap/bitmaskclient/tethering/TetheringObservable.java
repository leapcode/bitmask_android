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

    private boolean isWifiTetheringEnabled;
    private boolean isUsbTetheringEnabled;
    private boolean isBluetoothTetheringEnabled;

    private TetheringObservable() { }

    public static TetheringObservable getInstance() {
        if (instance == null) {
            instance = new TetheringObservable();
        }
        return instance;
    }

    static void setWifiTethering(boolean enabled) {
        getInstance().isWifiTetheringEnabled = enabled;
        getInstance().setChanged();
        getInstance().notifyObservers();
    }

    static void setUsbTethering(boolean enabled) {
        getInstance().isUsbTetheringEnabled = enabled;
        getInstance().setChanged();
        getInstance().notifyObservers();
    }

    static void setBluetoothTethering(boolean enabled) {
        getInstance().isBluetoothTetheringEnabled = enabled;
        getInstance().setChanged();
        getInstance().notifyObservers();
    }

    public boolean isBluetoothTetheringEnabled() {
        return isBluetoothTetheringEnabled;
    }

    public boolean isUsbTetheringEnabled() {
        return isUsbTetheringEnabled;
    }

    public boolean isWifiTetheringEnabled() {
        return isWifiTetheringEnabled;
    }
}
