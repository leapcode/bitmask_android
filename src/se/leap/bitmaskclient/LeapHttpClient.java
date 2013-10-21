/**
 * Copyright (c) 2013 LEAP Encryption Access Project and contributers
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
 package se.leap.bitmaskclient;

import java.security.KeyStore;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import android.content.Context;

/**
 * Implements an HTTP client, enabling LEAP Android app to manage its own runtime keystore or bypass default Android security measures.
 * 
 * @author rafa
 *
 */
public class LeapHttpClient extends DefaultHttpClient {
	final Context context;

	private static LeapHttpClient client;

	/**
	 * If the class scope client is null, it creates one and imports, if existing, the main certificate from Shared Preferences. 
	 * @param context
	 * @return the new client.
	 */
	public static LeapHttpClient getInstance(Context context) {
		if(client == null) {
			client = new LeapHttpClient(context);
			String cert_string = ConfigHelper.getStringFromSharedPref(Provider.CA_CERT);
			if(cert_string != null) {
				ConfigHelper.addTrustedCertificate("provider_ca_certificate", cert_string);
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

	/**
	 * Uses keystore from ConfigHelper for the SSLSocketFactory.
	 * @return
	 */
	private SSLSocketFactory newSslSocketFactory() {
		try {
			KeyStore trusted = ConfigHelper.getKeystore();
			SSLSocketFactory sf = new SSLSocketFactory(trusted);

			return sf;
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	public LeapHttpClient(Context context) {
		this.context = context;
	}
}
