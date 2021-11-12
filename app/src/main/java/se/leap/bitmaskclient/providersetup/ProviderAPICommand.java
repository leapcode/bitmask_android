package se.leap.bitmaskclient.providersetup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import se.leap.bitmaskclient.base.models.Constants;
import se.leap.bitmaskclient.base.models.Provider;

public class ProviderAPICommand {
    private static final String TAG = ProviderAPICommand.class.getSimpleName();
    private final Context context;

    private final String action;
    private final Bundle parameters;
    private final ResultReceiver resultReceiver;
    private final Provider provider;

    private ProviderAPICommand(@NonNull Context context, @NonNull String action, @NonNull Provider provider, ResultReceiver resultReceiver) {
        this(context.getApplicationContext(), action, Bundle.EMPTY, provider, resultReceiver);
    }
    private ProviderAPICommand(@NonNull Context context, @NonNull String action, @NonNull Provider provider) {
        this(context.getApplicationContext(), action, Bundle.EMPTY, provider);
    }

    private ProviderAPICommand(@NonNull Context context, @NonNull String action, @NonNull Bundle parameters, @NonNull Provider provider) {
        this(context.getApplicationContext(), action, parameters, provider, null);
    }

    private ProviderAPICommand(@NonNull Context context, @NonNull String action, @NonNull Bundle parameters, @NonNull Provider provider, @Nullable ResultReceiver resultReceiver) {
        super();
        this.context = context;
        this.action = action;
        this.parameters = parameters;
        this.resultReceiver = resultReceiver;
        this.provider = provider;
    }

    private boolean isInitialized() {
        return context != null;
    }

    private void execute() {
        if (isInitialized()) {
            Intent intent = setUpIntent();
            ProviderAPI.enqueueWork(context, intent);
        }
    }

    private Intent setUpIntent() {
        Intent command = new Intent(context, ProviderAPI.class);

        command.setAction(action);
        command.putExtra(ProviderAPI.PARAMETERS, parameters);
        if (resultReceiver != null) {
            command.putExtra(ProviderAPI.RECEIVER_KEY, resultReceiver);
        }
        command.putExtra(Constants.PROVIDER_KEY, provider);

        return command;
    }

    public static void execute(Context context, String action, Provider provider) {
        ProviderAPICommand command = new ProviderAPICommand(context, action, provider);
        command.execute();
    }

    public static void execute(Context context, String action, Bundle parameters, Provider provider) {
        ProviderAPICommand command = new ProviderAPICommand(context, action, parameters, provider);
        command.execute();
    }

    public static void execute(Context context, String action, Bundle parameters, Provider provider, ResultReceiver resultReceiver) {
        ProviderAPICommand command = new ProviderAPICommand(context, action, parameters, provider, resultReceiver);
        command.execute();
    }

    public static void execute(Context context, String action, Provider provider, ResultReceiver resultReceiver) {
        ProviderAPICommand command = new ProviderAPICommand(context, action, provider, resultReceiver);
        command.execute();
    }
}
