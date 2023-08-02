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

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

import se.leap.bitmaskclient.base.utils.Cmd;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.testutils.MockSharedPreferences;


@RunWith(PowerMockRunner.class)
@PrepareForTest({WifiManagerWrapper.class, TetheringStateManager.class, Cmd.class, NetworkInterface.class, PreferenceHelper.class})
public class TetheringStateManagerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Context mockContext;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    IntentFilter intentFilter;

    private TetheringObservable observable;

    @Before
    public void setup() throws Exception {
        PowerMockito.whenNew(IntentFilter.class).withArguments(anyString()).thenReturn(intentFilter);
        PowerMockito.whenNew(IntentFilter.class).withNoArguments().thenReturn(intentFilter);
        mockStatic(PreferenceHelper.class);

        observable = TetheringObservable.getInstance();

    }

    @Test
    public void updateUsbTetheringState_findsRndisX_returnsTrue() throws Exception {
        WifiManagerWrapper mockWrapper = mock(WifiManagerWrapper.class);
        when(mockWrapper.isWifiAPEnabled()).thenReturn(false);
        PowerMockito.whenNew(WifiManagerWrapper.class).withAnyArguments().thenReturn(mockWrapper);

        PowerMockito.mockStatic(NetworkInterface.class);
        NetworkInterface mock1 = PowerMockito.mock(NetworkInterface.class);
        when(mock1.isLoopback()).thenReturn(false);
        when(mock1.getName()).thenReturn("eth0");
        NetworkInterface mock2 = PowerMockito.mock(NetworkInterface.class);
        when(mock2.isLoopback()).thenReturn(false);
        when(mock2.getName()).thenReturn("rndis0");

        NetworkInterface[] networkInterfaces = new NetworkInterface[2];
        networkInterfaces[0] = mock1;
        networkInterfaces[1] = mock2;

        PowerMockito.when(NetworkInterface.getNetworkInterfaces()).then(new Answer<Enumeration<NetworkInterface>>() {
            @Override
            public Enumeration<NetworkInterface> answer(InvocationOnMock invocation) throws Throwable {
                return Collections.enumeration(Arrays.asList(networkInterfaces));
            }
        });

        TetheringObservable.setUsbTethering(false, "192.168.42.0/24", "rndis0");
        TetheringStateManager.getInstance().init(mockContext);
        TetheringStateManager manager = TetheringStateManager.getInstance();
        assertTrue(observable.isUsbTetheringEnabled());
    }

    @Test
    public void updateUsbTetheringState_doesntFindRndisX_returnsFalse() throws Exception {
        WifiManagerWrapper mockWrapper = mock(WifiManagerWrapper.class);
        when(mockWrapper.isWifiAPEnabled()).thenReturn(false);
        PowerMockito.whenNew(WifiManagerWrapper.class).withAnyArguments().thenReturn(mockWrapper);

        PowerMockito.mockStatic(NetworkInterface.class);
        NetworkInterface mock1 = PowerMockito.mock(NetworkInterface.class);
        when(mock1.isLoopback()).thenReturn(false);
        when(mock1.getName()).thenReturn("eth0");
        NetworkInterface mock2 = PowerMockito.mock(NetworkInterface.class);
        when(mock2.isLoopback()).thenReturn(false);
        when(mock2.getName()).thenReturn("wifi0");

        NetworkInterface[] networkInterfaces = new NetworkInterface[2];
        networkInterfaces[0] = mock1;
        networkInterfaces[1] = mock2;

        PowerMockito.when(NetworkInterface.getNetworkInterfaces()).then(new Answer<Enumeration<NetworkInterface>>() {
            @Override
            public Enumeration<NetworkInterface> answer(InvocationOnMock invocation) throws Throwable {
                return Collections.enumeration(Arrays.asList(networkInterfaces));
            }
        });

        TetheringObservable.setUsbTethering(true, "192.168.42.0/24", "rndis0");
        TetheringStateManager.getInstance().init(mockContext);
        TetheringStateManager manager = TetheringStateManager.getInstance();
        assertFalse(observable.isUsbTetheringEnabled());
    }

    @Test
    public void updateUsbTetheringState_ThrowsException_returnsFalse() throws Exception {
        WifiManagerWrapper mockWrapper = mock(WifiManagerWrapper.class);
        when(mockWrapper.isWifiAPEnabled()).thenReturn(false);
        PowerMockito.whenNew(WifiManagerWrapper.class).withAnyArguments().thenReturn(mockWrapper);

        PowerMockito.mockStatic(NetworkInterface.class);
        PowerMockito.when(NetworkInterface.getNetworkInterfaces()).thenThrow(new SocketException());

        TetheringObservable.setUsbTethering(true, "192.168.42.0/24", "rndis0");
        TetheringStateManager.getInstance().init(mockContext);
        TetheringStateManager manager = TetheringStateManager.getInstance();
        assertFalse(observable.isUsbTetheringEnabled());
    }

