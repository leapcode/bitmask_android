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

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import se.leap.bitmaskclient.providersetup.connectivity.OkHttpClientGenerator;


public class DownloadService extends JobIntentService implements UpdateDownloadManager.DownloadServiceCallback {

    static final int JOB_ID = 161376;
    static final String NOTIFICATION_CHANNEL_NEWSTATUS_ID = "bitmask_download_service_news";

    final public static String TAG = DownloadService.class.getSimpleName(),
            PROGRESS_VALUE = "progressValue",
            NO_NEW_VERISON = "noNewVersion",
            DOWNLOAD_FAILED = "downloadFailed",
            NO_PUB_KEY = "noPubKey",
            VERIFICATION_ERROR = "verificationError";

    final public static int
            UPDATE_DOWNLOADED = 1,
            UPDATE_DOWNLOAD_FAILED = 2,
            UPDATE_FOUND = 3,
            UPDATE_NOT_FOUND = 4,
            DOWNLOAD_PROGRESS = 6;


    private UpdateDownloadManager updateDownloadManager;


    @Override
    public void onCreate() {
        super.onCreate();
        updateDownloadManager = initDownloadManager();
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        updateDownloadManager.handleIntent(intent);
    }

    /**
     * Convenience method for enqueuing work in to this service.
     */
    static void enqueueWork(Context context, Intent work) {
        try {
            DownloadService.enqueueWork(context, DownloadService.class, JOB_ID, work);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private UpdateDownloadManager initDownloadManager() {
        OkHttpClientGenerator clientGenerator = new OkHttpClientGenerator(null);
        return new UpdateDownloadManager(this, clientGenerator, this);
    }

    @Override
    public void broadcastEvent(Intent intent) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
