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
import android.os.ResultReceiver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import se.leap.bitmaskclient.providersetup.ProviderAPI;

public class DownloadServiceCommand {

    public final static String
            CHECK_VERSION_FILE = "checkVersionFile",
            DOWNLOAD_UPDATE = "downloadUpdate";

    private Context context;
    private String action;
    private ResultReceiver resultReceiver;

    private DownloadServiceCommand(@NonNull Context context, @NonNull String action) {
        this(context.getApplicationContext(), action, null);
    }

    private DownloadServiceCommand(@NonNull Context context, @NonNull String action, @Nullable ResultReceiver resultReceiver) {
        super();
        this.context = context;
        this.action = action;
        this.resultReceiver = resultReceiver;
    }


    private Intent setUpIntent() {
        Intent command = new Intent(context, ProviderAPI.class);
        command.setAction(action);
        if (resultReceiver != null) {
            command.putExtra(ProviderAPI.RECEIVER_KEY, resultReceiver);
        }
        return command;
    }

    private boolean isInitialized() {
        return context != null;
    }


    private void execute() {
        if (isInitialized()) {
            Intent intent = setUpIntent();
            DownloadService.enqueueWork(context, intent);
        }
    }

    public static void execute(Context context, String action) {
        DownloadServiceCommand command = new DownloadServiceCommand(context, action);
        command.execute();
    }

    public static void execute(Context context, String action, ResultReceiver resultReceiver) {
        DownloadServiceCommand command = new DownloadServiceCommand(context, action, resultReceiver);
        command.execute();
    }

}
