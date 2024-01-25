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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

public class TetheringStateManagerTest {

    private TetheringObservable observable;

    @Before
    public void setup() throws Exception {
        observable = TetheringObservable.getInstance();
    }

    @Test
    public void testGetWifiAddressRangee_keepsLastSeenAddressAndInterface() throws Exception {
        //WifiTethering was switched on
        TetheringObservable.setWifiTethering(true, "192.168.40.0/24", "wlan0");

        assertEquals("192.168.40.0/24", observable.getTetheringState().wifiAddress);
        assertEquals("192.168.40.0/24", observable.getTetheringState().lastSeenWifiAddress);
        assertEquals("wlan0", observable.getTetheringState().wifiInterface);
        assertEquals("wlan0", observable.getTetheringState().lastSeenWifiInterface);
        //Wifi tethering was switched off
        TetheringObservable.setWifiTethering(true, "", "");
        assertEquals("", observable.getTetheringState().wifiAddress);
        assertEquals("192.168.40.0/24", observable.getTetheringState().lastSeenWifiAddress);
        assertEquals("", observable.getTetheringState().wifiInterface);
        assertEquals("wlan0", observable.getTetheringState().lastSeenWifiInterface);
    }

    @Test
    public void testGetUsbAddressRange_keepsLastSeenAddressAndInterface() throws Exception {
        WifiManagerWrapper mockWrapper = mock(WifiManagerWrapper.class);
        when(mockWrapper.isWifiAPEnabled()).thenReturn(true);
        PowerMockito.whenNew(WifiManagerWrapper.class).withAnyArguments().thenReturn(mockWrapper);

        //UsbTethering was switched on
        TetheringObservable.setUsbTethering(true, "192.168.40.0/24", "rndis0");
        assertEquals("192.168.40.0/24", observable.getTetheringState().usbAddress);
        assertEquals("192.168.40.0/24", observable.getTetheringState().lastSeenUsbAddress);
        assertEquals("rndis0", observable.getTetheringState().usbInterface);
        assertEquals("rndis0", observable.getTetheringState().lastSeenUsbInterface);
        //UsbTethering tethering was switched off
        TetheringObservable.setUsbTethering(true, "", "");
        assertEquals("", observable.getTetheringState().usbAddress);
        assertEquals("192.168.40.0/24", observable.getTetheringState().lastSeenUsbAddress);
        assertEquals("", observable.getTetheringState().usbInterface);
        assertEquals("rndis0", observable.getTetheringState().lastSeenUsbInterface);
    }


}