package se.leap.bitmaskclient.base.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import android.net.Uri;
import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;


@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.P})
public class IntroducerTest {

    @Test
    public void testGetQueryParam() {
        try {
            String auth = "solitech_w4gOlm+abcdefaF2DE1Q6dg==";
            String encodedAuth = URLEncoder.encode(auth, "UTF-8");
            Uri uri = Uri.parse("obfsvpn://example.org:443?auth=" + encodedAuth);
            assertEquals(auth, uri.getQueryParameter("auth"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    public void testFromUrl() {
        try {
            Introducer intro = Introducer.fromUrl("obfsvpnintro://37.2.240.90:443?fqdn=ft1.example.org&kcp=1&cert=XXXXXXX&auth=solitech_w4gOlm%2BsbF8spFL8E1Q6dg%3D%3D");
            assertEquals(intro.getFullyQualifiedDomainName(), "ft1.example.org");
            assertEquals("solitech_w4gOlm+sbF8spFL8E1Q6dg==", intro.getAuthToken());
            assertEquals("obfsvpnintro://37.2.240.90:443?fqdn=ft1.example.org&kcp=1&cert=XXXXXXX&auth=solitech_w4gOlm%2BsbF8spFL8E1Q6dg%3D%3D", intro.toUrl());
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testFromUrl_homograph_attack() {
        String code = "obfsvpnintro://37.2.240.90:443?fqdn=ft1.bitmasк.net&kcp=0&cert=XXXXXXX&auth=solitech_w4gOlm%2BseC5spDL8E1Q6dg";
        assertThrows(IllegalArgumentException.class, () -> Introducer.fromUrl(code));
    }

    @Test
    public void testFromUrl_invalid_fqdn() {
        String code = "obfsvpnintro://37.2.240.90:443?fqdn=file://var/wwww&kcp=0&cert=XXXXXXX&auth=solitech_w4gOlm%2BseC5spDL8E1Q6dg";
        assertThrows(IllegalArgumentException.class, () -> Introducer.fromUrl(code));
    }

    @Test
    public void testFromUrl_missing_fqdn() {
        String code = "obfsvpnintro://37.2.240.90:443?fqdn=&kcp=0&cert=XXXXXXX&auth=solitech_w4gOlm%2BseC5spDL8E1Q6dg";
        assertThrows(IllegalArgumentException.class, () -> Introducer.fromUrl(code));

        String code2 = "obfsvpnintro://37.2.240.90:443?kcp=0&cert=XXXXXXX&auth=solitech_w4gOlm%2BseC5spDL8E1Q6dg";
        assertThrows(IllegalArgumentException.class, () -> Introducer.fromUrl(code2));
    }

    @Test
    public void testFromUrl_missing_cert() {
        String code = "obfsvpnintro://37.2.240.90:443?fqdn=ft1.bitmask.net&kcp=0&cert=&auth=solitech_w4gOlm%2BseC5spDL8E1Q6dg";
        assertThrows(IllegalArgumentException.class, () -> Introducer.fromUrl(code));
    }

    @Test
    public void testFromUrl_missing_auth() {
        String code = "obfsvpnintro://37.2.240.90:443?fqdn=ft1.bitmask.net&kcp=0&cert=XXXXXXX&auth=";
        assertThrows(IllegalArgumentException.class, () -> Introducer.fromUrl(code));

        String code2 = "obfsvpnintro://37.2.240.90:443?fqdn=ft1.bitmask.net&kcp=0&cert=XXXXXXX";
        assertThrows(IllegalArgumentException.class, () -> Introducer.fromUrl(code2));
    }
}
