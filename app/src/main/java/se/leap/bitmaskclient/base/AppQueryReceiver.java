package se.leap.bitmaskclient.base;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import se.leap.bitmaskclient.base.utils.ApplicationInfoManager;

public class AppQueryReceiver extends BroadcastReceiver {

    private static final String TAG = AppQueryReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null ||
                intent.getAction() == null || intent.getData() == null ||
                !intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED) ||
                intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
            return;
        }

        String packageName = intent.getData().getEncodedSchemeSpecificPart();
        if (packageName != null) {
            ApplicationInfoManager appManager = new ApplicationInfoManager(context);
            appManager.onApplicationRemoved(packageName);
        }
    }

    public static void register(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        context.registerReceiver(new AppQueryReceiver(), filter);
    }
}