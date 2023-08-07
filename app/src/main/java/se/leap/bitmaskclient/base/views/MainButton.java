package se.leap.bitmaskclient.base.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;

import androidx.annotation.DrawableRes;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.databinding.VMainButtonBinding;

public class MainButton extends RelativeLayout {

    private static final String TAG = MainButton.class.getSimpleName();

    AppCompatImageView button;

    public MainButton(Context context) {
        super(context);
        initLayout(context);
    }

    public MainButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initLayout(context);
    }

    public MainButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initLayout(context);
    }


    @TargetApi(21)
    public MainButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initLayout(context);
    }

    private void initLayout(Context context) {
        VMainButtonBinding binding = VMainButtonBinding.inflate(LayoutInflater.from(context), this, true);
        button = binding.button;
    }

    public void updateState(boolean isOn, boolean isProcessing) {
        if (isProcessing) {
            button.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.button_circle_cancel));
            button.setTag("button_circle_cancel");
        } else {
            button.setImageDrawable(
                    ContextCompat.getDrawable(getContext(),
                    isOn ? R.drawable.button_circle_stop : R.drawable.button_circle_start));
            button.setTag(isOn ? "button_circle_stop" : "button_circle_start");
        }
    }

    public void setCustomDrawable(@DrawableRes int drawableResource) {
        Drawable drawable = ContextCompat.getDrawable(getContext(), drawableResource);
        if (drawable == null) {
            return;
        }

        button.setImageDrawable(drawable);
        button.setTag("button_setup_circle_custom");
    }
}
