package se.leap.bitmaskclient.base.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import de.blinkt.openvpn.core.connection.Connection;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.models.Location;
import se.leap.bitmaskclient.eip.GatewaysManager.Load;

public class SelectLocationEntry extends RelativeLayout {

    private static final String TAG = SelectLocationEntry.class.getSimpleName();
    AppCompatTextView title;
    AppCompatTextView locationText;
    SimpleCheckBox selectedView;
    AppCompatImageView bridgesView;
    LocationIndicator locationIndicator;
    View divider;

  //  private OnClickListener onClickListener;

    public SelectLocationEntry(Context context) {
        super(context);
        initLayout(context);
    }

    public SelectLocationEntry(Context context, AttributeSet attrs) {
        super(context, attrs);
        initLayout(context);
    }

    public SelectLocationEntry(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initLayout(context);
    }

     @TargetApi(21)
    public SelectLocationEntry(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initLayout(context);
    }

    private void initLayout(Context context) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootview = inflater.inflate(R.layout.v_select_text_list_item, this, true);
        title = rootview.findViewById(R.id.title);
        locationIndicator = rootview.findViewById(R.id.quality);
        locationText = rootview.findViewById(R.id.location);
        bridgesView = rootview.findViewById(R.id.bridge_image);
        selectedView = rootview.findViewById(R.id.selected);
        divider = rootview.findViewById(R.id.divider);
    }

    public void setTitle(String text) {
        title.setText(text);
        title.setVisibility(text != null ? VISIBLE : GONE);
    }
    public void setLocation(Location location) {
        boolean valid = location.hasLocationInfo();
        locationText.setVisibility(valid ? VISIBLE : GONE);
        locationIndicator.setVisibility(valid ? VISIBLE : GONE);
        bridgesView.setVisibility(valid ? VISIBLE : GONE);
        locationText.setText(location.name);
        locationIndicator.setLoad(Load.getLoadByValue(location.averageLoad));
        bridgesView.setVisibility(location.supportedTransports.contains(Connection.TransportType.OBFS4) ? VISIBLE : GONE);
        selectedView.setChecked(location.selected);
    }

    public void showDivider(boolean show) {
        divider.setVisibility(show ? VISIBLE : GONE);
    }

    public void setSelected(boolean selected) {
        selectedView.setChecked(selected);
    }

    public boolean isSelected() {
        return selectedView.checkView.getVisibility() == VISIBLE;
    }

}
