/**
 * Copyright (c) 2013 LEAP Encryption Access Project and contributers
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
package se.leap.bitmaskclient;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Base64;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static se.leap.bitmaskclient.Constants.ALWAYS_ON_SHOW_DIALOG;
import static se.leap.bitmaskclient.Constants.DEFAULT_SHARED_PREFS_BATTERY_SAVER;
import static se.leap.bitmaskclient.Constants.PREFERENCES_APP_VERSION;
import static se.leap.bitmaskclient.Constants.PROVIDER_CONFIGURED;
import static se.leap.bitmaskclient.Constants.PROVIDER_EIP_DEFINITION;
import static se.leap.bitmaskclient.Constants.PROVIDER_PRIVATE_KEY;
import static se.leap.bitmaskclient.Constants.PROVIDER_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.Constants.SHARED_PREFERENCES;

/**
 * Stores constants, and implements auxiliary methods used across all Bitmask Android classes.
 *
 * @author parmegv
 * @author MeanderingCode
 */
public class ConfigHelper {
    private static final String TAG = ConfigHelper.class.getName();
    private static KeyStore keystore_trusted;

    final public static String NG_1024 =
            "eeaf0ab9adb38dd69c33f80afa8fc5e86072618775ff3c0b9ea2314c9c256576d674df7496ea81d3383b4813d692c6e0e0d5d8e250b98be48e495c1d6089dad15dc7d7b46154d6b6ce8ef4ad69b15d4982559b297bcf1885c529f566660e57ec68edbc3c05726cc02fd4cbf4976eaa9afd5138fe8376435b9fc61d2fc0eb06e3";
    final public static BigInteger G = new BigInteger("2");

    public static boolean checkErroneousDownload(String downloadedString) {
        try {
            if (downloadedString == null || downloadedString.isEmpty() || new JSONObject(downloadedString).has(ProviderAPI.ERRORS) || new JSONObject(downloadedString).has(ProviderAPI.BACKEND_ERROR_KEY)) {
                return true;
            } else {
                return false;
            }
        } catch (NullPointerException | JSONException e) {
            return false;
        }
    }

    /**
     * Treat the input as the MSB representation of a number,
     * and lop off leading zero elements.  For efficiency, the
     * input is simply returned if no leading zeroes are found.
     *
     * @param in array to be trimmed
     */
    public static byte[] trim(byte[] in) {
        if (in.length == 0 || in[0] != 0)
            return in;

        int len = in.length;
        int i = 1;
        while (in[i] == 0 && i < len)
            ++i;
        byte[] ret = new byte[len - i];
        System.arraycopy(in, i, ret, 0, len - i);
        return ret;
    }

    public static X509Certificate parseX509CertificateFromString(String certificateString) {
        java.security.cert.Certificate certificate = null;
        CertificateFactory cf;
        try {
            cf = CertificateFactory.getInstance("X.509");

            certificateString = certificateString.replaceFirst("-----BEGIN CERTIFICATE-----", "").replaceFirst("-----END CERTIFICATE-----", "").trim();
            byte[] cert_bytes = Base64.decode(certificateString);
            InputStream caInput = new ByteArrayInputStream(cert_bytes);
            try {
                certificate = cf.generateCertificate(caInput);
                System.out.println("ca=" + ((X509Certificate) certificate).getSubjectDN());
            } finally {
                caInput.close();
            }
        } catch (NullPointerException | CertificateException | IOException | IllegalArgumentException e) {
            return null;
        }
        return (X509Certificate) certificate;
    }

    public static String loadInputStreamAsString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    //allows us to mock FileInputStream
    public static InputStream getInputStreamFrom(String filePath) throws FileNotFoundException {
        return new FileInputStream(filePath);
    }

