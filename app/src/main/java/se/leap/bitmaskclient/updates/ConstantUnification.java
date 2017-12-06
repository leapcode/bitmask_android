package se.leap.bitmaskclient.updates;

import android.content.SharedPreferences;

public class ConstantUnification {

    private String OLD_ALLOWED_ANON = "allow_anonymous";
    private String OLD_ALLOWED_REGISTERED = "allow_registration";
    private String OLD_VPN_CERTIFICATE = "cert";
    private String OLD_PRIVATE_KEY = "Constants.PRIVATE_KEY";
    private String OLD_KEY = "Constants.KEY";
    private String OLD_PROVIDER_CONFIGURED = "Constants.PROVIDER_CONFIGURED";

    public static void upgrade(SharedPreferences preferences) {
        // TODO MOVE SAVED DATA
    }

}
