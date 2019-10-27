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

import android.util.Log;

import de.blinkt.openvpn.core.VpnStatus;
import shapeshifter.ShapeShifter;

public class Shapeshifter  {

    public static final String DISPATCHER_PORT = "4430";
    public static final String DISPATCHER_IP = "127.0.0.1";
    private static final String TAG = Shapeshifter.class.getSimpleName();

    private ShapeShifter shapeShifter;

    public class ShapeshifterLogger implements shapeshifter.Logger {
        @Override
        public void log(String s) {
            Log.e(TAG, "SHAPESHIFTER ERROR: " + s);
            VpnStatus.logError(VpnStatus.ErrorType.SHAPESHIFTER);
            VpnStatus.logError(s);
            try {
                shapeShifter.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public  Shapeshifter(Obfs4Options options) {
        shapeShifter = new ShapeShifter();
        shapeShifter.setLogger(new ShapeshifterLogger());
        setup(options);
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

    public boolean stop() {
        try {
            shapeShifter.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            VpnStatus.logError(e.getLocalizedMessage());
        }
        return false;
    }

}
