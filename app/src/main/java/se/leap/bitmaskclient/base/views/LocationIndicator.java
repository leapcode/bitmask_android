package se.leap.bitmaskclient.base.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.eip.GatewaysManager;

import static androidx.core.content.ContextCompat.getColor;

public class LocationIndicator extends LinearLayout {

    private View level1;
    private View level1_2;
    private View level2;
    private View level2_2;
    private View level3;
    private View level3_2;
    private int tintColor;

    public LocationIndicator(Context context) {
        super(context);
        initLayout(context, null);
    }

    public LocationIndicator(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initLayout(context, attrs);

    }

    public LocationIndicator(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initLayout(context, attrs);
    }


    void initLayout(Context context, AttributeSet attrs) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootview = inflater.inflate(R.layout.v_location_status_indicator, this, true);
        level1 = rootview.findViewById(R.id.level1);
        level1_2 = rootview.findViewById(R.id.level1_2);
        level2 = rootview.findViewById(R.id.level2);
        level2_2 = rootview.findViewById(R.id.level2_2);
        level3 = rootview.findViewById(R.id.level3);
        level3_2 = rootview.findViewById(R.id.level3_2);
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LocationIndicator);
            this.tintColor = typedArray.getColor(R.styleable.LocationIndicator_tint, getColor(context, R.color.black800_high_transparent));
            typedArray.recycle();
        } else {
            this.tintColor = getColor(context, R.color.black800_high_transparent);
        }
    }

    public void setLoad(GatewaysManager.Load load) {
        switch (load) {
            case GOOD:
                level1.setBackgroundColor(getColor(getContext(), R.color.green200));
                level1_2.setBackgroundColor(getColor(getContext(), R.color.green200));
                level2.setBackgroundColor(getColor(getContext(), R.color.green200));
                level2_2.setBackgroundColor(getColor(getContext(), R.color.green200));
                level3.setBackgroundColor(getColor(getContext(), R.color.green200));
                level3_2.setBackgroundColor(getColor(getContext(), R.color.green200));
                break;
            case AVERAGE:
                level1.setBackgroundColor(getColor(getContext(), R.color.amber200));
                level1_2.setBackgroundColor(getColor(getContext(), R.color.amber200));
                level2.setBackgroundColor(getColor(getContext(), R.color.amber200));
                level2_2.setBackgroundColor(getColor(getContext(), R.color.amber200));
                level3.setBackgroundColor(tintColor);
                level3_2.setBackgroundColor(tintColor);
                break;
            case CRITICAL:
                level1.setBackgroundColor(getColor(getContext(), R.color.red200));
                level1_2.setBackgroundColor(getColor(getContext(), R.color.red200));
                level2.setBackgroundColor(tintColor);
                level2_2.setBackgroundColor(tintColor);
                level3.setBackgroundColor(tintColor);
                level3_2.setBackgroundColor(tintColor);
                break;
            default:
                level1.setBackgroundColor(tintColor);
                level1_2.setBackgroundColor(tintColor);
                level2.setBackgroundColor(tintColor);
                level2_2.setBackgroundColor(tintColor);
                level3.setBackgroundColor(tintColor);
                level3_2.setBackgroundColor(tintColor);
                break;
        }
    }
}
