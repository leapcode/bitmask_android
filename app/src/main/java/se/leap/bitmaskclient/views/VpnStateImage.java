package se.leap.bitmaskclient.views;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;

import se.leap.bitmaskclient.R;

/**
 * Created by cyberta on 12.02.18.
 */


public class VpnStateImage extends ConstraintLayout {

    ProgressBar progressBar;
    AppCompatImageView stateIcon;

    public VpnStateImage(Context context) {
        super(context);
        initLayout(context);
    }

    public VpnStateImage(Context context, AttributeSet attrs) {
        super(context, attrs);
        initLayout(context);
    }

    public VpnStateImage(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initLayout(context);
    }

    void initLayout(Context context) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootview = inflater.inflate(R.layout.v_main_button, this, true);
        stateIcon = rootview.findViewById(R.id.vpn_state_key);
        progressBar = rootview.findViewById(R.id.progressBar);
        progressBar.setIndeterminate(true);
    }

    public void showProgress() {
        progressBar.setVisibility(VISIBLE);
    }


    public void stopProgress() {
        progressBar.setVisibility(GONE);
  }

    public void setStateIcon(int resource) {
        stateIcon.setImageResource(resource);
    }


}
