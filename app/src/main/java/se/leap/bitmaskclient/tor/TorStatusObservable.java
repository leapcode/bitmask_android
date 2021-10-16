package se.leap.bitmaskclient.tor;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Observable;
import java.util.Vector;

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
    private int bootstrapPercent = -1;
    private Vector<String> lastLogs = new Vector<>(100);

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

    public static void logMessage(Context context, String tag, String message) {
        Log.d(tag, message);
        addLog(message);
        if (getInstance().status != TorStatus.OFF) {
            getInstance().torNotificationManager.buildTorNotification(context, getStringForCurrentStatus(context), message, getInstance().bootstrapPercent);
        }
        instance.setChanged();
        instance.notifyObservers();
    }

    private static void addLog(String message) {
        if (instance.lastLogs.size() > 100) {
            instance.lastLogs.remove(0);
        }
        instance.lastLogs.add(message);
    }

    public static void updateState(Context context, String status) {
        updateState(context,status, -1, null);
    }

    public static void updateState(Context context, String status, int bootstrapPercent, @Nullable String logKey) {
        try {
            Log.d(TAG, "update tor state: " + status + " " + bootstrapPercent + " "+ logKey);
            getInstance().status = TorStatus.valueOf(status);
            if (bootstrapPercent != -1) {
                getInstance().bootstrapPercent = bootstrapPercent;
            }
            int progress = getInstance().status == TorStatus.STARTING ? getInstance().bootstrapPercent : -1;


            if (getInstance().status == TorStatus.OFF) {
                getInstance().torNotificationManager.cancelNotifications(context);
            } else if (logKey != null) {
                String log = getStringFor(context, logKey);
                addLog(log);
                getInstance().torNotificationManager.buildTorNotification(context, getStringForCurrentStatus(context), log, progress);
            } else {
                String log = instance.lastLogs.size() > 0 ? instance.lastLogs.lastElement() : "";
                getInstance().torNotificationManager.buildTorNotification(context, getStringForCurrentStatus(context), log, progress);
            }

            instance.setChanged();
            instance.notifyObservers();

        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private static String getStringFor(Context context, String key) {
        switch (key) {
            case "conn_pt":
                return context.getString(R.string.log_conn_pt);
            case "conn_done_pt":
                return context.getString(R.string.log_conn_done_pt);
            case "conn_done":
                return context.getString(R.string.log_conn_done);
            case "handshake":
                return context.getString(R.string.log_handshake);
            case "handshake_done":
                return context.getString(R.string.log_handshake_done);
            case "onehop_create":
                return context.getString(R.string.log_onehop_create);
            case "requesting_status":
                return context.getString(R.string.log_requesting_status);
            case "loading_status":
                return context.getString(R.string.log_loading_status);
            case "loading_keys":
                return context.getString(R.string.log_loading_keys);
            case "requesting_descriptors":
                return context.getString(R.string.log_requesting_desccriptors);
            case "loading_descriptors":
                return context.getString(R.string.log_loading_descriptors);
            case "enough_dirinfo":
                return context.getString(R.string.log_enough_dirinfo);
            case "ap_handshake_done":
                return context.getString(R.string.log_ap_handshake_done);
            case "circuit_create":
                return context.getString(R.string.log_circuit_create);
            case "done":
                return context.getString(R.string.log_done);
            default:
                return key;
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

    public static String getStringForCurrentStatus(Context context) {
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
