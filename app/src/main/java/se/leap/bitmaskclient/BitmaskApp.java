package se.leap.bitmaskclient;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.multidex.MultiDexApplication;

import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import static se.leap.bitmaskclient.Constants.SHARED_PREFERENCES;
import static se.leap.bitmaskclient.utils.PreferenceHelper.getSavedProviderFromSharedPreferences;

/**
 * Created by cyberta on 24.10.17.
 */

public class BitmaskApp extends MultiDexApplication {

    private final static String TAG = BitmaskApp.class.getSimpleName();
    private RefWatcher refWatcher;
    private ProviderObservable providerObservable;


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
        SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        providerObservable = ProviderObservable.getInstance();
        providerObservable.updateProvider(getSavedProviderFromSharedPreferences(preferences));
        EipSetupObserver.init(this, preferences);
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
