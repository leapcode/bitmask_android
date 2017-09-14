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

import android.content.*;
import android.os.*;
import android.test.*;

import org.json.*;

import java.io.*;
import java.util.*;

import se.leap.bitmaskclient.*;
import se.leap.bitmaskclient.eip.*;

/**
 * @author parmegv
 * //FIXME: The class VpnCertificateValidator should be tested with unit tests!
 */
public class TestVpnCertificateValidator extends InstrumentationTestCase {

    String certificate_valid_from_nov2012_to_nov2022 = "";

    Context context;
    FromAssets assets;

    @Override
    protected void setUp() throws Exception {
        context = getInstrumentation().getContext();
        assets = new FromAssets(context);
        JSONObject secrets = new JSONObject(assets.toString(TestConstants.SECRETS_FILE));
        certificate_valid_from_nov2012_to_nov2022 = secrets.getString(Provider.CA_CERT);
        super.setUp();
    }


    //TODO: This test proves that the validation method is weird. Valid dates range between Nov. 2oo6 and Nov. 2017 instead of Nov. 2012 and Nov.2022
    public void testIsValid() {
        VpnCertificateValidator validator = new VpnCertificateValidator(certificate_valid_from_nov2012_to_nov2022);
        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.YEAR, 2006);
        calendar.set(Calendar.MONTH, Calendar.NOVEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, 6);        calendar.set(Calendar.YEAR, 2006);
        validator.setCalendarProvider(new TestCalendarProvider(calendar.getTimeInMillis()));
        assertFalse(validator.isValid());

        calendar.set(Calendar.YEAR, 2010);
        calendar.set(Calendar.MONTH, Calendar.NOVEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, 6);
        validator.setCalendarProvider(new TestCalendarProvider(calendar.getTimeInMillis()));
        assertTrue(validator.isValid());

        calendar.set(Calendar.YEAR, 2011);
        calendar.set(Calendar.MONTH, Calendar.NOVEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, 6);
        validator.setCalendarProvider(new TestCalendarProvider(calendar.getTimeInMillis()));
        assertTrue(validator.isValid());

        calendar.set(Calendar.YEAR, 2017);
        calendar.set(Calendar.MONTH, Calendar.NOVEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, 5);
        validator.setCalendarProvider(new TestCalendarProvider(calendar.getTimeInMillis()));
        assertTrue(validator.isValid());

        calendar.set(Calendar.YEAR, 2017);
        calendar.set(Calendar.MONTH, Calendar.NOVEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, 6);
        validator.setCalendarProvider(new TestCalendarProvider(calendar.getTimeInMillis()));
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
        int result = 0;
        try {
            Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}
