package se.leap.bitmaskclient.base.utils;

import static org.junit.Assert.assertEquals;

import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.testutils.TestSetupHelper;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.LOLLIPOP, Build.VERSION_CODES.P})
public class CredentialsParserTest {

    @Test
    public void testCertificateResponse() throws IOException, XmlPullParserException {
        String ed25519_creds = TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ed25519_credentials.pem"));
        Provider provider = new Provider("https://demo.bitmask.net");
        CredentialsParser.parseXml(ed25519_creds, provider);
        assertEquals(
                "-----BEGIN PRIVATE KEY-----\n" +
                "MC4CAQAwBQYDK2VwBCIEIF+HZvpSdhnTbYeT635bT2+IU4FbW3EWlHuUnXvhb10m\n" +
                "-----END PRIVATE KEY-----", provider.getPrivateKeyString());
        assertEquals(
                "-----BEGIN CERTIFICATE-----\n" +
                "MIIBgzCCASigAwIBAgIRALD3Z4SsobpcU7tcC0r9JOQwCgYIKoZIzj0EAwIwNzE1\n" +
                "MDMGA1UEAwwsUHJvdmlkZXIgUm9vdCBDQSAoY2xpZW50IGNlcnRpZmljYXRlcyBv\n" +
                "bmx5ISkwHhcNMjQxMTA1MTU0MjU0WhcNMjQxMTI5MTU0MjU0WjAUMRIwEAYDVQQD\n" +
                "EwlVTkxJTUlURUQwKjAFBgMrZXADIQC5QkZAcpkQ3Rm54gN5iLEU1Zp1w+patXVT\n" +
                "W9GRXmFz+6NnMGUwDgYDVR0PAQH/BAQDAgeAMBMGA1UdJQQMMAoGCCsGAQUFBwMC\n" +
                "MB0GA1UdDgQWBBRMxeMW4vqGK7FBkDt2+8upfkK1kzAfBgNVHSMEGDAWgBS0pVQs\n" +
                "1wnvNYG0AnmkxUcLOw+BLDAKBggqhkjOPQQDAgNJADBGAiEAg112+zWMm9qrPTvK\n" +
                "99IMa+wbeNzZLSoN9xewf5rxOX0CIQCvMi08JcajsAJ9Dg6YAQgpmFdb35HDCzve\n" +
                "lhkTCWJpgQ==\n" +
                "-----END CERTIFICATE-----", provider.getVpnCertificate());
        assertEquals(
                "-----BEGIN CERTIFICATE-----\n" +
                "MIIBozCCAUigAwIBAgIBATAKBggqhkjOPQQDAjA3MTUwMwYDVQQDDCxQcm92aWRl\n" +
                "ciBSb290IENBIChjbGllbnQgY2VydGlmaWNhdGVzIG9ubHkhKTAeFw0yNDEwMjMx\n" +
                "MjA0MjRaFw0yOTEwMjMxMjA5MjRaMDcxNTAzBgNVBAMMLFByb3ZpZGVyIFJvb3Qg\n" +
                "Q0EgKGNsaWVudCBjZXJ0aWZpY2F0ZXMgb25seSEpMFkwEwYHKoZIzj0CAQYIKoZI\n" +
                "zj0DAQcDQgAEMImwbNTDrXMeWfyTb2TMNzXNr79OsKjLDdZWqVT0iHMI8apo2P4H\n" +
                "eXCHVGjS2Z+jpyI1u9ic3igThsKEmdZMSKNFMEMwDgYDVR0PAQH/BAQDAgKkMBIG\n" +
                "A1UdEwEB/wQIMAYBAf8CAQEwHQYDVR0OBBYEFLSlVCzXCe81gbQCeaTFRws7D4Es\n" +
                "MAoGCCqGSM49BAMCA0kAMEYCIQCw88nXg/vs/KgGqH1uPs9oZkOxucVn/ZEznYzg\n" +
                "szLhtAIhAPY32oHwmj3yHO9H2Jp7x0CoHuu1fKd9fQTBvEEbi7o9\n" +
                "-----END CERTIFICATE-----", provider.getCaCert());
    }

}
