package se.leap.bitmaskclient.base.models;

import java.net.MalformedURLException;
import java.net.URL;

public class DefaultedURL {
    private URL DEFAULT_URL;
    private String default_url = "https://example.net";

    private URL url;

    DefaultedURL() {
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

    @Override
    public String toString() {
        return url.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DefaultedURL) {
            return url.equals(((DefaultedURL) o).getUrl());
        }
        return false;
    }

}
