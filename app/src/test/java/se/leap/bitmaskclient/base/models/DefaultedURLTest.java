package se.leap.bitmaskclient.base.models;

import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

import se.leap.bitmaskclient.base.models.DefaultedURL;

import static org.junit.Assert.*;

/**
 * Created by cyberta on 11.02.18.
 */
public class DefaultedURLTest {

    @Test
    public void testEquals_false() throws MalformedURLException {
        DefaultedURL defaultedURL = new DefaultedURL();
        DefaultedURL customURL = new DefaultedURL();
        customURL.setUrl(new URL("https://customurl.com"));

        assertFalse(defaultedURL.equals(customURL));
    }

    @Test
    public void testEquals_true() throws MalformedURLException {
        DefaultedURL defaultedURL = new DefaultedURL();
        DefaultedURL customURL = new DefaultedURL();
        assertTrue(defaultedURL.equals(customURL));
    }

}
