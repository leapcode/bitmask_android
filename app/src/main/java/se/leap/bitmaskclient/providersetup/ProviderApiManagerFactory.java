package se.leap.bitmaskclient.providersetup;

import android.content.res.Resources;

import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.providersetup.connectivity.OkHttpClientGenerator;

public class ProviderApiManagerFactory {
    private final Resources resources;
    private final ProviderApiManagerBase.ProviderApiServiceCallback callback;
    private static final String TAG = ProviderApiManagerFactory.class.getSimpleName();

    public ProviderApiManagerFactory(Resources resources, ProviderApiManagerBase.ProviderApiServiceCallback callback) {
        this.resources = resources;
        this.callback = callback;
    }

    public IProviderApiManager getProviderApiManager(Provider provider) throws IllegalArgumentException {
        if (provider.getApiVersion() >= 5) {
            return new ProviderApiManagerV5(resources, callback);
        }
        OkHttpClientGenerator clientGenerator = new OkHttpClientGenerator(resources);
        return new ProviderApiManagerV3(resources, clientGenerator, callback);
    }
}
