package se.leap.bitmaskclient.base.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import se.leap.bitmaskclient.R;
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
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootview = inflater.inflate(R.layout.v_location_button, this, true);
        locationIndicator = rootview.findViewById(R.id.load_indicator);
        textView = rootview.findViewById(R.id.text_location);
        bridgeView = rootview.findViewById(R.id.bridge_icn);
        recommendedView = rootview.findViewById(R.id.recommended_icn);
    }

    public void setLocationLoad(GatewaysManager.Load load) {
        locationIndicator.setLoad(load);
    }

    public void setText(CharSequence text) {
        textView.setText(text);
    }

    public void showBridgeIndicator(boolean show) {
        bridgeView.setVisibility(show ? VISIBLE : GONE);
    }

    public void showRecommendedIndicator(boolean show) {
        recommendedView.setVisibility(show? VISIBLE : GONE );
    }
}
