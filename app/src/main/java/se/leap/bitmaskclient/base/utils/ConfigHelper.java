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
package se.leap.bitmaskclient.base.utils;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
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
import java.util.Calendar;

import se.leap.bitmaskclient.BuildConfig;
import se.leap.bitmaskclient.providersetup.ProviderAPI;
import se.leap.bitmaskclient.R;

import static se.leap.bitmaskclient.base.models.Constants.DEFAULT_BITMASK;

/**
 * Stores constants, and implements auxiliary methods used across all Bitmask Android classes.
 * Wraps BuildConfigFields for to support easier unit testing
 *
 * @author parmegv
 * @author MeanderingCode
 */
public class ConfigHelper {
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

    public static RSAPrivateKey parseRsaKeyFromString(String rsaKeyString) {
        RSAPrivateKey key;
        try {
            KeyFactory kf;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                kf = KeyFactory.getInstance("RSA", "BC");
            } else {
                kf = KeyFactory.getInstance("RSA");
            }
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
        } catch (NullPointerException e) {
            e.printStackTrace();
            return null;
        } catch (NoSuchProviderException e) {
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

    public static void ensureNotOnMainThread(@NonNull Context context) throws IllegalStateException{
        Looper looper = Looper.myLooper();
        if (looper != null && looper == context.getMainLooper()) {
            throw new IllegalStateException(
                    "calling this from your main thread can lead to deadlock");
        }
    }

    public static boolean isDefaultBitmask() {
        return BuildConfig.FLAVOR_branding.equals(DEFAULT_BITMASK);
    }

    public static boolean preferAnonymousUsage() {
        return BuildConfig.priotize_anonymous_usage;
    }

    public static int getCurrentTimezone() {
        return Calendar.getInstance().get(Calendar.ZONE_OFFSET) / 3600000;
    }

    public static String getProviderFormattedString(Resources resources, @StringRes int resourceId) {
        String appName = resources.getString(R.string.app_name);
        return resources.getString(resourceId, appName);
    }

    public static boolean stringEqual(@Nullable String string1, @Nullable String string2) {
        return (string1 == null && string2 == null) ||
                (string1 != null && string1.equals(string2));
    }

    public static String getApkFileName() {
        try {
            return BuildConfig.update_apk_url.substring(BuildConfig.update_apk_url.lastIndexOf("/"));
        } catch (Exception e) {
            return null;
        }
    }

    public static String getVersionFileName() {
        try {
            return BuildConfig.version_file_url.substring(BuildConfig.version_file_url.lastIndexOf("/"));
        } catch (Exception e) {
            return null;
        }
    }

    public static String getSignatureFileName() {
        try {
            return BuildConfig.signature_url.substring(BuildConfig.signature_url.lastIndexOf("/"));
        } catch (Exception e) {
            return null;
        }
    }

}
