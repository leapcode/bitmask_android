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

import static android.app.Activity.RESULT_CANCELED;
import static se.leap.bitmaskclient.appUpdate.DownloadService.DOWNLOAD_PROGRESS;
import static se.leap.bitmaskclient.appUpdate.DownloadService.NO_NEW_VERISON;
import static se.leap.bitmaskclient.appUpdate.DownloadService.PROGRESS_VALUE;
import static se.leap.bitmaskclient.appUpdate.DownloadService.UPDATE_DOWNLOADED;
import static se.leap.bitmaskclient.appUpdate.DownloadService.UPDATE_DOWNLOAD_FAILED;
import static se.leap.bitmaskclient.appUpdate.DownloadService.UPDATE_FOUND;
import static se.leap.bitmaskclient.appUpdate.DownloadService.UPDATE_NOT_FOUND;
import static se.leap.bitmaskclient.appUpdate.DownloadService.VERIFICATION_ERROR;
import static se.leap.bitmaskclient.appUpdate.DownloadServiceCommand.DOWNLOAD_UPDATE;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_DOWNLOAD_SERVICE_EVENT;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_CODE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.models.Constants;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;

public class DownloadBroadcastReceiver extends BroadcastReceiver {

    public static final String ACTION_DOWNLOAD = "se.leap.bitmaskclient.appUpdate.ACTION_DOWNLOAD";
    private static final String TAG = DownloadBroadcastReceiver.class.getSimpleName();

    private DownloadNotificationManager notificationManager;


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }

        if (notificationManager == null) {
            notificationManager = new DownloadNotificationManager(context.getApplicationContext());
        }

        int resultCode = intent.getIntExtra(BROADCAST_RESULT_CODE, RESULT_CANCELED);
        Bundle resultData = intent.getParcelableExtra(Constants.BROADCAST_RESULT_KEY);

        switch (action) {
            case BROADCAST_DOWNLOAD_SERVICE_EVENT:
                switch (resultCode) {
                    case UPDATE_FOUND:
                        notificationManager.buildDownloadFoundNotification();
                        break;
                    case UPDATE_NOT_FOUND:
                        if (resultData.getBoolean(NO_NEW_VERISON, false)) {
                            PreferenceHelper.setLastAppUpdateCheck();
                        }
                        break;
                    case UPDATE_DOWNLOADED:
                        notificationManager.buildDownloadSuccessfulNotification();
                        break;
                    case UPDATE_DOWNLOAD_FAILED:
                        if (resultData.getBoolean(VERIFICATION_ERROR, false)) {
                            Toast.makeText(context.getApplicationContext(), context.getString(R.string.version_update_error_pgp_verification), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(context.getApplicationContext(), context.getString(R.string.version_update_error), Toast.LENGTH_LONG).show();
                        }
                        notificationManager.cancelNotifications();
                        break;
                    case DOWNLOAD_PROGRESS:
                        int progress = resultData.getInt(PROGRESS_VALUE, 0);
                        notificationManager.buildDownloadUpdateProgress(progress);
                        break;
                }
                break;

            case ACTION_DOWNLOAD:
                DownloadServiceCommand.execute(context.getApplicationContext(), DOWNLOAD_UPDATE);
                break;

            default:
                break;
        }

    }
}
