package se.leap.bitmaskclient.base.models;

import androidx.annotation.NonNull;

public class Location {
    @NonNull public String name;
    public double averageLoad;
    public int numberOfGateways;

    public Location(@NonNull String name, double averageLoad, int numberOfGateways) {
        this.name = name;
        this.averageLoad = averageLoad;
        this.numberOfGateways = numberOfGateways;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location)) return false;

        Location location = (Location) o;

        if (Double.compare(location.averageLoad, averageLoad) != 0) return false;
        if (numberOfGateways != location.numberOfGateways) return false;
        return name.equals(location.name);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = name.hashCode();
        temp = Double.doubleToLongBits(averageLoad);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + numberOfGateways;
        return result;
    }
}
