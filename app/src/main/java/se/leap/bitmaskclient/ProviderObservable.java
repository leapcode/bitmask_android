package se.leap.bitmaskclient;

import java.util.Observable;

import se.leap.bitmaskclient.utils.PreferenceHelper;

/**
 * Created by cyberta on 05.12.18.
 */
public class ProviderObservable extends Observable {
    private static ProviderObservable instance;
    private Provider currentProvider;

    public static ProviderObservable getInstance() {
        if (instance == null) {
            instance = new ProviderObservable();
        }
        return instance;
    }

    public synchronized void updateProvider(Provider provider) {
        instance.currentProvider = provider;
        instance.setChanged();
        instance.notifyObservers();
    }

    public Provider getCurrentProvider() {
        return instance.currentProvider;
    }

}
