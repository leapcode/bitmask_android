package se.leap.bitmaskclient;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.pedrogomez.renderers.Renderer;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnItemClick;
import butterknife.OnItemSelected;

/**
 * Created by parmegv on 4/12/14.
 */
public class ProviderRenderer extends Renderer<Provider> {
    private final Context context;

    @InjectView(R.id.provider_name)
    TextView name;
    @InjectView(R.id.provider_domain)
    TextView domain;

    public ProviderRenderer(Context context) {
        this.context = context;
    }

    @Override
    protected View inflate(LayoutInflater inflater, ViewGroup parent) {
        View view = inflater.inflate(R.layout.provider_list_item, parent, false);
        ButterKnife.inject(this, view);
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
        name.setText(provider.getName());
        domain.setText(provider.getDomain());
    }
}
