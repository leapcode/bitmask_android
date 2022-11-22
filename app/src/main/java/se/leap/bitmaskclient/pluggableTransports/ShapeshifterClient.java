/**
 * Copyright (c) 2020 LEAP Encryption Access Project and contributors
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

package se.leap.bitmaskclient.pluggableTransports;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Observable;
import java.util.Observer;

import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.VpnStatus;
import se.leap.bitmaskclient.eip.EipStatus;

public class ShapeshifterClient implements Observer {

    public static final String DISPATCHER_PORT = "4430";
    public static final String DISPATCHER_IP = "127.0.0.1";
    private static final int MAX_RETRY = 5;
    private static final int RETRY_TIME = 4000;
    private static final String TAG = ShapeshifterClient.class.getSimpleName();

    private final shapeshifter.Shapeshifter_ shapeShifter;
    private boolean isErrorHandling;
    private boolean noNetwork;
    private int retry = 0;
    private final Handler reconnectHandler;

    public class ShapeshifterLogger implements shapeshifter.Logger {
        @Override
        public void log(String s) {
            Log.e(TAG, "SHAPESHIFTER ERROR: " + s);
            VpnStatus.logError(s);
            isErrorHandling = true;
            close();

            if (retry < MAX_RETRY && !noNetwork) {
                retry++;
                reconnectHandler.postDelayed(ShapeshifterClient.this::reconnect, RETRY_TIME);
            } else {
                VpnStatus.logError(VpnStatus.ErrorType.SHAPESHIFTER);
            }
        }
    }

    public ShapeshifterClient(Obfs4Options options) {
        shapeShifter = new shapeshifter.Shapeshifter_();
        shapeShifter.setLogger(new ShapeshifterLogger());
        setup(options);
        Looper.prepare();
        reconnectHandler = new Handler();
        EipStatus.getInstance().addObserver(this);
        Log.d(TAG, "shapeshifter initialized with: \n" + shapeShifter.toString());
    }

    private void setup(Obfs4Options options) {
        shapeShifter.setSocksAddr(DISPATCHER_IP+":"+DISPATCHER_PORT);
        shapeShifter.setTarget(options.remoteIP+":"+options.remotePort);
        shapeShifter.setCert(options.cert);
    }

    public void setOptions(Obfs4Options options) {
        setup(options);
    }

    public void start() {
        try {
            shapeShifter.open();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "SHAPESHIFTER ERROR: " + e.getLocalizedMessage());
            VpnStatus.logError(VpnStatus.ErrorType.SHAPESHIFTER);
            VpnStatus.logError(e.getLocalizedMessage());
        }
    }

    private void close() {
        try {
            shapeShifter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void reconnect() {
        try {
            shapeShifter.open();
            retry = 0;
            isErrorHandling = false;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "SHAPESHIFTER RECONNECTION ERROR: " + e.getLocalizedMessage());
            VpnStatus.logError("Unable to reconnect shapeshifter: " + e.getLocalizedMessage());
        }
    }

    public boolean stop() {
        try {
            shapeShifter.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            VpnStatus.logError(e.getLocalizedMessage());
        }
        EipStatus.getInstance().deleteObserver(this);
        return false;
    }

    @Override
    public void update(Observable observable, Object arg) {
        if (observable instanceof EipStatus) {
            EipStatus status = (EipStatus) observable;
            if (status.getLevel() == ConnectionStatus.LEVEL_NONETWORK) {
                noNetwork = true;
            } else {
                noNetwork = false;
                if (isErrorHandling) {
                    isErrorHandling = false;
                    close();
                    start();
                }
            }
        }
    }
}
