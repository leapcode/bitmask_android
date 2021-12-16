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

import androidx.annotation.VisibleForTesting;

import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import se.leap.bitmaskclient.base.utils.ConfigHelper;

public class VpnCertificateValidator {
    public final static String TAG = VpnCertificateValidator.class.getSimpleName();

    private final String certificate;
    private CalendarProviderInterface calendarProvider;

    public VpnCertificateValidator(String certificate) {
        this.certificate = certificate;
        this.calendarProvider = new CalendarProvider();
    }

    @VisibleForTesting
    public void setCalendarProvider(CalendarProviderInterface calendarProvider) {
        this.calendarProvider = calendarProvider;
    }

    /**
     *
     * @return true if all certificates are valid for more than 15 more days
     */
    public boolean isValid() {
        if (certificate.isEmpty()) {
            return false;
        }

        ArrayList<X509Certificate> x509Certificates = ConfigHelper.parseX509CertificatesFromString(certificate);
        if (x509Certificates == null) {
            return false;
        }
        for (X509Certificate cert : x509Certificates) {
            if (!isValid(cert)) {
                return false;
            }
        }
        return true;
    }


    private boolean isValid(X509Certificate certificate) {
        if (certificate == null) {
            return false;
        }

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
