package se.leap.bitmaskclient.eip;

import android.content.Intent;
import android.net.VpnService;

public class VoidVpnService extends VpnService  {

    static final String TAG = VoidVpnService.class.getSimpleName();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
	String action = intent.getAction();
	if (action == Constants.START_BLOCKING_VPN_PROFILE) {
	    new Thread(new Runnable() {
		    public void run() {			
			Builder builder = new Builder();
			builder.setSession("Blocking until running");
			builder.addAddress("10.42.0.8",16);
			builder.addRoute("0.0.0.0", 1);
			builder.addRoute("192.168.1.0", 24);
			builder.addDnsServer("10.42.0.1");
			try {
			    builder.establish();
			} catch (Exception e) {
			    e.printStackTrace();
			}
			android.util.Log.d(TAG, "VoidVpnService set up");
		    }
		}).run();
	}
	return 0;
    }
}
