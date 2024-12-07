package se.leap.bitmaskclient.base.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.EdECPrivateKey;
import java.security.interfaces.RSAPrivateKey;

import se.leap.bitmaskclient.testutils.TestSetupHelper;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.UPSIDE_DOWN_CAKE, Build.VERSION_CODES.P, Build.VERSION_CODES.O, Build.VERSION_CODES.N})
public class PrivateKeyHelperTest {

    @Test
    public void parsePrivateKeyFromString_testRSA() throws IOException {
        String rsa_key = TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("private_rsa_key.pem"));
        PrivateKey pk = PrivateKeyHelper.parsePrivateKeyFromString(rsa_key);
        assertNotNull(pk);
        assertTrue(pk instanceof RSAPrivateKey);
    }

    @Test
    public void parsePrivateKeyFromString_testEd25519() throws IOException {
        String ed25519_key = TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("private_PKCS8_encoded_ed25519_key.pem"));
        PrivateKey pk = PrivateKeyHelper.parsePrivateKeyFromString(ed25519_key);
        assertNotNull(pk);
        assertTrue(pk instanceof EdECPrivateKey);
    }

    @Test
    public void parsePrivateKeyFromString_testEcDSA() throws IOException {
        String ed_dsa_key = TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("private_PKCS8_encoded_ecdsa_key.pem"));
        PrivateKey pk = PrivateKeyHelper.parsePrivateKeyFromString(ed_dsa_key);
        assertNotNull(pk);
        assertTrue(pk instanceof ECPrivateKey);
    }
}