package se.leap.bitmaskclient.base.utils;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import static org.junit.Assert.assertEquals;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(DataProviderRunner.class)
public class ConfigHelperTest {

    @DataProvider
    public static Object[][] dataProviderIPs() {
        // @formatter:off
        return new Object[][] {
                { "0.0.0.0", true },
                { "1.1.1.1", true },
                { "8.8.8.8", true },
                { "127.0.0.1", true },
                { "255.255.255.255", true },
                { "200.50.20.10", true },
                { "97.72.15.12", true },
                {"02.0.0.0", false},
                {"10.000.1.1", false},
                {"256.256.256.256", false},
                {"127..0.1", false},
                {"127.0..1", false},
                {"127.0.0.", false},
                {"127.0.0", false},
                {"255.255.255.255.255", false},
                {"", false},
                {null, false},
        };
    }


    @Test
    @UseDataProvider("dataProviderIPs")
    public void testisIPv4_validIPs_returnsTrue(String ip, boolean isValidExpected) {
        assertEquals(isValidExpected, ConfigHelper.isIPv4(ip));
    }
}