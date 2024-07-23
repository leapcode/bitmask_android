package se.leap.bitmaskclient.base.utils;

import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.spongycastle.util.encoders.Base64;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.interfaces.EdECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import de.blinkt.openvpn.core.NativeUtils;

public class PrivateKeyHelper {

    public static final String RSA = "RSA";
    public static final String ED_25519 = "Ed25519";

    public static final String RSA_KEY_BEGIN = "-----BEGIN RSA PRIVATE KEY-----\n";
    public static final String RSA_KEY_END = "-----END RSA PRIVATE KEY-----";
    public static final String ED_25519_KEY_BEGIN = "-----BEGIN PRIVATE KEY-----\n";
    public static final String ED_25519_KEY_END = "-----END PRIVATE KEY-----";


    public interface PrivateKeyHelperInterface {


        @Nullable PrivateKey parsePrivateKeyFromString(String privateKeyString);
    }

    public static class DefaultPrivateKeyHelper implements PrivateKeyHelperInterface {

        public PrivateKey parsePrivateKeyFromString(String privateKeyString) {
            if (privateKeyString == null || privateKeyString.isBlank()) {
                return null;
            }
            if (privateKeyString.contains(RSA_KEY_BEGIN)) {
                return parseRsaKeyFromString(privateKeyString);
            } else if (privateKeyString.contains(ED_25519_KEY_BEGIN)) {
               return parseECPrivateKey(privateKeyString);
            } else {
                return null;
            }
        }

        private RSAPrivateKey parseRsaKeyFromString(String rsaKeyString) {
            RSAPrivateKey key;
            try {
                KeyFactory kf;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    kf = KeyFactory.getInstance(RSA, "BC");
                } else {
                    kf = KeyFactory.getInstance(RSA);
                }
                rsaKeyString = rsaKeyString.replaceFirst(RSA_KEY_BEGIN, "").replaceFirst(RSA_KEY_END, "");

                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.decode(rsaKeyString));
                key = (RSAPrivateKey) kf.generatePrivate(keySpec);
            } catch (InvalidKeySpecException | NoSuchAlgorithmException | NullPointerException |
                     NoSuchProviderException e) {
                e.printStackTrace();
                return null;
            }

            return key;
        }

        private EdECPrivateKey parseECPrivateKey(String ecKeyString) {
            KeyFactory kf;
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    kf = KeyFactory.getInstance(ED_25519, "BC");
                } else {
                    kf = KeyFactory.getInstance(ED_25519);
                }
                ecKeyString = ecKeyString.replaceFirst(ED_25519_KEY_BEGIN, "").replaceFirst(ED_25519_KEY_END, "");
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.decode(ecKeyString));
                return (EdECPrivateKey) kf.generatePrivate(keySpec);
            } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private static PrivateKeyHelperInterface instance = new DefaultPrivateKeyHelper();

    @VisibleForTesting
    public PrivateKeyHelper(PrivateKeyHelperInterface helperInterface) {
        if (!NativeUtils.isUnitTest()) {
            throw new IllegalStateException("PrivateKeyHelper injected with PrivateKeyHelperInterface outside of an unit test");
        }
        instance = helperInterface;
    }

    public static @Nullable PrivateKey parsePrivateKeyFromString(String rsaKeyString) {
       return instance.parsePrivateKeyFromString(rsaKeyString);
    }
}
