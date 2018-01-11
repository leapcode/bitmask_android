package se.leap.bitmaskclient.eip;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import se.leap.bitmaskclient.testutils.TestSetupHelper;

import static junit.framework.Assert.assertTrue;

/**
 * Created by cyberta on 03.10.17.
 */
public class VpnConfigGeneratorTest {

    private VpnConfigGenerator vpnConfigGenerator;
    private JSONObject generalConfig;
    private JSONObject gateway;
    private JSONObject secrets;

    String expectedVPNConfig_tcp_udp = "cipher AES-128-CBC \n" +
            "auth SHA1 \n" +
            "tun-ipv6 true \n" +
            "keepalive 10 30 \n" +
            "tls-cipher DHE-RSA-AES128-SHA \n" +
            "client\n" +
            "remote 198.252.153.84 443 tcp\n" +
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
            "<key>\n" +
            "-----BEGIN RSA PRIVATE KEY-----\n" +
            "MIIEwAIBADANBgkqhkiG9w0BAQEFAASCBKowggSmAgEAAoIBAQDUTYWeGgsHS+fjijmziniNqw6h\n" +
            "MBpyK4S/cM6PxV28C33VuOWPTMcIYesctjZANWFCggfFTQSjV5Qaxq9UK4i27tayLbCdlVS6hpbl\n" +
            "Vf4DuI3Gj1Pv1rtITBShtvCf3T7yBnjW4wVpOpsUAAOViKUSvUU3kPPMFWhiGQw8yHYr82ts6XMo\n" +
            "jwMoonW5Ml4e7C7Cr22QesC63q7emNcpUd0pZGT9C33RgDAHZDMrlyjo4HEp1JbUfB0gbmXElJbE\n" +
            "1TNdZ62HhgmMjzTUN1GGrQ1t91AEoEQwaK65o4YSj+yFv6KXZZz5OWaz94tKiN9v26EXtBFmRlyb\n" +
            "6+D9ynSd9LghAgMBAAECggEBANPHLRXkhsHVj1EkzqBx7gXr8CEMmiTvknFh9zvltrZhhDoRQjWr\n" +
            "chPDkcRHY2Cznvy4N0YyqQDD2ULIlZdSAgPxxothFoBruWSD47yMBmLx08ORsDpcqt/YvPAATJI8\n" +
            "IpFNsXcyaXBp/M57oRemgnxp/8UJPJmFdWX99H4hvffh/jdj7POgYiWUaAl37XTYZKZ4nzKU2wpL\n" +
            "EDLj9RKPz9gG7CYp2zrLC9LaAsrXVrKwPBw6g+XwbClaqFj97db3mrY4lr6mTo89qmus1AU+fBDH\n" +
            "3Xlpmc8JwB+30TvhRNKrpLx9cEjuEj7K1gm8Y4dWCjPi+lNbtAyUBcgPJFa/81ECgYEA7pLoBU/Y\n" +
            "ZYjyHFca8FvDBcBh6haHfqJr9doXWtgjDrbi3o2n5wHqfKhFWOH6vPEQozkOVeX1ze6HOiRmGBpW\n" +
            "r+r7x8TD25L7I6HJw3M351RWOAfkF0w/RTVdetcTgduQtfN1u6BDhYSVceXMjyQYx7MhfETWI8Gh\n" +
            "KSYm8OEDYiUCgYEA489fmbrCcUnXzpTsbswJ5NmSoEXbcX8cLxnQuzE0z9GHhQdrMjOpXR76reTW\n" +
            "6jcuudarNcwRUYSWWhjCDKHhpx4HhasWPaHgr7jIzcRw8yZSJRSxKr8sl1qh6g7s47JcmfXOMWLt\n" +
            "yuyE933XrT19Th4ODZHY40Uv35mPjMi9d00CgYEAyRNAQtndBRa7GG/B4Ls2T+6pl+aNJIo4e+no\n" +
            "rURlp800wWabEPRocdBRQmyULBLxduBr2LIMzhgwGSz8b2wji/l9ZA3PFY135bxClVzSzUIjuO3N\n" +
            "rGUzHl2wAAyuAFDSUshzfkPBJRNt8aVBF5PQ3t93ZYmPAmv8LPZe875yX5ECgYEAsUEcwK/ZNW7g\n" +
            "dQPZR4iJNkC4Xu6cBZ6Cnn92swBheEYvLSoNlX0vDZ7aLE3/jzQqrjzC8NP8sbH5jtbuvgeDXZX3\n" +
            "AmGRp5j6C6A61ihAPmEVz3ZfN8SSfJ3vl//PAIg6lyz0J+cy4Q7RkwSeuVQ72Hl4M8TEvmmKC3Af\n" +
            "ispy6Y0CgYEAgl1o2lo+ACyk+oVQPaaPqK3d7WOBFp4eR2nXFor/vsx9igQOlZUgzRDQsR8jo1o9\n" +
            "efOSBf87igrZGgssys89pWa2dnXnz5PMmzkKr6bw4D9Ez6u6Puc9UZhGw/8wDYg6fSosdB9utspm\n" +
            "M698ycef7jBNMDgmhpSvfw5GctoNQ4s=\n" +
            "-----END RSA PRIVATE KEY-----\n" +
            "</key>\n" +
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
            "remote-cert-tls server\n" +
            "persist-tun\n" +
            "auth-retry nointeract";

