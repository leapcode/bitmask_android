/**
 * Copyright (c) 2018 LEAP Encryption Access Project and contributers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package se.leap.bitmaskclient;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import de.blinkt.openvpn.core.Preferences;
import de.blinkt.openvpn.core.VpnStatus;
import se.leap.bitmaskclient.eip.EipCommand;
import se.leap.bitmaskclient.userstatus.User;
import se.leap.bitmaskclient.utils.PreferenceHelper;

import static se.leap.bitmaskclient.Constants.APP_ACTION_CONFIGURE_ALWAYS_ON_PROFILE;
import static se.leap.bitmaskclient.Constants.EIP_RESTART_ON_BOOT;
import static se.leap.bitmaskclient.Constants.PREFERENCES_APP_VERSION;
import static se.leap.bitmaskclient.Constants.PROVIDER_EIP_DEFINITION;
import static se.leap.bitmaskclient.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.Constants.REQUEST_CODE_CONFIGURE_LEAP;
import static se.leap.bitmaskclient.Constants.SHARED_PREFERENCES;
import static se.leap.bitmaskclient.MainActivity.ACTION_SHOW_VPN_FRAGMENT;
import static se.leap.bitmaskclient.utils.ConfigHelper.isDefaultBitmask;
import static se.leap.bitmaskclient.utils.PreferenceHelper.getSavedProviderFromSharedPreferences;
import static se.leap.bitmaskclient.utils.PreferenceHelper.providerInSharedPreferences;
import static se.leap.bitmaskclient.utils.PreferenceHelper.storeProviderInPreferences;

/**
 * Activity shown at startup. Evaluates if App is started for the first time or has been upgraded
 * and acts and calls another activity accordingly.
 *
 */
public class StartActivity extends Activity{
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

