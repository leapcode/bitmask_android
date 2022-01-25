package se.leap.bitmaskclient.tor;
/**
 * Copyright (c) 2021 LEAP Encryption Access Project and contributors
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
import android.content.Context;
import android.os.FileObserver;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.torproject.jni.ClientTransportPluginInterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import IPtProxy.IPtProxy;

public class ClientTransportPlugin implements ClientTransportPluginInterface {
    public static String TAG = ClientTransportPlugin.class.getSimpleName();

    private HashMap<String, String> mFronts;
    private final WeakReference<Context> contextRef;
    private long snowflakePort = -1;
    private FileObserver logFileObserver;
    private static final Pattern SNOWFLAKE_LOG_TIMESTAMP_PATTERN = Pattern.compile("((19|2[0-9])[0-9]{2}\\/\\d{1,2}\\/\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}) ([\\S|\\s]+)");

    public ClientTransportPlugin(Context context) {
        this.contextRef = new WeakReference<>(context);
        loadCdnFronts(context);
    }

    @Override
    public void start() {
        Context context = contextRef.get();
        if (context == null) {
            return;
        }
        File logfile = new File(context.getApplicationContext().getCacheDir(), "snowflake.log");
        Log.d(TAG, "logfile at " + logfile.getAbsolutePath());
        try {
            if (logfile.exists()) {
                logfile.delete();
            }
            logfile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //this is using the current, default Tor snowflake infrastructure
        String target = getCdnFront("snowflake-target");
        String front = getCdnFront("snowflake-front");
        String stunServer = getCdnFront("snowflake-stun");
        Log.d(TAG, "startSnowflake. target: " + target + ", front:" + front + ", stunServer" + stunServer);
        snowflakePort = IPtProxy.startSnowflake(stunServer, target, front, null, logfile.getAbsolutePath(), false, false, true, 5);
        Log.d(TAG, "startSnowflake running on port: " + snowflakePort);
        watchLogFile(logfile);
    }

    private void watchLogFile(File logfile) {
        final Vector<String> lastBuffer = new Vector<>();
        logFileObserver = new FileObserver(logfile.getAbsolutePath()) {
            @Override
            public void onEvent(int event, @Nullable String name) {
                if (FileObserver.MODIFY == event) {
                    try (Scanner scanner = new Scanner(logfile)) {
                        Vector<String> currentBuffer = new Vector<>();
                        while (scanner.hasNextLine()) {
                            currentBuffer.add(scanner.nextLine());
                        }
                        if (lastBuffer.size() < currentBuffer.size()) {
                            int startIndex =  lastBuffer.size() > 0 ? lastBuffer.size() - 1 : 0;
                            int endIndex = currentBuffer.size() - 1;
                            Collection<String> newMessages = currentBuffer.subList(startIndex, endIndex);
                            for (String message : newMessages) {
                                logSnowflakeMessage(message);
                            }
                            lastBuffer.addAll(newMessages);
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        logFileObserver.startWatching();
    }

    @Override
    public void stop() {
        IPtProxy.stopSnowflake();
         try {
            TorStatusObservable.waitUntil(this::isSnowflakeOff, 10);
        } catch (InterruptedException | TimeoutException e) {
            e.printStackTrace();
        }
        snowflakePort = -1;
        logFileObserver.stopWatching();
    }

    private boolean isSnowflakeOff() {
        return TorStatusObservable.getSnowflakeStatus() == TorStatusObservable.SnowflakeStatus.OFF;
    }

    @Override
    public String getTorrc() {
        return "UseBridges 1\n" +
                "ClientTransportPlugin snowflake socks5 127.0.0.1:" + snowflakePort + "\n" +
                "Bridge snowflake 192.0.2.3:1";
    }

    private void loadCdnFronts(Context context) {
        if (mFronts == null) {
            mFronts = new HashMap<>();
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("fronts")));
            String line;
            while (true) {
                line = reader.readLine();
                if (line == null) break;
                String[] front = line.split(" ");
                mFronts.put(front[0], front[1]);
                Log.d(TAG, "front: " + front[0] + ", " + front[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    private String getCdnFront(String service) {
        if (mFronts != null) {
            return mFronts.get(service);
        }
        return null;
    }

    private void logSnowflakeMessage(String message) {
        Matcher matcher = SNOWFLAKE_LOG_TIMESTAMP_PATTERN.matcher(message);
        if (matcher.matches()) {
            try {
                String strippedString = matcher.group(3).trim();
                if (strippedString.length() > 0) {
                    TorStatusObservable.logSnowflakeMessage(contextRef.get(), strippedString);
                }
            } catch (IndexOutOfBoundsException | IllegalStateException e) {
                e.printStackTrace();
            }
        } else {
            TorStatusObservable.logSnowflakeMessage(contextRef.get(), message);
        }
    }
}
