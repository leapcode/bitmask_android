package se.leap.bitmaskclient.base;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Observable;
import java.util.Observer;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.eip.EipCommand;
import se.leap.bitmaskclient.eip.EipStatus;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.models.ProviderObservable;


@TargetApi(Build.VERSION_CODES.N)
public class BitmaskTileService extends TileService implements PropertyChangeListener {
    
    @SuppressLint("Override")
    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public void onClick() {
        super.onClick();
        Provider provider = ProviderObservable.getInstance().getCurrentProvider();
        if (provider.isConfigured()) {
            if (!isLocked()) {
                onTileTap();
            } else {
                unlockAndRun(this::onTileTap);
            }
        } else {
            Intent intent = new Intent(getApplicationContext(), StartActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    private void onTileTap() {
        EipStatus eipStatus = EipStatus.getInstance();
        if (eipStatus.isConnecting() || eipStatus.isBlocking() || eipStatus.isConnected() || eipStatus.isReconnecting()) {
            EipCommand.stopVPN(this);
        } else {
            EipCommand.startVPN(this, false);
        }
    }


    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public void onTileAdded() {
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        EipStatus.getInstance().addObserver(this);
        propertyChange(new PropertyChangeEvent(EipStatus.getInstance(), EipStatus.PROPERTY_CHANGE, null, EipStatus.getInstance()));
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        EipStatus.getInstance().deleteObserver(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        Tile t = getQsTile();
        // Tile t should never be null according to https://developer.android.com/reference/kotlin/android/service/quicksettings/TileService.
        // Hovever we've got crash reports.
        if (t == null) {
            return;
        }
        if (EipStatus.PROPERTY_CHANGE.equals(evt.getPropertyName())) {
            EipStatus status = (EipStatus) evt.getNewValue();
            Icon icon;
            String title;
            if (status.isConnecting() || status.isReconnecting()) {
                icon = Icon.createWithResource(getApplicationContext(), R.drawable.vpn_connecting);
                title = getResources().getString(R.string.cancel);
                t.setState(Tile.STATE_ACTIVE);
            } else if (status.isConnected()) {
                icon = Icon.createWithResource(getApplicationContext(), R.drawable.vpn_connected);
                title = String.format(getString(R.string.qs_disconnect), getString(R.string.app_name));
                t.setState(Tile.STATE_ACTIVE);
            } else if (status.isBlocking()) {
                icon = Icon.createWithResource(getApplicationContext(), R.drawable.vpn_blocking);
                title = getString(R.string.vpn_button_turn_off_blocking);
                t.setState(Tile.STATE_ACTIVE);
            } else {
                icon = Icon.createWithResource(getApplicationContext(), R.drawable.vpn_disconnected);
                title = String.format(getString(R.string.qs_enable_vpn), getString(R.string.app_name));
                t.setState(Tile.STATE_INACTIVE);
            }


            t.setIcon(icon);
            t.setLabel(title);

            t.updateTile();
        }
    }
}
