package se.leap.bitmaskclient;

import android.text.TextUtils;

public interface Constants {

    //////////////////////////////////////////////
    // PREFERENCES CONSTANTS
    /////////////////////////////////////////////

    String SHARED_PREFERENCES = "LEAPPreferences";
    String PREFERENCES_APP_VERSION = "bitmask version";
    String ALWAYS_ON_SHOW_DIALOG = "DIALOG.ALWAYS_ON_SHOW_DIALOG";


     //////////////////////////////////////////////
    // REQUEST CODE CONSTANTS
    /////////////////////////////////////////////

    String REQUEST_CODE_KEY = "request_code";
    int REQUEST_CODE_CONFIGURE_LEAP = 0;
    int REQUEST_CODE_SWITCH_PROVIDER = 1;
    int REQUEST_CODE_LOG_IN = 2;
    int REQUEST_CODE_ADD_PROVIDER = 3;


    //////////////////////////////////////////////
    // APP CONSTANTS
    /////////////////////////////////////////////

    String APP_ACTION_QUIT = "quit";
    String APP_ACTION_CONFIGURE_ALWAYS_ON_PROFILE = "configure always-on profile";


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

    String EIP_RECEIVER = "EIP.RECEIVER";
    String EIP_REQUEST = "EIP.REQUEST";
    String EIP_RESTART_ON_BOOT = "EIP.RESTART_ON_BOOT";
    String EIP_IS_ALWAYS_ON = "EIP.EIP_IS_ALWAYS_ON";
    String EIP_EARLY_ROUTES = "EIP.EARLY_ROUTES";


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
    String BROADCAST_RESULT_CODE = "BROADCAST.RESULT_CODE";
    String BROADCAST_RESULT_KEY = "BROADCAST.RESULT_KEY";


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


}