/* //TODO enable these tests as soon as bluetooth tethering has been enabled again
    @Test
    public void updateBluetoothTetheringState_btDeviceFound_returnTrue() throws Exception {
        WifiManagerWrapper mockWrapper = mock(WifiManagerWrapper.class);
        when(mockWrapper.isWifiAPEnabled()).thenReturn(true);
        PowerMockito.whenNew(WifiManagerWrapper.class).withAnyArguments().thenReturn(mockWrapper);

        mockStatic(Cmd.class);
        PowerMockito.when(Cmd.runBlockingCmd(any(), any(StringBuilder.class))).then(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                StringBuilder logStringBuilder = invocation.getArgument(1);
                logStringBuilder.append("bt-pan device found");
                return 0;
            }
        });

        TetheringObservable.setBluetoothTethering(false);
        TetheringStateManager.getInstance().init(mockContext);
        TetheringStateManager manager = TetheringStateManager.getInstance();
        assertTrue(observable.isBluetoothTetheringEnabled());
    }


    @Test
    public void updateBluetoothTetheringState_btPanDeviceNotFound_returnFalse() throws Exception {
        WifiManagerWrapper mockWrapper = mock(WifiManagerWrapper.class);
        when(mockWrapper.isWifiAPEnabled()).thenReturn(true);
        PowerMockito.whenNew(WifiManagerWrapper.class).withAnyArguments().thenReturn(mockWrapper);

        mockStatic(Cmd.class);
        PowerMockito.when(Cmd.runBlockingCmd(any(), any(StringBuilder.class))).then(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                StringBuilder logStringBuilder = invocation.getArgument(1);
                logStringBuilder.append("bt-pan device not found");
                return 1;
            }
        });

        TetheringObservable.setBluetoothTethering(true);
        TetheringStateManager.getInstance().init(mockContext);
        TetheringStateManager manager = TetheringStateManager.getInstance();
        assertFalse(observable.isBluetoothTetheringEnabled());
    }

    @Test
    public void updateBluetoothTetheringState_ThrowsException_returnsFalse() throws Exception {
        WifiManagerWrapper mockWrapper = mock(WifiManagerWrapper.class);
        when(mockWrapper.isWifiAPEnabled()).thenReturn(true);
        PowerMockito.whenNew(WifiManagerWrapper.class).withAnyArguments().thenReturn(mockWrapper);

        mockStatic(Cmd.class);
        PowerMockito.when(Cmd.runBlockingCmd(any(), any(StringBuilder.class))).
                thenThrow(new SecurityException("Creation of subprocess is not allowed"));

        TetheringObservable.setBluetoothTethering(true);
        TetheringStateManager.getInstance().init(mockContext);
        TetheringStateManager manager = TetheringStateManager.getInstance();
        assertFalse(observable.isBluetoothTetheringEnabled());
    }

    @Test
    public void updateBluetoothTetheringState_WifiManagerWrapperThrowsException_hasNoInfluenceOnResult() throws Exception {
        WifiManagerWrapper mockWrapper = mock(WifiManagerWrapper.class);
        when(mockWrapper.isWifiAPEnabled()).thenThrow(new NoSuchMethodException());
        PowerMockito.whenNew(WifiManagerWrapper.class).withAnyArguments().thenReturn(mockWrapper);

        mockStatic(Cmd.class);
        PowerMockito.when(Cmd.runBlockingCmd(any(), any(StringBuilder.class))).then(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                StringBuilder logStringBuilder = invocation.getArgument(1);
                logStringBuilder.append("bt-pan device found");
                return 0;
            }
        });

        TetheringObservable.setBluetoothTethering(false);
        TetheringStateManager.getInstance().init(mockContext);
        TetheringStateManager manager = TetheringStateManager.getInstance();
        assertTrue(observable.isBluetoothTetheringEnabled());
    }
    */

    @Test
    public void updateWifiTetheringState_ignoreFailingWifiAPReflection_keepsOldValueTrue() throws Exception {
        WifiManagerWrapper mockWrapper = mock(WifiManagerWrapper.class);
        when(mockWrapper.isWifiAPEnabled()).thenThrow(new NoSuchMethodException());
        PowerMockito.whenNew(WifiManagerWrapper.class).withAnyArguments().thenReturn(mockWrapper);

        TetheringObservable.setWifiTethering(true, "192.168.43.0/24", "wlan0");
        TetheringStateManager.getInstance().init(mockContext);
        TetheringStateManager manager = TetheringStateManager.getInstance();
        assertTrue(observable.isWifiTetheringEnabled());
    }

    @Test
    public void updateWifiTetheringState_ignoreFailingWifiAPReflection_keepsOldValueFalse() throws Exception {
        WifiManagerWrapper mockWrapper = mock(WifiManagerWrapper.class);
        when(mockWrapper.isWifiAPEnabled()).thenThrow(new NoSuchMethodException());
        PowerMockito.whenNew(WifiManagerWrapper.class).withAnyArguments().thenReturn(mockWrapper);

        TetheringObservable.setWifiTethering(false, "", "");
        TetheringStateManager.getInstance().init(mockContext);
        TetheringStateManager manager = TetheringStateManager.getInstance();
        assertFalse(observable.isWifiTetheringEnabled());
    }

    @Test
    public void updateWifiTetheringState_WifiApReflectionWithoutException_changeValueToTrue() throws Exception {
        WifiManagerWrapper mockWrapper = mock(WifiManagerWrapper.class);
        when(mockWrapper.isWifiAPEnabled()).thenReturn(true);
        PowerMockito.whenNew(WifiManagerWrapper.class).withAnyArguments().thenReturn(mockWrapper);

        TetheringObservable.setWifiTethering(false, "", "");
        TetheringStateManager.getInstance().init(mockContext);
        TetheringStateManager manager = TetheringStateManager.getInstance();
        assertTrue(observable.isWifiTetheringEnabled());
    }

    @Test
    public void updateWifiTetheringState_WifiApReflectionWithoutException_changeValueToFalse() throws Exception {
        WifiManagerWrapper mockWrapper = mock(WifiManagerWrapper.class);
        when(mockWrapper.isWifiAPEnabled()).thenReturn(false);
        PowerMockito.whenNew(WifiManagerWrapper.class).withAnyArguments().thenReturn(mockWrapper);

        TetheringObservable.setWifiTethering(true, "", "");
        TetheringStateManager.getInstance().init(mockContext);
        TetheringStateManager manager = TetheringStateManager.getInstance();
        assertFalse(observable.isWifiTetheringEnabled());
    }

    @Test
    public void testGetWifiAddressRangee_keepsLastSeenAddressAndInterface() throws Exception {
        WifiManagerWrapper mockWrapper = mock(WifiManagerWrapper.class);
        when(mockWrapper.isWifiAPEnabled()).thenReturn(true);
        PowerMockito.whenNew(WifiManagerWrapper.class).withAnyArguments().thenReturn(mockWrapper);

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