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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_KEY;
import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_START;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.CORRECTLY_DOWNLOADED_GEOIP_JSON;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.CORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.ERRORS;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INCORRECTLY_DOWNLOADED_GEOIP_JSON;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INCORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.MISSING_NETWORK_CONNECTION;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.PARAMETERS;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.PROVIDER_NOK;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.PROVIDER_OK;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.TOR_EXCEPTION;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.TOR_TIMEOUT;
import static se.leap.bitmaskclient.testutils.BackendMockResponses.BackendMockProvider.TestBackendErrorCase.ERROR_CASE_FETCH_EIP_SERVICE_CERTIFICATE_INVALID;
import static se.leap.bitmaskclient.testutils.BackendMockResponses.BackendMockProvider.TestBackendErrorCase.ERROR_CASE_MICONFIGURED_PROVIDER;
import static se.leap.bitmaskclient.testutils.BackendMockResponses.BackendMockProvider.TestBackendErrorCase.ERROR_CASE_UPDATED_CERTIFICATE;
import static se.leap.bitmaskclient.testutils.BackendMockResponses.BackendMockProvider.TestBackendErrorCase.ERROR_DNS_RESUOLUTION_TOR_FALLBACK;
import static se.leap.bitmaskclient.testutils.BackendMockResponses.BackendMockProvider.TestBackendErrorCase.ERROR_GEOIP_SERVICE_IS_DOWN;
import static se.leap.bitmaskclient.testutils.BackendMockResponses.BackendMockProvider.TestBackendErrorCase.ERROR_GEOIP_SERVICE_IS_DOWN_TOR_FALLBACK;
import static se.leap.bitmaskclient.testutils.BackendMockResponses.BackendMockProvider.TestBackendErrorCase.NO_ERROR;
import static se.leap.bitmaskclient.testutils.BackendMockResponses.BackendMockProvider.TestBackendErrorCase.NO_ERROR_API_V4;
import static se.leap.bitmaskclient.testutils.MockHelper.mockBase64;
import static se.leap.bitmaskclient.testutils.MockHelper.mockBundle;
import static se.leap.bitmaskclient.testutils.MockHelper.mockClientGenerator;
import static se.leap.bitmaskclient.testutils.MockHelper.mockConfigHelper;
import static se.leap.bitmaskclient.testutils.MockHelper.mockIntent;
import static se.leap.bitmaskclient.testutils.MockHelper.mockPreferenceHelper;
import static se.leap.bitmaskclient.testutils.MockHelper.mockProviderApiConnector;
import static se.leap.bitmaskclient.testutils.MockHelper.mockRSAHelper;
import static se.leap.bitmaskclient.testutils.MockHelper.mockResources;
import static se.leap.bitmaskclient.testutils.MockHelper.mockResultReceiver;
import static se.leap.bitmaskclient.testutils.MockHelper.mockTextUtils;
import static se.leap.bitmaskclient.testutils.MockHelper.mockTorStatusObservable;
import static se.leap.bitmaskclient.testutils.TestSetupHelper.getConfiguredProvider;
import static se.leap.bitmaskclient.testutils.TestSetupHelper.getConfiguredProviderAPIv4;
import static se.leap.bitmaskclient.testutils.TestSetupHelper.getProvider;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;

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
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.util.concurrent.TimeoutException;

import se.leap.bitmaskclient.BuildConfig;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.utils.ConfigHelper;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.providersetup.ProviderAPI;
import se.leap.bitmaskclient.providersetup.ProviderApiConnector;
import se.leap.bitmaskclient.providersetup.ProviderApiManager;
import se.leap.bitmaskclient.providersetup.ProviderApiManagerBase;
import se.leap.bitmaskclient.providersetup.ProviderSetupObservable;
import se.leap.bitmaskclient.tor.TorStatusObservable;


