/**
 * Copyright (c) 2021 LEAP Encryption Access Project and contributers
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
package se.leap.bitmaskclient.base.models;

import android.text.TextUtils;

import se.leap.bitmaskclient.BuildConfig;

public interface Constants {

    //////////////////////////////////////////////
    // PREFERENCES CONSTANTS
    /////////////////////////////////////////////

    String SHARED_PREFERENCES = "LEAPPreferences";
    String SHARED_ENCRYPTED_PREFERENCES = "LEAPEncryptedPreferences";
    String PREFERENCES_APP_VERSION = "bitmask version";
    String ALWAYS_ON_SHOW_DIALOG = "DIALOG.ALWAYS_ON_SHOW_DIALOG";
    String CLEARLOG = "clearlogconnect";
    String LAST_USED_PROFILE = "last_used_profile";
    String EXCLUDED_APPS = "excluded_apps";
    String USE_BRIDGES = "usePluggableTransports";
    String ALLOW_TETHERING_BLUETOOTH = "tethering_bluetooth";
    String ALLOW_TETHERING_WIFI = "tethering_wifi";
    String ALLOW_TETHERING_USB = "tethering_usb";
    String SHOW_EXPERIMENTAL = "show_experimental";
    String USE_IPv6_FIREWALL = "use_ipv6_firewall";
    String RESTART_ON_UPDATE = "restart_on_update";
    String LAST_UPDATE_CHECK = "last_update_check";
    String PREFERRED_CITY = "preferred_city";
    // ATTENTION: this key is also used in bitmask-core for persistence
    String COUNTRYCODE = "COUNTRYCODE";
    String USE_SNOWFLAKE = "use_snowflake";
    String PREFER_UDP = "prefer_UDP";
    String GATEWAY_PINNING = "gateway_pinning";
    String ALLOW_EXPERIMENTAL_TRANSPORTS = "allow_experimental_transports";
    String USE_OBFUSCATION_PINNING = "use_obfuscation_pinning";
    String OBFUSCATION_PINNING_IP = "obfuscation_pinning_ip";
    String OBFUSCATION_PINNING_PORT = "obfuscation_pinning_port";
    String OBFUSCATION_PINNING_CERT = "obfuscation_pinning_cert";
    String OBFUSCATION_PINNING_KCP = "obfuscation_pinning_udp";
    String OBFUSCATION_PINNING_LOCATION = "obfuscation_pinning_location";
    String USE_SYSTEM_PROXY = "usesystemproxy";
    String CUSTOM_PROVIDER_DOMAINS = "custom_provider_domains";
    String USE_PORT_HOPPING = "use_port_hopping";
    String USE_TUNNEL = "tunnel";


    //////////////////////////////////////////////
    // REQUEST CODE CONSTANTS
    /////////////////////////////////////////////

    String REQUEST_CODE_KEY = "request_code";
    int REQUEST_CODE_CONFIGURE_LEAP = 0;
    int REQUEST_CODE_SWITCH_PROVIDER = 1;
    int REQUEST_CODE_LOG_IN = 2;
    int REQUEST_CODE_ADD_PROVIDER = 3;
    int REQUEST_CODE_REQUEST_UPDATE = 4;


    //////////////////////////////////////////////
    // APP CONSTANTS
    /////////////////////////////////////////////

    String APP_ACTION_QUIT = "quit";
    String APP_ACTION_CONFIGURE_ALWAYS_ON_PROFILE = "configure always-on profile";
    String DEFAULT_BITMASK = "normal";
    String CUSTOM_BITMASK = "custom";
    String DANGER_ON = "danger_on";


    String ASK_TO_CANCEL_VPN = "ask_to_cancel_vpn";
    String EXTRA_MOTD_MSG = "extra_motd_message";


    //////////////////////////////////////////////
    // EIP CONSTANTS
    /////////////////////////////////////////////

    String EIP_ACTION_CHECK_CERT_VALIDITY = "EIP.CHECK_CERT_VALIDITY";
    String EIP_ACTION_START = "se.leap.bitmaskclient.EIP.START";
    String EIP_ACTION_STOP = "se.leap.bitmaskclient.EIP.STOP";
    String EIP_ACTION_IS_RUNNING = "se.leap.bitmaskclient.EIP.IS_RUNNING";
    String EIP_ACTION_START_ALWAYS_ON_VPN = "se.leap.bitmaskclient.START_ALWAYS_ON_VPN";
    String EIP_ACTION_START_BLOCKING_VPN = "se.leap.bitmaskclient.EIP_ACTION_START_BLOCKING_VPN";
    String EIP_ACTION_STOP_BLOCKING_VPN = "se.leap.bitmaskclient.EIP_ACTION_STOP_BLOCKING_VPN";
    String EIP_ACTION_PREPARE_VPN = "se.leap.bitmaskclient.EIP_ACTION_PREPARE_VPN";
    String EIP_ACTION_LAUNCH_VPN = "se.leap.bitmaskclient.EIP_ACTION_LAUNCH_VPN";

    String EIP_RECEIVER = "EIP.RECEIVER";
    String EIP_REQUEST = "EIP.REQUEST";
    String EIP_RESTART_ON_BOOT = "EIP.RESTART_ON_BOOT";
    String EIP_IS_ALWAYS_ON = "EIP.EIP_IS_ALWAYS_ON";
    String EIP_EARLY_ROUTES = "EIP.EARLY_ROUTES";
    String EIP_N_CLOSEST_GATEWAY = "EIP.N_CLOSEST_GATEWAY";


    //////////////////////////////////////////////
    // PROVIDER CONSTANTS
    /////////////////////////////////////////////

    String PROVIDER_ALLOW_ANONYMOUS = "allow_anonymous";
    String PROVIDER_ALLOWED_REGISTERED = "allow_registration";
    String PROVIDER_VPN_CERTIFICATE = "cert";
    String PROVIDER_PRIVATE_KEY = "Constants.PROVIDER_PRIVATE_KEY";
    String PROVIDER_KEY = "Constants.PROVIDER_KEY";
    String PROVIDER_CONFIGURED = "Constants.PROVIDER_CONFIGURED";
    String PROVIDER_EIP_DEFINITION = "Constants.EIP_DEFINITION";
    String PROVIDER_PROFILE_UUID = "Constants.PROVIDER_PROFILE_UUID";
    String PROVIDER_PROFILE = "Constants.PROVIDER_PROFILE";
    String PROVIDER_MOTD = "Constants.PROVIDER_MOTD";
    String PROVIDER_MOTD_HASHES = "Constants.PROVIDER_MOTD_HASHES";
    String PROVIDER_MOTD_LAST_SEEN = "Constants.PROVIDER_MOTD_LAST_SEEN";
    String PROVIDER_MOTD_LAST_UPDATED = "Constants.PROVIDER_MOTD_LAST_UPDATED";
    String PROVIDER_MODELS_PROVIDER = "Constants.PROVIDER_MODELS_PROVIDER";
    String PROVIDER_MODELS_EIPSERVICE = "Constants.PROVIDER_MDOELS_EIPSERVICE";
    String PROVIDER_MODELS_GATEWAYS = "Constants.PROVIDER_MODELS_GATEWAYS";
    String PROVIDER_MODELS_BRIDGES = "Constants.PROVIDER_MODELS_BRIDGES";

    ////////////////////////////////////////////////
    // PRESHIPPED PROVIDER CONFIG
    ////////////////////////////////////////////////
    String URLS = "urls";
    String EXT_JSON = ".json";
    String EXT_PEM = ".pem";

    //////////////////////////////////////////////
    // CREDENTIAL CONSTANTS
    /////////////////////////////////////////////

    String CREDENTIALS_USERNAME = "username";
    String CREDENTIALS_PASSWORD = "password";

    enum CREDENTIAL_ERRORS {
        USERNAME_MISSING,
        PASSWORD_INVALID_LENGTH,
        RISEUP_WARNING
    }

    //////////////////////////////////////////////
    // BROADCAST CONSTANTS
    /////////////////////////////////////////////

    String BROADCAST_EIP_EVENT = "BROADCAST.EIP_EVENT";
    String BROADCAST_PROVIDER_API_EVENT = "BROADCAST.PROVIDER_API_EVENT";
    String BROADCAST_GATEWAY_SETUP_OBSERVER_EVENT = "BROADCAST.GATEWAY_SETUP_WATCHER_EVENT";
    String BROADCAST_RESULT_CODE = "BROADCAST.RESULT_CODE";
    String BROADCAST_RESULT_KEY = "BROADCAST.RESULT_KEY";
    String BROADCAST_DOWNLOAD_SERVICE_EVENT = "BROADCAST.DOWNLOAD_SERVICE_EVENT";


    //////////////////////////////////////////////
    // ICS-OPENVPN CONSTANTS
    /////////////////////////////////////////////
    String DEFAULT_SHARED_PREFS_BATTERY_SAVER = "screenoff";

    //////////////////////////////////////////////
    // CUSTOM CONSTANTS
    /////////////////////////////////////////////
    boolean ENABLE_DONATION = BuildConfig.enable_donation;
    boolean ENABLE_DONATION_REMINDER = BuildConfig.enable_donation_reminder;
    int DONATION_REMINDER_DURATION = BuildConfig.donation_reminder_duration;
    String DONATION_URL = TextUtils.isEmpty(BuildConfig.donation_url) ?
            BuildConfig.default_donation_url : BuildConfig.donation_url;
    String LAST_DONATION_REMINDER_DATE = "last_donation_reminder_date";
    String FIRST_TIME_USER_DATE = "first_time_user_date";


    //////////////////////////////////////////////
    // JSON KEYS
    /////////////////////////////////////////////
    String IP_ADDRESS = "ip_address";
    String IP_ADDRESS6 = "ip_address6";
    String REMOTE = "remote";
    String SOCKS_PROXY = "socks-proxy";
    String PORTS = "ports";
    String PROTOCOLS = "protocols";
    String UDP = "udp";
    String TCP = "tcp";
    String KCP = "kcp";
    String CAPABILITIES = "capabilities";
    String TRANSPORT = "transport";
    String TYPE = "type";
    String OPTIONS = "options";
    String IAT_MODE = "iatMode";
    String CERT = "cert";
    String CERTS = "certs";
    String ENDPOINTS = "endpoints";
    String PORT_SEED = "port_seed";
    String PORT_COUNT = "port_count";
    String EXPERIMENTAL = "experimental";
    String VERSION = "version";
    String NAME = "name";
    String TIMEZONE = "timezone";
    String LOCATIONS = "locations";
    String LOCATION = "location";
    String OPENVPN_CONFIGURATION = "openvpn_configuration";
    String GATEWAYS = "gateways";
    String HOST = "host";
    String SORTED_GATEWAYS = "sortedGateways";
    String FULLNESS = "fullness";
    String OVERLOAD = "overload";
}
