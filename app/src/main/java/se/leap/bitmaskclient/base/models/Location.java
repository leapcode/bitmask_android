/**
 * Copyright (c) 2021 LEAP Encryption Access Project and contributers
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

package se.leap.bitmaskclient.base.models;

import androidx.annotation.NonNull;

import java.util.Comparator;
import java.util.HashMap;

import de.blinkt.openvpn.core.connection.Connection.TransportType;

public class Location implements Cloneable {
    @NonNull private String name = "";
    @NonNull private HashMap<TransportType, Double> averageLoad = new HashMap<>();
    @NonNull private HashMap<TransportType, Integer> numberOfGateways = new HashMap<>();
    public boolean selected;

    public Location() {}

    public Location(@NonNull String name,
                    @NonNull HashMap<TransportType, Double> averageLoad,
                    @NonNull HashMap<TransportType, Integer> numberOfGateways,
                    boolean selected) {
        this.name = name;
        this.averageLoad = averageLoad;
        this.numberOfGateways = numberOfGateways;
        this.selected = selected;
    }

    public boolean hasLocationInfo() {
        return !numberOfGateways.isEmpty() && !averageLoad.isEmpty() && !name.isEmpty();
    }

    public boolean supportsTransport(TransportType transportType) {
        return numberOfGateways.containsKey(transportType.getMetaType());
    }

    public void setAverageLoad(TransportType transportType, double load) {
        averageLoad.put(transportType.getMetaType(), load);
    }

    public double getAverageLoad(TransportType transportType) {
        if (averageLoad.containsKey(transportType.getMetaType())) {
            return averageLoad.get(transportType.getMetaType());
        }
        return 0;
    }

    public void setNumberOfGateways(TransportType transportType, int numbers) {
        numberOfGateways.put(transportType.getMetaType(), numbers);
    }

    public int getNumberOfGateways(TransportType transportType) {
        if (numberOfGateways.containsKey(transportType.getMetaType())) {
            return numberOfGateways.get(transportType.getMetaType());
        }
        return 0;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location)) return false;

        Location location = (Location) o;

        if (!name.equals(location.name)) return false;
        if (!averageLoad.equals(location.averageLoad)) return false;
        return numberOfGateways.equals(location.numberOfGateways);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + averageLoad.hashCode();
        result = 31 * result + numberOfGateways.hashCode();
        return result;
    }

    @Override
    public Location clone() throws CloneNotSupportedException {
        Location copy = (Location) super.clone();
        copy.name = this.name;
        copy.numberOfGateways = (HashMap<TransportType, Integer>) this.numberOfGateways.clone();
        copy.averageLoad = (HashMap<TransportType, Double>) this.averageLoad.clone();
        return copy;
    }

    public static class SortByAverageLoad implements Comparator<Location> {
        TransportType transportType;
        public SortByAverageLoad(TransportType transportType) {
            this.transportType = transportType.getMetaType();
        }

        @Override
        public int compare(Location location1, Location location2) {
            if (location1.supportsTransport(transportType) && location2.supportsTransport(transportType)) {
                return (int) (location1.getAverageLoad(transportType) * 100) - (int) (location2.getAverageLoad(transportType) * 100);
            } else if (location1.supportsTransport(transportType) && !location2.supportsTransport(transportType)) {
                return -1;
            } else {
                return 1;
            }
        }
    }
}
