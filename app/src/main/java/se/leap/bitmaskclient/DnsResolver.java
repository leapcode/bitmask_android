package se.leap.bitmaskclient;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Dns;

class DnsResolver implements Dns {

    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        try {
            return Dns.SYSTEM.lookup(hostname);
        } catch (UnknownHostException e) {
            ProviderObservable observable = ProviderObservable.getInstance();
            Provider currentProvider;
            if (observable.getProviderToSetup() != null) {
                currentProvider = observable.getProviderToSetup();
            } else {
                currentProvider = observable.getCurrentProvider();
            }
            if (currentProvider.hasProviderIp()) {
                ArrayList<InetAddress> addresses = new ArrayList<>();
                addresses.add(InetAddress.getByAddress(hostname, currentProvider.getProviderIpAsBytes()));
                return addresses;
            } else {
                throw new UnknownHostException("Hostname " + hostname + " not found");
            }
        }
    }
}
