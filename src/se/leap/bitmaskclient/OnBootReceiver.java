package se.leap.bitmaskclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class OnBootReceiver extends BroadcastReceiver {

	// Debug: am broadcast -a android.intent.action.BOOT_COMPLETED
	@Override
	public void onReceive(Context context, Intent intent) {
	    if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
		if (!context.getSharedPreferences(Dashboard.SHARED_PREFERENCES, Context.MODE_PRIVATE).getString(Provider.KEY, "").isEmpty() && context.getSharedPreferences(Dashboard.SHARED_PREFERENCES, Context.MODE_PRIVATE).getBoolean(Dashboard.START_ON_BOOT, false)) {
		    Intent dashboard_intent = new Intent(context, Dashboard.class);
		    dashboard_intent.setAction(EIP.ACTION_START_EIP);
		    dashboard_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		    context.startActivity(dashboard_intent);
		}
	    }
	}
}
