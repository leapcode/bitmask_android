package se.leap.bitmaskclient.base.utils;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
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
            actionBarTitle.setSubtitleTextColor(ContextCompat.getColor(bar.getThemedContext(), actionBarTextColor));
        }

        Toolbar tb = activity.findViewById(R.id.toolbar);
        if (tb != null && tb.getOverflowIcon() != null) {
            tb.getOverflowIcon().setTint(ContextCompat.getColor(bar.getThemedContext(), actionBarTextColor));
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

    public static void tintMenuIcons(Context context, Menu menu, @ColorRes int color) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            Drawable normalDrawable = item.getIcon();
            Drawable wrapDrawable = DrawableCompat.wrap(normalDrawable);
            DrawableCompat.setTint(wrapDrawable, context.getResources().getColor(color));

            item.setIcon(wrapDrawable);
        }
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

    public interface AnimationInterface {
        void onAnimationEnd();
    }

    public static void animateContainerVisibility(View container, boolean isExpanded) {
        animateContainerVisibility(container, isExpanded, null);
    }

        public static void animateContainerVisibility(View container, boolean isExpanded, AnimationInterface animationInterface) {

        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

        container.measure(widthMeasureSpec, heightMeasureSpec);
        int measuredHeight = container.getMeasuredHeight();

        int targetHeight = isExpanded ? 0 : measuredHeight; // Get the actual content height of the view
        int initialHeight = isExpanded ? measuredHeight : 0;

        ValueAnimator animator = ValueAnimator.ofInt(initialHeight, targetHeight);
        animator.setDuration(250); // Set the duration of the animation in milliseconds

        animator.addUpdateListener(animation -> {
            container.getLayoutParams().height = (int) animation.getAnimatedValue();
            container.requestLayout();
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(@NonNull Animator animation) {
                if (initialHeight == 0 && container.getVisibility() == GONE) {
                    container.setVisibility(VISIBLE);
                }
            }

            @Override
            public void onAnimationEnd(@NonNull Animator animation) {
                if (targetHeight == 0) {
                    container.setVisibility(GONE);
                }
                if (animationInterface != null) {
                    animationInterface.onAnimationEnd();
                }
            }

            @Override
            public void onAnimationCancel(@NonNull Animator animation) {
                container.setVisibility(targetHeight == 0 ? GONE : VISIBLE);
            }

            @Override
            public void onAnimationRepeat(@NonNull Animator animation) {}
        });

        animator.start();
    }

    public static void hideKeyboardFrom(Context context, View view) {
        if (context == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static boolean isKeyboardShown(Context context) {
        if (context == null) {
            return false;
        }
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        return imm.isActive();
    }

}
