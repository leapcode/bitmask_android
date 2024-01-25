package se.leap.bitmaskclient.base.utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import de.blinkt.openvpn.core.NativeUtils;

/**
 * Created by cyberta on 18.03.18.
 */

public class InputStreamHelper {
    public interface InputStreamHelperInterface {
        InputStream getInputStreamFrom(String filePath) throws FileNotFoundException;

    }

    private static InputStreamHelperInterface instance = new DefaultInputStreamHelper();

    private static class DefaultInputStreamHelper implements InputStreamHelperInterface {
        @Override
        public InputStream getInputStreamFrom(String filePath) throws FileNotFoundException {
            return new FileInputStream(filePath);
        }
    }

    public InputStreamHelper(InputStreamHelperInterface helperInterface) {
        if (!NativeUtils.isUnitTest()) {
            throw new IllegalStateException("InputStreamHelper injected with InputStreamHelperInterface outside of an unit test");
        }
        instance = helperInterface;
    }
    //allows us to mock FileInputStream
    public static InputStream getInputStreamFrom(String filePath) throws FileNotFoundException {
        return instance.getInputStreamFrom(filePath);
    }

    public static String loadInputStreamAsString(InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static JSONObject inputStreamToJson(InputStream inputStream) {
        JSONObject json = new JSONObject();
        try {
            byte[] bytes = new byte[inputStream.available()];
            if (inputStream.read(bytes) > 0)
                json = new JSONObject(new String(bytes));
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
}
