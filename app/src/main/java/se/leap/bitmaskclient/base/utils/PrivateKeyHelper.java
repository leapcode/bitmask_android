package se.leap.bitmaskclient.base.utils;

import static android.util.Base64.encodeToString;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.spongycastle.util.encoders.Base64;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import de.blinkt.openvpn.core.NativeUtils;

public class PrivateKeyHelper {

    public static final String TAG = PrivateKeyHelper.class.getSimpleName();

    public static final String RSA = "RSA";
    public static final String ED_25519 = "Ed25519";
    public static final String ECDSA = "ECDSA";

    public static final String RSA_KEY_BEGIN = "-----BEGIN RSA PRIVATE KEY-----\n";
    public static final String RSA_KEY_END = "-----END RSA PRIVATE KEY-----";
    public static final String EC_KEY_BEGIN = "-----BEGIN PRIVATE KEY-----\n";
    public static final String EC_KEY_END = "-----END PRIVATE KEY-----";


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
            } else if (privateKeyString.contains(EC_KEY_BEGIN)) {
               return parseECPrivateKey(privateKeyString);
            } else {
                return null;
            }
        }

        private RSAPrivateKey parseRsaKeyFromString(String rsaKeyString) {
            RSAPrivateKey key;
            try {
                KeyFactory kf;
                kf = KeyFactory.getInstance(RSA, "BC");
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

        private PrivateKey parseECPrivateKey(String ecKeyString) {
            String base64 = ecKeyString.replace(EC_KEY_BEGIN, "").replace(EC_KEY_END, "");
            byte[] keyBytes = Base64.decode(base64);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            String errMsg;
            try {
                KeyFactory keyFactory = KeyFactory.getInstance(ED_25519, "BC");
                return keyFactory.generatePrivate(keySpec);
            } catch (InvalidKeySpecException | NoSuchAlgorithmException | NoSuchProviderException e) {
                errMsg = e.toString();
            }

            try {
                KeyFactory keyFactory = KeyFactory.getInstance(ECDSA, "BC");
                return keyFactory.generatePrivate(keySpec);
            } catch (InvalidKeySpecException | NoSuchAlgorithmException | NoSuchProviderException e) {
                errMsg += "\n" + e.toString();
                Log.e(TAG, errMsg);
            }
            return null;
        }
    }

    public static String getPEMFormattedPrivateKey(PrivateKey key) throws NullPointerException {
        if (key == null) {
            throw new NullPointerException("Private key was null.");
        }
        String keyString = encodeToString(key.getEncoded(), android.util.Base64.DEFAULT);

        if (key instanceof RSAPrivateKey) {
            return (RSA_KEY_BEGIN + keyString + RSA_KEY_END);
        } else {
            return EC_KEY_BEGIN + keyString + EC_KEY_END;
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
