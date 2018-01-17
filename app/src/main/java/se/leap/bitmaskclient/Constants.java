package se.leap.bitmaskclient;

public interface Constants {

    //////////////////////////////////////////////
    // PREFERENCES CONSTANTS
    /////////////////////////////////////////////

    String SHARED_PREFERENCES = "LEAPPreferences";
    String PREFERENCES_APP_VERSION = "bitmask version";


    //////////////////////////////////////////////
    // EIP CONSTANTS
    /////////////////////////////////////////////

    String EIP_ACTION_CHECK_CERT_VALIDITY = "EIP.CHECK_CERT_VALIDITY";
    String EIP_ACTION_START = "se.leap.bitmaskclient.EIP.START";
    String EIP_ACTION_STOP = "se.leap.bitmaskclient.EIP.STOP";
    String EIP_ACTION_UPDATE = "se.leap.bitmaskclient.EIP.UPDATE";
    String EIP_ACTION_IS_RUNNING = "se.leap.bitmaskclient.EIP.IS_RUNNING";
    String EIP_ACTION_START_ALWAYS_ON_EIP = "se.leap.bitmaskclient.START_ALWAYS_ON_EIP";
    String EIP_ACTION_START_BLOCKING_VPN = "se.leap.bitmaskclient.EIP_ACTION_START_BLOCKING_VPN";
    String EIP_ACTION_STOP_BLOCKING_VPN = "se.leap.bitmaskclient.EIP_ACTION_STOP_BLOCKING_VPN";

    String EIP_NOTIFICATION = "EIP.NOTIFICATION";
    String EIP_RECEIVER = "EIP.RECEIVER";
    String EIP_REQUEST = "EIP.REQUEST";
    String EIP_RESTART_ON_BOOT = "EIP.RESTART_ON_BOOT";
    String EIP_IS_ALWAYS_ON = "EIP.EIP_IS_ALWAYS_ON";



    //////////////////////////////////////////////
    // PROVIDER CONSTANTS
    /////////////////////////////////////////////
    String PROVIDER_ALLOW_ANONYMOUS = "allow_anonymous";
    String PROVIDER_ALLOWED_REGISTERED = "allow_registration";
    String PROVIDER_VPN_CERTIFICATE = "cert";
    String PROVIDER_PRIVATE_KEY = "Constants.PROVIDER_PRIVATE_KEY";
    String PROVIDER_KEY = "Constants.PROVIDER_KEY";
    String PROVIDER_CONFIGURED = "Constants.PROVIDER_CONFIGURED";
}
