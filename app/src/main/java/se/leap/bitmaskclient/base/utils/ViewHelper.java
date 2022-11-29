package se.leap.bitmaskclient.base.utils;

import android.app.Notification;
import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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

    public static boolean isBrightColor(int color) {
        if (android.R.color.transparent == color)
            return true;

        boolean rtnValue = false;

        int[] rgb = { Color.red(color), Color.green(color), Color.blue(color) };

        int brightness = (int) Math.sqrt(rgb[0] * rgb[0] * .241 + rgb[1]
                * rgb[1] * .691 + rgb[2] * rgb[2] * .068);

        // color is light
        if (brightness >= 200) {
            rtnValue = true;
        }

        return rtnValue;
    }

    public static void setActionBarTextColor(ActionBar bar, @ColorRes int titleColor) {
        CharSequence titleCharSequence = bar.getTitle();
        if (titleCharSequence == null) {
            return;
        }
        String title = titleCharSequence.toString();
        Spannable spannableTitle = new SpannableString(title);
        spannableTitle.setSpan(new ForegroundColorSpan(ContextCompat.getColor(bar.getThemedContext(), titleColor)), 0, spannableTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        bar.setTitle(spannableTitle);
    }

}
