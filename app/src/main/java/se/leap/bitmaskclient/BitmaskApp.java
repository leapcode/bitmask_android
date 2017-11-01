package se.leap.bitmaskclient;

import android.app.Application;

/**
 * Created by cyberta on 24.10.17.
 */

public class BitmaskApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PRNGFixes.apply();
        //TODO: add LeakCanary!
    }
}
