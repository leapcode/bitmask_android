/**
 * Copyright (c) 2019 LEAP Encryption Access Project and contributors
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

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.ref.WeakReference;

import de.blinkt.openvpn.core.VpnStatus;
import shapeshifter.ShapeShifter;

public class Shapeshifter  {

    public static final String DISPATCHER_PORT = "4430";
    public static final String DISPATCHER_IP = "127.0.0.1";
    private static final String TAG = Shapeshifter.class.getSimpleName();

    private ShapeShifter shapeShifter;
    private ShapeshifterErrorListner shapeshifterErrorListner;

    public interface ShapeshifterErrorListenerCallback {
        void onStarted();
    }

    public  Shapeshifter(Obfs4Options options) {
        shapeShifter = new ShapeShifter();
        setup(options);
        Log.d(TAG, "shapeshifter initialized with: \n" + shapeShifter.toString());
    }

    private void setup(Obfs4Options options) {
        shapeShifter.setSocksAddr(DISPATCHER_IP+":"+DISPATCHER_PORT);
        shapeShifter.setTarget(options.remoteIP+":"+options.remotePort);
        shapeShifter.setCert(options.cert);
    }

    public void start() {
        ShapeshifterErrorListenerCallback errorListenerCallback = () -> {
            ;
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> {
                try {
                    Log.d(TAG, "shapeshifter open");
                    shapeShifter.open();
                } catch (Exception e) {
                    Log.e(TAG, "SHAPESHIFTER ERROR: " + e.getLocalizedMessage());
                    VpnStatus.logError(VpnStatus.ErrorType.SHAPESHIFTER);
                    VpnStatus.logError(e.getLocalizedMessage());
                }
            }, 200);
        };
        shapeshifterErrorListner = new ShapeshifterErrorListner(errorListenerCallback);
        shapeshifterErrorListner.execute(shapeShifter);
    }

    public boolean stop() {
        try {
            shapeShifter.close();
            shapeshifterErrorListner.cancel(true);
            Log.d(TAG, "shapeshifter closed");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            VpnStatus.logError("SHAPESHIFTER ERROR " + e.getLocalizedMessage());
        }
        return false;
    }

    static class ShapeshifterErrorListner extends AsyncTask<ShapeShifter, Void, Void> {

        WeakReference<ShapeshifterErrorListenerCallback> callbackWeakReference;

        public ShapeshifterErrorListner(ShapeshifterErrorListenerCallback callback) {
            callbackWeakReference = new WeakReference<>(callback);
        }

        @Override
        protected Void doInBackground(ShapeShifter... shapeShifters) {
            ShapeShifter shapeshifter = shapeShifters[0];
            Log.d(TAG, "Shapeshifter error listener started");
            ShapeshifterErrorListenerCallback callback = callbackWeakReference.get();
            if (callback != null) {
                callback.onStarted();
            }
            try {
                shapeshifter.getLastError();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "SHAPESHIFTER ERROR: " + e.getLocalizedMessage());
                VpnStatus.logError(VpnStatus.ErrorType.SHAPESHIFTER);
                VpnStatus.logError(e.getLocalizedMessage());
                try {
                    shapeshifter.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            return null;
        }
    }

}
