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
import android.test.InstrumentationTestCase;

import org.json.JSONObject;

import java.util.Calendar;

import se.leap.bitmaskclient.Provider;
import se.leap.bitmaskclient.eip.VpnCertificateValidator;

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


    public void testIsValid() {
        VpnCertificateValidator validator = new VpnCertificateValidator(certificate_valid_from_nov2012_to_nov2022);
        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.YEAR, 2006);
        calendar.set(Calendar.MONTH, Calendar.NOVEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, 6);        calendar.set(Calendar.YEAR, 2006);
        validator.setCalendarProvider(new TestCalendarProvider(calendar.getTimeInMillis()));
        assertFalse(validator.isValid());

        calendar.set(Calendar.YEAR, 2011);
        calendar.set(Calendar.MONTH, Calendar.NOVEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, 6);
        validator.setCalendarProvider(new TestCalendarProvider(calendar.getTimeInMillis()));
        assertFalse(validator.isValid());

        calendar.set(Calendar.YEAR, 2012);
        calendar.set(Calendar.MONTH, Calendar.NOVEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, 7);
        validator.setCalendarProvider(new TestCalendarProvider(calendar.getTimeInMillis()));
        assertTrue(validator.isValid());

        calendar.set(Calendar.YEAR, 2022);
        calendar.set(Calendar.MONTH, Calendar.OCTOBER);
        calendar.set(Calendar.DAY_OF_MONTH, 21);
        validator.setCalendarProvider(new TestCalendarProvider(calendar.getTimeInMillis()));
        assertTrue(validator.isValid());

        calendar.set(Calendar.YEAR, 2022);
        calendar.set(Calendar.MONTH, Calendar.OCTOBER);
        calendar.set(Calendar.DAY_OF_MONTH, 23);
        validator.setCalendarProvider(new TestCalendarProvider(calendar.getTimeInMillis()));
        assertFalse(validator.isValid());

    }

}