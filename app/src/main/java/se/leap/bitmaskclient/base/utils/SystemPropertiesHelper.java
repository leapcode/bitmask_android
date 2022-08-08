package se.leap.bitmaskclient.base.utils;

import android.content.Context;

import java.lang.reflect.Method;


public class SystemPropertiesHelper {

    /**
     * Checks if SystemProperties contains a given key using reflection
     * @return true if reflection was successful and the key was found
     */
    public static boolean contains(String key, Context context) {
        String result = null;
        try {
            ClassLoader cl = context.getClassLoader();
            @SuppressWarnings("rawtypes")
            Class SystemProperties = cl.loadClass("android.os.SystemProperties");
            @SuppressWarnings("rawtypes")
            Class[] paramTypes= new Class[1];
            paramTypes[0]= String.class;

            Method get = SystemProperties.getMethod("get", paramTypes);
            Object[] params= new Object[1];
            params[0]= key;

            result = (String) get.invoke(SystemProperties, params);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result != null && !result.isEmpty();
    }
}

