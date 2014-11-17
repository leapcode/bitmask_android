package se.leap.bitmaskclient.eip;

import java.security.cert.*;
import java.util.*;
import org.json.*;

public class GatewaySelector {
    List<Gateway> gateways;

    public GatewaySelector(List<Gateway> gateways) {
	this.gateways = gateways;
    }

    public Gateway select() {
	return closestGateway();
    }
    
    private Gateway closestGateway() {
	TreeMap<Integer, Set<Gateway>> offsets = calculateOffsets();
	return offsets.isEmpty() ? null : offsets.firstEntry().getValue().iterator().next();
    }
    
    private TreeMap<Integer, Set<Gateway>> calculateOffsets() {
	TreeMap<Integer, Set<Gateway>> offsets = new TreeMap<Integer, Set<Gateway>>();
	int localOffset = Calendar.getInstance().get(Calendar.ZONE_OFFSET) / 3600000;
	for(Gateway gateway : gateways) {
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
	    dist = 12 - (dist -12); // Well i'll be. Absolute values make equations do funny things.
	return dist;
    }
}
