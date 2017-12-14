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
    String EIP_ACTION_START = "EIP.START";
    String EIP_ACTION_STOP = "EIP.STOP";
    String EIP_ACTION_UPDATE = "EIP.UPDATE";
    String EIP_ACTION_IS_RUNNING = "EIP.IS_RUNNING";
    String EIP_ACTION_BLOCK_VPN_PROFILE = "EIP.ACTION_BLOCK_VPN_PROFILE";

    String EIP_NOTIFICATION = "EIP.NOTIFICATION";
    String EIP_RECEIVER = "EIP.RECEIVER";
    String EIP_REQUEST = "EIP.REQUEST";


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
