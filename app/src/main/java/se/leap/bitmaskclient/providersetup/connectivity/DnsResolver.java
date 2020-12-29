package se.leap.bitmaskclient.providersetup.connectivity;

import androidx.annotation.NonNull;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Dns;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.models.ProviderObservable;
import se.leap.bitmaskclient.base.utils.IPAddress;

class DnsResolver implements Dns {

    @Override
    public List<InetAddress> lookup(@NonNull String hostname) throws UnknownHostException {
        try {
            return Dns.SYSTEM.lookup(hostname);
        } catch (UnknownHostException e) {
            ProviderObservable observable = ProviderObservable.getInstance();
            Provider currentProvider;
            if (observable.getProviderForDns() != null) {
                currentProvider = observable.getProviderForDns();
            } else {
                currentProvider = observable.getCurrentProvider();
            }
            String ip = currentProvider.getIpForHostname(hostname);
            if (!ip.isEmpty()) {
                ArrayList<InetAddress> addresses = new ArrayList<>();
                addresses.add(InetAddress.getByAddress(hostname, IPAddress.asBytes(ip)));
                return addresses;
            } else {
                throw new UnknownHostException("Hostname " + hostname + " not found");
            }
        }
    }
}
