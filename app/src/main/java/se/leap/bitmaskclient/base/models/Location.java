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
import java.util.HashSet;
import de.blinkt.openvpn.core.connection.Connection;

public class Location {
    @NonNull public String name;
    @NonNull public HashSet<Connection.TransportType> supportedTransports;
    public double averageLoad;
    public int numberOfGateways;

    public Location(@NonNull String name, double averageLoad, int numberOfGateways, @NonNull HashSet<Connection.TransportType> supportedTransports) {
        this.name = name;
        this.averageLoad = averageLoad;
        this.numberOfGateways = numberOfGateways;
        this.supportedTransports = supportedTransports;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location)) return false;

        Location location = (Location) o;

        if (Double.compare(location.averageLoad, averageLoad) != 0) return false;
        if (numberOfGateways != location.numberOfGateways) return false;
        if (!name.equals(location.name)) return false;
        return supportedTransports.equals(location.supportedTransports);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = name.hashCode();
        result = 31 * result + supportedTransports.hashCode();
        temp = Double.doubleToLongBits(averageLoad);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + numberOfGateways;
        return result;
    }
}
