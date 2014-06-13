package se.leap.bitmaskclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class EipStatusReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
	if (intent.getAction().equals("de.blinkt.openvpn.VPN_STATUS")) {
	    context.getSharedPreferences(Dashboard.SHARED_PREFERENCES, Context.MODE_PRIVATE).edit().putString(EIP.STATUS, intent.getStringExtra("status")).commit();
	}
    }
}
