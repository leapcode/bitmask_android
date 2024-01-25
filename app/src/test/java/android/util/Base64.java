package android.util;

import java.util.Arrays;

public class Base64 {

    /**
     * Base64-encode the given data and return a newly allocated
     * String with the result.
     *
     * @param input  the data to encode
     * @param flags  controls certain features of the encoded output.
     *               Passing {@code DEFAULT} results in output that
     *               adheres to RFC 2045.
     */
    public static String encodeToString(byte[] input, int flags) {
        return Arrays.toString(java.util.Base64.getEncoder().encode(input));
    }
}
