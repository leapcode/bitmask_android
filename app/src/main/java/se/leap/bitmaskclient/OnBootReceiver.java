package se.leap.bitmaskclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import static android.content.Intent.ACTION_BOOT_COMPLETED;
import static se.leap.bitmaskclient.Constants.EIP_IS_ALWAYS_ON;
import static se.leap.bitmaskclient.Constants.EIP_RESTART_ON_BOOT;
import static se.leap.bitmaskclient.Constants.PROVIDER_VPN_CERTIFICATE;

public class OnBootReceiver extends BroadcastReceiver {

    SharedPreferences preferences;

    // Debug: am broadcast -a android.intent.action.BOOT_COMPLETED
    @Override
    public void onReceive(Context context, Intent intent) {
        //Lint complains if we're not checking the intent action
        if (intent == null || !ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        preferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES, Context.MODE_PRIVATE);
        boolean providerConfigured = !preferences.getString(PROVIDER_VPN_CERTIFICATE, "").isEmpty();
        boolean startOnBoot = preferences.getBoolean(EIP_RESTART_ON_BOOT, false);
        boolean isAlwaysOnConfigured = preferences.getBoolean(EIP_IS_ALWAYS_ON, false);
        Log.d("OpenVPN", "OpenVPN onBoot intent received. Provider configured? " + providerConfigured + "  Start on boot? " + startOnBoot + "  isAlwaysOn feature configured: " + isAlwaysOnConfigured);
        if (providerConfigured) {
            if (isAlwaysOnConfigured) {
                //exit because the app is already setting up the vpn
                return;
            }
            if (startOnBoot) {
                Intent dashboard_intent = new Intent(context, Dashboard.class);
                dashboard_intent.putExtra(EIP_RESTART_ON_BOOT, true);
                dashboard_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(dashboard_intent);
            }
        } else {
            if (isAlwaysOnConfigured) {
                Intent dashboard_intent = new Intent(context, Dashboard.class);
                dashboard_intent.putExtra(Constants.APP_ACTION_CONFIGURE_ALWAYS_ON_PROFILE, true);
                dashboard_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(dashboard_intent);
            }
        }
    }
}
