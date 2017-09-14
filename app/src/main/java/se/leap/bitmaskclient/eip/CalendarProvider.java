package se.leap.bitmaskclient.eip;

import java.util.Calendar;

/**
 * Created by cyberta on 13.09.17.
 */

class CalendarProvider implements CalendarProviderInterface {

    public Calendar getCalendar() {
        return Calendar.getInstance();
    }
}
