package se.leap.bitmaskclient;

import android.content.*;
import android.os.*;

import org.jetbrains.annotations.*;

public class ProviderAPICommand {
    private static Context context;

    private static String action;
    private static Bundle parameters;
    private static ResultReceiver result_receiver;

    public static void initialize(Context context) {
        ProviderAPICommand.context = context;
    }

    private static boolean isInitialized() {
        return context != null;
    }

    public static void execute(Bundle parameters, @NotNull String action, @NotNull ResultReceiver result_receiver) throws IllegalStateException {
        if(!isInitialized()) throw new IllegalStateException();

        ProviderAPICommand.action = action;
        ProviderAPICommand.parameters = parameters;
        ProviderAPICommand.result_receiver = result_receiver;

        Intent intent = setUpIntent();
        context.startService(intent);
    }

    private static Intent setUpIntent() {
        Intent command = new Intent(context, ProviderAPI.class);

        command.setAction(action);
        command.putExtra(ProviderAPI.PARAMETERS, parameters);
        command.putExtra(ProviderAPI.RECEIVER_KEY, result_receiver);

        return command;
    }


}
