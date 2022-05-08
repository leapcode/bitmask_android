/**
 * Copyright (c) 2020 LEAP Encryption Access Project and contributers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package se.leap.bitmaskclient.base.utils;

import androidx.annotation.WorkerThread;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class Cmd {

    private static final String TAG = Cmd.class.getSimpleName();

    @WorkerThread
    public static int runBlockingCmd(String[] cmds, StringBuilder log) throws Exception {
        return runCmd(cmds, log, true);
    }

    @WorkerThread
    private static int runCmd(String[] cmds, StringBuilder log,
                       boolean waitFor) throws Exception {

        int exitCode = -1;
        Process proc = Runtime.getRuntime().exec("sh");
        OutputStreamWriter out = new OutputStreamWriter(proc.getOutputStream());

        try {
            for (String cmd : cmds) {
                out.write(cmd);
                out.write("\n");
            }

            out.flush();
            out.write("exit\n");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            out.close();
        }

        if (waitFor) {
            // Consume the "stdout"
            InputStreamReader reader = new InputStreamReader(proc.getInputStream());
            readToLogString(reader, log);

            // Consume the "stderr"
            reader = new InputStreamReader(proc.getErrorStream());
            readToLogString(reader, log);

            try {
                exitCode = proc.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return exitCode;
    }

    private static void readToLogString(InputStreamReader reader, StringBuilder log) throws IOException {
        final char buf[] = new char[10];
        int read = 0;
        try {
            while ((read = reader.read(buf)) != -1) {
                if (log != null)
                    log.append(buf, 0, read);
            }
        } catch (IOException e) {
            reader.close();
            throw new IOException(e);
        }
        reader.close();
    }
}
