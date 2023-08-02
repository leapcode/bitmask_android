package se.leap.bitmaskclient.providersetup.connectivity;

import static java.net.InetAddress.getByName;

import android.util.Log;

import androidx.annotation.NonNull;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import de.blinkt.openvpn.core.VpnStatus;
import okhttp3.Dns;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.dnsoverhttps.DnsOverHttps;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.models.ProviderObservable;
import se.leap.bitmaskclient.base.utils.IPAddress;
import se.leap.bitmaskclient.tor.TorStatusObservable;

public class DnsResolver implements Dns {
    OkHttpClient dohHttpClient;
    boolean preferDoH;

    public DnsResolver(OkHttpClient dohHttpClient, boolean preferDoH) {
        this.dohHttpClient = dohHttpClient;
        this.preferDoH = preferDoH;
    }

    @NonNull
    @Override
    public List<InetAddress> lookup(@NonNull String hostname) throws UnknownHostException {
        Log.d("DNS", "trying to resolve DNS for " + hostname);
        List<InetAddress> list = null;
        if (preferDoH && !"127.0.0.1".equals(hostname)) {
            if ((list = tryLookupDoH(hostname)) == null) {
                list  = tryLookupSystemDNS(hostname);
            }
        } else {
            if ((list = tryLookupSystemDNS(hostname)) == null) {
                list = tryLookupDoH(hostname);
            }
        }

        if (list != null) {
            return list;
        }

        Log.d("DNS", "try hard coded IPs");
        // let's check if there's an hard-coded IP we can use
        ProviderObservable observable = ProviderObservable.getInstance();
        Provider currentProvider;
        if (observable.getProviderForDns() != null) {
            currentProvider = observable.getProviderForDns();
        } else {
            currentProvider = observable.getCurrentProvider();
        }
        String ip = currentProvider.getIpForHostname(hostname);
        if (!ip.isEmpty()) {
            VpnStatus.logWarning("[API] Normal DNS resolution for " + hostname + " seems to be blocked. Circumventing.");
            ArrayList<InetAddress> addresses = new ArrayList<>();
            addresses.add(InetAddress.getByAddress(hostname, IPAddress.asBytes(ip)));
            return addresses;
        } else {
            VpnStatus.logWarning("[API] Could not resolve DNS for " + hostname);
            throw new UnknownHostException("Hostname " + hostname + " not found");
        }
    }

    private List<InetAddress> tryLookupSystemDNS(@NonNull String hostname) throws RuntimeException, UnknownHostException {
        try {
            Log.d("DNS", "trying to resolve " + hostname + " with system DNS");
            return Dns.SYSTEM.lookup(hostname);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<InetAddress> tryLookupDoH(@NonNull String hostname) throws UnknownHostException {
        DnsOverHttps njallaDoH = new DnsOverHttps.Builder().client(dohHttpClient)
                .url(HttpUrl.get("https://dns.njal.la/dns-query"))
                .bootstrapDnsHosts(getByName("95.215.19.53"), getByName("2001:67c:2354:2::53"))
                .build();
        try {
            Log.d("DNS", "DoH via dns.njal.la");
            return njallaDoH.lookup(hostname);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            Log.e("DNS", "DoH via dns.njal.la failed");
        }

        DnsOverHttps quad9 = new DnsOverHttps.Builder().client(dohHttpClient)
                .url(HttpUrl.get("https://dns.quad9.net/dns-query"))
                .bootstrapDnsHosts(getByName("9.9.9.9"), getByName("149.112.112.112"), getByName("2620:fe::fe"), getByName("2620:fe::9"))
                .build();
        try {
            Log.d("DNS", "DoH via dns.quad9.net");
            return quad9.lookup(hostname);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            Log.e("DNS", "DoH via dns.quad9.net failed");
        }

        DnsOverHttps cloudFlareDoHClient = new DnsOverHttps.Builder().client(dohHttpClient)
                .url(HttpUrl.get("https://1.1.1.1/dns-query"))
                .bootstrapDnsHosts(getByName("1.1.1.1"), getByName("1.0.0.1"))
                .build();

        try {
            Log.d("DNS", "DoH via cloudflare 1.1.1.1");
            return cloudFlareDoHClient.lookup(hostname);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            Log.e("DNS", "DoH via cloudflare failed");
        }
        return null;
    }
}
