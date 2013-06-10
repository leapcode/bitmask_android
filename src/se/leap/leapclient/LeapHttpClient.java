package se.leap.leapclient;

import java.security.KeyStore;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import android.content.Context;

public class LeapHttpClient extends DefaultHttpClient {
	final Context context;

	private static LeapHttpClient client;

	public static LeapHttpClient getInstance(Context context) {
		if(client == null) {
			client = new LeapHttpClient(context);
			String cert_string = ConfigHelper.getStringFromSharedPref(ConfigHelper.MAIN_CERT_KEY);
			if(cert_string != null) {
				ConfigHelper.addTrustedCertificate("recovered_certificate", cert_string);
			}
		}
		return client;
	}

	@Override
	protected ClientConnectionManager createClientConnectionManager() {
		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		// Register for port 443 our SSLSocketFactory with our keystore
		// to the ConnectionManager
		registry.register(new Scheme("https", newSslSocketFactory(), 443));
		return new SingleClientConnManager(getParams(), registry);
	}

	private SSLSocketFactory newSslSocketFactory() {
		try {
			// Get an instance of the Bouncy Castle KeyStore format
			KeyStore trusted = ConfigHelper.getKeystore();

			// Pass the keystore to the SSLSocketFactory. The factory is responsible
			// for the verification of the server certificate.
			SSLSocketFactory sf = new SSLSocketFactory(trusted);

			// Hostname verification from certificate
			// http://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html#d4e506
			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

			return sf;
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	public LeapHttpClient(Context context) {
		this.context = context;
	}
}
