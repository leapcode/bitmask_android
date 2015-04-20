package se.leap.bitmaskclient;

import android.content.*;

import de.blinkt.openvpn.activities.*;

public class LogWindowWrapper {
    private static LogWindowWrapper instance;

    private static String TAG = LogWindowWrapper.class.getName();
    private Context context;

    public LogWindowWrapper(Context context) {
        this.context = context;
    }

    public void showLog() {
        Intent startLW = new Intent(context, LogWindow.class);
        startLW.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(startLW);
    }

    public static LogWindowWrapper getInstance(Context context) {
        if(instance == null)
            instance = new LogWindowWrapper(context);
        return instance;
    }
}
