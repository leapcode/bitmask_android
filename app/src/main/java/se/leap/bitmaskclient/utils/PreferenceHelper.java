package se.leap.bitmaskclient.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.blinkt.openvpn.VpnProfile;
import se.leap.bitmaskclient.Provider;

import static android.content.Context.MODE_PRIVATE;
import static se.leap.bitmaskclient.Constants.ALWAYS_ON_SHOW_DIALOG;
import static se.leap.bitmaskclient.Constants.DEFAULT_SHARED_PREFS_BATTERY_SAVER;
import static se.leap.bitmaskclient.Constants.EXCLUDED_APPS;
import static se.leap.bitmaskclient.Constants.LAST_USED_PROFILE;
import static se.leap.bitmaskclient.Constants.PREFERENCES_APP_VERSION;
import static se.leap.bitmaskclient.Constants.PROVIDER_CONFIGURED;
import static se.leap.bitmaskclient.Constants.PROVIDER_EIP_DEFINITION;
import static se.leap.bitmaskclient.Constants.PROVIDER_PRIVATE_KEY;
import static se.leap.bitmaskclient.Constants.PROVIDER_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.Constants.SHARED_PREFERENCES;
import static se.leap.bitmaskclient.Constants.SU_PERMISSION;
import static se.leap.bitmaskclient.Constants.USE_PLUGGABLE_TRANSPORTS;

/**
 * Created by cyberta on 18.03.18.
 */

public class PreferenceHelper {
    public static boolean providerInSharedPreferences(@NonNull SharedPreferences preferences) {
        return preferences.getBoolean(PROVIDER_CONFIGURED, false);
    }

