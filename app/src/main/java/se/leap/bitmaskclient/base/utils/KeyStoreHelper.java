package se.leap.bitmaskclient.base.utils;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Created by cyberta on 18.03.18.
 */

public class KeyStoreHelper {
    private static KeyStore trustedKeystore;

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
            trustedKeystore.setCertificateEntry(provider, cert);
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
            if (trustedKeystore == null) {
                trustedKeystore = KeyStore.getInstance("BKS");
                trustedKeystore.load(null);
            }
            trustedKeystore.setCertificateEntry(provider, cert);
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
        return trustedKeystore;
    }

}
