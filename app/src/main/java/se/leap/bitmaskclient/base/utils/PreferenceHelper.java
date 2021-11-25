package se.leap.bitmaskclient.base.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import de.blinkt.openvpn.VpnProfile;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.tor.TorStatusObservable;

import static android.content.Context.MODE_PRIVATE;
import static se.leap.bitmaskclient.base.models.Constants.ALLOW_TETHERING_BLUETOOTH;
import static se.leap.bitmaskclient.base.models.Constants.ALLOW_TETHERING_USB;
import static se.leap.bitmaskclient.base.models.Constants.ALLOW_TETHERING_WIFI;
import static se.leap.bitmaskclient.base.models.Constants.ALWAYS_ON_SHOW_DIALOG;
import static se.leap.bitmaskclient.base.models.Constants.DEFAULT_SHARED_PREFS_BATTERY_SAVER;
import static se.leap.bitmaskclient.base.models.Constants.EXCLUDED_APPS;
import static se.leap.bitmaskclient.base.models.Constants.LAST_UPDATE_CHECK;
import static se.leap.bitmaskclient.base.models.Constants.LAST_USED_PROFILE;
import static se.leap.bitmaskclient.base.models.Constants.PREFERRED_CITY;
import static se.leap.bitmaskclient.base.models.Constants.PREFER_UDP;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_CONFIGURED;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_EIP_DEFINITION;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_PRIVATE_KEY;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.base.models.Constants.RESTART_ON_UPDATE;
import static se.leap.bitmaskclient.base.models.Constants.SHARED_PREFERENCES;
import static se.leap.bitmaskclient.base.models.Constants.SHOW_EXPERIMENTAL;
import static se.leap.bitmaskclient.base.models.Constants.USE_BRIDGES;
import static se.leap.bitmaskclient.base.models.Constants.USE_IPv6_FIREWALL;
import static se.leap.bitmaskclient.base.models.Constants.USE_SNOWFLAKE;

/**
 * Created by cyberta on 18.03.18.
 */

public class PreferenceHelper {

    public static Provider getSavedProviderFromSharedPreferences(@NonNull SharedPreferences preferences) {
        Provider provider = new Provider();
        try {
            provider.setMainUrl(new URL(preferences.getString(Provider.MAIN_URL, "")));
            provider.setProviderIp(preferences.getString(Provider.PROVIDER_IP, ""));
            provider.setProviderApiIp(preferences.getString(Provider.PROVIDER_API_IP, ""));
            provider.setGeoipUrl(preferences.getString(Provider.GEOIP_URL, ""));
            provider.define(new JSONObject(preferences.getString(Provider.KEY, "")));
            provider.setCaCert(preferences.getString(Provider.CA_CERT, ""));
            provider.setVpnCertificate(preferences.getString(PROVIDER_VPN_CERTIFICATE, ""));
            provider.setPrivateKey(preferences.getString(PROVIDER_PRIVATE_KEY, ""));
            provider.setEipServiceJson(new JSONObject(preferences.getString(PROVIDER_EIP_DEFINITION, "")));
        } catch (MalformedURLException | JSONException e) {
            e.printStackTrace();
        }

        return provider;
    }

    public static String getFromPersistedProvider(String toFetch, String providerDomain, SharedPreferences preferences) {
        return preferences.getString(toFetch + "." + providerDomain, "");
    }

    // TODO: replace commit with apply after refactoring EIP
    //FIXME: don't save private keys in shared preferences! use the keystore
    public static void storeProviderInPreferences(SharedPreferences preferences, Provider provider) {
        preferences.edit().putBoolean(PROVIDER_CONFIGURED, true).
                putString(Provider.PROVIDER_IP, provider.getProviderIp()).
                putString(Provider.GEOIP_URL, provider.getGeoipUrl().toString()).
                putString(Provider.PROVIDER_API_IP, provider.getProviderApiIp()).
                putString(Provider.MAIN_URL, provider.getMainUrlString()).
                putString(Provider.KEY, provider.getDefinitionString()).
                putString(Provider.CA_CERT, provider.getCaCert()).
                putString(PROVIDER_EIP_DEFINITION, provider.getEipServiceJsonString()).
                putString(PROVIDER_PRIVATE_KEY, provider.getPrivateKey()).
                putString(PROVIDER_VPN_CERTIFICATE, provider.getVpnCertificate()).
                commit();

        String providerDomain = provider.getDomain();
        preferences.edit().putBoolean(PROVIDER_CONFIGURED, true).
                putString(Provider.PROVIDER_IP + "." + providerDomain, provider.getProviderIp()).
                putString(Provider.PROVIDER_API_IP + "." + providerDomain, provider.getProviderApiIp()).
                putString(Provider.MAIN_URL + "." + providerDomain, provider.getMainUrlString()).
                putString(Provider.GEOIP_URL + "." + providerDomain, provider.getGeoipUrl().toString()).
                putString(Provider.KEY + "." + providerDomain, provider.getDefinitionString()).
                putString(Provider.CA_CERT + "." + providerDomain, provider.getCaCert()).
                putString(PROVIDER_EIP_DEFINITION + "." + providerDomain, provider.getEipServiceJsonString()).
                apply();
    }

