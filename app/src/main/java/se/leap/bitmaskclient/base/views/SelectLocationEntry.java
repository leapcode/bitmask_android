package se.leap.bitmaskclient.base.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import de.blinkt.openvpn.core.connection.Connection;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.models.Location;
import se.leap.bitmaskclient.eip.GatewaysManager.Load;

import static de.blinkt.openvpn.core.connection.Connection.TransportType.OBFS4;

public class SelectLocationEntry extends LinearLayout {

    private static final String TAG = SelectLocationEntry.class.getSimpleName();
    AppCompatTextView title;
    AppCompatTextView locationText;
    SimpleCheckBox selectedView;
    AppCompatImageView bridgesView;
    LocationIndicator locationIndicator;
    View divider;

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

    public void setLocation(Location location, Connection.TransportType transportType) {
        boolean hasData = location.hasLocationInfo();
        boolean supportsSelectedTransport = location.supportsTransport(transportType);
        locationText.setVisibility(hasData ? VISIBLE : GONE);
        locationIndicator.setVisibility(hasData ? VISIBLE : GONE);
        bridgesView.setVisibility(transportType.isPluggableTransport()  && supportsSelectedTransport ? VISIBLE : GONE);
        locationText.setText(location.getName());
        locationIndicator.setLoad(Load.getLoadByValue(location.getAverageLoad(transportType)));
        selectedView.setChecked(location.selected);
        locationText.setEnabled(supportsSelectedTransport);
        selectedView.setEnabled(supportsSelectedTransport);
        setEnabled(!hasData || supportsSelectedTransport);
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
