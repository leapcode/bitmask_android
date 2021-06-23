package se.leap.bitmaskclient.tor;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Observable;

import se.leap.bitmaskclient.R;

public class TorStatusObservable extends Observable {

    private static final String TAG = TorStatusObservable.class.getSimpleName();

    public enum TorStatus {
        ON,
        OFF,
        STARTING,
        STOPPING,
        UNKOWN
    }

    private static TorStatusObservable instance;
    private TorStatus status = TorStatus.UNKOWN;
    private final TorNotificationManager torNotificationManager;
    private String lastError;
    private int port = -1;

    private TorStatusObservable() {
        torNotificationManager = new TorNotificationManager();
    }

    public static TorStatusObservable getInstance() {
        if (instance == null) {
            instance = new TorStatusObservable();
        }
        return instance;
    }

    public static TorStatus getStatus() {
        return getInstance().status;
    }


    public static void updateState(Context context, String status) {
        try {
            Log.d(TAG, "update tor state: " + status);
            getInstance().status = TorStatus.valueOf(status);
            if (getInstance().status == TorStatus.OFF) {
                getInstance().torNotificationManager.cancelNotifications(context);
            } else {
                getInstance().torNotificationManager.buildTorNotification(context, getStringForCurrentStatus(context));
            }
            instance.setChanged();
            instance.notifyObservers();


        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public static void setLastError(String error) {
        getInstance().lastError = error;
        instance.setChanged();
        instance.notifyObservers();
    }

    public static void setProxyPort(int port) {
        getInstance().port = port;
        instance.setChanged();
        instance.notifyObservers();
    }

    public static int getProxyPort() {
        return getInstance().port;
    }


    @Nullable
    public String getLastError() {
        return lastError;
    }

    private static String getStringForCurrentStatus(Context context) {
        switch (getInstance().status) {
            case ON:
                return context.getString(R.string.tor_started);
            case STARTING:
                return context.getString(R.string.tor_starting);
            case STOPPING:
                return context.getString(R.string.tor_stopping);
            case OFF:
            case UNKOWN:
                break;
        }
        return null;
    }
}
