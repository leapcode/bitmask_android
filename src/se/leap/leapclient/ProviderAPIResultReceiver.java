package se.leap.leapclient;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

/**
 * Implements the ResultReceiver needed by Activities using ProviderAPI to receive the results of its operations. 
 * @author parmegv
 *
 */
public class ProviderAPIResultReceiver extends ResultReceiver {
	private Receiver mReceiver;
	
	public ProviderAPIResultReceiver(Handler handler) {
		super(handler);
		// TODO Auto-generated constructor stub
	}
	
	public void setReceiver(Receiver receiver) {
        mReceiver = receiver;
    }

	/**
	 * Interface to enable ProviderAPIResultReceiver to receive results from the ProviderAPI IntentService. 
	 * @author parmegv
	 *
	 */
    public interface Receiver {
        public void onReceiveResult(int resultCode, Bundle resultData);
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        if (mReceiver != null) {
            mReceiver.onReceiveResult(resultCode, resultData);
        }
    }
    
}
