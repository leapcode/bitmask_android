package se.leap.bitmaskclient.eip;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.util.Calendar;

import se.leap.bitmaskclient.base.utils.CertificateHelper;
import se.leap.bitmaskclient.testutils.TestCalendarProvider;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static se.leap.bitmaskclient.testutils.MockHelper.mockCertificateHelper;
import static se.leap.bitmaskclient.testutils.TestSetupHelper.getInputAsString;

public class VpnCertificateValidatorTest {

    @Before
    public void setup() {
    }

    @Test
    public void test_isValid() throws NoSuchAlgorithmException, CertificateEncodingException, IOException {
        String cert = getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.pem"));
        Calendar c = new Calendar.Builder().setDate(2024, 11, 2).setCalendarType("gregorian").build();
        CertificateHelper helper = mockCertificateHelper("falseFingerPrint");
        VpnCertificateValidator validator = new VpnCertificateValidator(cert);
        validator.setCalendarProvider(new TestCalendarProvider(c.getTimeInMillis()));
        assertTrue( validator.isValid());
    }

    @Test
    public void test_isValid_lessThan1day_returnFalse() throws NoSuchAlgorithmException, CertificateEncodingException, IOException {
        String cert = getInputAsString(getClass().getClassLoader().getResourceAsStream("riseup.net.pem"));
        Calendar c = new Calendar.Builder().setDate(2026, 11, 2).setCalendarType("gregorian").build();
        CertificateHelper helper = mockCertificateHelper("falseFingerPrint");
        VpnCertificateValidator validator = new VpnCertificateValidator(cert);
        validator.setCalendarProvider(new TestCalendarProvider(c.getTimeInMillis()));
        assertFalse( validator.isValid());
    }

    @Test
    public void test_isValid_multipleCerts_failIfOneExpires() throws NoSuchAlgorithmException, CertificateEncodingException, IOException {
        String cert = getInputAsString(getClass().getClassLoader().getResourceAsStream("float.hexacab.org.pem"));
        Calendar c = new Calendar.Builder().setDate(2024, 3, 28).setCalendarType("gregorian").build();
        CertificateHelper helper = mockCertificateHelper("falseFingerPrint");
        VpnCertificateValidator validator = new VpnCertificateValidator(cert);
        validator.setCalendarProvider(new TestCalendarProvider(c.getTimeInMillis()));
        assertFalse(validator.isValid());
    }

    @Test
    public void test_isValid_multipleCerts_allValid() throws NoSuchAlgorithmException, CertificateEncodingException, IOException {
        String cert = getInputAsString(getClass().getClassLoader().getResourceAsStream("float.hexacab.org.pem"));
        Calendar c = new Calendar.Builder().setDate(2024, 3, 27).setCalendarType("gregorian").build();
        CertificateHelper helper = mockCertificateHelper("falseFingerPrint");
        VpnCertificateValidator validator = new VpnCertificateValidator(cert);
        validator.setCalendarProvider(new TestCalendarProvider(c.getTimeInMillis()));
        assertTrue(validator.isValid());
    }

    @Test
    public void test_shouldBeUpdated_lessThan8days_returnTrue() throws NoSuchAlgorithmException, CertificateEncodingException, IOException {
        String cert = getInputAsString(getClass().getClassLoader().getResourceAsStream("float.hexacab.org.pem"));
        Calendar c = new Calendar.Builder().setDate(2024, 3, 21).setCalendarType("gregorian").build();
        CertificateHelper helper = mockCertificateHelper("falseFingerPrint");
        VpnCertificateValidator validator = new VpnCertificateValidator(cert);
        validator.setCalendarProvider(new TestCalendarProvider(c.getTimeInMillis()));
        assertTrue(validator.shouldBeUpdated());
    }

    @Test
    public void test_shouldBeUpdated_moreThan8days_returnFalse() throws NoSuchAlgorithmException, CertificateEncodingException, IOException {
        String cert = getInputAsString(getClass().getClassLoader().getResourceAsStream("float.hexacab.org.pem"));
        Calendar c = new Calendar.Builder().setDate(2024, 3, 20).setCalendarType("gregorian").build();
        CertificateHelper helper = mockCertificateHelper("falseFingerPrint");
        VpnCertificateValidator validator = new VpnCertificateValidator(cert);
        validator.setCalendarProvider(new TestCalendarProvider(c.getTimeInMillis()));
        assertFalse(validator.shouldBeUpdated());
    }
}