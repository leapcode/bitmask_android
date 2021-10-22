package se.leap.bitmaskclient.tor;

import android.content.Context;
import android.util.Log;

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

    public static final String LOG_TAG_TOR = "[TOR]";
    public static final String LOG_TAG_SNOWFLAKE = "[SNOWFLAKE]";

    private static TorStatusObservable instance;
    private TorStatus status = TorStatus.UNKOWN;
    private final TorNotificationManager torNotificationManager;
    private String lastError;
    private String lastTorLog;
    private String lastSnowflakeLog;
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

    public static void logSnowflakeMessage(Context context, String message) {
        Log.d(LOG_TAG_SNOWFLAKE, message);
        addLog(message);
        getInstance().lastSnowflakeLog = message;
        if (getInstance().status != TorStatus.OFF) {
            getInstance().torNotificationManager.buildTorNotification(context, getStringForCurrentStatus(context), getNotificationLog(), getBootstrapProgress());
        }
        instance.setChanged();
        instance.notifyObservers();
    }

    private static String getNotificationLog() {
        String snowflakeIcon = new String(Character.toChars(0x2744));
        String snowflakeLog = getInstance().lastSnowflakeLog;
        // we don't want to show the response json in the notification
        if (snowflakeLog != null && snowflakeLog.contains("Received answer: {")) {
            snowflakeLog = "Received Answer.";
        }
        return "Tor: " + getInstance().lastTorLog + "\n" +
                snowflakeIcon + ": " + snowflakeLog;
    }

    public static int getBootstrapProgress() {
        return getInstance().status == TorStatus.STARTING ? getInstance().bootstrapPercent : -1;
    }

    private static void addLog(String message) {
        if (instance.lastLogs.size() > 100) {
            instance.lastLogs.remove(99);
        }
        instance.lastLogs.add(0, message.trim());
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

            if (getInstance().status == TorStatus.OFF) {
                getInstance().torNotificationManager.cancelNotifications(context);
            } else {
                if (logKey != null) {
                    getInstance().lastTorLog = getStringFor(context, logKey);
                    addLog(getInstance().lastTorLog);
                }
                getInstance().torNotificationManager.buildTorNotification(context, getStringForCurrentStatus(context), getNotificationLog(), getBootstrapProgress());
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
    public static String getLastTorLog() {
        return getInstance().lastTorLog;
    }

    @Nullable
    public static String getLastSnowflakeLog() {
        return getInstance().lastSnowflakeLog;
    }

    public static Vector<String> getLastLogs() {
        return getInstance().lastLogs;
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
