package se.leap.bitmaskclient.base.models;

import static se.leap.bitmaskclient.base.models.Introducer.IntroducerException.ADDRESS_FORMAT;
import static se.leap.bitmaskclient.base.models.Introducer.IntroducerException.AUTH_MISSING;
import static se.leap.bitmaskclient.base.models.Introducer.IntroducerException.CERTIFICATE_LENGTH;
import static se.leap.bitmaskclient.base.models.Introducer.IntroducerException.CERTIFICATE_MISSING;
import static se.leap.bitmaskclient.base.models.Introducer.IntroducerException.FQDN_INVALID;
import static se.leap.bitmaskclient.base.models.Introducer.IntroducerException.FQDN_LENGTH;
import static se.leap.bitmaskclient.base.models.Introducer.IntroducerException.FQDN_MISSING;
import static se.leap.bitmaskclient.base.models.Introducer.IntroducerException.UNKNOWN_TYPE;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import java.io.UnsupportedEncodingException;
import java.net.IDN;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.IllegalFormatException;
import java.util.Locale;

import se.leap.bitmaskclient.R;

public class Introducer implements Parcelable {
    private final String type;
    private final String address;
    private final String certificate;
    private final String fullyQualifiedDomainName;
    private final boolean kcpEnabled;
    private final String auth;

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
            throw new IntroducerException(UNKNOWN_TYPE, "Unknown type: ", type);
        }
        if (!address.contains(":") || address.split(":").length != 2) {
            throw new IntroducerException(ADDRESS_FORMAT, "Expected address in format ipaddr:port");
        }
        if (certificate.length() != 70) {
            throw new IntroducerException(CERTIFICATE_LENGTH, "Wrong certificate length: ", String.valueOf(certificate.length()));
        }
        if (!"localhost".equals(fullyQualifiedDomainName) && fullyQualifiedDomainName.split("\\.").length < 2) {
            throw new IntroducerException(FQDN_LENGTH, "Expected a FQDN, got: ", fullyQualifiedDomainName);
        }

        if (auth == null || auth.isEmpty()) {
            throw new IntroducerException(AUTH_MISSING, "Auth token is missing");
        }
        return true;
    }

    /**
     * Helper method to create an Introducer model object, containing the information how to query a menshen API via a proxy
     * @param introducerUrl String representing an 'invite code'
     * @return Introducer model
     * @throws NullPointerException in case the parameter introducerUrl is null
     * @throws IntroducerException in case the parameter introducerUrl is invalid
     */
    public static Introducer fromUrl(String introducerUrl) throws NullPointerException, IntroducerException {
        Uri uri = Uri.parse(introducerUrl);
        String fqdn = uri.getQueryParameter("fqdn");
        if (fqdn == null || fqdn.isEmpty()) {
            throw new IntroducerException(FQDN_MISSING, "FQDN not found in the introducer URL");
        }

        if (!isAscii(fqdn)) {
            throw new IntroducerException(FQDN_INVALID, "FQDN is not ASCII: " + fqdn, fqdn);
        }

        boolean kcp = "1".equals(uri.getQueryParameter( "kcp"));

        String cert = uri.getQueryParameter( "cert");
        if (cert == null || cert.isEmpty()) {
            throw new IntroducerException(CERTIFICATE_MISSING, "Cert not found in the introducer URL");
        }

        String auth = uri.getQueryParameter( "auth");
        if (auth == null || auth.isEmpty()) {
            throw new IntroducerException(AUTH_MISSING, "Authentication token not found in the introducer URL");
        }
        return new Introducer(uri.getScheme(), uri.getAuthority(), cert, fqdn, kcp, auth);
    }

    public String getAuthToken() {
        return auth;
    }

    private static boolean isAscii(String fqdn) {
        try {
            String asciiFQDN = IDN.toASCII(fqdn, IDN.USE_STD3_ASCII_RULES);
            return fqdn.equals(asciiFQDN);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public String toUrl() throws UnsupportedEncodingException {
        return String.format(Locale.US, "%s://%s?fqdn=%s&kcp=%d&cert=%s&auth=%s", type, address, URLEncoder.encode(fullyQualifiedDomainName, "UTF-8"), kcpEnabled ? 1 : 0, URLEncoder.encode(certificate, "UTF-8"),  URLEncoder.encode(auth, "UTF-8"));
    }

    public static class IntroducerException extends IllegalArgumentException {
        public static final int UNKNOWN_TYPE = 100;
        public static final int ADDRESS_FORMAT = 200;
        public static final int CERTIFICATE_LENGTH = 300;
        public static final int CERTIFICATE_MISSING = 310;
        public static final int FQDN_LENGTH = 400;
        public static final int FQDN_MISSING = 410;
        public static final int FQDN_INVALID = 420;
        public static final int AUTH_MISSING = 500;

        private final Object[] args;
        private final int type;

        public IntroducerException(int type, String defaultMsg, Object... args) {
            super(args == null ? defaultMsg : String.format(defaultMsg, args));
            this.type = type;
            this.args = args;
        }


        @Nullable
        public String getLocalizedMessage(Context context) {
            if (context == null) {
                return getMessage();
            }
            try {
                return switch (type) {
                    case UNKNOWN_TYPE ->  context.getString(R.string.error_invite_unknown_type, args);
                    case ADDRESS_FORMAT -> context.getString(R.string.error_invite_address_format);
                    case CERTIFICATE_LENGTH -> context.getString(R.string.error_invite_certificate_length, args);
                    case CERTIFICATE_MISSING -> context.getString(R.string.error_invite_certificate_missing);
                    case FQDN_LENGTH -> context.getString(R.string.error_invite_fqdn_length, args);
                    case FQDN_MISSING -> context.getString(R.string.error_invite_fqdn_missing);
                    case FQDN_INVALID -> context.getString(R.string.error_invite_fqdn_invalid, args);
                    case AUTH_MISSING -> context.getString(R.string.error_invite_auth_missing);
                    default -> getMessage();
                };
            } catch (IllegalFormatException e) {
                e.printStackTrace();
                return getMessage();
            }

        }
    }
}