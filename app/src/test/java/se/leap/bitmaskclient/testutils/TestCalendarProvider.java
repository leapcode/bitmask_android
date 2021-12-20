package se.leap.bitmaskclient.testutils;

import java.util.Calendar;

import se.leap.bitmaskclient.eip.CalendarProviderInterface;

/**
 * Created by cyberta on 13.09.17.
 */

public class TestCalendarProvider implements CalendarProviderInterface {

    private long currentTimeInMillis = 0;

    public TestCalendarProvider(long currentTimeInMillis) {
        this.currentTimeInMillis = currentTimeInMillis;
    }

    @Override
    public Calendar getCalendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentTimeInMillis);
        return calendar;
    }

}
