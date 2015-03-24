package se.leap.bitmaskclient;

import android.content.*;
import android.util.*;

import se.leap.bitmaskclient.eip.*;

public class OnBootReceiver extends BroadcastReceiver {

    SharedPreferences preferences;

    // Debug: am broadcast -a android.intent.action.BOOT_COMPLETED
    @Override
    public void onReceive(Context context, Intent intent) {
        preferences = context.getSharedPreferences(Dashboard.SHARED_PREFERENCES, Context.MODE_PRIVATE);
        boolean provider_configured = !preferences.getString(Provider.KEY, "").isEmpty();
        boolean start_on_boot = preferences.getBoolean(Dashboard.START_ON_BOOT, false);
        Log.d("OnBootReceiver", "Provider configured " + String.valueOf(provider_configured));
        Log.d("OnBootReceiver", "Start on boot " + String.valueOf(start_on_boot));
        if (provider_configured && start_on_boot) {
            Intent dashboard_intent = new Intent(context, Dashboard.class);
            dashboard_intent.setAction(Constants.ACTION_START_EIP);
            dashboard_intent.putExtra(Dashboard.ON_BOOT, true);
            dashboard_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(dashboard_intent);
        }
    }
}
