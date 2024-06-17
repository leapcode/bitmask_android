package se.leap.bitmaskclient.base.utils;

import static android.content.Context.MODE_PRIVATE;
import static se.leap.bitmaskclient.base.models.Constants.ALLOW_EXPERIMENTAL_TRANSPORTS;
import static se.leap.bitmaskclient.base.models.Constants.ALLOW_TETHERING_BLUETOOTH;
import static se.leap.bitmaskclient.base.models.Constants.ALLOW_TETHERING_USB;
import static se.leap.bitmaskclient.base.models.Constants.ALLOW_TETHERING_WIFI;
import static se.leap.bitmaskclient.base.models.Constants.ALWAYS_ON_SHOW_DIALOG;
import static se.leap.bitmaskclient.base.models.Constants.CLEARLOG;
import static se.leap.bitmaskclient.base.models.Constants.CUSTOM_PROVIDER_DOMAINS;
import static se.leap.bitmaskclient.base.models.Constants.DEFAULT_SHARED_PREFS_BATTERY_SAVER;
import static se.leap.bitmaskclient.base.models.Constants.EIP_IS_ALWAYS_ON;
import static se.leap.bitmaskclient.base.models.Constants.EIP_RESTART_ON_BOOT;
import static se.leap.bitmaskclient.base.models.Constants.EXCLUDED_APPS;
import static se.leap.bitmaskclient.base.models.Constants.FIRST_TIME_USER_DATE;
import static se.leap.bitmaskclient.base.models.Constants.GATEWAY_PINNING;
import static se.leap.bitmaskclient.base.models.Constants.LAST_DONATION_REMINDER_DATE;
import static se.leap.bitmaskclient.base.models.Constants.LAST_UPDATE_CHECK;
import static se.leap.bitmaskclient.base.models.Constants.LAST_USED_PROFILE;
import static se.leap.bitmaskclient.base.models.Constants.OBFUSCATION_PINNING_CERT;
import static se.leap.bitmaskclient.base.models.Constants.OBFUSCATION_PINNING_IP;
import static se.leap.bitmaskclient.base.models.Constants.OBFUSCATION_PINNING_KCP;
import static se.leap.bitmaskclient.base.models.Constants.OBFUSCATION_PINNING_LOCATION;
import static se.leap.bitmaskclient.base.models.Constants.OBFUSCATION_PINNING_PORT;
import static se.leap.bitmaskclient.base.models.Constants.PREFERENCES_APP_VERSION;
import static se.leap.bitmaskclient.base.models.Constants.PREFERRED_CITY;
import static se.leap.bitmaskclient.base.models.Constants.PREFER_UDP;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_CONFIGURED;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_EIP_DEFINITION;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_MOTD;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_MOTD_HASHES;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_MOTD_LAST_SEEN;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_MOTD_LAST_UPDATED;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_PRIVATE_KEY;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_VPN_CERTIFICATE;
import static se.leap.bitmaskclient.base.models.Constants.RESTART_ON_UPDATE;
import static se.leap.bitmaskclient.base.models.Constants.SHARED_ENCRYPTED_PREFERENCES;
import static se.leap.bitmaskclient.base.models.Constants.SHARED_PREFERENCES;
import static se.leap.bitmaskclient.base.models.Constants.SHOW_EXPERIMENTAL;
import static se.leap.bitmaskclient.base.models.Constants.USE_BRIDGES;
import static se.leap.bitmaskclient.base.models.Constants.USE_IPv6_FIREWALL;
import static se.leap.bitmaskclient.base.models.Constants.USE_OBFUSCATION_PINNING;
import static se.leap.bitmaskclient.base.models.Constants.USE_SNOWFLAKE;
import static se.leap.bitmaskclient.base.models.Constants.USE_SYSTEM_PROXY;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.NativeUtils;
import se.leap.bitmaskclient.BuildConfig;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.tor.TorStatusObservable;

/**
 * Created by cyberta on 18.03.18.
 */

public class PreferenceHelper {

