package se.leap.bitmaskclient.pluggableTransports;

import android.util.Log;

import shapeshifter.ShapeShifter;

public class Shapeshifter {

    public static final String DISPATCHER_PORT = "4430";
    public static final String DISPATCHER_IP = "127.0.0.1";
    private static final String TAG = Shapeshifter.class.getSimpleName();

    ShapeShifter shapeShifter;

    public  Shapeshifter(DispatcherOptions options) {
        shapeShifter = new ShapeShifter();
        shapeShifter.setIatMode(Long.valueOf(options.iatMode));
        shapeShifter.setSocksAddr(DISPATCHER_IP+":"+DISPATCHER_PORT);
        shapeShifter.setTarget(options.remoteIP+":"+options.remotePort);
        shapeShifter.setCert(options.cert);
        Log.d(TAG, "shapeshifter initialized with: iat - " + shapeShifter.getIatMode() +
                "; socksAddr - " + shapeShifter.getSocksAddr() +
                "; target addr - " + shapeShifter.getTarget() +
                "; cert - " + shapeShifter.getCert());
    }

    public boolean start() {
        try {
            shapeShifter.open();
            Log.d(TAG, "shapeshifter opened");
            return true;
        } catch (Exception e) {
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
        }
        return false;
    }
}
