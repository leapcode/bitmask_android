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
import se.leap.bitmaskclient.eip.EIP;
import se.leap.bitmaskclient.eip.EipCommand;
import se.leap.bitmaskclient.userstatus.User;

import static se.leap.bitmaskclient.Constants.APP_ACTION_CONFIGURE_ALWAYS_ON_PROFILE;
import static se.leap.bitmaskclient.Constants.EIP_ACTION_START;
import static se.leap.bitmaskclient.Constants.EIP_RESTART_ON_BOOT;
import static se.leap.bitmaskclient.Constants.PREFERENCES_APP_VERSION;
import static se.leap.bitmaskclient.Constants.PROVIDER_EIP_DEFINITION;
import static se.leap.bitmaskclient.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.Constants.REQUEST_CODE_CONFIGURE_LEAP;
import static se.leap.bitmaskclient.Constants.SHARED_PREFERENCES;
import static se.leap.bitmaskclient.MainActivity.ACTION_SHOW_VPN_FRAGMENT;

/**
 * Activity shown at startup. Evaluates if App is started for the first time or has been upgraded
 * and acts and calls another activity accordingly.
 *
 */
public class StartActivity extends Activity {
    public static final String TAG = StartActivity.class.getSimpleName();

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
        preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);

        Log.d(TAG, "Started");

        switch (checkAppStart()) {
            case NORMAL:
                break;

            case FIRST:
                storeAppVersion();
                // TODO start ProfileCreation & replace below code
                // (new Intent(getActivity(), ProviderListActivity.class), Constants.REQUEST_CODE_SWITCH_PROVIDER);
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
        VpnStatus.initLogCache(getApplicationContext().getCacheDir());
        User.init(getString(R.string.default_username));

        prepareEIP();

    }

    /**
     *  check if normal start, first run, up or downgrade
     *  @return @StartupMode
     */
    @StartupMode
    private int checkAppStart() {
        try {
            versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            previousVersionCode = preferences.getInt(PREFERENCES_APP_VERSION, -1);

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
        if (hasNewFeature(FeatureVersionCode.RENAMED_EIP_IN_PREFERENCES)) {
            String eipJson = preferences.getString(PROVIDER_KEY, null);
            if (eipJson != null) {
                preferences.edit().putString(PROVIDER_EIP_DEFINITION, eipJson).
                        remove(PROVIDER_KEY).apply();
            }
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
        preferences.edit().putInt(PREFERENCES_APP_VERSION, versionCode).apply();
    }

    private void prepareEIP() {
        boolean provider_exists = ConfigHelper.providerInSharedPreferences(preferences);
        if (provider_exists) {
            Provider provider = ConfigHelper.getSavedProviderFromSharedPreferences(preferences);
            if(!provider.isConfigured()) {
                configureLeapProvider();
            } else {
                Log.d(TAG, "vpn provider is configured");
                if (getIntent() != null && getIntent().getBooleanExtra(EIP_RESTART_ON_BOOT, false)) {
                    EipCommand.startVPN(this, true);
                    finish();
                    return;
                }
                showMainActivity();
            }
        } else {
            configureLeapProvider();
        }
    }

    private void configureLeapProvider() {
        if (getIntent().hasExtra(APP_ACTION_CONFIGURE_ALWAYS_ON_PROFILE)) {
            getIntent().removeExtra(APP_ACTION_CONFIGURE_ALWAYS_ON_PROFILE);
        }
        startActivityForResult(new Intent(this, ProviderListActivity.class), REQUEST_CODE_CONFIGURE_LEAP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_CODE_CONFIGURE_LEAP) {
            if (resultCode == RESULT_OK && data != null && data.hasExtra(Provider.KEY)) {
                Provider provider = data.getParcelableExtra(Provider.KEY);
                ConfigHelper.storeProviderInPreferences(preferences, provider);
                EipCommand.startVPN(this, false);
                showMainActivity();
            } else if (resultCode == RESULT_CANCELED) {
                finish();
            }
        }
    }

    private void showMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(ACTION_SHOW_VPN_FRAGMENT);
        startActivity(intent);
        finish();
    }

}
