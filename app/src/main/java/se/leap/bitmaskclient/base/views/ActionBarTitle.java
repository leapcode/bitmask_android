package se.leap.bitmaskclient.base.views;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;

import se.leap.bitmaskclient.databinding.VActionbarTitleBinding;

public class ActionBarTitle extends LinearLayoutCompat {
    private AppCompatTextView actionBarTitle;
    private AppCompatTextView actionBarSubtitle;
    private LinearLayoutCompat container;

    public ActionBarTitle(@NonNull Context context) {
        super(context);
        initLayout(context);
    }

    public ActionBarTitle(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initLayout(context);
    }

    public ActionBarTitle(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initLayout(context);
    }

    public void initLayout(Context context) {
        VActionbarTitleBinding binding = VActionbarTitleBinding.inflate(LayoutInflater.from(context), this, true);
        actionBarTitle = binding.actionBarTitle;
        actionBarSubtitle = binding.actionBarSubtitle;
        container = binding.actionBarTitleContainer;
    }

    public void setTitle(CharSequence text) {
        actionBarTitle.setText(text);
    }

    public void setTitleCaps(boolean caps) {
        actionBarTitle.setAllCaps(caps);
    }

    public void setSubtitle(CharSequence text) {
        actionBarSubtitle.setText(text);
    }

    public void setTitle(@StringRes int resId) {
        actionBarTitle.setText(resId);
    }

    public void setSubtitle(@StringRes int resId) {
        actionBarSubtitle.setText(resId);
    }

    public void setTitleTextColor(@ColorInt int color) {
        actionBarTitle.setTextColor(color);
    }

    public @ColorInt int getTitleTextColor() {
        return actionBarTitle.getCurrentTextColor();
    }

    public void setSubtitleTextColor(@ColorInt int color) {
        actionBarSubtitle.setTextColor(color);
    }

    public void showSubtitle(boolean show) {
        actionBarSubtitle.setVisibility(show ? VISIBLE : GONE);
    }

    public void setCentered(boolean centered) {
        LayoutParams titleLayoutParams = (LayoutParams) actionBarTitle.getLayoutParams();
        LayoutParams subtitleLayoutParams = (LayoutParams) actionBarSubtitle.getLayoutParams();
        LayoutParams containerLayoutParams = (LayoutParams) container.getLayoutParams();
        if (centered) {
            titleLayoutParams.gravity = Gravity.CENTER;
            subtitleLayoutParams.gravity = Gravity.CENTER;
            containerLayoutParams.gravity = Gravity.CENTER;
        } else {
            titleLayoutParams.gravity = Gravity.NO_GRAVITY;
            subtitleLayoutParams.gravity = Gravity.NO_GRAVITY;
            containerLayoutParams.gravity = Gravity.NO_GRAVITY;
        }
        actionBarTitle.setLayoutParams(titleLayoutParams);
        actionBarSubtitle.setLayoutParams(subtitleLayoutParams);
        container.setLayoutParams(containerLayoutParams);
    }

    public void setSingleBoldTitle() {
        showSubtitle(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            actionBarTitle.setTypeface(Typeface.create(null,900,false));
        } else {
            actionBarTitle.setTypeface(actionBarTitle.getTypeface(), Typeface.BOLD);
        }
        actionBarTitle.setLetterSpacing(0.05f);
        actionBarTitle.setTextSize(24f);
    }
}
