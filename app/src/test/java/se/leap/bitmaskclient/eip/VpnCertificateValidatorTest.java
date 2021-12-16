package se.leap.bitmaskclient.eip;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.util.Calendar;

import se.leap.bitmaskclient.base.utils.ConfigHelper;
import se.leap.bitmaskclient.testutils.TestCalendarProvider;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static se.leap.bitmaskclient.testutils.MockHelper.mockConfigHelper;
import static se.leap.bitmaskclient.testutils.TestSetupHelper.getInputAsString;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ConfigHelper.class})
public class VpnCertificateValidatorTest {

    @Before
    public void setup() {
    }

    @Test
    public void test_isValid() throws NoSuchAlgorithmException, CertificateEncodingException, IOException {
        String cert = getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.pem"));
        Calendar c = new Calendar.Builder().setDate(2018, 1, 1).setCalendarType("gregorian").build();
        mockConfigHelper("falseFingerPrint");
        VpnCertificateValidator validator = new VpnCertificateValidator(cert);
        validator.setCalendarProvider(new TestCalendarProvider(c.getTimeInMillis()));
        assertTrue( validator.isValid());
    }

    @Test
    public void test_isValid_lessThan15days_returnFalse() throws NoSuchAlgorithmException, CertificateEncodingException, IOException {
        String cert = getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.pem"));
        Calendar c = new Calendar.Builder().setDate(2024, 4, 14).setCalendarType("gregorian").build();
        mockConfigHelper("falseFingerPrint");
        VpnCertificateValidator validator = new VpnCertificateValidator(cert);
        validator.setCalendarProvider(new TestCalendarProvider(c.getTimeInMillis()));
        assertFalse( validator.isValid());
    }

    @Test
    public void test_isValid_multipleCerts_failIfOneExpires() throws NoSuchAlgorithmException, CertificateEncodingException, IOException {
        String cert = getInputAsString(getClass().getClassLoader().getResourceAsStream("float.hexacab.org.pem"));
        Calendar c = new Calendar.Builder().setDate(2024, 4, 14).setCalendarType("gregorian").build();
        mockConfigHelper("falseFingerPrint");
        VpnCertificateValidator validator = new VpnCertificateValidator(cert);
        validator.setCalendarProvider(new TestCalendarProvider(c.getTimeInMillis()));
        assertFalse(validator.isValid());
    }

    @Test
    public void test_isValid_multipleCerts_allValid() throws NoSuchAlgorithmException, CertificateEncodingException, IOException {
        String cert = getInputAsString(getClass().getClassLoader().getResourceAsStream("float.hexacab.org.pem"));
        Calendar c = new Calendar.Builder().setDate(2024, 4, 13).setCalendarType("gregorian").build();
        mockConfigHelper("falseFingerPrint");
        VpnCertificateValidator validator = new VpnCertificateValidator(cert);
        validator.setCalendarProvider(new TestCalendarProvider(c.getTimeInMillis()));
        assertFalse(validator.isValid());
    }
}