package se.leap.bitmaskclient.providersetup;

import com.pedrogomez.renderers.*;

import java.util.*;

import se.leap.bitmaskclient.base.models.Provider;

/**
 * Created by parmegv on 4/12/14.
 */
public class ProviderRendererBuilder extends RendererBuilder<Provider> {
    public ProviderRendererBuilder(Collection<Renderer<Provider>> prototypes) {
        super(prototypes);
    }

    @Override
    protected Class getPrototypeClass(Provider content) {
        return ProviderRenderer.class;
    }
}
