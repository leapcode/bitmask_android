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
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import java.io.File;

import se.leap.bitmaskclient.BuildConfig;

public class FileProviderUtil {

    private static final String AUTHORITY = BuildConfig.APPLICATION_ID +".fileprovider";

    public static Uri getUriFor(@NonNull Context context, @NonNull File file) {
        if (Build.VERSION.SDK_INT >= 24) return FileProvider.getUriForFile(context, AUTHORITY, file);
        else                             return Uri.fromFile(file);
    }
}
