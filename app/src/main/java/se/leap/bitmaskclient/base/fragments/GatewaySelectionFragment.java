/**
 * Copyright (c) 2021 LEAP Encryption Access Project and contributors
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
package se.leap.bitmaskclient.base.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.MainActivity;
import se.leap.bitmaskclient.base.models.Location;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.base.views.IconSwitchEntry;
import se.leap.bitmaskclient.eip.EipCommand;
import se.leap.bitmaskclient.eip.EipStatus;
import se.leap.bitmaskclient.eip.GatewaysManager;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static se.leap.bitmaskclient.base.MainActivity.ACTION_SHOW_VPN_FRAGMENT;
import static se.leap.bitmaskclient.base.models.Constants.PREFERRED_CITY;
import static se.leap.bitmaskclient.base.models.Constants.SHARED_PREFERENCES;
import static se.leap.bitmaskclient.base.models.Constants.USE_PLUGGABLE_TRANSPORTS;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getPreferredCity;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.setPreferredCity;

public class GatewaySelectionFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener, Observer {

    private static final String TAG = GatewaySelectionFragment.class.getSimpleName();


    private RecyclerView recyclerView;
    private LocationListAdapter locationListAdapter;
    private IconSwitchEntry autoSelectionSwitch;
    private AppCompatButton vpnButton;
    private GatewaysManager gatewaysManager;
    private SharedPreferences preferences;
    private EipStatus eipStatus;

    public GatewaySelectionFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gatewaysManager = new GatewaysManager(getContext());
        preferences = getContext().getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        eipStatus = EipStatus.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.f_gateway_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initRecyclerView();
        initAutoSelectionSwitch();
        initVpnButton();
        eipStatus.addObserver(this);
        preferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        preferences.unregisterOnSharedPreferenceChangeListener(this);
        eipStatus.deleteObserver(this);
    }



    private void initRecyclerView() {
        recyclerView = (RecyclerView) getActivity().findViewById(R.id.gatewaySelection_list);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this.getContext());
        recyclerView.setLayoutManager(layoutManager);
        locationListAdapter = new LocationListAdapter(gatewaysManager.getGatewayLocations());
        recyclerView.setAdapter(locationListAdapter);
        recyclerView.setVisibility(getPreferredCity(getContext()) == null ? INVISIBLE : VISIBLE);
    }

    private void initAutoSelectionSwitch() {
        autoSelectionSwitch = getActivity().findViewById(R.id.automatic_gateway_switch);
        autoSelectionSwitch.setSingleLine(false);
        autoSelectionSwitch.setSubtitle(getString(R.string.gateway_selection_warning, getString(R.string.app_name)));
        autoSelectionSwitch.setChecked(getPreferredCity(getContext()) == null);
        autoSelectionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            recyclerView.setVisibility(!isChecked ? VISIBLE : View.GONE);
            Log.d(TAG, "autoselection enabled: " + isChecked);
            if (isChecked) {
                PreferenceHelper.setPreferredCity(getContext(), null);
                locationListAdapter.resetSelection();
            }
            setVpnButtonState();
        });
    }

    private void initVpnButton() {
        vpnButton = getActivity().findViewById(R.id.vpn_button);
        setVpnButtonState();
        vpnButton.setOnClickListener(v -> {
            EipCommand.startVPN(getContext(), false);
            Intent intent = new Intent(getContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setAction(ACTION_SHOW_VPN_FRAGMENT);
            startActivity(intent);
        });
    }

    private void setVpnButtonState() {
        if (eipStatus.isDisconnected()) {
            vpnButton.setText(R.string.vpn_button_turn_on);
        } else {
            vpnButton.setText(R.string.reconnect);
        }
        vpnButton.setEnabled(
                (locationListAdapter.selectedLocation != null && locationListAdapter.selectedLocation.selected) ||
                        autoSelectionSwitch.isChecked());
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (USE_PLUGGABLE_TRANSPORTS.equals(key)) {
            locationListAdapter.updateData(gatewaysManager.getGatewayLocations());
            setVpnButtonState();
        } else if (PREFERRED_CITY.equals(key)) {
            setVpnButtonState();
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof EipStatus) {
            eipStatus = (EipStatus) o;
            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(this::setVpnButtonState);
            }
        }

    }

    static class LocationListAdapter extends RecyclerView.Adapter<LocationListAdapter.ViewHolder> {
        private static final String TAG = LocationListAdapter.class.getSimpleName();
        private List<Location> values;
        private Location selectedLocation = null;

        static class ViewHolder extends RecyclerView.ViewHolder {
            public AppCompatTextView locationLabel;
            public AppCompatTextView qualityLabel;
            public AppCompatImageView checkedIcon;
            public View layout;

            public ViewHolder(View v) {
                super(v);
                layout = v;
                locationLabel = (AppCompatTextView) v.findViewById(R.id.location);
                qualityLabel = (AppCompatTextView) v.findViewById(R.id.quality);
                checkedIcon = (AppCompatImageView) v.findViewById(R.id.checked_icon);
            }
        }

        public void add(int position, Location item) {
            values.add(position, item);
            notifyItemInserted(position);
        }

        public void remove(int position) {
            values.remove(position);
            notifyItemRemoved(position);
        }

        public void resetSelection() {
            if (selectedLocation != null) {
                selectedLocation.selected = false;
                notifyDataSetChanged();
            }
        }

        public void updateData(List<Location> data) {
            values = data;
            notifyDataSetChanged();
        }

        public LocationListAdapter(List<Location> data) {
            values = data;
        }

        @NonNull
        @Override
        public LocationListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                 int viewType) {
            LayoutInflater inflater = LayoutInflater.from(
                    parent.getContext());
            View v = inflater.inflate(R.layout.v_select_text_list_item, parent, false);
            return new ViewHolder(v);
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            final Location location = values.get(position);
            holder.locationLabel.setText(location.name);
            holder.layout.setOnClickListener(v -> {
                Log.d(TAG, "view at position clicked: " + position);
                if (selectedLocation == null) {
                    selectedLocation = location;
                    selectedLocation.selected = true;
                } else if (selectedLocation.name.equals(location.name)){
                    selectedLocation.selected = !selectedLocation.selected;
                } else {
                    selectedLocation.selected = false;
                    selectedLocation = location;
                    selectedLocation.selected = true;
                }
                setPreferredCity(holder.layout.getContext(), selectedLocation.selected ? selectedLocation.name : null);
                holder.checkedIcon.setVisibility(selectedLocation.selected ? VISIBLE : INVISIBLE);
                notifyDataSetChanged();
            });
            Drawable checkIcon = DrawableCompat.wrap(holder.layout.getContext().getResources().getDrawable(R.drawable.ic_check_bold)).mutate();
            DrawableCompat.setTint(checkIcon, ContextCompat.getColor(holder.layout.getContext(), R.color.colorSuccess));
            holder.checkedIcon.setImageDrawable(checkIcon);
            holder.checkedIcon.setVisibility(location.selected ? VISIBLE : INVISIBLE);
            holder.qualityLabel.setText(getQualityString(location.averageLoad));
            if (location.selected) {
                selectedLocation = location;
            }
        }

        public String getQualityString(double quality) {
            if (quality == 0) {
                return "";
            } else if (quality < 0.25) {
                return "BAD";
            } else if (quality < 0.6) {
                return "AVERAGE";
            } else if (quality < 0.8){
                return "GOOD";
            } else {
                return "EXCELLENT";
            }
        }


        @Override
        public int getItemCount() {
            return values.size();
        }
    }

}