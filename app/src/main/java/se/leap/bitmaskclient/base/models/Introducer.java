package se.leap.bitmaskclient.base.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

public class Introducer implements Parcelable {
    private String type;
    private String address;
    private String certificate;
    private String fullyQualifiedDomainName;
    private boolean kcpEnabled;

    private String auth;

    public Introducer(String type, String address, String certificate, String fullyQualifiedDomainName, boolean kcpEnabled, String auth) {
        this.type = type;
        this.address = address;
        this.certificate = certificate;
        this.fullyQualifiedDomainName = fullyQualifiedDomainName;
        this.kcpEnabled = kcpEnabled;
        this.auth = auth;
    }

    protected Introducer(Parcel in) {
        type = in.readString();
        address = in.readString();
        certificate = in.readString();
        fullyQualifiedDomainName = in.readString();
        kcpEnabled = in.readByte() != 0;
        auth = in.readString();
    }

    public String getFullyQualifiedDomainName() {
        return fullyQualifiedDomainName;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(type);
        dest.writeString(address);
        dest.writeString(certificate);
        dest.writeString(fullyQualifiedDomainName);
        dest.writeByte((byte) (kcpEnabled ? 1 : 0));
        dest.writeString(auth);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Introducer> CREATOR = new Creator<>() {
        @Override
        public Introducer createFromParcel(Parcel in) {
            return new Introducer(in);
        }

        @Override
        public Introducer[] newArray(int size) {
            return new Introducer[size];
        }
    };

    public boolean validate() {
        if (!"obfsvpnintro".equals(type)) {
            throw new IllegalArgumentException("Unknown type: " + type);
        }
        if (!address.contains(":") || address.split(":").length != 2) {
            throw new IllegalArgumentException("Expected address in format ipaddr:port");
        }
        if (certificate.length() != 70) {
            throw new IllegalArgumentException("Wrong certificate length: " + certificate.length());
        }
        if (!"localhost".equals(fullyQualifiedDomainName) && fullyQualifiedDomainName.split("\\.").length < 2) {
            throw new IllegalArgumentException("Expected a FQDN, got: " + fullyQualifiedDomainName);
        }

        if (auth == null || auth.isEmpty()) {
            throw new IllegalArgumentException("Auth token is missing");
        }
        return true;
    }

    public static Introducer fromUrl(String introducerUrl) throws URISyntaxException, IllegalArgumentException {
        URI uri = new URI(introducerUrl);
        String fqdn = getQueryParam(uri, "fqdn");
        if (fqdn == null || fqdn.isEmpty()) {
            throw new IllegalArgumentException("FQDN not found in the introducer URL");
        }

        boolean kcp = "1".equals(getQueryParam(uri, "kcp"));

        String cert = getQueryParam(uri, "cert");
        if (cert == null || cert.isEmpty()) {
            throw new IllegalArgumentException("Cert not found in the introducer URL");
        }

        String auth = getQueryParam(uri, "auth");
        if (auth == null || auth.isEmpty()) {
            throw new IllegalArgumentException("Authentication token not found in the introducer URL");
        }
        return new Introducer(uri.getScheme(), uri.getAuthority(), cert, fqdn, kcp, auth);
    }

    public String toUrl() throws UnsupportedEncodingException {
        return String.format("%s://%s?fqdn=%s&kcp=%d&cert=%s&auth=%s", type, address, URLEncoder.encode(fullyQualifiedDomainName, "UTF-8"), kcpEnabled ? 1 : 0, URLEncoder.encode(certificate, "UTF-8"),  URLEncoder.encode(auth, "UTF-8"));
    }

    private static String getQueryParam(URI uri, String param) {
        String[] queryParams = uri.getQuery().split("&");
        for (String queryParam : queryParams) {
            String[] keyValue = queryParam.split("=");
            if (keyValue.length == 2 && keyValue[0].equals(param)) {
                return keyValue[1];
            }
        }
        return null;
    }
}