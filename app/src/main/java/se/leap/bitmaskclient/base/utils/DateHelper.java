package se.leap.bitmaskclient.base.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Contains helper methods related to date manipulation.
 *
 * @author Janak
 */
public class DateHelper {
    private static final String DATE_PATTERN = "dd/MM/yyyy";
    private static final String DATE_PATTERN_RFC822_NUMERIC_ZONE = "dd MMM yy hh:mm Z"; // RFC822 with numeric zone
    private static final int ONE_DAY = 86400000; //1000*60*60*24

    public static long getDateDiffToCurrentDateInDays(String startDate) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN, Locale.US);
        Date lastDate = sdf.parse(startDate);
        Date currentDate = new Date();
        return (currentDate.getTime() - lastDate.getTime()) / ONE_DAY;
    }

    public static String getCurrentDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN, Locale.US);
        Date lastDate = new Date();
        return sdf.format(lastDate);
    }

    /**
     * Return date in a specific format bitmaskcore can handle.
     * @param milliSeconds Date in milliseconds
     * @return String representing date in specified format
     */
    public static String getFormattedDateWithTimezone(long milliSeconds) throws IllegalArgumentException {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN_RFC822_NUMERIC_ZONE, Locale.US);
        return sdf.format(milliSeconds);
    }
}