    /**
     * Sets the profile that is connected (to connect if the service restarts)
     */
    public static void setLastUsedVpnProfile(Context context, VpnProfile connectedProfile) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor prefsedit = prefs.edit();
        prefsedit.putString(LAST_USED_PROFILE, connectedProfile.toJson());
        prefsedit.apply();
    }

    /**
     * Returns the profile that was last connected (to connect if the service restarts)
     */
    public static VpnProfile getLastConnectedVpnProfile(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        String lastConnectedProfileJson = preferences.getString(LAST_USED_PROFILE, null);
        return VpnProfile.fromJson(lastConnectedProfileJson);
    }

    public static void deleteProviderDetailsFromPreferences(@NonNull SharedPreferences preferences, String providerDomain) {
        preferences.edit().
                remove(Provider.KEY + "." + providerDomain).
                remove(Provider.CA_CERT + "." + providerDomain).
                remove(Provider.PROVIDER_IP + "." + providerDomain).
                remove(Provider.PROVIDER_API_IP + "." + providerDomain).
                remove(Provider.MAIN_URL + "." + providerDomain).
                remove(Provider.GEOIP_URL + "." + providerDomain).
                remove(PROVIDER_EIP_DEFINITION + "." + providerDomain).
                remove(PROVIDER_PRIVATE_KEY + "." + providerDomain).
                remove(PROVIDER_VPN_CERTIFICATE + "." + providerDomain).
                apply();
    }

    public static void setLastAppUpdateCheck(Context context) {
        putLong(context, LAST_UPDATE_CHECK, System.currentTimeMillis());
    }

    public static long getLastAppUpdateCheck(Context context) {
        return getLong(context, LAST_UPDATE_CHECK, 0);
    }

    public static void restartOnUpdate(Context context, boolean isEnabled) {
        putBoolean(context, RESTART_ON_UPDATE, isEnabled);
    }

    public static boolean getRestartOnUpdate(Context context) {
        return getBoolean(context, RESTART_ON_UPDATE, false);
    }

    public static boolean getPreferUDP(Context context) {
        return getBoolean(context, PREFER_UDP, false);
    }

    public static void preferUDP(Context context, boolean prefer) {
        putBoolean(context, PREFER_UDP, prefer);
    }

    public static boolean getUseBridges(SharedPreferences preferences) {
        return preferences.getBoolean(USE_BRIDGES, false);
    }

    public static boolean getUseBridges(Context context) {
        return getBoolean(context, USE_BRIDGES, false);
    }

    public static void useBridges(Context context, boolean isEnabled) {
        putBoolean(context, USE_BRIDGES, isEnabled);
    }

    public static Boolean getUseSnowflake(SharedPreferences preferences) {
        return preferences.getBoolean(USE_SNOWFLAKE, true);
    }

    public static void useSnowflake(Context context, boolean isEnabled) {
        putBoolean(context, USE_SNOWFLAKE, isEnabled);
        if (!isEnabled) {
            TorStatusObservable.setProxyPort(-1);
        }
    }

    public static boolean hasSnowflakePrefs(SharedPreferences preferences) {
        return preferences.contains(USE_SNOWFLAKE);
    }

    public static boolean hasSnowflakePrefs(Context context) {
        return hasKey(context, USE_SNOWFLAKE);
    }

    public static Boolean getUseSnowflake(Context context) {
        return getBoolean(context, USE_SNOWFLAKE, true);
    }

    public static void saveBattery(Context context, boolean isEnabled) {
        putBoolean(context, DEFAULT_SHARED_PREFS_BATTERY_SAVER, isEnabled);
    }

    public static boolean getSaveBattery(Context context) {
        return getBoolean(context, DEFAULT_SHARED_PREFS_BATTERY_SAVER, false);
    }

    public static void allowUsbTethering(Context context, boolean isEnabled) {
        putBoolean(context, ALLOW_TETHERING_USB, isEnabled);
    }

    public static boolean isUsbTetheringAllowed(Context context) {
        return getBoolean(context, ALLOW_TETHERING_USB, false);
    }

    public static void allowWifiTethering(Context context, boolean isEnabled) {
        putBoolean(context, ALLOW_TETHERING_WIFI, isEnabled);
    }

    public static boolean isWifiTetheringAllowed(Context context) {
        return getBoolean(context, ALLOW_TETHERING_WIFI, false);
    }

    public static void allowBluetoothTethering(Context context, boolean isEnabled) {
        putBoolean(context, ALLOW_TETHERING_BLUETOOTH, isEnabled);
    }

    public static boolean isBluetoothTetheringAllowed(Context context) {
        return getBoolean(context, ALLOW_TETHERING_BLUETOOTH, false);
    }

    public static void setShowExperimentalFeatures(Context context, boolean show) {
        putBoolean(context, SHOW_EXPERIMENTAL, show);
    }

    public static boolean showExperimentalFeatures(Context context) {
        return getBoolean(context, SHOW_EXPERIMENTAL, false);
    }

    public static void setUseIPv6Firewall(Context context, boolean useFirewall) {
        putBoolean(context, USE_IPv6_FIREWALL, useFirewall);
    }

    public static boolean useIpv6Firewall(Context context) {
        return getBoolean(context, USE_IPv6_FIREWALL, false);
    }

    public static void saveShowAlwaysOnDialog(Context context, boolean showAlwaysOnDialog) {
        putBoolean(context, ALWAYS_ON_SHOW_DIALOG, showAlwaysOnDialog);
    }

    public static boolean getShowAlwaysOnDialog(Context context) {
        return getBoolean(context, ALWAYS_ON_SHOW_DIALOG, true);
    }

    public static String getPreferredCity(Context context) {
        return getString(context, PREFERRED_CITY, null);
    }

    @WorkerThread
    public static void setPreferredCity(Context context, String city) {
        putStringSync(context, PREFERRED_CITY, city);
    }

    public static JSONObject getEipDefinitionFromPreferences(SharedPreferences preferences) {
        JSONObject result = new JSONObject();
        try {
            String eipDefinitionString = preferences.getString(PROVIDER_EIP_DEFINITION, "");
            if (!eipDefinitionString.isEmpty()) {
                result = new JSONObject(eipDefinitionString);
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    public static void setExcludedApps(Context context, Set<String> apps) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor prefsedit = prefs.edit();
        prefsedit.putStringSet(EXCLUDED_APPS, apps);
        prefsedit.apply();
    }

    public static Set<String> getExcludedApps(Context context) {
        if (context == null) {
            return null;
        }
        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        return preferences.getStringSet(EXCLUDED_APPS, new HashSet<>());
    }

    public static long getLong(Context context, String key, long defValue) {
        if (context == null) {
            return defValue;
        }
        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        return preferences.getLong(key, defValue);
    }

    public static void putLong(Context context, String key, long value) {
        if (context == null) {
            return;
        }
        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        preferences.edit().putLong(key, value).apply();
    }

    public static String getString(Context context, String key, String defValue) {
        if (context == null) {
            return defValue;
        }
        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        return preferences.getString(key, defValue);
    }

    @WorkerThread
    public static void putStringSync(Context context, String key, String value) {
        if (context == null) {
            return;
        }
        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        preferences.edit().putString(key, value).commit();
    }

    public static void putString(Context context, String key, String value) {
        if (context == null) {
            return;
        }
        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        preferences.edit().putString(key, value).apply();
    }

    public static boolean getBoolean(Context context, String key, Boolean defValue) {
        if (context == null) {
            return false;
        }

        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        return preferences.getBoolean(key, defValue);
    }

    public static void putBoolean(Context context, String key, Boolean value) {
        if (context == null) {
            return;
        }

        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        preferences.edit().putBoolean(key, value).apply();
    }

    private static Boolean hasKey(Context context, String key) {
        if (context == null) {
            return false;
        }

        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        return preferences.contains(key);
    }
}
