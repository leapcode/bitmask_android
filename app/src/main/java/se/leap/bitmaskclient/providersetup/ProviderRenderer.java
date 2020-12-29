package se.leap.bitmaskclient.providersetup;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pedrogomez.renderers.Renderer;

import butterknife.BindView;
import butterknife.ButterKnife;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.models.Provider;

/**
 * Created by parmegv on 4/12/14.
 */
public class ProviderRenderer extends Renderer<Provider> {
    private final Context context;

    @BindView(R.id.provider_name)
    TextView name;
    @BindView(R.id.provider_domain)
    TextView domain;

    public ProviderRenderer(Context context) {
        this.context = context;
    }

    @Override
    protected View inflate(LayoutInflater inflater, ViewGroup parent) {
        View view = inflater.inflate(R.layout.v_provider_list_item, parent, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    protected void setUpView(View rootView) {
        /*
         * Empty implementation substituted with the usage of ButterKnife library by Jake Wharton.
         */
    }

    @Override
    protected void hookListeners(View rootView) {
        //Empty
    }

    @Override
    public void render() {
        Provider provider = getContent();
        if (!provider.isDefault()) {
            name.setText(provider.getName());
            domain.setText(provider.getDomain());
        } else {
            domain.setText(R.string.add_provider);
        }
    }
}
