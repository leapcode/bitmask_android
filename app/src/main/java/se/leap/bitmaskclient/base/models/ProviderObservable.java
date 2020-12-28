package se.leap.bitmaskclient.base.models;

import java.util.Observable;

/**
 * Created by cyberta on 05.12.18.
 */
public class ProviderObservable extends Observable {
    private static ProviderObservable instance;
    private Provider currentProvider;
    private Provider providerForDns;

    public static ProviderObservable getInstance() {
        if (instance == null) {
            instance = new ProviderObservable();
        }
        return instance;
    }

    public synchronized void updateProvider(Provider provider) {
        instance.currentProvider = provider;
        instance.providerForDns = null;
        instance.setChanged();
        instance.notifyObservers();
    }

    public Provider getCurrentProvider() {
        return instance.currentProvider;
    }

    public void setProviderForDns(Provider provider) {
        this.providerForDns = provider;
    }

    public Provider getProviderForDns() {
        return instance.providerForDns;
    }

}
