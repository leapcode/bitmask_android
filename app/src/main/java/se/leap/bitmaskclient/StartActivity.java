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

    private int versionCode;
    private int previousVersionCode;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(Constants.SHARED_PREFERENCES, MODE_PRIVATE);

        Log.d(TAG, "Started");

        switch (checkAppStart()) {
            case NORMAL:
                break;

            case FIRST:
                storeAppVersion();
                // TODO start ProfileCreation & replace below code
                break;

            case UPGRADE:
                executeUpgrade();
                // TODO show donation dialog
                break;

            case DOWNGRADE:
                // TODO think how and why this should happen and what todo
                break;
        }

        // initialize app necessities
        ProviderAPICommand.initialize(getApplicationContext());
        VpnStatus.initLogCache(getApplicationContext().getCacheDir());
        User.init(getString(R.string.default_username));

        // go to Dashboard
        Intent intent = new Intent(this, Dashboard.class);
        startActivity(intent);
    }

    /**
     *  check if normal start, first run, up or downgrade
     *  @return @StartupMode
     */
    @StartupMode
    private int checkAppStart() {
        try {
            versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            previousVersionCode = preferences.getInt(Constants.PREFERENCES_APP_VERSION, -1);

            // versions do match -> normal start
            if (versionCode == previousVersionCode) {
                Log.d(TAG, "App start was: NORMAL START");
                return NORMAL;
            }

            // no previous app version -> first start
            if (previousVersionCode == -1 ) {
                Log.d(TAG, "FIRST START");
                return FIRST;
            }

            // version has increased -> upgrade
            if (versionCode > previousVersionCode) {
                Log.d(TAG, "UPGRADE");
                return UPGRADE;
            }
            // version has decreased -> downgrade
            if (versionCode < previousVersionCode) {
                Log.d(TAG, "DOWNGRADE");
                return DOWNGRADE;
            }

        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "Splash screen didn't find any " + getPackageName() + " package");
        }

        return NORMAL;
    }

    /**
     * execute necessary upgrades for version change
     */
    private void executeUpgrade() {
        if (hasNewFeature(FeatureVersionCode.MULTIPLE_PROFILES)) {
            // TODO prepare usage of multiple profiles
        }

        // ensure all upgrades have passed before storing new information
        storeAppVersion();
    }

    /**
     * check if an upgrade passed or moved to given milestone
     * @param featureVersionCode Version code of the Milestone FeatureVersionCode.MILE_STONE
     * @return true if milestone is reached - false otherwise
     */
    private boolean hasNewFeature(int featureVersionCode) {
        return previousVersionCode < featureVersionCode && versionCode >= featureVersionCode;
    }

    private void storeAppVersion() {
        preferences.edit().putInt(Constants.PREFERENCES_APP_VERSION, versionCode).apply();
    }

}
