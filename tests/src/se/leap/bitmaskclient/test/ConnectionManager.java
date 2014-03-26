package se.leap.bitmaskclient.test;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ConnectionManager {
    static void setMobileDataEnabled(boolean enabled, Context context) {
	final ConnectivityManager conman = (ConnectivityManager)  context.getSystemService(Context.CONNECTIVITY_SERVICE);
	Method[] methods = conman.getClass().getMethods();
	for (Method method : methods) {
	    if (method.getName().equals("setMobileDataEnabled")) {
		method.setAccessible(true);
		try {
		    method.invoke(conman, enabled);
		} catch (InvocationTargetException e) {
		    e.printStackTrace();
		} catch (IllegalAccessException e) {
		    e.printStackTrace();
		}
	    }
	}
    }

    static void setWifiEnabled(boolean enabled, Context context) {
	WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE); 
	wifiManager.setWifiEnabled(enabled);
    }
}
