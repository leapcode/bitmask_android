package se.leap.bitmaskclient.base.utils;

import androidx.annotation.Nullable;

import java.util.Locale;

public class StringUtils {

    /**
     * Capitalizes the first character of the given string using the specified locale.
     *
     * <p>This method handles null and empty strings by returning them unchanged.
     * If the locale is null, the string is also returned unchanged.</p>
     *
     * @param string  the string to be capitalized, may be null or empty
     * @param locale  the locale to be used for capitalization, may be null
     * @return        the capitalized string, or the original string
     * @see String#toUpperCase(Locale)
     */
    public static @Nullable String capitalize(@Nullable String string, @Nullable Locale locale) {
        if (string == null || string.isEmpty() || locale == null) {
            return string;
        }

        return string.substring(0, 1).toUpperCase(locale) + string.substring(1);
    }
}