    String expectedVPNConfig_udp_tcp = "cipher AES-128-CBC \n" +
            "auth SHA1 \n" +
            "tun-ipv6 true \n" +
            "keepalive 10 30 \n" +
            "tls-cipher DHE-RSA-AES128-SHA \n" +
            "client\n" +
            "remote 198.252.153.84 443 udp\n" +
            "remote 198.252.153.84 443 tcp\n" +
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
            "<key>\n" +
            "-----BEGIN RSA PRIVATE KEY-----\n" +
            "MIIEwAIBADANBgkqhkiG9w0BAQEFAASCBKowggSmAgEAAoIBAQDUTYWeGgsHS+fjijmziniNqw6h\n" +
            "MBpyK4S/cM6PxV28C33VuOWPTMcIYesctjZANWFCggfFTQSjV5Qaxq9UK4i27tayLbCdlVS6hpbl\n" +
            "Vf4DuI3Gj1Pv1rtITBShtvCf3T7yBnjW4wVpOpsUAAOViKUSvUU3kPPMFWhiGQw8yHYr82ts6XMo\n" +
            "jwMoonW5Ml4e7C7Cr22QesC63q7emNcpUd0pZGT9C33RgDAHZDMrlyjo4HEp1JbUfB0gbmXElJbE\n" +
            "1TNdZ62HhgmMjzTUN1GGrQ1t91AEoEQwaK65o4YSj+yFv6KXZZz5OWaz94tKiN9v26EXtBFmRlyb\n" +
            "6+D9ynSd9LghAgMBAAECggEBANPHLRXkhsHVj1EkzqBx7gXr8CEMmiTvknFh9zvltrZhhDoRQjWr\n" +
            "chPDkcRHY2Cznvy4N0YyqQDD2ULIlZdSAgPxxothFoBruWSD47yMBmLx08ORsDpcqt/YvPAATJI8\n" +
            "IpFNsXcyaXBp/M57oRemgnxp/8UJPJmFdWX99H4hvffh/jdj7POgYiWUaAl37XTYZKZ4nzKU2wpL\n" +
            "EDLj9RKPz9gG7CYp2zrLC9LaAsrXVrKwPBw6g+XwbClaqFj97db3mrY4lr6mTo89qmus1AU+fBDH\n" +
            "3Xlpmc8JwB+30TvhRNKrpLx9cEjuEj7K1gm8Y4dWCjPi+lNbtAyUBcgPJFa/81ECgYEA7pLoBU/Y\n" +
            "ZYjyHFca8FvDBcBh6haHfqJr9doXWtgjDrbi3o2n5wHqfKhFWOH6vPEQozkOVeX1ze6HOiRmGBpW\n" +
            "r+r7x8TD25L7I6HJw3M351RWOAfkF0w/RTVdetcTgduQtfN1u6BDhYSVceXMjyQYx7MhfETWI8Gh\n" +
            "KSYm8OEDYiUCgYEA489fmbrCcUnXzpTsbswJ5NmSoEXbcX8cLxnQuzE0z9GHhQdrMjOpXR76reTW\n" +
            "6jcuudarNcwRUYSWWhjCDKHhpx4HhasWPaHgr7jIzcRw8yZSJRSxKr8sl1qh6g7s47JcmfXOMWLt\n" +
            "yuyE933XrT19Th4ODZHY40Uv35mPjMi9d00CgYEAyRNAQtndBRa7GG/B4Ls2T+6pl+aNJIo4e+no\n" +
            "rURlp800wWabEPRocdBRQmyULBLxduBr2LIMzhgwGSz8b2wji/l9ZA3PFY135bxClVzSzUIjuO3N\n" +
            "rGUzHl2wAAyuAFDSUshzfkPBJRNt8aVBF5PQ3t93ZYmPAmv8LPZe875yX5ECgYEAsUEcwK/ZNW7g\n" +
            "dQPZR4iJNkC4Xu6cBZ6Cnn92swBheEYvLSoNlX0vDZ7aLE3/jzQqrjzC8NP8sbH5jtbuvgeDXZX3\n" +
            "AmGRp5j6C6A61ihAPmEVz3ZfN8SSfJ3vl//PAIg6lyz0J+cy4Q7RkwSeuVQ72Hl4M8TEvmmKC3Af\n" +
            "ispy6Y0CgYEAgl1o2lo+ACyk+oVQPaaPqK3d7WOBFp4eR2nXFor/vsx9igQOlZUgzRDQsR8jo1o9\n" +
            "efOSBf87igrZGgssys89pWa2dnXnz5PMmzkKr6bw4D9Ez6u6Puc9UZhGw/8wDYg6fSosdB9utspm\n" +
            "M698ycef7jBNMDgmhpSvfw5GctoNQ4s=\n" +
            "-----END RSA PRIVATE KEY-----\n" +
            "</key>\n" +
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
            "remote-cert-tls server\n" +
            "persist-tun\n" +
            "auth-retry nointeract";


    @Before
    public void setUp() throws  Exception {
        generalConfig = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("general_configuration.json")));
        secrets = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("secrets.json")));

    }
    @Test
    public void testGenerate_tcp_udp() throws Exception {
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("gateway_tcp_udp.json")));
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, gateway);

        String vpnConfig = vpnConfigGenerator.generate();
        assertTrue(vpnConfig.equals(expectedVPNConfig_tcp_udp));
    }

    @Test
    public void testGenerate_udp_tcp() throws Exception {
        gateway = new JSONObject(TestSetupHelper.getInputAsString(getClass().getClassLoader().getResourceAsStream("gateway_udp_tcp.json")));
        vpnConfigGenerator = new VpnConfigGenerator(generalConfig, secrets, gateway);

        String vpnConfig = vpnConfigGenerator.generate();
        assertTrue(vpnConfig.equals(expectedVPNConfig_udp_tcp));
    }

}