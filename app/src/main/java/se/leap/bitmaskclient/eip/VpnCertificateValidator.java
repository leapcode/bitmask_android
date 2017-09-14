/**
 * Copyright (c) 2013 LEAP Encryption Access Project and contributers
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
package se.leap.bitmaskclient.eip;

import java.security.cert.*;
import java.util.*;

import se.leap.bitmaskclient.*;

public class VpnCertificateValidator {
    public final static String TAG = VpnCertificateValidator.class.getSimpleName();

    private String certificate;
    protected CalendarProviderInterface calendarProvider;

    public VpnCertificateValidator(String certificate) {
        this.certificate = certificate;
        calendarProvider = new CalendarProvider();
    }

    public void setCalendarProvider(CalendarProviderInterface calendarProvider) {
        this.calendarProvider = calendarProvider;
    }

    public boolean isValid() {
        if (!certificate.isEmpty()) {
            X509Certificate certificate_x509 = ConfigHelper.parseX509CertificateFromString(certificate);
            return isValid(certificate_x509);
        } else return true;
    }


    /* FIXME: the validation seems to be syntactically wrong.
     * if the valid time span of a certificate is between 01.01.14 and 01.01.16 this method would return true for current dates between 01.01.13 and 01.01.15!!!
     */
    private boolean isValid(X509Certificate certificate) {
        Calendar offset_date = calculateOffsetCertificateValidity(certificate);
        try {
            certificate.checkValidity(offset_date.getTime());
            return true;
        } catch (CertificateExpiredException e) {
            return false;
        } catch (CertificateNotYetValidException e) {
            return false;
        }
    }

    private Calendar calculateOffsetCertificateValidity(X509Certificate certificate) {
        long preventive_time = Math.abs(certificate.getNotBefore().getTime() - certificate.getNotAfter().getTime()) / 2;
        long current_date_millis = calendarProvider.getCalendar().getTimeInMillis();

        Calendar limit_date = calendarProvider.getCalendar();
        limit_date.setTimeInMillis(current_date_millis + preventive_time);
        return limit_date;
    }
}
