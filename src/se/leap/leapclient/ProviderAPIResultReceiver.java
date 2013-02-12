package se.leap.leapclient;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

public class ProviderAPIResultReceiver extends ResultReceiver {
	private Receiver mReceiver;
	
	public ProviderAPIResultReceiver(Handler handler) {
		super(handler);
		// TODO Auto-generated constructor stub
	}

	public void setReceiver(Receiver receiver) {
        mReceiver = receiver;
    }

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
