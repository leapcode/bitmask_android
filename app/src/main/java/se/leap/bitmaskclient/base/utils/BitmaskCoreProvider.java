package se.leap.bitmaskclient.base.utils;

import de.blinkt.openvpn.core.NativeUtils;
import mobile.BitmaskMobile;
import mobilemodels.BitmaskMobileCore;

public class BitmaskCoreProvider {
    private static BitmaskMobileCore customMobileCore;
    public static BitmaskMobileCore getBitmaskMobile() {
        if (customMobileCore == null) {
            return new BitmaskMobile(new PreferenceHelper.SharedPreferenceStore());
        }
        return customMobileCore;
    }

    public static void initBitmaskMobile(BitmaskMobileCore bitmaskMobileCore) {
        if (!NativeUtils.isUnitTest()) {
            throw new IllegalStateException("Initializing custom BitmaskMobileCore implementation outside of an unit test is not allowed");
        }
        BitmaskCoreProvider.customMobileCore = bitmaskMobileCore;
    }
}