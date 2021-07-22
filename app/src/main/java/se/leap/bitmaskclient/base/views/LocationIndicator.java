package se.leap.bitmaskclient.base.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.eip.GatewaysManager;

public class LocationIndicator extends LinearLayout {

    private View level1;
    private View level2;
    private View level3;

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
        level2 = rootview.findViewById(R.id.level2);
        level3 = rootview.findViewById(R.id.level3);
    }

    public void setLoad(GatewaysManager.Load load) {
        switch (load) {
            case GOOD:
                level1.setBackgroundColor(getResources().getColor(R.color.green200));
                level2.setBackgroundColor(getResources().getColor(R.color.green200));
                level3.setBackgroundColor(getResources().getColor(R.color.green200));
                level1.setVisibility(VISIBLE);
                level2.setVisibility(VISIBLE);
                level3.setVisibility(VISIBLE);
                break;
            case AVERAGE:
                level1.setBackgroundColor(getResources().getColor(R.color.yellow200));
                level2.setBackgroundColor(getResources().getColor(R.color.yellow200));
                level1.setVisibility(VISIBLE);
                level2.setVisibility(VISIBLE);
                level3.setVisibility(INVISIBLE);
                break;
            case CRITICAL:
                level1.setBackgroundColor(getResources().getColor(R.color.red200));
                level1.setVisibility(VISIBLE);
                level2.setVisibility(INVISIBLE);
                level3.setVisibility(INVISIBLE);
                break;
            default:
                level1.setVisibility(INVISIBLE);
                level2.setVisibility(INVISIBLE);
                level3.setVisibility(INVISIBLE);
                break;
        }
    }
}
