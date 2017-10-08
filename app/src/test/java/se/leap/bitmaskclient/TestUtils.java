package se.leap.bitmaskclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by cyberta on 08.10.17.
 */

public class TestUtils {

    public static String getInputAsString(InputStream fileAsInputStream) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(fileAsInputStream));
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();
        while (line != null) {
            sb.append(line);
            line = br.readLine();
        }

        return sb.toString();
    }

}
