package se.leap.bitmaskclient.base.utils;

import android.content.Context;

import org.torproject.jni.ClientTransportPluginInterface;

import se.leap.bitmaskclient.tor.SnowflakePlugin;

/**
 * ClientTransportPluginProvider keeps a global reference of an implementation of ClientTransportPluginInterface.
 * Currently the underlying plugin is utilizing IPtPtroxy and that library cannot be instantiated twice.
 * Therefore we need keep a global reference to it and pass that around.
 */
public class ClientTransportPluginProvider {
    private final SnowflakePlugin plugin;
    private static ClientTransportPluginProvider instance = null;

    private ClientTransportPluginProvider(Context context) throws IllegalStateException {
        plugin = new SnowflakePlugin(context);
    }

    public static void init(Context context) {
        if (instance == null) {
            try {
                instance = new ClientTransportPluginProvider(context);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }


    public static ClientTransportPluginInterface get() {
        if (instance != null) {
            return instance.plugin;
        }
        return null;
    }
}
