/**
 * Copyright (c) 2018 LEAP Encryption Access Project and contributers
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

package se.leap.bitmaskclient.eip;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;

import se.leap.bitmaskclient.ConfigHelper;
import se.leap.bitmaskclient.Provider;
import se.leap.bitmaskclient.ProviderAPI;
import se.leap.bitmaskclient.ProviderApiConnector;
import se.leap.bitmaskclient.ProviderApiManager;
import se.leap.bitmaskclient.ProviderApiManagerBase;
import se.leap.bitmaskclient.testutils.MockSharedPreferences;

import static se.leap.bitmaskclient.Constants.BROADCAST_RESULT_KEY;
import static se.leap.bitmaskclient.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.ProviderAPI.ERRORS;
import static se.leap.bitmaskclient.ProviderAPI.PROVIDER_NOK;
import static se.leap.bitmaskclient.ProviderAPI.PROVIDER_OK;
import static se.leap.bitmaskclient.testutils.BackendMockResponses.BackendMockProvider.TestBackendErrorCase.ERROR_CASE_UPDATED_CERTIFICATE;
import static se.leap.bitmaskclient.testutils.BackendMockResponses.BackendMockProvider.TestBackendErrorCase.NO_ERROR;
import static se.leap.bitmaskclient.testutils.MockHelper.mockBundle;
import static se.leap.bitmaskclient.testutils.MockHelper.mockClientGenerator;
import static se.leap.bitmaskclient.testutils.MockHelper.mockConfigHelper;
import static se.leap.bitmaskclient.testutils.MockHelper.mockFingerprintForCertificate;
import static se.leap.bitmaskclient.testutils.MockHelper.mockIntent;
import static se.leap.bitmaskclient.testutils.MockHelper.mockProviderApiConnector;
import static se.leap.bitmaskclient.testutils.MockHelper.mockResources;
import static se.leap.bitmaskclient.testutils.MockHelper.mockResultReceiver;
import static se.leap.bitmaskclient.testutils.MockHelper.mockTextUtils;
import static se.leap.bitmaskclient.testutils.TestSetupHelper.getInputAsString;


/**
 * Created by cyberta on 04.01.18.
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({ProviderApiManager.class, TextUtils.class, ConfigHelper.class, ProviderApiConnector.class})
public class ProviderApiManagerTest {

    private SharedPreferences mockPreferences;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Resources mockResources;
    @Mock(answer =  Answers.RETURNS_DEEP_STUBS)
    private Context mockContext;

    private ProviderApiManager providerApiManager;

    class TestProviderApiServiceCallback implements ProviderApiManagerBase.ProviderApiServiceCallback {

        //Intent expectedIntent;
        TestProviderApiServiceCallback(/*Intent expectedIntent*/) {
            //this.expectedIntent = expectedIntent;
        }

        @Override
        public void broadcastEvent(Intent intent) {
            //assertEquals("expected intent: ", expectedIntent, intent);
        }
    }

    @Before
    public void setUp() throws Exception {

        Bundle bundle = mockBundle();
        PowerMockito.whenNew(Bundle.class).withAnyArguments().thenReturn(bundle);
        Intent intent = mockIntent();
        PowerMockito.whenNew(Intent.class).withAnyArguments().thenReturn(intent);
        mockTextUtils();
        mockPreferences = new MockSharedPreferences();
        mockResources = mockResources(getClass().getClassLoader().getResourceAsStream("error_messages.json"));
    }

    private Provider getConfiguredProvider() throws IOException, JSONException {
        return getProvider(null, null, null);
    }

    private Provider getProvider(String domain, String caCertFile, String jsonFile) {
        if (domain == null)
            domain = "https://riseup.net";
        if (caCertFile == null)
            caCertFile = "riseup.net.pem";
        if (jsonFile == null)
            jsonFile = "riseup.net.json";

        try {
            return new Provider(
                    new URL(domain),
                    getInputAsString(getClass().getClassLoader().getResourceAsStream(caCertFile)),
                    getInputAsString(getClass().getClassLoader().getResourceAsStream(jsonFile))

            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Test
    public void test_handleIntentSetupProvider_noProviderMainURL() throws IOException, JSONException {
        Provider provider = new Provider("");

        providerApiManager = new ProviderApiManager(mockPreferences, mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());
        Bundle expectedResult = mockBundle();

        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errors\":\"It doesn't seem to be a Bitmask provider.\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();
        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_NOK, expectedResult));
        providerApiCommand.putExtra(PROVIDER_KEY, provider);

        providerApiManager.handleIntent(providerApiCommand);
    }

    @Test
    public void test_handleIntentSetupProvider_happyPath_preseededProviderAndCA() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = getConfiguredProvider();

        mockFingerprintForCertificate(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockProviderApiConnector(NO_ERROR);
        providerApiManager = new ProviderApiManager(mockPreferences, mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());
        Bundle expectedResult = mockBundle();

        expectedResult.putBoolean(BROADCAST_RESULT_KEY, true);
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();

        providerApiCommand.putExtra(PROVIDER_KEY, provider);
        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_OK, expectedResult));

        providerApiManager.handleIntent(providerApiCommand);
    }

    @Test
    public void test_handleIntentSetupProvider_happyPath_no_preseededProviderAndCA() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = new Provider("https://riseup.net");

        mockFingerprintForCertificate("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockProviderApiConnector(NO_ERROR);
        providerApiManager = new ProviderApiManager(mockPreferences, mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());
        Bundle expectedResult = mockBundle();

        expectedResult.putBoolean(BROADCAST_RESULT_KEY, true);
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();

        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_OK, expectedResult));
        providerApiCommand.putExtra(PROVIDER_KEY, provider);

        providerApiManager.handleIntent(providerApiCommand);
    }

    @Test
    public void test_handleIntentSetupProvider_happyPath_storedProviderAndCAFromPreviousSetup() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = new Provider("https://riseup.net");
        mockConfigHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494", getConfiguredProvider());

        mockProviderApiConnector(NO_ERROR);
        mockPreferences.edit().putString(Provider.KEY + ".riseup.net", getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.json"))).apply();
        mockPreferences.edit().putString(Provider.CA_CERT + ".riseup.net", getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.pem"))).apply();
        providerApiManager = new ProviderApiManager(mockPreferences, mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, true);
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();
        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_OK, expectedResult));

        providerApiCommand.putExtra(PROVIDER_KEY, provider);

        providerApiManager.handleIntent(providerApiCommand);
    }

    @Test
    public void test_handleIntentSetupProvider_preseededProviderAndCA_failedCAPinning() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = getConfiguredProvider();
        mockFingerprintForCertificate(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29495");
        mockProviderApiConnector(NO_ERROR);
        providerApiManager = new ProviderApiManager(mockPreferences, mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());
        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errorId\":\"ERROR_CERTIFICATE_PINNING\",\"errors\":\"Stored provider certificate is invalid. You can either update Bitmask (recommended) or update the provider certificate using a commercial CA certificate.\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();

        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_NOK, expectedResult));

        providerApiCommand.putExtra(PROVIDER_KEY, provider);

        providerApiManager.handleIntent(providerApiCommand);
    }

    @Test
    public void test_handleIntentSetupProvider_no_preseededProviderAndCA_failedPinning() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = new Provider("https://riseup.net");
        mockFingerprintForCertificate("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29495");
        mockProviderApiConnector(NO_ERROR);
        providerApiManager = new ProviderApiManager(mockPreferences, mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errorId\":\"ERROR_CERTIFICATE_PINNING\",\"errors\":\"Stored provider certificate is invalid. You can either update Bitmask (recommended) or update the provider certificate using a commercial CA certificate.\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();

        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_NOK, expectedResult));

        providerApiCommand.putExtra(PROVIDER_KEY, provider);

        providerApiManager.handleIntent(providerApiCommand);
    }

    @Test
    public void test_handleIntentSetupProvider_storedProviderAndCAFromPreviousSetup_failedPinning() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = new Provider("https://riseup.net");
        mockConfigHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29495", getConfiguredProvider());

        mockProviderApiConnector(NO_ERROR);
        mockPreferences.edit().putString(Provider.KEY + ".riseup.net", getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.json"))).apply();
        mockPreferences.edit().putString(Provider.CA_CERT + ".riseup.net", getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.pem"))).apply();
        providerApiManager = new ProviderApiManager(mockPreferences, mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errorId\":\"ERROR_CERTIFICATE_PINNING\",\"errors\":\"Stored provider certificate is invalid. You can either update Bitmask (recommended) or update the provider certificate using a commercial CA certificate.\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();

        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_NOK, expectedResult));

        providerApiCommand.putExtra(PROVIDER_KEY, provider);

        providerApiManager.handleIntent(providerApiCommand);
    }


    @Test
    public void test_handleIntentSetupProvider_preseededProviderAndCA_outdatedCertificate() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = getProvider(null ,"outdated_cert.pem", null);
        mockProviderApiConnector(NO_ERROR);
        providerApiManager = new ProviderApiManager(mockPreferences, mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errorId\":\"ERROR_INVALID_CERTIFICATE\",\"errors\":\"Stored provider certificate is expired. You can either update Bitmask (recommended) or update the provider certificate using a commercial CA certificate.\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();

        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_NOK, expectedResult));

        providerApiCommand.putExtra(PROVIDER_KEY, provider);

        providerApiManager.handleIntent(providerApiCommand);
    }

    @Test
    public void test_handleIntentSetupProvider_storedProviderAndCAFromPreviousSetup_outdatedCertificate() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = new Provider("https://riseup.net");
        mockProviderApiConnector(NO_ERROR);
        mockPreferences.edit().putString(Provider.KEY + ".riseup.net", getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.json"))).apply();
        mockPreferences.edit().putString(Provider.CA_CERT + ".riseup.net", getInputAsString(getClass().getClassLoader().getResourceAsStream("outdated_cert.pem"))).apply();
        providerApiManager = new ProviderApiManager(mockPreferences, mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errorId\":\"ERROR_INVALID_CERTIFICATE\",\"errors\":\"Stored provider certificate is expired. You can either update Bitmask (recommended) or update the provider certificate using a commercial CA certificate.\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();

        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_NOK, expectedResult));

        providerApiCommand.putExtra(PROVIDER_KEY, provider);

        providerApiManager.handleIntent(providerApiCommand);
    }

    @Test
    public void test_handleIntentSetupProvider_preseededProviderAndCA_ValidCertificateButUpdatedCertificateOnServerSide() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = getConfiguredProvider();

        mockConfigHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494", getConfiguredProvider());
        mockProviderApiConnector(ERROR_CASE_UPDATED_CERTIFICATE);

        providerApiManager = new ProviderApiManager(mockPreferences, mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());
        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errorId\":\"ERROR_INVALID_CERTIFICATE\",\"errors\":\"Stored provider certificate is invalid. You can either update Bitmask (recommended) or update the provider certificate using a commercial CA certificate.\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();

        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_NOK, expectedResult));

        providerApiCommand.putExtra(PROVIDER_KEY, provider);

        providerApiManager.handleIntent(providerApiCommand);
    }

    @Test
    public void test_handleIntentSetupProvider_storedProviderAndCAFromPreviousSetup_ValidCertificateButUpdatedCertificateOnServerSide() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = new Provider("https://riseup.net");

        mockConfigHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494", getConfiguredProvider());
        mockProviderApiConnector(ERROR_CASE_UPDATED_CERTIFICATE);
        mockPreferences.edit().putString(Provider.KEY + ".riseup.net", getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.json"))).apply();
        mockPreferences.edit().putString(Provider.CA_CERT + ".riseup.net", getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.pem"))).apply();
        providerApiManager = new ProviderApiManager(mockPreferences, mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errorId\":\"ERROR_INVALID_CERTIFICATE\",\"errors\":\"Stored provider certificate is invalid. You can either update Bitmask (recommended) or update the provider certificate using a commercial CA certificate.\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();

        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_NOK, expectedResult));

        providerApiCommand.putExtra(PROVIDER_KEY, provider);

        providerApiManager.handleIntent(providerApiCommand);
    }
}
