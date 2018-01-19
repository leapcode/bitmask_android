package se.leap.bitmaskclient;

import android.app.Application;
import android.content.Context;

import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

/**
 * Created by cyberta on 24.10.17.
 */

public class BitmaskApp extends Application {

    private RefWatcher refWatcher;

    @Override
    public void onCreate() {
        super.onCreate();
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        refWatcher = LeakCanary.install(this);
        // Normal app init code...*/
        PRNGFixes.apply();
    }

    /**
     * Use this method to get a RefWatcher object that checks for memory leaks in the given context.
     * Call refWatcher.watch(this) to check if all references get garbage collected.
     * @param context
     * @return the RefWatcher object
     */
    public static RefWatcher getRefWatcher(Context context) {
        BitmaskApp application = (BitmaskApp) context.getApplicationContext();
        return application.refWatcher;
    }


}
