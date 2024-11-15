package se.leap.bitmaskclient.eip;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static de.blinkt.openvpn.core.connection.Connection.TransportType.OBFS4;
import static de.blinkt.openvpn.core.connection.Connection.TransportType.OBFS4_HOP;
import static de.blinkt.openvpn.core.connection.Connection.TransportType.OPENVPN;
import static se.leap.bitmaskclient.base.models.Constants.IP_ADDRESS;
import static se.leap.bitmaskclient.base.models.Constants.IP_ADDRESS6;
import static se.leap.bitmaskclient.base.models.Constants.OPENVPN_CONFIGURATION;
import static se.leap.bitmaskclient.base.models.Transport.createTransportsFrom;
import static se.leap.bitmaskclient.eip.VpnConfigGenerator.Configuration.createProfileConfig;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.Vector;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.connection.Connection;
import de.blinkt.openvpn.core.connection.Obfs4Connection;
import se.leap.bitmaskclient.base.models.ProviderObservable;
import se.leap.bitmaskclient.base.models.Transport;
import se.leap.bitmaskclient.base.utils.BuildConfigHelper;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.base.utils.PrivateKeyHelper;
import se.leap.bitmaskclient.testutils.MockHelper;
import se.leap.bitmaskclient.testutils.MockSharedPreferences;
import se.leap.bitmaskclient.testutils.TestSetupHelper;

