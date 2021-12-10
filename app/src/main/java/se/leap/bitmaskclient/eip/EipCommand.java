package se.leap.bitmaskclient.eip;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.ResultReceiver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import de.blinkt.openvpn.VpnProfile;

import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_CHECK_CERT_VALIDITY;
import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_LAUNCH_VPN;
import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_START;
import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_START_BLOCKING_VPN;
import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_STOP;
import static se.leap.bitmaskclient.base.models.Constants.EIP_EARLY_ROUTES;
import static se.leap.bitmaskclient.base.models.Constants.EIP_N_CLOSEST_GATEWAY;
import static se.leap.bitmaskclient.base.models.Constants.EIP_RECEIVER;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_PROFILE;

/**
 * Use this class to send commands to EIP
 */

public class EipCommand {

    private static void execute(@NonNull Context context, @NonNull String action) {
        execute(context, action, null, null);
    }

    /**
     * Send a command to EIP
     * @param context the context to start the command from
     * @param action A valid String constant from EIP class representing an Intent
     *               filter for the EIP class
     * @param resultReceiver The resultreceiver to reply to
     */
    private static void execute(@NonNull Context context, @NonNull String action, @Nullable ResultReceiver resultReceiver, @Nullable Intent vpnIntent) {
        if (vpnIntent == null) {
            vpnIntent = new Intent();
        }
        vpnIntent.setComponent(new ComponentName(context.getApplicationContext(), EIP.class));
        vpnIntent.setAction(action);
        if (resultReceiver != null)
            vpnIntent.putExtra(EIP_RECEIVER, resultReceiver);
        EIP.enqueueWork(context.getApplicationContext(), vpnIntent);
    }

    public static void startVPN(@NonNull Context context, boolean earlyRoutes) {
        Intent baseIntent = new Intent();
        baseIntent.putExtra(EIP_EARLY_ROUTES, earlyRoutes);
        baseIntent.putExtra(EIP_N_CLOSEST_GATEWAY, 0);
        execute(context, EIP_ACTION_START, null, baseIntent);
    }

    public static void startVPN(@NonNull Context context, boolean earlyRoutes, int nClosestGateway) {
        Intent baseIntent = new Intent();
        baseIntent.putExtra(EIP_EARLY_ROUTES, earlyRoutes);
        baseIntent.putExtra(EIP_N_CLOSEST_GATEWAY, nClosestGateway);
        execute(context, EIP_ACTION_START, null, baseIntent);
    }

    public static void startBlockingVPN(Context context) {
        execute(context, EIP_ACTION_START_BLOCKING_VPN);
    }

    @VisibleForTesting
    public static void startVPN(@NonNull Context context, ResultReceiver resultReceiver) {
        execute(context, EIP_ACTION_START, resultReceiver, null);
    }

    public static void stopVPN(@NonNull Context context) {
        execute(context, EIP_ACTION_STOP);
    }

    public static void launchVPNProfile(@NonNull Context context, VpnProfile vpnProfile, Integer closestGateway) {
        Intent baseIntent = new Intent();
        baseIntent.putExtra(PROVIDER_PROFILE, vpnProfile);
        baseIntent.putExtra(EIP_N_CLOSEST_GATEWAY, closestGateway);
        execute(context, EIP_ACTION_LAUNCH_VPN, null, baseIntent);
    }

    public static void launchVoidVPN(@NonNull Context context) { execute(context, EIP_ACTION_START_BLOCKING_VPN);}

    @VisibleForTesting
    public static void stopVPN(@NonNull Context context, ResultReceiver resultReceiver) {
        execute(context, EIP_ACTION_STOP, resultReceiver, null);
    }

    public static void checkVpnCertificate(@NonNull Context context) {
        execute(context, EIP_ACTION_CHECK_CERT_VALIDITY);
    }

    @VisibleForTesting
    public static void checkVpnCertificate(@NonNull Context context, ResultReceiver resultReceiver) {
        execute(context, EIP_ACTION_CHECK_CERT_VALIDITY, resultReceiver, null);
    }

}
