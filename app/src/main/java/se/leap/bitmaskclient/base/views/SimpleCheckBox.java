package se.leap.bitmaskclient.base.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.appcompat.widget.AppCompatImageView;

import se.leap.bitmaskclient.R;

public class SimpleCheckBox extends RelativeLayout {

    AppCompatImageView checkView;


    public SimpleCheckBox(Context context) {
        super(context);
        initLayout(context);
    }

    public SimpleCheckBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        initLayout(context);
    }

    public SimpleCheckBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initLayout(context);
    }

    @TargetApi(21)
    public SimpleCheckBox(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initLayout(context);
    }

    private void initLayout(Context context) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootview = inflater.inflate(R.layout.v_simple_checkbox, this, true);
        this.checkView = rootview.findViewById(R.id.check_view);
    }

    public void setChecked(boolean checked) {
        this.checkView.setVisibility(checked ? VISIBLE : INVISIBLE);
    }
}
