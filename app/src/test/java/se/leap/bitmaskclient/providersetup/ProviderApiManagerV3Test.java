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

package se.leap.bitmaskclient.providersetup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_KEY;
import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_START;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.base.models.Constants.USE_BRIDGES;
import static se.leap.bitmaskclient.base.models.Constants.USE_SNOWFLAKE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.CORRECTLY_DOWNLOADED_GEOIP_JSON;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.CORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.DOWNLOAD_GEOIP_JSON;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.ERRORS;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INCORRECTLY_DOWNLOADED_GEOIP_JSON;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.INCORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.PROVIDER_NOK;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.PROVIDER_OK;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.SET_UP_PROVIDER;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.UPDATE_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.testutils.BackendMockResponses.BackendMockProvider.TestBackendErrorCase.ERROR_CASE_FETCH_EIP_SERVICE_CERTIFICATE_INVALID;
import static se.leap.bitmaskclient.testutils.BackendMockResponses.BackendMockProvider.TestBackendErrorCase.ERROR_CASE_MICONFIGURED_PROVIDER;
import static se.leap.bitmaskclient.testutils.BackendMockResponses.BackendMockProvider.TestBackendErrorCase.ERROR_CASE_UPDATED_CERTIFICATE;
import static se.leap.bitmaskclient.testutils.BackendMockResponses.BackendMockProvider.TestBackendErrorCase.ERROR_DNS_RESUOLUTION_TOR_FALLBACK;
import static se.leap.bitmaskclient.testutils.BackendMockResponses.BackendMockProvider.TestBackendErrorCase.ERROR_GEOIP_SERVICE_IS_DOWN;
import static se.leap.bitmaskclient.testutils.BackendMockResponses.BackendMockProvider.TestBackendErrorCase.ERROR_GEOIP_SERVICE_IS_DOWN_TOR_FALLBACK;
import static se.leap.bitmaskclient.testutils.BackendMockResponses.BackendMockProvider.TestBackendErrorCase.NO_ERROR;
import static se.leap.bitmaskclient.testutils.BackendMockResponses.BackendMockProvider.TestBackendErrorCase.NO_ERROR_API_V4;
import static se.leap.bitmaskclient.testutils.MockHelper.mockBuildConfigHelper;
import static se.leap.bitmaskclient.testutils.MockHelper.mockCertificateHelper;
import static se.leap.bitmaskclient.testutils.MockHelper.mockClientGenerator;
import static se.leap.bitmaskclient.testutils.MockHelper.mockContext;
import static se.leap.bitmaskclient.testutils.MockHelper.mockPreferenceHelper;
import static se.leap.bitmaskclient.testutils.MockHelper.mockPrivateKeyHelper;
import static se.leap.bitmaskclient.testutils.MockHelper.mockProviderApiConnector;
import static se.leap.bitmaskclient.testutils.MockHelper.mockResources;
import static se.leap.bitmaskclient.testutils.MockHelper.mockResultReceiver;
import static se.leap.bitmaskclient.testutils.TestSetupHelper.getConfiguredProvider;
import static se.leap.bitmaskclient.testutils.TestSetupHelper.getConfiguredProviderAPIv4;
import static se.leap.bitmaskclient.testutils.TestSetupHelper.getProvider;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.util.concurrent.TimeoutException;

import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.utils.BuildConfigHelper;
import se.leap.bitmaskclient.base.utils.CertificateHelper;
import se.leap.bitmaskclient.base.utils.HandlerProvider;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.base.utils.PrivateKeyHelper;
import se.leap.bitmaskclient.testutils.MockSharedPreferences;
import se.leap.bitmaskclient.tor.TorStatusObservable;


