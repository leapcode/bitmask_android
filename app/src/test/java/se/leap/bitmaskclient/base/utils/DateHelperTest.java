package se.leap.bitmaskclient.base.utils;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateHelperTest {

    @Test
    public void getFormattedDateWithTimezone() {
        Pattern datePattern = Pattern.compile("^10 Nov 22 [1]?[0-9]:26 [+-][0-9]{4}$");
        String formattedDate = DateHelper.getFormattedDateWithTimezone(1668075969744L);
        Matcher matcher = datePattern.matcher(formattedDate);
        System.out.println(formattedDate);
        assertTrue("date should be formatted similar to 10 Nov 22 11:26 +0100", matcher.find());
    }
}