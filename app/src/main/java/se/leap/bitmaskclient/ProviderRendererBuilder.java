package se.leap.bitmaskclient;

import com.pedrogomez.renderers.Renderer;
import com.pedrogomez.renderers.RendererBuilder;

import java.util.Collection;

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
