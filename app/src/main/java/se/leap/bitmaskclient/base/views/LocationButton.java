package se.leap.bitmaskclient.base.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import se.leap.bitmaskclient.databinding.VLocationButtonBinding;
import se.leap.bitmaskclient.eip.GatewaysManager;

public class LocationButton extends RelativeLayout {
    private LocationIndicator locationIndicator;
    private AppCompatTextView textView;
    private AppCompatImageView bridgeView;
    private AppCompatImageView recommendedView;

    public LocationButton(@NonNull Context context) {
        super(context);
        initLayout(context);
    }

    public LocationButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initLayout(context);
    }

    private void initLayout(Context context) {
        VLocationButtonBinding binding = VLocationButtonBinding.inflate(LayoutInflater.from(context), this, true);
        locationIndicator = binding.loadIndicator;
        textView = binding.textLocation;
        bridgeView = binding.bridgeIcn;
        recommendedView = binding.recommendedIcn;

    }

    public void setTextColor(@ColorRes int color) {
        textView.setTextColor(ContextCompat.getColor(getContext(), color));
    }

    public void setLocationLoad(GatewaysManager.Load load) {
        locationIndicator.setLoad(load);
    }

    public void setText(CharSequence text) {
        textView.setText(text);
    }

    public void setText(@StringRes int resId) {
        textView.setText(resId);
    }

    public void showBridgeIndicator(boolean show) {
        bridgeView.setVisibility(show ? VISIBLE : GONE);
    }

    public void showRecommendedIndicator(boolean show) {
        recommendedView.setVisibility(show? VISIBLE : GONE );
    }
}
