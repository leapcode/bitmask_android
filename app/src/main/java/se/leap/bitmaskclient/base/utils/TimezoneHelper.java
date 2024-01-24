package se.leap.bitmaskclient.base.utils;

import androidx.annotation.VisibleForTesting;

import java.util.Calendar;

public class TimezoneHelper {

    public interface TimezoneInterface {
        int getCurrentTimezone();
    }

    private static TimezoneInterface instance = new DefaultTimezoneHelper();

    @VisibleForTesting
    public TimezoneHelper(TimezoneInterface timezoneInterface) {
        instance = timezoneInterface;
    }

    public static TimezoneInterface get() {
        return instance;
    }

    public static int timezoneDistance(int localTimezone, int remoteTimezone) { // Distance along the numberline of Prime Meridian centric, assumes UTC-11 through UTC+12
        int dist = Math.abs(localTimezone - remoteTimezone);
        // Farther than 12 timezones and it's shorter around the "back"
        if (dist > 12)
            dist = 12 - (dist - 12); // Well i'll be. Absolute values make equations do funny things.
        return dist;
    }

    public static int getCurrentTimezone() {
        return get().getCurrentTimezone();
    }

    public static class DefaultTimezoneHelper implements TimezoneInterface {
        @Override
        public int getCurrentTimezone() {
            return Calendar.getInstance().get(Calendar.ZONE_OFFSET) / 3600000;
        }
    }
}
