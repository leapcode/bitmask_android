package se.leap.bitmaskclient.base.utils;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by cyberta on 18.03.18.
 */

public class FileHelper {
    public static File createFile(File dir, String fileName) {
        return new File(dir, fileName);
    }

    public static void persistFile(File file, String content) throws IOException {
        FileWriter writer = new FileWriter(file);
        writer.write(content);
        writer.close();
    }

    public static String readPublicKey(Context context) {
        {
            InputStream inputStream;
            try {
                inputStream = context.getAssets().open("public.pgp");
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();
                return sb.toString();
            } catch (IOException errabi) {
                return null;
            }
        }
    }

}
