package se.leap.bitmaskclient.base.utils;

import android.content.Context;

import androidx.annotation.DimenRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

/**
 * Created by cyberta on 29.06.18.
 */

public class ViewHelper {

    public static int convertDimensionToPx(Context context, @DimenRes int dimension) {
        return context.getResources().getDimensionPixelSize(dimension);
    }

    /**
     * Sets the subtitle of an activities action bar. The activity needs to be an AppCompatActivity.
     * @param fragment
     * @param stringId
     */
    public static void setActionBarTitle(Fragment fragment, @StringRes int stringId) {
        AppCompatActivity appCompatActivity = (AppCompatActivity) fragment.getActivity();
        if (appCompatActivity != null) {
            ActionBar actionBar = appCompatActivity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setSubtitle(stringId);
            }
        }
    }

}
