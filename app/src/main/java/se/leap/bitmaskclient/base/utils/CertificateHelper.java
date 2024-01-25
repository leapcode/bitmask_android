package se.leap.bitmaskclient.base.utils;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import de.blinkt.openvpn.core.NativeUtils;

public class CertificateHelper {

    public interface CertificateHelperInterface {
        String getFingerprintFromCertificate(X509Certificate certificate, String encoding) throws NoSuchAlgorithmException, CertificateEncodingException;

    }

    public static class DefaultCertificateHelper implements CertificateHelperInterface {

        public String byteArrayToHex(byte[] input) {
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
        @Override
        public String getFingerprintFromCertificate(X509Certificate certificate, String encoding) throws NoSuchAlgorithmException, CertificateEncodingException {
            byte[] byteArray = MessageDigest.getInstance(encoding).digest(certificate.getEncoded());
            return byteArrayToHex(byteArray);
        }
    }

    private static CertificateHelperInterface instance = new DefaultCertificateHelper();

    @VisibleForTesting
    public CertificateHelper(CertificateHelperInterface helperInterface) {
        if (!NativeUtils.isUnitTest()) {
            throw new IllegalStateException("CertificateHelper injected with CertificateHelperInterface outside of an unit test");
        }
        instance = helperInterface;
    }

    @NonNull
    public static String getFingerprintFromCertificate(X509Certificate certificate, String encoding) throws NoSuchAlgorithmException, CertificateEncodingException {
        return instance.getFingerprintFromCertificate(certificate, encoding);
    }

}
