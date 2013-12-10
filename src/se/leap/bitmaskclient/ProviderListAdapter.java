package se.leap.bitmaskclient;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TwoLineListItem;

public class ProviderListAdapter<T> extends ArrayAdapter<T> {
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
    
    public void unHideAll() {
    	for (int provider_index = 0; provider_index < hidden.length; provider_index++)
    		hidden[provider_index] = false;
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

	public ProviderListAdapter(Context mContext, int layout, List<T> objects) {
		super(mContext, layout, objects);
		if(hidden == null) {
			hidden = new boolean[objects.size()];
			for (int i = 0; i < objects.size(); i++)
				hidden[i] = false;
		}
	}

	public ProviderListAdapter(Context mContext, int layout, List<T> objects, boolean show_all_providers) {
		super(mContext, layout, objects);
		if(show_all_providers) {
			hidden = new boolean[objects.size()];
			for (int i = 0; i < objects.size(); i++)
				hidden[i] = false;
		}
	}
	
	@Override
	public void add(T item) {
		super.add(item);
		boolean[] new_hidden = new boolean[hidden.length+1];
		System.arraycopy(hidden, 0, new_hidden, 0, hidden.length);
		new_hidden[hidden.length] = false;
		hidden = new_hidden;
	}
	
	@Override
	public void remove(T item) {
		super.remove(item);
		boolean[] new_hidden = new boolean[hidden.length-1];
		System.arraycopy(hidden, 0, new_hidden, 0, hidden.length-1);
		hidden = new_hidden;
	}

	@Override
	public View getView(int index, View convertView, ViewGroup parent) {
		TwoLineListItem row;
		int position = getRealPosition(index);
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = (TwoLineListItem)inflater.inflate(android.R.layout.simple_list_item_2, null);                    
		} else {
			row = (TwoLineListItem)convertView;
		}
		ProviderListContent.ProviderItem data = ProviderListContent.ITEMS.get(position);
		row.getText1().setText(data.domain());
		row.getText2().setText(data.name());

		return row;
	}
}
