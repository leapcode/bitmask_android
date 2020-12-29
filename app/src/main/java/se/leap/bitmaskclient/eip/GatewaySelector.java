package se.leap.bitmaskclient.eip;

import android.util.Log;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static se.leap.bitmaskclient.base.utils.ConfigHelper.getCurrentTimezone;

public class GatewaySelector {
    private final static String TAG = GatewaySelector.class.getSimpleName();
    List<Gateway> gateways;
    TreeMap<Integer, Set<Gateway>> offsets;

    public GatewaySelector(List<Gateway> gateways) {
        this.gateways = gateways;
        this.offsets = calculateOffsets();

    }

    public Gateway select() {
        return closestGateway();
    }

    public Gateway select(int nClosest) {
        int i = 0;
        for (Map.Entry<Integer,Set<Gateway>> entrySet : offsets.entrySet()) {
            for (Gateway gateway : entrySet.getValue()) {
                if (i == nClosest) {
                    return gateway;
                }
                i = i + 1;
            }
        }

        Log.e(TAG, "There are less than " + (nClosest + 1) + " Gateways available.");
        return null;
    }

    private Gateway closestGateway() {
        return offsets.isEmpty() ? null : offsets.firstEntry().getValue().iterator().next();
    }

    private TreeMap<Integer, Set<Gateway>> calculateOffsets() {
        TreeMap<Integer, Set<Gateway>> offsets = new TreeMap<Integer, Set<Gateway>>();
        int localOffset = getCurrentTimezone();
        for (Gateway gateway : gateways) {
            int dist = timezoneDistance(localOffset, gateway.getTimezone());
            Set<Gateway> set = (offsets.get(dist) != null) ?
                    offsets.get(dist) : new HashSet<Gateway>();
            set.add(gateway);
            offsets.put(dist, set);
        }
        return offsets;
    }

    private int timezoneDistance(int local_timezone, int remote_timezone) {
        // Distance along the numberline of Prime Meridian centric, assumes UTC-11 through UTC+12
        int dist = Math.abs(local_timezone - remote_timezone);
        // Farther than 12 timezones and it's shorter around the "back"
        if (dist > 12)
            dist = 12 - (dist - 12); // Well i'll be. Absolute values make equations do funny things.
        return dist;
    }
}
