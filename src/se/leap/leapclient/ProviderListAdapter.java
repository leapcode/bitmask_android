package se.leap.leapclient;

import java.util.List;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TwoLineListItem;

public class ProviderListAdapter<T> extends ArrayAdapter<T> {
	T[] items = null;
	boolean[] hidden = null;
	
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
		for(int i=0;i<items.length;i++)
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
		return (items.length - getHiddenCount());
	}

	public ProviderListAdapter(Context mContext, int layout, List<T> objects) {
		super(mContext, layout, objects);
		items = objects.toArray((T[])new Object[0]);
		hidden = new boolean[items.length];
		for (int i = 0; i < items.length; i++)
			hidden[i] = false;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TwoLineListItem row;            
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = (TwoLineListItem)inflater.inflate(android.R.layout.simple_list_item_2, null);                    
		} else {
			row = (TwoLineListItem)convertView;
		}
		ProviderListContent.ProviderItem data = ProviderListContent.ITEMS.get(position);
		row.getText1().setText(data.domain);
		row.getText2().setText(data.name);

		return row;
	}
}
