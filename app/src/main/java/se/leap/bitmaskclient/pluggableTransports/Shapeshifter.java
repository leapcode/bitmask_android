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
import android.util.Log;

import de.blinkt.openvpn.core.VpnStatus;
import shapeshifter.ShapeShifter;

public class Shapeshifter {

    public static final String DISPATCHER_PORT = "4430";
    public static final String DISPATCHER_IP = "127.0.0.1";
    private static final String TAG = Shapeshifter.class.getSimpleName();

    ShapeShifter shapeShifter;
    ShapeshifterErrorListner shapeshifterErrorListner;

    public  Shapeshifter(Obfs4Options options) {
        shapeShifter = new ShapeShifter();
        shapeShifter.setSocksAddr(DISPATCHER_IP+":"+DISPATCHER_PORT);
        shapeShifter.setTarget(options.remoteIP+":"+options.remotePort);
        shapeShifter.setCert(options.cert);
        Log.d(TAG, "shapeshifter initialized with: \n" + shapeShifter.toString());
    }

    public boolean start() {
        try {
            shapeshifterErrorListner = new ShapeshifterErrorListner();
            shapeshifterErrorListner.execute(shapeShifter);
            shapeShifter.open();
            Log.d(TAG, "shapeshifter opened");
            return true;
        } catch (Exception e) {
            VpnStatus.logError("SHAPESHIFTER ERROR " + e.getLocalizedMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean stop() {
        try {
            shapeShifter.close();
            Log.d(TAG, "shapeshifter closed");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            VpnStatus.logError("SHAPESHIFTER ERROR " + e.getLocalizedMessage());
        }
        return false;
    }

    static class ShapeshifterErrorListner extends AsyncTask<ShapeShifter, Void, Void> {
        @Override
        protected Void doInBackground(ShapeShifter... shapeShifters) {
            ShapeShifter shapeshifter = shapeShifters[0];
            try {
                shapeshifter.getLastError();
            } catch (Exception e) {
                e.printStackTrace();
                VpnStatus.logError("SHAPESHIFTER ERROR " + e.getLocalizedMessage());
                try {
                    shapeshifter.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    VpnStatus.logError("SHAPESHIFTER ERROR " + e.getLocalizedMessage());
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

}
