package se.leap.bitmaskclient.tethering;

import de.blinkt.openvpn.core.VpnStatus;
import se.leap.bitmaskclient.eip.EipStatus;

public class TetheringState implements Cloneable {
    public boolean isWifiTetheringEnabled;
    public boolean isUsbTetheringEnabled;
    public boolean isBluetoothTetheringEnabled;
    public boolean isVpnWifiTetheringAllowed;
    public boolean isVpnUsbTetheringAllowed;
    public boolean isVpnBluetoothTetheringAllowed;
    public String wifiInterface = "";
    public String lastSeenWifiInterface = "";
    public String wifiAddress = "";
    public String lastSeenWifiAddress = "";
    public String usbInterface = "";
    public String lastSeenUsbInterface = "";
    public String usbAddress = "";
    public String lastSeenUsbAddress = "";
    public String bluetoothInterface = "";
    public String lastSeenBluetoothInterface = "";
    public String bluetoothAddress = "";
    public String lastSeenBluetoothAddress = "";


    public boolean tetherWifiVpn() {
        return isWifiTetheringEnabled && isVpnWifiTetheringAllowed;
    }

    public boolean tetherUsbVpn() {
        return isUsbTetheringEnabled && isVpnUsbTetheringAllowed;
    }

    public boolean tetherBluetoothVpn() {
        return isBluetoothTetheringEnabled && isVpnBluetoothTetheringAllowed;
    }

    public boolean hasAnyDeviceTetheringEnabled() {
        return isBluetoothTetheringEnabled || isUsbTetheringEnabled || isWifiTetheringEnabled;
    }

    public boolean hasAnyVpnTetheringAllowed() {
        return isVpnWifiTetheringAllowed || isVpnUsbTetheringAllowed || isVpnBluetoothTetheringAllowed;
    }

    public boolean isVpnTetheringRunning() {
        return (tetherWifiVpn() || tetherUsbVpn() || tetherBluetoothVpn()) && (EipStatus.getInstance().isConnecting() || EipStatus.getInstance().isConnected() || EipStatus.getInstance().isBlocking());
    }


}
