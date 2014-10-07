package se.leap.bitmaskclient;

import android.content.Context;
import android.net.VpnService;

public class VoidVpnService extends VpnService {
    public void setUp(Context context) {
	VpnService.prepare(context); // stops the VPN connection created by another application.
	Builder builder = new Builder();
	builder.addAddress("10.42.0.8",16);
	builder.addRoute("0.0.0.0", 1);
	builder.addRoute("128.0.0.0", 1);
	builder.addRoute("192.168.1.0", 24);
	builder.addDnsServer("10.42.0.1");
	builder.establish();
    }
}
