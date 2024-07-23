package se.leap.bitmaskclient.providersetup;

import static org.junit.Assert.*;

import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_KEY;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.base.models.Constants.USE_BRIDGES;
import static se.leap.bitmaskclient.base.models.Constants.USE_SNOWFLAKE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.CORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.ERRORS;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.MISSING_NETWORK_CONNECTION;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.PARAMETERS;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.PROVIDER_OK;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.TOR_EXCEPTION;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.TOR_TIMEOUT;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.UPDATE_INVALID_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.testutils.BackendMockResponses.BackendMockProvider.TestBackendErrorCase.NO_ERROR;
import static se.leap.bitmaskclient.testutils.BackendMockResponses.BackendMockProvider.TestBackendErrorCase.NO_ERROR_API_V4;
import static se.leap.bitmaskclient.testutils.MockHelper.mockBuildConfigHelper;
import static se.leap.bitmaskclient.testutils.MockHelper.mockCertificateHelper;
import static se.leap.bitmaskclient.testutils.MockHelper.mockClientGenerator;
import static se.leap.bitmaskclient.testutils.MockHelper.mockContext;
import static se.leap.bitmaskclient.testutils.MockHelper.mockPreferenceHelper;
import static se.leap.bitmaskclient.testutils.MockHelper.mockProviderApiConnector;
import static se.leap.bitmaskclient.testutils.MockHelper.mockPrivateKeyHelper;
import static se.leap.bitmaskclient.testutils.MockHelper.mockResources;
import static se.leap.bitmaskclient.testutils.MockHelper.mockResultReceiver;
import static se.leap.bitmaskclient.testutils.TestSetupHelper.getConfiguredProvider;
import static se.leap.bitmaskclient.testutils.TestSetupHelper.getConfiguredProviderAPIv4;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;

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

public class ProviderApiManagerTest {

    private Resources mockResources;
    private Context mockContext;

    private ProviderApiManager providerApiManager;

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
    public void test_handleIntentSetupProvider_TorBridgesPreferencesEnabledTimeout_TimeoutError() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, TimeoutException, InterruptedException {
        Provider provider = getConfiguredProviderAPIv4();
        SharedPreferences sharedPreferences = new MockSharedPreferences();
        sharedPreferences.edit().putBoolean(USE_BRIDGES, true).putBoolean(USE_SNOWFLAKE, true).commit();
        PreferenceHelper preferenceHelper = mockPreferenceHelper(provider, sharedPreferences);
        ProviderApiConnector mockedApiConnector = mockProviderApiConnector(NO_ERROR_API_V4);

        providerApiManager = new ProviderApiManager(mockResources, new ProviderApiManagerTest.TestProviderApiServiceCallback(true, true));

        Bundle expectedResult = new Bundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errorId\":\"ERROR_TOR_TIMEOUT\",\"initalAction\":\"setUpProvider\",\"errors\":\"Starting bridges failed. Do you want to retry or continue with an unobfuscated secure connection to configure Bitmask?\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = new Intent();
        providerApiCommand.putExtra(PROVIDER_KEY, provider);
        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(TOR_TIMEOUT, expectedResult));
        providerApiCommand.putExtra(PARAMETERS, new Bundle());

