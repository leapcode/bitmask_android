/*
 * Copyright (c) 2012-2018 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core;

import android.content.Context;
import android.net.*;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.net.ConnectivityManagerCompat;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.util.Vector;

public class NetworkUtils {

    private static final String TAG = NetworkUtils.class.getSimpleName();

    public static Vector<String> getLocalNetworks(Context c, boolean ipv6) {
        Vector<String> nets = new Vector<>();
        ConnectivityManager conn = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network[] networks = conn.getAllNetworks();
        for (Network network : networks) {
            try {
                NetworkInfo ni = conn.getNetworkInfo(network);
                LinkProperties li = conn.getLinkProperties(network);

                NetworkCapabilities nc = conn.getNetworkCapabilities(network);

                if (nc == null) {
                    continue;
                }

                // Skip VPN networks like ourselves
                if (nc.hasTransport(NetworkCapabilities.TRANSPORT_VPN))
                    continue;

                // Also skip mobile networks
                if (nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                    continue;


                for (LinkAddress la : li.getLinkAddresses()) {
                    if ((la.getAddress() instanceof Inet4Address && !ipv6) ||
                            (la.getAddress() instanceof Inet6Address && ipv6))
                        nets.add(la.toString());
                }
            } catch (SecurityException se) {
                se.printStackTrace();
            }
        }
        return nets;
    }
}