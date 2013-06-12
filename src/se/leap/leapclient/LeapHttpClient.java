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
		registry.register(new Scheme("https", newSslSocketFactory(), 443));

		return new SingleClientConnManager(getParams(), registry);
	}

	private SSLSocketFactory newSslSocketFactory() {
		try {
			KeyStore trusted = ConfigHelper.getKeystore();
			SSLSocketFactory sf = new SSLSocketFactory(trusted);
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
