package se.leap.bitmaskclient.test;

import com.robotium.solo.*;

import java.text.*;
import java.util.*;

public class Screenshot {
    private static String default_name = Screenshot.class.getPackage().getName();
    private static DateFormat date_format = DateFormat.getDateTimeInstance();
    private static int DEFAULT_MILLISECONDS_TO_SLEEP = 500;
    private static int milliseconds_to_sleep = 0;
    private static Solo solo;

    public static void initialize(Solo solo) {
        Screenshot.solo = solo;
    }

    public static void take(String name) {
        solo.takeScreenshot(name.replace(" ", "_") + " " + getTimeStamp());
    }

    public static void takeWithSleep(String name) {
        sleepBefore();
        take(name);
    }

    public static void take() {
        take(default_name + "_" + getTimeStamp());
    }

    public static void takeWithSleep() {
        sleepBefore();
        take();
    }

    private static String getTimeStamp() {
        return date_format.format(Calendar.getInstance().getTime()).replace(" ", "_").replace("/", "_").replace(":", "_");
    }

    public static void setTimeToSleep(double seconds) {
        long milliseconds_to_sleep = Math.round(seconds * 1000);
        Screenshot.milliseconds_to_sleep = Math.round(milliseconds_to_sleep);
    }

    private static void sleepBefore() {
        if(milliseconds_to_sleep == 0)
            solo.sleep(DEFAULT_MILLISECONDS_TO_SLEEP);
        else
            solo.sleep(milliseconds_to_sleep);
        milliseconds_to_sleep = 0;
    }
}

