package se.leap.bitmaskclient.base.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.eip.GatewaysManager;

public class LocationButton extends LinearLayoutCompat {
    private LocationIndicator locationIndicator;
    private AppCompatTextView textView;
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
    }

    public void setLocationLoad(GatewaysManager.Load load) {
        locationIndicator.setLoad(load);
    }

    public void setText(CharSequence text) {
        textView.setText(text);
    }
}
