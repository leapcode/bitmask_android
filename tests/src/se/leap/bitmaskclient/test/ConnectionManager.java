package se.leap.bitmaskclient.test;

import android.content.Context;
import android.net.ConnectivityManager;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import android.util.Log;

public class ConnectionManager {
    static void setMobileDataEnabled(boolean enabled, Context context) {
	final ConnectivityManager conman = (ConnectivityManager)  context.getSystemService(Context.CONNECTIVITY_SERVICE);
	try {
	    final Class conmanClass = Class.forName(conman.getClass().getName());
	    final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
	    iConnectivityManagerField.setAccessible(true);
	    final Object iConnectivityManager = iConnectivityManagerField.get(conman);
	    final Class iConnectivityManagerClass =  Class.forName(iConnectivityManager.getClass().getName());
	    final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
	    setMobileDataEnabledMethod.setAccessible(true);

	    setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
	    Log.d("testDashboard", "setMobileDataEnabled(" + enabled + ");");
	} catch (ClassNotFoundException e) {
	    e.printStackTrace();
	} catch (NoSuchMethodException e) {
	    e.printStackTrace();
	} catch (IllegalAccessException e) {
	    e.printStackTrace();
	} catch (NoSuchFieldException e) {
	    e.printStackTrace();
	} catch (InvocationTargetException e) {
	    e.printStackTrace();
	}
    }
}
