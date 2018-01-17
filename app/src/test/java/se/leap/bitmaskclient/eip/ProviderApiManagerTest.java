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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;

import se.leap.bitmaskclient.ConfigHelper;
import se.leap.bitmaskclient.Provider;
import se.leap.bitmaskclient.ProviderAPI;
import se.leap.bitmaskclient.ProviderApiConnector;
import se.leap.bitmaskclient.ProviderApiManager;
import se.leap.bitmaskclient.ProviderApiManagerBase;
import se.leap.bitmaskclient.testutils.MockSharedPreferences;

import static se.leap.bitmaskclient.ProviderAPI.ERRORS;
import static se.leap.bitmaskclient.ProviderAPI.PROVIDER_NOK;
import static se.leap.bitmaskclient.ProviderAPI.PROVIDER_OK;
import static se.leap.bitmaskclient.ProviderAPI.RESULT_KEY;
import static se.leap.bitmaskclient.testutils.BackendMockResponses.BackendMockProvider.TestBackendErrorCase.ERROR_CASE_UPDATED_CERTIFICATE;
import static se.leap.bitmaskclient.testutils.BackendMockResponses.BackendMockProvider.TestBackendErrorCase.NO_ERROR;
import static se.leap.bitmaskclient.testutils.TestSetupHelper.getInputAsString;
import static se.leap.bitmaskclient.testutils.TestSetupHelper.mockBundle;
import static se.leap.bitmaskclient.testutils.TestSetupHelper.mockClientGenerator;
import static se.leap.bitmaskclient.testutils.TestSetupHelper.mockFingerprintForCertificate;
import static se.leap.bitmaskclient.testutils.TestSetupHelper.mockIntent;
import static se.leap.bitmaskclient.testutils.TestSetupHelper.mockProviderApiConnector;
import static se.leap.bitmaskclient.testutils.TestSetupHelper.mockResources;
import static se.leap.bitmaskclient.testutils.TestSetupHelper.mockResultReceiver;
import static se.leap.bitmaskclient.testutils.TestSetupHelper.mockTextUtils;


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


    @Test
    public void test_handleIntentSetupProvider_noProviderMainURL() {
        providerApiManager = new ProviderApiManager(mockPreferences, mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());
        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errors\":\"It doesn't seem to be a Bitmask provider.\"}");

        Intent provider_API_command = mockIntent();
        Bundle parameters = mockBundle();
        parameters.putString(Provider.MAIN_URL, "");

        provider_API_command.setAction(ProviderAPI.SET_UP_PROVIDER);
        provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);
        provider_API_command.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_NOK, expectedResult));

        providerApiManager.handleIntent(provider_API_command);
    }

    @Test
    public void test_handleIntentSetupProvider_happyPath_preseededProviderAndCA() throws IOException, CertificateEncodingException, NoSuchAlgorithmException {
        mockFingerprintForCertificate(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockProviderApiConnector(NO_ERROR);
        providerApiManager = new ProviderApiManager(mockPreferences, mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());
        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(RESULT_KEY, true);

        Intent provider_API_command = mockIntent();
        Bundle parameters = mockBundle();
        parameters.putString(Provider.MAIN_URL, "https://riseup.net");
        parameters.putString(Provider.CA_CERT, getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.pem")));
        parameters.putString(Provider.KEY, getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.json")));

        provider_API_command.setAction(ProviderAPI.SET_UP_PROVIDER);
        provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);
        provider_API_command.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_OK, expectedResult));

        providerApiManager.handleIntent(provider_API_command);
    }

    @Test
    public void test_handleIntentSetupProvider_happyPath_no_preseededProviderAndCA() throws IOException, CertificateEncodingException, NoSuchAlgorithmException {
        mockFingerprintForCertificate("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockProviderApiConnector(NO_ERROR);
        providerApiManager = new ProviderApiManager(mockPreferences, mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());
        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(RESULT_KEY, true);

        Intent provider_API_command = mockIntent();
        Bundle parameters = mockBundle();
        parameters.putString(Provider.MAIN_URL, "https://riseup.net");

        provider_API_command.setAction(ProviderAPI.SET_UP_PROVIDER);
        provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);
        provider_API_command.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_OK, expectedResult));

        providerApiManager.handleIntent(provider_API_command);
    }

    @Test
    public void test_handleIntentSetupProvider_happyPath_storedProviderAndCAFromPreviousSetup() throws IOException, CertificateEncodingException, NoSuchAlgorithmException {
        mockFingerprintForCertificate("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockProviderApiConnector(NO_ERROR);
        mockPreferences.edit().putString(Provider.KEY + ".riseup.net", getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.json"))).apply();
        mockPreferences.edit().putString(Provider.CA_CERT + ".riseup.net", getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.pem"))).apply();
        providerApiManager = new ProviderApiManager(mockPreferences, mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());
        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(RESULT_KEY, true);

        Intent provider_API_command = mockIntent();
        Bundle parameters = mockBundle();
        parameters.putString(Provider.MAIN_URL, "https://riseup.net");

        provider_API_command.setAction(ProviderAPI.SET_UP_PROVIDER);
        provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);
        provider_API_command.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_OK, expectedResult));

        providerApiManager.handleIntent(provider_API_command);
    }

    @Test
    public void test_handleIntentSetupProvider_preseededProviderAndCA_failedCAPinning() throws IOException, CertificateEncodingException, NoSuchAlgorithmException {
        mockFingerprintForCertificate(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29495");
        mockProviderApiConnector(NO_ERROR);
        providerApiManager = new ProviderApiManager(mockPreferences, mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());
        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errorId\":\"ERROR_CERTIFICATE_PINNING\",\"errors\":\"Stored provider certificate is invalid. You can either update Bitmask (recommended) or update the provider certificate using a commercial CA certificate.\"}");

        Intent provider_API_command = mockIntent();
        Bundle parameters = mockBundle();
        parameters.putString(Provider.MAIN_URL, "https://riseup.net");
        parameters.putString(Provider.CA_CERT, getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.pem")));
        parameters.putString(Provider.KEY, getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.json")));

        provider_API_command.setAction(ProviderAPI.SET_UP_PROVIDER);
        provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);
        provider_API_command.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_NOK, expectedResult));

        providerApiManager.handleIntent(provider_API_command);
    }

    @Test
    public void test_handleIntentSetupProvider_no_preseededProviderAndCA_failedPinning() throws IOException, CertificateEncodingException, NoSuchAlgorithmException {
        mockFingerprintForCertificate("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29495");
        mockProviderApiConnector(NO_ERROR);
        providerApiManager = new ProviderApiManager(mockPreferences, mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());
        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errorId\":\"ERROR_CERTIFICATE_PINNING\",\"errors\":\"Stored provider certificate is invalid. You can either update Bitmask (recommended) or update the provider certificate using a commercial CA certificate.\"}");

        Intent provider_API_command = mockIntent();
        Bundle parameters = mockBundle();
        parameters.putString(Provider.MAIN_URL, "https://riseup.net");

        provider_API_command.setAction(ProviderAPI.SET_UP_PROVIDER);
        provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);
        provider_API_command.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_NOK, expectedResult));

        providerApiManager.handleIntent(provider_API_command);
    }

    @Test
    public void test_handleIntentSetupProvider_storedProviderAndCAFromPreviousSetup_failedPinning() throws IOException, CertificateEncodingException, NoSuchAlgorithmException {
        mockFingerprintForCertificate("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29495");
        mockProviderApiConnector(NO_ERROR);
        mockPreferences.edit().putString(Provider.KEY + ".riseup.net", getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.json"))).apply();
        mockPreferences.edit().putString(Provider.CA_CERT + ".riseup.net", getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.pem"))).apply();
        providerApiManager = new ProviderApiManager(mockPreferences, mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());
        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errorId\":\"ERROR_CERTIFICATE_PINNING\",\"errors\":\"Stored provider certificate is invalid. You can either update Bitmask (recommended) or update the provider certificate using a commercial CA certificate.\"}");

        Intent provider_API_command = mockIntent();
        Bundle parameters = mockBundle();
        parameters.putString(Provider.MAIN_URL, "https://riseup.net");

        provider_API_command.setAction(ProviderAPI.SET_UP_PROVIDER);
        provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);
        provider_API_command.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_NOK, expectedResult));

        providerApiManager.handleIntent(provider_API_command);
    }


    @Test
    public void test_handleIntentSetupProvider_preseededProviderAndCA_outdatedCertificate() throws IOException, CertificateEncodingException, NoSuchAlgorithmException {
        mockProviderApiConnector(NO_ERROR);
        providerApiManager = new ProviderApiManager(mockPreferences, mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());
        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errorId\":\"ERROR_INVALID_CERTIFICATE\",\"errors\":\"Stored provider certificate is expired. You can either update Bitmask (recommended) or update the provider certificate using a commercial CA certificate.\"}");

        Intent provider_API_command = mockIntent();
        Bundle parameters = mockBundle();
        parameters.putString(Provider.MAIN_URL, "https://riseup.net");
        parameters.putString(Provider.CA_CERT, getInputAsString(getClass().getClassLoader().getResourceAsStream("outdated_cert.pem")));
        parameters.putString(Provider.KEY, getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.json")));

        provider_API_command.setAction(ProviderAPI.SET_UP_PROVIDER);
        provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);
        provider_API_command.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_NOK, expectedResult));

        providerApiManager.handleIntent(provider_API_command);
    }

    @Test
    public void test_handleIntentSetupProvider_storedProviderAndCAFromPreviousSetup_outdatedCertificate() throws IOException, CertificateEncodingException, NoSuchAlgorithmException {
        mockProviderApiConnector(NO_ERROR);
        mockPreferences.edit().putString(Provider.KEY + ".riseup.net", getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.json"))).apply();
        mockPreferences.edit().putString(Provider.CA_CERT + ".riseup.net", getInputAsString(getClass().getClassLoader().getResourceAsStream("outdated_cert.pem"))).apply();
        providerApiManager = new ProviderApiManager(mockPreferences, mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());
        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errorId\":\"ERROR_INVALID_CERTIFICATE\",\"errors\":\"Stored provider certificate is expired. You can either update Bitmask (recommended) or update the provider certificate using a commercial CA certificate.\"}");

        Intent provider_API_command = mockIntent();
        Bundle parameters = mockBundle();
        parameters.putString(Provider.MAIN_URL, "https://riseup.net");

        provider_API_command.setAction(ProviderAPI.SET_UP_PROVIDER);
        provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);
        provider_API_command.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_NOK, expectedResult));

        providerApiManager.handleIntent(provider_API_command);
    }

    @Test
    public void test_handleIntentSetupProvider_preseededProviderAndCA_ValidCertificateButUpdatedCertificateOnServerSide() throws IOException, CertificateEncodingException, NoSuchAlgorithmException {
        mockFingerprintForCertificate(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockProviderApiConnector(ERROR_CASE_UPDATED_CERTIFICATE);

        providerApiManager = new ProviderApiManager(mockPreferences, mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());
        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errorId\":\"ERROR_INVALID_CERTIFICATE\",\"errors\":\"Stored provider certificate is invalid. You can either update Bitmask (recommended) or update the provider certificate using a commercial CA certificate.\"}");

        Intent provider_API_command = mockIntent();
        Bundle parameters = mockBundle();
        parameters.putString(Provider.MAIN_URL, "https://riseup.net");
        parameters.putString(Provider.CA_CERT, getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.pem")));
        parameters.putString(Provider.KEY, getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.json")));

        provider_API_command.setAction(ProviderAPI.SET_UP_PROVIDER);
        provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);
        provider_API_command.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_NOK, expectedResult));

        providerApiManager.handleIntent(provider_API_command);
    }

    @Test
    public void test_handleIntentSetupProvider_storedProviderAndCAFromPreviousSetup_ValidCertificateButUpdatedCertificateOnServerSide() throws IOException, CertificateEncodingException, NoSuchAlgorithmException {
        mockFingerprintForCertificate("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockProviderApiConnector(ERROR_CASE_UPDATED_CERTIFICATE);
        mockPreferences.edit().putString(Provider.KEY + ".riseup.net", getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.json"))).apply();
        mockPreferences.edit().putString(Provider.CA_CERT + ".riseup.net", getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.pem"))).apply();
        providerApiManager = new ProviderApiManager(mockPreferences, mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());
        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errorId\":\"ERROR_INVALID_CERTIFICATE\",\"errors\":\"Stored provider certificate is invalid. You can either update Bitmask (recommended) or update the provider certificate using a commercial CA certificate.\"}");

        Intent provider_API_command = mockIntent();
        Bundle parameters = mockBundle();
        parameters.putString(Provider.MAIN_URL, "https://riseup.net");

        provider_API_command.setAction(ProviderAPI.SET_UP_PROVIDER);
        provider_API_command.putExtra(ProviderAPI.PARAMETERS, parameters);
        provider_API_command.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_NOK, expectedResult));

        providerApiManager.handleIntent(provider_API_command);
    }
}
