package se.leap.bitmaskclient.base.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

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

    public LocationIndicator(Context context) {
        super(context);
        initLayout(context);
    }

    public LocationIndicator(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initLayout(context);

    }

    public LocationIndicator(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initLayout(context);
    }


    void initLayout(Context context) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootview = inflater.inflate(R.layout.v_location_status_indicator, this, true);
        level1 = rootview.findViewById(R.id.level1);
        level1_2 = rootview.findViewById(R.id.level1_2);
        level2 = rootview.findViewById(R.id.level2);
        level2_2 = rootview.findViewById(R.id.level2_2);
        level3 = rootview.findViewById(R.id.level3);
        level3_2 = rootview.findViewById(R.id.level3_2);
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
                level1.setBackgroundColor(getColor(getContext(), R.color.yellow200));
                level1_2.setBackgroundColor(getColor(getContext(), R.color.yellow200));
                level2.setBackgroundColor(getColor(getContext(), R.color.yellow200));
                level2_2.setBackgroundColor(getColor(getContext(), R.color.yellow200));
                level3.setBackgroundColor(getColor(getContext(), R.color.black800_high_transparent));
                level3_2.setBackgroundColor(getColor(getContext(), R.color.black800_high_transparent));
                break;
            case CRITICAL:
                level1.setBackgroundColor(getColor(getContext(), R.color.red200));
                level1_2.setBackgroundColor(getColor(getContext(), R.color.red200));
                level2.setBackgroundColor(getColor(getContext(), R.color.black800_high_transparent));
                level2_2.setBackgroundColor(getColor(getContext(), R.color.black800_high_transparent));
                level3.setBackgroundColor(getColor(getContext(), R.color.black800_high_transparent));
                level3_2.setBackgroundColor(getColor(getContext(), R.color.black800_high_transparent));
                break;
            default:
                level1.setBackgroundColor(getColor(getContext(), R.color.black800_high_transparent));
                level1_2.setBackgroundColor(getColor(getContext(), R.color.black800_high_transparent));
                level2.setBackgroundColor(getColor(getContext(), R.color.black800_high_transparent));
                level2_2.setBackgroundColor(getColor(getContext(), R.color.black800_high_transparent));
                level3.setBackgroundColor(getColor(getContext(), R.color.black800_high_transparent));
                level3_2.setBackgroundColor(getColor(getContext(), R.color.black800_high_transparent));;
                break;
        }
    }
}
