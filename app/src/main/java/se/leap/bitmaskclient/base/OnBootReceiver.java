package se.leap.bitmaskclient.base;

import static android.content.Intent.ACTION_BOOT_COMPLETED;
import static se.leap.bitmaskclient.base.models.Constants.APP_ACTION_CONFIGURE_ALWAYS_ON_PROFILE;
import static se.leap.bitmaskclient.base.models.Constants.EIP_RESTART_ON_BOOT;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import de.blinkt.openvpn.core.VpnStatus;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;

public class OnBootReceiver extends BroadcastReceiver {

    // Debug: am broadcast -a android.intent.action.BOOT_COMPLETED
    @Override
    public void onReceive(Context context, Intent intent) {
        //Lint complains if we're not checking the intent action
        if (intent == null || !ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        boolean providerConfigured = !PreferenceHelper.getProviderVPNCertificate().isEmpty();
        boolean startOnBoot = PreferenceHelper.getRestartOnBoot() && Build.VERSION.SDK_INT < Build.VERSION_CODES.O;
        boolean isAlwaysOnConfigured = VpnStatus.isAlwaysOn();
        Log.d("OpenVPN", "OpenVPN onBoot intent received. Provider configured? " + providerConfigured + "  Start on boot? " + startOnBoot + "  isAlwaysOn feature configured: " + isAlwaysOnConfigured);
        if (providerConfigured) {
            if (isAlwaysOnConfigured) {
                //exit because the app is already setting up the vpn
                return;
            }
            if (startOnBoot) {
                Log.d("OpenVpn", "start StartActivity!");
                Intent startActivityIntent = new Intent(context.getApplicationContext(), StartActivity.class);
                startActivityIntent.putExtra(EIP_RESTART_ON_BOOT, true);
                startActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(startActivityIntent);
            }
        } else {
            if (isAlwaysOnConfigured) {
                Intent dashboardIntent = new Intent(context.getApplicationContext(), StartActivity.class);
                dashboardIntent.putExtra(APP_ACTION_CONFIGURE_ALWAYS_ON_PROFILE, true);
                dashboardIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(dashboardIntent);
            }
        }
    }
}
