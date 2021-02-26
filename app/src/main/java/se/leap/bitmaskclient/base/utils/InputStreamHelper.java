package se.leap.bitmaskclient.base.utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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

    public static String extractKeyFromInputStream(InputStream inputStream, String key) {
        String value = "";

        JSONObject fileContents = inputStreamToJson(inputStream);
        if (fileContents != null)
            value = fileContents.optString(key);
        return value;
    }

    public static JSONObject inputStreamToJson(InputStream inputStream) {
        JSONObject json = null;
        try {
            byte[] bytes = new byte[inputStream.available()];
            if (inputStream.read(bytes) > 0)
                json = new JSONObject(new String(bytes));
            inputStream.reset();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
}
