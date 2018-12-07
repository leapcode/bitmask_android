package se.leap.bitmaskclient.eip;

import android.util.Log;

import java.util.*;

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

    public Gateway select(int nClosest) throws IndexOutOfBoundsException{
        int i = 0;
        for (Map.Entry<Integer,Set<Gateway>> entrySet : offsets.entrySet()) {
            Iterator<Gateway> iterator = entrySet.getValue().iterator();
            while (iterator.hasNext()) {
                Gateway gateway = iterator.next();
                if (i == nClosest)  {
                    return gateway;
                }
                i = i+1;
            }
        }

        Log.e(TAG, "There are less than " + nClosest + " Gateways available.");
        return null;
    }

    private Gateway closestGateway() {
        return offsets.isEmpty() ? null : offsets.firstEntry().getValue().iterator().next();
    }

    private TreeMap<Integer, Set<Gateway>> calculateOffsets() {
        TreeMap<Integer, Set<Gateway>> offsets = new TreeMap<Integer, Set<Gateway>>();
        int localOffset = Calendar.getInstance().get(Calendar.ZONE_OFFSET) / 3600000;
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