    private static final String TAG = PreferenceHelper.class.getSimpleName();

    private static SharedPreferences preferences;
    private static final Object LOCK = new Object();

    private SharedPreferences initSharedPreferences(Context appContext) {
        Log.d(TAG, "getSharedPreferences is null");
        SharedPreferences preferences = null;
        try {
            MasterKey masterKey = new MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            preferences = EncryptedSharedPreferences.create(
                    appContext,
                    SHARED_ENCRYPTED_PREFERENCES,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            preferences = appContext.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        }

        Log.d(TAG, "getSharedPreferences finished");
        return preferences;
    }

    public PreferenceHelper(SharedPreferences preferences) {
        if (!NativeUtils.isUnitTest()) {
            throw new IllegalStateException("PreferenceHelper injected with shared preference outside of an unit test");
        }
        synchronized (LOCK) {
            PreferenceHelper.preferences = preferences;
        }
    }
    public PreferenceHelper(Context context) {
        synchronized (LOCK) {
            preferences = initSharedPreferences(context.getApplicationContext());
        }
    }

    public static void registerOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        synchronized (LOCK) {
            preferences.registerOnSharedPreferenceChangeListener(listener);
        }
    }

    public static void unregisterOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        synchronized (LOCK) {
            preferences.unregisterOnSharedPreferenceChangeListener(listener);
        }
    }

    public static Provider getSavedProviderFromSharedPreferences() {
        Provider provider = new Provider();
        synchronized (LOCK) {
            try {
                provider.setMainUrl(new URL(preferences.getString(Provider.MAIN_URL, "")));
                provider.setProviderIp(preferences.getString(Provider.PROVIDER_IP, ""));
                provider.setProviderApiIp(preferences.getString(Provider.PROVIDER_API_IP, ""));
                provider.setGeoipUrl(preferences.getString(Provider.GEOIP_URL, ""));
                provider.setMotdUrl(preferences.getString(Provider.MOTD_URL, ""));
                provider.define(new JSONObject(preferences.getString(Provider.KEY, "")));
                provider.setCaCert(preferences.getString(Provider.CA_CERT, ""));
                provider.setVpnCertificate(preferences.getString(PROVIDER_VPN_CERTIFICATE, ""));
                provider.setPrivateKey(preferences.getString(PROVIDER_PRIVATE_KEY, ""));
                provider.setEipServiceJson(new JSONObject(preferences.getString(PROVIDER_EIP_DEFINITION, "")));
                provider.setMotdJson(new JSONObject(preferences.getString(PROVIDER_MOTD, "")));
                provider.setLastMotdSeen(preferences.getLong(PROVIDER_MOTD_LAST_SEEN, 0L));
                provider.setLastMotdUpdate(preferences.getLong(PROVIDER_MOTD_LAST_UPDATED, 0L));
                provider.setMotdLastSeenHashes(preferences.getStringSet(PROVIDER_MOTD_HASHES, new HashSet<>()));
            } catch (MalformedURLException | JSONException e) {
                e.printStackTrace();
            }
        }

        return provider;
    }

    public static String getFromPersistedProvider(String toFetch, String providerDomain) {
        synchronized (LOCK) {
            return preferences.getString(toFetch + "." + providerDomain, "");
        }
    }

    public static long getLongFromPersistedProvider(String toFetch, String providerDomain) {
        synchronized (LOCK) {
            return preferences.getLong(toFetch + "." + providerDomain, 0L);
        }
    }

    public static Set<String> getStringSetFromPersistedProvider(String toFetch, String providerDomain) {
        synchronized (LOCK) {
            return preferences.getStringSet(toFetch + "." + providerDomain, new HashSet<>());
        }
    }

    public static void persistProviderAsync(Provider provider) {
        synchronized (LOCK) {
            storeProviderInPreferences(provider, true);
        }
    }

    public static void storeProviderInPreferences(Provider provider) {
        synchronized (LOCK) {
            storeProviderInPreferences(provider, false);
        }
    }

    /**
     *
     * @return HashMap with main URL string as key and Provider as value
     */
    public static HashMap<String, Provider> getCustomProviders() {
        Set<String> providerDomains = getCustomProviderDomains();
        HashMap<String, Provider> customProviders = new HashMap<>();
        for (String domain : providerDomains) {
            String mainURL = preferences.getString(Provider.MAIN_URL + "." + domain, null);
            if (mainURL != null) {
                customProviders.put(mainURL, Provider.createCustomProvider(mainURL, domain));
            }
        }
        return customProviders;
    }

    public static void setCustomProviders(Set<Provider> providers) {
        Set<String> newProviderDomains = new HashSet<>();

        // add
        SharedPreferences.Editor editor = preferences.edit();
        for (Provider provider : providers) {
            String providerDomain = provider.getDomain();
                    editor.putString(Provider.MAIN_URL + "." + providerDomain, provider.getMainUrlString());
            newProviderDomains.add(providerDomain);
        }

        // remove
        Set<String> removedProviderDomains = getCustomProviderDomains();
        removedProviderDomains.removeAll(newProviderDomains);
        for (String providerDomain : removedProviderDomains) {
            editor.remove(Provider.MAIN_URL + "." + providerDomain);
        }

        editor.putStringSet(CUSTOM_PROVIDER_DOMAINS, newProviderDomains);
        editor.apply();
    }

    static Set<String> getCustomProviderDomains() {
        return preferences.getStringSet(CUSTOM_PROVIDER_DOMAINS, new HashSet<>());
    }

    // TODO: replace commit with apply after refactoring EIP
    public static void storeProviderInPreferences(Provider provider, boolean async) {
        synchronized (LOCK) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(PROVIDER_CONFIGURED, true).
                    putString(Provider.PROVIDER_IP, provider.getProviderIp()).
                    putString(Provider.GEOIP_URL, provider.getGeoipUrl().toString()).
                    putString(Provider.MOTD_URL, provider.getMotdUrl().toString()).
                    putString(Provider.PROVIDER_API_IP, provider.getProviderApiIp()).
                    putString(Provider.MAIN_URL, provider.getMainUrlString()).
                    putString(Provider.KEY, provider.getDefinitionString()).
                    putString(Provider.CA_CERT, provider.getCaCert()).
                    putString(PROVIDER_EIP_DEFINITION, provider.getEipServiceJsonString()).
                    putString(PROVIDER_PRIVATE_KEY, provider.getPrivateKey()).
                    putString(PROVIDER_VPN_CERTIFICATE, provider.getVpnCertificate()).
                    putString(PROVIDER_MOTD, provider.getMotdJsonString()).
                    putStringSet(PROVIDER_MOTD_HASHES, provider.getMotdLastSeenHashes()).
                    putLong(PROVIDER_MOTD_LAST_SEEN, provider.getLastMotdSeen()).
                    putLong(PROVIDER_MOTD_LAST_UPDATED, provider.getLastMotdUpdate());
            if (async) {
                editor.apply();
            } else {
                editor.commit();
            }

            String providerDomain = provider.getDomain();
            preferences.edit().putBoolean(PROVIDER_CONFIGURED, true).
                    putString(Provider.PROVIDER_IP + "." + providerDomain, provider.getProviderIp()).
                    putString(Provider.PROVIDER_API_IP + "." + providerDomain, provider.getProviderApiIp()).
                    putString(Provider.MAIN_URL + "." + providerDomain, provider.getMainUrlString()).
                    putString(Provider.GEOIP_URL + "." + providerDomain, provider.getGeoipUrl().toString()).
                    putString(Provider.MOTD_URL + "." + providerDomain, provider.getMotdUrl().toString()).
                    putString(Provider.KEY + "." + providerDomain, provider.getDefinitionString()).
                    putString(Provider.CA_CERT + "." + providerDomain, provider.getCaCert()).
                    putString(PROVIDER_EIP_DEFINITION + "." + providerDomain, provider.getEipServiceJsonString()).
                    putString(PROVIDER_MOTD + "." + providerDomain, provider.getMotdJsonString()).
                    putStringSet(PROVIDER_MOTD_HASHES + "." + providerDomain, provider.getMotdLastSeenHashes()).
                    putLong(PROVIDER_MOTD_LAST_SEEN + "." + providerDomain, provider.getLastMotdSeen()).
                    putLong(PROVIDER_MOTD_LAST_UPDATED + "." + providerDomain, provider.getLastMotdUpdate()).
                    apply();
        }
    }

    public static void putProviderString(String providerDomain, String key, String value) {
        synchronized (LOCK) {
            putString(key+"."+providerDomain, value);
        }
    }

    /**
     * Sets the profile that is connected (to connect if the service restarts)
     */
    public static void setLastUsedVpnProfile(VpnProfile connectedProfile) {
        synchronized (LOCK) {
            preferences.edit().putString(LAST_USED_PROFILE, connectedProfile.toJson()).apply();
        }
    }

    /**
     * Returns the profile that was last connected (to connect if the service restarts)
     */
    public static VpnProfile getLastConnectedVpnProfile() {
        String lastConnectedProfileJson = null;
        synchronized (LOCK) {
            lastConnectedProfileJson = preferences.getString(LAST_USED_PROFILE, null);
        }
        return VpnProfile.fromJson(lastConnectedProfileJson);
    }

    public static void deleteProviderDetailsFromPreferences(String providerDomain) {
        synchronized (LOCK) {
            preferences.edit().
                    remove(Provider.KEY + "." + providerDomain).
                    remove(Provider.CA_CERT + "." + providerDomain).
                    remove(Provider.PROVIDER_IP + "." + providerDomain).
                    remove(Provider.PROVIDER_API_IP + "." + providerDomain).
                    remove(Provider.MAIN_URL + "." + providerDomain).
                    remove(Provider.GEOIP_URL + "." + providerDomain).
                    remove(Provider.MOTD_URL + "." + providerDomain).
                    remove(PROVIDER_EIP_DEFINITION + "." + providerDomain).
                    remove(PROVIDER_PRIVATE_KEY + "." + providerDomain).
                    remove(PROVIDER_VPN_CERTIFICATE + "." + providerDomain).
                    remove(PROVIDER_MOTD + "." + providerDomain).
                    remove(PROVIDER_MOTD_HASHES + "." + providerDomain).
                    remove(PROVIDER_MOTD_LAST_SEEN + "." + providerDomain).
                    remove(PROVIDER_MOTD_LAST_UPDATED + "." + providerDomain).
                    apply();
        }
    }

    public static void deleteCurrentProviderDetailsFromPreferences() {
        synchronized (LOCK) {
            preferences.edit().
                    remove(Provider.KEY).
                    remove(Provider.CA_CERT).
                    remove(Provider.PROVIDER_IP).
                    remove(Provider.PROVIDER_API_IP).
                    remove(Provider.MAIN_URL).
                    remove(Provider.GEOIP_URL).
                    remove(Provider.MOTD_URL).
                    remove(PROVIDER_EIP_DEFINITION).
                    remove(PROVIDER_PRIVATE_KEY).
                    remove(PROVIDER_VPN_CERTIFICATE).
                    remove(PROVIDER_MOTD).
                    remove(PROVIDER_MOTD_HASHES).
                    remove(PROVIDER_MOTD_LAST_SEEN).
                    remove(PROVIDER_MOTD_LAST_UPDATED).
                    apply();
        }
    }

    // used in fatweb flavor
     @SuppressWarnings("unused")
    public static void setLastAppUpdateCheck() {
        putLong(LAST_UPDATE_CHECK, System.currentTimeMillis());
    }

    public static long getLastAppUpdateCheck() {
        return getLong(LAST_UPDATE_CHECK, 0);
    }

    public static void restartOnUpdate(boolean isEnabled) {
        putBoolean(RESTART_ON_UPDATE, isEnabled);
    }

    public static boolean getRestartOnUpdate() {
        return getBoolean(RESTART_ON_UPDATE, false);
    }

    public static boolean getRestartOnBoot() {
        return getBoolean(EIP_RESTART_ON_BOOT, false);
    }

    public static void restartOnBoot(boolean isEnabled) {
        putBoolean(EIP_RESTART_ON_BOOT, isEnabled);
    }

    public static void restartOnBootSync(boolean isEnabled) {
        putBooleanSync(EIP_RESTART_ON_BOOT, isEnabled);
    }

    public static void isAlwaysOnSync(boolean isEnabled) {
        putBooleanSync(EIP_IS_ALWAYS_ON, isEnabled);
    }

    public static boolean getIsAlwaysOn() {
        return getBoolean(EIP_IS_ALWAYS_ON, false);
    }

    public static void lastDonationReminderDate(String dateString) {
        putString(LAST_DONATION_REMINDER_DATE, dateString);
    }

    public static String getLastDonationReminderDate() {
        return getString(LAST_DONATION_REMINDER_DATE, null);
    }

    public static void firstTimeUserDate(String dateString) {
        putString(FIRST_TIME_USER_DATE, dateString);
    }

    public static String getFirstTimeUserDate() {
        return getString(FIRST_TIME_USER_DATE, null);
    }

    public static void setProviderVPNCertificate(String certificate) {
        putString(PROVIDER_VPN_CERTIFICATE, certificate);
    }
    public static String getProviderVPNCertificate() {
        return getString(PROVIDER_VPN_CERTIFICATE, "");
    }

    public static int getAppVersion() {
        return getInt(PREFERENCES_APP_VERSION, -1);
    }

    public static void setAppVersion(int version) {
        putInt(PREFERENCES_APP_VERSION, version);
    }

    public static boolean getClearLog() {
        return getBoolean(CLEARLOG, true);
    }

    public static void setClearLog(boolean clearLog) {
        putBoolean(CLEARLOG, clearLog);
    }

    public static boolean getPreferUDP() {
        return getBoolean(PREFER_UDP, BuildConfig.prefer_udp);
    }

    public static void preferUDP(boolean prefer) {
        putBoolean(PREFER_UDP, prefer);
    }

    public static String getPinnedGateway() {
        return getString(GATEWAY_PINNING, null);
    }

    public static void pinGateway(String value) {
        putString(GATEWAY_PINNING, value);
    }

    public static boolean getUseBridges() {
        return getBoolean(USE_BRIDGES, false);
    }

    public static void useBridges(boolean isEnabled) {
        putBoolean(USE_BRIDGES, isEnabled);
    }

    public static void useSnowflake(boolean isEnabled) {
        putBoolean(USE_SNOWFLAKE, isEnabled);
        if (!isEnabled) {
            TorStatusObservable.setProxyPort(-1);
        }
    }

    public static boolean hasSnowflakePrefs() {
        return hasKey(USE_SNOWFLAKE);
    }

    public static Boolean getUseSnowflake() {
        return getBoolean(USE_SNOWFLAKE, true);
    }

    public static void saveBattery(boolean isEnabled) {
        putBoolean(DEFAULT_SHARED_PREFS_BATTERY_SAVER, isEnabled);
    }

    public static boolean getSaveBattery() {
        return getBoolean(DEFAULT_SHARED_PREFS_BATTERY_SAVER, false);
    }

    public static void allowUsbTethering(boolean isEnabled) {
        putBoolean(ALLOW_TETHERING_USB, isEnabled);
    }

    public static boolean isUsbTetheringAllowed() {
        return getBoolean(ALLOW_TETHERING_USB, false);
    }

    public static void allowWifiTethering(boolean isEnabled) {
        putBoolean(ALLOW_TETHERING_WIFI, isEnabled);
    }

    public static boolean isWifiTetheringAllowed() {
        return getBoolean(ALLOW_TETHERING_WIFI, false);
    }

    public static void allowBluetoothTethering(boolean isEnabled) {
        putBoolean(ALLOW_TETHERING_BLUETOOTH, isEnabled);
    }

    public static boolean isBluetoothTetheringAllowed() {
        return getBoolean(ALLOW_TETHERING_BLUETOOTH, false);
    }

    public static void setShowExperimentalFeatures(boolean show) {
        putBoolean(SHOW_EXPERIMENTAL, show);
    }

    public static boolean showExperimentalFeatures() {
        return getBoolean(SHOW_EXPERIMENTAL, false);
    }

    public static void setAllowExperimentalTransports(boolean show) {
        putBoolean(ALLOW_EXPERIMENTAL_TRANSPORTS, show);
    }

    public static boolean allowExperimentalTransports() {
        return getBoolean(ALLOW_EXPERIMENTAL_TRANSPORTS, false);
    }

    public static boolean useSystemProxy() {
        return getBoolean(USE_SYSTEM_PROXY, true);
    }

    public static void setUseObfuscationPinning(Boolean pinning) {
        putBoolean(USE_OBFUSCATION_PINNING, pinning);
    }

    public static boolean useObfuscationPinning() {
        return getUseBridges() &&
                getBoolean(USE_OBFUSCATION_PINNING, false) &&
                !TextUtils.isEmpty(getObfuscationPinningIP()) &&
                !TextUtils.isEmpty(getObfuscationPinningCert()) &&
                !TextUtils.isEmpty(getObfuscationPinningPort());
    }

    public static void setObfuscationPinningIP(String ip) {
        putString(OBFUSCATION_PINNING_IP, ip);
    }

    public static String getObfuscationPinningIP() {
        return getString(OBFUSCATION_PINNING_IP, null);
    }

    public static void setObfuscationPinningPort(String port) {
        putString(OBFUSCATION_PINNING_PORT, port);
    }

    public static String getObfuscationPinningPort() {
        return getString(OBFUSCATION_PINNING_PORT, null);
    }

    public static void setObfuscationPinningCert(String cert) {
        putString(OBFUSCATION_PINNING_CERT, cert);
    }

    public static String getObfuscationPinningCert() {
        return getString(OBFUSCATION_PINNING_CERT, null);
    }

    public static void setObfuscationPinningGatewayLocation(String location) {
        putString(OBFUSCATION_PINNING_LOCATION, location);
    }

    public static String getObfuscationPinningGatewayLocation() {
        return getString(OBFUSCATION_PINNING_LOCATION, null);
    }

    public static Boolean getObfuscationPinningKCP() {
        return getBoolean(OBFUSCATION_PINNING_KCP, false);
    }

    public static void setObfuscationPinningKCP(boolean isKCP) {
        putBoolean(OBFUSCATION_PINNING_KCP, isKCP);
    }

    public static void setUseIPv6Firewall(boolean useFirewall) {
        putBoolean(USE_IPv6_FIREWALL, useFirewall);
    }

    public static boolean useIpv6Firewall() {
        return getBoolean(USE_IPv6_FIREWALL, false);
    }

    public static void saveShowAlwaysOnDialog(boolean showAlwaysOnDialog) {
        putBoolean(ALWAYS_ON_SHOW_DIALOG, showAlwaysOnDialog);
    }

    public static boolean getShowAlwaysOnDialog() {
        return getBoolean(ALWAYS_ON_SHOW_DIALOG, true);
    }

    public static String getPreferredCity() {
        return useObfuscationPinning() ? null : getString(PREFERRED_CITY, null);
    }

    @WorkerThread
    public static void setPreferredCity(String city) {
        putStringSync(PREFERRED_CITY, city);
    }

    @VisibleForTesting
    public static JSONObject getEipDefinitionFromPreferences(SharedPreferences preferences) {
        JSONObject result = new JSONObject();
        String eipDefinitionString = "";
        try {
            synchronized (LOCK) {
                eipDefinitionString = preferences.getString(PROVIDER_EIP_DEFINITION, "");
            }
            if (!eipDefinitionString.isEmpty()) {
                result = new JSONObject(eipDefinitionString);
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    public static JSONObject getEipDefinitionFromPreferences() {
        JSONObject result = new JSONObject();
        String eipDefinitionString = "";
        try {
            synchronized (LOCK) {
                eipDefinitionString = preferences.getString(PROVIDER_EIP_DEFINITION, "");
            }
            if (!eipDefinitionString.isEmpty()) {
                result = new JSONObject(eipDefinitionString);
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    public static void setExcludedApps(Set<String> apps) {
        putStringSet(EXCLUDED_APPS, apps);
    }

    public static Set<String> getExcludedApps() {
        synchronized (LOCK) {
            return preferences.getStringSet(EXCLUDED_APPS, new HashSet<>());
        }
    }

    public static long getLong(String key, long defValue) {
        synchronized (LOCK) {
            return preferences.getLong(key, defValue);
        }
    }

    public static void putLong(String key, long value) {
        synchronized (LOCK) {
            preferences.edit().putLong(key, value).apply();
        }
    }

    public static int getInt(String key, int defValue) {
        synchronized (LOCK) {
            return preferences.getInt(key, defValue);
        }
    }

    public static void putInt(String key, int value) {
        synchronized (LOCK) {
            preferences.edit().putInt(key, value).apply();
        }
    }

    public static String getString(String key, String defValue) {
        synchronized (LOCK) {
            return preferences.getString(key, defValue);
        }
    }

    @WorkerThread
    public static void putStringSync(String key, String value) {
        synchronized (LOCK) {
            preferences.edit().putString(key, value).commit();
        }
    }

    public static void putString(String key, String value) {
        synchronized (LOCK) {
            preferences.edit().putString(key, value).apply();
        }
    }

    public static void putStringSet(String key, Set<String> value) {
        synchronized (LOCK) {
            preferences.edit().putStringSet(key, value).apply();
        }
    }

    public static boolean getBoolean(String key, Boolean defValue) {
        synchronized (LOCK) {
            return preferences.getBoolean(key, defValue);
        }
    }

    public static void putBoolean(String key, Boolean value) {
        synchronized (LOCK) {
            preferences.edit().putBoolean(key, value).apply();
        }
    }

    public static void putBooleanSync(String key, Boolean value) {
        synchronized (LOCK) {
            preferences.edit().putBoolean(key, value).commit();
        }
    }

    public static Boolean hasKey(String key) {
        synchronized (LOCK) {
            return preferences.contains(key);
        }
    }

    public static void migrateToEncryptedPrefs(Context context) {
        synchronized (LOCK) {
            if (!(preferences instanceof EncryptedSharedPreferences)) {
                Log.e(TAG, "Failed to migrate shared preferences");
                return;
            }
            SharedPreferences.Editor encryptedEditor = preferences.edit();
            SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
            Map<String,?> keys = preferences.getAll();

            for(Map.Entry<String,?> entry : keys.entrySet()){
                try {
                    Object value = entry.getValue();
                    if (value instanceof String) {
                        encryptedEditor.putString(entry.getKey(), (String) value);
                    } else if (value instanceof Boolean) {
                        encryptedEditor.putBoolean(entry.getKey(), (Boolean) value);
                    } else if (value instanceof Integer) {
                        encryptedEditor.putInt(entry.getKey(), (Integer) value);
                    } else if (value instanceof Set<?>) {
                        encryptedEditor.putStringSet(entry.getKey(), (Set<String>) value);
                    } else if (value instanceof Long) {
                        encryptedEditor.putLong(entry.getKey(), (Long) value);
                    } else if (value instanceof Float) {
                        encryptedEditor.putFloat(entry.getKey(), (Float) value);
                    }
                } catch (ClassCastException e) {
                    e.printStackTrace();
                }
            }
            encryptedEditor.commit();
            preferences.edit().clear().apply();
        }
        }
}
