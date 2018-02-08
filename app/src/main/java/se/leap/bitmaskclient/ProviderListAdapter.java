package se.leap.bitmaskclient;

import android.view.LayoutInflater;

import com.pedrogomez.renderers.AdapteeCollection;
import com.pedrogomez.renderers.RendererAdapter;
import com.pedrogomez.renderers.RendererBuilder;

public class ProviderListAdapter extends RendererAdapter<Provider> {
    public ProviderListAdapter(LayoutInflater layoutInflater, RendererBuilder rendererBuilder,
                               AdapteeCollection<Provider> collection) {
        super(layoutInflater, rendererBuilder, collection);
    }

    public void saveProviders() {
        ProviderManager provider_manager = (ProviderManager) getCollection();
        provider_manager.saveCustomProvidersToFile();
    }
}