/**
 * Created by cyberta on 03.10.17.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.P})
public class VpnConfigGeneratorTest {

    Context context;
    PreferenceHelper preferenceHelper;
    private SharedPreferences sharedPreferences;

    private VpnConfigGenerator vpnConfigGenerator;
    private JSONObject generalConfig;
    private JSONObject gateway;
    private JSONObject secrets;

    String expectedVPNConfig_v1_tcp_udp = "# Config for OpenVPN 2.x\n" +
            "# Enables connection to GUI\n" +
            "management /data/data/se.leap.bitmask/mgmtsocket unix\n" +
            "management-client\n" +
            "management-query-passwords\n" +
            "management-hold\n" +
            "\n" +
            "setenv IV_GUI_VER \"se.leap.bitmaskclient 0.9.10\" \n" +
            "setenv IV_PLAT_VER \"28 9 ROBO Android unknown robolectric\"\n" +
            "machine-readable-output\n" +
            "allow-recursive-routing\n" +
            "ifconfig-nowarn\n" +
            "client\n" +
            "verb 4\n" +
            "connect-retry 2 300\n" +
            "resolv-retry 60\n" +
            "dev tun\n" +
            "remote 198.252.153.84 443 tcp-client\n" +
            "remote 198.252.153.84 443 udp\n" +
            "<ca>\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIFbzCCA1egAwIBAgIBATANBgkqhkiG9w0BAQ0FADBKMRgwFgYDVQQDDA9CaXRt\n" +
            "YXNrIFJvb3QgQ0ExEDAOBgNVBAoMB0JpdG1hc2sxHDAaBgNVBAsME2h0dHBzOi8v\n" +
            "Yml0bWFzay5uZXQwHhcNMTIxMTA2MDAwMDAwWhcNMjIxMTA2MDAwMDAwWjBKMRgw\n" +
            "FgYDVQQDDA9CaXRtYXNrIFJvb3QgQ0ExEDAOBgNVBAoMB0JpdG1hc2sxHDAaBgNV\n" +
            "BAsME2h0dHBzOi8vYml0bWFzay5uZXQwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAw\n" +
            "ggIKAoICAQC1eV4YvayaU+maJbWrD4OHo3d7S1BtDlcvkIRS1Fw3iYDjsyDkZxai\n" +
            "dHp4EUasfNQ+EVtXUvtk6170EmLco6Elg8SJBQ27trE6nielPRPCfX3fQzETRfvB\n" +
            "7tNvGw4Jn2YKiYoMD79kkjgyZjkJ2r/bEHUSevmR09BRp86syHZerdNGpXYhcQ84\n" +
            "CA1+V+603GFIHnrP+uQDdssW93rgDNYu+exT+Wj6STfnUkugyjmPRPjL7wh0tzy+\n" +
            "znCeLl4xiV3g9sjPnc7r2EQKd5uaTe3j71sDPF92KRk0SSUndREz+B1+Dbe/RGk4\n" +
            "MEqGFuOzrtsgEhPIX0hplhb0Tgz/rtug+yTT7oJjBa3u20AAOQ38/M99EfdeJvc4\n" +
            "lPFF1XBBLh6X9UKF72an2NuANiX6XPySnJgZ7nZ09RiYZqVwu/qt3DfvLfhboq+0\n" +
            "bQvLUPXrVDr70onv5UDjpmEA/cLmaIqqrduuTkFZOym65/PfAPvpGnt7crQj/Ibl\n" +
            "DEDYZQmP7AS+6zBjoOzNjUGE5r40zWAR1RSi7zliXTu+yfsjXUIhUAWmYR6J3KxB\n" +
            "lfsiHBQ+8dn9kC3YrUexWoOqBiqJOAJzZh5Y1tqgzfh+2nmHSB2dsQRs7rDRRlyy\n" +
            "YMbkpzL9ZsOUO2eTP1mmar6YjCN+rggYjRrX71K2SpBG6b1zZxOG+wIDAQABo2Aw\n" +
            "XjAdBgNVHQ4EFgQUuYGDLL2sswnYpHHvProt1JU+D48wDgYDVR0PAQH/BAQDAgIE\n" +
            "MAwGA1UdEwQFMAMBAf8wHwYDVR0jBBgwFoAUuYGDLL2sswnYpHHvProt1JU+D48w\n" +
            "DQYJKoZIhvcNAQENBQADggIBADeG67vaFcbITGpi51264kHPYPEWaXUa5XYbtmBl\n" +
            "cXYyB6hY5hv/YNuVGJ1gWsDmdeXEyj0j2icGQjYdHRfwhrbEri+h1EZOm1cSBDuY\n" +
            "k/P5+ctHyOXx8IE79DBsZ6IL61UKIaKhqZBfLGYcWu17DVV6+LT+AKtHhOrv3TSj\n" +
            "RnAcKnCbKqXLhUPXpK0eTjPYS2zQGQGIhIy9sQXVXJJJsGrPgMxna1Xw2JikBOCG\n" +
            "htD/JKwt6xBmNwktH0GI/LVtVgSp82Clbn9C4eZN9E5YbVYjLkIEDhpByeC71QhX\n" +
            "EIQ0ZR56bFuJA/CwValBqV/G9gscTPQqd+iETp8yrFpAVHOW+YzSFbxjTEkBte1J\n" +
            "aF0vmbqdMAWLk+LEFPQRptZh0B88igtx6tV5oVd+p5IVRM49poLhuPNJGPvMj99l\n" +
            "mlZ4+AeRUnbOOeAEuvpLJbel4rhwFzmUiGoeTVoPZyMevWcVFq6BMkS+jRR2w0jK\n" +
            "G6b0v5XDHlcFYPOgUrtsOBFJVwbutLvxdk6q37kIFnWCd8L3kmES5q4wjyFK47Co\n" +
            "Ja8zlx64jmMZPg/t3wWqkZgXZ14qnbyG5/lGsj5CwVtfDljrhN0oCWK1FZaUmW3d\n" +
            "69db12/g4f6phldhxiWuGC/W6fCW5kre7nmhshcltqAJJuU47iX+DarBFiIj816e\n" +
            "yV8e\n" +
            "-----END CERTIFICATE-----\n" +
            "\n" +
            "</ca>\n" +
            "<cert>\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIEjDCCAnSgAwIBAgIQG6MBp/cd9DlY+7cdvp3R3jANBgkqhkiG9w0BAQsFADBmMRAwDgYDVQQK\n" +
            "DAdCaXRtYXNrMRwwGgYDVQQLDBNodHRwczovL2JpdG1hc2submV0MTQwMgYDVQQDDCtCaXRtYXNr\n" +
            "IFJvb3QgQ0EgKGNsaWVudCBjZXJ0aWZpY2F0ZXMgb25seSEpMB4XDTE0MTIwNTAwMDAwMFoXDTE1\n" +
            "MDMwNTAwMDAwMFowLTErMCkGA1UEAwwiVU5MSU1JVEVEZDBwZDdkMzE4eTNtOHNkeXllaTFqYmZl\n" +
            "eDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANRNhZ4aCwdL5+OKObOKeI2rDqEwGnIr\n" +
            "hL9wzo/FXbwLfdW45Y9Mxwhh6xy2NkA1YUKCB8VNBKNXlBrGr1QriLbu1rItsJ2VVLqGluVV/gO4\n" +
            "jcaPU+/Wu0hMFKG28J/dPvIGeNbjBWk6mxQAA5WIpRK9RTeQ88wVaGIZDDzIdivza2zpcyiPAyii\n" +
            "dbkyXh7sLsKvbZB6wLrert6Y1ylR3SlkZP0LfdGAMAdkMyuXKOjgcSnUltR8HSBuZcSUlsTVM11n\n" +
            "rYeGCYyPNNQ3UYatDW33UASgRDBorrmjhhKP7IW/opdlnPk5ZrP3i0qI32/boRe0EWZGXJvr4P3K\n" +
            "dJ30uCECAwEAAaNvMG0wHQYDVR0OBBYEFK8bMVAM4GBB5sHptoIOAaIvlYueMAsGA1UdDwQEAwIH\n" +
            "gDATBgNVHSUEDDAKBggrBgEFBQcDAjAJBgNVHRMEAjAAMB8GA1UdIwQYMBaAFId+E7bsWFsUWah9\n" +
            "vZuPvZ7O+aJsMA0GCSqGSIb3DQEBCwUAA4ICAQAQOX81csVhvP422NKkZH7+g3npBpl+sEHedaGR\n" +
            "xYPOu4HrA4TVF9h44sljRoRJyenGNdBZCXcLKHg889eePTf8Z5K3lTojp6hvwyA6tgxOMHT1kESW\n" +
            "PfqnRw8mHfHJuE3g+4YNUMwggzwc/VZATdV/7M33sarVN9AUOHou9n9BizgCC+UnYlS+F2POumE3\n" +
            "FbOhKo5uubI02MwBYlN2JVO2TBt1Q20w8wc6cU07Xi5Epp+1mkgFiOShkNtPcJmEyBWJhxDtSDOW\n" +
            "2doqWYNqH2kq7B5R/kyyfcpFJqAnBTV7xs+C5rTS1mW7LpxfdCUMbYuLCpyxpO3A/DhAm8n47tUH\n" +
            "lBtmo8Avdb8VdFpYiGBpB0o9kTFcsWFb2GkWFBduGfSEB8jUI7QtqhgZqocAKK/cweSRV8FwyUcn\n" +
            "R0prRm3QEi9fbXqEddzjSY9y/lqWYzT7u+IOAQpKroeZ4wzgYperDNOUFuYk1rP7yuvjP2pV5rcN\n" +
            "yPoBP60TPVWMRM4WJm6nTogAz2qBrFsf/XwT/ajzbsjT6HNB7QbRE+wkFkqspoXG5Agp7KQ8lW3L\n" +
            "SKCDGOQJz7VIE85pD0tg7QEXBEw8oaRZtMjQ0Gvs25mxXAKka4wGasaWfYH6d0E+iKYcWn86V1rH\n" +
            "K2ZoknT+Nno5jgjFuUR3fZseNizEfx7BteooKQ==\n" +
            "-----END CERTIFICATE-----\n" +
            "</cert>\n" +
            "management-external-key nopadding pkcs1 pss digest\n" +
            //"# crl-verify file missing in config profile\n" +
            "remote-cert-tls server\n" +
            "data-ciphers AES-128-CBC\n" +
            "cipher AES-128-CBC\n" +
            "auth SHA1\n" +
            "persist-tun\n" +
            "# persist-tun also enables pre resolving to avoid DNS resolve problem\n" +
            "preresolve\n" +
            "# Use system proxy setting\n" +
            "management-query-proxy\n" +
            "# Custom configuration options\n" +
            "# You are on your on own here :)\n" +
            "# These options found in the config file do not map to config settings:\n" +
            "keepalive 10 30 \n" +
            "tls-cipher DHE-RSA-AES128-SHA";

    String expectedVPNConfig_v1_udp_tcp = "# Config for OpenVPN 2.x\n" +
            "# Enables connection to GUI\n" +
            "management /data/data/se.leap.bitmask/mgmtsocket unix\n" +
            "management-client\n" +
            "management-query-passwords\n" +
            "management-hold\n" +
            "\n" +
            "setenv IV_GUI_VER \"se.leap.bitmaskclient 0.9.10\" \n" +
            "setenv IV_PLAT_VER \"28 9 ROBO Android unknown robolectric\"\n" +
            "machine-readable-output\n" +
            "allow-recursive-routing\n" +
            "ifconfig-nowarn\n" +
            "client\n" +
            "verb 4\n" +
            "connect-retry 2 300\n" +
            "resolv-retry 60\n" +
            "dev tun\n" +
            "remote 198.252.153.84 443 udp\n" +
            "remote 198.252.153.84 443 tcp-client\n" +
            "<ca>\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIFbzCCA1egAwIBAgIBATANBgkqhkiG9w0BAQ0FADBKMRgwFgYDVQQDDA9CaXRt\n" +
            "YXNrIFJvb3QgQ0ExEDAOBgNVBAoMB0JpdG1hc2sxHDAaBgNVBAsME2h0dHBzOi8v\n" +
            "Yml0bWFzay5uZXQwHhcNMTIxMTA2MDAwMDAwWhcNMjIxMTA2MDAwMDAwWjBKMRgw\n" +
            "FgYDVQQDDA9CaXRtYXNrIFJvb3QgQ0ExEDAOBgNVBAoMB0JpdG1hc2sxHDAaBgNV\n" +
            "BAsME2h0dHBzOi8vYml0bWFzay5uZXQwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAw\n" +
            "ggIKAoICAQC1eV4YvayaU+maJbWrD4OHo3d7S1BtDlcvkIRS1Fw3iYDjsyDkZxai\n" +
            "dHp4EUasfNQ+EVtXUvtk6170EmLco6Elg8SJBQ27trE6nielPRPCfX3fQzETRfvB\n" +
            "7tNvGw4Jn2YKiYoMD79kkjgyZjkJ2r/bEHUSevmR09BRp86syHZerdNGpXYhcQ84\n" +
            "CA1+V+603GFIHnrP+uQDdssW93rgDNYu+exT+Wj6STfnUkugyjmPRPjL7wh0tzy+\n" +
            "znCeLl4xiV3g9sjPnc7r2EQKd5uaTe3j71sDPF92KRk0SSUndREz+B1+Dbe/RGk4\n" +
            "MEqGFuOzrtsgEhPIX0hplhb0Tgz/rtug+yTT7oJjBa3u20AAOQ38/M99EfdeJvc4\n" +
            "lPFF1XBBLh6X9UKF72an2NuANiX6XPySnJgZ7nZ09RiYZqVwu/qt3DfvLfhboq+0\n" +
            "bQvLUPXrVDr70onv5UDjpmEA/cLmaIqqrduuTkFZOym65/PfAPvpGnt7crQj/Ibl\n" +
            "DEDYZQmP7AS+6zBjoOzNjUGE5r40zWAR1RSi7zliXTu+yfsjXUIhUAWmYR6J3KxB\n" +
            "lfsiHBQ+8dn9kC3YrUexWoOqBiqJOAJzZh5Y1tqgzfh+2nmHSB2dsQRs7rDRRlyy\n" +
            "YMbkpzL9ZsOUO2eTP1mmar6YjCN+rggYjRrX71K2SpBG6b1zZxOG+wIDAQABo2Aw\n" +
            "XjAdBgNVHQ4EFgQUuYGDLL2sswnYpHHvProt1JU+D48wDgYDVR0PAQH/BAQDAgIE\n" +
            "MAwGA1UdEwQFMAMBAf8wHwYDVR0jBBgwFoAUuYGDLL2sswnYpHHvProt1JU+D48w\n" +
            "DQYJKoZIhvcNAQENBQADggIBADeG67vaFcbITGpi51264kHPYPEWaXUa5XYbtmBl\n" +
            "cXYyB6hY5hv/YNuVGJ1gWsDmdeXEyj0j2icGQjYdHRfwhrbEri+h1EZOm1cSBDuY\n" +
            "k/P5+ctHyOXx8IE79DBsZ6IL61UKIaKhqZBfLGYcWu17DVV6+LT+AKtHhOrv3TSj\n" +
            "RnAcKnCbKqXLhUPXpK0eTjPYS2zQGQGIhIy9sQXVXJJJsGrPgMxna1Xw2JikBOCG\n" +
            "htD/JKwt6xBmNwktH0GI/LVtVgSp82Clbn9C4eZN9E5YbVYjLkIEDhpByeC71QhX\n" +
            "EIQ0ZR56bFuJA/CwValBqV/G9gscTPQqd+iETp8yrFpAVHOW+YzSFbxjTEkBte1J\n" +
            "aF0vmbqdMAWLk+LEFPQRptZh0B88igtx6tV5oVd+p5IVRM49poLhuPNJGPvMj99l\n" +
            "mlZ4+AeRUnbOOeAEuvpLJbel4rhwFzmUiGoeTVoPZyMevWcVFq6BMkS+jRR2w0jK\n" +
            "G6b0v5XDHlcFYPOgUrtsOBFJVwbutLvxdk6q37kIFnWCd8L3kmES5q4wjyFK47Co\n" +
            "Ja8zlx64jmMZPg/t3wWqkZgXZ14qnbyG5/lGsj5CwVtfDljrhN0oCWK1FZaUmW3d\n" +
            "69db12/g4f6phldhxiWuGC/W6fCW5kre7nmhshcltqAJJuU47iX+DarBFiIj816e\n" +
            "yV8e\n" +
            "-----END CERTIFICATE-----\n" +
            "\n" +
            "</ca>\n" +
            "<cert>\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIEjDCCAnSgAwIBAgIQG6MBp/cd9DlY+7cdvp3R3jANBgkqhkiG9w0BAQsFADBmMRAwDgYDVQQK\n" +
            "DAdCaXRtYXNrMRwwGgYDVQQLDBNodHRwczovL2JpdG1hc2submV0MTQwMgYDVQQDDCtCaXRtYXNr\n" +
            "IFJvb3QgQ0EgKGNsaWVudCBjZXJ0aWZpY2F0ZXMgb25seSEpMB4XDTE0MTIwNTAwMDAwMFoXDTE1\n" +
            "MDMwNTAwMDAwMFowLTErMCkGA1UEAwwiVU5MSU1JVEVEZDBwZDdkMzE4eTNtOHNkeXllaTFqYmZl\n" +
            "eDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANRNhZ4aCwdL5+OKObOKeI2rDqEwGnIr\n" +
            "hL9wzo/FXbwLfdW45Y9Mxwhh6xy2NkA1YUKCB8VNBKNXlBrGr1QriLbu1rItsJ2VVLqGluVV/gO4\n" +
            "jcaPU+/Wu0hMFKG28J/dPvIGeNbjBWk6mxQAA5WIpRK9RTeQ88wVaGIZDDzIdivza2zpcyiPAyii\n" +
            "dbkyXh7sLsKvbZB6wLrert6Y1ylR3SlkZP0LfdGAMAdkMyuXKOjgcSnUltR8HSBuZcSUlsTVM11n\n" +
            "rYeGCYyPNNQ3UYatDW33UASgRDBorrmjhhKP7IW/opdlnPk5ZrP3i0qI32/boRe0EWZGXJvr4P3K\n" +
            "dJ30uCECAwEAAaNvMG0wHQYDVR0OBBYEFK8bMVAM4GBB5sHptoIOAaIvlYueMAsGA1UdDwQEAwIH\n" +
            "gDATBgNVHSUEDDAKBggrBgEFBQcDAjAJBgNVHRMEAjAAMB8GA1UdIwQYMBaAFId+E7bsWFsUWah9\n" +
            "vZuPvZ7O+aJsMA0GCSqGSIb3DQEBCwUAA4ICAQAQOX81csVhvP422NKkZH7+g3npBpl+sEHedaGR\n" +
            "xYPOu4HrA4TVF9h44sljRoRJyenGNdBZCXcLKHg889eePTf8Z5K3lTojp6hvwyA6tgxOMHT1kESW\n" +
            "PfqnRw8mHfHJuE3g+4YNUMwggzwc/VZATdV/7M33sarVN9AUOHou9n9BizgCC+UnYlS+F2POumE3\n" +
            "FbOhKo5uubI02MwBYlN2JVO2TBt1Q20w8wc6cU07Xi5Epp+1mkgFiOShkNtPcJmEyBWJhxDtSDOW\n" +
            "2doqWYNqH2kq7B5R/kyyfcpFJqAnBTV7xs+C5rTS1mW7LpxfdCUMbYuLCpyxpO3A/DhAm8n47tUH\n" +
            "lBtmo8Avdb8VdFpYiGBpB0o9kTFcsWFb2GkWFBduGfSEB8jUI7QtqhgZqocAKK/cweSRV8FwyUcn\n" +
            "R0prRm3QEi9fbXqEddzjSY9y/lqWYzT7u+IOAQpKroeZ4wzgYperDNOUFuYk1rP7yuvjP2pV5rcN\n" +
            "yPoBP60TPVWMRM4WJm6nTogAz2qBrFsf/XwT/ajzbsjT6HNB7QbRE+wkFkqspoXG5Agp7KQ8lW3L\n" +
            "SKCDGOQJz7VIE85pD0tg7QEXBEw8oaRZtMjQ0Gvs25mxXAKka4wGasaWfYH6d0E+iKYcWn86V1rH\n" +
            "K2ZoknT+Nno5jgjFuUR3fZseNizEfx7BteooKQ==\n" +
            "-----END CERTIFICATE-----\n" +
            "</cert>\n" +
            "management-external-key nopadding pkcs1 pss digest\n" +
            //"# crl-verify file missing in config profile\n" +
            "remote-cert-tls server\n" +
            "data-ciphers AES-128-CBC\n" +
            "cipher AES-128-CBC\n" +
            "auth SHA1\n" +
            "persist-tun\n" +
            "# persist-tun also enables pre resolving to avoid DNS resolve problem\n" +
            "preresolve\n" +
            "# Use system proxy setting\n" +
            "management-query-proxy\n" +
            "# Custom configuration options\n" +
            "# You are on your on own here :)\n" +
            "# These options found in the config file do not map to config settings:\n" +
            "keepalive 10 30 \n" +
            "tls-cipher DHE-RSA-AES128-SHA";

    String expectedVPNConfig_v3_obfs4 = "# Config for OpenVPN 2.x\n" +
            "# Enables connection to GUI\n" +
            "management /data/data/se.leap.bitmask/mgmtsocket unix\n" +
            "management-client\n" +
            "management-query-passwords\n" +
            "management-hold\n" +
            "\n" +
            "setenv IV_GUI_VER \"se.leap.bitmaskclient 0.9.10\" \n" +
            "setenv IV_PLAT_VER \"28 9 ROBO Android unknown robolectric\"\n" +
            "machine-readable-output\n" +
            "allow-recursive-routing\n" +
            "ifconfig-nowarn\n" +
            "client\n" +
            "verb 4\n" +
            "connect-retry 2 300\n" +
            "resolv-retry 60\n" +
            "dev tun\n" +
            "remote 127.0.0.1 8080 udp\n" +
            "<ca>\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIFbzCCA1egAwIBAgIBATANBgkqhkiG9w0BAQ0FADBKMRgwFgYDVQQDDA9CaXRt\n" +
            "YXNrIFJvb3QgQ0ExEDAOBgNVBAoMB0JpdG1hc2sxHDAaBgNVBAsME2h0dHBzOi8v\n" +
            "Yml0bWFzay5uZXQwHhcNMTIxMTA2MDAwMDAwWhcNMjIxMTA2MDAwMDAwWjBKMRgw\n" +
            "FgYDVQQDDA9CaXRtYXNrIFJvb3QgQ0ExEDAOBgNVBAoMB0JpdG1hc2sxHDAaBgNV\n" +
            "BAsME2h0dHBzOi8vYml0bWFzay5uZXQwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAw\n" +
            "ggIKAoICAQC1eV4YvayaU+maJbWrD4OHo3d7S1BtDlcvkIRS1Fw3iYDjsyDkZxai\n" +
            "dHp4EUasfNQ+EVtXUvtk6170EmLco6Elg8SJBQ27trE6nielPRPCfX3fQzETRfvB\n" +
            "7tNvGw4Jn2YKiYoMD79kkjgyZjkJ2r/bEHUSevmR09BRp86syHZerdNGpXYhcQ84\n" +
            "CA1+V+603GFIHnrP+uQDdssW93rgDNYu+exT+Wj6STfnUkugyjmPRPjL7wh0tzy+\n" +
            "znCeLl4xiV3g9sjPnc7r2EQKd5uaTe3j71sDPF92KRk0SSUndREz+B1+Dbe/RGk4\n" +
            "MEqGFuOzrtsgEhPIX0hplhb0Tgz/rtug+yTT7oJjBa3u20AAOQ38/M99EfdeJvc4\n" +
            "lPFF1XBBLh6X9UKF72an2NuANiX6XPySnJgZ7nZ09RiYZqVwu/qt3DfvLfhboq+0\n" +
            "bQvLUPXrVDr70onv5UDjpmEA/cLmaIqqrduuTkFZOym65/PfAPvpGnt7crQj/Ibl\n" +
            "DEDYZQmP7AS+6zBjoOzNjUGE5r40zWAR1RSi7zliXTu+yfsjXUIhUAWmYR6J3KxB\n" +
            "lfsiHBQ+8dn9kC3YrUexWoOqBiqJOAJzZh5Y1tqgzfh+2nmHSB2dsQRs7rDRRlyy\n" +
            "YMbkpzL9ZsOUO2eTP1mmar6YjCN+rggYjRrX71K2SpBG6b1zZxOG+wIDAQABo2Aw\n" +
            "XjAdBgNVHQ4EFgQUuYGDLL2sswnYpHHvProt1JU+D48wDgYDVR0PAQH/BAQDAgIE\n" +
            "MAwGA1UdEwQFMAMBAf8wHwYDVR0jBBgwFoAUuYGDLL2sswnYpHHvProt1JU+D48w\n" +
            "DQYJKoZIhvcNAQENBQADggIBADeG67vaFcbITGpi51264kHPYPEWaXUa5XYbtmBl\n" +
            "cXYyB6hY5hv/YNuVGJ1gWsDmdeXEyj0j2icGQjYdHRfwhrbEri+h1EZOm1cSBDuY\n" +
            "k/P5+ctHyOXx8IE79DBsZ6IL61UKIaKhqZBfLGYcWu17DVV6+LT+AKtHhOrv3TSj\n" +
            "RnAcKnCbKqXLhUPXpK0eTjPYS2zQGQGIhIy9sQXVXJJJsGrPgMxna1Xw2JikBOCG\n" +
            "htD/JKwt6xBmNwktH0GI/LVtVgSp82Clbn9C4eZN9E5YbVYjLkIEDhpByeC71QhX\n" +
            "EIQ0ZR56bFuJA/CwValBqV/G9gscTPQqd+iETp8yrFpAVHOW+YzSFbxjTEkBte1J\n" +
            "aF0vmbqdMAWLk+LEFPQRptZh0B88igtx6tV5oVd+p5IVRM49poLhuPNJGPvMj99l\n" +
            "mlZ4+AeRUnbOOeAEuvpLJbel4rhwFzmUiGoeTVoPZyMevWcVFq6BMkS+jRR2w0jK\n" +
            "G6b0v5XDHlcFYPOgUrtsOBFJVwbutLvxdk6q37kIFnWCd8L3kmES5q4wjyFK47Co\n" +
            "Ja8zlx64jmMZPg/t3wWqkZgXZ14qnbyG5/lGsj5CwVtfDljrhN0oCWK1FZaUmW3d\n" +
            "69db12/g4f6phldhxiWuGC/W6fCW5kre7nmhshcltqAJJuU47iX+DarBFiIj816e\n" +
            "yV8e\n" +
            "-----END CERTIFICATE-----\n" +
            "\n" +
            "</ca>\n" +
            "<cert>\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIEjDCCAnSgAwIBAgIQG6MBp/cd9DlY+7cdvp3R3jANBgkqhkiG9w0BAQsFADBmMRAwDgYDVQQK\n" +
            "DAdCaXRtYXNrMRwwGgYDVQQLDBNodHRwczovL2JpdG1hc2submV0MTQwMgYDVQQDDCtCaXRtYXNr\n" +
            "IFJvb3QgQ0EgKGNsaWVudCBjZXJ0aWZpY2F0ZXMgb25seSEpMB4XDTE0MTIwNTAwMDAwMFoXDTE1\n" +
            "MDMwNTAwMDAwMFowLTErMCkGA1UEAwwiVU5MSU1JVEVEZDBwZDdkMzE4eTNtOHNkeXllaTFqYmZl\n" +
            "eDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANRNhZ4aCwdL5+OKObOKeI2rDqEwGnIr\n" +
            "hL9wzo/FXbwLfdW45Y9Mxwhh6xy2NkA1YUKCB8VNBKNXlBrGr1QriLbu1rItsJ2VVLqGluVV/gO4\n" +
            "jcaPU+/Wu0hMFKG28J/dPvIGeNbjBWk6mxQAA5WIpRK9RTeQ88wVaGIZDDzIdivza2zpcyiPAyii\n" +
            "dbkyXh7sLsKvbZB6wLrert6Y1ylR3SlkZP0LfdGAMAdkMyuXKOjgcSnUltR8HSBuZcSUlsTVM11n\n" +
            "rYeGCYyPNNQ3UYatDW33UASgRDBorrmjhhKP7IW/opdlnPk5ZrP3i0qI32/boRe0EWZGXJvr4P3K\n" +
            "dJ30uCECAwEAAaNvMG0wHQYDVR0OBBYEFK8bMVAM4GBB5sHptoIOAaIvlYueMAsGA1UdDwQEAwIH\n" +
            "gDATBgNVHSUEDDAKBggrBgEFBQcDAjAJBgNVHRMEAjAAMB8GA1UdIwQYMBaAFId+E7bsWFsUWah9\n" +
            "vZuPvZ7O+aJsMA0GCSqGSIb3DQEBCwUAA4ICAQAQOX81csVhvP422NKkZH7+g3npBpl+sEHedaGR\n" +
            "xYPOu4HrA4TVF9h44sljRoRJyenGNdBZCXcLKHg889eePTf8Z5K3lTojp6hvwyA6tgxOMHT1kESW\n" +
            "PfqnRw8mHfHJuE3g+4YNUMwggzwc/VZATdV/7M33sarVN9AUOHou9n9BizgCC+UnYlS+F2POumE3\n" +
            "FbOhKo5uubI02MwBYlN2JVO2TBt1Q20w8wc6cU07Xi5Epp+1mkgFiOShkNtPcJmEyBWJhxDtSDOW\n" +
            "2doqWYNqH2kq7B5R/kyyfcpFJqAnBTV7xs+C5rTS1mW7LpxfdCUMbYuLCpyxpO3A/DhAm8n47tUH\n" +
            "lBtmo8Avdb8VdFpYiGBpB0o9kTFcsWFb2GkWFBduGfSEB8jUI7QtqhgZqocAKK/cweSRV8FwyUcn\n" +
            "R0prRm3QEi9fbXqEddzjSY9y/lqWYzT7u+IOAQpKroeZ4wzgYperDNOUFuYk1rP7yuvjP2pV5rcN\n" +
            "yPoBP60TPVWMRM4WJm6nTogAz2qBrFsf/XwT/ajzbsjT6HNB7QbRE+wkFkqspoXG5Agp7KQ8lW3L\n" +
            "SKCDGOQJz7VIE85pD0tg7QEXBEw8oaRZtMjQ0Gvs25mxXAKka4wGasaWfYH6d0E+iKYcWn86V1rH\n" +
            "K2ZoknT+Nno5jgjFuUR3fZseNizEfx7BteooKQ==\n" +
            "-----END CERTIFICATE-----\n" +
            "</cert>\n" +
            "management-external-key nopadding pkcs1 pss digest\n" +
            //"# crl-verify file missing in config profile\n" +
            "route 37.218.247.60  255.255.255.255 net_gateway\n"+
            "remote-cert-tls server\n" +
            "data-ciphers AES-128-CBC\n" +
            "cipher AES-128-CBC\n" +
            "auth SHA1\n" +
            "persist-tun\n" +
            "# persist-tun also enables pre resolving to avoid DNS resolve problem\n" +
            "preresolve\n" +
            "# Use system proxy setting\n" +
            "management-query-proxy\n" +
            "# Custom configuration options\n" +
            "# You are on your on own here :)\n" +
            "# These options found in the config file do not map to config settings:\n" +
            "keepalive 10 30 \n" +
            "tls-cipher DHE-RSA-AES128-SHA \n";

    String expectedVPNConfig_v3_obfsvpn_obfs4 = "# Config for OpenVPN 2.x\n" +
            "# Enables connection to GUI\n" +
            "management /data/data/se.leap.bitmask/mgmtsocket unix\n" +
            "management-client\n" +
            "management-query-passwords\n" +
            "management-hold\n" +
            "\n" +
            "setenv IV_GUI_VER \"se.leap.bitmaskclient 0.9.10\" \n" +
            "setenv IV_PLAT_VER \"28 9 ROBO Android unknown robolectric\"\n" +
            "machine-readable-output\n" +
            "allow-recursive-routing\n" +
            "ifconfig-nowarn\n" +
            "client\n" +
            "verb 4\n" +
            "connect-retry 2 300\n" +
            "resolv-retry 60\n" +
            "dev tun\n" +
            "remote 127.0.0.1 8080 udp\n" +
            "<ca>\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIFbzCCA1egAwIBAgIBATANBgkqhkiG9w0BAQ0FADBKMRgwFgYDVQQDDA9CaXRt\n" +
            "YXNrIFJvb3QgQ0ExEDAOBgNVBAoMB0JpdG1hc2sxHDAaBgNVBAsME2h0dHBzOi8v\n" +
            "Yml0bWFzay5uZXQwHhcNMTIxMTA2MDAwMDAwWhcNMjIxMTA2MDAwMDAwWjBKMRgw\n" +
            "FgYDVQQDDA9CaXRtYXNrIFJvb3QgQ0ExEDAOBgNVBAoMB0JpdG1hc2sxHDAaBgNV\n" +
            "BAsME2h0dHBzOi8vYml0bWFzay5uZXQwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAw\n" +
            "ggIKAoICAQC1eV4YvayaU+maJbWrD4OHo3d7S1BtDlcvkIRS1Fw3iYDjsyDkZxai\n" +
            "dHp4EUasfNQ+EVtXUvtk6170EmLco6Elg8SJBQ27trE6nielPRPCfX3fQzETRfvB\n" +
            "7tNvGw4Jn2YKiYoMD79kkjgyZjkJ2r/bEHUSevmR09BRp86syHZerdNGpXYhcQ84\n" +
            "CA1+V+603GFIHnrP+uQDdssW93rgDNYu+exT+Wj6STfnUkugyjmPRPjL7wh0tzy+\n" +
            "znCeLl4xiV3g9sjPnc7r2EQKd5uaTe3j71sDPF92KRk0SSUndREz+B1+Dbe/RGk4\n" +
            "MEqGFuOzrtsgEhPIX0hplhb0Tgz/rtug+yTT7oJjBa3u20AAOQ38/M99EfdeJvc4\n" +
            "lPFF1XBBLh6X9UKF72an2NuANiX6XPySnJgZ7nZ09RiYZqVwu/qt3DfvLfhboq+0\n" +
            "bQvLUPXrVDr70onv5UDjpmEA/cLmaIqqrduuTkFZOym65/PfAPvpGnt7crQj/Ibl\n" +
            "DEDYZQmP7AS+6zBjoOzNjUGE5r40zWAR1RSi7zliXTu+yfsjXUIhUAWmYR6J3KxB\n" +
            "lfsiHBQ+8dn9kC3YrUexWoOqBiqJOAJzZh5Y1tqgzfh+2nmHSB2dsQRs7rDRRlyy\n" +
            "YMbkpzL9ZsOUO2eTP1mmar6YjCN+rggYjRrX71K2SpBG6b1zZxOG+wIDAQABo2Aw\n" +
            "XjAdBgNVHQ4EFgQUuYGDLL2sswnYpHHvProt1JU+D48wDgYDVR0PAQH/BAQDAgIE\n" +
            "MAwGA1UdEwQFMAMBAf8wHwYDVR0jBBgwFoAUuYGDLL2sswnYpHHvProt1JU+D48w\n" +
            "DQYJKoZIhvcNAQENBQADggIBADeG67vaFcbITGpi51264kHPYPEWaXUa5XYbtmBl\n" +
            "cXYyB6hY5hv/YNuVGJ1gWsDmdeXEyj0j2icGQjYdHRfwhrbEri+h1EZOm1cSBDuY\n" +
            "k/P5+ctHyOXx8IE79DBsZ6IL61UKIaKhqZBfLGYcWu17DVV6+LT+AKtHhOrv3TSj\n" +
            "RnAcKnCbKqXLhUPXpK0eTjPYS2zQGQGIhIy9sQXVXJJJsGrPgMxna1Xw2JikBOCG\n" +
            "htD/JKwt6xBmNwktH0GI/LVtVgSp82Clbn9C4eZN9E5YbVYjLkIEDhpByeC71QhX\n" +
            "EIQ0ZR56bFuJA/CwValBqV/G9gscTPQqd+iETp8yrFpAVHOW+YzSFbxjTEkBte1J\n" +
            "aF0vmbqdMAWLk+LEFPQRptZh0B88igtx6tV5oVd+p5IVRM49poLhuPNJGPvMj99l\n" +
            "mlZ4+AeRUnbOOeAEuvpLJbel4rhwFzmUiGoeTVoPZyMevWcVFq6BMkS+jRR2w0jK\n" +
            "G6b0v5XDHlcFYPOgUrtsOBFJVwbutLvxdk6q37kIFnWCd8L3kmES5q4wjyFK47Co\n" +
            "Ja8zlx64jmMZPg/t3wWqkZgXZ14qnbyG5/lGsj5CwVtfDljrhN0oCWK1FZaUmW3d\n" +
            "69db12/g4f6phldhxiWuGC/W6fCW5kre7nmhshcltqAJJuU47iX+DarBFiIj816e\n" +
            "yV8e\n" +
            "-----END CERTIFICATE-----\n" +
            "\n" +
            "</ca>\n" +
            "<cert>\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIEjDCCAnSgAwIBAgIQG6MBp/cd9DlY+7cdvp3R3jANBgkqhkiG9w0BAQsFADBmMRAwDgYDVQQK\n" +
            "DAdCaXRtYXNrMRwwGgYDVQQLDBNodHRwczovL2JpdG1hc2submV0MTQwMgYDVQQDDCtCaXRtYXNr\n" +
            "IFJvb3QgQ0EgKGNsaWVudCBjZXJ0aWZpY2F0ZXMgb25seSEpMB4XDTE0MTIwNTAwMDAwMFoXDTE1\n" +
            "MDMwNTAwMDAwMFowLTErMCkGA1UEAwwiVU5MSU1JVEVEZDBwZDdkMzE4eTNtOHNkeXllaTFqYmZl\n" +
            "eDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANRNhZ4aCwdL5+OKObOKeI2rDqEwGnIr\n" +
            "hL9wzo/FXbwLfdW45Y9Mxwhh6xy2NkA1YUKCB8VNBKNXlBrGr1QriLbu1rItsJ2VVLqGluVV/gO4\n" +
            "jcaPU+/Wu0hMFKG28J/dPvIGeNbjBWk6mxQAA5WIpRK9RTeQ88wVaGIZDDzIdivza2zpcyiPAyii\n" +
            "dbkyXh7sLsKvbZB6wLrert6Y1ylR3SlkZP0LfdGAMAdkMyuXKOjgcSnUltR8HSBuZcSUlsTVM11n\n" +
            "rYeGCYyPNNQ3UYatDW33UASgRDBorrmjhhKP7IW/opdlnPk5ZrP3i0qI32/boRe0EWZGXJvr4P3K\n" +
            "dJ30uCECAwEAAaNvMG0wHQYDVR0OBBYEFK8bMVAM4GBB5sHptoIOAaIvlYueMAsGA1UdDwQEAwIH\n" +
            "gDATBgNVHSUEDDAKBggrBgEFBQcDAjAJBgNVHRMEAjAAMB8GA1UdIwQYMBaAFId+E7bsWFsUWah9\n" +
            "vZuPvZ7O+aJsMA0GCSqGSIb3DQEBCwUAA4ICAQAQOX81csVhvP422NKkZH7+g3npBpl+sEHedaGR\n" +
            "xYPOu4HrA4TVF9h44sljRoRJyenGNdBZCXcLKHg889eePTf8Z5K3lTojp6hvwyA6tgxOMHT1kESW\n" +
            "PfqnRw8mHfHJuE3g+4YNUMwggzwc/VZATdV/7M33sarVN9AUOHou9n9BizgCC+UnYlS+F2POumE3\n" +
            "FbOhKo5uubI02MwBYlN2JVO2TBt1Q20w8wc6cU07Xi5Epp+1mkgFiOShkNtPcJmEyBWJhxDtSDOW\n" +
            "2doqWYNqH2kq7B5R/kyyfcpFJqAnBTV7xs+C5rTS1mW7LpxfdCUMbYuLCpyxpO3A/DhAm8n47tUH\n" +
            "lBtmo8Avdb8VdFpYiGBpB0o9kTFcsWFb2GkWFBduGfSEB8jUI7QtqhgZqocAKK/cweSRV8FwyUcn\n" +
            "R0prRm3QEi9fbXqEddzjSY9y/lqWYzT7u+IOAQpKroeZ4wzgYperDNOUFuYk1rP7yuvjP2pV5rcN\n" +
            "yPoBP60TPVWMRM4WJm6nTogAz2qBrFsf/XwT/ajzbsjT6HNB7QbRE+wkFkqspoXG5Agp7KQ8lW3L\n" +
            "SKCDGOQJz7VIE85pD0tg7QEXBEw8oaRZtMjQ0Gvs25mxXAKka4wGasaWfYH6d0E+iKYcWn86V1rH\n" +
            "K2ZoknT+Nno5jgjFuUR3fZseNizEfx7BteooKQ==\n" +
            "-----END CERTIFICATE-----\n" +
            "</cert>\n" +
            "management-external-key nopadding pkcs1 pss digest\n" +
            //"# crl-verify file missing in config profile\n" +
            "route 37.218.247.60  255.255.255.255 net_gateway\n"+
            "remote-cert-tls server\n" +
            "data-ciphers AES-128-CBC\n" +
            "cipher AES-128-CBC\n" +
            "auth SHA1\n" +
            "persist-tun\n" +
            "# persist-tun also enables pre resolving to avoid DNS resolve problem\n" +
            "preresolve\n" +
            "# Use system proxy setting\n" +
            "management-query-proxy\n" +
            "# Custom configuration options\n" +
            "# You are on your on own here :)\n" +
            "# These options found in the config file do not map to config settings:\n" +
            "keepalive 10 30 \n" +
            "tls-cipher DHE-RSA-AES128-SHA \n";

    String expectedVPNConfig_v3_ovpn_tcp_udp = "# Config for OpenVPN 2.x\n" +
            "# Enables connection to GUI\n" +
            "management /data/data/se.leap.bitmask/mgmtsocket unix\n" +
            "management-client\n" +
            "management-query-passwords\n" +
            "management-hold\n" +
            "\n" +
            "setenv IV_GUI_VER \"se.leap.bitmaskclient 0.9.10\" \n" +
            "setenv IV_PLAT_VER \"28 9 ROBO Android unknown robolectric\"\n" +
            "machine-readable-output\n" +
            "allow-recursive-routing\n" +
            "ifconfig-nowarn\n" +
            "client\n" +
            "verb 4\n" +
            "connect-retry 2 300\n" +
            "resolv-retry 60\n" +
            "dev tun\n" +
            "remote 37.218.247.60 1195 tcp-client\n" +
            "remote 37.218.247.60 1195 udp\n" +
            "<ca>\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIFbzCCA1egAwIBAgIBATANBgkqhkiG9w0BAQ0FADBKMRgwFgYDVQQDDA9CaXRt\n" +
            "YXNrIFJvb3QgQ0ExEDAOBgNVBAoMB0JpdG1hc2sxHDAaBgNVBAsME2h0dHBzOi8v\n" +
            "Yml0bWFzay5uZXQwHhcNMTIxMTA2MDAwMDAwWhcNMjIxMTA2MDAwMDAwWjBKMRgw\n" +
            "FgYDVQQDDA9CaXRtYXNrIFJvb3QgQ0ExEDAOBgNVBAoMB0JpdG1hc2sxHDAaBgNV\n" +
            "BAsME2h0dHBzOi8vYml0bWFzay5uZXQwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAw\n" +
            "ggIKAoICAQC1eV4YvayaU+maJbWrD4OHo3d7S1BtDlcvkIRS1Fw3iYDjsyDkZxai\n" +
            "dHp4EUasfNQ+EVtXUvtk6170EmLco6Elg8SJBQ27trE6nielPRPCfX3fQzETRfvB\n" +
            "7tNvGw4Jn2YKiYoMD79kkjgyZjkJ2r/bEHUSevmR09BRp86syHZerdNGpXYhcQ84\n" +
            "CA1+V+603GFIHnrP+uQDdssW93rgDNYu+exT+Wj6STfnUkugyjmPRPjL7wh0tzy+\n" +
            "znCeLl4xiV3g9sjPnc7r2EQKd5uaTe3j71sDPF92KRk0SSUndREz+B1+Dbe/RGk4\n" +
            "MEqGFuOzrtsgEhPIX0hplhb0Tgz/rtug+yTT7oJjBa3u20AAOQ38/M99EfdeJvc4\n" +
            "lPFF1XBBLh6X9UKF72an2NuANiX6XPySnJgZ7nZ09RiYZqVwu/qt3DfvLfhboq+0\n" +
            "bQvLUPXrVDr70onv5UDjpmEA/cLmaIqqrduuTkFZOym65/PfAPvpGnt7crQj/Ibl\n" +
            "DEDYZQmP7AS+6zBjoOzNjUGE5r40zWAR1RSi7zliXTu+yfsjXUIhUAWmYR6J3KxB\n" +
            "lfsiHBQ+8dn9kC3YrUexWoOqBiqJOAJzZh5Y1tqgzfh+2nmHSB2dsQRs7rDRRlyy\n" +
            "YMbkpzL9ZsOUO2eTP1mmar6YjCN+rggYjRrX71K2SpBG6b1zZxOG+wIDAQABo2Aw\n" +
            "XjAdBgNVHQ4EFgQUuYGDLL2sswnYpHHvProt1JU+D48wDgYDVR0PAQH/BAQDAgIE\n" +
            "MAwGA1UdEwQFMAMBAf8wHwYDVR0jBBgwFoAUuYGDLL2sswnYpHHvProt1JU+D48w\n" +
            "DQYJKoZIhvcNAQENBQADggIBADeG67vaFcbITGpi51264kHPYPEWaXUa5XYbtmBl\n" +
            "cXYyB6hY5hv/YNuVGJ1gWsDmdeXEyj0j2icGQjYdHRfwhrbEri+h1EZOm1cSBDuY\n" +
            "k/P5+ctHyOXx8IE79DBsZ6IL61UKIaKhqZBfLGYcWu17DVV6+LT+AKtHhOrv3TSj\n" +
            "RnAcKnCbKqXLhUPXpK0eTjPYS2zQGQGIhIy9sQXVXJJJsGrPgMxna1Xw2JikBOCG\n" +
            "htD/JKwt6xBmNwktH0GI/LVtVgSp82Clbn9C4eZN9E5YbVYjLkIEDhpByeC71QhX\n" +
            "EIQ0ZR56bFuJA/CwValBqV/G9gscTPQqd+iETp8yrFpAVHOW+YzSFbxjTEkBte1J\n" +
            "aF0vmbqdMAWLk+LEFPQRptZh0B88igtx6tV5oVd+p5IVRM49poLhuPNJGPvMj99l\n" +
            "mlZ4+AeRUnbOOeAEuvpLJbel4rhwFzmUiGoeTVoPZyMevWcVFq6BMkS+jRR2w0jK\n" +
            "G6b0v5XDHlcFYPOgUrtsOBFJVwbutLvxdk6q37kIFnWCd8L3kmES5q4wjyFK47Co\n" +
            "Ja8zlx64jmMZPg/t3wWqkZgXZ14qnbyG5/lGsj5CwVtfDljrhN0oCWK1FZaUmW3d\n" +
            "69db12/g4f6phldhxiWuGC/W6fCW5kre7nmhshcltqAJJuU47iX+DarBFiIj816e\n" +
            "yV8e\n" +
            "-----END CERTIFICATE-----\n" +
            "\n" +
            "</ca>\n" +
            "<cert>\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIEjDCCAnSgAwIBAgIQG6MBp/cd9DlY+7cdvp3R3jANBgkqhkiG9w0BAQsFADBmMRAwDgYDVQQK\n" +
            "DAdCaXRtYXNrMRwwGgYDVQQLDBNodHRwczovL2JpdG1hc2submV0MTQwMgYDVQQDDCtCaXRtYXNr\n" +
            "IFJvb3QgQ0EgKGNsaWVudCBjZXJ0aWZpY2F0ZXMgb25seSEpMB4XDTE0MTIwNTAwMDAwMFoXDTE1\n" +
            "MDMwNTAwMDAwMFowLTErMCkGA1UEAwwiVU5MSU1JVEVEZDBwZDdkMzE4eTNtOHNkeXllaTFqYmZl\n" +
            "eDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANRNhZ4aCwdL5+OKObOKeI2rDqEwGnIr\n" +
            "hL9wzo/FXbwLfdW45Y9Mxwhh6xy2NkA1YUKCB8VNBKNXlBrGr1QriLbu1rItsJ2VVLqGluVV/gO4\n" +
            "jcaPU+/Wu0hMFKG28J/dPvIGeNbjBWk6mxQAA5WIpRK9RTeQ88wVaGIZDDzIdivza2zpcyiPAyii\n" +
            "dbkyXh7sLsKvbZB6wLrert6Y1ylR3SlkZP0LfdGAMAdkMyuXKOjgcSnUltR8HSBuZcSUlsTVM11n\n" +
            "rYeGCYyPNNQ3UYatDW33UASgRDBorrmjhhKP7IW/opdlnPk5ZrP3i0qI32/boRe0EWZGXJvr4P3K\n" +
            "dJ30uCECAwEAAaNvMG0wHQYDVR0OBBYEFK8bMVAM4GBB5sHptoIOAaIvlYueMAsGA1UdDwQEAwIH\n" +
            "gDATBgNVHSUEDDAKBggrBgEFBQcDAjAJBgNVHRMEAjAAMB8GA1UdIwQYMBaAFId+E7bsWFsUWah9\n" +
            "vZuPvZ7O+aJsMA0GCSqGSIb3DQEBCwUAA4ICAQAQOX81csVhvP422NKkZH7+g3npBpl+sEHedaGR\n" +
            "xYPOu4HrA4TVF9h44sljRoRJyenGNdBZCXcLKHg889eePTf8Z5K3lTojp6hvwyA6tgxOMHT1kESW\n" +
            "PfqnRw8mHfHJuE3g+4YNUMwggzwc/VZATdV/7M33sarVN9AUOHou9n9BizgCC+UnYlS+F2POumE3\n" +
            "FbOhKo5uubI02MwBYlN2JVO2TBt1Q20w8wc6cU07Xi5Epp+1mkgFiOShkNtPcJmEyBWJhxDtSDOW\n" +
            "2doqWYNqH2kq7B5R/kyyfcpFJqAnBTV7xs+C5rTS1mW7LpxfdCUMbYuLCpyxpO3A/DhAm8n47tUH\n" +
            "lBtmo8Avdb8VdFpYiGBpB0o9kTFcsWFb2GkWFBduGfSEB8jUI7QtqhgZqocAKK/cweSRV8FwyUcn\n" +
            "R0prRm3QEi9fbXqEddzjSY9y/lqWYzT7u+IOAQpKroeZ4wzgYperDNOUFuYk1rP7yuvjP2pV5rcN\n" +
            "yPoBP60TPVWMRM4WJm6nTogAz2qBrFsf/XwT/ajzbsjT6HNB7QbRE+wkFkqspoXG5Agp7KQ8lW3L\n" +
            "SKCDGOQJz7VIE85pD0tg7QEXBEw8oaRZtMjQ0Gvs25mxXAKka4wGasaWfYH6d0E+iKYcWn86V1rH\n" +
            "K2ZoknT+Nno5jgjFuUR3fZseNizEfx7BteooKQ==\n" +
            "-----END CERTIFICATE-----\n" +
            "</cert>\n" +
            "management-external-key nopadding pkcs1 pss digest\n" +
            //"# crl-verify file missing in config profile\n" +
            "remote-cert-tls server\n" +
            "data-ciphers AES-128-CBC\n" +
            "cipher AES-128-CBC\n" +
            "auth SHA1\n" +
            "persist-tun\n" +
            "# persist-tun also enables pre resolving to avoid DNS resolve problem\n" +
            "preresolve\n" +
            "# Use system proxy setting\n" +
            "management-query-proxy\n" +
            "# Custom configuration options\n" +
            "# You are on your on own here :)\n" +
            "# These options found in the config file do not map to config settings:\n" +
            "keepalive 10 30 \n" +
            "tls-cipher DHE-RSA-AES128-SHA \n";

    String expectedVPNConfig_v3_ovpn_udp_tcp = "# Config for OpenVPN 2.x\n" +
            "# Enables connection to GUI\n" +
            "management /data/data/se.leap.bitmask/mgmtsocket unix\n" +
            "management-client\n" +
            "management-query-passwords\n" +
            "management-hold\n" +
            "\n" +
            "setenv IV_GUI_VER \"se.leap.bitmaskclient 0.9.10\" \n" +
            "setenv IV_PLAT_VER \"28 9 ROBO Android unknown robolectric\"\n" +
            "machine-readable-output\n" +
            "allow-recursive-routing\n" +
            "ifconfig-nowarn\n" +
            "client\n" +
            "verb 4\n" +
            "connect-retry 2 300\n" +
            "resolv-retry 60\n" +
            "dev tun\n" +
            "remote 37.218.247.60 1195 udp\n" +
            "remote 37.218.247.60 1195 tcp-client\n" +
            "<ca>\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIFbzCCA1egAwIBAgIBATANBgkqhkiG9w0BAQ0FADBKMRgwFgYDVQQDDA9CaXRt\n" +
            "YXNrIFJvb3QgQ0ExEDAOBgNVBAoMB0JpdG1hc2sxHDAaBgNVBAsME2h0dHBzOi8v\n" +
            "Yml0bWFzay5uZXQwHhcNMTIxMTA2MDAwMDAwWhcNMjIxMTA2MDAwMDAwWjBKMRgw\n" +
            "FgYDVQQDDA9CaXRtYXNrIFJvb3QgQ0ExEDAOBgNVBAoMB0JpdG1hc2sxHDAaBgNV\n" +
            "BAsME2h0dHBzOi8vYml0bWFzay5uZXQwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAw\n" +
            "ggIKAoICAQC1eV4YvayaU+maJbWrD4OHo3d7S1BtDlcvkIRS1Fw3iYDjsyDkZxai\n" +
            "dHp4EUasfNQ+EVtXUvtk6170EmLco6Elg8SJBQ27trE6nielPRPCfX3fQzETRfvB\n" +
            "7tNvGw4Jn2YKiYoMD79kkjgyZjkJ2r/bEHUSevmR09BRp86syHZerdNGpXYhcQ84\n" +
            "CA1+V+603GFIHnrP+uQDdssW93rgDNYu+exT+Wj6STfnUkugyjmPRPjL7wh0tzy+\n" +
            "znCeLl4xiV3g9sjPnc7r2EQKd5uaTe3j71sDPF92KRk0SSUndREz+B1+Dbe/RGk4\n" +
            "MEqGFuOzrtsgEhPIX0hplhb0Tgz/rtug+yTT7oJjBa3u20AAOQ38/M99EfdeJvc4\n" +
            "lPFF1XBBLh6X9UKF72an2NuANiX6XPySnJgZ7nZ09RiYZqVwu/qt3DfvLfhboq+0\n" +
            "bQvLUPXrVDr70onv5UDjpmEA/cLmaIqqrduuTkFZOym65/PfAPvpGnt7crQj/Ibl\n" +
            "DEDYZQmP7AS+6zBjoOzNjUGE5r40zWAR1RSi7zliXTu+yfsjXUIhUAWmYR6J3KxB\n" +
            "lfsiHBQ+8dn9kC3YrUexWoOqBiqJOAJzZh5Y1tqgzfh+2nmHSB2dsQRs7rDRRlyy\n" +
            "YMbkpzL9ZsOUO2eTP1mmar6YjCN+rggYjRrX71K2SpBG6b1zZxOG+wIDAQABo2Aw\n" +
            "XjAdBgNVHQ4EFgQUuYGDLL2sswnYpHHvProt1JU+D48wDgYDVR0PAQH/BAQDAgIE\n" +
            "MAwGA1UdEwQFMAMBAf8wHwYDVR0jBBgwFoAUuYGDLL2sswnYpHHvProt1JU+D48w\n" +
            "DQYJKoZIhvcNAQENBQADggIBADeG67vaFcbITGpi51264kHPYPEWaXUa5XYbtmBl\n" +
            "cXYyB6hY5hv/YNuVGJ1gWsDmdeXEyj0j2icGQjYdHRfwhrbEri+h1EZOm1cSBDuY\n" +
            "k/P5+ctHyOXx8IE79DBsZ6IL61UKIaKhqZBfLGYcWu17DVV6+LT+AKtHhOrv3TSj\n" +
            "RnAcKnCbKqXLhUPXpK0eTjPYS2zQGQGIhIy9sQXVXJJJsGrPgMxna1Xw2JikBOCG\n" +
            "htD/JKwt6xBmNwktH0GI/LVtVgSp82Clbn9C4eZN9E5YbVYjLkIEDhpByeC71QhX\n" +
            "EIQ0ZR56bFuJA/CwValBqV/G9gscTPQqd+iETp8yrFpAVHOW+YzSFbxjTEkBte1J\n" +
            "aF0vmbqdMAWLk+LEFPQRptZh0B88igtx6tV5oVd+p5IVRM49poLhuPNJGPvMj99l\n" +
            "mlZ4+AeRUnbOOeAEuvpLJbel4rhwFzmUiGoeTVoPZyMevWcVFq6BMkS+jRR2w0jK\n" +
            "G6b0v5XDHlcFYPOgUrtsOBFJVwbutLvxdk6q37kIFnWCd8L3kmES5q4wjyFK47Co\n" +
            "Ja8zlx64jmMZPg/t3wWqkZgXZ14qnbyG5/lGsj5CwVtfDljrhN0oCWK1FZaUmW3d\n" +
            "69db12/g4f6phldhxiWuGC/W6fCW5kre7nmhshcltqAJJuU47iX+DarBFiIj816e\n" +
            "yV8e\n" +
            "-----END CERTIFICATE-----\n" +
            "\n" +
            "</ca>\n" +
            "<cert>\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIEjDCCAnSgAwIBAgIQG6MBp/cd9DlY+7cdvp3R3jANBgkqhkiG9w0BAQsFADBmMRAwDgYDVQQK\n" +
            "DAdCaXRtYXNrMRwwGgYDVQQLDBNodHRwczovL2JpdG1hc2submV0MTQwMgYDVQQDDCtCaXRtYXNr\n" +
            "IFJvb3QgQ0EgKGNsaWVudCBjZXJ0aWZpY2F0ZXMgb25seSEpMB4XDTE0MTIwNTAwMDAwMFoXDTE1\n" +
            "MDMwNTAwMDAwMFowLTErMCkGA1UEAwwiVU5MSU1JVEVEZDBwZDdkMzE4eTNtOHNkeXllaTFqYmZl\n" +
            "eDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANRNhZ4aCwdL5+OKObOKeI2rDqEwGnIr\n" +
            "hL9wzo/FXbwLfdW45Y9Mxwhh6xy2NkA1YUKCB8VNBKNXlBrGr1QriLbu1rItsJ2VVLqGluVV/gO4\n" +
            "jcaPU+/Wu0hMFKG28J/dPvIGeNbjBWk6mxQAA5WIpRK9RTeQ88wVaGIZDDzIdivza2zpcyiPAyii\n" +
            "dbkyXh7sLsKvbZB6wLrert6Y1ylR3SlkZP0LfdGAMAdkMyuXKOjgcSnUltR8HSBuZcSUlsTVM11n\n" +
            "rYeGCYyPNNQ3UYatDW33UASgRDBorrmjhhKP7IW/opdlnPk5ZrP3i0qI32/boRe0EWZGXJvr4P3K\n" +
            "dJ30uCECAwEAAaNvMG0wHQYDVR0OBBYEFK8bMVAM4GBB5sHptoIOAaIvlYueMAsGA1UdDwQEAwIH\n" +
            "gDATBgNVHSUEDDAKBggrBgEFBQcDAjAJBgNVHRMEAjAAMB8GA1UdIwQYMBaAFId+E7bsWFsUWah9\n" +
            "vZuPvZ7O+aJsMA0GCSqGSIb3DQEBCwUAA4ICAQAQOX81csVhvP422NKkZH7+g3npBpl+sEHedaGR\n" +
            "xYPOu4HrA4TVF9h44sljRoRJyenGNdBZCXcLKHg889eePTf8Z5K3lTojp6hvwyA6tgxOMHT1kESW\n" +
            "PfqnRw8mHfHJuE3g+4YNUMwggzwc/VZATdV/7M33sarVN9AUOHou9n9BizgCC+UnYlS+F2POumE3\n" +
            "FbOhKo5uubI02MwBYlN2JVO2TBt1Q20w8wc6cU07Xi5Epp+1mkgFiOShkNtPcJmEyBWJhxDtSDOW\n" +
            "2doqWYNqH2kq7B5R/kyyfcpFJqAnBTV7xs+C5rTS1mW7LpxfdCUMbYuLCpyxpO3A/DhAm8n47tUH\n" +
            "lBtmo8Avdb8VdFpYiGBpB0o9kTFcsWFb2GkWFBduGfSEB8jUI7QtqhgZqocAKK/cweSRV8FwyUcn\n" +
            "R0prRm3QEi9fbXqEddzjSY9y/lqWYzT7u+IOAQpKroeZ4wzgYperDNOUFuYk1rP7yuvjP2pV5rcN\n" +
            "yPoBP60TPVWMRM4WJm6nTogAz2qBrFsf/XwT/ajzbsjT6HNB7QbRE+wkFkqspoXG5Agp7KQ8lW3L\n" +
            "SKCDGOQJz7VIE85pD0tg7QEXBEw8oaRZtMjQ0Gvs25mxXAKka4wGasaWfYH6d0E+iKYcWn86V1rH\n" +
            "K2ZoknT+Nno5jgjFuUR3fZseNizEfx7BteooKQ==\n" +
            "-----END CERTIFICATE-----\n" +
            "</cert>\n" +
            "management-external-key nopadding pkcs1 pss digest\n" +
            //"# crl-verify file missing in config profile\n" +
            "remote-cert-tls server\n" +
            "data-ciphers AES-128-CBC\n" +
            "cipher AES-128-CBC\n" +
            "auth SHA1\n" +
            "persist-tun\n" +
            "# persist-tun also enables pre resolving to avoid DNS resolve problem\n" +
            "preresolve\n" +
            "# Use system proxy setting\n" +
            "management-query-proxy\n" +
            "# Custom configuration options\n" +
            "# You are on your on own here :)\n" +
            "# These options found in the config file do not map to config settings:\n" +
            "keepalive 10 30 \n" +
            "tls-cipher DHE-RSA-AES128-SHA \n";

    String expectedVPNConfig_v3_ovpn_udp_tcp_defaultDataCiphers = "# Config for OpenVPN 2.x\n" +
            "# Enables connection to GUI\n" +
            "management /data/data/se.leap.bitmask/mgmtsocket unix\n" +
            "management-client\n" +
            "management-query-passwords\n" +
            "management-hold\n" +
            "\n" +
            "setenv IV_GUI_VER \"se.leap.bitmaskclient 0.9.10\" \n" +
            "setenv IV_PLAT_VER \"28 9 ROBO Android unknown robolectric\"\n" +
            "machine-readable-output\n" +
            "allow-recursive-routing\n" +
            "ifconfig-nowarn\n" +
            "client\n" +
            "verb 4\n" +
            "connect-retry 2 300\n" +
            "resolv-retry 60\n" +
            "dev tun\n" +
            "remote 37.218.247.60 1195 udp\n" +
            "remote 37.218.247.60 1195 tcp-client\n" +
            "<ca>\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIFbzCCA1egAwIBAgIBATANBgkqhkiG9w0BAQ0FADBKMRgwFgYDVQQDDA9CaXRt\n" +
            "YXNrIFJvb3QgQ0ExEDAOBgNVBAoMB0JpdG1hc2sxHDAaBgNVBAsME2h0dHBzOi8v\n" +
            "Yml0bWFzay5uZXQwHhcNMTIxMTA2MDAwMDAwWhcNMjIxMTA2MDAwMDAwWjBKMRgw\n" +
            "FgYDVQQDDA9CaXRtYXNrIFJvb3QgQ0ExEDAOBgNVBAoMB0JpdG1hc2sxHDAaBgNV\n" +
            "BAsME2h0dHBzOi8vYml0bWFzay5uZXQwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAw\n" +
            "ggIKAoICAQC1eV4YvayaU+maJbWrD4OHo3d7S1BtDlcvkIRS1Fw3iYDjsyDkZxai\n" +
            "dHp4EUasfNQ+EVtXUvtk6170EmLco6Elg8SJBQ27trE6nielPRPCfX3fQzETRfvB\n" +
            "7tNvGw4Jn2YKiYoMD79kkjgyZjkJ2r/bEHUSevmR09BRp86syHZerdNGpXYhcQ84\n" +
            "CA1+V+603GFIHnrP+uQDdssW93rgDNYu+exT+Wj6STfnUkugyjmPRPjL7wh0tzy+\n" +
            "znCeLl4xiV3g9sjPnc7r2EQKd5uaTe3j71sDPF92KRk0SSUndREz+B1+Dbe/RGk4\n" +
            "MEqGFuOzrtsgEhPIX0hplhb0Tgz/rtug+yTT7oJjBa3u20AAOQ38/M99EfdeJvc4\n" +
            "lPFF1XBBLh6X9UKF72an2NuANiX6XPySnJgZ7nZ09RiYZqVwu/qt3DfvLfhboq+0\n" +
            "bQvLUPXrVDr70onv5UDjpmEA/cLmaIqqrduuTkFZOym65/PfAPvpGnt7crQj/Ibl\n" +
            "DEDYZQmP7AS+6zBjoOzNjUGE5r40zWAR1RSi7zliXTu+yfsjXUIhUAWmYR6J3KxB\n" +
            "lfsiHBQ+8dn9kC3YrUexWoOqBiqJOAJzZh5Y1tqgzfh+2nmHSB2dsQRs7rDRRlyy\n" +
            "YMbkpzL9ZsOUO2eTP1mmar6YjCN+rggYjRrX71K2SpBG6b1zZxOG+wIDAQABo2Aw\n" +
            "XjAdBgNVHQ4EFgQUuYGDLL2sswnYpHHvProt1JU+D48wDgYDVR0PAQH/BAQDAgIE\n" +
            "MAwGA1UdEwQFMAMBAf8wHwYDVR0jBBgwFoAUuYGDLL2sswnYpHHvProt1JU+D48w\n" +
            "DQYJKoZIhvcNAQENBQADggIBADeG67vaFcbITGpi51264kHPYPEWaXUa5XYbtmBl\n" +
            "cXYyB6hY5hv/YNuVGJ1gWsDmdeXEyj0j2icGQjYdHRfwhrbEri+h1EZOm1cSBDuY\n" +
            "k/P5+ctHyOXx8IE79DBsZ6IL61UKIaKhqZBfLGYcWu17DVV6+LT+AKtHhOrv3TSj\n" +
            "RnAcKnCbKqXLhUPXpK0eTjPYS2zQGQGIhIy9sQXVXJJJsGrPgMxna1Xw2JikBOCG\n" +
            "htD/JKwt6xBmNwktH0GI/LVtVgSp82Clbn9C4eZN9E5YbVYjLkIEDhpByeC71QhX\n" +
            "EIQ0ZR56bFuJA/CwValBqV/G9gscTPQqd+iETp8yrFpAVHOW+YzSFbxjTEkBte1J\n" +
            "aF0vmbqdMAWLk+LEFPQRptZh0B88igtx6tV5oVd+p5IVRM49poLhuPNJGPvMj99l\n" +
            "mlZ4+AeRUnbOOeAEuvpLJbel4rhwFzmUiGoeTVoPZyMevWcVFq6BMkS+jRR2w0jK\n" +
            "G6b0v5XDHlcFYPOgUrtsOBFJVwbutLvxdk6q37kIFnWCd8L3kmES5q4wjyFK47Co\n" +
            "Ja8zlx64jmMZPg/t3wWqkZgXZ14qnbyG5/lGsj5CwVtfDljrhN0oCWK1FZaUmW3d\n" +
            "69db12/g4f6phldhxiWuGC/W6fCW5kre7nmhshcltqAJJuU47iX+DarBFiIj816e\n" +
            "yV8e\n" +
            "-----END CERTIFICATE-----\n" +
            "\n" +
            "</ca>\n" +
            "<cert>\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIEjDCCAnSgAwIBAgIQG6MBp/cd9DlY+7cdvp3R3jANBgkqhkiG9w0BAQsFADBmMRAwDgYDVQQK\n" +
            "DAdCaXRtYXNrMRwwGgYDVQQLDBNodHRwczovL2JpdG1hc2submV0MTQwMgYDVQQDDCtCaXRtYXNr\n" +
            "IFJvb3QgQ0EgKGNsaWVudCBjZXJ0aWZpY2F0ZXMgb25seSEpMB4XDTE0MTIwNTAwMDAwMFoXDTE1\n" +
            "MDMwNTAwMDAwMFowLTErMCkGA1UEAwwiVU5MSU1JVEVEZDBwZDdkMzE4eTNtOHNkeXllaTFqYmZl\n" +
            "eDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANRNhZ4aCwdL5+OKObOKeI2rDqEwGnIr\n" +
            "hL9wzo/FXbwLfdW45Y9Mxwhh6xy2NkA1YUKCB8VNBKNXlBrGr1QriLbu1rItsJ2VVLqGluVV/gO4\n" +
            "jcaPU+/Wu0hMFKG28J/dPvIGeNbjBWk6mxQAA5WIpRK9RTeQ88wVaGIZDDzIdivza2zpcyiPAyii\n" +
            "dbkyXh7sLsKvbZB6wLrert6Y1ylR3SlkZP0LfdGAMAdkMyuXKOjgcSnUltR8HSBuZcSUlsTVM11n\n" +
            "rYeGCYyPNNQ3UYatDW33UASgRDBorrmjhhKP7IW/opdlnPk5ZrP3i0qI32/boRe0EWZGXJvr4P3K\n" +
            "dJ30uCECAwEAAaNvMG0wHQYDVR0OBBYEFK8bMVAM4GBB5sHptoIOAaIvlYueMAsGA1UdDwQEAwIH\n" +
            "gDATBgNVHSUEDDAKBggrBgEFBQcDAjAJBgNVHRMEAjAAMB8GA1UdIwQYMBaAFId+E7bsWFsUWah9\n" +
            "vZuPvZ7O+aJsMA0GCSqGSIb3DQEBCwUAA4ICAQAQOX81csVhvP422NKkZH7+g3npBpl+sEHedaGR\n" +
            "xYPOu4HrA4TVF9h44sljRoRJyenGNdBZCXcLKHg889eePTf8Z5K3lTojp6hvwyA6tgxOMHT1kESW\n" +
            "PfqnRw8mHfHJuE3g+4YNUMwggzwc/VZATdV/7M33sarVN9AUOHou9n9BizgCC+UnYlS+F2POumE3\n" +
            "FbOhKo5uubI02MwBYlN2JVO2TBt1Q20w8wc6cU07Xi5Epp+1mkgFiOShkNtPcJmEyBWJhxDtSDOW\n" +
            "2doqWYNqH2kq7B5R/kyyfcpFJqAnBTV7xs+C5rTS1mW7LpxfdCUMbYuLCpyxpO3A/DhAm8n47tUH\n" +
            "lBtmo8Avdb8VdFpYiGBpB0o9kTFcsWFb2GkWFBduGfSEB8jUI7QtqhgZqocAKK/cweSRV8FwyUcn\n" +
            "R0prRm3QEi9fbXqEddzjSY9y/lqWYzT7u+IOAQpKroeZ4wzgYperDNOUFuYk1rP7yuvjP2pV5rcN\n" +
            "yPoBP60TPVWMRM4WJm6nTogAz2qBrFsf/XwT/ajzbsjT6HNB7QbRE+wkFkqspoXG5Agp7KQ8lW3L\n" +
            "SKCDGOQJz7VIE85pD0tg7QEXBEw8oaRZtMjQ0Gvs25mxXAKka4wGasaWfYH6d0E+iKYcWn86V1rH\n" +
            "K2ZoknT+Nno5jgjFuUR3fZseNizEfx7BteooKQ==\n" +
            "-----END CERTIFICATE-----\n" +
            "</cert>\n" +
            "management-external-key nopadding pkcs1 pss digest\n" +
            //"# crl-verify file missing in config profile\n" +
            "remote-cert-tls server\n" +
            "data-ciphers AES-256-GCM:AES-128-GCM:AES-128-CBC\n" +
            "cipher AES-128-CBC\n" +
            "auth SHA1\n" +
            "persist-tun\n" +
            "# persist-tun also enables pre resolving to avoid DNS resolve problem\n" +
            "preresolve\n" +
            "# Use system proxy setting\n" +
            "management-query-proxy\n" +
            "# Custom configuration options\n" +
            "# You are on your on own here :)\n" +
            "# These options found in the config file do not map to config settings:\n" +
            "keepalive 10 30 \n" +
            "tls-cipher DHE-RSA-AES128-SHA \n";

    String expectedVPNConfig_v4_ovpn_tcp_udp = "# Config for OpenVPN 2.x\n" +
            "# Enables connection to GUI\n" +
            "management /data/data/se.leap.bitmask/mgmtsocket unix\n" +
            "management-client\n" +
            "management-query-passwords\n" +
            "management-hold\n" +
            "\n" +
            "setenv IV_GUI_VER \"se.leap.bitmaskclient 0.9.10\" \n" +
            "setenv IV_PLAT_VER \"28 9 ROBO Android unknown robolectric\"\n" +
            "machine-readable-output\n" +
            "allow-recursive-routing\n" +
            "ifconfig-nowarn\n" +
            "client\n" +
            "verb 4\n" +
            "connect-retry 2 300\n" +
            "resolv-retry 60\n" +
            "dev tun\n" +
            "remote 2001:db8:123::1056 1195 tcp-client\n" +
            "remote 37.218.247.60 1195 tcp-client\n" +
            "remote 2001:db8:123::1056 1195 udp\n" +
            "remote 37.218.247.60 1195 udp\n" +
            "<ca>\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIFbzCCA1egAwIBAgIBATANBgkqhkiG9w0BAQ0FADBKMRgwFgYDVQQDDA9CaXRt\n" +
            "YXNrIFJvb3QgQ0ExEDAOBgNVBAoMB0JpdG1hc2sxHDAaBgNVBAsME2h0dHBzOi8v\n" +
            "Yml0bWFzay5uZXQwHhcNMTIxMTA2MDAwMDAwWhcNMjIxMTA2MDAwMDAwWjBKMRgw\n" +
            "FgYDVQQDDA9CaXRtYXNrIFJvb3QgQ0ExEDAOBgNVBAoMB0JpdG1hc2sxHDAaBgNV\n" +
            "BAsME2h0dHBzOi8vYml0bWFzay5uZXQwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAw\n" +
            "ggIKAoICAQC1eV4YvayaU+maJbWrD4OHo3d7S1BtDlcvkIRS1Fw3iYDjsyDkZxai\n" +
            "dHp4EUasfNQ+EVtXUvtk6170EmLco6Elg8SJBQ27trE6nielPRPCfX3fQzETRfvB\n" +
            "7tNvGw4Jn2YKiYoMD79kkjgyZjkJ2r/bEHUSevmR09BRp86syHZerdNGpXYhcQ84\n" +
            "CA1+V+603GFIHnrP+uQDdssW93rgDNYu+exT+Wj6STfnUkugyjmPRPjL7wh0tzy+\n" +
            "znCeLl4xiV3g9sjPnc7r2EQKd5uaTe3j71sDPF92KRk0SSUndREz+B1+Dbe/RGk4\n" +
            "MEqGFuOzrtsgEhPIX0hplhb0Tgz/rtug+yTT7oJjBa3u20AAOQ38/M99EfdeJvc4\n" +
            "lPFF1XBBLh6X9UKF72an2NuANiX6XPySnJgZ7nZ09RiYZqVwu/qt3DfvLfhboq+0\n" +
            "bQvLUPXrVDr70onv5UDjpmEA/cLmaIqqrduuTkFZOym65/PfAPvpGnt7crQj/Ibl\n" +
            "DEDYZQmP7AS+6zBjoOzNjUGE5r40zWAR1RSi7zliXTu+yfsjXUIhUAWmYR6J3KxB\n" +
            "lfsiHBQ+8dn9kC3YrUexWoOqBiqJOAJzZh5Y1tqgzfh+2nmHSB2dsQRs7rDRRlyy\n" +
            "YMbkpzL9ZsOUO2eTP1mmar6YjCN+rggYjRrX71K2SpBG6b1zZxOG+wIDAQABo2Aw\n" +
            "XjAdBgNVHQ4EFgQUuYGDLL2sswnYpHHvProt1JU+D48wDgYDVR0PAQH/BAQDAgIE\n" +
            "MAwGA1UdEwQFMAMBAf8wHwYDVR0jBBgwFoAUuYGDLL2sswnYpHHvProt1JU+D48w\n" +
            "DQYJKoZIhvcNAQENBQADggIBADeG67vaFcbITGpi51264kHPYPEWaXUa5XYbtmBl\n" +
            "cXYyB6hY5hv/YNuVGJ1gWsDmdeXEyj0j2icGQjYdHRfwhrbEri+h1EZOm1cSBDuY\n" +
            "k/P5+ctHyOXx8IE79DBsZ6IL61UKIaKhqZBfLGYcWu17DVV6+LT+AKtHhOrv3TSj\n" +
            "RnAcKnCbKqXLhUPXpK0eTjPYS2zQGQGIhIy9sQXVXJJJsGrPgMxna1Xw2JikBOCG\n" +
            "htD/JKwt6xBmNwktH0GI/LVtVgSp82Clbn9C4eZN9E5YbVYjLkIEDhpByeC71QhX\n" +
            "EIQ0ZR56bFuJA/CwValBqV/G9gscTPQqd+iETp8yrFpAVHOW+YzSFbxjTEkBte1J\n" +
            "aF0vmbqdMAWLk+LEFPQRptZh0B88igtx6tV5oVd+p5IVRM49poLhuPNJGPvMj99l\n" +
            "mlZ4+AeRUnbOOeAEuvpLJbel4rhwFzmUiGoeTVoPZyMevWcVFq6BMkS+jRR2w0jK\n" +
            "G6b0v5XDHlcFYPOgUrtsOBFJVwbutLvxdk6q37kIFnWCd8L3kmES5q4wjyFK47Co\n" +
            "Ja8zlx64jmMZPg/t3wWqkZgXZ14qnbyG5/lGsj5CwVtfDljrhN0oCWK1FZaUmW3d\n" +
            "69db12/g4f6phldhxiWuGC/W6fCW5kre7nmhshcltqAJJuU47iX+DarBFiIj816e\n" +
            "yV8e\n" +
            "-----END CERTIFICATE-----\n" +
            "\n" +
            "</ca>\n" +
            "<cert>\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIEjDCCAnSgAwIBAgIQG6MBp/cd9DlY+7cdvp3R3jANBgkqhkiG9w0BAQsFADBmMRAwDgYDVQQK\n" +
            "DAdCaXRtYXNrMRwwGgYDVQQLDBNodHRwczovL2JpdG1hc2submV0MTQwMgYDVQQDDCtCaXRtYXNr\n" +
            "IFJvb3QgQ0EgKGNsaWVudCBjZXJ0aWZpY2F0ZXMgb25seSEpMB4XDTE0MTIwNTAwMDAwMFoXDTE1\n" +
            "MDMwNTAwMDAwMFowLTErMCkGA1UEAwwiVU5MSU1JVEVEZDBwZDdkMzE4eTNtOHNkeXllaTFqYmZl\n" +
            "eDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANRNhZ4aCwdL5+OKObOKeI2rDqEwGnIr\n" +
            "hL9wzo/FXbwLfdW45Y9Mxwhh6xy2NkA1YUKCB8VNBKNXlBrGr1QriLbu1rItsJ2VVLqGluVV/gO4\n" +
            "jcaPU+/Wu0hMFKG28J/dPvIGeNbjBWk6mxQAA5WIpRK9RTeQ88wVaGIZDDzIdivza2zpcyiPAyii\n" +
            "dbkyXh7sLsKvbZB6wLrert6Y1ylR3SlkZP0LfdGAMAdkMyuXKOjgcSnUltR8HSBuZcSUlsTVM11n\n" +
            "rYeGCYyPNNQ3UYatDW33UASgRDBorrmjhhKP7IW/opdlnPk5ZrP3i0qI32/boRe0EWZGXJvr4P3K\n" +
            "dJ30uCECAwEAAaNvMG0wHQYDVR0OBBYEFK8bMVAM4GBB5sHptoIOAaIvlYueMAsGA1UdDwQEAwIH\n" +
            "gDATBgNVHSUEDDAKBggrBgEFBQcDAjAJBgNVHRMEAjAAMB8GA1UdIwQYMBaAFId+E7bsWFsUWah9\n" +
            "vZuPvZ7O+aJsMA0GCSqGSIb3DQEBCwUAA4ICAQAQOX81csVhvP422NKkZH7+g3npBpl+sEHedaGR\n" +
            "xYPOu4HrA4TVF9h44sljRoRJyenGNdBZCXcLKHg889eePTf8Z5K3lTojp6hvwyA6tgxOMHT1kESW\n" +
            "PfqnRw8mHfHJuE3g+4YNUMwggzwc/VZATdV/7M33sarVN9AUOHou9n9BizgCC+UnYlS+F2POumE3\n" +
            "FbOhKo5uubI02MwBYlN2JVO2TBt1Q20w8wc6cU07Xi5Epp+1mkgFiOShkNtPcJmEyBWJhxDtSDOW\n" +
            "2doqWYNqH2kq7B5R/kyyfcpFJqAnBTV7xs+C5rTS1mW7LpxfdCUMbYuLCpyxpO3A/DhAm8n47tUH\n" +
            "lBtmo8Avdb8VdFpYiGBpB0o9kTFcsWFb2GkWFBduGfSEB8jUI7QtqhgZqocAKK/cweSRV8FwyUcn\n" +
            "R0prRm3QEi9fbXqEddzjSY9y/lqWYzT7u+IOAQpKroeZ4wzgYperDNOUFuYk1rP7yuvjP2pV5rcN\n" +
            "yPoBP60TPVWMRM4WJm6nTogAz2qBrFsf/XwT/ajzbsjT6HNB7QbRE+wkFkqspoXG5Agp7KQ8lW3L\n" +
            "SKCDGOQJz7VIE85pD0tg7QEXBEw8oaRZtMjQ0Gvs25mxXAKka4wGasaWfYH6d0E+iKYcWn86V1rH\n" +
            "K2ZoknT+Nno5jgjFuUR3fZseNizEfx7BteooKQ==\n" +
            "-----END CERTIFICATE-----\n" +
            "</cert>\n" +
            "management-external-key nopadding pkcs1 pss digest\n" +
            //"# crl-verify file missing in config profile\n" +
            "comp-lzo\n" +
            "nobind\n"+
            "remote-cert-tls server\n" +
            "data-ciphers AES-256-GCM:AES-128-GCM:AES-256-CBC\n" +
            "cipher AES-256-CBC\n" +
            "auth SHA1\n" +
            "persist-tun\n" +
            "# persist-tun also enables pre resolving to avoid DNS resolve problem\n" +
            "preresolve\n" +
            "# Use system proxy setting\n" +
            "management-query-proxy\n" +
            "# Custom configuration options\n" +
            "# You are on your on own here :)\n" +
            "# These options found in the config file do not map to config settings:\n" +
            "keepalive 10 30 \n" +
            "tls-cipher DHE-RSA-AES128-SHA \n" +
            "sndbuf 0 \n" +
            "rcvbuf 0 \n";

    String expectedVPNConfig_v4_ovpn_udp_tcp = "# Config for OpenVPN 2.x\n" +
            "# Enables connection to GUI\n" +
            "management /data/data/se.leap.bitmask/mgmtsocket unix\n" +
            "management-client\n" +
            "management-query-passwords\n" +
            "management-hold\n" +
            "\n" +
            "setenv IV_GUI_VER \"se.leap.bitmaskclient 0.9.10\" \n" +
            "setenv IV_PLAT_VER \"28 9 ROBO Android unknown robolectric\"\n" +
            "machine-readable-output\n" +
            "allow-recursive-routing\n" +
            "ifconfig-nowarn\n" +
            "client\n" +
            "verb 4\n" +
            "connect-retry 2 300\n" +
            "resolv-retry 60\n" +
            "dev tun\n" +
            "remote 2001:db8:123::1056 1195 udp\n" +
            "remote 37.218.247.60 1195 udp\n" +
            "remote 2001:db8:123::1056 1195 tcp-client\n" +
            "remote 37.218.247.60 1195 tcp-client\n" +
            "<ca>\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIFbzCCA1egAwIBAgIBATANBgkqhkiG9w0BAQ0FADBKMRgwFgYDVQQDDA9CaXRt\n" +
            "YXNrIFJvb3QgQ0ExEDAOBgNVBAoMB0JpdG1hc2sxHDAaBgNVBAsME2h0dHBzOi8v\n" +
            "Yml0bWFzay5uZXQwHhcNMTIxMTA2MDAwMDAwWhcNMjIxMTA2MDAwMDAwWjBKMRgw\n" +
            "FgYDVQQDDA9CaXRtYXNrIFJvb3QgQ0ExEDAOBgNVBAoMB0JpdG1hc2sxHDAaBgNV\n" +
            "BAsME2h0dHBzOi8vYml0bWFzay5uZXQwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAw\n" +
            "ggIKAoICAQC1eV4YvayaU+maJbWrD4OHo3d7S1BtDlcvkIRS1Fw3iYDjsyDkZxai\n" +
            "dHp4EUasfNQ+EVtXUvtk6170EmLco6Elg8SJBQ27trE6nielPRPCfX3fQzETRfvB\n" +
            "7tNvGw4Jn2YKiYoMD79kkjgyZjkJ2r/bEHUSevmR09BRp86syHZerdNGpXYhcQ84\n" +
            "CA1+V+603GFIHnrP+uQDdssW93rgDNYu+exT+Wj6STfnUkugyjmPRPjL7wh0tzy+\n" +
            "znCeLl4xiV3g9sjPnc7r2EQKd5uaTe3j71sDPF92KRk0SSUndREz+B1+Dbe/RGk4\n" +
            "MEqGFuOzrtsgEhPIX0hplhb0Tgz/rtug+yTT7oJjBa3u20AAOQ38/M99EfdeJvc4\n" +
            "lPFF1XBBLh6X9UKF72an2NuANiX6XPySnJgZ7nZ09RiYZqVwu/qt3DfvLfhboq+0\n" +
            "bQvLUPXrVDr70onv5UDjpmEA/cLmaIqqrduuTkFZOym65/PfAPvpGnt7crQj/Ibl\n" +
            "DEDYZQmP7AS+6zBjoOzNjUGE5r40zWAR1RSi7zliXTu+yfsjXUIhUAWmYR6J3KxB\n" +
            "lfsiHBQ+8dn9kC3YrUexWoOqBiqJOAJzZh5Y1tqgzfh+2nmHSB2dsQRs7rDRRlyy\n" +
            "YMbkpzL9ZsOUO2eTP1mmar6YjCN+rggYjRrX71K2SpBG6b1zZxOG+wIDAQABo2Aw\n" +
            "XjAdBgNVHQ4EFgQUuYGDLL2sswnYpHHvProt1JU+D48wDgYDVR0PAQH/BAQDAgIE\n" +
            "MAwGA1UdEwQFMAMBAf8wHwYDVR0jBBgwFoAUuYGDLL2sswnYpHHvProt1JU+D48w\n" +
            "DQYJKoZIhvcNAQENBQADggIBADeG67vaFcbITGpi51264kHPYPEWaXUa5XYbtmBl\n" +
            "cXYyB6hY5hv/YNuVGJ1gWsDmdeXEyj0j2icGQjYdHRfwhrbEri+h1EZOm1cSBDuY\n" +
            "k/P5+ctHyOXx8IE79DBsZ6IL61UKIaKhqZBfLGYcWu17DVV6+LT+AKtHhOrv3TSj\n" +
            "RnAcKnCbKqXLhUPXpK0eTjPYS2zQGQGIhIy9sQXVXJJJsGrPgMxna1Xw2JikBOCG\n" +
            "htD/JKwt6xBmNwktH0GI/LVtVgSp82Clbn9C4eZN9E5YbVYjLkIEDhpByeC71QhX\n" +
            "EIQ0ZR56bFuJA/CwValBqV/G9gscTPQqd+iETp8yrFpAVHOW+YzSFbxjTEkBte1J\n" +
            "aF0vmbqdMAWLk+LEFPQRptZh0B88igtx6tV5oVd+p5IVRM49poLhuPNJGPvMj99l\n" +
            "mlZ4+AeRUnbOOeAEuvpLJbel4rhwFzmUiGoeTVoPZyMevWcVFq6BMkS+jRR2w0jK\n" +
            "G6b0v5XDHlcFYPOgUrtsOBFJVwbutLvxdk6q37kIFnWCd8L3kmES5q4wjyFK47Co\n" +
            "Ja8zlx64jmMZPg/t3wWqkZgXZ14qnbyG5/lGsj5CwVtfDljrhN0oCWK1FZaUmW3d\n" +
            "69db12/g4f6phldhxiWuGC/W6fCW5kre7nmhshcltqAJJuU47iX+DarBFiIj816e\n" +
            "yV8e\n" +
            "-----END CERTIFICATE-----\n" +
            "\n" +
            "</ca>\n" +
            "<cert>\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIEjDCCAnSgAwIBAgIQG6MBp/cd9DlY+7cdvp3R3jANBgkqhkiG9w0BAQsFADBmMRAwDgYDVQQK\n" +
            "DAdCaXRtYXNrMRwwGgYDVQQLDBNodHRwczovL2JpdG1hc2submV0MTQwMgYDVQQDDCtCaXRtYXNr\n" +
            "IFJvb3QgQ0EgKGNsaWVudCBjZXJ0aWZpY2F0ZXMgb25seSEpMB4XDTE0MTIwNTAwMDAwMFoXDTE1\n" +
            "MDMwNTAwMDAwMFowLTErMCkGA1UEAwwiVU5MSU1JVEVEZDBwZDdkMzE4eTNtOHNkeXllaTFqYmZl\n" +
            "eDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANRNhZ4aCwdL5+OKObOKeI2rDqEwGnIr\n" +
            "hL9wzo/FXbwLfdW45Y9Mxwhh6xy2NkA1YUKCB8VNBKNXlBrGr1QriLbu1rItsJ2VVLqGluVV/gO4\n" +
            "jcaPU+/Wu0hMFKG28J/dPvIGeNbjBWk6mxQAA5WIpRK9RTeQ88wVaGIZDDzIdivza2zpcyiPAyii\n" +
            "dbkyXh7sLsKvbZB6wLrert6Y1ylR3SlkZP0LfdGAMAdkMyuXKOjgcSnUltR8HSBuZcSUlsTVM11n\n" +
            "rYeGCYyPNNQ3UYatDW33UASgRDBorrmjhhKP7IW/opdlnPk5ZrP3i0qI32/boRe0EWZGXJvr4P3K\n" +
            "dJ30uCECAwEAAaNvMG0wHQYDVR0OBBYEFK8bMVAM4GBB5sHptoIOAaIvlYueMAsGA1UdDwQEAwIH\n" +
            "gDATBgNVHSUEDDAKBggrBgEFBQcDAjAJBgNVHRMEAjAAMB8GA1UdIwQYMBaAFId+E7bsWFsUWah9\n" +
            "vZuPvZ7O+aJsMA0GCSqGSIb3DQEBCwUAA4ICAQAQOX81csVhvP422NKkZH7+g3npBpl+sEHedaGR\n" +
            "xYPOu4HrA4TVF9h44sljRoRJyenGNdBZCXcLKHg889eePTf8Z5K3lTojp6hvwyA6tgxOMHT1kESW\n" +
            "PfqnRw8mHfHJuE3g+4YNUMwggzwc/VZATdV/7M33sarVN9AUOHou9n9BizgCC+UnYlS+F2POumE3\n" +
            "FbOhKo5uubI02MwBYlN2JVO2TBt1Q20w8wc6cU07Xi5Epp+1mkgFiOShkNtPcJmEyBWJhxDtSDOW\n" +
            "2doqWYNqH2kq7B5R/kyyfcpFJqAnBTV7xs+C5rTS1mW7LpxfdCUMbYuLCpyxpO3A/DhAm8n47tUH\n" +
            "lBtmo8Avdb8VdFpYiGBpB0o9kTFcsWFb2GkWFBduGfSEB8jUI7QtqhgZqocAKK/cweSRV8FwyUcn\n" +
            "R0prRm3QEi9fbXqEddzjSY9y/lqWYzT7u+IOAQpKroeZ4wzgYperDNOUFuYk1rP7yuvjP2pV5rcN\n" +
            "yPoBP60TPVWMRM4WJm6nTogAz2qBrFsf/XwT/ajzbsjT6HNB7QbRE+wkFkqspoXG5Agp7KQ8lW3L\n" +
            "SKCDGOQJz7VIE85pD0tg7QEXBEw8oaRZtMjQ0Gvs25mxXAKka4wGasaWfYH6d0E+iKYcWn86V1rH\n" +
            "K2ZoknT+Nno5jgjFuUR3fZseNizEfx7BteooKQ==\n" +
            "-----END CERTIFICATE-----\n" +
            "</cert>\n" +
            "management-external-key nopadding pkcs1 pss digest\n" +
            //"# crl-verify file missing in config profile\n" +
            "comp-lzo\n" +
            "nobind\n"+
            "remote-cert-tls server\n" +
            "data-ciphers AES-256-GCM:AES-128-GCM:AES-256-CBC\n" +
            "cipher AES-256-CBC\n" +
            "auth SHA1\n" +
            "persist-tun\n" +
            "# persist-tun also enables pre resolving to avoid DNS resolve problem\n" +
            "preresolve\n" +
            "# Use system proxy setting\n" +
            "management-query-proxy\n" +
            "# Custom configuration options\n" +
            "# You are on your on own here :)\n" +
            "# These options found in the config file do not map to config settings:\n" +
            "keepalive 10 30 \n" +
            "tls-cipher DHE-RSA-AES128-SHA \n" +
            "sndbuf 0 \n" +
            "rcvbuf 0 \n";

    String expectedVPNConfig_v4_ovpn_multiport_tcpudp = "# Config for OpenVPN 2.x\n" +
            "# Enables connection to GUI\n" +
            "management /data/data/se.leap.bitmask/mgmtsocket unix\n" +
            "management-client\n" +
            "management-query-passwords\n" +
            "management-hold\n" +
            "\n" +
            "setenv IV_GUI_VER \"se.leap.bitmaskclient 0.9.10\" \n" +
            "setenv IV_PLAT_VER \"28 9 ROBO Android unknown robolectric\"\n" +
            "machine-readable-output\n" +
            "allow-recursive-routing\n" +
            "ifconfig-nowarn\n" +
            "client\n" +
            "verb 4\n" +
            "connect-retry 2 300\n" +
            "resolv-retry 60\n" +
            "dev tun\n" +
            "remote 2001:db8:123::1056 53 udp\n" +
            "remote 37.218.247.60 53 udp\n" +
            "remote 2001:db8:123::1056 80 udp\n" +
            "remote 37.218.247.60 80 udp\n" +
            "remote 2001:db8:123::1056 1194 udp\n" +
            "remote 37.218.247.60 1194 udp\n" +
            "remote 2001:db8:123::1056 53 tcp-client\n" +
            "remote 37.218.247.60 53 tcp-client\n" +
            "remote 2001:db8:123::1056 80 tcp-client\n" +
            "remote 37.218.247.60 80 tcp-client\n" +
            "remote 2001:db8:123::1056 1194 tcp-client\n" +
            "remote 37.218.247.60 1194 tcp-client\n" +
            "<ca>\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIFbzCCA1egAwIBAgIBATANBgkqhkiG9w0BAQ0FADBKMRgwFgYDVQQDDA9CaXRt\n" +
            "YXNrIFJvb3QgQ0ExEDAOBgNVBAoMB0JpdG1hc2sxHDAaBgNVBAsME2h0dHBzOi8v\n" +
            "Yml0bWFzay5uZXQwHhcNMTIxMTA2MDAwMDAwWhcNMjIxMTA2MDAwMDAwWjBKMRgw\n" +
            "FgYDVQQDDA9CaXRtYXNrIFJvb3QgQ0ExEDAOBgNVBAoMB0JpdG1hc2sxHDAaBgNV\n" +
            "BAsME2h0dHBzOi8vYml0bWFzay5uZXQwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAw\n" +
            "ggIKAoICAQC1eV4YvayaU+maJbWrD4OHo3d7S1BtDlcvkIRS1Fw3iYDjsyDkZxai\n" +
            "dHp4EUasfNQ+EVtXUvtk6170EmLco6Elg8SJBQ27trE6nielPRPCfX3fQzETRfvB\n" +
            "7tNvGw4Jn2YKiYoMD79kkjgyZjkJ2r/bEHUSevmR09BRp86syHZerdNGpXYhcQ84\n" +
            "CA1+V+603GFIHnrP+uQDdssW93rgDNYu+exT+Wj6STfnUkugyjmPRPjL7wh0tzy+\n" +
            "znCeLl4xiV3g9sjPnc7r2EQKd5uaTe3j71sDPF92KRk0SSUndREz+B1+Dbe/RGk4\n" +
            "MEqGFuOzrtsgEhPIX0hplhb0Tgz/rtug+yTT7oJjBa3u20AAOQ38/M99EfdeJvc4\n" +
            "lPFF1XBBLh6X9UKF72an2NuANiX6XPySnJgZ7nZ09RiYZqVwu/qt3DfvLfhboq+0\n" +
            "bQvLUPXrVDr70onv5UDjpmEA/cLmaIqqrduuTkFZOym65/PfAPvpGnt7crQj/Ibl\n" +
            "DEDYZQmP7AS+6zBjoOzNjUGE5r40zWAR1RSi7zliXTu+yfsjXUIhUAWmYR6J3KxB\n" +
            "lfsiHBQ+8dn9kC3YrUexWoOqBiqJOAJzZh5Y1tqgzfh+2nmHSB2dsQRs7rDRRlyy\n" +
            "YMbkpzL9ZsOUO2eTP1mmar6YjCN+rggYjRrX71K2SpBG6b1zZxOG+wIDAQABo2Aw\n" +
            "XjAdBgNVHQ4EFgQUuYGDLL2sswnYpHHvProt1JU+D48wDgYDVR0PAQH/BAQDAgIE\n" +
            "MAwGA1UdEwQFMAMBAf8wHwYDVR0jBBgwFoAUuYGDLL2sswnYpHHvProt1JU+D48w\n" +
            "DQYJKoZIhvcNAQENBQADggIBADeG67vaFcbITGpi51264kHPYPEWaXUa5XYbtmBl\n" +
            "cXYyB6hY5hv/YNuVGJ1gWsDmdeXEyj0j2icGQjYdHRfwhrbEri+h1EZOm1cSBDuY\n" +
            "k/P5+ctHyOXx8IE79DBsZ6IL61UKIaKhqZBfLGYcWu17DVV6+LT+AKtHhOrv3TSj\n" +
            "RnAcKnCbKqXLhUPXpK0eTjPYS2zQGQGIhIy9sQXVXJJJsGrPgMxna1Xw2JikBOCG\n" +
            "htD/JKwt6xBmNwktH0GI/LVtVgSp82Clbn9C4eZN9E5YbVYjLkIEDhpByeC71QhX\n" +
            "EIQ0ZR56bFuJA/CwValBqV/G9gscTPQqd+iETp8yrFpAVHOW+YzSFbxjTEkBte1J\n" +
            "aF0vmbqdMAWLk+LEFPQRptZh0B88igtx6tV5oVd+p5IVRM49poLhuPNJGPvMj99l\n" +
            "mlZ4+AeRUnbOOeAEuvpLJbel4rhwFzmUiGoeTVoPZyMevWcVFq6BMkS+jRR2w0jK\n" +
            "G6b0v5XDHlcFYPOgUrtsOBFJVwbutLvxdk6q37kIFnWCd8L3kmES5q4wjyFK47Co\n" +
            "Ja8zlx64jmMZPg/t3wWqkZgXZ14qnbyG5/lGsj5CwVtfDljrhN0oCWK1FZaUmW3d\n" +
            "69db12/g4f6phldhxiWuGC/W6fCW5kre7nmhshcltqAJJuU47iX+DarBFiIj816e\n" +
            "yV8e\n" +
            "-----END CERTIFICATE-----\n" +
            "\n" +
            "</ca>\n" +
            "<cert>\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIEjDCCAnSgAwIBAgIQG6MBp/cd9DlY+7cdvp3R3jANBgkqhkiG9w0BAQsFADBmMRAwDgYDVQQK\n" +
            "DAdCaXRtYXNrMRwwGgYDVQQLDBNodHRwczovL2JpdG1hc2submV0MTQwMgYDVQQDDCtCaXRtYXNr\n" +
            "IFJvb3QgQ0EgKGNsaWVudCBjZXJ0aWZpY2F0ZXMgb25seSEpMB4XDTE0MTIwNTAwMDAwMFoXDTE1\n" +
            "MDMwNTAwMDAwMFowLTErMCkGA1UEAwwiVU5MSU1JVEVEZDBwZDdkMzE4eTNtOHNkeXllaTFqYmZl\n" +
            "eDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANRNhZ4aCwdL5+OKObOKeI2rDqEwGnIr\n" +
            "hL9wzo/FXbwLfdW45Y9Mxwhh6xy2NkA1YUKCB8VNBKNXlBrGr1QriLbu1rItsJ2VVLqGluVV/gO4\n" +
            "jcaPU+/Wu0hMFKG28J/dPvIGeNbjBWk6mxQAA5WIpRK9RTeQ88wVaGIZDDzIdivza2zpcyiPAyii\n" +
            "dbkyXh7sLsKvbZB6wLrert6Y1ylR3SlkZP0LfdGAMAdkMyuXKOjgcSnUltR8HSBuZcSUlsTVM11n\n" +
            "rYeGCYyPNNQ3UYatDW33UASgRDBorrmjhhKP7IW/opdlnPk5ZrP3i0qI32/boRe0EWZGXJvr4P3K\n" +
            "dJ30uCECAwEAAaNvMG0wHQYDVR0OBBYEFK8bMVAM4GBB5sHptoIOAaIvlYueMAsGA1UdDwQEAwIH\n" +
            "gDATBgNVHSUEDDAKBggrBgEFBQcDAjAJBgNVHRMEAjAAMB8GA1UdIwQYMBaAFId+E7bsWFsUWah9\n" +
            "vZuPvZ7O+aJsMA0GCSqGSIb3DQEBCwUAA4ICAQAQOX81csVhvP422NKkZH7+g3npBpl+sEHedaGR\n" +
            "xYPOu4HrA4TVF9h44sljRoRJyenGNdBZCXcLKHg889eePTf8Z5K3lTojp6hvwyA6tgxOMHT1kESW\n" +
            "PfqnRw8mHfHJuE3g+4YNUMwggzwc/VZATdV/7M33sarVN9AUOHou9n9BizgCC+UnYlS+F2POumE3\n" +
            "FbOhKo5uubI02MwBYlN2JVO2TBt1Q20w8wc6cU07Xi5Epp+1mkgFiOShkNtPcJmEyBWJhxDtSDOW\n" +
            "2doqWYNqH2kq7B5R/kyyfcpFJqAnBTV7xs+C5rTS1mW7LpxfdCUMbYuLCpyxpO3A/DhAm8n47tUH\n" +
            "lBtmo8Avdb8VdFpYiGBpB0o9kTFcsWFb2GkWFBduGfSEB8jUI7QtqhgZqocAKK/cweSRV8FwyUcn\n" +
            "R0prRm3QEi9fbXqEddzjSY9y/lqWYzT7u+IOAQpKroeZ4wzgYperDNOUFuYk1rP7yuvjP2pV5rcN\n" +
            "yPoBP60TPVWMRM4WJm6nTogAz2qBrFsf/XwT/ajzbsjT6HNB7QbRE+wkFkqspoXG5Agp7KQ8lW3L\n" +
            "SKCDGOQJz7VIE85pD0tg7QEXBEw8oaRZtMjQ0Gvs25mxXAKka4wGasaWfYH6d0E+iKYcWn86V1rH\n" +
            "K2ZoknT+Nno5jgjFuUR3fZseNizEfx7BteooKQ==\n" +
            "-----END CERTIFICATE-----\n" +
            "</cert>\n" +
            "management-external-key nopadding pkcs1 pss digest\n" +
            //"# crl-verify file missing in config profile\n" +
            "comp-lzo\n" +
            "nobind\n"+
            "remote-cert-tls server\n" +
            "data-ciphers AES-256-GCM:AES-128-GCM:AES-256-CBC\n" +
            "cipher AES-256-CBC\n" +
            "auth SHA1\n" +
            "persist-tun\n" +
            "# persist-tun also enables pre resolving to avoid DNS resolve problem\n" +
            "preresolve\n" +
            "# Use system proxy setting\n" +
            "management-query-proxy\n" +
            "# Custom configuration options\n" +
            "# You are on your on own here :)\n" +
            "# These options found in the config file do not map to config settings:\n" +
            "keepalive 10 30 \n" +
            "tls-cipher DHE-RSA-AES128-SHA \n" +
            "sndbuf 0 \n" +
            "rcvbuf 0 \n";


    String expectedVPNConfig_v4_ovpn_tcp_udp_new_ciphers = "# Config for OpenVPN 2.x\n" +
            "# Enables connection to GUI\n" +
            "management /data/data/se.leap.bitmask/mgmtsocket unix\n" +
            "management-client\n" +
            "management-query-passwords\n" +
            "management-hold\n" +
            "\n" +
            "setenv IV_GUI_VER \"se.leap.bitmaskclient 0.9.10\" \n" +
            "setenv IV_PLAT_VER \"28 9 ROBO Android unknown robolectric\"\n" +
            "machine-readable-output\n" +
            "allow-recursive-routing\n" +
            "ifconfig-nowarn\n" +
            "client\n" +
            "verb 4\n" +
            "connect-retry 2 300\n" +
            "resolv-retry 60\n" +
            "dev tun\n" +
            "remote 2001:db8:123::1056 1195 tcp-client\n" +
            "remote 37.218.247.60 1195 tcp-client\n" +
            "remote 2001:db8:123::1056 1195 udp\n" +
            "remote 37.218.247.60 1195 udp\n" +
            "<ca>\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIFbzCCA1egAwIBAgIBATANBgkqhkiG9w0BAQ0FADBKMRgwFgYDVQQDDA9CaXRt\n" +
            "YXNrIFJvb3QgQ0ExEDAOBgNVBAoMB0JpdG1hc2sxHDAaBgNVBAsME2h0dHBzOi8v\n" +
            "Yml0bWFzay5uZXQwHhcNMTIxMTA2MDAwMDAwWhcNMjIxMTA2MDAwMDAwWjBKMRgw\n" +
            "FgYDVQQDDA9CaXRtYXNrIFJvb3QgQ0ExEDAOBgNVBAoMB0JpdG1hc2sxHDAaBgNV\n" +
            "BAsME2h0dHBzOi8vYml0bWFzay5uZXQwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAw\n" +
            "ggIKAoICAQC1eV4YvayaU+maJbWrD4OHo3d7S1BtDlcvkIRS1Fw3iYDjsyDkZxai\n" +
            "dHp4EUasfNQ+EVtXUvtk6170EmLco6Elg8SJBQ27trE6nielPRPCfX3fQzETRfvB\n" +
            "7tNvGw4Jn2YKiYoMD79kkjgyZjkJ2r/bEHUSevmR09BRp86syHZerdNGpXYhcQ84\n" +
            "CA1+V+603GFIHnrP+uQDdssW93rgDNYu+exT+Wj6STfnUkugyjmPRPjL7wh0tzy+\n" +
            "znCeLl4xiV3g9sjPnc7r2EQKd5uaTe3j71sDPF92KRk0SSUndREz+B1+Dbe/RGk4\n" +
            "MEqGFuOzrtsgEhPIX0hplhb0Tgz/rtug+yTT7oJjBa3u20AAOQ38/M99EfdeJvc4\n" +
            "lPFF1XBBLh6X9UKF72an2NuANiX6XPySnJgZ7nZ09RiYZqVwu/qt3DfvLfhboq+0\n" +
            "bQvLUPXrVDr70onv5UDjpmEA/cLmaIqqrduuTkFZOym65/PfAPvpGnt7crQj/Ibl\n" +
            "DEDYZQmP7AS+6zBjoOzNjUGE5r40zWAR1RSi7zliXTu+yfsjXUIhUAWmYR6J3KxB\n" +
            "lfsiHBQ+8dn9kC3YrUexWoOqBiqJOAJzZh5Y1tqgzfh+2nmHSB2dsQRs7rDRRlyy\n" +
            "YMbkpzL9ZsOUO2eTP1mmar6YjCN+rggYjRrX71K2SpBG6b1zZxOG+wIDAQABo2Aw\n" +
            "XjAdBgNVHQ4EFgQUuYGDLL2sswnYpHHvProt1JU+D48wDgYDVR0PAQH/BAQDAgIE\n" +
            "MAwGA1UdEwQFMAMBAf8wHwYDVR0jBBgwFoAUuYGDLL2sswnYpHHvProt1JU+D48w\n" +
            "DQYJKoZIhvcNAQENBQADggIBADeG67vaFcbITGpi51264kHPYPEWaXUa5XYbtmBl\n" +
            "cXYyB6hY5hv/YNuVGJ1gWsDmdeXEyj0j2icGQjYdHRfwhrbEri+h1EZOm1cSBDuY\n" +
            "k/P5+ctHyOXx8IE79DBsZ6IL61UKIaKhqZBfLGYcWu17DVV6+LT+AKtHhOrv3TSj\n" +
            "RnAcKnCbKqXLhUPXpK0eTjPYS2zQGQGIhIy9sQXVXJJJsGrPgMxna1Xw2JikBOCG\n" +
            "htD/JKwt6xBmNwktH0GI/LVtVgSp82Clbn9C4eZN9E5YbVYjLkIEDhpByeC71QhX\n" +
            "EIQ0ZR56bFuJA/CwValBqV/G9gscTPQqd+iETp8yrFpAVHOW+YzSFbxjTEkBte1J\n" +
            "aF0vmbqdMAWLk+LEFPQRptZh0B88igtx6tV5oVd+p5IVRM49poLhuPNJGPvMj99l\n" +
            "mlZ4+AeRUnbOOeAEuvpLJbel4rhwFzmUiGoeTVoPZyMevWcVFq6BMkS+jRR2w0jK\n" +
            "G6b0v5XDHlcFYPOgUrtsOBFJVwbutLvxdk6q37kIFnWCd8L3kmES5q4wjyFK47Co\n" +
            "Ja8zlx64jmMZPg/t3wWqkZgXZ14qnbyG5/lGsj5CwVtfDljrhN0oCWK1FZaUmW3d\n" +
            "69db12/g4f6phldhxiWuGC/W6fCW5kre7nmhshcltqAJJuU47iX+DarBFiIj816e\n" +
            "yV8e\n" +
            "-----END CERTIFICATE-----\n" +
            "\n" +
            "</ca>\n" +
            "<cert>\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIEjDCCAnSgAwIBAgIQG6MBp/cd9DlY+7cdvp3R3jANBgkqhkiG9w0BAQsFADBmMRAwDgYDVQQK\n" +
            "DAdCaXRtYXNrMRwwGgYDVQQLDBNodHRwczovL2JpdG1hc2submV0MTQwMgYDVQQDDCtCaXRtYXNr\n" +
            "IFJvb3QgQ0EgKGNsaWVudCBjZXJ0aWZpY2F0ZXMgb25seSEpMB4XDTE0MTIwNTAwMDAwMFoXDTE1\n" +
            "MDMwNTAwMDAwMFowLTErMCkGA1UEAwwiVU5MSU1JVEVEZDBwZDdkMzE4eTNtOHNkeXllaTFqYmZl\n" +
            "eDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANRNhZ4aCwdL5+OKObOKeI2rDqEwGnIr\n" +
            "hL9wzo/FXbwLfdW45Y9Mxwhh6xy2NkA1YUKCB8VNBKNXlBrGr1QriLbu1rItsJ2VVLqGluVV/gO4\n" +
            "jcaPU+/Wu0hMFKG28J/dPvIGeNbjBWk6mxQAA5WIpRK9RTeQ88wVaGIZDDzIdivza2zpcyiPAyii\n" +
            "dbkyXh7sLsKvbZB6wLrert6Y1ylR3SlkZP0LfdGAMAdkMyuXKOjgcSnUltR8HSBuZcSUlsTVM11n\n" +
            "rYeGCYyPNNQ3UYatDW33UASgRDBorrmjhhKP7IW/opdlnPk5ZrP3i0qI32/boRe0EWZGXJvr4P3K\n" +
            "dJ30uCECAwEAAaNvMG0wHQYDVR0OBBYEFK8bMVAM4GBB5sHptoIOAaIvlYueMAsGA1UdDwQEAwIH\n" +
            "gDATBgNVHSUEDDAKBggrBgEFBQcDAjAJBgNVHRMEAjAAMB8GA1UdIwQYMBaAFId+E7bsWFsUWah9\n" +
            "vZuPvZ7O+aJsMA0GCSqGSIb3DQEBCwUAA4ICAQAQOX81csVhvP422NKkZH7+g3npBpl+sEHedaGR\n" +
            "xYPOu4HrA4TVF9h44sljRoRJyenGNdBZCXcLKHg889eePTf8Z5K3lTojp6hvwyA6tgxOMHT1kESW\n" +
            "PfqnRw8mHfHJuE3g+4YNUMwggzwc/VZATdV/7M33sarVN9AUOHou9n9BizgCC+UnYlS+F2POumE3\n" +
            "FbOhKo5uubI02MwBYlN2JVO2TBt1Q20w8wc6cU07Xi5Epp+1mkgFiOShkNtPcJmEyBWJhxDtSDOW\n" +
            "2doqWYNqH2kq7B5R/kyyfcpFJqAnBTV7xs+C5rTS1mW7LpxfdCUMbYuLCpyxpO3A/DhAm8n47tUH\n" +
            "lBtmo8Avdb8VdFpYiGBpB0o9kTFcsWFb2GkWFBduGfSEB8jUI7QtqhgZqocAKK/cweSRV8FwyUcn\n" +
            "R0prRm3QEi9fbXqEddzjSY9y/lqWYzT7u+IOAQpKroeZ4wzgYperDNOUFuYk1rP7yuvjP2pV5rcN\n" +
            "yPoBP60TPVWMRM4WJm6nTogAz2qBrFsf/XwT/ajzbsjT6HNB7QbRE+wkFkqspoXG5Agp7KQ8lW3L\n" +
            "SKCDGOQJz7VIE85pD0tg7QEXBEw8oaRZtMjQ0Gvs25mxXAKka4wGasaWfYH6d0E+iKYcWn86V1rH\n" +
            "K2ZoknT+Nno5jgjFuUR3fZseNizEfx7BteooKQ==\n" +
            "-----END CERTIFICATE-----\n" +
            "</cert>\n" +
            "management-external-key nopadding pkcs1 pss digest\n" +
            //"# crl-verify file missing in config profile\n" +
            "nobind\n"+
            "remote-cert-tls server\n" +
            "data-ciphers AES-256-GCM:AES-128-CBC\n" +
            "cipher AES-256-GCM:AES-128-CBC\n" +
            "auth SHA1\n" +
            "float\n"+
            "persist-tun\n" +
            "# persist-tun also enables pre resolving to avoid DNS resolve problem\n" +
            "preresolve\n" +
            "# Use system proxy setting\n" +
            "management-query-proxy\n" +
            "# Custom configuration options\n" +
            "# You are on your on own here :)\n" +
            "# These options found in the config file do not map to config settings:\n" +
            "keepalive 10 30 \n" +
            "tls-cipher TLS-ECDHE-ECDSA-WITH-AES-256-GCM-SHA384:DHE-RSA-AES128-SHA \n" +
            "sndbuf 0 \n" +
            "rcvbuf 0 \n" +
            "tls-version-min 1.2 \n";

    String expectedVPNConfig_hopping_pt_portHopping = "# Config for OpenVPN 2.x\n" +
            "# Enables connection to GUI\n" +
            "management /data/data/se.leap.bitmask/mgmtsocket unix\n" +
            "management-client\n" +
            "management-query-passwords\n" +
            "management-hold\n" +
            "\n" +
            "setenv IV_GUI_VER \"se.leap.bitmaskclient 0.9.10\" \n" +
            "setenv IV_PLAT_VER \"28 9 ROBO Android unknown robolectric\"\n" +
            "machine-readable-output\n" +
            "allow-recursive-routing\n" +
            "ifconfig-nowarn\n" +
            "client\n" +
            "verb 4\n" +
            "connect-retry 2 300\n" +
            "resolv-retry 60\n" +
            "dev tun\n" +
            "remote 127.0.0.1 8080 udp\n" +
            "<ca>\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIFbzCCA1egAwIBAgIBATANBgkqhkiG9w0BAQ0FADBKMRgwFgYDVQQDDA9CaXRt\n" +
            "YXNrIFJvb3QgQ0ExEDAOBgNVBAoMB0JpdG1hc2sxHDAaBgNVBAsME2h0dHBzOi8v\n" +
            "Yml0bWFzay5uZXQwHhcNMTIxMTA2MDAwMDAwWhcNMjIxMTA2MDAwMDAwWjBKMRgw\n" +
            "FgYDVQQDDA9CaXRtYXNrIFJvb3QgQ0ExEDAOBgNVBAoMB0JpdG1hc2sxHDAaBgNV\n" +
            "BAsME2h0dHBzOi8vYml0bWFzay5uZXQwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAw\n" +
            "ggIKAoICAQC1eV4YvayaU+maJbWrD4OHo3d7S1BtDlcvkIRS1Fw3iYDjsyDkZxai\n" +
            "dHp4EUasfNQ+EVtXUvtk6170EmLco6Elg8SJBQ27trE6nielPRPCfX3fQzETRfvB\n" +
            "7tNvGw4Jn2YKiYoMD79kkjgyZjkJ2r/bEHUSevmR09BRp86syHZerdNGpXYhcQ84\n" +
            "CA1+V+603GFIHnrP+uQDdssW93rgDNYu+exT+Wj6STfnUkugyjmPRPjL7wh0tzy+\n" +
            "znCeLl4xiV3g9sjPnc7r2EQKd5uaTe3j71sDPF92KRk0SSUndREz+B1+Dbe/RGk4\n" +
            "MEqGFuOzrtsgEhPIX0hplhb0Tgz/rtug+yTT7oJjBa3u20AAOQ38/M99EfdeJvc4\n" +
            "lPFF1XBBLh6X9UKF72an2NuANiX6XPySnJgZ7nZ09RiYZqVwu/qt3DfvLfhboq+0\n" +
            "bQvLUPXrVDr70onv5UDjpmEA/cLmaIqqrduuTkFZOym65/PfAPvpGnt7crQj/Ibl\n" +
            "DEDYZQmP7AS+6zBjoOzNjUGE5r40zWAR1RSi7zliXTu+yfsjXUIhUAWmYR6J3KxB\n" +
            "lfsiHBQ+8dn9kC3YrUexWoOqBiqJOAJzZh5Y1tqgzfh+2nmHSB2dsQRs7rDRRlyy\n" +
            "YMbkpzL9ZsOUO2eTP1mmar6YjCN+rggYjRrX71K2SpBG6b1zZxOG+wIDAQABo2Aw\n" +
            "XjAdBgNVHQ4EFgQUuYGDLL2sswnYpHHvProt1JU+D48wDgYDVR0PAQH/BAQDAgIE\n" +
            "MAwGA1UdEwQFMAMBAf8wHwYDVR0jBBgwFoAUuYGDLL2sswnYpHHvProt1JU+D48w\n" +
            "DQYJKoZIhvcNAQENBQADggIBADeG67vaFcbITGpi51264kHPYPEWaXUa5XYbtmBl\n" +
            "cXYyB6hY5hv/YNuVGJ1gWsDmdeXEyj0j2icGQjYdHRfwhrbEri+h1EZOm1cSBDuY\n" +
            "k/P5+ctHyOXx8IE79DBsZ6IL61UKIaKhqZBfLGYcWu17DVV6+LT+AKtHhOrv3TSj\n" +
            "RnAcKnCbKqXLhUPXpK0eTjPYS2zQGQGIhIy9sQXVXJJJsGrPgMxna1Xw2JikBOCG\n" +
            "htD/JKwt6xBmNwktH0GI/LVtVgSp82Clbn9C4eZN9E5YbVYjLkIEDhpByeC71QhX\n" +
            "EIQ0ZR56bFuJA/CwValBqV/G9gscTPQqd+iETp8yrFpAVHOW+YzSFbxjTEkBte1J\n" +
            "aF0vmbqdMAWLk+LEFPQRptZh0B88igtx6tV5oVd+p5IVRM49poLhuPNJGPvMj99l\n" +
            "mlZ4+AeRUnbOOeAEuvpLJbel4rhwFzmUiGoeTVoPZyMevWcVFq6BMkS+jRR2w0jK\n" +
            "G6b0v5XDHlcFYPOgUrtsOBFJVwbutLvxdk6q37kIFnWCd8L3kmES5q4wjyFK47Co\n" +
            "Ja8zlx64jmMZPg/t3wWqkZgXZ14qnbyG5/lGsj5CwVtfDljrhN0oCWK1FZaUmW3d\n" +
            "69db12/g4f6phldhxiWuGC/W6fCW5kre7nmhshcltqAJJuU47iX+DarBFiIj816e\n" +
            "yV8e\n" +
            "-----END CERTIFICATE-----\n" +
            "\n" +
            "</ca>\n" +
            "<cert>\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIEjDCCAnSgAwIBAgIQG6MBp/cd9DlY+7cdvp3R3jANBgkqhkiG9w0BAQsFADBmMRAwDgYDVQQK\n" +
            "DAdCaXRtYXNrMRwwGgYDVQQLDBNodHRwczovL2JpdG1hc2submV0MTQwMgYDVQQDDCtCaXRtYXNr\n" +
            "IFJvb3QgQ0EgKGNsaWVudCBjZXJ0aWZpY2F0ZXMgb25seSEpMB4XDTE0MTIwNTAwMDAwMFoXDTE1\n" +
            "MDMwNTAwMDAwMFowLTErMCkGA1UEAwwiVU5MSU1JVEVEZDBwZDdkMzE4eTNtOHNkeXllaTFqYmZl\n" +
            "eDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANRNhZ4aCwdL5+OKObOKeI2rDqEwGnIr\n" +
            "hL9wzo/FXbwLfdW45Y9Mxwhh6xy2NkA1YUKCB8VNBKNXlBrGr1QriLbu1rItsJ2VVLqGluVV/gO4\n" +
            "jcaPU+/Wu0hMFKG28J/dPvIGeNbjBWk6mxQAA5WIpRK9RTeQ88wVaGIZDDzIdivza2zpcyiPAyii\n" +
            "dbkyXh7sLsKvbZB6wLrert6Y1ylR3SlkZP0LfdGAMAdkMyuXKOjgcSnUltR8HSBuZcSUlsTVM11n\n" +
            "rYeGCYyPNNQ3UYatDW33UASgRDBorrmjhhKP7IW/opdlnPk5ZrP3i0qI32/boRe0EWZGXJvr4P3K\n" +
            "dJ30uCECAwEAAaNvMG0wHQYDVR0OBBYEFK8bMVAM4GBB5sHptoIOAaIvlYueMAsGA1UdDwQEAwIH\n" +
            "gDATBgNVHSUEDDAKBggrBgEFBQcDAjAJBgNVHRMEAjAAMB8GA1UdIwQYMBaAFId+E7bsWFsUWah9\n" +
            "vZuPvZ7O+aJsMA0GCSqGSIb3DQEBCwUAA4ICAQAQOX81csVhvP422NKkZH7+g3npBpl+sEHedaGR\n" +
            "xYPOu4HrA4TVF9h44sljRoRJyenGNdBZCXcLKHg889eePTf8Z5K3lTojp6hvwyA6tgxOMHT1kESW\n" +
            "PfqnRw8mHfHJuE3g+4YNUMwggzwc/VZATdV/7M33sarVN9AUOHou9n9BizgCC+UnYlS+F2POumE3\n" +
            "FbOhKo5uubI02MwBYlN2JVO2TBt1Q20w8wc6cU07Xi5Epp+1mkgFiOShkNtPcJmEyBWJhxDtSDOW\n" +
            "2doqWYNqH2kq7B5R/kyyfcpFJqAnBTV7xs+C5rTS1mW7LpxfdCUMbYuLCpyxpO3A/DhAm8n47tUH\n" +
            "lBtmo8Avdb8VdFpYiGBpB0o9kTFcsWFb2GkWFBduGfSEB8jUI7QtqhgZqocAKK/cweSRV8FwyUcn\n" +
            "R0prRm3QEi9fbXqEddzjSY9y/lqWYzT7u+IOAQpKroeZ4wzgYperDNOUFuYk1rP7yuvjP2pV5rcN\n" +
            "yPoBP60TPVWMRM4WJm6nTogAz2qBrFsf/XwT/ajzbsjT6HNB7QbRE+wkFkqspoXG5Agp7KQ8lW3L\n" +
            "SKCDGOQJz7VIE85pD0tg7QEXBEw8oaRZtMjQ0Gvs25mxXAKka4wGasaWfYH6d0E+iKYcWn86V1rH\n" +
            "K2ZoknT+Nno5jgjFuUR3fZseNizEfx7BteooKQ==\n" +
            "-----END CERTIFICATE-----\n" +
            "</cert>\n" +
            "management-external-key nopadding pkcs1 pss digest\n" +
            //"# crl-verify file missing in config profile\n" +
            "route 192.81.208.164  255.255.255.255 net_gateway\n"+
            "nobind\n"+
            "remote-cert-tls server\n" +
            "data-ciphers AES-256-GCM\n" +
            "cipher AES-256-GCM\n" +
            "auth SHA512\n" +
            "float\n"+
            "persist-tun\n" +
            "# persist-tun also enables pre resolving to avoid DNS resolve problem\n" +
            "preresolve\n" +
            "# Use system proxy setting\n" +
            "management-query-proxy\n" +
            "# Custom configuration options\n" +
            "# You are on your on own here :)\n" +
            "# These options found in the config file do not map to config settings:\n" +
            "keepalive 10 30 \n" +
            "tls-cipher TLS-ECDHE-ECDSA-WITH-AES-256-GCM-SHA384 \n" +
            "sndbuf 0 \n" +
            "rcvbuf 0 \n" +
            "tls-version-min 1.2 \n";

    String expectedVPNConfig_hopping_pt_portAndIpHopping = "# Config for OpenVPN 2.x\n" +
            "# Enables connection to GUI\n" +
            "management /data/data/se.leap.bitmask/mgmtsocket unix\n" +
            "management-client\n" +
            "management-query-passwords\n" +
            "management-hold\n" +
            "\n" +
            "setenv IV_GUI_VER \"se.leap.bitmaskclient 0.9.10\" \n" +
            "setenv IV_PLAT_VER \"28 9 ROBO Android unknown robolectric\"\n" +
            "machine-readable-output\n" +
            "allow-recursive-routing\n" +
            "ifconfig-nowarn\n" +
            "client\n" +
            "verb 4\n" +
            "connect-retry 2 300\n" +
            "resolv-retry 60\n" +
            "dev tun\n" +
            "remote 127.0.0.1 8080 udp\n" +
            "<ca>\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIFbzCCA1egAwIBAgIBATANBgkqhkiG9w0BAQ0FADBKMRgwFgYDVQQDDA9CaXRt\n" +
            "YXNrIFJvb3QgQ0ExEDAOBgNVBAoMB0JpdG1hc2sxHDAaBgNVBAsME2h0dHBzOi8v\n" +
            "Yml0bWFzay5uZXQwHhcNMTIxMTA2MDAwMDAwWhcNMjIxMTA2MDAwMDAwWjBKMRgw\n" +
            "FgYDVQQDDA9CaXRtYXNrIFJvb3QgQ0ExEDAOBgNVBAoMB0JpdG1hc2sxHDAaBgNV\n" +
            "BAsME2h0dHBzOi8vYml0bWFzay5uZXQwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAw\n" +
            "ggIKAoICAQC1eV4YvayaU+maJbWrD4OHo3d7S1BtDlcvkIRS1Fw3iYDjsyDkZxai\n" +
            "dHp4EUasfNQ+EVtXUvtk6170EmLco6Elg8SJBQ27trE6nielPRPCfX3fQzETRfvB\n" +
            "7tNvGw4Jn2YKiYoMD79kkjgyZjkJ2r/bEHUSevmR09BRp86syHZerdNGpXYhcQ84\n" +
            "CA1+V+603GFIHnrP+uQDdssW93rgDNYu+exT+Wj6STfnUkugyjmPRPjL7wh0tzy+\n" +
            "znCeLl4xiV3g9sjPnc7r2EQKd5uaTe3j71sDPF92KRk0SSUndREz+B1+Dbe/RGk4\n" +
            "MEqGFuOzrtsgEhPIX0hplhb0Tgz/rtug+yTT7oJjBa3u20AAOQ38/M99EfdeJvc4\n" +
            "lPFF1XBBLh6X9UKF72an2NuANiX6XPySnJgZ7nZ09RiYZqVwu/qt3DfvLfhboq+0\n" +
            "bQvLUPXrVDr70onv5UDjpmEA/cLmaIqqrduuTkFZOym65/PfAPvpGnt7crQj/Ibl\n" +
            "DEDYZQmP7AS+6zBjoOzNjUGE5r40zWAR1RSi7zliXTu+yfsjXUIhUAWmYR6J3KxB\n" +
            "lfsiHBQ+8dn9kC3YrUexWoOqBiqJOAJzZh5Y1tqgzfh+2nmHSB2dsQRs7rDRRlyy\n" +
            "YMbkpzL9ZsOUO2eTP1mmar6YjCN+rggYjRrX71K2SpBG6b1zZxOG+wIDAQABo2Aw\n" +
            "XjAdBgNVHQ4EFgQUuYGDLL2sswnYpHHvProt1JU+D48wDgYDVR0PAQH/BAQDAgIE\n" +
            "MAwGA1UdEwQFMAMBAf8wHwYDVR0jBBgwFoAUuYGDLL2sswnYpHHvProt1JU+D48w\n" +
            "DQYJKoZIhvcNAQENBQADggIBADeG67vaFcbITGpi51264kHPYPEWaXUa5XYbtmBl\n" +
            "cXYyB6hY5hv/YNuVGJ1gWsDmdeXEyj0j2icGQjYdHRfwhrbEri+h1EZOm1cSBDuY\n" +
            "k/P5+ctHyOXx8IE79DBsZ6IL61UKIaKhqZBfLGYcWu17DVV6+LT+AKtHhOrv3TSj\n" +
            "RnAcKnCbKqXLhUPXpK0eTjPYS2zQGQGIhIy9sQXVXJJJsGrPgMxna1Xw2JikBOCG\n" +
            "htD/JKwt6xBmNwktH0GI/LVtVgSp82Clbn9C4eZN9E5YbVYjLkIEDhpByeC71QhX\n" +
            "EIQ0ZR56bFuJA/CwValBqV/G9gscTPQqd+iETp8yrFpAVHOW+YzSFbxjTEkBte1J\n" +
            "aF0vmbqdMAWLk+LEFPQRptZh0B88igtx6tV5oVd+p5IVRM49poLhuPNJGPvMj99l\n" +
            "mlZ4+AeRUnbOOeAEuvpLJbel4rhwFzmUiGoeTVoPZyMevWcVFq6BMkS+jRR2w0jK\n" +
            "G6b0v5XDHlcFYPOgUrtsOBFJVwbutLvxdk6q37kIFnWCd8L3kmES5q4wjyFK47Co\n" +
            "Ja8zlx64jmMZPg/t3wWqkZgXZ14qnbyG5/lGsj5CwVtfDljrhN0oCWK1FZaUmW3d\n" +
            "69db12/g4f6phldhxiWuGC/W6fCW5kre7nmhshcltqAJJuU47iX+DarBFiIj816e\n" +
            "yV8e\n" +
            "-----END CERTIFICATE-----\n" +
            "\n" +
            "</ca>\n" +
            "<cert>\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIEjDCCAnSgAwIBAgIQG6MBp/cd9DlY+7cdvp3R3jANBgkqhkiG9w0BAQsFADBmMRAwDgYDVQQK\n" +
            "DAdCaXRtYXNrMRwwGgYDVQQLDBNodHRwczovL2JpdG1hc2submV0MTQwMgYDVQQDDCtCaXRtYXNr\n" +
            "IFJvb3QgQ0EgKGNsaWVudCBjZXJ0aWZpY2F0ZXMgb25seSEpMB4XDTE0MTIwNTAwMDAwMFoXDTE1\n" +
            "MDMwNTAwMDAwMFowLTErMCkGA1UEAwwiVU5MSU1JVEVEZDBwZDdkMzE4eTNtOHNkeXllaTFqYmZl\n" +
            "eDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANRNhZ4aCwdL5+OKObOKeI2rDqEwGnIr\n" +
            "hL9wzo/FXbwLfdW45Y9Mxwhh6xy2NkA1YUKCB8VNBKNXlBrGr1QriLbu1rItsJ2VVLqGluVV/gO4\n" +
            "jcaPU+/Wu0hMFKG28J/dPvIGeNbjBWk6mxQAA5WIpRK9RTeQ88wVaGIZDDzIdivza2zpcyiPAyii\n" +
            "dbkyXh7sLsKvbZB6wLrert6Y1ylR3SlkZP0LfdGAMAdkMyuXKOjgcSnUltR8HSBuZcSUlsTVM11n\n" +
            "rYeGCYyPNNQ3UYatDW33UASgRDBorrmjhhKP7IW/opdlnPk5ZrP3i0qI32/boRe0EWZGXJvr4P3K\n" +
            "dJ30uCECAwEAAaNvMG0wHQYDVR0OBBYEFK8bMVAM4GBB5sHptoIOAaIvlYueMAsGA1UdDwQEAwIH\n" +
            "gDATBgNVHSUEDDAKBggrBgEFBQcDAjAJBgNVHRMEAjAAMB8GA1UdIwQYMBaAFId+E7bsWFsUWah9\n" +
            "vZuPvZ7O+aJsMA0GCSqGSIb3DQEBCwUAA4ICAQAQOX81csVhvP422NKkZH7+g3npBpl+sEHedaGR\n" +
            "xYPOu4HrA4TVF9h44sljRoRJyenGNdBZCXcLKHg889eePTf8Z5K3lTojp6hvwyA6tgxOMHT1kESW\n" +
            "PfqnRw8mHfHJuE3g+4YNUMwggzwc/VZATdV/7M33sarVN9AUOHou9n9BizgCC+UnYlS+F2POumE3\n" +
            "FbOhKo5uubI02MwBYlN2JVO2TBt1Q20w8wc6cU07Xi5Epp+1mkgFiOShkNtPcJmEyBWJhxDtSDOW\n" +
            "2doqWYNqH2kq7B5R/kyyfcpFJqAnBTV7xs+C5rTS1mW7LpxfdCUMbYuLCpyxpO3A/DhAm8n47tUH\n" +
            "lBtmo8Avdb8VdFpYiGBpB0o9kTFcsWFb2GkWFBduGfSEB8jUI7QtqhgZqocAKK/cweSRV8FwyUcn\n" +
            "R0prRm3QEi9fbXqEddzjSY9y/lqWYzT7u+IOAQpKroeZ4wzgYperDNOUFuYk1rP7yuvjP2pV5rcN\n" +
            "yPoBP60TPVWMRM4WJm6nTogAz2qBrFsf/XwT/ajzbsjT6HNB7QbRE+wkFkqspoXG5Agp7KQ8lW3L\n" +
            "SKCDGOQJz7VIE85pD0tg7QEXBEw8oaRZtMjQ0Gvs25mxXAKka4wGasaWfYH6d0E+iKYcWn86V1rH\n" +
            "K2ZoknT+Nno5jgjFuUR3fZseNizEfx7BteooKQ==\n" +
            "-----END CERTIFICATE-----\n" +
            "</cert>\n" +
            "management-external-key nopadding pkcs1 pss digest\n" +
            //"# crl-verify file missing in config profile\n" +
            "route 192.81.208.164  255.255.255.255 net_gateway\n"+
            "route 192.81.208.165  255.255.255.255 net_gateway\n"+
            "route 192.81.208.166  255.255.255.255 net_gateway\n"+
            "nobind\n"+
            "remote-cert-tls server\n" +
            "data-ciphers AES-256-GCM\n" +
            "cipher AES-256-GCM\n" +
            "auth SHA512\n" +
            "float\n"+
            "persist-tun\n" +
            "# persist-tun also enables pre resolving to avoid DNS resolve problem\n" +
            "preresolve\n" +
            "# Use system proxy setting\n" +
            "management-query-proxy\n" +
            "# Custom configuration options\n" +
            "# You are on your on own here :)\n" +
            "# These options found in the config file do not map to config settings:\n" +
            "keepalive 10 30 \n" +
            "tls-version-min 1.2 \n" +
            "ping-restart 300 \n" +
            "tls-cipher TLS-ECDHE-ECDSA-WITH-AES-256-GCM-SHA384 \n";


    @Before
    public void setUp() throws  Exception {
        generalConfig = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("general_configuration.json")));
        secrets = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("secrets.json")));
        context = MockHelper.mockContext();

        ProviderObservable providerObservable = MockHelper.mockProviderObservable(TestSetupHelper.getConfiguredProvider());
        PrivateKeyHelper privateKeyHelper = MockHelper.mockPrivateKeyHelper();
        sharedPreferences = new MockSharedPreferences();
        preferenceHelper = new PreferenceHelper(new MockSharedPreferences());
        when(context.getCacheDir()).thenReturn(new File("/data/data/se.leap.bitmask"));
    }

    private static boolean containsKey(Vector<VpnProfile> profiles, Connection.TransportType transportType) {
        for (VpnProfile profile : profiles) {
            if (profile.getTransportType() == transportType) {
                return true;
            }
        }
        return false;
    }

    private static VpnProfile getVpnProfile(Vector<VpnProfile> profiles, Connection.TransportType transportType) {
        for (VpnProfile profile : profiles) {
            if (profile.getTransportType() == transportType) {
                return profile;
            }
        }
        return null;
    }

    @Test
    public void testGenerateVpnProfile_v1_tcp_udp() throws Exception {
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("gateway_tcp_udp.json")));
        VpnConfigGenerator.Configuration configuration = createProfileConfig(createTransportsFrom(gateway, 1), 1, gateway.optString(IP_ADDRESS), gateway.optString(IP_ADDRESS6), "");
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        assertFalse(containsKey(vpnProfiles, OBFS4));
        assertEquals(expectedVPNConfig_v1_tcp_udp.trim(), getVpnProfile(vpnProfiles, OPENVPN).getConfigFile(context, false).trim());
    }

    @Test
    public void testGenerateVpnProfile_v1_udp_tcp() throws Exception {
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("gateway_udp_tcp.json")));
        VpnConfigGenerator.Configuration configuration = createProfileConfig(createTransportsFrom(gateway, 1), 1, gateway.optString(IP_ADDRESS), gateway.optString(IP_ADDRESS6), "");
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        assertFalse(containsKey(vpnProfiles, OBFS4));
        assertEquals(expectedVPNConfig_v1_udp_tcp.trim(), getVpnProfile(vpnProfiles, OPENVPN).getConfigFile(context, false).trim());
    }

    @Test
    public void testGenerateVpnProfile_v2_tcp_udp() throws Exception {
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("gateway_tcp_udp.json")));

        VpnConfigGenerator.Configuration configuration = createProfileConfig(createTransportsFrom(gateway, 2), 2, gateway.optString(IP_ADDRESS), gateway.optString(IP_ADDRESS6), "");
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        assertFalse(containsKey(vpnProfiles, OBFS4));
        assertEquals(expectedVPNConfig_v1_tcp_udp.trim(), getVpnProfile(vpnProfiles, OPENVPN).getConfigFile(context, false).trim());
    }

    @Test
    public void testGenerateVpnProfile_v2_udp_tcp() throws Exception {
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("gateway_udp_tcp.json")));
        VpnConfigGenerator.Configuration configuration = createProfileConfig(createTransportsFrom(gateway, 2), 2, gateway.optString(IP_ADDRESS), gateway.optString(IP_ADDRESS6), "");
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        assertFalse(containsKey(vpnProfiles, OBFS4));
        assertEquals(expectedVPNConfig_v1_udp_tcp.trim(), getVpnProfile(vpnProfiles, OPENVPN).getConfigFile(context, false).trim());
    }


    @Test
    public void testGenerateVpnProfile_v3_obfs4() throws Exception {
        BuildConfigHelper buildConfigHelper = MockHelper.mockBuildConfigHelper(false);
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo.bitmask.eip-service-obfsvpn1.0.0.json"))).getJSONArray("gateways").getJSONObject(0);
        VpnConfigGenerator.Configuration configuration = createProfileConfig(createTransportsFrom(gateway, 3), 3, gateway.optString(IP_ADDRESS), gateway.optString(IP_ADDRESS6), "");
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        assertTrue(containsKey(vpnProfiles, OBFS4));
        assertTrue(containsKey(vpnProfiles, OPENVPN));
        System.out.println(getVpnProfile(vpnProfiles, OBFS4).getConfigFile(context, false));
        assertEquals(expectedVPNConfig_v3_obfs4.trim(), getVpnProfile(vpnProfiles, OBFS4).getConfigFile(context, false).trim());
    }

    @Test
    public void testGenerateVpnProfile_v3_obfs4_obfsvpn() throws Exception {
        BuildConfigHelper buildConfigHelper = MockHelper.mockBuildConfigHelper(true);
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo.bitmask.eip-service-obfsvpn1.0.0.json"))).getJSONArray("gateways").getJSONObject(0);
        VpnConfigGenerator.Configuration configuration = createProfileConfig(createTransportsFrom(gateway, 3), 3, gateway.optString(IP_ADDRESS), gateway.optString(IP_ADDRESS6), "");
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        assertTrue(containsKey(vpnProfiles, OBFS4));
        assertTrue(containsKey(vpnProfiles, OPENVPN));
        System.out.println(getVpnProfile(vpnProfiles, OBFS4).getConfigFile(context, false));
        assertEquals(expectedVPNConfig_v3_obfsvpn_obfs4.trim(), getVpnProfile(vpnProfiles, OBFS4).getConfigFile(context, false).trim());
    }

    @Test
    public void testGenerateVpnProfile_v3_ovpn_tcp_udp() throws Exception {
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_pt_tcp_udp.eip-service.json"))).getJSONArray("gateways").getJSONObject(0);
        VpnConfigGenerator.Configuration configuration = createProfileConfig(createTransportsFrom(gateway, 3), 3, gateway.optString(IP_ADDRESS), gateway.optString(IP_ADDRESS6), "");
        configuration.preferUDP = false;
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        assertTrue(containsKey(vpnProfiles, OBFS4));
        assertTrue(containsKey(vpnProfiles, OPENVPN));
        System.out.println(getVpnProfile(vpnProfiles, OPENVPN).getConfigFile(context, false));
        assertEquals(expectedVPNConfig_v3_ovpn_tcp_udp.trim(), getVpnProfile(vpnProfiles, OPENVPN).getConfigFile(context, false).trim());
    }

    @Test
    public void testGenerateVpnProfile_v3_ovpn_udp_tcp() throws Exception {
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_pt_udp_tcp.eip-service.json"))).getJSONArray("gateways").getJSONObject(0);
        VpnConfigGenerator.Configuration configuration = createProfileConfig(createTransportsFrom(gateway, 3), 3, gateway.optString(IP_ADDRESS), gateway.optString(IP_ADDRESS6), "");
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        assertTrue(containsKey(vpnProfiles, OBFS4));
        assertTrue(containsKey(vpnProfiles, OPENVPN));
        System.out.println(getVpnProfile(vpnProfiles, OPENVPN).getConfigFile(context, false));
        assertEquals(expectedVPNConfig_v3_ovpn_udp_tcp.trim(), getVpnProfile(vpnProfiles, OPENVPN).getConfigFile(context, false).trim());
    }

    @Test
    public void testGenerateVpnProfile_v3_ovpn_addDataCiphersDefaults() throws Exception {
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_pt_udp_tcp.eip-service.json"))).getJSONArray("gateways").getJSONObject(0);
        //delete "data-ciphers" from config to test if the resulting openvpn config file will contain the default value taken from "cipher" flag
        generalConfig.put("data-ciphers", null);
        VpnConfigGenerator.Configuration configuration = createProfileConfig(createTransportsFrom(gateway, 3), 3, gateway.optString(IP_ADDRESS), gateway.optString(IP_ADDRESS6), "");
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        assertTrue(containsKey(vpnProfiles, OBFS4));
        assertTrue(containsKey(vpnProfiles, OPENVPN));
        System.out.println(getVpnProfile(vpnProfiles, OPENVPN).getConfigFile(context, false));
        assertEquals(expectedVPNConfig_v3_ovpn_udp_tcp_defaultDataCiphers.trim(), getVpnProfile(vpnProfiles, OPENVPN).getConfigFile(context, false).trim());
    }

    @Test
    public void testGenerateVpnProfile_v4_ovpn_tcp_udp() throws Exception {
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("v4/ptdemo_pt_tcp_udp.eip-service.json"))).getJSONArray("gateways").getJSONObject(0);
        generalConfig = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("v4/ptdemo_pt_tcp_udp.eip-service.json"))).getJSONObject(OPENVPN_CONFIGURATION);
        VpnConfigGenerator.Configuration configuration = createProfileConfig(createTransportsFrom(gateway, 4), 4, gateway.optString(IP_ADDRESS), gateway.optString(IP_ADDRESS6), "");
        configuration.preferUDP = false;
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        assertTrue(containsKey(vpnProfiles, OBFS4));
        assertTrue(containsKey(vpnProfiles, OPENVPN));
        System.out.println(getVpnProfile(vpnProfiles, OPENVPN).getConfigFile(context, false));
        assertEquals(expectedVPNConfig_v4_ovpn_tcp_udp.trim(), getVpnProfile(vpnProfiles, OPENVPN).getConfigFile(context, false).trim());
    }

    @Test
    public void testGenerateVpnProfile_v4_ovpn_udp_tcp() throws Exception {
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("v4/ptdemo_pt_udp_tcp.eip-service.json"))).getJSONArray("gateways").getJSONObject(0);
        generalConfig = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("v4/ptdemo_pt_udp_tcp.eip-service.json"))).getJSONObject(OPENVPN_CONFIGURATION);
        VpnConfigGenerator.Configuration configuration = createProfileConfig(createTransportsFrom(gateway, 4), 4, gateway.optString(IP_ADDRESS), gateway.optString(IP_ADDRESS6), "");
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        assertTrue(containsKey(vpnProfiles, OBFS4));
        assertTrue(containsKey(vpnProfiles, OPENVPN));
        System.out.println(getVpnProfile(vpnProfiles, OPENVPN).getConfigFile(context, false));
        assertEquals(expectedVPNConfig_v4_ovpn_udp_tcp.trim(), getVpnProfile(vpnProfiles, OPENVPN).getConfigFile(context, false).trim());
    }

    @Test
    public void testGenerateVpnProfile_v3_ipv6only_allowOpenvpnIPv6Only() throws Exception {
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_misconfigured_ipv6.json"))).getJSONArray("gateways").getJSONObject(0);
        generalConfig = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_misconfigured_ipv6.json"))).getJSONObject(OPENVPN_CONFIGURATION);
        VpnConfigGenerator.Configuration configuration = createProfileConfig(createTransportsFrom(gateway, 3), 3, gateway.optString(IP_ADDRESS), gateway.optString(IP_ADDRESS6), "");
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        assertTrue(containsKey(vpnProfiles, OPENVPN));
    }

    @Test
    public void testGenerateVpnProfile_v3_obfs4IPv6_skip() throws Exception {
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_misconfigured_ipv6.json"))).getJSONArray("gateways").getJSONObject(0);
        generalConfig = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_misconfigured_ipv6.json"))).getJSONObject(OPENVPN_CONFIGURATION);
        VpnConfigGenerator.Configuration configuration = createProfileConfig(createTransportsFrom(gateway, 3), 3, gateway.optString(IP_ADDRESS), gateway.optString(IP_ADDRESS6), "");
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        assertFalse(containsKey(vpnProfiles, OBFS4));
    }

    /**
     * obfs4 cannot be used with ipv6 addresses currently
     */
    @Test
    public void testGenerateVpnProfile_v3_obfs4IPv4AndIPv6_skipIPv6() throws Exception {
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_misconfigured_ipv4ipv6.json"))).getJSONArray("gateways").getJSONObject(0);
        generalConfig = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_misconfigured_ipv4ipv6.json"))).getJSONObject(OPENVPN_CONFIGURATION);
        VpnConfigGenerator.Configuration configuration = createProfileConfig(createTransportsFrom(gateway, 3), 3, gateway.optString(IP_ADDRESS), gateway.optString(IP_ADDRESS6), "");
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        assertTrue(containsKey(vpnProfiles, OBFS4));
        assertTrue(containsKey(vpnProfiles, OPENVPN));
        assertEquals(1, getVpnProfile(vpnProfiles, OBFS4).mConnections.length);
        assertEquals("37.218.247.60/32", getVpnProfile(vpnProfiles, OBFS4).mExcludedRoutes.trim());
    }

    /**
     * obfs4 cannot be used with UDP
     */
    @Test
    public void testGenerateVpnProfile_v3_obfs4udp_skip() throws Exception {
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_misconfigured_udp.json"))).getJSONArray("gateways").getJSONObject(0);
        generalConfig = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_misconfigured_udp.json"))).getJSONObject(OPENVPN_CONFIGURATION);
        VpnConfigGenerator.Configuration configuration = createProfileConfig(createTransportsFrom(gateway, 3), 3, gateway.optString(IP_ADDRESS), gateway.optString(IP_ADDRESS6), "");
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        assertFalse(containsKey(vpnProfiles, OBFS4));
        assertTrue(containsKey(vpnProfiles, OPENVPN));
    }

    /**
     * obfs4 cannot be used with UDP (only TCP or KCP), openvpn needs to support UDP
     */
    @Test
    public void testGenerateVpnProfile_v3_obfs4TCP_openvpnTCP_skip() throws Exception {
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_misconfigured_tcp2.json"))).getJSONArray("gateways").getJSONObject(0);
        generalConfig = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_misconfigured_tcp2.json"))).getJSONObject(OPENVPN_CONFIGURATION);
        VpnConfigGenerator.Configuration configuration = createProfileConfig(createTransportsFrom(gateway, 3), 3, gateway.optString(IP_ADDRESS), gateway.optString(IP_ADDRESS6), "");
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        assertFalse(containsKey(vpnProfiles, OBFS4));
        assertTrue(containsKey(vpnProfiles, OPENVPN));
    }

    @Test
    public void testGenerateVpnProfile_v3_obfs4UDPAndTCP_skipUDP() throws Exception {
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_misconfigured_udptcp.json"))).getJSONArray("gateways").getJSONObject(0);
        generalConfig = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_misconfigured_udptcp.json"))).getJSONObject(OPENVPN_CONFIGURATION);
        VpnConfigGenerator.Configuration configuration = createProfileConfig(createTransportsFrom(gateway, 3), 3, gateway.optString(IP_ADDRESS), gateway.optString(IP_ADDRESS6), "");
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        assertTrue(containsKey(vpnProfiles, OBFS4));
        assertTrue(containsKey(vpnProfiles, OPENVPN));
        assertEquals(1, getVpnProfile(vpnProfiles, OBFS4).mConnections.length);
    }

    @Test
    public void testGenerateVpnProfile_preferUDP_firstRemotesUDP() throws Exception {
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("v4/multiport_tcpudp_eip-service.json"))).getJSONArray("gateways").getJSONObject(0);
        generalConfig = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("v4/multiport_tcpudp_eip-service.json"))).getJSONObject(OPENVPN_CONFIGURATION);
        VpnConfigGenerator.Configuration configuration = createProfileConfig(createTransportsFrom(gateway, 3), 3, gateway.optString(IP_ADDRESS), gateway.optString(IP_ADDRESS6), "");
        configuration.preferUDP = true;
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        assertTrue(containsKey(vpnProfiles, OBFS4));
        assertTrue(containsKey(vpnProfiles, OPENVPN));
        System.out.println(getVpnProfile(vpnProfiles, OPENVPN).getConfigFile(context, false));
        assertEquals(expectedVPNConfig_v4_ovpn_multiport_tcpudp.trim(), getVpnProfile(vpnProfiles, OPENVPN).getConfigFile(context, false).trim());
    }

    @Test
    public void testGenerateVpnProfile_testNewCiphers() throws Exception {
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("v4/ptdemo_pt_tcp_udp_new_ciphers.eip-service.json"))).getJSONArray("gateways").getJSONObject(0);
        generalConfig = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("v4/ptdemo_pt_tcp_udp_new_ciphers.eip-service.json"))).getJSONObject(OPENVPN_CONFIGURATION);
        VpnConfigGenerator.Configuration configuration = createProfileConfig(createTransportsFrom(gateway, 4), 4, gateway.optString(IP_ADDRESS), gateway.optString(IP_ADDRESS6), "");
        configuration.preferUDP = false;
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        System.out.println(getVpnProfile(vpnProfiles, OPENVPN).getConfigFile(context, false));
        assertEquals(expectedVPNConfig_v4_ovpn_tcp_udp_new_ciphers.trim(), getVpnProfile(vpnProfiles, OPENVPN).getConfigFile(context, false).trim());
    }

    @Test
    public void testGenerateVpnProfileExperimentalTransportsEnabled () throws Exception {
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_kcp_gateways.json"))).getJSONArray("gateways").getJSONObject(2);
        generalConfig = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_kcp_gateways.json"))).getJSONObject(OPENVPN_CONFIGURATION);
        VpnConfigGenerator.Configuration configuration = createProfileConfig(createTransportsFrom(gateway, 3), 3, gateway.optString(IP_ADDRESS), gateway.optString(IP_ADDRESS6), "");
        configuration.experimentalTransports = true;
        configuration.preferUDP = true;
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        assertTrue(containsKey(vpnProfiles, OBFS4) && ((Obfs4Connection)getVpnProfile(vpnProfiles, OBFS4).mConnections[0]).getObfs4Options().transport.getProtocols()[0].equals("kcp"));
        assertTrue(containsKey(vpnProfiles, OPENVPN));
    }

    @Test
    public void testGenerateVpnProfile_experimentalTransportsEnabled_KCPMisconfiguredWithUDP_SkippingObfsKCP () throws Exception {
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_misconfigured_kcp_gateways.json"))).getJSONArray("gateways").getJSONObject(2);
        generalConfig = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_misconfigured_kcp_gateways.json"))).getJSONObject(OPENVPN_CONFIGURATION);
        VpnConfigGenerator.Configuration configuration = createProfileConfig(createTransportsFrom(gateway, 3), 3, gateway.optString(IP_ADDRESS), gateway.optString(IP_ADDRESS6), "");
        configuration.experimentalTransports = true;
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        assertFalse(containsKey(vpnProfiles, OBFS4));
        assertTrue(containsKey(vpnProfiles, OPENVPN));
    }

    @Test
    public void testGenerateVpnProfile_ObfuscationPinningNotEnabled_obfs4AndOpenvpnProfile () throws Exception {
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_kcp_gateways.json"))).getJSONArray("gateways").getJSONObject(1);
        generalConfig = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_kcp_gateways.json"))).getJSONObject(OPENVPN_CONFIGURATION);
        VpnConfigGenerator.Configuration configuration = new VpnConfigGenerator.Configuration();
        configuration.apiVersion = 3;
        configuration.useObfuscationPinning = false;
        configuration.obfuscationProxyPort = "443";
        configuration.obfuscationProxyIP = "5.6.7.8";
        configuration.obfuscationProxyCert = "asdfasdf";
        configuration.obfuscationProxyKCP = true;
        configuration.remoteGatewayIP = "1.2.3.4";
        configuration.transports = createTransportsFrom(gateway, 3);
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        assertTrue("has openvpn profile", containsKey(vpnProfiles, OPENVPN));
        assertTrue("has obfs4 profile", containsKey(vpnProfiles, OBFS4));
        assertTrue("bridge is running KCP", getVpnProfile(vpnProfiles, OBFS4).mGatewayIp.equals("1.2.3.4"));

    }

    @Test
    public void testGenerateVpnProfile_ObfuscationPinningEnabled_obfs4Profile () throws Exception {
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_kcp_gateways.json"))).getJSONArray("gateways").getJSONObject(0);
        generalConfig = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_kcp_gateways.json"))).getJSONObject(OPENVPN_CONFIGURATION);
        VpnConfigGenerator.Configuration configuration = new VpnConfigGenerator.Configuration();
        configuration.apiVersion = 3;
        configuration.useObfuscationPinning = true;
        configuration.obfuscationProxyKCP = false;
        configuration.obfuscationProxyPort = "443";
        configuration.obfuscationProxyIP = "5.6.7.8";
        configuration.obfuscationProxyCert = "asdfasdf";
        configuration.remoteGatewayIP = "1.2.3.4";
        configuration.transports = createTransportsFrom(gateway, 3);
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        assertFalse("has openvpn profile", containsKey(vpnProfiles, OPENVPN));
        assertTrue("has obfs4 profile", containsKey(vpnProfiles, OBFS4));
        assertTrue("bridge is pinned one", getVpnProfile(vpnProfiles, OBFS4).getTransportType() == OBFS4 && getVpnProfile(vpnProfiles, OBFS4).mConnections[0].isUseUdp());
        assertTrue("bridge is running TCP", ((Obfs4Connection) getVpnProfile(vpnProfiles, OBFS4).mConnections[0]).getObfs4Options().transport.getProtocols()[0].equals("tcp"));
    }

    @Test
    public void testGenerateVpnProfile_ObfuscationPinningEnabled_kcp_obfs4KcpProfile () throws Exception {
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_kcp_gateways.json"))).getJSONArray("gateways").getJSONObject(0);
        generalConfig = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_kcp_gateways.json"))).getJSONObject(OPENVPN_CONFIGURATION);
        VpnConfigGenerator.Configuration configuration = new VpnConfigGenerator.Configuration();
        configuration.apiVersion = 3;
        configuration.useObfuscationPinning = true;
        configuration.obfuscationProxyKCP = true;
        configuration.obfuscationProxyPort = "443";
        configuration.obfuscationProxyIP = "5.6.7.8";
        configuration.obfuscationProxyCert = "asdfasdf";
        configuration.remoteGatewayIP = "1.2.3.4";
        configuration.transports = createTransportsFrom(gateway, 3);

        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        assertFalse("has openvpn profile", containsKey(vpnProfiles, OPENVPN));
        assertTrue("has no obfs4 profile", containsKey(vpnProfiles, OBFS4));
        assertTrue("bridge is pinned one", getVpnProfile(vpnProfiles, OBFS4).getTransportType() == OBFS4 && getVpnProfile(vpnProfiles, OBFS4).mGatewayIp.equals("1.2.3.4"));
        assertTrue("bridge is running KCP", ((Obfs4Connection) getVpnProfile(vpnProfiles, OBFS4).mConnections[0]).getObfs4Options().transport.getProtocols()[0].equals("kcp"));
    }
    @Test
    public void testGenerateVpnProfile_obfs4hop_tcp () throws Exception {
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_obfs4hop_tcp_gateways.json"))).getJSONArray("gateways").getJSONObject(2);
        generalConfig = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_obfs4hop_tcp_gateways.json"))).getJSONObject(OPENVPN_CONFIGURATION);
        VpnConfigGenerator.Configuration configuration = createProfileConfig(createTransportsFrom(gateway, 3), 3, gateway.optString(IP_ADDRESS), gateway.optString(IP_ADDRESS6), "");
        configuration.experimentalTransports = true;
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        assertTrue(containsKey(vpnProfiles, OBFS4_HOP) && ((Obfs4Connection)getVpnProfile(vpnProfiles, OBFS4_HOP).mConnections[0]).getObfs4Options().transport.getProtocols()[0].equals("tcp"));
        assertTrue(containsKey(vpnProfiles, OPENVPN));
    }

    @Test
    public void testGenerateVpnProfile_obfs4hop_kcp () throws Exception {
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_obfs4hop_kcp_gateways.json"))).getJSONArray("gateways").getJSONObject(2);
        generalConfig = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("ptdemo_obfs4hop_kcp_gateways.json"))).getJSONObject(OPENVPN_CONFIGURATION);
        VpnConfigGenerator.Configuration configuration = createProfileConfig(createTransportsFrom(gateway, 3), 3, gateway.optString(IP_ADDRESS), gateway.optString(IP_ADDRESS6), "");
        configuration.experimentalTransports = true;
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        assertTrue(containsKey(vpnProfiles, OBFS4_HOP) && ((Obfs4Connection)getVpnProfile(vpnProfiles, OBFS4_HOP).mConnections[0]).getObfs4Options().transport.getProtocols()[0].equals("kcp"));
        assertTrue(containsKey(vpnProfiles, OPENVPN));
    }

    @Test
    public void testGetConfigFile_testHoppingPtPortHopping_decoupled() throws Exception {
        BuildConfigHelper buildConfigHelper = MockHelper.mockBuildConfigHelper(true);
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("decoupled_pt_portHopping.eip-service.json"))).getJSONArray("gateways").getJSONObject(2);
        generalConfig = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("decoupled_pt_portHopping.eip-service.json"))).getJSONObject(OPENVPN_CONFIGURATION);
        VpnConfigGenerator.Configuration configuration = createProfileConfig(createTransportsFrom(gateway, 3), 3, gateway.optString(IP_ADDRESS), gateway.optString(IP_ADDRESS6), "");
        configuration.experimentalTransports = true;
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        System.out.println(getVpnProfile(vpnProfiles, OBFS4_HOP).getConfigFile(context, false));
        assertEquals(expectedVPNConfig_hopping_pt_portHopping.trim(), getVpnProfile(vpnProfiles, OBFS4_HOP).getConfigFile(context, false).trim());
    }

    @Test
    public void testGetConfigFile_testHoppingPtPortAndIPHopping_decoupled() throws Exception {
        BuildConfigHelper buildConfigHelper = MockHelper.mockBuildConfigHelper(true);
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("decoupled_pt_portHopping.eip-service.json"))).getJSONArray("gateways").getJSONObject(2);
        generalConfig = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("decoupled_pt_portHopping.eip-service.json"))).getJSONObject(OPENVPN_CONFIGURATION);
        VpnConfigGenerator.Configuration configuration = createProfileConfig(createTransportsFrom(gateway, 3), 3, gateway.optString(IP_ADDRESS), gateway.optString(IP_ADDRESS6), "");
        configuration.experimentalTransports = true;
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        System.out.println(getVpnProfile(vpnProfiles, OBFS4_HOP).getConfigFile(context, false));
        assertEquals(expectedVPNConfig_hopping_pt_portHopping.trim(), getVpnProfile(vpnProfiles, OBFS4_HOP).getConfigFile(context, false).trim());
    }
    @Test
    public void testGenerateVpnProfile_obfs4_decoupled() throws Exception {
        BuildConfigHelper buildConfigHelper = MockHelper.mockBuildConfigHelper(true);
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("decoupled_pt.eip-service.json"))).getJSONArray("gateways").getJSONObject(1);
        generalConfig = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("decoupled_pt.eip-service.json"))).getJSONObject(OPENVPN_CONFIGURATION);
        VpnConfigGenerator.Configuration configuration = createProfileConfig(createTransportsFrom(gateway, 3), 3, gateway.optString(IP_ADDRESS), gateway.optString(IP_ADDRESS6), "");
        configuration.experimentalTransports = true;
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        assertTrue(containsKey(vpnProfiles, OBFS4));
        assertTrue(((Obfs4Connection)getVpnProfile(vpnProfiles, OBFS4).mConnections[0]).getObfs4Options().transport.getProtocols()[0].equals("tcp"));
        assertFalse(containsKey(vpnProfiles, OPENVPN));
    }

    @Test
    public void testGenerateVpnProfile_obfs4hop_decoupled() throws Exception {
        BuildConfigHelper buildConfigHelper = MockHelper.mockBuildConfigHelper(true);
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("decoupled_pt.eip-service.json"))).getJSONArray("gateways").getJSONObject(2);
        generalConfig = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("decoupled_pt.eip-service.json"))).getJSONObject(OPENVPN_CONFIGURATION);
        VpnConfigGenerator.Configuration configuration = createProfileConfig(createTransportsFrom(gateway, 3), 3, gateway.optString(IP_ADDRESS), gateway.optString(IP_ADDRESS6), "");
        configuration.experimentalTransports = true;
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        assertTrue(containsKey(vpnProfiles, OBFS4_HOP));
        assertTrue(((Obfs4Connection)getVpnProfile(vpnProfiles, OBFS4_HOP).mConnections[0]).getObfs4Options().transport.getProtocols()[0].equals("tcp"));
        assertFalse(containsKey(vpnProfiles, OPENVPN));
        assertFalse(containsKey(vpnProfiles, OBFS4));
    }

    @Test
    public void testGenerateVpnProfile_noExperimental_skipObfs4Hop() throws Exception {
        BuildConfigHelper buildConfigHelper = MockHelper.mockBuildConfigHelper(true);
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("decoupled_pt.eip-service.json"))).getJSONArray("gateways").getJSONObject(2);
        generalConfig = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("decoupled_pt.eip-service.json"))).getJSONObject(OPENVPN_CONFIGURATION);
        VpnConfigGenerator.Configuration configuration = createProfileConfig(createTransportsFrom(gateway, 3), 3, gateway.optString(IP_ADDRESS), gateway.optString(IP_ADDRESS6), "");
        configuration.experimentalTransports = false;
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Exception exception = null;
        try {
            vpnConfigGenerator.generateVpnProfiles();
        } catch (ConfigParser.ConfigParseError e) {
            exception = e;
        }
        assertNotNull(exception);
    }

    @Test
    public void testGenerateVpnProfile_obfs4hop_onlyPortHopping_decoupled() throws Exception {
        BuildConfigHelper buildConfigHelper = MockHelper.mockBuildConfigHelper(true);
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("decoupled_pt_portHopping.eip-service.json"))).getJSONArray("gateways").getJSONObject(2);
        generalConfig = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("decoupled_pt_portHopping.eip-service.json"))).getJSONObject(OPENVPN_CONFIGURATION);
        VpnConfigGenerator.Configuration configuration = createProfileConfig(createTransportsFrom(gateway, 3), 3, gateway.optString(IP_ADDRESS), gateway.optString(IP_ADDRESS6), "");
        configuration.experimentalTransports = true;
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, configuration);
        Vector<VpnProfile> vpnProfiles = vpnConfigGenerator.generateVpnProfiles();
        assertTrue(containsKey(vpnProfiles, OBFS4_HOP));
        assertTrue(((Obfs4Connection)getVpnProfile(vpnProfiles, OBFS4_HOP).mConnections[0]).getObfs4Options().transport.getProtocols()[0].equals("tcp"));
        assertFalse(containsKey(vpnProfiles, OPENVPN));
        assertFalse(containsKey(vpnProfiles, OBFS4));
    }
}