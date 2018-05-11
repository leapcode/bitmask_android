/**
 * Copyright (c) 2018 LEAP Encryption Access Project and contributers
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

import android.support.annotation.NonNull;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by cyberta on 21.02.18.
 */

public class DrawerSettingsAdapter extends BaseAdapter {

    //item types
    public static final int NONE = -1;
    public static final int SWITCH_PROVIDER = 0;
    public static final int LOG = 1;
    public static final int ABOUT = 2;
    public static final int BATTERY_SAVER = 3;
    public static final int ALWAYS_ON = 4;

    //view types
    public final static int VIEW_SIMPLE_TEXT = 0;
    public final static int VIEW_SWITCH = 1;

    public static class DrawerSettingsItem {
        private String description = "";
        private int viewType = VIEW_SIMPLE_TEXT;
        private boolean isChecked = false;
        private int itemType = NONE;
        private CompoundButton.OnCheckedChangeListener callback;

        private DrawerSettingsItem(String description, int viewType, boolean isChecked, int itemType, CompoundButton.OnCheckedChangeListener callback) {
            this.description = description;
            this.viewType = viewType;
            this.isChecked = isChecked;
            this.itemType = itemType;
            this.callback = callback;
        }

        public static DrawerSettingsItem getSimpleTextInstance(String description, int itemType) {
            return new DrawerSettingsItem(description, VIEW_SIMPLE_TEXT, false, itemType, null);
        }

        public static DrawerSettingsItem getSwitchInstance(String description, boolean isChecked, int itemType, CompoundButton.OnCheckedChangeListener callback) {
            return new DrawerSettingsItem(description, VIEW_SWITCH, isChecked, itemType, callback);
        }

        public int getItemType() {
            return itemType;
        }

        public void setChecked(boolean checked) {
            isChecked = checked;
        }

        public boolean isChecked() {
            return isChecked;
        }
    }

    private ArrayList<DrawerSettingsItem> mData = new ArrayList<>();
    private LayoutInflater mInflater;

    public DrawerSettingsAdapter(LayoutInflater layoutInflater) {
        mInflater = layoutInflater;
    }

    public void addItem(final DrawerSettingsItem item) {
        mData.add(item);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        DrawerSettingsItem item = mData.get(position);
        return item.viewType;
    }

    @Override
    public int getViewTypeCount() {
        boolean hasSwitchItem = false;
        for (DrawerSettingsItem item : mData) {
            if (item.viewType == VIEW_SWITCH) {
                hasSwitchItem = true;
                break;
            }
        }
        return hasSwitchItem ? 2 : 1;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public DrawerSettingsItem getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        DrawerSettingsItem drawerSettingsItem = mData.get(position);
        ViewHolder holder = null;
        int type = getItemViewType(position);
        if (convertView == null) {
            holder = new ViewHolder();
            switch(type) {
                case VIEW_SIMPLE_TEXT:
                    convertView = initTextViewBinding(holder);
                    holder.textView.setText(drawerSettingsItem.description);
                    break;
                case VIEW_SWITCH:
                    convertView = initSwitchBinding(holder);
                    bindSwitch(drawerSettingsItem, holder);
                    break;
            }
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
            switch (type) {
                case VIEW_SIMPLE_TEXT:
                    if (holder.isSwitchViewHolder()) {
                        holder.resetSwitchView();
                        convertView = initTextViewBinding(holder);
                    }
                    holder.textView.setText(drawerSettingsItem.description);
                    break;
                case VIEW_SWITCH:
                    if (!holder.isSwitchViewHolder()) {
                        holder.resetTextView();
                        convertView = initSwitchBinding(holder);
                    }
                    bindSwitch(drawerSettingsItem, holder);
                    break;
            }
            convertView.setTag(holder);
        }
        return convertView;
    }

    private void bindSwitch(DrawerSettingsItem drawerSettingsItem, ViewHolder holder) {
        holder.switchView.setChecked(drawerSettingsItem.isChecked);
        holder.switchView.setText(drawerSettingsItem.description);
        holder.switchView.setOnCheckedChangeListener(drawerSettingsItem.callback);
    }

    @NonNull
    private View initSwitchBinding(ViewHolder holder) {
        View convertView = mInflater.inflate(R.layout.v_switch_list_item, null);
        holder.switchView = convertView.findViewById(android.R.id.text1);
        return convertView;
    }

    @NonNull
    private View initTextViewBinding(ViewHolder holder) {
        View convertView = mInflater.inflate(R.layout.v_single_list_item, null);
        holder.textView = convertView.findViewById(android.R.id.text1);
        return convertView;
    }

    public DrawerSettingsItem getDrawerItem(int elementType) {
        for (DrawerSettingsItem item : mData) {
            if (item.itemType == elementType) {
                return item;
            }
        }
        return null;
    }

    static class ViewHolder {
        TextView textView;
        SwitchCompat switchView;

        boolean isSwitchViewHolder() {
            return switchView != null;
        }

        void resetSwitchView() {
            switchView.setOnCheckedChangeListener(null);
            switchView = null;
        }

        void resetTextView() {
            textView = null;
        }
    }
}



