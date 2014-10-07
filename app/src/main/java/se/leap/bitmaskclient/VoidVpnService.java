package se.leap.bitmaskclient;

import android.content.Intent;
import android.net.VpnService;
import android.util.Log;

public class VoidVpnService extends VpnService {

    static final String START_BLOCKING_VPN_PROFILE = "se.leap.bitmaskclient.START_BLOCKING_VPN_PROFILE";
    static final String TAG = VoidVpnService.class.getSimpleName();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
	String action = intent.getAction();
	if (action == START_BLOCKING_VPN_PROFILE) {
	    new Thread(new Runnable() {
		    public void run() {
			blockConnections();
		    }
		}).run();
	}
	return 0;
    }
        
    public void blockConnections() {
	Builder builder = new Builder();
	builder.setSession("Blocking until running");
	builder.addAddress("10.42.0.8",16);
	builder.addRoute("0.0.0.0", 1);
	builder.addRoute("128.0.0.0", 1);
	builder.addRoute("192.168.1.0", 24);
	builder.addDnsServer("10.42.0.1");
	builder.establish();
	android.util.Log.d(TAG, "VoidVpnService set up");
	try {
	    new java.net.Socket("sdf.org", 80);
	    Log.d(TAG, "VoidVpnService doesn's stop traffic");
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
