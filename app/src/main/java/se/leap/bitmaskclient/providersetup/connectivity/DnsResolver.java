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

class DnsResolver implements Dns {
    OkHttpClient dohHttpClient;
    boolean forceDoH;

    public DnsResolver(OkHttpClient dohHttpClient, boolean forceDoH) {
        this.dohHttpClient = dohHttpClient;
        this.forceDoH = forceDoH;
    }

    @NonNull
    @Override
    public List<InetAddress> lookup(@NonNull String hostname) throws UnknownHostException {
        Log.d("DNS", "trying to resolve DNS for " + hostname);

        try {
            if (forceDoH) {
                List<InetAddress> list = lookupDoH(hostname);
                for (InetAddress address : list) {
                    Log.d("DNS", "DoH ---> " + address.toString());
                }
                return list;
            }
            return Dns.SYSTEM.lookup(hostname);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            Log.e("DNS", "DoH via cloudflare failed");

            // if not yet tried, do DNS over Https after normal DNS failed
            if (!forceDoH) {
                try {
                   return lookupDoH(hostname);
                } catch (RuntimeException uhe) {
                   uhe.printStackTrace();
                }
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
    }

    private List<InetAddress> lookupDoH(@NonNull String hostname) throws RuntimeException, UnknownHostException {
        DnsOverHttps ahablitzDoHClient = new DnsOverHttps.Builder().client(dohHttpClient)
                .url(HttpUrl.get("https://blitz.ahadns.com"))
                .build();
        try {
            Log.d("DNS", "DoH via blitz.ahadns.com");
            return ahablitzDoHClient.lookup(hostname);
        } catch (UnknownHostException e) {
             e.printStackTrace();
            Log.e("DNS", "DoH via blitz.ahadns.com failed");
        }

        DnsOverHttps googleDoHClient = new DnsOverHttps.Builder().client(dohHttpClient)
                .url(HttpUrl.get("https://dns.google/dns-query"))
                .bootstrapDnsHosts(getByName("8.8.4.4"), getByName("8.8.8.8"))
                .build();
        try {
            Log.d("DNS", "DoH via dns.google");
            return googleDoHClient.lookup(hostname);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            Log.e("DNS", "DoH via dns.google failed");

        }

        DnsOverHttps cloudFlareDoHClient = new DnsOverHttps.Builder().client(dohHttpClient)
                .url(HttpUrl.get("https://1.1.1.1/dns-query"))
                .bootstrapDnsHosts(getByName("1.1.1.1"), getByName("1.0.0.1"))
                .build();

        Log.d("DNS", "DoH via cloudflare 1.1.1.1");
        return cloudFlareDoHClient.lookup(hostname);
    }
}
