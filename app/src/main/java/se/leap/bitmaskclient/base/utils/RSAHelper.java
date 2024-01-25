package se.leap.bitmaskclient.base.utils;

import android.os.Build;

import androidx.annotation.VisibleForTesting;

import org.spongycastle.util.encoders.Base64;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import de.blinkt.openvpn.core.NativeUtils;

public class RSAHelper {

    public interface RSAHelperInterface {
        RSAPrivateKey parseRsaKeyFromString(String rsaKeyString);
    }

    public static class DefaultRSAHelper implements RSAHelperInterface {

        @Override
        public RSAPrivateKey parseRsaKeyFromString(String rsaKeyString) {
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
    }

    private static RSAHelperInterface instance = new DefaultRSAHelper();

    @VisibleForTesting
    public RSAHelper(RSAHelperInterface helperInterface) {
        if (!NativeUtils.isUnitTest()) {
            throw new IllegalStateException("RSAHelper injected with RSAHelperInterface outside of an unit test");
        }
        instance = helperInterface;
    }

    public static RSAPrivateKey parseRsaKeyFromString(String rsaKeyString) {
       return instance.parseRsaKeyFromString(rsaKeyString);
    }
}
