package se.leap.bitmaskclient.tor;
/**
 * Copyright (c) 2026 LEAP Encryption Access Project and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

import static se.leap.bitmaskclient.tor.SnowflakePlugin.RendezvousStrategy.HTTP;
import static se.leap.bitmaskclient.tor.SnowflakePlugin.RendezvousStrategy.SQS;
import static se.leap.bitmaskclient.tor.TorStatusObservable.SnowflakeStatus.RECONNECTING;
import static se.leap.bitmaskclient.tor.TorStatusObservable.SnowflakeStatus.STOPPED;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import org.jetbrains.annotations.Blocking;
import org.torproject.jni.ClientTransportPluginInterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeoutException;

import IPtProxy.Controller;
import IPtProxy.IPtProxy;
import IPtProxy.OnTransportEvents;
import mobilemodels.BitmaskMobileCore;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.utils.BitmaskCoreProvider;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;

public class SnowflakePlugin implements ClientTransportPluginInterface {
    public static String TAG = SnowflakePlugin.class.getSimpleName();

    enum RendezvousStrategy {
        HTTP,
        // sqs queue
        SQS,
        //amp cache
        AMP
    }

    private HashMap<String, String> mFronts;
    private final WeakReference<Context> contextRef;
    Handler handler;
    HandlerThread handlerThread;
    Controller controller;

    /**
     * Constructs a SnowflakePlugin instance.
     *
     * @param context an arbitrary context
     * @throws IllegalStateException if initialization of the IPtProxy controller fails
     */
    public SnowflakePlugin(Context context) throws IllegalStateException {
        Log.d(TAG, "initialize ClientTransport Plugin");
        controller = initializeController(context.getApplicationContext());
        this.contextRef = new WeakReference<>(context.getApplicationContext());
        handlerThread = new HandlerThread("clientTransportPlugin", Thread.MIN_PRIORITY);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        loadFronts(context.getApplicationContext());
    }

    private Controller initializeController(Context context)  throws IllegalStateException {
        File ptDir = new File(context.getCacheDir(), "/pt_state/");
        Controller controller = IPtProxy.newController(ptDir.getAbsolutePath(), true, false, "DEBUG", new OnTransportEvents() {
            @Override
            public void connected(String s) {
                Log.d(TAG, "snowflake status callback: connected");
                TorStatusObservable.logSnowflakeMessage(context, context.getString(R.string.log_done));
                TorStatusObservable.setSnowflakeStatus(TorStatusObservable.SnowflakeStatus.CONNECTED);
            }

            @Override
            public void error(String s, Exception e) {
                Log.d(TAG, "snowflake status callback: error - " + e.getMessage());
                TorStatusObservable.logSnowflakeMessage(context, e.getMessage());
                TorStatusObservable.logSnowflakeMessage(context, context.getString(R.string.state_reconnecting));
                TorStatusObservable.setSnowflakeStatus(RECONNECTING, e.getMessage());
            }

            @Override
            public void stopped(String s, Exception e) {
                Log.d(TAG, "snowflake status callback: stopped");
                if (e != null && e.getMessage() != null) {
                    Log.e(TAG, e.getMessage());
                }
                TorStatusObservable.logSnowflakeMessage(context, context.getString(R.string.state_noprocess));
                TorStatusObservable.setSnowflakeStatus(STOPPED);
            }
        });

        if (controller == null) {
            throw new IllegalStateException("Failed to initialize IPtProxy controller");
        }
        return controller;
    }

    /**
     * Starts Snowflake.
     *
     * This method selects the best rendezvous strategy based on the user's
     * estimated broad geographical location and initiates the connection attempt.
     */
    @Override
    public void start() {
        Context context = contextRef.get();
        if (context == null) {
            return;
        }

        shortenLogs(context);
        RendezvousStrategy strategy = selectRendezvousStrategy();
        startConnectionAttempt(strategy);
    }

    private void shortenLogs(Context context) {
        // ensure we keep the persisted logfile short
        File logfile = new File(context.getApplicationContext().getCacheDir(), "/pt_state/" + IPtProxy.LogFileName);
        try {
            if (logfile.exists()) {
                logfile.delete();
            }
            logfile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Attempt to select best rendezvous strategy depending on users broad location.
     * @return a snowflake RendezvousStrategy
     */
    private RendezvousStrategy selectRendezvousStrategy() {
        Random random = new Random();
        ArrayList<RendezvousStrategy> strategies = new ArrayList<>(Arrays.asList(RendezvousStrategy.values()));
        String countryCode = getCountryCode();
        if ("RU".equals(countryCode) || "CN".equals(countryCode)) {
            strategies.remove(RendezvousStrategy.HTTP);
        } else {
            strategies.remove(SQS);
        }
        int randomIndex = random.nextInt(strategies.size());
        return strategies.get(randomIndex);
    }

    /**
     * Attempt to determine the country code from the users IP address
     * @return country code or null if geoip lookup failed
     */
    private String getCountryCode() {
        try {
            String geoIPLookupURL = PreferenceHelper.getGeoIPLookupURL();
            if (geoIPLookupURL != null) {
                BitmaskMobileCore bm = BitmaskCoreProvider.getBitmaskMobile();
                String stunServers = getFront(HTTP, "ice").replaceAll("stun:", "");
                List<String> serverList = Arrays.asList(stunServers.split(","));
                Collections.shuffle(serverList);
                stunServers =  String.join(",", serverList);
                bm.setStunServers(stunServers);
                bm.setCountryCodeLookupURL(geoIPLookupURL);
                return bm.getGeolocation();
            }
        } catch (Exception e) {
            // ignore
        }
        return PreferenceHelper.getBaseCountry();
    }

    private void startConnectionAttempt(RendezvousStrategy strategy) {
        try {
            String target = null;
            String fronts = null;
            String ampCache = null;
            String sqsQueue = null;
            String sqsCreds = null;
            String stunServer = getFront(strategy, "ice");
            switch (strategy) {
                case HTTP -> {
                    target = getFront(strategy, "url");
                    fronts = getFront(strategy, "fronts");
                }
                case AMP -> {
                    target = getFront(strategy, "url");
                    fronts = getFront(strategy, "fronts");
                    ampCache = getFront(strategy, "ampcache");
                }
                case SQS -> {
                    sqsCreds = getFront(strategy, "sqscreds");
                    sqsQueue = getFront(strategy, "sqsqueue");
                }
            }


            startSnowflake(stunServer, target, fronts, ampCache, sqsQueue, sqsCreds, 5);
        } catch (NullPointerException npe) {
            Log.e(TAG, "failed to start Snowflake: " + npe.getMessage());
            Context c = contextRef.get();
            if (c != null) {
                TorStatusObservable.logSnowflakeMessage(c, npe.getMessage());
            }
            TorStatusObservable.setSnowflakeStatus(STOPPED);
        }
    }

/**
     StartSnowflake - Start IPtProxy's Snowflake client.
     @param ice Comma-separated list of ICE servers.
     @param url URL of signaling broker.
     @param fronts Comma-separated list of front domains.
     @param ampCache OPTIONAL. URL of AMP cache to use as a proxy for signaling.
        Only needed when you want to do the rendezvous over AMP instead of a domain fronted server.
     @param sqsQueueURL OPTIONAL. URL of SQS Queue to use as a proxy for signaling.
     @param sqsCredsStr OPTIONAL. Credentials to access SQS Queue
     @param maxPeers Capacity for number of multiplexed WebRTC peers. DEFAULTs to 1 if less than that.
 */
    private void startSnowflake(String ice, String url, String fronts, String ampCache, String sqsQueueURL, String sqsCredsStr, long maxPeers) {
        Context c = contextRef.get();
        try {
            Log.d(TAG, "start snowflake with ice " + ice + "\nurl: " + url + "\nfronts: " + fronts + "\nampCache: " + ampCache + "\nsqsQueue: " + sqsQueueURL);
            if (ice != null) controller.setSnowflakeIceServers(ice);
            if (url != null) controller.setSnowflakeBrokerUrl(url);
            if (fronts != null) controller.setSnowflakeFrontDomains(fronts);
            if (ampCache != null) controller.setSnowflakeAmpCacheUrl(ampCache);
            if (sqsQueueURL != null) controller.setSnowflakeSqsUrl(sqsQueueURL);
            if (sqsCredsStr != null) controller.setSnowflakeSqsCreds(sqsCredsStr);
            controller.setSnowflakeMaxPeers(maxPeers);
            controller.start(IPtProxy.Snowflake, "");
            if (c != null) {
                TorStatusObservable.logSnowflakeMessage(c, c.getString(R.string.snowflake_started));
            }
            TorStatusObservable.setSnowflakeStatus(TorStatusObservable.SnowflakeStatus.STARTED);
        } catch (Exception e) {
            Log.e(TAG, this.toString() + "failed to start Snowflake: " + e.getMessage());
            if (c != null) {
                TorStatusObservable.logSnowflakeMessage(c, e.getMessage());
            }
            TorStatusObservable.setSnowflakeStatus(STOPPED);
        }
    }

    /**
     * Stops Snowflake connection.
     * This method is blocking the current thread until snowflake was stopped or the timeout reached.
     */
    @Blocking
    @Override
    public void stop() {
        controller.stop(IPtProxy.Snowflake);
        try {
            TorStatusObservable.waitUntil(this::isSnowflakeOff, 10);
        } catch (InterruptedException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    private boolean isSnowflakeOff() {
        return TorStatusObservable.getSnowflakeStatus() == TorStatusObservable.SnowflakeStatus.STOPPED;
    }

    /**
     * Retrieves the Tor configuration string for the Snowflake pluggable transport.
     *
     * @return a Tor configuration string if the controller is running, otherwise an empty string
     */
    @Override
    public String getTorrc() {
        if (controller.port(IPtProxy.Snowflake) <= 0) {
            return "";
        }
        return "UseBridges 1\n" +
                "ClientTransportPlugin snowflake socks5 127.0.0.1:" + controller.port(IPtProxy.Snowflake) + "\n" +
                "Bridge snowflake 192.0.2.3:1";
    }

    private void loadFronts(Context context) {
        if (mFronts == null) {
            mFronts = new HashMap<>();
        }
        for (RendezvousStrategy strategy : RendezvousStrategy.values()) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("fronts-"+strategy.toString().toLowerCase())));
                String line;
                while (true) {
                    line = reader.readLine();
                    if (line == null) break;
                    String[] front = line.split(" ");
                    mFronts.put(strategy+front[0], front[1]);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private String getFront(RendezvousStrategy strategy, String arg) throws NullPointerException {
        if (mFronts != null) {
            String front = mFronts.get(strategy+arg);
            if (front != null) {
                return front;
            }
        }
        throw new NullPointerException("expected value "+ arg + " for rendezvous strategy " + strategy + " not found.");
    }
}
