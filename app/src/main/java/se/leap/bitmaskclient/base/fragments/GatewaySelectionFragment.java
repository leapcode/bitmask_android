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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import de.blinkt.openvpn.core.VpnStatus;
import de.blinkt.openvpn.core.connection.Connection;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.MainActivity;
import se.leap.bitmaskclient.base.models.Location;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.base.views.SelectLocationEntry;
import se.leap.bitmaskclient.eip.EIP;
import se.leap.bitmaskclient.eip.EipCommand;
import se.leap.bitmaskclient.eip.EipStatus;
import se.leap.bitmaskclient.eip.GatewaysManager;

import static android.view.View.VISIBLE;
import static de.blinkt.openvpn.core.connection.Connection.TransportType.OBFS4;
import static de.blinkt.openvpn.core.connection.Connection.TransportType.OPENVPN;
import static se.leap.bitmaskclient.base.MainActivity.ACTION_SHOW_DIALOG_FRAGMENT;
import static se.leap.bitmaskclient.base.MainActivity.ACTION_SHOW_VPN_FRAGMENT;
import static se.leap.bitmaskclient.base.models.Constants.LOCATION;

interface LocationListSelectionListener {
    void onLocationManuallySelected(Location location);
}

public class GatewaySelectionFragment extends Fragment implements Observer, LocationListSelectionListener {

    private static final String TAG = GatewaySelectionFragment.class.getSimpleName();


    private RecyclerView recyclerView;
    private LocationListAdapter locationListAdapter;
    private SelectLocationEntry recommendedLocation;
    private GatewaysManager gatewaysManager;
    private EipStatus eipStatus;

    public GatewaySelectionFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gatewaysManager = new GatewaysManager(getContext());
        eipStatus = EipStatus.getInstance();
        eipStatus.addObserver(this);
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
        initRecommendedLocationEntry();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        eipStatus.deleteObserver(this);
    }

    private void initRecyclerView() {
        recyclerView = getActivity().findViewById(R.id.gatewaySelection_list);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this.getContext());
        recyclerView.setLayoutManager(layoutManager);
        locationListAdapter = new LocationListAdapter(gatewaysManager.getGatewayLocations(), this);
        recyclerView.setAdapter(locationListAdapter);
        recyclerView.setVisibility(VISIBLE);
    }

    private void initRecommendedLocationEntry() {
        recommendedLocation = getActivity().findViewById(R.id.recommended_location);
        recommendedLocation.setTitle(getString(R.string.gateway_selection_automatic_location));
        recommendedLocation.showDivider(true);
        recommendedLocation.setOnClickListener(v -> {
            recommendedLocation.setSelected(true);
            locationListAdapter.unselectAll();
            startEipService(null);
        });
        updateRecommendedLocation();
    }

    private void updateRecommendedLocation() {
        Location location = new Location();
        boolean isManualSelection = PreferenceHelper.getPreferredCity(getContext()) != null;
        if (!isManualSelection && eipStatus.isConnected()) {
            try {
                location = gatewaysManager.getLocation(VpnStatus.getCurrentlyConnectingVpnName()).clone();
            } catch (NullPointerException | CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }
        location.selected = !isManualSelection;
        recommendedLocation.setLocation(location);
    }

    protected void startEipService(String preferredCity) {
        new Thread(() -> {
            Context context = getContext();
            if (context == null) {
                return;
            }
            PreferenceHelper.setPreferredCity(context, preferredCity);
            EipCommand.startVPN(context, false);
            Intent intent = new Intent(getContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setAction(ACTION_SHOW_VPN_FRAGMENT);
            startActivity(intent);
        }).start();
    }

    @Override
    public void onLocationManuallySelected(Location location) {
        recommendedLocation.setSelected(false);
        String name = location.name;
        Connection.TransportType selectedTransport = PreferenceHelper.getUseBridges(getContext()) ? OBFS4 : OPENVPN;
        if (location.supportedTransports.contains(selectedTransport)) {
            startEipService(name);
        } else {
            askToChangeTransport(name);
        }
    }

    private void askToChangeTransport(String name) {
        Intent intent = new Intent(getContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(ACTION_SHOW_DIALOG_FRAGMENT);
        intent.putExtra(EIP.ERRORID, EIP.EIPErrors.TRANSPORT_NOT_SUPPORTED.toString());
        intent.putExtra(EIP.ERRORS, getString(R.string.warning_bridges_not_supported, name));
        intent.putExtra(LOCATION, name);
        startActivity(intent);
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof EipStatus) {
            eipStatus = (EipStatus) o;
            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(this::updateRecommendedLocation);
            }
        }
    }

    static class LocationListAdapter extends RecyclerView.Adapter<LocationListAdapter.ViewHolder> {
        private static final String TAG = LocationListAdapter.class.getSimpleName();
        private final List<Location> values;
        private final WeakReference<LocationListSelectionListener> callback;

        static class ViewHolder extends RecyclerView.ViewHolder {
            public SelectLocationEntry entry;

            public ViewHolder(SelectLocationEntry v) {
                super(v);
                entry = v;
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

        public void unselectAll() {
            for (Location l : values) {
                l.selected = false;
            }
            notifyDataSetChanged();
        }

        public LocationListAdapter(List<Location> data, LocationListSelectionListener selectionListener) {
            values = data;
            callback = new WeakReference<>(selectionListener);
        }

        @NonNull
        @Override
        public LocationListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                 int viewType) {
            SelectLocationEntry entry = new SelectLocationEntry(parent.getContext());
            return new ViewHolder(entry);
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            final Location location = values.get(position);
            holder.entry.setLocation(location);
            holder.entry.setOnClickListener(v -> {
                Log.d(TAG, "onClick view at position clicked: " + position);
                LocationListSelectionListener listener = callback.get();
                if (listener != null) {
                    unselectAll();
                    location.selected = true;
                    listener.onLocationManuallySelected(location);
                    notifyDataSetChanged();
                }
            });
        }

        @Override
        public int getItemCount() {
            return values.size();
        }
    }

}