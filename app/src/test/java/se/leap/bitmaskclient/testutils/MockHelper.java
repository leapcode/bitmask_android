package se.leap.bitmaskclient.testutils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_EIP_DEFINITION;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_MOTD;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_MOTD_HASHES;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_MOTD_LAST_SEEN;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_MOTD_LAST_UPDATED;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_PRIVATE_KEY;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getEipDefinitionFromPreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.ResultReceiver;

import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.UnknownHostException;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import okhttp3.OkHttpClient;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.models.ProviderObservable;
import se.leap.bitmaskclient.base.utils.CertificateHelper;
import se.leap.bitmaskclient.base.utils.FileHelper;
import se.leap.bitmaskclient.base.utils.BuildConfigHelper;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.base.utils.PrivateKeyHelper;
import se.leap.bitmaskclient.providersetup.ProviderApiConnector;
import se.leap.bitmaskclient.providersetup.connectivity.DnsResolver;
import se.leap.bitmaskclient.providersetup.connectivity.OkHttpClientGenerator;
import se.leap.bitmaskclient.testutils.BackendMockResponses.BackendMockProvider;
import se.leap.bitmaskclient.testutils.matchers.BundleMatcher;

/**
 * Created by cyberta on 29.01.18.
 */

public class MockHelper {

    public static ResultReceiver mockResultReceiver(final int expectedResultCode) {
        ResultReceiver resultReceiver = mock(ResultReceiver.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                int resultCode = (int) arguments[0];
                assertEquals("expected resultCode: ", expectedResultCode, resultCode);
                return null;
            }
        }).when(resultReceiver).send(anyInt(), any(Bundle.class));
        return resultReceiver;
    }

    public static ResultReceiver mockResultReceiver(final int expectedResultCode, final Bundle expectedBundle) {
        ResultReceiver resultReceiver = mock(ResultReceiver.class);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                int resultCode = (int) arguments[0];
                Bundle bundle = (Bundle) arguments[1];
                Set<String> keys = expectedBundle.keySet();
                Iterator<String> iterator = keys.iterator();
                HashMap<String, Integer> expectedIntegers = new HashMap<>();
                HashMap<String, String> expectedStrings = new HashMap<>();
                HashMap<String, Boolean> expectedBooleans = new HashMap<>();
                HashMap<String, Parcelable> expectedParcelables = new HashMap<>();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    Object value = expectedBundle.get(key);

                    if (value instanceof Boolean) {
                        expectedBooleans.put(key, (Boolean) value);
                    } else if (value instanceof Integer) {
                        expectedIntegers.put(key, (Integer) value);
                    } else if (value instanceof String) {
                        expectedStrings.put(key, (String) value);
                    } else if (value instanceof Parcelable) {
                        expectedParcelables.put(key, (Parcelable) value);
                    }
                }
                assertThat("expected bundle: ", bundle, new BundleMatcher(expectedIntegers, expectedStrings, expectedBooleans, expectedParcelables));
                assertEquals("expected resultCode: ", expectedResultCode, resultCode);
                return null;
            }
        }).when(resultReceiver).send(anyInt(), any(Bundle.class));
        return resultReceiver;
    }

    public static class MockFileHelper implements FileHelper.FileHelperInterface {
        private final File file;
        private int persistFileCounter = 0;
        public MockFileHelper(File file) {
            this.file = file;
        }


        @Override
        public File createFile(File dir, String fileName) {
            return file;
        }

        @Override
        public void persistFile(File file, String content) throws IOException {
            persistFileCounter++;
        }

        public int getPersistFileCounter() {
            return persistFileCounter;
        }
    }

    public static FileHelper mockFileHelper(final File mockedFile) throws FileNotFoundException {
        return new FileHelper(new MockFileHelper(mockedFile));
    }

    public static PrivateKeyHelper mockPrivateKeyHelper() {
        return new PrivateKeyHelper(rsaKeyString -> new RSAPrivateKey() {
            @Override
            public BigInteger getPrivateExponent() {
                return BigInteger.TEN;
            }

            @Override
            public String getAlgorithm() {
                return "RSA";
            }

            @Override
            public String getFormat() {
                return null;
            }

            @Override
            public byte[] getEncoded() {
                return new byte[0];
            }

            @Override
            public BigInteger getModulus() {
                return BigInteger.ONE;
            }
        });

    }

    public static BuildConfigHelper mockBuildConfigHelper() {
        return mockBuildConfigHelper(true);
    }

    public static BuildConfigHelper mockBuildConfigHelper(boolean isDefaultBitmask) {
        return new BuildConfigHelper(new BuildConfigHelper.BuildConfigHelperInterface() {

            @Override
            public boolean hasObfuscationPinningDefaults() {
                return false;
            }

            @Override
            public String obfsvpnIP() {
                return null;
            }

            @Override
            public String obfsvpnPort() {
                return null;
            }

            @Override
            public String obfsvpnCert() {
                return null;
            }

            @Override
            public boolean useKcp() {
                return false;
            }

            @Override
            public boolean isDefaultBitmask() {
                return isDefaultBitmask;
            }
        });
    }

    public static CertificateHelper mockCertificateHelper(String mockedFingerprint) {
        return new CertificateHelper((certificate, encoding) -> mockedFingerprint);
    }

    public static PreferenceHelper mockPreferenceHelper(final Provider providerFromPrefs, SharedPreferences sharedPreferences) {
        PreferenceHelper preferenceHelper = new PreferenceHelper(sharedPreferences);

        sharedPreferences.edit().
                putString(PROVIDER_PRIVATE_KEY, providerFromPrefs.getPrivateKeyString()).
                putString(PROVIDER_VPN_CERTIFICATE, providerFromPrefs.getVpnCertificate()).
                putString(Provider.KEY, providerFromPrefs.getDefinitionString()).
                putString(Provider.CA_CERT_FINGERPRINT, providerFromPrefs.getCaCertFingerprint()).
                putString(Provider.GEOIP_URL, providerFromPrefs.getGeoipUrl().toString()).
                putString(Provider.MOTD_URL, providerFromPrefs.getMotdUrl().toString()).
                putString(PROVIDER_MOTD, providerFromPrefs.getMotdJsonString()).
                putLong(PROVIDER_MOTD_LAST_SEEN, providerFromPrefs.getLastMotdSeen()).
                putLong(PROVIDER_MOTD_LAST_UPDATED, providerFromPrefs.getLastMotdUpdate()).
                putStringSet(PROVIDER_MOTD_HASHES, providerFromPrefs.getMotdLastSeenHashes()).
                commit();

        return preferenceHelper;
    }

    public static PreferenceHelper mockPreferenceHelper(final Provider provider) {
        SharedPreferences sharedPreferences = new MockSharedPreferences();
        PreferenceHelper preferenceHelper = new PreferenceHelper(sharedPreferences);

        sharedPreferences.edit().
                putString(PROVIDER_PRIVATE_KEY, provider.getPrivateKeyString()).
                putString(PROVIDER_VPN_CERTIFICATE, provider.getVpnCertificate()).
                putString(Provider.KEY, provider.getDefinitionString()).
                putString(Provider.CA_CERT_FINGERPRINT, provider.getCaCertFingerprint()).
                putString(Provider.GEOIP_URL, provider.getGeoipUrl().toString()).
                putString(Provider.MOTD_URL, provider.getMotdUrl().toString()).
                putString(PROVIDER_MOTD, provider.getMotdJsonString()).
                putLong(PROVIDER_MOTD_LAST_SEEN, provider.getLastMotdSeen()).
                putLong(PROVIDER_MOTD_LAST_UPDATED, provider.getLastMotdUpdate()).
                putStringSet(PROVIDER_MOTD_HASHES, provider.getMotdLastSeenHashes()).
                commit();

        if (!provider.getDomain().isBlank()) {
            String providerDomain = provider.getDomain();
            sharedPreferences.edit().
                    putString(Provider.PROVIDER_IP + "." + providerDomain, provider.getProviderIp()).
                    putString(Provider.PROVIDER_API_IP + "." + providerDomain, provider.getProviderApiIp()).
                    putString(Provider.MAIN_URL + "." + providerDomain, provider.getMainUrl()).
                    putString(Provider.GEOIP_URL + "." + providerDomain, provider.getGeoipUrl()).
                    putString(Provider.MOTD_URL + "." + providerDomain, provider.getMotdUrl()).
                    putString(Provider.KEY + "." + providerDomain, provider.getDefinitionString()).
                    putString(Provider.CA_CERT + "." + providerDomain, provider.getCaCert()).
                    putString(PROVIDER_EIP_DEFINITION + "." + providerDomain, provider.getEipServiceJsonString()).
                    putString(PROVIDER_MOTD + "." + providerDomain, provider.getMotdJsonString()).
                    putStringSet(PROVIDER_MOTD_HASHES + "." + providerDomain, provider.getMotdLastSeenHashes()).
                    putLong(PROVIDER_MOTD_LAST_SEEN + "." + providerDomain, provider.getLastMotdSeen()).
                    putLong(PROVIDER_MOTD_LAST_UPDATED + "." + providerDomain, provider.getLastMotdUpdate()).
                    commit();
        }

        return preferenceHelper;
    }

    public static void mockPreferenceHelper(MockSharedPreferences preferences) {
        mockStatic(PreferenceHelper.class);

        when(getEipDefinitionFromPreferences()).thenAnswer(new Answer<JSONObject>() {
            @Override
            public JSONObject answer(InvocationOnMock invocation) throws Throwable {

                return getEipDefinitionFromPreferences(preferences);
            }
        });
    }

    public static ProviderObservable mockProviderObservable(Provider provider) {
        ProviderObservable observable = ProviderObservable.getInstance();
        observable.updateProvider(provider);
        return observable;
    }

    public static ProviderApiConnector mockProviderApiConnector(final BackendMockProvider.TestBackendErrorCase errorCase) throws IOException {
        return new ProviderApiConnector(BackendMockProvider.provideBackendResponsesFor(errorCase));
    }

    public static OkHttpClientGenerator mockClientGenerator(boolean resolveDNS) throws UnknownHostException {
        OkHttpClientGenerator mockClientGenerator = mock(OkHttpClientGenerator.class);
        OkHttpClient mockedOkHttpClient = mock(OkHttpClient.class);
        DnsResolver mockedDnsResolver = mock(DnsResolver.class);
        when(mockClientGenerator.initCommercialCAHttpClient(any(JSONObject.class), anyInt())).thenReturn(mockedOkHttpClient);
        when(mockClientGenerator.initSelfSignedCAHttpClient(anyString(), anyInt(), any(JSONObject.class))).thenReturn(mockedOkHttpClient);
        if (resolveDNS) {
            when(mockedDnsResolver.lookup(anyString())).thenReturn(new ArrayList<>());
        } else {
            when(mockedDnsResolver.lookup(anyString())).thenThrow(new UnknownHostException());
        }
        return mockClientGenerator;
    }

    public static OkHttpClientGenerator mockClientGenerator() throws UnknownHostException {
        return mockClientGenerator(true);
    }

    public static Resources mockResources(InputStream inputStream) throws IOException, JSONException {
        Resources mockedResources = mock(Resources.class, RETURNS_DEEP_STUBS);
        JSONObject errorMessages = new JSONObject(TestSetupHelper.getInputAsString(inputStream));


        when(mockedResources.getString(Mockito.eq(R.string.warning_corrupted_provider_details), anyString())).
                thenReturn(String.format(errorMessages.getString("warning_corrupted_provider_details"), "Bitmask"));
        when(mockedResources.getString(R.string.server_unreachable_message)).
                thenReturn(errorMessages.getString("server_unreachable_message"));
        when(mockedResources.getString(R.string.error_security_pinnedcertificate)).
                thenReturn(errorMessages.getString("error.security.pinnedcertificate"));
        when(mockedResources.getString(Mockito.eq(R.string.malformed_url), anyString())).
                thenReturn(String.format(errorMessages.getString("malformed_url"), "Bitmask"));
        when(mockedResources.getString(Mockito.eq(R.string.certificate_error), anyString())).
                thenReturn(String.format(errorMessages.getString("certificate_error"), "Bitmask"));
        when(mockedResources.getString(R.string.error_srp_math_error_user_message)).
                thenReturn(errorMessages.getString("error_srp_math_error_user_message"));
        when(mockedResources.getString(R.string.error_bad_user_password_user_message)).
                thenReturn(errorMessages.getString("error_bad_user_password_user_message"));
        when(mockedResources.getString(R.string.error_not_valid_password_user_message)).
                thenReturn(errorMessages.getString("error_not_valid_password_user_message"));
        when(mockedResources.getString(R.string.error_client_http_user_message)).
                thenReturn(errorMessages.getString("error_client_http_user_message"));
        when(mockedResources.getString(R.string.error_io_exception_user_message)).
                thenReturn(errorMessages.getString("error_io_exception_user_message"));
        when(mockedResources.getString(R.string.error_json_exception_user_message)).
                thenReturn(errorMessages.getString("error_json_exception_user_message"));
        when(mockedResources.getString(R.string.error_no_such_algorithm_exception_user_message)).
                thenReturn(errorMessages.getString("error_no_such_algorithm_exception_user_message"));
        when(mockedResources.getString(Mockito.eq(R.string.warning_corrupted_provider_details), anyString())).
                thenReturn(String.format(errorMessages.getString("warning_corrupted_provider_details"), "Bitmask"));
        when(mockedResources.getString(Mockito.eq(R.string.warning_corrupted_provider_cert), anyString())).
                thenReturn(String.format(errorMessages.getString("warning_corrupted_provider_cert"), "Bitmask"));
        when(mockedResources.getString(Mockito.eq(R.string.warning_expired_provider_cert), anyString())).
                thenReturn(String.format(errorMessages.getString("warning_expired_provider_cert"), "Bitmask"));
        when(mockedResources.getString(Mockito.eq(R.string.setup_error_text), anyString())).
                thenReturn(String.format(errorMessages.getString("setup_error_text"), "Bitmask"));
        when(mockedResources.getString(Mockito.eq(R.string.setup_error_text_custom), anyString())).
                thenReturn(String.format(errorMessages.getString("setup_error_text_custom"), "RiseupVPN"));
        when(mockedResources.getString(R.string.app_name)).
                thenReturn("Bitmask");
        when(mockedResources.getString(Mockito.eq(R.string.error_tor_timeout), anyString())).
                thenReturn(String.format(errorMessages.getString("error_tor_timeout"), "Bitmask"));
        when(mockedResources.getString(Mockito.eq(R.string.error_network_connection), anyString())).
                thenReturn(String.format(errorMessages.getString("error_network_connection"), "Bitmask"));
        return mockedResources;
    }

    public static Context mockContext() throws PackageManager.NameNotFoundException {
        Context context = mock(Context.class, RETURNS_DEEP_STUBS);
        when(context.getPackageName()).thenReturn("se.leap.bitmaskclient");
        PackageInfo mockPackageInfo = new PackageInfo();
        mockPackageInfo.versionName = "0.9.10";
        when(context.getPackageManager().getPackageInfo(anyString(), anyInt())).thenReturn(mockPackageInfo);
        return context;
    }
}