/**
 * Created by cyberta on 04.01.18.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.P})
public class ProviderApiManagerV3Test {

    private Resources mockResources;
    private Context mockContext;

    private ProviderApiManagerV3 providerApiManager;

    static class TestProviderApiServiceCallback implements ProviderApiManagerBase.ProviderApiServiceCallback {
        Throwable startTorServiceException;
        boolean hasNetworkConnection;
        boolean torTimeout;
        TorStatusObservable torStatusObservable;

        TestProviderApiServiceCallback() {
            this(null, true);
        }
        TestProviderApiServiceCallback(@Nullable Throwable startTorServiceException, boolean hasNetworkConnection) {
            this.startTorServiceException = startTorServiceException;
            this.hasNetworkConnection = hasNetworkConnection;
            this.torStatusObservable = TorStatusObservable.getInstance();
        }

        TestProviderApiServiceCallback(boolean torTimeout, boolean hasNetworkConnection) {
            this.hasNetworkConnection = hasNetworkConnection;
            this.torStatusObservable = TorStatusObservable.getInstance();
            this.torTimeout = torTimeout;
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
            if (!torTimeout) {
                try {
                    TorStatusObservable.updateState(mockContext(), TorStatusObservable.TorStatus.ON.toString());
                } catch (PackageManager.NameNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
            return true;
        }

        @Override
        public void stopTorService() {
        }

        @Override
        public int getTorHttpTunnelPort() {
            return 8118;
        }

        @Override
        public int getTorSocksProxyPort() {
            return 9050;
        }

        @Override
        public boolean hasNetworkConnection() {
            return hasNetworkConnection;
        }

        @Override
        public void saveProvider(Provider p) {

        }

    }

    @Before
    public void setUp() throws Exception {
        mockContext = mockContext();
        mockResources = mockResources(getClass().getClassLoader().getResourceAsStream("error_messages.json"));

        HandlerProvider handlerProvider = new HandlerProvider((r, delay) -> new Thread(r).start());
        BuildConfigHelper buildConfigHelper = mockBuildConfigHelper(true);
        TorStatusObservable torStatusObservable = TorStatusObservable.getInstance();
        TorStatusObservable.setProxyPort(-1);
        TorStatusObservable.setLastError(null);
        TorStatusObservable.updateState(mockContext, TorStatusObservable.TorStatus.OFF.toString());
    }

    @Test
    public void test_handleIntentSetupProvider_noProviderMainURL() throws IOException, JSONException {
        Provider provider = new Provider("");
        PreferenceHelper preferenceHelper = mockPreferenceHelper(provider);
        providerApiManager = new ProviderApiManagerV3(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());
        Bundle expectedResult = new Bundle();

        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errors\":\"It doesn't seem to be a Bitmask provider.\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        providerApiManager.handleAction(SET_UP_PROVIDER, provider, new Bundle(), mockResultReceiver(PROVIDER_NOK, expectedResult));
    }

    @Test
    public void test_handleIntentSetupProvider_happyPath_preseededProviderAndCA() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = getConfiguredProvider();
        PreferenceHelper preferenceHelper = mockPreferenceHelper(provider);
        CertificateHelper certHelper = mockCertificateHelper(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        ProviderApiConnector providerApiConnector = mockProviderApiConnector(NO_ERROR);

        providerApiManager = new ProviderApiManagerV3(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());
        Bundle expectedResult = new Bundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, true);
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        providerApiManager.handleAction(SET_UP_PROVIDER, provider, new Bundle(), mockResultReceiver(PROVIDER_OK, expectedResult));
    }

    @Test
    public void test_handleIntentSetupProvider_happyPath_no_preseededProviderAndCA() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = getConfiguredProvider();
        PreferenceHelper preferenceHelper = mockPreferenceHelper(provider);
        CertificateHelper certHelper = mockCertificateHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        ProviderApiConnector mockedApiConnector = mockProviderApiConnector(NO_ERROR);

        providerApiManager = new ProviderApiManagerV3(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());
        Bundle expectedResult = new Bundle();

        expectedResult.putBoolean(BROADCAST_RESULT_KEY, true);
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        providerApiManager.handleAction(SET_UP_PROVIDER, provider, new Bundle(), mockResultReceiver(PROVIDER_OK, expectedResult));
    }

    @Test
    public void test_handleIntentSetupProvider_happyPath_storedProviderAndCAFromPreviousSetup() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = new Provider("https://riseup.net");
        PreferenceHelper preferenceHelper = mockPreferenceHelper(getConfiguredProvider());
        CertificateHelper certHelper = mockCertificateHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        ProviderApiConnector mockedApiConnector = mockProviderApiConnector(NO_ERROR);

        providerApiManager = new ProviderApiManagerV3(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = new Bundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, true);
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        providerApiManager.handleAction(SET_UP_PROVIDER, provider, new Bundle(), mockResultReceiver(PROVIDER_OK, expectedResult));
    }

    @Test
    public void test_handleIntentSetupProvider_preseededProviderAndCA_failedCAPinning() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = getConfiguredProvider();

        PreferenceHelper preferenceHelper = mockPreferenceHelper(provider);
        CertificateHelper certHelper = mockCertificateHelper(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29495");
        ProviderApiConnector mockedApiConnector = mockProviderApiConnector(NO_ERROR);
        providerApiManager = new ProviderApiManagerV3(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());
        Bundle expectedResult = new Bundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errors\":\"Stored provider certificate is invalid. You can either update Bitmask (recommended) or update the provider certificate using a commercial CA certificate.\",\"errorId\":\"ERROR_CERTIFICATE_PINNING\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        providerApiManager.handleAction(SET_UP_PROVIDER, provider, new Bundle(), mockResultReceiver(PROVIDER_NOK, expectedResult));
    }

    @Test
    public void test_handleIntentSetupProvider_no_preseededProviderAndCA_failedPinning() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = new Provider("https://riseup.net");
        PreferenceHelper preferenceHelper = mockPreferenceHelper(provider);
        CertificateHelper certHelper = mockCertificateHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29495");
        ProviderApiConnector mockedApiConnector = mockProviderApiConnector(NO_ERROR);

        providerApiManager = new ProviderApiManagerV3(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = new Bundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errors\":\"Stored provider certificate is invalid. You can either update Bitmask (recommended) or update the provider certificate using a commercial CA certificate.\",\"errorId\":\"ERROR_CERTIFICATE_PINNING\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        providerApiManager.handleAction(SET_UP_PROVIDER, provider, new Bundle(), mockResultReceiver(PROVIDER_NOK, expectedResult));
    }

    @Test
    public void test_handleIntentSetupProvider_storedProviderAndCAFromPreviousSetup_failedPinning() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = new Provider("https://riseup.net");
        mockPreferenceHelper(getConfiguredProvider());
        CertificateHelper certHelper = mockCertificateHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29495");

        ProviderApiConnector mockedApiConnector = mockProviderApiConnector(NO_ERROR);
        providerApiManager = new ProviderApiManagerV3(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = new Bundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errors\":\"Stored provider certificate is invalid. You can either update Bitmask (recommended) or update the provider certificate using a commercial CA certificate.\",\"errorId\":\"ERROR_CERTIFICATE_PINNING\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        providerApiManager.handleAction(SET_UP_PROVIDER, provider, new Bundle(), mockResultReceiver(PROVIDER_NOK, expectedResult));
    }


    @Test
    public void test_handleIntentSetupProvider_preseededProviderAndCA_outdatedCertificate() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = getProvider(null ,null, null, null, "outdated_cert.pem", null, null, null);
        PreferenceHelper preferenceHelper = mockPreferenceHelper(provider);
        ProviderApiConnector mockedApiConnector = mockProviderApiConnector(NO_ERROR);
        CertificateHelper certHelper = mockCertificateHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        providerApiManager = new ProviderApiManagerV3(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = new Bundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errors\":\"Stored provider certificate is expired. You can either update Bitmask (recommended) or update the provider certificate using a commercial CA certificate.\",\"errorId\":\"ERROR_INVALID_CERTIFICATE\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        providerApiManager.handleAction(SET_UP_PROVIDER, provider, new Bundle(), mockResultReceiver(PROVIDER_NOK, expectedResult));
    }

    @Test
    public void test_handleIntentSetupProvider_storedProviderAndCAFromPreviousSetup_outdatedCertificate() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = getProvider(null, null, null, null, "outdated_cert.pem", "riseup.net.json", null, null);
        PreferenceHelper preferenceHelper = mockPreferenceHelper(provider);
        PreferenceHelper.getEipDefinitionFromPreferences();
        ProviderApiConnector mockedApiConnector = mockProviderApiConnector(NO_ERROR);
        CertificateHelper certHelper = mockCertificateHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        providerApiManager = new ProviderApiManagerV3(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = new Bundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errors\":\"Stored provider certificate is expired. You can either update Bitmask (recommended) or update the provider certificate using a commercial CA certificate.\",\"errorId\":\"ERROR_INVALID_CERTIFICATE\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        providerApiManager.handleAction(SET_UP_PROVIDER, provider, new Bundle(), mockResultReceiver(PROVIDER_NOK, expectedResult));
    }

    @Test
    public void test_handleIntentSetupProvider_preseededProviderAndCA_ValidCertificateButUpdatedCertificateOnServerSide() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = getConfiguredProvider();
        PreferenceHelper preferenceHelper = mockPreferenceHelper(provider);
        CertificateHelper certHelper = mockCertificateHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        ProviderApiConnector mockedApiConnector = mockProviderApiConnector(ERROR_CASE_UPDATED_CERTIFICATE);

        providerApiManager = new ProviderApiManagerV3(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());
        Bundle expectedResult = new Bundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errors\":\"Stored provider certificate is invalid. You can either update Bitmask (recommended) or update the provider certificate using a commercial CA certificate.\",\"errorId\":\"ERROR_INVALID_CERTIFICATE\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = new Intent();

        providerApiManager.handleAction(SET_UP_PROVIDER, provider, new Bundle(), mockResultReceiver(PROVIDER_NOK, expectedResult));
    }

    @Test
    public void test_handleIntentSetupProvider_storedProviderAndCAFromPreviousSetup_ValidCertificateButUpdatedCertificateOnServerSide() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = getConfiguredProvider();
        PreferenceHelper preferenceHelper = mockPreferenceHelper(provider);
        CertificateHelper certHelper = mockCertificateHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        ProviderApiConnector mockedApiConnector = mockProviderApiConnector(ERROR_CASE_UPDATED_CERTIFICATE);
        providerApiManager = new ProviderApiManagerV3(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = new Bundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errors\":\"Stored provider certificate is invalid. You can either update Bitmask (recommended) or update the provider certificate using a commercial CA certificate.\",\"errorId\":\"ERROR_INVALID_CERTIFICATE\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        providerApiManager.handleAction(SET_UP_PROVIDER, provider, new Bundle(), mockResultReceiver(PROVIDER_NOK, expectedResult));
    }

    @Test
    public void test_handleIntentSetupProvider_preseededProviderAndCA_failedConfiguration() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {

        Provider provider = getProvider(null, null, null, null, null, "riseup_net_invalid_config.json", null, null);
        PreferenceHelper preferenceHelper = mockPreferenceHelper(provider);

        CertificateHelper certHelper = mockCertificateHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        ProviderApiConnector mockedApiConnector = mockProviderApiConnector(NO_ERROR);

        providerApiManager = new ProviderApiManagerV3(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = new Bundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errors\":\"There was an error configuring Bitmask with your chosen provider.\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        providerApiManager.handleAction(SET_UP_PROVIDER, provider, new Bundle(), mockResultReceiver(PROVIDER_NOK, expectedResult));
    }


    @Test
    public void test_handleIntentSetupProvider_preseededCustomProviderAndCA_failedConfiguration() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = new Provider("riseup.net");
        PreferenceHelper preferenceHelper = new PreferenceHelper(new MockSharedPreferences());

        ProviderApiConnector mockedApiConnector = mockProviderApiConnector(ERROR_CASE_MICONFIGURED_PROVIDER);
        CertificateHelper certHelper = mockCertificateHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        BuildConfigHelper buildConfigHelper = mockBuildConfigHelper(false);

        providerApiManager = new ProviderApiManagerV3(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = new Bundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errors\":\"There was an error configuring RiseupVPN.\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        providerApiManager.handleAction(SET_UP_PROVIDER, provider, new Bundle(), mockResultReceiver(PROVIDER_NOK, expectedResult));
    }

    @Test
    public void test_handleIntentSetupProvider_outdatedPreseededProviderAndCA_successfulConfiguration() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {

        Provider provider = getProvider(null, null, null, null, null, "riseup_net_outdated_config.json", null, null);
        PreferenceHelper preferenceHelper = mockPreferenceHelper(provider);
        CertificateHelper certHelper = mockCertificateHelper(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        ProviderApiConnector mockedApiConnector = mockProviderApiConnector(NO_ERROR);

        providerApiManager = new ProviderApiManagerV3(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = new Bundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, true);
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        providerApiManager.handleAction(SET_UP_PROVIDER, provider, new Bundle(), mockResultReceiver(PROVIDER_OK, expectedResult));
    }

    @Test
    public void test_handleIntentSetupProvider_failingEipServiceFetch_failedConfiguration() throws IOException, NoSuchAlgorithmException, CertificateEncodingException {
        Provider provider = new Provider("https://riseup.net");
        PreferenceHelper preferenceHelper = mockPreferenceHelper(provider);
        CertificateHelper certHelper = mockCertificateHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        ProviderApiConnector mockedApiConnector = mockProviderApiConnector(ERROR_CASE_FETCH_EIP_SERVICE_CERTIFICATE_INVALID);

        providerApiManager = new ProviderApiManagerV3(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = new Bundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putParcelable(PROVIDER_KEY, provider);
        expectedResult.putString(ERRORS, "This is not a trusted Bitmask provider.");

        providerApiManager.handleAction(SET_UP_PROVIDER, provider, new Bundle(), mockResultReceiver(PROVIDER_NOK, expectedResult));
    }

    @Test
    public void test_handleIntentGetGeoip_happyPath() throws IOException, NoSuchAlgorithmException, CertificateEncodingException, JSONException {
        Provider provider = getConfiguredProvider();
        provider.setGeoIpJson(new JSONObject());
        PreferenceHelper preferenceHelper = mockPreferenceHelper(provider);
        Provider expectedProvider = getConfiguredProvider();
        CertificateHelper certHelper = mockCertificateHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        ProviderApiConnector mockedApiConnector = mockProviderApiConnector(NO_ERROR);
        providerApiManager = new ProviderApiManagerV3(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = new Bundle();
        expectedResult.putBoolean(EIP_ACTION_START, true);
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, true);
        expectedResult.putParcelable(PROVIDER_KEY, expectedProvider);

        Bundle extrasBundle = new Bundle();
        extrasBundle.putBoolean(EIP_ACTION_START, true);

        providerApiManager.handleAction(DOWNLOAD_GEOIP_JSON, provider, extrasBundle, mockResultReceiver(CORRECTLY_DOWNLOADED_GEOIP_JSON, expectedResult));
    }


    @Test
    public void test_handleIntentGetGeoip_serviceDown_failToDownload() throws IOException, NoSuchAlgorithmException, CertificateEncodingException, JSONException {
        Provider provider = getConfiguredProvider();
        SharedPreferences mockSharedPref = new MockSharedPreferences();
        mockSharedPref.edit().
                putBoolean(USE_BRIDGES, false).
                putBoolean(USE_SNOWFLAKE, false).commit();
        PreferenceHelper preferenceHelper = mockPreferenceHelper(provider, mockSharedPref);
        CertificateHelper certHelper = mockCertificateHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        ProviderApiConnector mockedApiConnector = mockProviderApiConnector(ERROR_GEOIP_SERVICE_IS_DOWN);
        providerApiManager = new ProviderApiManagerV3(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = new Bundle();
        expectedResult.putBoolean(EIP_ACTION_START, true);
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Bundle extrasBundle = new Bundle();
        extrasBundle.putBoolean(EIP_ACTION_START, true);
        providerApiManager.handleAction(DOWNLOAD_GEOIP_JSON, provider, extrasBundle,  mockResultReceiver(INCORRECTLY_DOWNLOADED_GEOIP_JSON, expectedResult));
    }

    @Test
    public void test_handleIntentGetGeoip_serviceDown_torNotStarted() throws IOException, NoSuchAlgorithmException, CertificateEncodingException, JSONException, TimeoutException, InterruptedException {
        Provider provider = getConfiguredProvider();
        PreferenceHelper preferenceHelper = mockPreferenceHelper(provider);
        CertificateHelper certHelper = mockCertificateHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        ProviderApiConnector mockedApiConnector = mockProviderApiConnector(ERROR_GEOIP_SERVICE_IS_DOWN_TOR_FALLBACK);
        providerApiManager = new ProviderApiManagerV3(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = new Bundle();
        expectedResult.putBoolean(EIP_ACTION_START, true);
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Bundle extrasBundle = new Bundle();
        extrasBundle.putBoolean(EIP_ACTION_START, true);
        providerApiManager.handleAction(DOWNLOAD_GEOIP_JSON, provider, extrasBundle, mockResultReceiver(INCORRECTLY_DOWNLOADED_GEOIP_JSON, expectedResult));
        
        // also assert that Tor was not allowed to start
        assertEquals(-1, TorStatusObservable.getProxyPort());

    }

    @Test
    public void test_handleIntentGetGeoip_didNotReachTimeoutToFetchNew_returnsFailure() throws IOException, NoSuchAlgorithmException, CertificateEncodingException, JSONException {
        Provider provider = getConfiguredProvider();
        provider.setLastGeoIpUpdate(System.currentTimeMillis());
        PreferenceHelper preferenceHelper = mockPreferenceHelper(provider);
        CertificateHelper certHelper = mockCertificateHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        ProviderApiConnector mockedApiConnector = mockProviderApiConnector(NO_ERROR);
        providerApiManager = new ProviderApiManagerV3(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = new Bundle();
        expectedResult.putBoolean(EIP_ACTION_START, true);
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Bundle extrasBundle = new Bundle();
        extrasBundle.putBoolean(EIP_ACTION_START, true);
        providerApiManager.handleAction(DOWNLOAD_GEOIP_JSON, provider, extrasBundle, mockResultReceiver(INCORRECTLY_DOWNLOADED_GEOIP_JSON, expectedResult));
        
    }

    @Test
    public void test_handleIntentGetGeoip_noGeoipServiceURLDefined_returnsFailure() throws IOException, NoSuchAlgorithmException, CertificateEncodingException, JSONException {

        Provider provider = getConfiguredProvider();
        provider.setGeoipUrl(null);
        provider.setGeoIpJson(new JSONObject());
        PreferenceHelper preferenceHelper = mockPreferenceHelper(provider);
        CertificateHelper certHelper = mockCertificateHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        ProviderApiConnector mockedApiConnector = mockProviderApiConnector(NO_ERROR);
        providerApiManager = new ProviderApiManagerV3(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        Bundle expectedResult = new Bundle();
        expectedResult.putBoolean(EIP_ACTION_START, true);
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        providerApiManager.handleAction(DOWNLOAD_GEOIP_JSON, provider, new Bundle(), mockResultReceiver(CORRECTLY_DOWNLOADED_GEOIP_JSON, expectedResult));
    }

    @Test
    public void test_handleIntentSetupProvider_APIv4_happyPath() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = getConfiguredProviderAPIv4();
        PreferenceHelper preferenceHelper = mockPreferenceHelper(provider);
        CertificateHelper certHelper = mockCertificateHelper(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        ProviderApiConnector mockedApiConnector = mockProviderApiConnector(NO_ERROR_API_V4);

        providerApiManager = new ProviderApiManagerV3(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());
        Bundle expectedResult = new Bundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, true);
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        providerApiManager.handleAction(SET_UP_PROVIDER, provider, new Bundle(), mockResultReceiver(PROVIDER_OK, expectedResult));
    }

    @Test
    public void test_handleIntentSetupProvider_TorFallback_SecondTryHappyPath() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, TimeoutException, InterruptedException {

        Provider provider = new Provider("riseup.net");
        PreferenceHelper preferenceHelper = mockPreferenceHelper(provider);
        CertificateHelper certHelper = mockCertificateHelper(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        ProviderApiConnector mockedApiConnector = mockProviderApiConnector(ERROR_DNS_RESUOLUTION_TOR_FALLBACK);
        providerApiManager = new ProviderApiManagerV3(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        providerApiManager.handleAction(SET_UP_PROVIDER, provider, new Bundle(), mockResultReceiver(PROVIDER_OK));
        assertNotEquals(-1, TorStatusObservable.getProxyPort());
    }


    @Test
    public void test_handleIntentSetupProvider_TorFallbackStartServiceException_SecondTryFailed() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, TimeoutException, InterruptedException {
        Provider provider = getConfiguredProviderAPIv4();
        PreferenceHelper preferenceHelper = mockPreferenceHelper(provider);
        CertificateHelper certHelper = mockCertificateHelper(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        ProviderApiConnector mockedApiConnector = mockProviderApiConnector(ERROR_DNS_RESUOLUTION_TOR_FALLBACK);
        providerApiManager = new ProviderApiManagerV3(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback(new IllegalStateException("Tor service start not failed."), true));

        providerApiManager.handleAction(SET_UP_PROVIDER, provider, new Bundle(), mockResultReceiver(PROVIDER_NOK));
        assertEquals(-1, TorStatusObservable.getProxyPort());
    }

    @Test
    public void test_handleIntentSetupProvider_TorFallbackTimeoutException_SecondTryFailed() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, TimeoutException, InterruptedException {
        Provider provider = getConfiguredProviderAPIv4();
        PreferenceHelper preferenceHelper = mockPreferenceHelper(provider);
        CertificateHelper certHelper = mockCertificateHelper(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        ProviderApiConnector mockedApiConnector = mockProviderApiConnector(ERROR_DNS_RESUOLUTION_TOR_FALLBACK);
        providerApiManager = new ProviderApiManagerV3(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback(true, true));

        providerApiManager.handleAction(SET_UP_PROVIDER, provider, new Bundle(), mockResultReceiver(PROVIDER_NOK));
        assertEquals(-1, TorStatusObservable.getProxyPort());
    }

    @Test
    public void test_handleIntentSetupProvider_TorBridgesDisabled_TorNotStarted() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, TimeoutException, InterruptedException {
        Provider provider = getConfiguredProviderAPIv4();

        SharedPreferences sharedPreferences = new MockSharedPreferences();
        sharedPreferences.edit().putBoolean(USE_BRIDGES, false).putBoolean(USE_SNOWFLAKE, false).commit();
        PreferenceHelper preferenceHelper = mockPreferenceHelper(provider, sharedPreferences);
        CertificateHelper certHelper = mockCertificateHelper(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        ProviderApiConnector mockedApiConnector = mockProviderApiConnector(NO_ERROR_API_V4);

        providerApiManager = new ProviderApiManagerV3(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        providerApiManager.handleAction(SET_UP_PROVIDER, provider, new Bundle(), mockResultReceiver(PROVIDER_OK));
        assertEquals(-1, TorStatusObservable.getProxyPort());
    }

    @Test
    public void test_handleIntentUpdateVPNCertificate_TorBridgesPreferencesNotConfigured_TorStartedAndSuccess() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, TimeoutException, InterruptedException {
        Provider provider = getConfiguredProviderAPIv4();
        PreferenceHelper preferenceHelper = mockPreferenceHelper(provider);
        CertificateHelper certHelper = mockCertificateHelper(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        PrivateKeyHelper privateKeyHelper = mockPrivateKeyHelper();
        ProviderApiConnector mockedApiConnector = mockProviderApiConnector(ERROR_DNS_RESUOLUTION_TOR_FALLBACK);

        providerApiManager = new ProviderApiManagerV3(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        providerApiManager.handleAction(UPDATE_INVALID_VPN_CERTIFICATE, provider, new Bundle(), mockResultReceiver(CORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE));
        assertEquals(8118, TorStatusObservable.getProxyPort());
    }

    @Test
    public void test_handleIntentUpdateVPNCertificate_TorBridgesPreferencesFalse_TorNotStartedAndFailure() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, TimeoutException, InterruptedException {
        Provider provider = getConfiguredProviderAPIv4();

        SharedPreferences sharedPreferences = new MockSharedPreferences();
        sharedPreferences.edit().putBoolean(USE_BRIDGES, false).putBoolean(USE_SNOWFLAKE, false).commit();
        PreferenceHelper preferenceHelper = mockPreferenceHelper(provider, sharedPreferences);
        CertificateHelper certHelper = mockCertificateHelper(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        ProviderApiConnector mockedApiConnector = mockProviderApiConnector(ERROR_DNS_RESUOLUTION_TOR_FALLBACK);

        providerApiManager = new ProviderApiManagerV3(mockResources, mockClientGenerator(), new TestProviderApiServiceCallback());

        providerApiManager.handleAction(UPDATE_INVALID_VPN_CERTIFICATE, provider, new Bundle(), mockResultReceiver(INCORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE));
        assertEquals(-1, TorStatusObservable.getProxyPort());
    }
}
