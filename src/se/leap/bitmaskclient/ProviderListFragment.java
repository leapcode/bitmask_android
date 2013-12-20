/**
 * Copyright (c) 2013 LEAP Encryption Access Project and contributers
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
 package se.leap.bitmaskclient;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.ProviderListContent.ProviderItem;
import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

/**
 * A list fragment representing a list of Providers. This fragment
 * also supports tablet devices by allowing list items to be given an
 * 'activated' state upon selection. This helps indicate which item is
 * currently being viewed in a {@link DashboardFragment}.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class ProviderListFragment extends ListFragment {

	public static String TAG = "provider_list_fragment";
	public static String SHOW_ALL_PROVIDERS = "show_all_providers";
	
	private ProviderListAdapter<ProviderItem> content_adapter;
	
    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = sDummyCallbacks;

    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void onItemSelected(String id);
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(String id) {
        }
    };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ProviderListFragment() {
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	if(getArguments().containsKey(SHOW_ALL_PROVIDERS))
    		content_adapter = new ProviderListAdapter<ProviderListContent.ProviderItem>(
    				getActivity(),
    				android.R.layout.simple_list_item_activated_2,
    				ProviderListContent.ITEMS, getArguments().getBoolean(SHOW_ALL_PROVIDERS));
    	else
    		content_adapter = new ProviderListAdapter<ProviderListContent.ProviderItem>(
    				getActivity(),
    				android.R.layout.simple_list_item_activated_2,
    				ProviderListContent.ITEMS);
    		
			
        setListAdapter(content_adapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
    	return inflater.inflate(R.layout.provider_list_fragment, container, false);
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }
    	String top_padding_key = getResources().getString(R.string.top_padding);
    	if(getArguments() != null && getArguments().containsKey(top_padding_key)) {
    		int topPadding = getArguments().getInt(top_padding_key);
    		View current_view = getView();
    		getView().setPadding(current_view.getPaddingLeft(), topPadding, current_view.getPaddingRight(), current_view.getPaddingBottom());
    	}
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        mCallbacks.onItemSelected(ProviderListContent.ITEMS.get(position).name());

        for(int item_position = 0; item_position < listView.getCount(); item_position++) {
        	if(item_position != position)
        		content_adapter.hide(item_position);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    public void notifyAdapter() {
    	content_adapter.notifyDataSetChanged();
    }
    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        getListView().setChoiceMode(activateOnItemClick
                ? ListView.CHOICE_MODE_SINGLE
                : ListView.CHOICE_MODE_NONE);
    }

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(mActivatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }
    
    public void removeLastItem() {
    	unhideAll();
    	content_adapter.remove(content_adapter.getItem(content_adapter.getCount()-1));
    	content_adapter.notifyDataSetChanged();
    }
    
    public void addItem(ProviderItem provider) {
    	content_adapter.add(provider);
    	content_adapter.notifyDataSetChanged();
    }
    
    public void hideAllBut(int position) {
    	int real_count = content_adapter.getCount();
    	for(int i = 0; i < real_count;)
    		if(i != position) {
    			content_adapter.hide(i);
    			position--;
    			real_count--;
    		} else {
    			i++;
    		}
    }
    
    public void unhideAll() {
    	if(content_adapter != null) {
    		content_adapter.unHideAll();
    		content_adapter.notifyDataSetChanged();
    	}
    }

	/**
	 * @return a new instance of this ListFragment.
	 */
	public static ProviderListFragment newInstance() {
		return new ProviderListFragment();
	}
}
