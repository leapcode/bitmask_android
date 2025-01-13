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

import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Looper;
import android.util.Patterns;
import android.webkit.URLUtil;

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
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.blinkt.openvpn.core.NativeUtils;
import okhttp3.internal.publicsuffix.PublicSuffixDatabase;
import se.leap.bitmaskclient.BuildConfig;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.providersetup.ProviderAPI;

/**
 * Stores constants, and implements auxiliary methods used across all Bitmask Android classes.
 * Wraps BuildConfigFields for to support easier unit testing
 *
 * @author parmegv
 * @author MeanderingCode
 * @author cyberta
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

    public static ArrayList<X509Certificate> parseX509CertificatesFromString(String certificateString) {
        ArrayList<X509Certificate> certificates = new ArrayList<>();
        CertificateFactory cf;
        try {
            cf = CertificateFactory.getInstance("X.509");
            Pattern pattern = Pattern.compile("((-----BEGIN CERTIFICATE-----)([A-Za-z0-9+/=\\n]+)(-----END CERTIFICATE-----)+)");
            Matcher matcher = pattern.matcher(certificateString);
            while (matcher.find()) {
                String certificate = matcher.group(3);
                if (certificate == null) continue;
                byte[] certBytes = Base64.decode(certificate.trim());
                try (InputStream caInput = new ByteArrayInputStream(certBytes)) {
                    X509Certificate x509certificate = (X509Certificate) cf.generateCertificate(caInput);
                    certificates.add(x509certificate);
                    System.out.println("ca=" + x509certificate.getSubjectDN() + ", SAN= " + x509certificate.getSubjectAlternativeNames());
                } catch (IOException | CertificateException | NullPointerException | IllegalArgumentException | ClassCastException e) {
                    e.printStackTrace();
                }
            }
            return certificates;
        } catch (CertificateException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String parseX509CertificatesToString(ArrayList<X509Certificate> certs) {
        StringBuilder sb = new StringBuilder();
        for (X509Certificate certificate : certs) {

            byte[] derCert = new byte[0];
            try {
                derCert = certificate.getEncoded();
                byte[] encodedCert = Base64.encode(derCert);
                String base64Cert = new String(encodedCert);

                // add cert header
                sb.append("-----BEGIN CERTIFICATE-----\n");

                // split base64 string into lines of 64 characters
                int index = 0;
                while (index < base64Cert.length()) {
                    sb.append(base64Cert.substring(index, Math.min(index + 64, base64Cert.length())))
                            .append("\n");
                    index += 64;
                }

                // add cert footer
                sb.append("-----END CERTIFICATE-----\n");
            } catch (CertificateEncodingException e) {
                e.printStackTrace();
            }
        }
        return sb.toString().trim();
    }

    public static void ensureNotOnMainThread(@NonNull Context context) throws IllegalStateException{
        Looper looper = Looper.myLooper();
        if (looper != null && looper == context.getMainLooper()) {
            throw new IllegalStateException(
                    "calling this from your main thread can lead to deadlock");
        }
    }

    public static boolean preferAnonymousUsage() {
        return BuildConfig.priotize_anonymous_usage;
    }

    /**
     *
     * @param remoteTimezone
     * @return a value between 0.1 and 1.0
     */
    public static double getConnectionQualityFromTimezoneDistance(int remoteTimezone) {
        int localTimeZone = TimezoneHelper.getCurrentTimezone();
        int distance = TimezoneHelper.timezoneDistance(localTimeZone, remoteTimezone);
        return Math.max(distance / 12.0, 0.1);
    }

    public static String getProviderFormattedString(Resources resources, @StringRes int resourceId) {
        String appName = resources.getString(R.string.app_name);
        return resources.getString(resourceId, appName);
    }

    public static boolean stringEqual(@Nullable String string1, @Nullable String string2) {
        return (string1 == null && string2 == null) ||
                (string1 != null && string1.equals(string2));
    }

    @SuppressWarnings("unused")
    // FatWeb Flavor uses that for auto-update
    public static String getApkFileName() {
        try {
            return BuildConfig.update_apk_url.substring(BuildConfig.update_apk_url.lastIndexOf("/"));
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unused")
    // FatWeb Flavor uses that for auto-update
    public static String getVersionFileName() {
        try {
            return BuildConfig.version_file_url.substring(BuildConfig.version_file_url.lastIndexOf("/"));
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unused")
    // FatWeb Flavor uses that for auto-update
    public static String getSignatureFileName() {
        try {
            return BuildConfig.signature_url.substring(BuildConfig.signature_url.lastIndexOf("/"));
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isIPv4(String ipv4) {
        if (ipv4 == null) {
            return false;
        }
        Matcher matcher = Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9][0-9]|[0-9])\\.){3}(?:25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9][0-9]|[0-9])$").matcher(ipv4);
        return matcher.matches();
    }

    public static boolean isNetworkUrl(String url) {
        return url != null && URLUtil.isNetworkUrl(url) && Patterns.WEB_URL.matcher(url).matches();
    }

    public static boolean isDomainName(String url) {
        return url != null && Patterns.DOMAIN_NAME.matcher(url).matches();
    }

    /**
     * Extracts a domain from a given URL
     * @param mainUrl URL as String
     * @return Domain as String, null if mainUrl is an invalid URL
     */
    public static String getDomainFromMainURL(String mainUrl) {
        try {
            String topLevelDomain = PublicSuffixDatabase.Companion.get().getEffectiveTldPlusOne(mainUrl);
            return topLevelDomain.replaceFirst("http[s]?://", "").replaceFirst("/.*", "");
        } catch (NullPointerException | IllegalArgumentException e) {
            return null;
        }
    }
    
    public static boolean isCalyxOSWithTetheringSupport(Context context) {
        return SystemPropertiesHelper.contains("ro.calyxos.version", context) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
    }

    public static int getPendingIntentFlags() {
        int flags = PendingIntent.FLAG_CANCEL_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return flags;
    }

    public static int getTorTimeout() {
        if (NativeUtils.isUnitTest()) {
            return 1;
        }
        return 180;
    }
}
