package se.leap.bitmaskclient.base.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Created by cyberta on 18.03.18.
 */

public class InputStreamHelper {
    //allows us to mock FileInputStream
    public static InputStream getInputStreamFrom(String filePath) throws FileNotFoundException {
        return new FileInputStream(filePath);
    }

    public static String loadInputStreamAsString(InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
