package se.leap.bitmaskclient.eip;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import static android.content.Intent.CATEGORY_DEFAULT;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_EIP_EVENT;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_CODE;
import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_KEY;
import static se.leap.bitmaskclient.base.models.Constants.EIP_REQUEST;

public class EipResultBroadcast {
    private static final String TAG = EipResultBroadcast.class.getSimpleName();


    /**
     * send resultCode and resultData to receiver or
     * broadcast the result if no receiver is defined
     *
     * @param action     the action that has been performed
     * @param resultCode RESULT_OK if action was successful RESULT_CANCELED otherwise
     */
    public static void tellToReceiverOrBroadcast(Context context, String action, int resultCode) {
        tellToReceiverOrBroadcast(context, action, resultCode, null, new Bundle());
    }

    /**
     * send resultCode and resultData to receiver or
     * broadcast the result if no receiver is defined
     *
     * @param action     the action that has been performed
     * @param resultCode RESULT_OK if action was successful RESULT_CANCELED otherwise
     * @param resultData other data to broadcast or return to receiver
     */
    public static void tellToReceiverOrBroadcast(Context context, String action, int resultCode, ResultReceiver receiver, Bundle resultData) {
        resultData.putString(EIP_REQUEST, action);
        if (receiver != null) {
            receiver.send(resultCode, resultData);
        } else {
            broadcastEvent(context, resultCode, resultData);
        }
    }

    /**
     * send resultCode and resultData to receiver or
     * broadcast the result if no receiver is defined
     *
     * @param action     the action that has been performed
     * @param resultCode RESULT_OK if action was successful RESULT_CANCELED otherwise
     * @param resultData other data to broadcast or return to receiver
     */
    public static void tellToReceiverOrBroadcast(Context context, String action, int resultCode, Bundle resultData) {
        resultData.putString(EIP_REQUEST, action);
        broadcastEvent(context, resultCode, resultData);
    }



    /**
     * broadcast result
     *
     * @param resultCode RESULT_OK if action was successful RESULT_CANCELED otherwise
     * @param resultData other data to broadcast or return to receiver
     */
    public static void broadcastEvent(Context context, int resultCode, Bundle resultData) {
        Intent intentUpdate = new Intent(BROADCAST_EIP_EVENT);
        intentUpdate.addCategory(CATEGORY_DEFAULT);
        intentUpdate.putExtra(BROADCAST_RESULT_CODE, resultCode);
        intentUpdate.putExtra(BROADCAST_RESULT_KEY, resultData);
        Log.d(TAG, "sending broadcast");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intentUpdate);
    }
}
