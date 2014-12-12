package se.leap.bitmaskclient.test;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.test.ServiceTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import se.leap.bitmaskclient.Dashboard;
import se.leap.bitmaskclient.eip.Constants;
import se.leap.bitmaskclient.eip.EIP;

public class testEIP extends ServiceTestCase<EIP> {

    private Context context;
    private Intent intent;
    private SharedPreferences preferences;

    public testEIP(Class<EIP> activityClass) {
        super(activityClass);
        context = getSystemContext();
        intent = new Intent(context, EIP.class);
        preferences = context.getSharedPreferences(Dashboard.SHARED_PREFERENCES, Context.MODE_PRIVATE);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @MediumTest
    private void testCheckCertValidity() {
        testEmptyCertificate();
        testExpiredCertificate();
        // Wait for the service to start
        // Check result is OK.
    }

    private void testEmptyCertificate() {
        preferences.edit().putString(Constants.CERTIFICATE, "");
        startService(Constants.ACTION_CHECK_CERT_VALIDITY);
    }

    private void testExpiredCertificate() {
        String expired_certificate = "expired certificate";
        preferences.edit().putString(Constants.CERTIFICATE, expired_certificate);
        startService(Constants.ACTION_CHECK_CERT_VALIDITY);
    }

    private void startService(String action) {
        intent.setAction(action);
        startService(intent);
    }
}
