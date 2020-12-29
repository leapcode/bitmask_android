package se.leap.bitmaskclient.testutils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.ResultReceiver;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.OkHttpClient;
import se.leap.bitmaskclient.providersetup.connectivity.OkHttpClientGenerator;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.models.ProviderObservable;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.testutils.BackendMockResponses.BackendMockProvider;
import se.leap.bitmaskclient.testutils.matchers.BundleMatcher;
import se.leap.bitmaskclient.base.utils.ConfigHelper;
import se.leap.bitmaskclient.base.utils.FileHelper;
import se.leap.bitmaskclient.base.utils.InputStreamHelper;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_PRIVATE_KEY;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.base.utils.FileHelper.createFile;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getEipDefinitionFromPreferences;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getFromPersistedProvider;

/**
 * Created by cyberta on 29.01.18.
 */

public class MockHelper {
    @NonNull
    public static Bundle mockBundle() {
        final Map<String, Boolean> fakeBooleanBundle = new HashMap<>();
        final Map<String, String> fakeStringBundle = new HashMap<>();
        final Map<String, Integer> fakeIntBundle = new HashMap<>();
        final Map<String, Parcelable> fakeParcelableBundle = new HashMap<>();

        Bundle bundle = mock(Bundle.class);

        //mock String values in Bundle
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                String key = ((String) arguments[0]);
                String value = ((String) arguments[1]);
                fakeStringBundle.put(key, value);
                return null;
            }
        }).when(bundle).putString(anyString(), anyString());
        when(bundle.getString(anyString())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                String key = ((String) arguments[0]);
                return fakeStringBundle.get(key);
            }
        });

        //mock Boolean values in Bundle
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                String key = ((String) arguments[0]);
                Boolean value = ((boolean) arguments[1]);
                fakeBooleanBundle.put(key, value);
                return null;
            }
        }).when(bundle).putBoolean(anyString(), anyBoolean());
        when(bundle.getBoolean(anyString())).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                String key = ((String) arguments[0]);
                return fakeBooleanBundle.get(key);
            }
        });

        //mock Integer values in Bundle
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                String key = ((String) arguments[0]);
                Integer value = ((int) arguments[1]);
                fakeIntBundle.put(key, value);
                return null;
            }
        }).when(bundle).putInt(anyString(), anyInt());
        when(bundle.getInt(anyString())).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                String key = ((String) arguments[0]);
                return fakeIntBundle.get(key);
            }
        });

        //mock Parcelable values in Bundle
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                String key = ((String) arguments[0]);
                Parcelable value = ((Parcelable) arguments[1]);
                fakeParcelableBundle.put(key, value);
                return null;
            }
        }).when(bundle).putParcelable(anyString(), any(Parcelable.class));
        when(bundle.getParcelable(anyString())).thenAnswer(new Answer<Parcelable>() {
            @Override
            public Parcelable answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                String key = ((String) arguments[0]);
                return fakeParcelableBundle.get(key);
            }
        });

        //mock get
        when(bundle.get(anyString())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                String key = ((String) arguments[0]);
                if (fakeBooleanBundle.containsKey(key)) {
                    return fakeBooleanBundle.get(key);
                } else if (fakeIntBundle.containsKey(key)) {
                    return fakeIntBundle.get(key);
                } else if (fakeStringBundle.containsKey(key)) {
                    return fakeStringBundle.get(key);
                } else {
                    return fakeParcelableBundle.get(key);
                }
            }
        });

        //mock getKeySet
       when(bundle.keySet()).thenAnswer(new Answer<Set<String>>() {
           @Override
           public Set<String> answer(InvocationOnMock invocation) throws Throwable {
               //this whole approach as a drawback:
               //you should not add the same keys for values of different types
               HashSet<String> keys = new HashSet<String>();
               keys.addAll(fakeBooleanBundle.keySet());
               keys.addAll(fakeIntBundle.keySet());
               keys.addAll(fakeStringBundle.keySet());
               keys.addAll(fakeParcelableBundle.keySet());
               return keys;
           }
        });

        //mock containsKey
        when(bundle.containsKey(anyString())).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                String key = (String) invocation.getArguments()[0];
                return fakeBooleanBundle.containsKey(key) ||
                        fakeStringBundle.containsKey(key) ||
                        fakeIntBundle.containsKey(key) ||
                        fakeParcelableBundle.containsKey(key);
            }
        });

        return bundle;
    }

    public static Intent mockIntent() {
        Intent intent = mock(Intent.class);
        final String[] action = new String[1];
        final Map<String, Object> fakeExtras = new HashMap<>();
        final List<String> categories = new ArrayList<>();


        //mock Action in intent
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                action[0] = ((String) arguments[0]);
                return null;
            }
        }).when(intent).setAction(anyString());
        when(intent.getAction()).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return action[0];
            }
        });

        //mock Bundle in intent extras
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                String key = ((String) arguments[0]);
                Bundle value = ((Bundle) arguments[1]);
                fakeExtras.put(key, value);
                return null;
            }
        }).when(intent).putExtra(anyString(), any(Bundle.class));
        when(intent.getBundleExtra(anyString())).thenAnswer(new Answer<Bundle>() {
            @Override
            public Bundle answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                String key = ((String) arguments[0]);
                return (Bundle) fakeExtras.get(key);
            }
        });

        //mock Parcelable in intent extras
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                String key = ((String) arguments[0]);
                Parcelable value = ((Parcelable) arguments[1]);
                fakeExtras.put(key, value);
                return null;
            }
        }).when(intent).putExtra(anyString(), any(Parcelable.class));
        when(intent.getParcelableExtra(anyString())).thenAnswer(new Answer<Parcelable>() {
            @Override
            public Parcelable answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                String key = ((String) arguments[0]);
                return (Parcelable) fakeExtras.get(key);
            }
        });
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                categories.add(((String) arguments[0]));
                return null;
            }
        }).when(intent).addCategory(anyString());

        when(intent.getCategories()).thenAnswer(new Answer<Set<String>>() {
            @Override
            public Set<String> answer(InvocationOnMock invocation) throws Throwable {
                return new HashSet<>(categories);
            }
        });

        return intent;
    }

    public static void mockTextUtils() {
        mockStatic(TextUtils.class);

        when(TextUtils.equals(any(CharSequence.class), any(CharSequence.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                CharSequence a = (CharSequence) invocation.getArguments()[0];
                CharSequence b = (CharSequence) invocation.getArguments()[1];
                if (a == b) return true;
                int length;
                if (a != null && b != null && (length = a.length()) == b.length()) {
                    if (a instanceof String && b instanceof String) {
                        return a.equals(b);
                    } else {
                        for (int i = 0; i < length; i++) {
                            if (a.charAt(i) != b.charAt(i)) return false;
                        }
                        return true;
                    }
                }
                return false;
            }
        });

        when(TextUtils.isEmpty(any(CharSequence.class))).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                CharSequence param = (CharSequence) invocation.getArguments()[0];
                return param == null || param.length() == 0;
            }
        });
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

    public static void mockInputStreamHelper() throws FileNotFoundException {
        mockStatic(InputStreamHelper.class);
        when(InputStreamHelper.loadInputStreamAsString(any(InputStream.class))).thenCallRealMethod();
        when(InputStreamHelper.getInputStreamFrom(anyString())).thenAnswer(new Answer<InputStream>() {
            @Override
            public InputStream answer(InvocationOnMock invocation) throws Throwable {
                String filename = (String) invocation.getArguments()[0];
                return getClass().getClassLoader().getResourceAsStream(filename);
            }
        });

    }

    public static void mockFileHelper(final File mockedFile) throws FileNotFoundException {
        mockStatic(FileHelper.class);
        when(createFile(any(File.class), anyString())).thenReturn(mockedFile);
    }

    public static void mockConfigHelper(String mockedFingerprint) throws CertificateEncodingException, NoSuchAlgorithmException {
        // FIXME use MockSharedPreferences instead of provider
        mockStatic(ConfigHelper.class);
        when(ConfigHelper.getFingerprintFromCertificate(any(X509Certificate.class), anyString())).thenReturn(mockedFingerprint);
        when(ConfigHelper.checkErroneousDownload(anyString())).thenCallRealMethod();
        when(ConfigHelper.parseX509CertificateFromString(anyString())).thenCallRealMethod();
        when(ConfigHelper.getProviderFormattedString(any(Resources.class), anyInt())).thenCallRealMethod();
    }

    public static void mockPreferenceHelper(final Provider providerFromPrefs) {
        // FIXME use MockSharedPreferences instead of provider
        mockStatic(PreferenceHelper.class);
        when(getFromPersistedProvider(anyString(), anyString(), any(SharedPreferences.class))).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                String key = (String) invocation.getArguments()[0];
                switch (key) {
                    case PROVIDER_PRIVATE_KEY:
                        return providerFromPrefs.getPrivateKey();
                    case PROVIDER_VPN_CERTIFICATE:
                        return providerFromPrefs.getVpnCertificate();
                    case Provider.KEY:
                        return providerFromPrefs.getDefinition().toString();
                    case Provider.CA_CERT_FINGERPRINT:
                        return providerFromPrefs.getCaCertFingerprint();
                    case Provider.CA_CERT:
                        return providerFromPrefs.getCaCert();
                    case Provider.GEOIP_URL:
                        return providerFromPrefs.getGeoipUrl().toString();

                }
                return null;
            }
        });
    }

    public static void mockPreferenceHelper(MockSharedPreferences preferences) {
        mockStatic(PreferenceHelper.class);

        when(getEipDefinitionFromPreferences(any(SharedPreferences.class))).thenAnswer(new Answer<JSONObject>() {
            @Override
            public JSONObject answer(InvocationOnMock invocation) throws Throwable {
                return getEipDefinitionFromPreferences(preferences);
            }
        });
    }

    public static void mockProviderObserver(Provider provider) {
        ProviderObservable observable = ProviderObservable.getInstance();
        observable.updateProvider(provider);
        mockStatic(ProviderObservable.class);
        when(ProviderObservable.getInstance()).thenAnswer((Answer<ProviderObservable>) invocation -> observable);
    }

    public static void mockFingerprintForCertificate(String mockedFingerprint) throws CertificateEncodingException, NoSuchAlgorithmException {
        mockStatic(ConfigHelper.class);
        when(ConfigHelper.getFingerprintFromCertificate(any(X509Certificate.class), anyString())).thenReturn(mockedFingerprint);
        when(ConfigHelper.checkErroneousDownload(anyString())).thenCallRealMethod();
        when(ConfigHelper.parseX509CertificateFromString(anyString())).thenCallRealMethod();
        when(ConfigHelper.getProviderFormattedString(any(Resources.class), anyInt())).thenCallRealMethod();
    }

    public static void mockProviderApiConnector(final BackendMockProvider.TestBackendErrorCase errorCase) throws IOException {
        BackendMockProvider.provideBackendResponsesFor(errorCase);
    }

    public static OkHttpClientGenerator mockClientGenerator(boolean resolveDNS) throws UnknownHostException {
        OkHttpClientGenerator mockClientGenerator = mock(OkHttpClientGenerator.class);
        OkHttpClient mockedOkHttpClient = mock(OkHttpClient.class, RETURNS_DEEP_STUBS);
        when(mockClientGenerator.initCommercialCAHttpClient(any(JSONObject.class))).thenReturn(mockedOkHttpClient);
        when(mockClientGenerator.initSelfSignedCAHttpClient(anyString(), any(JSONObject.class))).thenReturn(mockedOkHttpClient);
        if (resolveDNS) {
            when(mockedOkHttpClient.dns().lookup(anyString())).thenReturn(new ArrayList<>());
        } else {
            when(mockedOkHttpClient.dns().lookup(anyString())).thenThrow(new UnknownHostException());
        }
        return mockClientGenerator;
    }

    public static OkHttpClientGenerator mockClientGenerator() throws UnknownHostException {
        return mockClientGenerator(true);
    }

    public static Resources mockResources(InputStream inputStream) throws IOException, JSONException {
        Resources mockedResources = mock(Resources.class, RETURNS_DEEP_STUBS);
        JSONObject errorMessages = new JSONObject(TestSetupHelper.getInputAsString(inputStream));


        when(mockedResources.getString(eq(R.string.warning_corrupted_provider_details), anyString())).
                thenReturn(String.format(errorMessages.getString("warning_corrupted_provider_details"), "Bitmask"));
        when(mockedResources.getString(R.string.server_unreachable_message)).
                thenReturn(errorMessages.getString("server_unreachable_message"));
        when(mockedResources.getString(R.string.error_security_pinnedcertificate)).
                thenReturn(errorMessages.getString("error.security.pinnedcertificate"));
        when(mockedResources.getString(eq(R.string.malformed_url), anyString())).
                thenReturn(String.format(errorMessages.getString("malformed_url"), "Bitmask"));
        when(mockedResources.getString(eq(R.string.certificate_error), anyString())).
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
        when(mockedResources.getString(eq(R.string.warning_corrupted_provider_details), anyString())).
                thenReturn(String.format(errorMessages.getString("warning_corrupted_provider_details"), "Bitmask"));
        when(mockedResources.getString(eq(R.string.warning_corrupted_provider_cert), anyString())).
                thenReturn(String.format(errorMessages.getString("warning_corrupted_provider_cert"), "Bitmask"));
        when(mockedResources.getString(eq(R.string.warning_expired_provider_cert), anyString())).
                thenReturn(String.format(errorMessages.getString("warning_expired_provider_cert"), "Bitmask"));
        when(mockedResources.getString(eq(R.string.setup_error_text), anyString())).
                thenReturn(String.format(errorMessages.getString("setup_error_text"), "Bitmask"));
        when(mockedResources.getString(R.string.app_name)).
                thenReturn("Bitmask");
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
