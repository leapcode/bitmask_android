package se.leap.bitmaskclient.eip;

import android.content.Context;
import android.content.Intent;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static se.leap.bitmaskclient.Constants.EIP_ACTION_CHECK_CERT_VALIDITY;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_START;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_STOP;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_UPDATE;
import static se.leap.bitmaskclient.Constants.EIP_RECEIVER;

/**
 * Use this class to send commands to EIP
 */

public class EipCommand {

    public static void execute(@NotNull Context context, @NotNull String action) {
        execute(context, action, null);
    }

    /**
     * Send a command to EIP
     * @param context the context to start the command from
     * @param action A valid String constant from EIP class representing an Intent
     *               filter for the EIP class
     * @param resultReceiver The resultreceiver to reply to
     */
    public static void execute(@NotNull Context context, @NotNull String action, @Nullable ResultReceiver resultReceiver) {
        // TODO validate "action"...how do we get the list of intent-filters for a class via Android API?
        Intent vpnIntent = new Intent(context.getApplicationContext(), EIP.class);
        vpnIntent.setAction(action);
        if (resultReceiver != null)
            vpnIntent.putExtra(EIP_RECEIVER, resultReceiver);
        context.startService(vpnIntent);
    }

    public static void updateEipService(@NonNull Context context, ResultReceiver resultReceiver) {
        execute(context, EIP_ACTION_UPDATE, resultReceiver);
    }

    public static void updateEipService(@NonNull Context context) {
        execute(context, EIP_ACTION_UPDATE);
    }

    public static void startVPN(@NonNull Context context) {
        execute(context, EIP_ACTION_START);
    }

    public static void startVPN(@NonNull Context context, ResultReceiver resultReceiver) {
        execute(context, EIP_ACTION_START, resultReceiver);
    }

    public static void stopVPN(@NonNull Context context) {
        execute(context, EIP_ACTION_STOP);
    }

    public static void stopVPN(@NonNull Context context, ResultReceiver resultReceiver) {
        execute(context, EIP_ACTION_STOP, resultReceiver);
    }

    public static void checkVpnCertificate(@NonNull Context context) {
        execute(context, EIP_ACTION_CHECK_CERT_VALIDITY);
    }

    public static void checkVpnCertificate(@NonNull Context context, ResultReceiver resultReceiver) {
        execute(context, EIP_ACTION_CHECK_CERT_VALIDITY, resultReceiver);
    }

}
