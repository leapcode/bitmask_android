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

package se.leap.bitmaskclient.testutils;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.ArgumentMatchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import se.leap.bitmaskclient.ConfigHelper;
import se.leap.bitmaskclient.OkHttpClientGenerator;
import se.leap.bitmaskclient.ProviderApiConnector;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.testutils.answers.BackendAnswerFabric;
import se.leap.bitmaskclient.testutils.matchers.BundleMatcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static se.leap.bitmaskclient.testutils.answers.BackendAnswerFabric.TestBackendErrorCase.ERROR_NO_CONNECTION;
import static se.leap.bitmaskclient.testutils.answers.BackendAnswerFabric.getAnswerForErrorcase;

/**
 * Created by cyberta on 08.10.17.
 */

public class TestSetupHelper {



    public static String getInputAsString(InputStream fileAsInputStream) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(fileAsInputStream));
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();
        while (line != null) {
            sb.append(line);
            line = br.readLine();
        }

        return sb.toString();
    }

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

    public static void mockFingerprintForCertificate(String mockedFingerprint) throws CertificateEncodingException, NoSuchAlgorithmException {
        mockStatic(ConfigHelper.class);
        when(ConfigHelper.getFingerprintFromCertificate(any(X509Certificate.class), anyString())).thenReturn(mockedFingerprint);
        when(ConfigHelper.checkErroneousDownload(anyString())).thenCallRealMethod();
        when(ConfigHelper.base64toHex(anyString())).thenCallRealMethod();
        when(ConfigHelper.parseX509CertificateFromString(anyString())).thenCallRealMethod();
    }

    public static void mockProviderApiConnector(final BackendAnswerFabric.TestBackendErrorCase errorCase) throws IOException {
        mockStatic(ProviderApiConnector.class);
        when(ProviderApiConnector.canConnect(any(OkHttpClient.class), anyString())).thenReturn(errorCase != ERROR_NO_CONNECTION);
        when(ProviderApiConnector.requestStringFromServer(anyString(), anyString(), nullable(String.class), ArgumentMatchers.<Pair<String,String>>anyList(), any(OkHttpClient.class))).thenAnswer(getAnswerForErrorcase(errorCase));
    }

    public static OkHttpClientGenerator mockClientGenerator() {
        OkHttpClientGenerator mockClientGenerator = mock(OkHttpClientGenerator.class);
        OkHttpClient mockedOkHttpClient = mock(OkHttpClient.class);
        when(mockClientGenerator.initCommercialCAHttpClient(any(JSONObject.class))).thenReturn(mockedOkHttpClient);
        when(mockClientGenerator.initSelfSignedCAHttpClient(any(JSONObject.class))).thenReturn(mockedOkHttpClient);
        when(mockClientGenerator.initSelfSignedCAHttpClient(any(JSONObject.class), anyString())).thenReturn(mockedOkHttpClient);
        return mockClientGenerator;
    }

    public static Resources mockResources(InputStream inputStream) throws IOException, JSONException {
        Resources mockedResources = mock(Resources.class, RETURNS_DEEP_STUBS);
        JSONObject errorMessages = new JSONObject(getInputAsString(inputStream));


        when(mockedResources.getString(R.string.warning_corrupted_provider_details)).
                thenReturn(errorMessages.getString("warning_corrupted_provider_details"));
        when(mockedResources.getString(R.string.server_unreachable_message)).
                thenReturn(errorMessages.getString("server_unreachable_message"));
        when(mockedResources.getString(R.string.error_security_pinnedcertificate)).
                thenReturn(errorMessages.getString("error.security.pinnedcertificate"));
        when(mockedResources.getString(R.string.malformed_url)).
                thenReturn(errorMessages.getString("malformed_url"));
        when(mockedResources.getString(R.string.certificate_error)).
                thenReturn(errorMessages.getString("certificate_error"));
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
        when(mockedResources.getString(R.string.warning_corrupted_provider_details)).
                thenReturn(errorMessages.getString("warning_corrupted_provider_details"));
        when(mockedResources.getString(R.string.warning_corrupted_provider_cert)).
                thenReturn(errorMessages.getString("warning_corrupted_provider_cert"));
        when(mockedResources.getString(R.string.warning_expired_provider_cert)).
                thenReturn(errorMessages.getString("warning_expired_provider_cert"));
        return mockedResources;
    }

}
