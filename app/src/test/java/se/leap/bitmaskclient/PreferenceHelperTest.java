package se.leap.bitmaskclient;

import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;

import se.leap.bitmaskclient.testutils.MockSharedPreferences;
import se.leap.bitmaskclient.utils.ConfigHelper;
import se.leap.bitmaskclient.utils.PreferenceHelper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static se.leap.bitmaskclient.Constants.PROVIDER_CONFIGURED;
import static se.leap.bitmaskclient.Constants.PROVIDER_EIP_DEFINITION;
import static se.leap.bitmaskclient.Constants.PROVIDER_PRIVATE_KEY;
import static se.leap.bitmaskclient.Constants.PROVIDER_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.testutils.TestSetupHelper.getInputAsString;
import static se.leap.bitmaskclient.utils.PreferenceHelper.getSavedProviderFromSharedPreferences;
import static se.leap.bitmaskclient.utils.PreferenceHelper.providerInSharedPreferences;

/**
 * Created by cyberta on 17.01.18.
 */
public class PreferenceHelperTest {

    private SharedPreferences mockPreferences;


    @Before
    public void setup() {
        mockPreferences = new MockSharedPreferences();
    }

    @Test
    public void providerInSharedPreferences_notInPreferences_returnsFalse() throws Exception {
        assertFalse(providerInSharedPreferences(mockPreferences));
    }

    @Test
    public void providerInSharedPreferences_inPreferences_returnsTrue() throws Exception {
        mockPreferences.edit().putBoolean(PROVIDER_CONFIGURED, true).apply();
        assertTrue(providerInSharedPreferences(mockPreferences));
    }

    @Test
    public void getSavedProviderFromSharedPreferences_notInPreferences_returnsDefaultProvider() throws Exception {
        Provider provider = getSavedProviderFromSharedPreferences(mockPreferences);
        assertFalse(provider.isConfigured());
    }

    @Test
    public void getSavedProviderFromSharedPreferences_notInPreferences_returnsConfiguredProvider() throws Exception {
        mockPreferences.edit()
                .putString(Provider.KEY, getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.json")))
                .putString(Provider.MAIN_URL, "https://riseup.net")
                .putString(Provider.CA_CERT, getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.pem")))
                .putString(PROVIDER_EIP_DEFINITION, getInputAsString(getClass().getClassLoader().getResourceAsStream("eip-service-two-gateways.json")))
                .putString(PROVIDER_VPN_CERTIFICATE, getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.vpn_cert.pem")))
                .putString(PROVIDER_PRIVATE_KEY, getInputAsString(getClass().getClassLoader().getResourceAsStream("private_rsa_key.pem")))
                .apply();
        Provider provider = getSavedProviderFromSharedPreferences(mockPreferences);
        assertTrue(provider.isConfigured());
    }


}