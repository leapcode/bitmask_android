/**
 * Copyright (c) 2013, 2014, 2015 LEAP Encryption Access Project and contributers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package se.leap.bitmaskclient.test;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.test.ServiceTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import se.leap.bitmaskclient.eip.EIP;

import static se.leap.bitmaskclient.Constants.EIP_ACTION_CHECK_CERT_VALIDITY;
import static se.leap.bitmaskclient.Constants.PROVIDER_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.Constants.SHARED_PREFERENCES;

/**
 * @author parmegv
 */
public class TestEIP extends ServiceTestCase<EIP> {

    private Context context;
    private Intent intent;
    private SharedPreferences preferences;

    public TestEIP(Class<EIP> activityClass) {
        super(activityClass);
        context = getSystemContext();
        intent = new Intent(context, EIP.class);
        preferences = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
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
        preferences.edit().putString(PROVIDER_VPN_CERTIFICATE, "").apply();
        startService(EIP_ACTION_CHECK_CERT_VALIDITY);
    }

    private void testExpiredCertificate() {
        String expired_certificate = "expired certificate";
        preferences.edit().putString(PROVIDER_VPN_CERTIFICATE, expired_certificate).apply();
        startService(EIP_ACTION_CHECK_CERT_VALIDITY);
    }

    private void startService(String action) {
        intent.setAction(action);
        startService(intent);
    }
}
