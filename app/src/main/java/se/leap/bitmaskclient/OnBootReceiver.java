package se.leap.bitmaskclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import static se.leap.bitmaskclient.eip.Constants.IS_ALWAYS_ON;
import static se.leap.bitmaskclient.eip.Constants.RESTART_ON_BOOT;
import static se.leap.bitmaskclient.eip.Constants.VPN_CERTIFICATE;

public class OnBootReceiver extends BroadcastReceiver {

    SharedPreferences preferences;


    // Debug: su && am broadcast -a android.intent.action.BOOT_COMPLETED
    @Override
    public void onReceive(Context context, Intent intent) {
        preferences = context.getSharedPreferences(Dashboard.SHARED_PREFERENCES, Context.MODE_PRIVATE);
        boolean provider_configured = !preferences.getString(VPN_CERTIFICATE, "").isEmpty();
        boolean start_on_boot = preferences.getBoolean(RESTART_ON_BOOT, false);
        boolean isAlwaysOnConfigured = preferences.getBoolean(IS_ALWAYS_ON, false);
        Log.d("OpenVPN", "OpenVPN onBoot intent received. Provider configured? " + provider_configured + "  Start on boot? " + start_on_boot + "  isAlwaysOn feature configured: " + isAlwaysOnConfigured);
        if (provider_configured) {
            if (isAlwaysOnConfigured) {
                //exit because the app is already setting up the vpn
                return;
            }
            if (start_on_boot) {
                Intent dashboard_intent = new Intent(context, Dashboard.class);
                dashboard_intent.putExtra(RESTART_ON_BOOT, true);
                dashboard_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(dashboard_intent);
            }
        } else {
            if (isAlwaysOnConfigured) {
                Intent dashboard_intent = new Intent(context, Dashboard.class);
                dashboard_intent.putExtra(Dashboard.ACTION_CONFIGURE_ALWAYS_ON_PROFILE, true);
                dashboard_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(dashboard_intent);
            }
        }
    }
}
