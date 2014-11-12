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

import android.util.Log;
import java.util.*;
import java.security.cert.*;
import java.text.*;

import se.leap.bitmaskclient.ConfigHelper;
import static se.leap.bitmaskclient.eip.Constants.*;

public class VpnCertificateValidator {
    public final static String TAG = VpnCertificateValidator.class.getSimpleName();

    public boolean isValid(String certificate) {
	if(!certificate.isEmpty()) {
	    X509Certificate certificate_x509 = ConfigHelper.parseX509CertificateFromString(certificate);
	    return isValid(certificate_x509);
	} else return false;
    }
    
    private boolean isValid(X509Certificate certificate) {
	Calendar offset_date = calculateOffsetCertificateValidity(certificate);
	try {
	    Log.d(TAG, "offset_date = " + offset_date.getTime().toString());
	    certificate.checkValidity(offset_date.getTime());
	    return true;
	} catch(CertificateExpiredException e) {
	    return false;
	} catch(CertificateNotYetValidException e) {
	    return false;
	}
    }

    private Calendar calculateOffsetCertificateValidity(X509Certificate certificate) {
	Log.d(TAG, "certificate not after = " + certificate.getNotAfter());
	long preventive_time = Math.abs(certificate.getNotBefore().getTime() - certificate.getNotAfter().getTime())/2;
	long current_date_millis = Calendar.getInstance().getTimeInMillis();
	    
	Calendar limit_date = Calendar.getInstance();
	limit_date.setTimeInMillis(current_date_millis + preventive_time);
	return limit_date;
    }
}
