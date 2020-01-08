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
