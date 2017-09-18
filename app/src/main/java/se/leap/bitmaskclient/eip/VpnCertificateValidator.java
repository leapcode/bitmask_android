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
    private CalendarProviderInterface calendarProvider;

    public VpnCertificateValidator(String certificate) {
        this.certificate = certificate;
        this.calendarProvider = new CalendarProvider();
    }

    public void setCalendarProvider(CalendarProviderInterface calendarProvider) {
        this.calendarProvider = calendarProvider;
    }

    /**
     *
     * @return true if there's a certificate that is valid for more than 15 more days
     */
    public boolean isValid() {
        if (certificate.isEmpty()) {
            return false;
        }

        X509Certificate certificate_x509 = ConfigHelper.parseX509CertificateFromString(certificate);
        return isValid(certificate_x509);
    }


    private boolean isValid(X509Certificate certificate) {
        Calendar offsetDate = calculateOffsetCertificateValidity(certificate);
        try {
            certificate.checkValidity(offsetDate.getTime());
            return true;
        } catch (CertificateExpiredException e) {
            return false;
        } catch (CertificateNotYetValidException e) {
            return false;
        }
    }

    private Calendar calculateOffsetCertificateValidity(X509Certificate certificate) {
        Calendar limitDate = calendarProvider.getCalendar();
        Date startDate = certificate.getNotBefore();
        // if certificates start date is before current date just return the current date without an offset
        if (startDate.getTime() >= limitDate.getTime().getTime()) {
            return limitDate;
        }
        // else add an offset of 15 days to the current date
        limitDate.add(Calendar.DAY_OF_YEAR, 15);

        return limitDate;
    }
}
