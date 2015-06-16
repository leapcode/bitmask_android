package se.leap.bitmaskclient;

import java.net.*;

public class DefaultedURL {
    private URL DEFAULT_URL;
    private String default_url = "https://example.net";

    private URL url;

    public DefaultedURL() {
        try {
            DEFAULT_URL = new URL(default_url);
            url = DEFAULT_URL;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public boolean isDefault() { return url.equals(DEFAULT_URL); }

    public void setUrl(URL url) {
        this.url = url;
    }

    public String getDomain() {
        return url.getHost();
    }

    public URL getUrl() {
        return url;
    }

    public String toString() {
        return url.toString();
    }
}
