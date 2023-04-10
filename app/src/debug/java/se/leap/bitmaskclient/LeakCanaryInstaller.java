package se.leap.bitmaskclient;

import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import leakcanary.AppWatcher;

public class LeakCanaryInstaller extends ContentProvider {

    @Override
    public boolean onCreate() {
        if (!isTest()) {
            AppWatcher.INSTANCE.manualInstall((Application)getContext().getApplicationContext());
        }
        return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }


     private boolean isTest() {
        try {
            return  Class.forName("org.junit.Test") != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