        fakeSetup();


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
        boolean provider_exists = providerInSharedPreferences(preferences);
        if (provider_exists) {
            Provider provider = getSavedProviderFromSharedPreferences(preferences);
            if(!provider.isConfigured()) {
                configureLeapProvider();
            } else {
                Log.d(TAG, "vpn provider is configured");
                if (getIntent() != null && getIntent().getBooleanExtra(EIP_RESTART_ON_BOOT, false)) {
                    EipCommand.startVPN(this.getApplicationContext(), true);
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
        if (isDefaultBitmask()) {
            startActivityForResult(new Intent(this, ProviderListActivity.class), REQUEST_CODE_CONFIGURE_LEAP);
        } else { // custom branded app
            startActivityForResult(new Intent(this, CustomProviderSetupActivity.class), REQUEST_CODE_CONFIGURE_LEAP);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_CODE_CONFIGURE_LEAP) {
            if (resultCode == RESULT_OK && data != null && data.hasExtra(Provider.KEY)) {
                Provider provider = data.getParcelableExtra(Provider.KEY);
                storeProviderInPreferences(preferences, provider);
                ProviderObservable.getInstance().updateProvider(provider);
                EipCommand.startVPN(this.getApplicationContext(), false);
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

    private void fakeSetup() {
        PreferenceHelper.putString(this, "Constants.EIP_DEFINITION.riseup.net", getRiseupEipJson());
        PreferenceHelper.putString(this, "Constants.EIP_DEFINITION", getRiseupEipJson());
        SharedPreferences prefs = Preferences.getDefaultSharedPreferences(this);
        SharedPreferences.Editor prefsedit = prefs.edit();
        prefsedit.remove("lastConnectedProfile").commit();
        File f  = new File(this.getCacheDir().getAbsolutePath() + "/android.conf");
        if (f.exists()) {
            Log.d(TAG, "android.conf exists -> delete:" + f.delete());
        }

        File filesDirectory = new File(this.getFilesDir().getAbsolutePath());
        if (filesDirectory.exists() && filesDirectory.isDirectory()) {
           File[] filesInDirectory = filesDirectory.listFiles();
           for (File file : filesInDirectory) {
               Log.d(TAG, "delete profile: " + file.getName() + ": "+ file.delete());

           }
        } else Log.d(TAG, "file folder doesn't exist");


        Log.d(TAG, "faked eipjson: " + PreferenceHelper.getString(this, "Constants.EIP_DEFINITION", ""));
        Log.d(TAG, "lastConnectedProfile is emty: " + (prefs.getString("lastConnectedProfile", null) == null));
    }

    private String getRiseupEipJson() {
        return "{\n" +
                "   \"gateways\":[\n" +
                "      {\n" +
                "         \"capabilities\":{\n" +
                "            \"adblock\":false,\n" +
                "            \"filter_dns\":false,\n" +
                "            \"limited\":false,\n" +
                "            \"ports\":[\n" +
                "               \"443\"\n" +
                "            ],\n" +
                "            \"protocols\":[\n" +
                "               \"tcp\"\n" +
                "            ],\n" +
                "            \"transport\":[\n" +
                "               \"openvpn\"\n" +
                "            ],\n" +
                "            \"user_ips\":false\n" +
                "         },\n" +
                "         \"host\":\"garza.riseup.net\",\n" +
                "         \"ip_address\":\"198.252.153.28\",\n" +
                "         \"location\":\"seattle\"\n" +
                "      },\n" +
                "      {\n" +
                "         \"capabilities\":{\n" +
                "            \"adblock\":false,\n" +
                "            \"filter_dns\":false,\n" +
                "            \"limited\":false,\n" +
                "            \"ports\":[\n" +
                "               \"443\"\n" +
                "            ],\n" +
                "            \"protocols\":[\n" +
                "               \"tcp\"\n" +
                "            ],\n" +
                "            \"transport\":[\n" +
                "               \"openvpn\"\n" +
                "            ],\n" +
                "            \"user_ips\":false\n" +
                "         },\n" +
                "         \"host\":\"no.giraffe.riseup.net\",\n" +
                "         \"ip_address\":\"37.218.242.212\",\n" +
                "         \"location\":\"amsterdam\"\n" +
                "      },\n" +
                "      {\n" +
                "         \"capabilities\":{\n" +
                "            \"adblock\":false,\n" +
                "            \"filter_dns\":false,\n" +
                "            \"limited\":false,\n" +
                "            \"ports\":[\n" +
                "               \"443\"\n" +
                "            ],\n" +
                "            \"protocols\":[\n" +
                "               \"tcp\"\n" +
                "            ],\n" +
                "            \"transport\":[\n" +
                "               \"openvpn\"\n" +
                "            ],\n" +
                "            \"user_ips\":false\n" +
                "         },\n" +
                "         \"host\":\"no.tenca.riseup.net\",\n" +
                "         \"ip_address\":\"5.79.86.181\",\n" +
                "         \"location\":\"amsterdam\"\n" +
                "      },\n" +
                "      {\n" +
                "         \"capabilities\":{\n" +
                "            \"adblock\":false,\n" +
                "            \"filter_dns\":false,\n" +
                "            \"limited\":false,\n" +
                "            \"ports\":[\n" +
                "               \"443\"\n" +
                "            ],\n" +
                "            \"protocols\":[\n" +
                "               \"tcp\"\n" +
                "            ],\n" +
                "            \"transport\":[\n" +
                "               \"openvpn\"\n" +
                "            ],\n" +
                "            \"user_ips\":false\n" +
                "         },\n" +
                "         \"host\":\"yal.riseup.net\",\n" +
                "         \"ip_address\":\"199.58.81.145\",\n" +
                "         \"location\":\"montreal\"\n" +
                "      }\n" +
                "   ],\n" +
                "   \"locations\":{\n" +
                "      \"amsterdam\":{\n" +
                "         \"country_code\":\"NL\",\n" +
                "         \"hemisphere\":\"N\",\n" +
                "         \"name\":\"Amsterdam\",\n" +
                "         \"timezone\":\"+2\"\n" +
                "      },\n" +
                "      \"montreal\":{\n" +
                "         \"country_code\":\"CA\",\n" +
                "         \"hemisphere\":\"N\",\n" +
                "         \"name\":\"Montreal\",\n" +
                "         \"timezone\":\"-5\"\n" +
                "      },\n" +
                "      \"seattle\":{\n" +
                "         \"country_code\":\"US\",\n" +
                "         \"hemisphere\":\"N\",\n" +
                "         \"name\":\"Seattle\",\n" +
                "         \"timezone\":\"-7\"\n" +
                "      }\n" +
                "   },\n" +
                "   \"openvpn_configuration\":{\n" +
                "      \"auth\":\"SHA1\",\n" +
                "      \"cipher\":\"AES-128-CBC\",\n" +
                "      \"keepalive\":\"10 30\",\n" +
                "      \"tls-cipher\":\"DHE-RSA-AES128-SHA\",\n" +
                "      \"tun-ipv6\":true\n" +
                "   },\n" +
                "   \"serial\":1,\n" +
                "   \"version\":1\n" +
                "}";
    }

}
