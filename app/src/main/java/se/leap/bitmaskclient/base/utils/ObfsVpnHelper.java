package se.leap.bitmaskclient.base.utils;

import androidx.annotation.VisibleForTesting;

import de.blinkt.openvpn.core.NativeUtils;
import se.leap.bitmaskclient.BuildConfig;

// ObfsVpnHelper class allows us to mock BuildConfig fields related to the pre-shipped circumvention settings
public class ObfsVpnHelper {

    public interface ObfsVpnHelperInterface {
        boolean useObfsVpn();
        boolean hasObfuscationPinningDefaults();
        String obfsvpnIP();
        String obfsvpnPort();
        String obfsvpnCert();
        boolean useKcp();
    }

    public static class DefaultObfsVpnHelper implements ObfsVpnHelperInterface {
        @Override
        public boolean useObfsVpn() {
            return BuildConfig.use_obfsvpn;
        }

        @Override
        public boolean hasObfuscationPinningDefaults() {
            return BuildConfig.obfsvpn_ip != null &&
                    BuildConfig.obfsvpn_port != null &&
                    BuildConfig.obfsvpn_cert != null &&
                    !BuildConfig.obfsvpn_ip.isEmpty() &&
                    !BuildConfig.obfsvpn_port.isEmpty() &&
                    !BuildConfig.obfsvpn_cert.isEmpty();
        }

        @Override
        public String obfsvpnIP() {
            return BuildConfig.obfsvpn_ip;
        }

        @Override
        public String obfsvpnPort() {
            return BuildConfig.obfsvpn_port;
        }

        @Override
        public String obfsvpnCert() {
            return BuildConfig.obfsvpn_cert;
        }

        @Override
        public boolean useKcp() {
            return BuildConfig.obfsvpn_use_kcp;
        }
    }

    private static ObfsVpnHelperInterface instance = new DefaultObfsVpnHelper();

    @VisibleForTesting
    public ObfsVpnHelper(ObfsVpnHelperInterface helperInterface) {
        if (!NativeUtils.isUnitTest()) {
            throw new IllegalStateException("ObfsVpnHelper injected with ObfsVpnHelperInterface outside of an unit test");
        }
        instance = helperInterface;
    }

    public static boolean useObfsVpn() {
        return instance.useObfsVpn();
    }

    public static boolean hasObfuscationPinningDefaults() {
        return instance.hasObfuscationPinningDefaults();
    }
    public static String obfsvpnIP() {
        return instance.obfsvpnIP();
    }
    public static String obfsvpnPort() {
        return instance.obfsvpnPort();
    }
    public static String obfsvpnCert() {
        return instance.obfsvpnCert();
    }
    public static boolean useKcp() {
        return instance.useKcp();
    }
}
