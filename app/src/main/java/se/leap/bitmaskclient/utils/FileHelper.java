package se.leap.bitmaskclient.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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

}
