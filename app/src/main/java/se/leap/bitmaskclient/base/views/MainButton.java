package se.leap.bitmaskclient.base.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.RelativeLayout;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

import java.lang.ref.WeakReference;

import se.leap.bitmaskclient.R;

public class MainButton extends RelativeLayout {

    private static final String TAG = MainButton.class.getSimpleName();

    interface MainButtonListener {
        void onButtonClicked();
    }

    AppCompatImageView glow;
    AppCompatImageView shadowLight;
    AnimationDrawable glowAnimation;
    WeakReference<MainButtonListener> callback;

    private boolean isOn = false;
    private boolean isProcessing = false;
    private boolean isError = true;



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

    public void setMainButtonListener(MainButtonListener callback) {
        this.callback = new WeakReference<>(callback);
    }

    private void initLayout(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootview = inflater.inflate(R.layout.v_main_btn, this, true);

        glow = rootview.findViewById(R.id.vpn_btn_glow);
        glowAnimation = (AnimationDrawable) glow.getBackground();
        shadowLight = rootview.findViewById(R.id.vpn_btn_shadow_light);

        rootview.setOnGenericMotionListener(new OnGenericMotionListener() {
            @Override
            public boolean onGenericMotion(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_BUTTON_PRESS:
                        Log.d(TAG, "onbuttonPrees");
                        break;
                    case MotionEvent.ACTION_BUTTON_RELEASE:
                        Log.d(TAG, "onButtonRelease");
                        break;
                }
                return false;
            }
        });
    }


    private void stopGlowAnimation() {
        AlphaAnimation fadeOutAnimation = new AlphaAnimation(1.0f, 0.0f);
        fadeOutAnimation.setDuration(300);
        fadeOutAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                glow.setVisibility(GONE);
                glowAnimation.stop();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        glow.startAnimation(fadeOutAnimation);
    }

    private void startGlowAnimation() {
        glow.setAlpha(1.0f);
        glow.setVisibility(VISIBLE);
        glowAnimation.start();
    }

    public void updateState(boolean isOn, boolean isProcessing, boolean isError) {
        if (this.isOn != isOn) {
            this.isOn = isOn;
            shadowLight.setVisibility(isOn ? VISIBLE : GONE);
        }

        if (this.isProcessing != isProcessing) {
            if (!isProcessing) {
               stopGlowAnimation();
            } else {
                startGlowAnimation();
            }
            this.isProcessing = isProcessing;
        }

        if (this.isError != isError) {
            @DrawableRes int drawableResource = isOn ? R.drawable.on_off_btn_start_2_enabled : R.drawable.on_off_btn_start_2_disabled;
            if (!isError) {
                setImageWithTint(shadowLight, drawableResource, R.color.colorSecondary);
            } else {
                setImageWithTint(shadowLight, drawableResource, R.color.colorWarning);
            }
            this.isError = isError;
        }
    }

    private void setImageWithTint(AppCompatImageView view, @DrawableRes int resourceId, @ColorRes int color) {
        view.setImageDrawable(ContextCompat.getDrawable(getContext(), resourceId));
        view.setColorFilter(ContextCompat.getColor(getContext(), color), PorterDuff.Mode.SRC_ATOP);
    }



}