    protected static RSAPrivateKey parseRsaKeyFromString(String rsaKeyString) {
        RSAPrivateKey key;
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA", "BC");
            rsaKeyString = rsaKeyString.replaceFirst("-----BEGIN RSA PRIVATE KEY-----", "").replaceFirst("-----END RSA PRIVATE KEY-----", "");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.decode(rsaKeyString));
            key = (RSAPrivateKey) kf.generatePrivate(keySpec);
        } catch (InvalidKeySpecException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } catch (NoSuchProviderException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } catch (NullPointerException e) {
            e.printStackTrace();
            return null;
        }

        return key;
    }

    private static String byteArrayToHex(byte[] input) {
        int readBytes = input.length;
        StringBuffer hexData = new StringBuffer();
        int onebyte;
        for (int i = 0; i < readBytes; i++) {
            onebyte = ((0x000000ff & input[i]) | 0xffffff00);
            hexData.append(Integer.toHexString(onebyte).substring(6));
        }
        return hexData.toString();
    }

    /**
     * Calculates the hexadecimal representation of a sha256/sha1 fingerprint of a certificate
     *
     * @param certificate
     * @param encoding
     * @return
     * @throws NoSuchAlgorithmException
     * @throws CertificateEncodingException
     */
    @NonNull
    public static String getFingerprintFromCertificate(X509Certificate certificate, String encoding) throws NoSuchAlgorithmException, CertificateEncodingException /*, UnsupportedEncodingException*/ {
        byte[] byteArray = MessageDigest.getInstance(encoding).digest(certificate.getEncoded());
        return byteArrayToHex(byteArray);
    }
    
    /**
     * Adds a new X509 certificate given its input stream and its provider name
     *
     * @param provider    used to store the certificate in the keystore
     * @param inputStream from which X509 certificate must be generated.
     */
    public static void addTrustedCertificate(String provider, InputStream inputStream) {
        CertificateFactory cf;
        try {
            cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert =
                    (X509Certificate) cf.generateCertificate(inputStream);
            keystore_trusted.setCertificateEntry(provider, cert);
        } catch (CertificateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (KeyStoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Adds a new X509 certificate given in its string from and using its provider name
     *
     * @param provider    used to store the certificate in the keystore
     * @param certificate
     */
    public static void addTrustedCertificate(String provider, String certificate) {

        try {
            X509Certificate cert = ConfigHelper.parseX509CertificateFromString(certificate);
            if (keystore_trusted == null) {
                keystore_trusted = KeyStore.getInstance("BKS");
                keystore_trusted.load(null);
            }
            keystore_trusted.setCertificateEntry(provider, cert);
        } catch (KeyStoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (CertificateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * @return class wide keystore
     */
    public static KeyStore getKeystore() {
        return keystore_trusted;
    }


    public static boolean providerInSharedPreferences(@NonNull SharedPreferences preferences) {
        return preferences.getBoolean(PROVIDER_CONFIGURED, false);
    }

    public static Provider getSavedProviderFromSharedPreferences(@NonNull SharedPreferences preferences) {
        Provider provider = new Provider();
        try {
            provider.setMainUrl(new URL(preferences.getString(Provider.MAIN_URL, "")));
            provider.define(new JSONObject(preferences.getString(Provider.KEY, "")));
            provider.setCaCert(preferences.getString(Provider.CA_CERT, ""));
            provider.setVpnCertificate(preferences.getString(PROVIDER_VPN_CERTIFICATE, ""));
            provider.setPrivateKey(preferences.getString(PROVIDER_PRIVATE_KEY, ""));
            provider.setEipServiceJson(new JSONObject(preferences.getString(PROVIDER_EIP_DEFINITION, "")));
        } catch (MalformedURLException | JSONException e) {
            e.printStackTrace();
        }

        return provider;
    }

    public static String getFromPersistedProvider(String toFetch, String providerDomain, SharedPreferences preferences) {
        return preferences.getString(toFetch + "." + providerDomain, "");
    }

    public static String getProviderName(String provider) {
        return getProviderName(null, provider);
    }

    public static String getProviderName(@Nullable SharedPreferences preferences) {
        return getProviderName(preferences,null);
    }

    public static String getProviderName(@Nullable SharedPreferences preferences, @Nullable String provider) {
        if (provider == null && preferences != null) {
            provider = preferences.getString(Provider.KEY, "");
        }
        try {
            JSONObject providerJson = new JSONObject(provider);
            String lang = Locale.getDefault().getLanguage();
            return providerJson.getJSONObject(Provider.NAME).getString(lang);
        } catch (JSONException e) {
            try {
                JSONObject providerJson = new JSONObject(provider);
                return providerJson.getJSONObject(Provider.NAME).getString("en");
            } catch (JSONException e2) {
                return null;
            }
        } catch (NullPointerException npe) {
            return null;
        }
    }

    public static String getProviderDomain(SharedPreferences preferences) {
        return getProviderDomain(preferences, null);
    }

    public static String getProviderDomain(String provider) {
        return getProviderDomain(null, provider);
    }

    public static String getProviderDomain(@Nullable SharedPreferences preferences, @Nullable String provider) {
        if (provider == null && preferences != null) {
            provider = preferences.getString(Provider.KEY, "");
        }
        try {
            JSONObject providerJson = new JSONObject(provider);
            return providerJson.getString(Provider.DOMAIN);
        } catch (JSONException | NullPointerException e) {
            return null;
        }
    }

    public static String getDescription(SharedPreferences preferences) {
        try {
            JSONObject providerJson = new JSONObject(preferences.getString(Provider.KEY, ""));
            String lang = Locale.getDefault().getLanguage();
            return providerJson.getJSONObject(Provider.DESCRIPTION).getString(lang);
        } catch (JSONException e) {
            try {
                JSONObject providerJson = new JSONObject(preferences.getString(Provider.KEY, ""));
                return providerJson.getJSONObject(Provider.DESCRIPTION).getString("en");
            } catch (JSONException e1) {
                return null;
            }
        }
    }

    // TODO: replace commit with apply after refactoring EIP
    //FIXME: don't save private keys in shared preferences! use the keystore
    public static void storeProviderInPreferences(SharedPreferences preferences, Provider provider) {
        preferences.edit().putBoolean(PROVIDER_CONFIGURED, true).
                putString(Provider.MAIN_URL, provider.getMainUrlString()).
                putString(Provider.KEY, provider.getDefinitionString()).
                putString(Provider.CA_CERT, provider.getCaCert()).
                putString(PROVIDER_EIP_DEFINITION, provider.getEipServiceJsonString()).
                putString(PROVIDER_PRIVATE_KEY, provider.getPrivateKey()).
                putString(PROVIDER_VPN_CERTIFICATE, provider.getVpnCertificate()).
                commit();

        String providerDomain = provider.getDomain();
        preferences.edit().putBoolean(PROVIDER_CONFIGURED, true).
                putString(Provider.MAIN_URL + "." + providerDomain, provider.getMainUrlString()).
                putString(Provider.KEY + "." + providerDomain, provider.getDefinitionString()).
                putString(Provider.CA_CERT + "." + providerDomain, provider.getCaCert()).
                putString(PROVIDER_EIP_DEFINITION + "." + providerDomain, provider.getEipServiceJsonString()).
                apply();
    }


    public static void clearDataOfLastProvider(SharedPreferences preferences) {
        clearDataOfLastProvider(preferences, false);
    }

    @Deprecated
    public static void clearDataOfLastProvider(SharedPreferences preferences, boolean commit) {
        Map<String, ?> allEntries = preferences.getAll();
        List<String> lastProvidersKeys = new ArrayList<>();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            //sort out all preferences that don't belong to the last provider
            if (entry.getKey().startsWith(Provider.KEY + ".") ||
                    entry.getKey().startsWith(Provider.CA_CERT + ".") ||
                    entry.getKey().startsWith(Provider.CA_CERT_FINGERPRINT + "." )||
                    entry.getKey().equals(PREFERENCES_APP_VERSION)
                    ) {
                continue;
            }
            lastProvidersKeys.add(entry.getKey());
        }

        SharedPreferences.Editor preferenceEditor = preferences.edit();
        for (String key : lastProvidersKeys) {
            preferenceEditor.remove(key);
        }
        if (commit) {
            preferenceEditor.commit();
        } else {
            preferenceEditor.apply();
        }
    }

    public static void deleteProviderDetailsFromPreferences(@NonNull SharedPreferences preferences, String providerDomain) {
        preferences.edit().
                remove(Provider.KEY + "." + providerDomain).
                remove(Provider.CA_CERT + "." + providerDomain).
                remove(Provider.CA_CERT_FINGERPRINT + "." + providerDomain).
                remove(Provider.MAIN_URL + "." + providerDomain).
                remove(Provider.KEY + "." + providerDomain).
                remove(Provider.CA_CERT + "." + providerDomain).
                remove(PROVIDER_EIP_DEFINITION + "." + providerDomain).
                remove(PROVIDER_PRIVATE_KEY + "." + providerDomain).
                remove(PROVIDER_VPN_CERTIFICATE + "." + providerDomain).
                apply();
    }

    public static void saveBattery(Context context, boolean isEnabled) {
        if (context == null) {
            return;
        }
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putBoolean(DEFAULT_SHARED_PREFS_BATTERY_SAVER, isEnabled).apply();
    }

    public static boolean getSaveBattery(Context context) {
        if (context == null) {
            return false;
        }
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(DEFAULT_SHARED_PREFS_BATTERY_SAVER, false);
    }

    public static void saveShowAlwaysOnDialog(Context context, boolean showAlwaysOnDialog) {
        if (context == null) {
            return;
        }
        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        preferences.edit().putBoolean(ALWAYS_ON_SHOW_DIALOG, showAlwaysOnDialog).apply();
    }

    public static boolean getShowAlwaysOnDialog(Context context) {
        if (context == null) {
            return true;
        }
        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return preferences.getBoolean(ALWAYS_ON_SHOW_DIALOG, true);
    }

    public static JSONObject getEipDefinitionFromPreferences(SharedPreferences preferences) {
        JSONObject result = new JSONObject();
        try {
            String eipDefinitionString = preferences.getString(PROVIDER_EIP_DEFINITION, "");
            if (!eipDefinitionString.isEmpty()) {
                result = new JSONObject(eipDefinitionString);
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    public static void ensureNotOnMainThread(@NonNull Context context) throws IllegalStateException{
        Looper looper = Looper.myLooper();
        if (looper != null && looper == context.getMainLooper()) {
            throw new IllegalStateException(
                    "calling this from your main thread can lead to deadlock");
        }
    }
}
