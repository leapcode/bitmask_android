package se.leap.bitmaskclient;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

/**
 * Created by cyberta on 24.10.17.
 * This class ensures that modern TLS algorithms will also be used on old devices (Android 4.1 - Android 4.4.4) in order to avoid
 * attacks like POODLE.
 */

public class TLSCompatSocketFactory extends SSLSocketFactory {

    private static final String TAG = TLSCompatSocketFactory.class.getName();
    private SSLSocketFactory internalSSLSocketFactory;
    private SSLContext sslContext;
    private TrustManager trustManager;

    public TLSCompatSocketFactory(String trustedCaCert) throws KeyManagementException, NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException, NoSuchProviderException {

        initTrustManager(trustedCaCert);
        internalSSLSocketFactory = sslContext.getSocketFactory();

    }

    public void initSSLSocketFactory(OkHttpClient.Builder builder) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, KeyManagementException, IllegalStateException {
        builder.sslSocketFactory(this, (X509TrustManager) trustManager);
    }


    private void initTrustManager(String trustedCaCert) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, IllegalStateException, KeyManagementException, NoSuchProviderException {
        java.security.cert.Certificate provider_certificate = ConfigHelper.parseX509CertificateFromString(trustedCaCert);

        // Create a KeyStore containing our trusted CAs
        String defaultType = KeyStore.getDefaultType();
        KeyStore keyStore = KeyStore.getInstance(defaultType);
        keyStore.load(null, null);
        keyStore.setCertificateEntry("provider_ca_certificate", provider_certificate);

        // Create a TrustManager that trusts the CAs in our KeyStore
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        tmf.init(keyStore);

        // Check if there's only 1 X509Trustmanager -> from okttp3 source code example
        TrustManager[] trustManagers = tmf.getTrustManagers();
        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
            throw new IllegalStateException("Unexpected default trust managers:"
                    + Arrays.toString(trustManagers));
        }

        trustManager = trustManagers[0];

        // Create an SSLContext that uses our TrustManager
        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);

    }


    @Override
    public String[] getDefaultCipherSuites() {
        return internalSSLSocketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return internalSSLSocketFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket() throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket());
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(address, port, localAddress, localPort));
    }

    private Socket enableTLSOnSocket(Socket socket) {
        if(socket != null && (socket instanceof SSLSocket)) {
            ((SSLSocket)socket).setEnabledProtocols(new String[] {"TLSv1.2"});
            ((SSLSocket)socket).setEnabledCipherSuites(getSupportedCipherSuites());
        }
        return socket;


    }



}
