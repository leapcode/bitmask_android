package se.leap.bitmaskclient.eip;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static se.leap.bitmaskclient.Constants.EIP_ACTION_CHECK_CERT_VALIDITY;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_START;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_STOP;
import static se.leap.bitmaskclient.Constants.EIP_EARLY_ROUTES;
import static se.leap.bitmaskclient.Constants.EIP_RECEIVER;

/**
 * Use this class to send commands to EIP
 */

public class EipCommand {

    public static void execute(@NotNull Context context, @NotNull String action) {
        execute(context, action, null, null);
    }

    /**
     * Send a command to EIP
     * @param context the context to start the command from
     * @param action A valid String constant from EIP class representing an Intent
     *               filter for the EIP class
     * @param resultReceiver The resultreceiver to reply to
     */
    public static void execute(@NotNull Context context, @NotNull String action, @Nullable ResultReceiver resultReceiver, @Nullable Intent vpnIntent) {
        // TODO validate "action"...how do we get the list of intent-filters for a class via Android API?
        if (vpnIntent == null) {
            vpnIntent = new Intent();
        }
        vpnIntent.setComponent(new ComponentName(context.getApplicationContext(), EIP.class));
        vpnIntent.setAction(action);
        if (resultReceiver != null)
            vpnIntent.putExtra(EIP_RECEIVER, resultReceiver);
        context.startService(vpnIntent);
    }

    public static void startVPN(@NonNull Context context, boolean earlyRoutes) {
        Intent baseIntent = new Intent();
        baseIntent.putExtra(EIP_EARLY_ROUTES, earlyRoutes);
        execute(context, EIP_ACTION_START, null, baseIntent);
    }

    @VisibleForTesting
    public static void startVPN(@NonNull Context context, ResultReceiver resultReceiver) {
        execute(context, EIP_ACTION_START, resultReceiver, null);
    }

    public static void stopVPN(@NonNull Context context) {
        execute(context, EIP_ACTION_STOP);
    }

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
