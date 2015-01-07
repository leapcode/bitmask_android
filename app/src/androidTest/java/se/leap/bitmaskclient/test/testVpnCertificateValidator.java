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
import android.os.SystemClock;
import android.test.InstrumentationTestCase;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;

import se.leap.bitmaskclient.Provider;
import se.leap.bitmaskclient.eip.VpnCertificateValidator;

/**
 * @author parmegv
 */
public class testVpnCertificateValidator extends InstrumentationTestCase {

    String certificate_valid_from_jan2015_to_nov2022 = "";

    Context context;
    FromAssets assets;

    @Override
    protected void setUp() throws Exception {
        context = getInstrumentation().getContext();
        assets = new FromAssets(context);
        JSONObject secrets = new JSONObject(assets.toString(TestConstants.SECRETS_FILE));
        certificate_valid_from_jan2015_to_nov2022 = secrets.getString(Provider.CA_CERT);
        super.setUp();
    }

    public void testIsValid() {
        VpnCertificateValidator validator = new VpnCertificateValidator(certificate_valid_from_jan2015_to_nov2022);
        setTime(2015, 1, 6);
        assertTrue(validator.isValid());
        setTime(2020, 1, 6);
        assertFalse(validator.isValid());
    }

    private void setTime(int year, int month, int day) {
            shellCommand("adb shell chmod 666 /dev/alarm");
            Calendar c = Calendar.getInstance();
            c.set(year, month, day, 12, 00, 00);
            SystemClock.setCurrentTimeMillis(c.getTimeInMillis());
            shellCommand("adb shell chmod 664 /dev/alarm");
    }

    private int shellCommand(String command) {
        int result = -1;
        try {
            result = Runtime.getRuntime().exec(command).waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}