        providerApiManager.handleIntent(providerApiCommand);
        //providerApiManager.handleAction(SET_UP_PROVIDER, provider, new Bundle(), mockResultReceiver(TOR_TIMEOUT, expectedResult));
        assertEquals(-1, TorStatusObservable.getProxyPort());
    }

    @Test
    public void test_handleIntentSetupProvider_noNetwork_NetworkError() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, JSONException {
        Provider provider = getConfiguredProvider();

        CertificateHelper certHelper = mockCertificateHelper("a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        ProviderApiConnector mockedApiConnector = mockProviderApiConnector(NO_ERROR);
        providerApiManager = new ProviderApiManager(mockResources, new ProviderApiManagerTest.TestProviderApiServiceCallback(null, false));
        Bundle expectedResult = new Bundle();

        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"errors\":\"Bitmask has no internet connection. Please check your WiFi and cellular data settings.\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = new Intent();

        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(MISSING_NETWORK_CONNECTION, expectedResult));
        providerApiCommand.putExtra(PROVIDER_KEY, provider);
        providerApiCommand.putExtra(PARAMETERS, new Bundle());
        providerApiManager.handleIntent(providerApiCommand);
    }


    @Test
    public void test_handleIntentSetupProvider_TorBridgesPreferenceEnabled_Success() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, TimeoutException, InterruptedException {
        Provider provider = getConfiguredProviderAPIv4();

        SharedPreferences sharedPreferences = new MockSharedPreferences();
        sharedPreferences.edit().putBoolean(USE_BRIDGES, true).putBoolean(USE_SNOWFLAKE, true).commit();
        PreferenceHelper preferenceHelper = mockPreferenceHelper(provider, sharedPreferences);
        CertificateHelper certHelper = mockCertificateHelper(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        ProviderApiConnector mockedApiConnector = mockProviderApiConnector(NO_ERROR_API_V4);

        providerApiManager = new ProviderApiManager(mockResources, new ProviderApiManagerTest.TestProviderApiServiceCallback());

        Intent providerApiCommand = new Intent();
        providerApiCommand.putExtra(PROVIDER_KEY, provider);
        providerApiCommand.setAction(ProviderAPI.SET_UP_PROVIDER);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(PROVIDER_OK));
        providerApiCommand.putExtra(PARAMETERS, new Bundle());

        providerApiManager.handleIntent(providerApiCommand);
        assertEquals(8118, TorStatusObservable.getProxyPort());
    }


    @Test
    public void test_handleIntentUpdateVPNCertificate_TorBridgesPreferencesTrue_TorStartedAndSuccess() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, TimeoutException, InterruptedException {
        Provider provider = getConfiguredProviderAPIv4();

        SharedPreferences sharedPreferences = new MockSharedPreferences();
        sharedPreferences.edit().putBoolean(USE_BRIDGES, true).putBoolean(USE_SNOWFLAKE, true).commit();
        PreferenceHelper preferenceHelper = mockPreferenceHelper(provider, sharedPreferences);
        CertificateHelper certHelper = mockCertificateHelper(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        PrivateKeyHelper privateKeyHelper = mockPrivateKeyHelper();
        ProviderApiConnector mockedApiConnector = mockProviderApiConnector(NO_ERROR_API_V4);

        providerApiManager = new ProviderApiManager(mockResources, new ProviderApiManagerTest.TestProviderApiServiceCallback());

        Intent providerApiCommand = new Intent();
        providerApiCommand.putExtra(PROVIDER_KEY, provider);
        providerApiCommand.setAction(UPDATE_INVALID_VPN_CERTIFICATE);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(CORRECTLY_UPDATED_INVALID_VPN_CERTIFICATE));
        providerApiCommand.putExtra(PARAMETERS, new Bundle());

        providerApiManager.handleIntent(providerApiCommand);
        assertNotEquals(-1, TorStatusObservable.getProxyPort());
    }

    @Test
    public void test_handleIntentUpdateVPNCertificate_TorBridgesPreferencesTrue_TorException_Failure() throws IOException, CertificateEncodingException, NoSuchAlgorithmException, TimeoutException, InterruptedException {
        Provider provider = getConfiguredProviderAPIv4();

        SharedPreferences sharedPreferences = new MockSharedPreferences();
        sharedPreferences.edit().putBoolean(USE_BRIDGES, true).putBoolean(USE_SNOWFLAKE, true).commit();
        PreferenceHelper preferenceHelper = mockPreferenceHelper(provider, sharedPreferences);
        CertificateHelper certHelper = mockCertificateHelper(" a5244308a1374709a9afce95e3ae47c1b44bc2398c0a70ccbf8b3a8a97f29494");
        ProviderApiConnector mockedApiConnector = mockProviderApiConnector(NO_ERROR_API_V4);

        providerApiManager = new ProviderApiManager(mockResources, new ProviderApiManagerTest.TestProviderApiServiceCallback(new IllegalStateException("Nothing works always."), true));

        Bundle expectedResult = new Bundle();
        expectedResult.putBoolean(BROADCAST_RESULT_KEY, false);
        expectedResult.putString(ERRORS, "{\"initalAction\":\"ProviderAPI.UPDATE_INVALID_VPN_CERTIFICATE\"}");
        expectedResult.putParcelable(PROVIDER_KEY, provider);

        Intent providerApiCommand = new Intent();
        providerApiCommand.putExtra(PROVIDER_KEY, provider);
        providerApiCommand.setAction(UPDATE_INVALID_VPN_CERTIFICATE);
        providerApiCommand.putExtra(ProviderAPI.RECEIVER_KEY, mockResultReceiver(TOR_EXCEPTION, expectedResult));
        providerApiCommand.putExtra(PARAMETERS, new Bundle());

        providerApiManager.handleIntent(providerApiCommand);
        assertEquals(-1, TorStatusObservable.getProxyPort());
    }
}