    public static Provider getSavedProviderFromSharedPreferences(@NonNull SharedPreferences preferences) {
        Provider provider = new Provider();
        try {
            provider.setMainUrl(new URL(preferences.getString(Provider.MAIN_URL, "")));
            provider.setProviderIp(preferences.getString(Provider.PROVIDER_IP, ""));
            provider.setProviderApiIp(preferences.getString(Provider.PROVIDER_API_IP, ""));
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

    public static String getProviderName(String provider) {
        return getProviderName(null, provider);
    }

    public static String getProviderName(@Nullable SharedPreferences preferences) {
        return getProviderName(preferences,null);
    }

    public static String getProviderName(@Nullable SharedPreferences preferences, @Nullable String provider) {
        if (provider == null && preferences != null) {
            provider = preferences.getString(Provider.KEY, "");
        }
        try {
            JSONObject providerJson = new JSONObject(provider);
            String lang = Locale.getDefault().getLanguage();
            return providerJson.getJSONObject(Provider.NAME).getString(lang);
        } catch (JSONException e) {
            try {
                JSONObject providerJson = new JSONObject(provider);
                return providerJson.getJSONObject(Provider.NAME).getString("en");
            } catch (JSONException e2) {
                return null;
            }
        } catch (NullPointerException npe) {
            return null;
        }
    }

    public static String getProviderDomain(SharedPreferences preferences) {
        return getProviderDomain(preferences, null);
    }

    public static String getProviderDomain(String provider) {
        return getProviderDomain(null, provider);
    }

    public static String getProviderDomain(@Nullable SharedPreferences preferences, @Nullable String provider) {
        if (provider == null && preferences != null) {
            provider = preferences.getString(Provider.KEY, "");
        }
        try {
            JSONObject providerJson = new JSONObject(provider);
            return providerJson.getString(Provider.DOMAIN);
        } catch (JSONException | NullPointerException e) {
            return null;
        }
    }

    public static String getDescription(SharedPreferences preferences) {
        try {
            JSONObject providerJson = new JSONObject(preferences.getString(Provider.KEY, ""));
            String lang = Locale.getDefault().getLanguage();
            return providerJson.getJSONObject(Provider.DESCRIPTION).getString(lang);
        } catch (JSONException e) {
            try {
                JSONObject providerJson = new JSONObject(preferences.getString(Provider.KEY, ""));
                return providerJson.getJSONObject(Provider.DESCRIPTION).getString("en");
            } catch (JSONException e1) {
                return null;
            }
        }
    }

    // TODO: replace commit with apply after refactoring EIP
    //FIXME: don't save private keys in shared preferences! use the keystore
    public static void storeProviderInPreferences(SharedPreferences preferences, Provider provider) {
        preferences.edit().putBoolean(PROVIDER_CONFIGURED, true).
                putString(Provider.PROVIDER_IP, provider.getProviderIp()).
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




    public static void clearDataOfLastProvider(SharedPreferences preferences) {
        clearDataOfLastProvider(preferences, false);
    }

    @Deprecated
    public static void clearDataOfLastProvider(SharedPreferences preferences, boolean commit) {
        Map<String, ?> allEntries = preferences.getAll();
        List<String> lastProvidersKeys = new ArrayList<>();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            //sort out all preferences that don't belong to the last provider
            if (entry.getKey().startsWith(Provider.KEY + ".") ||
                    entry.getKey().startsWith(Provider.CA_CERT + ".") ||
                    entry.getKey().startsWith(Provider.CA_CERT_FINGERPRINT + "." )||
                    entry.getKey().equals(PREFERENCES_APP_VERSION)
                    ) {
                continue;
            }
            lastProvidersKeys.add(entry.getKey());
        }

        SharedPreferences.Editor preferenceEditor = preferences.edit();
        for (String key : lastProvidersKeys) {
            preferenceEditor.remove(key);
        }
        if (commit) {
            preferenceEditor.commit();
        } else {
            preferenceEditor.apply();
        }
    }

    public static void deleteProviderDetailsFromPreferences(@NonNull SharedPreferences preferences, String providerDomain) {
        preferences.edit().
                remove(Provider.KEY + "." + providerDomain).
                remove(Provider.CA_CERT + "." + providerDomain).
                remove(Provider.PROVIDER_IP + "." + providerDomain).
                remove(Provider.PROVIDER_API_IP + "." + providerDomain).
                remove(Provider.MAIN_URL + "." + providerDomain).
                remove(PROVIDER_EIP_DEFINITION + "." + providerDomain).
                remove(PROVIDER_PRIVATE_KEY + "." + providerDomain).
                remove(PROVIDER_VPN_CERTIFICATE + "." + providerDomain).
                apply();
    }

    public static boolean hasSuPermission(Context context) {
        return getBoolean(context, SU_PERMISSION, false);
    }

    public static void setSuPermission(Context context, boolean allowed) {
        putBoolean(context, SU_PERMISSION, allowed);
    }

    public static boolean getUsePluggableTransports(Context context) {
        return getBoolean(context, USE_PLUGGABLE_TRANSPORTS, false);
    }

    public static void usePluggableTransports(Context context, boolean isEnabled) {
        putBoolean(context, USE_PLUGGABLE_TRANSPORTS, isEnabled);
    }

    public static void saveBattery(Context context, boolean isEnabled) {
        putBoolean(context, DEFAULT_SHARED_PREFS_BATTERY_SAVER, isEnabled);
    }

    public static boolean getSaveBattery(Context context) {
        return getBoolean(context, DEFAULT_SHARED_PREFS_BATTERY_SAVER, false);
    }

    public static void saveShowAlwaysOnDialog(Context context, boolean showAlwaysOnDialog) {
        putBoolean(context, ALWAYS_ON_SHOW_DIALOG, showAlwaysOnDialog);
    }

    public static boolean getShowAlwaysOnDialog(Context context) {
        return getBoolean(context, ALWAYS_ON_SHOW_DIALOG, true);
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

    public static String getString(Context context, String key, String defValue) {
        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        return preferences.getString(key, defValue);
    }

    public static void putString(Context context, String key, String value) {
        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        preferences.edit().putString(key, value).apply();
    }

    public static Boolean getBoolean(Context context, String key, Boolean defValue) {
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

}
