package se.leap.bitmaskclient.base.models;

import androidx.annotation.NonNull;

public class Location {
    @NonNull public String name;
    public double averageLoad;
    public int numberOfGateways;
    public boolean selected;

    public Location(@NonNull String name, double averageLoad, int numberOfGateways, boolean selected) {
        this.name = name;
        this.averageLoad = averageLoad;
        this.numberOfGateways = numberOfGateways;
        this.selected = selected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location)) return false;

        Location location = (Location) o;

        if (Double.compare(location.averageLoad, averageLoad) != 0) return false;
        if (numberOfGateways != location.numberOfGateways) return false;
        if (selected != location.selected) return false;
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
        result = 31 * result + (selected ? 1 : 0);
        return result;
    }
}