/**
 * Created by cyberta on 04.01.18.
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({ProviderApiManager.class, TextUtils.class, ConfigHelper.RSAHelper.class, ConfigHelper.class, ProviderApiConnector.class, PreferenceHelper.class, TorStatusObservable.class, android.util.Base64.class, ProviderSetupObservable.class})
public class ProviderApiManagerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Resources mockResources;
    @Mock(answer =  Answers.RETURNS_DEEP_STUBS)
    private Context mockContext;

    private ProviderApiManager providerApiManager;

    static class TestProviderApiServiceCallback implements ProviderApiManagerBase.ProviderApiServiceCallback {
        Throwable startTorServiceException;
        boolean hasNetworkConnection;
        TestProviderApiServiceCallback() {
            this(null, true);
        }
        TestProviderApiServiceCallback(@Nullable Throwable startTorServiceException, boolean hasNetworkConnection) {
            this.startTorServiceException = startTorServiceException;
            this.hasNetworkConnection = hasNetworkConnection;
        }

        @Override
        public void broadcastEvent(Intent intent) {
        }

        @Override
        public boolean startTorService() throws InterruptedException, IllegalStateException {
            if (startTorServiceException != null) {
                if (startTorServiceException instanceof InterruptedException) {
                    throw (InterruptedException) startTorServiceException;
                }
                if (startTorServiceException instanceof IllegalStateException) {
                    throw (IllegalStateException) startTorServiceException;
                }
            }
            return true;
        }

        @Override
        public void stopTorService() {
        }

        @Override
        public int getTorHttpTunnelPort() {
            return 0;
        }

        @Override
        public boolean hasNetworkConnection() {
            return hasNetworkConnection;
        }

    }

    @Before
    public void setUp() throws Exception {

        Bundle bundle = mockBundle();
        PowerMockito.whenNew(Bundle.class).withAnyArguments().thenReturn(bundle);
        Intent intent = mockIntent();
        PowerMockito.whenNew(Intent.class).withAnyArguments().thenReturn(intent);
        mockTextUtils();
        mockResources = mockResources(getClass().getClassLoader().getResourceAsStream("error_messages.json"));
    }

    @Test
    public void test_handleIntentSetupProvider_noProviderMainURL() throws IOException, JSONException {
        Provider provider = new Provider("");
        mockPreferenceHelper(provider);
        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());
        Bundle expectedResult = mockBundle();

        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errors\":\"It doesn't seem to be a Bitmask provider.\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();
        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_NOK, expectedResult));
        providerApiCommand.putExtra(PROVIDER_KEY, provider);
        providerApiCommand.putExtra(PARAMETERS, mockBundle());

        providerApiManager.handleIntent(providerApiCommand);
    }

    @Test
    public void test_handleIntentSetupProvider_happyPath_preseededProviderAndCA() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = getConfiguredProvider();
        mockPreferenceHelper(provider);
        mockConfigHelper(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockProviderApiConnector(NO_ERROR);
        mockStatic(ProviderSetupObservable.class);

        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());
        Bundle expectedResult = mockBundle();

        expectedResult.putBoolean(BROADCAST_RESULT_KEY, true);
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();

        providerApiCommand.putExtra(PROVIDER_KEY, provider);
        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_OK, expectedResult));
        providerApiCommand.putExtra(PARAMETERS, mockBundle());

        providerApiManager.handleIntent(providerApiCommand);
    }

    @Test
    public void test_handleIntentSetupProvider_happyPath_no_preseededProviderAndCA() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = getConfiguredProvider();
        mockPreferenceHelper(provider);
        mockConfigHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockProviderApiConnector(NO_ERROR);
        mockStatic(ProviderSetupObservable.class);

        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());
        Bundle expectedResult = mockBundle();

        expectedResult.putBoolean(BROADCAST_RESULT_KEY, true);
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();

        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_OK, expectedResult));
        providerApiCommand.putExtra(PROVIDER_KEY, provider);
        providerApiCommand.putExtra(PARAMETERS, mockBundle());

        providerApiManager.handleIntent(providerApiCommand);
    }

    @Test
    public void test_handleIntentSetupProvider_happyPath_storedProviderAndCAFromPreviousSetup() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = new Provider("https://riseup.net");
        mockPreferenceHelper(getConfiguredProvider());
        mockConfigHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockProviderApiConnector(NO_ERROR);
        mockStatic(ProviderSetupObservable.class);

        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, true);
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();
        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_OK, expectedResult));
        providerApiCommand.putExtra(PARAMETERS, mockBundle());
        providerApiCommand.putExtra(PROVIDER_KEY, provider);

        providerApiManager.handleIntent(providerApiCommand);
    }

    @Test
    public void test_handleIntentSetupProvider_preseededProviderAndCA_failedCAPinning() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = getConfiguredProvider();

        mockPreferenceHelper(provider);
        mockConfigHelper(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29495");
        mockProviderApiConnector(NO_ERROR);
        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());
        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errorId\":\"ERROR_CERTIFICATE_PINNING\",\"errors\":\"Stored provider certificate is invalid. You can either update Bitmask (recommended) or update the provider certificate using a commercial CA certificate.\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();

        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_NOK, expectedResult));
        providerApiCommand.putExtra(PARAMETERS, mockBundle());

        providerApiCommand.putExtra(PROVIDER_KEY, provider);

        providerApiManager.handleIntent(providerApiCommand);
    }

    @Test
    public void test_handleIntentSetupProvider_no_preseededProviderAndCA_failedPinning() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = new Provider("https://riseup.net");
        mockPreferenceHelper(provider);
        mockConfigHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29495");
        mockProviderApiConnector(NO_ERROR);
        mockStatic(ProviderSetupObservable.class);

        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errorId\":\"ERROR_CERTIFICATE_PINNING\",\"errors\":\"Stored provider certificate is invalid. You can either update Bitmask (recommended) or update the provider certificate using a commercial CA certificate.\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();

        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_NOK, expectedResult));
        providerApiCommand.putExtra(PARAMETERS, mockBundle());

        providerApiCommand.putExtra(PROVIDER_KEY, provider);

        providerApiManager.handleIntent(providerApiCommand);
    }

    @Test
    public void test_handleIntentSetupProvider_storedProviderAndCAFromPreviousSetup_failedPinning() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = new Provider("https://riseup.net");
        mockPreferenceHelper(getConfiguredProvider());
        mockConfigHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29495");

        mockProviderApiConnector(NO_ERROR);
        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errorId\":\"ERROR_CERTIFICATE_PINNING\",\"errors\":\"Stored provider certificate is invalid. You can either update Bitmask (recommended) or update the provider certificate using a commercial CA certificate.\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();

        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_NOK, expectedResult));
        providerApiCommand.putExtra(PARAMETERS, mockBundle());
        providerApiCommand.putExtra(PROVIDER_KEY, provider);

        providerApiManager.handleIntent(providerApiCommand);
    }


    @Test
    public void test_handleIntentSetupProvider_preseededProviderAndCA_outdatedCertificate() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = getProvider(null ,null, null, null, "outdated_cert.pem", null, null, null);
        mockPreferenceHelper(provider);
        mockProviderApiConnector(NO_ERROR);
        mockConfigHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errorId\":\"ERROR_INVALID_CERTIFICATE\",\"errors\":\"Stored provider certificate is expired. You can either update Bitmask (recommended) or update the provider certificate using a commercial CA certificate.\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();

        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_NOK, expectedResult));
        providerApiCommand.putExtra(PARAMETERS, mockBundle());
        providerApiCommand.putExtra(PROVIDER_KEY, provider);

        providerApiManager.handleIntent(providerApiCommand);
    }

    @Test
    public void test_handleIntentSetupProvider_storedProviderAndCAFromPreviousSetup_outdatedCertificate() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = getProvider(null, null, null, null, "outdated_cert.pem", "riseup.net.json", null, null);
        mockPreferenceHelper(provider);
        PreferenceHelper.getEipDefinitionFromPreferences();
        mockProviderApiConnector(NO_ERROR);
        mockConfigHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errorId\":\"ERROR_INVALID_CERTIFICATE\",\"errors\":\"Stored provider certificate is expired. You can either update Bitmask (recommended) or update the provider certificate using a commercial CA certificate.\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();

        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_NOK, expectedResult));
        providerApiCommand.putExtra(PARAMETERS, mockBundle());
        providerApiCommand.putExtra(PROVIDER_KEY, provider);

        providerApiManager.handleIntent(providerApiCommand);
    }

    @Test
    public void test_handleIntentSetupProvider_preseededProviderAndCA_ValidCertificateButUpdatedCertificateOnServerSide() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = getConfiguredProvider();
        mockPreferenceHelper(provider);
        mockConfigHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockProviderApiConnector(ERROR_CASE_UPDATED_CERTIFICATE);

        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());
        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errorId\":\"ERROR_INVALID_CERTIFICATE\",\"errors\":\"Stored provider certificate is invalid. You can either update Bitmask (recommended) or update the provider certificate using a commercial CA certificate.\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();

        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_NOK, expectedResult));
        providerApiCommand.putExtra(PARAMETERS, mockBundle());

        providerApiCommand.putExtra(PROVIDER_KEY, provider);

        providerApiManager.handleIntent(providerApiCommand);
    }

    @Test
    public void test_handleIntentSetupProvider_storedProviderAndCAFromPreviousSetup_ValidCertificateButUpdatedCertificateOnServerSide() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = new Provider("https://riseup.net");
        mockPreferenceHelper(getConfiguredProvider());
        mockConfigHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockProviderApiConnector(ERROR_CASE_UPDATED_CERTIFICATE);
        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errorId\":\"ERROR_INVALID_CERTIFICATE\",\"errors\":\"Stored provider certificate is invalid. You can either update Bitmask (recommended) or update the provider certificate using a commercial CA certificate.\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();

        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_NOK, expectedResult));
        providerApiCommand.putExtra(PARAMETERS, mockBundle());

        providerApiCommand.putExtra(PROVIDER_KEY, provider);

        providerApiManager.handleIntent(providerApiCommand);
    }

    @Test
    public void test_handleIntentSetupProvider_preseededProviderAndCA_failedConfiguration() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {

        Provider provider = getConfiguredProvider();
        mockPreferenceHelper(provider);

        mockConfigHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockProviderApiConnector(ERROR_CASE_MICONFIGURED_PROVIDER);
        mockStatic(ProviderSetupObservable.class);

        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errors\":\"There was an error configuring Bitmask with your chosen provider.\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();

        providerApiCommand.putExtra(PROVIDER_KEY, provider);
        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_NOK, expectedResult));
        providerApiCommand.putExtra(PARAMETERS, mockBundle());

        providerApiManager.handleIntent(providerApiCommand);
    }

    @Test
    public void test_handleIntentSetupProvider_preseededCustomProviderAndCA_failedConfiguration() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        if ("insecure".equals(BuildConfig.FLAVOR_implementation )) {
            return;
        }
        Provider provider = getConfiguredProvider();
        mockPreferenceHelper(provider);

        mockProviderApiConnector(ERROR_CASE_MICONFIGURED_PROVIDER);
        mockConfigHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        when(ConfigHelper.isDefaultBitmask()).thenReturn(false);
        mockStatic(ProviderSetupObservable.class);

        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errors\":\"There was an error configuring RiseupVPN.\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();

        providerApiCommand.putExtra(PROVIDER_KEY, provider);
        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_NOK, expectedResult));
        providerApiCommand.putExtra(PARAMETERS, mockBundle());

        providerApiManager.handleIntent(providerApiCommand);
    }

    @Test
    public void test_handleIntentSetupProvider_outdatedPreseededProviderAndCA_successfulConfiguration() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {

        Provider provider = getProvider(null, null, null, null, null, "riseup_net_outdated_config.json", null, null);
        mockPreferenceHelper(provider);
        mockConfigHelper(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockProviderApiConnector(NO_ERROR);
        mockStatic(ProviderSetupObservable.class);

        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, true);
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();

        providerApiCommand.putExtra(PROVIDER_KEY, provider);
        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_OK, expectedResult));
        providerApiCommand.putExtra(PARAMETERS, mockBundle());

        providerApiManager.handleIntent(providerApiCommand);
    }

    @Test
    public void test_handleIntentSetupProvider_failingEipServiceFetch_failedConfiguration() throws IOException, NoSuchAlgorithmException, CertificateEncodingException {
        if ("insecure".equals(BuildConfig.FLAVOR_implementation )) {
            return;
        }

        Provider provider = new Provider("https://riseup.net");
        mockPreferenceHelper(provider);

        mockConfigHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockProviderApiConnector(ERROR_CASE_FETCH_EIP_SERVICE_CERTIFICATE_INVALID);
        mockStatic(ProviderSetupObservable.class);

        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putParcelable(PROVIDER_KEY, provider);
        expectedResult.putString(ERRORS, "This is not a trusted Bitmask provider.");
        Intent providerApiCommand = mockIntent();

        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_NOK, expectedResult));
        providerApiCommand.putExtra(PROVIDER_KEY, provider);
        providerApiCommand.putExtra(PARAMETERS, mockBundle());

        providerApiManager.handleIntent(providerApiCommand);
    }


    @Test
    public void test_handleIntentGetGeoip_happyPath() throws IOException, NoSuchAlgorithmException, CertificateEncodingException, JSONException {
        if ("insecure".equals(BuildConfig.FLAVOR_implementation )) {
            return;
        }

        Provider inputProvider = getConfiguredProvider();
        mockPreferenceHelper(inputProvider);
        inputProvider.setGeoIpJson(new JSONObject());
        Provider expectedProvider = getConfiguredProvider();
        mockConfigHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockProviderApiConnector(NO_ERROR);
        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(EIP_ACTION_START, true);
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, true);
        expectedResult.putParcelable(PROVIDER_KEY, expectedProvider);

        Intent providerApiCommand = mockIntent();

        providerApiCommand.setAction(ProviderAPI.DOWNLOAD_GEOIP_JSON);
        Bundle extrasBundle = mockBundle();
        extrasBundle.putBoolean(EIP_ACTION_START, true);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(CORRECTLY_DOWNLOADED_GEOIP_JSON, expectedResult));
        providerApiCommand.putExtra(PROVIDER_KEY, inputProvider);
        providerApiCommand.putExtra(PARAMETERS, extrasBundle);

        providerApiManager.handleIntent(providerApiCommand);

    }


    @Test
    public void test_handleIntentGetGeoip_serviceDown_failToDownload() throws IOException, NoSuchAlgorithmException, CertificateEncodingException, JSONException {
        if ("insecure".equals(BuildConfig.FLAVOR_implementation)) {
            return;
        }

        Provider provider = getConfiguredProvider();
        mockPreferenceHelper(provider);
        mockConfigHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockProviderApiConnector(ERROR_GEOIP_SERVICE_IS_DOWN);
        when(PreferenceHelper.getUseBridges()).thenReturn(false);
        when(PreferenceHelper.getUseSnowflake()).thenReturn(false);
        when(PreferenceHelper.hasSnowflakePrefs()).thenReturn(true);

        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(EIP_ACTION_START, true);
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();

        providerApiCommand.setAction(ProviderAPI.DOWNLOAD_GEOIP_JSON);
        Bundle extrasBundle = mockBundle();
        extrasBundle.putBoolean(EIP_ACTION_START, true);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(INCORRECTLY_DOWNLOADED_GEOIP_JSON, expectedResult));
        providerApiCommand.putExtra(PROVIDER_KEY, provider);
        providerApiCommand.putExtra(PARAMETERS, extrasBundle);

        providerApiManager.handleIntent(providerApiCommand);

    }

    @Test
    public void test_handleIntentGetGeoip_serviceDown_torNotStarted() throws IOException, NoSuchAlgorithmException, CertificateEncodingException, JSONException, TimeoutException, InterruptedException {
        if ("insecure".equals(BuildConfig.FLAVOR_implementation)) {
            return;
        }

        mockTorStatusObservable(null);
        Provider provider = getConfiguredProvider();
        mockPreferenceHelper(provider);
        mockConfigHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockProviderApiConnector(ERROR_GEOIP_SERVICE_IS_DOWN_TOR_FALLBACK);
        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(EIP_ACTION_START, true);
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();

        providerApiCommand.setAction(ProviderAPI.DOWNLOAD_GEOIP_JSON);
        Bundle extrasBundle = mockBundle();
        extrasBundle.putBoolean(EIP_ACTION_START, true);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(INCORRECTLY_DOWNLOADED_GEOIP_JSON, expectedResult));
        providerApiCommand.putExtra(PROVIDER_KEY, provider);
        providerApiCommand.putExtra(PARAMETERS, extrasBundle);

        providerApiManager.handleIntent(providerApiCommand);
        // also assert that Tor was not allowed to start
        assertEquals(-1, TorStatusObservable.getProxyPort());

    }

    @Test
    public void test_handleIntentGetGeoip_didNotReachTimeoutToFetchNew_returnsFailure() throws IOException, NoSuchAlgorithmException, CertificateEncodingException, JSONException {
        if ("insecure".equals(BuildConfig.FLAVOR_implementation)) {
            return;
        }

        Provider provider = getConfiguredProvider();
        provider.setLastGeoIpUpdate(System.currentTimeMillis());
        mockPreferenceHelper(provider);
        mockConfigHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockProviderApiConnector(NO_ERROR);
        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(EIP_ACTION_START, true);
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();

        providerApiCommand.setAction(ProviderAPI.DOWNLOAD_GEOIP_JSON);
        Bundle extrasBundle = mockBundle();
        extrasBundle.putBoolean(EIP_ACTION_START, true);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(INCORRECTLY_DOWNLOADED_GEOIP_JSON, expectedResult));
        providerApiCommand.putExtra(PROVIDER_KEY, provider);
        providerApiCommand.putExtra(PARAMETERS, extrasBundle);

        providerApiManager.handleIntent(providerApiCommand);
    }

    @Test
    public void test_handleIntentGetGeoip_noGeoipServiceURLDefined_returnsFailure() throws IOException, NoSuchAlgorithmException, CertificateEncodingException, JSONException {
        if ("insecure".equals(BuildConfig.FLAVOR_implementation)) {
            return;
        }

        Provider provider = getConfiguredProvider();
        provider.setGeoipUrl(null);
        provider.setGeoIpJson(new JSONObject());
        mockPreferenceHelper(provider);
        mockConfigHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockProviderApiConnector(NO_ERROR);
        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(EIP_ACTION_START, true);
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();

        providerApiCommand.setAction(ProviderAPI.DOWNLOAD_GEOIP_JSON);
        Bundle extrasBundle = mockBundle();
        extrasBundle.putBoolean(EIP_ACTION_START, true);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(INCORRECTLY_DOWNLOADED_GEOIP_JSON, expectedResult));
        providerApiCommand.putExtra(PROVIDER_KEY, provider);
        providerApiCommand.putExtra(PARAMETERS, extrasBundle);

        providerApiManager.handleIntent(providerApiCommand);
    }

    @Test
    public void test_handleIntentSetupProvider_APIv4_happyPath() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = getConfiguredProviderAPIv4();
        mockPreferenceHelper(provider);
        mockConfigHelper(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockProviderApiConnector(NO_ERROR_API_V4);
        mockStatic(ProviderSetupObservable.class);

        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());
        Bundle expectedResult = mockBundle();

        expectedResult.putBoolean(BROADCAST_RESULT_KEY, true);
        expectedResult.putParcelable(PROVIDER_KEY, provider);
        Intent providerApiCommand = mockIntent();

        providerApiCommand.putExtra(PROVIDER_KEY, provider);
        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_OK, expectedResult));
        providerApiCommand.putExtra(PARAMETERS, mockBundle());

        providerApiManager.handleIntent(providerApiCommand);
    }

    @Test
    public void test_handleIntentSetupProvider_TorFallback_SecondTryHappyPath() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, TimeoutException, InterruptedException {

        Provider provider = getConfiguredProviderAPIv4();
        mockPreferenceHelper(provider);
        when(PreferenceHelper.hasSnowflakePrefs()).thenReturn(false);
        when(PreferenceHelper.getUseSnowflake()).thenReturn(true); // getUseSnowflake() defaults to true if not set
        mockStatic(ProviderSetupObservable.class);

        mockConfigHelper(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockProviderApiConnector(ERROR_DNS_RESUOLUTION_TOR_FALLBACK);
        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Intent providerApiCommand = mockIntent();
        providerApiCommand.putExtra(PROVIDER_KEY, provider);
        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_OK));
        providerApiCommand.putExtra(PARAMETERS, mockBundle());
        mockTorStatusObservable(null);

        providerApiManager.handleIntent(providerApiCommand);
        assertEquals(8118, TorStatusObservable.getProxyPort());
    }


    @Test
    public void test_handleIntentSetupProvider_TorFallbackStartServiceException_SecondTryFailed() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, TimeoutException, InterruptedException {
        Provider provider = getConfiguredProviderAPIv4();
        mockPreferenceHelper(provider);
        mockConfigHelper(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockProviderApiConnector(ERROR_DNS_RESUOLUTION_TOR_FALLBACK);
        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback(new IllegalStateException("Tor service start not failed."), true));

        Intent providerApiCommand = mockIntent();
        providerApiCommand.putExtra(PROVIDER_KEY, provider);
        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_NOK));
        providerApiCommand.putExtra(PARAMETERS, mockBundle());
        mockTorStatusObservable(null);

        providerApiManager.handleIntent(providerApiCommand);
        assertEquals(-1, TorStatusObservable.getProxyPort());
    }

    @Test
    public void test_handleIntentSetupProvider_TorFallbackTimeoutException_SecondTryFailed() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, TimeoutException, InterruptedException {
        Provider provider = getConfiguredProviderAPIv4();
        mockPreferenceHelper(provider);
        mockConfigHelper(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockProviderApiConnector(ERROR_DNS_RESUOLUTION_TOR_FALLBACK);
        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Intent providerApiCommand = mockIntent();
        providerApiCommand.putExtra(PROVIDER_KEY, provider);
        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_NOK));
        providerApiCommand.putExtra(PARAMETERS, mockBundle());
        mockTorStatusObservable(new TimeoutException("Tor took too long to start."));

        providerApiManager.handleIntent(providerApiCommand);
        assertEquals(-1, TorStatusObservable.getProxyPort());
    }

    @Test
    public void test_handleIntentSetupProvider_TorBridgesPreferenceEnabled_Success() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, TimeoutException, InterruptedException {
        Provider provider = getConfiguredProviderAPIv4();

        mockPreferenceHelper(provider);
        mockConfigHelper(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockProviderApiConnector(NO_ERROR_API_V4);
        mockStatic(ProviderSetupObservable.class);

        when(PreferenceHelper.getUseBridges()).thenReturn(true);
        when(PreferenceHelper.getUseSnowflake()).thenReturn(true);
        when(PreferenceHelper.hasSnowflakePrefs()).thenReturn(true);

        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Intent providerApiCommand = mockIntent();
        providerApiCommand.putExtra(PROVIDER_KEY, provider);
        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_OK));
        providerApiCommand.putExtra(PARAMETERS, mockBundle());
        mockTorStatusObservable(null);

        providerApiManager.handleIntent(providerApiCommand);
        assertEquals(8118, TorStatusObservable.getProxyPort());
    }

    @Test
    public void test_handleIntentSetupProvider_TorBridgesDisabled_TorNotStarted() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, TimeoutException, InterruptedException {
        Provider provider = getConfiguredProviderAPIv4();

        mockPreferenceHelper(provider);
        mockConfigHelper(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockProviderApiConnector(NO_ERROR_API_V4);

        mockStatic(ProviderSetupObservable.class);
        when(PreferenceHelper.getUseBridges()).thenReturn(false);
        when(PreferenceHelper.getUseSnowflake()).thenReturn(false);
        when(PreferenceHelper.hasSnowflakePrefs()).thenReturn(true);

        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Intent providerApiCommand = mockIntent();
        providerApiCommand.putExtra(PROVIDER_KEY, provider);
        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_OK));
        providerApiCommand.putExtra(PARAMETERS, mockBundle());
        mockTorStatusObservable(new TimeoutException("This timeout exception is never thrown"));

        providerApiManager.handleIntent(providerApiCommand);
        assertEquals(-1, TorStatusObservable.getProxyPort());
    }

    @Test
    public void test_handleIntentUpdateVPNCertificate_TorBridgesPreferencesNotConfigured_TorStartedAndSuccess() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, TimeoutException, InterruptedException {
        Provider provider = getConfiguredProviderAPIv4();
        mockPreferenceHelper(provider);
        when(PreferenceHelper.hasSnowflakePrefs()).thenReturn(false);
        when(PreferenceHelper.getUseSnowflake()).thenReturn(true);
        mockConfigHelper(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockRSAHelper();
        mockBase64();
        mockProviderApiConnector(ERROR_DNS_RESUOLUTION_TOR_FALLBACK);

        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Intent providerApiCommand = mockIntent();
        providerApiCommand.putExtra(PROVIDER_KEY, provider);
        providerApiCommand.setAction(ProviderAPI.UPDATE_INVALID_VPN_CERTIFICATE);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(CORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE));
        providerApiCommand.putExtra(PARAMETERS, mockBundle());
        mockTorStatusObservable(null);

        providerApiManager.handleIntent(providerApiCommand);
        assertNotEquals(-1, TorStatusObservable.getProxyPort());
    }

    @Test
    public void test_handleIntentUpdateVPNCertificate_TorBridgesPreferencesFalse_TorNotStartedAndFailure() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, TimeoutException, InterruptedException {
        Provider provider = getConfiguredProviderAPIv4();

        mockPreferenceHelper(provider);
        mockConfigHelper(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockBase64();
        mockProviderApiConnector(ERROR_DNS_RESUOLUTION_TOR_FALLBACK);
        when(PreferenceHelper.getUseBridges()).thenReturn(false);
        when(PreferenceHelper.getUseSnowflake()).thenReturn(false);
        when(PreferenceHelper.hasSnowflakePrefs()).thenReturn(true);

        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Intent providerApiCommand = mockIntent();
        providerApiCommand.putExtra(PROVIDER_KEY, provider);
        providerApiCommand.setAction(ProviderAPI.UPDATE_INVALID_VPN_CERTIFICATE);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(INCORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE));
        providerApiCommand.putExtra(PARAMETERS, mockBundle());
        mockTorStatusObservable(new TimeoutException("This timeout exception is never thrown"));

        providerApiManager.handleIntent(providerApiCommand);
        assertEquals(-1, TorStatusObservable.getProxyPort());
    }

    @Test
    public void test_handleIntentUpdateVPNCertificate_TorBridgesPreferencesTrue_TorStartedAndSuccess() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, TimeoutException, InterruptedException {
        Provider provider = getConfiguredProviderAPIv4();

        mockPreferenceHelper(provider);
        mockConfigHelper(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockBase64();
        mockRSAHelper();
        mockProviderApiConnector(NO_ERROR_API_V4);
        when(PreferenceHelper.getUseBridges()).thenReturn(true);
        when(PreferenceHelper.getUseSnowflake()).thenReturn(true);
        when(PreferenceHelper.hasSnowflakePrefs()).thenReturn(true);

        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Intent providerApiCommand = mockIntent();
        providerApiCommand.putExtra(PROVIDER_KEY, provider);
        providerApiCommand.setAction(ProviderAPI.UPDATE_INVALID_VPN_CERTIFICATE);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(CORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE));
        providerApiCommand.putExtra(PARAMETERS, mockBundle());
        mockTorStatusObservable(null);

        providerApiManager.handleIntent(providerApiCommand);
        assertNotEquals(-1, TorStatusObservable.getProxyPort());
    }

    @Test
    public void test_handleIntentUpdateVPNCertificate_TorBridgesPreferencesTrue_TorException_Failure() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, TimeoutException, InterruptedException {
        Provider provider = getConfiguredProviderAPIv4();

        mockPreferenceHelper(provider);
        mockConfigHelper(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockBase64();
        mockProviderApiConnector(NO_ERROR_API_V4);
        when(PreferenceHelper.getUseBridges()).thenReturn(true);
        when(PreferenceHelper.getUseSnowflake()).thenReturn(true);
        when(PreferenceHelper.hasSnowflakePrefs()).thenReturn(true);

        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"initalAction\":\"ProviderAPI.UPDATE_INVALID_VPN_CERTIFICATE\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();
        providerApiCommand.putExtra(PROVIDER_KEY, provider);
        providerApiCommand.setAction(ProviderAPI.UPDATE_INVALID_VPN_CERTIFICATE);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(TOR_EXCEPTION, expectedResult));
        providerApiCommand.putExtra(PARAMETERS, mockBundle());
        mockTorStatusObservable(new InterruptedException("Tor thread was interrupted."));

        providerApiManager.handleIntent(providerApiCommand);
        assertEquals(-1, TorStatusObservable.getProxyPort());
    }

    @Test
    public void test_handleIntentSetupProvider_TorBridgesPreferencesEnabledTimeout_TimeoutError() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, TimeoutException, InterruptedException {
        Provider provider = getConfiguredProviderAPIv4();

        mockPreferenceHelper(provider);
        when(PreferenceHelper.getUseBridges()).thenReturn(true);
        when(PreferenceHelper.getUseSnowflake()).thenReturn(true);
        when(PreferenceHelper.hasSnowflakePrefs()).thenReturn(true);

        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = mockBundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errorId\":\"ERROR_TOR_TIMEOUT\",\"initalAction\":\"setUpProvider\",\"errors\":\"Starting bridges failed. Do you want to retry or continue with an unobfuscated secure connection to configure Bitmask?\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();
        providerApiCommand.putExtra(PROVIDER_KEY, provider);
        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(TOR_TIMEOUT, expectedResult));
        providerApiCommand.putExtra(PARAMETERS, mockBundle());
        mockTorStatusObservable(new TimeoutException("Tor took too long to start."));

        providerApiManager.handleIntent(providerApiCommand);
        assertEquals(-1, TorStatusObservable.getProxyPort());
    }

    @Test
    public void test_handleIntentSetupProvider_noNetwork_NetworkError() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = getConfiguredProvider();

        mockConfigHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        mockProviderApiConnector(NO_ERROR);
        providerApiManager = new ProviderApiManager(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback(null, false));
        Bundle expectedResult = mockBundle();

        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errors\":\"Bitmask has no internet connection. Please check your WiFi and cellular data settings.\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = mockIntent();

        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(MISSING_NETWORK_CONNECTION, expectedResult));
        providerApiCommand.putExtra(PROVIDER_KEY, provider);
        providerApiCommand.putExtra(PARAMETERS, mockBundle());
        providerApiManager.handleIntent(providerApiCommand);
    }

}
