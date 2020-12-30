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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.File;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;

import static se.leap.bitmaskclient.appUpdate.DownloadConnector.APP_TYPE;
import static se.leap.bitmaskclient.appUpdate.FileProviderUtil.getUriFor;
import static se.leap.bitmaskclient.base.models.Constants.REQUEST_CODE_REQUEST_UPDATE;

public class InstallActivity extends Activity {

    private static final String TAG = InstallActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissionAndInstall();
    }

    private void requestPermissionAndInstall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !this.getPackageManager().canRequestPackageInstalls()) {
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:"+getPackageName())),
                    REQUEST_CODE_REQUEST_UPDATE);
        } else {
            installUpdate();
        }
    }

    protected void installUpdate() {
        PreferenceHelper.restartOnUpdate(this.getApplicationContext(), true);

        Intent installIntent = new Intent(Intent.ACTION_VIEW);
        File update = UpdateDownloadManager.getUpdateFile(this.getApplicationContext());
        if (update.exists()) {
            installIntent.setDataAndType(getUriFor(this.getApplicationContext(), update), APP_TYPE);
            installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            this.startActivity(installIntent);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_REQUEST_UPDATE) {
            if (resultCode == RESULT_OK) {
                installUpdate();
            } else {
                Toast.makeText(this, getString(R.string.version_update_error_permissions), Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}
