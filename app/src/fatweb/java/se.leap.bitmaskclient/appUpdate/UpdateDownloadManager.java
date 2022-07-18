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
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import java.io.File;

import okhttp3.OkHttpClient;
import pgpverify.Logger;
import pgpverify.PgpVerifier;
import se.leap.bitmaskclient.BuildConfig;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.providersetup.connectivity.OkHttpClientGenerator;

import static android.text.TextUtils.isEmpty;
import static se.leap.bitmaskclient.appUpdate.DownloadService.DOWNLOAD_FAILED;
import static se.leap.bitmaskclient.appUpdate.DownloadService.DOWNLOAD_PROGRESS;
import static se.leap.bitmaskclient.appUpdate.DownloadService.NO_NEW_VERISON;
import static se.leap.bitmaskclient.appUpdate.DownloadService.NO_PUB_KEY;
import static se.leap.bitmaskclient.appUpdate.DownloadService.PROGRESS_VALUE;
import static se.leap.bitmaskclient.appUpdate.DownloadService.UPDATE_DOWNLOADED;
import static se.leap.bitmaskclient.appUpdate.DownloadService.UPDATE_DOWNLOAD_FAILED;
import static se.leap.bitmaskclient.appUpdate.DownloadService.UPDATE_FOUND;
import static se.leap.bitmaskclient.appUpdate.DownloadService.UPDATE_NOT_FOUND;
import static se.leap.bitmaskclient.appUpdate.DownloadService.VERIFICATION_ERROR;
import static se.leap.bitmaskclient.appUpdate.DownloadServiceCommand.CHECK_VERSION_FILE;
import static se.leap.bitmaskclient.appUpdate.DownloadServiceCommand.DOWNLOAD_UPDATE;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_DOWNLOAD_SERVICE_EVENT;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_CODE;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_KEY;
import static se.leap.bitmaskclient.base.utils.FileHelper.readPublicKey;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.DELAY;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.PARAMETERS;
import static se.leap.bitmaskclient.providersetup.ProviderAPI.RECEIVER_KEY;

public class UpdateDownloadManager implements Logger, DownloadConnector.DownloadProgress {


    private static final String TAG = UpdateDownloadManager.class.getSimpleName();

    public interface DownloadServiceCallback {
        void broadcastEvent(Intent intent);
    }

    private Context context;

    private PgpVerifier pgpVerifier;
    private DownloadServiceCallback serviceCallback;
    OkHttpClientGenerator clientGenerator;


    public UpdateDownloadManager(Context context, OkHttpClientGenerator clientGenerator, DownloadServiceCallback callback) {
        this.context = context;
        this.clientGenerator = clientGenerator;
        pgpVerifier = new PgpVerifier();
        pgpVerifier.setLogger(this);
        serviceCallback = callback;
    }

    //pgpverify Logger interface
    @Override
    public void log(String s) {

    }

    @Override
    public void onUpdate(int progress) {
        Bundle resultData = new Bundle();
        resultData.putInt(PROGRESS_VALUE, progress);
        broadcastEvent(DOWNLOAD_PROGRESS, resultData);
    }

    public void handleIntent(Intent command) {
        ResultReceiver receiver = null;
        if (command.getParcelableExtra(RECEIVER_KEY) != null) {
            receiver = command.getParcelableExtra(RECEIVER_KEY);
        }
        String action = command.getAction();
        Bundle parameters = command.getBundleExtra(PARAMETERS);

        if (parameters.containsKey(DELAY)) {
            try {
                Thread.sleep(parameters.getLong(DELAY));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Bundle result = new Bundle();
        switch (action) {
            case CHECK_VERSION_FILE:
                result = checkVersionFile(result);
                if (result.getBoolean(BROADCAST_RESULT_KEY)) {
                    sendToReceiverOrBroadcast(receiver, UPDATE_FOUND, result);
                } else {
                    sendToReceiverOrBroadcast(receiver, UPDATE_NOT_FOUND, result);
                }
                break;
            case DOWNLOAD_UPDATE:
                result = downloadUpdate(result);
                if (result.getBoolean(BROADCAST_RESULT_KEY)) {
                    sendToReceiverOrBroadcast(receiver, UPDATE_DOWNLOADED, result);
                } else {
                    sendToReceiverOrBroadcast(receiver, UPDATE_DOWNLOAD_FAILED, result);
                }
                break;
        }
    }

    public static File getUpdateFile(Context context) {
        return new File(context.getExternalFilesDir(null) + "/" + context.getString(R.string.app_name) + "_update.apk");
    }

    private Bundle downloadUpdate(Bundle task) {

        String publicKey = readPublicKey(context);
        if (isEmpty(publicKey)) {
            task.putBoolean(BROADCAST_RESULT_KEY, false);
            task.putBoolean(NO_PUB_KEY, true);
            return task;
        }

        OkHttpClient client = clientGenerator.init();
        String signature = DownloadConnector.requestTextFileFromServer(BuildConfig.signature_url, client);
        if (signature == null) {
            task.putBoolean(BROADCAST_RESULT_KEY, false);
            task.putBoolean(DOWNLOAD_FAILED, true);
            return task;
        }

        File destinationFile = getUpdateFile(context);
        if (destinationFile.exists()) {
            destinationFile.delete();
        }

        destinationFile = DownloadConnector.requestFileFromServer(BuildConfig.update_apk_url, client, destinationFile, this);

        if (destinationFile == null) {
            task.putBoolean(BROADCAST_RESULT_KEY, false);
            task.putBoolean(DOWNLOAD_FAILED, true);
            return task;
        }

        boolean successfulVerified = pgpVerifier.verify(signature, publicKey, destinationFile.getAbsolutePath());
        if (!successfulVerified) {
            destinationFile.delete();
            task.putBoolean(BROADCAST_RESULT_KEY, false);
            task.putBoolean(VERIFICATION_ERROR, true);
            return task;
        }

        task.putBoolean(BROADCAST_RESULT_KEY, true);
        return task;
    }

    private Bundle checkVersionFile(Bundle task) {
        OkHttpClient client = clientGenerator.init();
        String versionString = DownloadConnector.requestTextFileFromServer(BuildConfig.version_file_url, client);

        if (versionString != null) {
            versionString = versionString.replace("\n", "").trim();
        }

        int version = -1;
        try {
            version = Integer.parseInt(versionString);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            Log.e(TAG, "could not parse version code: " + versionString);
        }

        if (version == -1) {
            task.putBoolean(BROADCAST_RESULT_KEY, false);
            task.putBoolean(DOWNLOAD_FAILED, true);
        } else if (BuildConfig.VERSION_CODE >= version) {
            task.putBoolean(BROADCAST_RESULT_KEY, false);
            task.putBoolean(NO_NEW_VERISON, true);
        } else {
            task.putBoolean(BROADCAST_RESULT_KEY, true);
        }
        return task;
    }

    private void sendToReceiverOrBroadcast(ResultReceiver receiver, int resultCode, Bundle resultData) {
        if (resultData == null || resultData == Bundle.EMPTY) {
            resultData = new Bundle();
        }
        if (receiver != null) {
            receiver.send(resultCode, resultData);
        } else {
            broadcastEvent(resultCode, resultData);
        }
    }

    private void broadcastEvent(int resultCode , Bundle resultData) {
        Intent intentUpdate = new Intent(BROADCAST_DOWNLOAD_SERVICE_EVENT);
        intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
        intentUpdate.putExtra(BROADCAST_RESULT_CODE, resultCode);
        intentUpdate.putExtra(BROADCAST_RESULT_KEY, resultData);
        serviceCallback.broadcastEvent(intentUpdate);
    }

}
