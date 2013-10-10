/**
 * Copyright (c) 2013 LEAP Encryption Access Project and contributers
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
 package se.leap.bitmaskclient;

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
