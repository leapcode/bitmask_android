package se.leap.bitmaskclient.base.utils;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import de.blinkt.openvpn.core.NativeUtils;

/**
 * Created by cyberta on 18.03.18.
 */

public class FileHelper {

    public interface FileHelperInterface {
        File createFile(File dir, String fileName);
        void persistFile(File file, String content) throws IOException;
    }

    public static class DefaultFileHelper implements FileHelperInterface {
        @Override
        public File createFile(File dir, String fileName) {
            return new File(dir, fileName);
        }

        @Override
        public void persistFile(File file, String content) throws IOException {
            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.close();
        }
    }

    private static FileHelperInterface instance = new DefaultFileHelper();

    @VisibleForTesting
    public FileHelper(FileHelperInterface helperInterface) {
        if (!NativeUtils.isUnitTest()) {
            throw new IllegalStateException("FileHelper injected with FileHelperInterface outside of an unit test");
        }

        instance = helperInterface;
    }

    public static File createFile(File dir, String fileName) {
        return instance.createFile(dir, fileName);
    }

    public static void persistFile(File file, String content) throws IOException {
        instance.persistFile(file, content);
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
            } catch (NullPointerException | IOException errabi) {
                return null;
            }
        }
    }

}
