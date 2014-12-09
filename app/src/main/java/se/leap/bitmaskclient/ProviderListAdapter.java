package se.leap.bitmaskclient;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TwoLineListItem;

import com.pedrogomez.renderers.AdapteeCollection;
import com.pedrogomez.renderers.RendererAdapter;
import com.pedrogomez.renderers.RendererBuilder;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ProviderListAdapter extends RendererAdapter<Provider> {
	private static boolean[] hidden = null;
	
	public void hide(int position) {
		hidden[getRealPosition(position)] = true;
		notifyDataSetChanged();
		notifyDataSetInvalidated();
	}
	
	public void unHide(int position) {
		hidden[getRealPosition(position)] = false;
		notifyDataSetChanged();
		notifyDataSetInvalidated();
	}

    public void showAllProviders() {
        for(int i = 0; i < hidden.length; i++)
            hidden[i] = false;
        notifyDataSetChanged();
        notifyDataSetInvalidated();
    }
    
    public void hideAllBut(int position) {
    	for (int i = 0; i < hidden.length; i++) {
            if (i != position)
                hidden[i] = true;
            else
                hidden[i] = false;
        }
        notifyDataSetChanged();
        notifyDataSetInvalidated();
    }
	
	private int getRealPosition(int position) {
		int hElements = getHiddenCountUpTo(position);
		int diff = 0;
		for(int i=0;i<hElements;i++) {
			diff++;
			if(hidden[position+diff])
				i--;
		}
		return (position + diff);
	}
	private int getHiddenCount() {
		int count = 0;
		for(int i=0;i<hidden.length;i++)
			if(hidden[i])
				count++;
		return count;
	}
	private int getHiddenCountUpTo(int location) {
		int count = 0;
		for(int i=0;i<=location;i++) {
			if(hidden[i])
				count++;
		}
		return count;
	}

	@Override
	public int getCount() {
		return (hidden.length - getHiddenCount());
	}

	public ProviderListAdapter(LayoutInflater layoutInflater, RendererBuilder rendererBuilder,
                               AdapteeCollection<Provider> collection) {
		super(layoutInflater, rendererBuilder, collection);
		if(hidden == null) {
			hidden = new boolean[collection.size()];
			for (int i = 0; i < collection.size(); i++)
				hidden[i] = false;
		}
	}

	public ProviderListAdapter(LayoutInflater layoutInflater, RendererBuilder rendererBuilder,
                               AdapteeCollection<Provider> collection, boolean show_all_providers) {
        super(layoutInflater, rendererBuilder, collection);
		if(show_all_providers) {
			hidden = new boolean[collection.size()];
			for (int i = 0; i < collection.size(); i++)
				hidden[i] = false;
		}
	}
	
	@Override
	public void add(Provider item) {
		super.add(item);
		boolean[] new_hidden = new boolean[hidden.length+1];
		System.arraycopy(hidden, 0, new_hidden, 0, hidden.length);
		new_hidden[hidden.length] = false;
		hidden = new_hidden;
	}
	
	@Override
	public void remove(Provider item) {
		super.remove(item);
		boolean[] new_hidden = new boolean[hidden.length-1];
		System.arraycopy(hidden, 0, new_hidden, 0, hidden.length-1);
		hidden = new_hidden;
	}

    protected int indexOf(Provider item) {
        int index = 0;
        ProviderManager provider_manager = (ProviderManager) getCollection();
        Set<Provider> providers = provider_manager.providers();
        for (Provider provider : providers) {
            if (provider.equals(item)) {
                break;
            } else index++;
        }
        return index;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return super.getView(getRealPosition(position), convertView, parent);
    }
}
