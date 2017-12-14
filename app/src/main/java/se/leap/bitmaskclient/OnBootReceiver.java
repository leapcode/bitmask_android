package se.leap.bitmaskclient;

import android.content.*;


public class OnBootReceiver extends BroadcastReceiver {

    SharedPreferences preferences;

    // Debug: am broadcast -a android.intent.action.BOOT_COMPLETED
    @Override
    public void onReceive(Context context, Intent intent) {
        preferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES, Context.MODE_PRIVATE);
        boolean provider_configured = !preferences.getString(Provider.KEY, "").isEmpty();
        boolean start_on_boot = preferences.getBoolean(Dashboard.START_ON_BOOT, false);
        if (provider_configured && start_on_boot) {
            Intent dashboard_intent = new Intent(context, Dashboard.class);
            dashboard_intent.setAction(Constants.EIP_ACTION_START);
            dashboard_intent.putExtra(Dashboard.ON_BOOT, true);
            dashboard_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(dashboard_intent);
        }
    }
}
