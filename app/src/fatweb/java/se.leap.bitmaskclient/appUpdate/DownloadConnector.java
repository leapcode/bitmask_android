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
package se.leap.bitmaskclient.appUpdate;


import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;


/**
 * This class encapsulates HTTP requests so that the results can be mocked
 * and it's owning UpdateDownloadManager class logic can be unit tested properly
 *
 */
public class DownloadConnector {

    private static final String TAG = DownloadConnector.class.getSimpleName();
    public final static String APP_TYPE = "application/vnd.android.package-archive";
    public final static String TEXT_FILE_TYPE = "application/text";

    public interface DownloadProgress {
        void onUpdate(int progress);
    }

    static String requestTextFileFromServer(@NonNull String url, @NonNull OkHttpClient okHttpClient) {
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", TEXT_FILE_TYPE)
                    .build();

            Response response = okHttpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                return null;
            }
            InputStream inputStream = response.body().byteStream();
            Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
            if (scanner.hasNext()) {
                return scanner.next();
            }
            return null;

        } catch (Exception e) {
            Log.d(TAG, "Text file download failed");
        }

        return null;
    }

    static File requestFileFromServer(@NonNull String url, @NonNull OkHttpClient okHttpClient, File destFile, DownloadProgress callback) {
        BufferedSink sink;
        BufferedSource source;
        try {
            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", APP_TYPE);
            Request request = requestBuilder.build();

            Response response = okHttpClient.newCall(request).execute();
            ResponseBody body = response.body();
            long contentLength = body.contentLength();
            source = body.source();
            sink = Okio.buffer(Okio.sink(destFile));
            Buffer sinkBuffer = sink.buffer();
            long totalBytesRead = 0;
            int bufferSize = 8 * 1024;
            long bytesRead;
            int lastProgress = 0;
            while ((bytesRead = source.read(sinkBuffer, bufferSize)) != -1) {
                sink.emit();
                totalBytesRead += bytesRead;
                int progress = (int) ((totalBytesRead * 100) / contentLength);
                // debouncing callbacks
                if (lastProgress < progress) {
                    lastProgress = progress;
                    callback.onUpdate(progress);
                }
            }
            sink.flush();

            return destFile;

        } catch (Exception e) {
            Log.d(TAG, "File download failed");
        }

        return null;
    }

}
