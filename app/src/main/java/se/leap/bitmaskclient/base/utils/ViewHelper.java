package se.leap.bitmaskclient.base.utils;

import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.views.ActionBarTitle;

/**
 * Created by cyberta on 29.06.18.
 */

public class ViewHelper {

    private static final String TAG = ViewHelper.class.getSimpleName();

    public static int convertDimensionToPx(Context context, @DimenRes int dimension) {
        return context.getResources().getDimensionPixelSize(dimension);
    }

    /**
     * Sets the subtitle of an activities action bar. The activity needs to be an AppCompatActivity.
     * @param fragment
     * @param stringId
     */
    public static void setActionBarSubtitle(Fragment fragment, @StringRes int stringId) {
        AppCompatActivity appCompatActivity = (AppCompatActivity) fragment.getActivity();
        if (appCompatActivity != null) {
            ActionBar actionBar = appCompatActivity.getSupportActionBar();
            if (actionBar != null) {
                View customView = actionBar.getCustomView();
                if (customView instanceof ActionBarTitle) {
                    ActionBarTitle actionBarTitle = (ActionBarTitle) customView;
                    actionBarTitle.setSubtitle(stringId);
                    actionBarTitle.showSubtitle(true);
                }
            }
        }
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
                View customView = actionBar.getCustomView();
                if (customView instanceof ActionBarTitle) {
                    ActionBarTitle actionBarTitle = (ActionBarTitle) customView;
                    actionBarTitle.setTitle(stringId);
                    actionBarTitle.showSubtitle(false);
                } else {
                    Log.e(TAG, "ActionBar has no custom action title!");
                }
            }
        }
    }


    public static void setDefaultActivityBarColor(Activity activity) {
        setActivityBarColor(activity, R.color.colorPrimary, R.color.colorPrimaryDark, R.color.colorActionBarTitleFont);
    }

    public static void setActivityBarColor(Activity activity, @ColorRes int primaryColor, @ColorRes int secondaryColor, @ColorRes int textColor) {
        if (!(activity instanceof AppCompatActivity)) {
            return;
        }

        ActionBar bar = ((AppCompatActivity) activity).getSupportActionBar();
        if (bar == null) {
            return;
        }
        int color = ContextCompat.getColor(activity, secondaryColor);
        bar.setBackgroundDrawable(new ColorDrawable(color));
        Window window = activity.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(activity, primaryColor));

        int actionBarTextColor;
        if (textColor == 0) {
            actionBarTextColor = isBrightColor(color) ? R.color.actionbar_connectivity_state_text_color_dark : R.color.actionbar_connectivity_state_text_color_light;
        } else {
            actionBarTextColor = textColor;
        }

        View customView = bar.getCustomView();
        if (customView instanceof ActionBarTitle) {
            ActionBarTitle actionBarTitle = (ActionBarTitle) customView;
            actionBarTitle.setTitleTextColor(ContextCompat.getColor(bar.getThemedContext(), actionBarTextColor));
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
        if (titleCharSequence == null || titleCharSequence.length() == 0) {
            return;
        }
        String title = titleCharSequence.toString();
        Spannable spannableTitle = new SpannableString(title);
        spannableTitle.setSpan(new ForegroundColorSpan(ContextCompat.getColor(bar.getThemedContext(), titleColor)), 0, spannableTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        bar.setTitle(spannableTitle);
    }

}
