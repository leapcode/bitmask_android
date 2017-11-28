package se.leap.bitmaskclient;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import de.blinkt.openvpn.core.VpnStatus;
import se.leap.bitmaskclient.userstatus.User;

/**
 * Activity shown at startup. Evaluates if App is started for the first time or has been upgraded
 * and acts and calls another activity accordingly.
 *
 */
public class StartActivity extends Activity {
    public static final String TAG = Dashboard.class.getSimpleName();

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FIRST, NORMAL, UPGRADE, DOWNGRADE})
    private @interface StartupMode {}
    private static final int FIRST = 0;
    private static final int NORMAL = 1;
    private static final int UPGRADE = 2;
    private static final int DOWNGRADE = 3;

    /**
     *  check if normal start, first run, up or downgrade
     *  @return @StartupMode
     */
    @StartupMode
    private int checkAppStart() {
        SharedPreferences preferences = getSharedPreferences(Constants.SHARED_PREFERENCES, MODE_PRIVATE);
        try {
            int versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            int lastDetectedVersion = preferences.getInt(Constants.PREFERENCES_APP_VERSION, -1);

            // versions do match -> normal start
            if (versionCode == lastDetectedVersion) {
                Log.d(TAG, "App start was: NORMAL START");
                return NORMAL;
            }

            // something changed -> save current version
            preferences.edit().putInt(Constants.PREFERENCES_APP_VERSION, versionCode).apply();

            // no previous app version -> first start
            if (lastDetectedVersion == -1 ) {
                Log.d(TAG, "App start was: FIRST START");
                return FIRST;
            }

            // version has increased -> upgrade
            if (versionCode > lastDetectedVersion) {
                Log.d(TAG, "App start was: UPGRADE");
                return UPGRADE;
            }
            // version has decreased -> downgrade
            if (versionCode < lastDetectedVersion) {
                Log.d(TAG, "App start was: DOWNGRADE");
                return DOWNGRADE;
            }

        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "Splash screen didn't find any " + getPackageName() + " package");
        }

        return NORMAL;
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent;

        Log.d(TAG, "Started");

        switch (checkAppStart()) {
            case NORMAL:
                break;

            case FIRST:
                // TODO start ProfileCreation & replace below code
                intent = new Intent(this, Dashboard.class);
                startActivity(intent);
                break;

            case UPGRADE:
                // TODO appropriate data copying
                // TODO show donation dialog
                break;

            case DOWNGRADE:
                // TODO think how and why this should happen and what todo
                break;
        }

        // initialize app necessities
        ProviderAPICommand.initialize(this);
        VpnStatus.initLogCache(getApplicationContext().getCacheDir());
        User.init(getString(R.string.default_username));

        // go to Dashboard
        intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
