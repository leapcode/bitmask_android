package se.leap.bitmaskclient.base.models;

import androidx.annotation.NonNull;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Created by cyberta on 05.12.18.
 */
public class ProviderObservable {
    private static ProviderObservable instance;
    private final PropertyChangeSupport changeSupport;
    public static final String PROPERTY_CHANGE = "ProviderObservable";

    private Provider currentProvider;
    private Provider providerForDns;

    public static ProviderObservable getInstance() {
        if (instance == null) {
            instance = new ProviderObservable();
        }
        return instance;
    }

    private ProviderObservable() {
        changeSupport = new PropertyChangeSupport(this);
        currentProvider = new Provider();
    }

    public void addObserver(PropertyChangeListener propertyChangeListener) {
        changeSupport.addPropertyChangeListener(propertyChangeListener);
    }

    public void deleteObserver(PropertyChangeListener propertyChangeListener) {
        changeSupport.removePropertyChangeListener(propertyChangeListener);
    }

    public synchronized void updateProvider(@NonNull Provider provider) {
        instance.currentProvider = provider;
        instance.providerForDns = null;
        instance.changeSupport.firePropertyChange(PROPERTY_CHANGE, null, provider);
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
