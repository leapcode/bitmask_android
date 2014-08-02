package se.leap.bitmaskclient;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.ProviderAPIResultReceiver;
import se.leap.bitmaskclient.ProviderAPIResultReceiver.Receiver;
import se.leap.bitmaskclient.Dashboard;

import de.blinkt.openvpn.activities.LogWindow;
import de.blinkt.openvpn.core.VpnStatus;
import de.blinkt.openvpn.core.VpnStatus.ConnectionStatus;
import de.blinkt.openvpn.core.VpnStatus.StateListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

public class EipServiceFragment extends Fragment implements StateListener, OnCheckedChangeListener {
	
	protected static final String IS_EIP_PENDING = "is_eip_pending";
    public static final String START_ON_BOOT = "start on boot";
	
	private View eipFragment;
	private static Switch eipSwitch;
	private View eipDetail;
	private TextView eipStatus;

	// private boolean eipAutoSwitched = true;
    
 	private boolean mEipStartPending = false;

    private static EIPReceiver mEIPReceiver;

    
    public static String TAG = "se.leap.bitmask.EipServiceFragment";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		eipFragment = inflater.inflate(R.layout.eip_service_fragment, container, false);		
		eipDetail = ((RelativeLayout) eipFragment.findViewById(R.id.eipDetail));
		eipDetail.setVisibility(View.VISIBLE);

		View eipSettings = eipFragment.findViewById(R.id.eipSettings);
		eipSettings.setVisibility(View.GONE); // FIXME too!

		if (mEipStartPending)
			eipFragment.findViewById(R.id.eipProgress).setVisibility(View.VISIBLE);
		
		eipStatus = (TextView) eipFragment.findViewById(R.id.eipStatus);

		eipSwitch = (Switch) eipFragment.findViewById(R.id.eipSwitch);
		eipSwitch.setOnCheckedChangeListener(this);
		
		if(getArguments() != null && getArguments().containsKey(START_ON_BOOT) && getArguments().getBoolean(START_ON_BOOT))
		    startEipFromScratch();
		
		return eipFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mEIPReceiver = new EIPReceiver(new Handler());

		if (savedInstanceState != null)
			mEipStartPending = savedInstanceState.getBoolean(IS_EIP_PENDING);
	}

	@Override
	public void onResume() {
		super.onResume();

		VpnStatus.addStateListener(this);
		
		eipCommand(EIP.ACTION_CHECK_CERT_VALIDITY);
		
		if(isEipConnected()) {
		    eipSwitch.setChecked(true);
		}
	}
    
	@Override
	public void onPause() {
		super.onPause();

		VpnStatus.removeStateListener(this);
	}
    
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(IS_EIP_PENDING, mEipStartPending);
	}

    protected void saveEipStatus() {
	boolean eip_is_on = false;
	Log.d("bitmask", "saveEipStatus");
	if(eipSwitch.isChecked()) {
	    eip_is_on = true;
	}

	if(getActivity() != null)
	    getActivity().getSharedPreferences(Dashboard.SHARED_PREFERENCES, Activity.MODE_PRIVATE).edit().putBoolean(Dashboard.START_ON_BOOT, eip_is_on).commit();
    }
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
	if (buttonView.equals(eipSwitch)){
	    handleEipSwitch(isChecked);
	}
    }

    private boolean isAllowedAnon() {
	return getActivity().getSharedPreferences(Dashboard.SHARED_PREFERENCES, Activity.MODE_PRIVATE).getBoolean(EIP.ALLOWED_ANON, false);
    }
    private boolean isEipConnected() {
	return getEIPString(EIP.STATUS).equalsIgnoreCase("LEVEL_CONNECTED");
    }
    private String getEIPString(String feature) {
	return getActivity().getSharedPreferences(Dashboard.SHARED_PREFERENCES, Activity.MODE_PRIVATE).getString(feature, "");
    }

    private boolean canStartEIP() {
	return (isAllowedAnon() || !getEIPString(EIP.CERTIFICATE).isEmpty()) && !mEipStartPending && !isEipConnected();
    }
    
    private void handleEipSwitch(boolean isChecked) {
	if(isChecked) {
	    handleEipSwitchOn();
	} else {
	    handleEipSwitchOff();
	}
	saveEipStatus();
    }

    private void handleEipSwitchOn() {
	if(canStartEIP()) {
	    startEipFromScratch();
	}
    }

    private void handleEipSwitchOff() {
	if(mEipStartPending) {
	    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
	    alertBuilder.setTitle(getResources().getString(R.string.eip_cancel_connect_title))
		.setMessage(getResources().getString(R.string.eip_cancel_connect_text))
		.setPositiveButton((R.string.eip_cancel_connect_cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			    stopEIP();
			}
		    })
		.setNegativeButton(getResources().getString(R.string.eip_cancel_connect_cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			    eipSwitch.setChecked(true);
			}
		    })
		.show();
	} else if(isEipConnected()) {
	    Log.d(TAG, "Stopping EIP");
	    stopEIP();
	}
    }

    public void startEipFromScratch() {
	mEipStartPending = true;
	    eipFragment.findViewById(R.id.eipProgress).setVisibility(View.VISIBLE);
	    eipStatus.setText(R.string.eip_status_start_pending);
	
	if(!eipSwitch.isChecked()) {
	    eipSwitch.setChecked(true);	
	    saveEipStatus();
	}
	eipCommand(EIP.ACTION_START_EIP);
    }

    private void stopEIP() {
	mEipStartPending = false;
	View eipProgressBar = getActivity().findViewById(R.id.eipProgress);
	if(eipProgressBar != null)
	    eipProgressBar.setVisibility(View.GONE);
	if(eipStatus != null)
	    eipStatus.setText(R.string.eip_state_not_connected);
	eipCommand(EIP.ACTION_STOP_EIP);
    }
	
	/**
	 * Send a command to EIP
	 * 
	 * @param action	A valid String constant from EIP class representing an Intent
	 * 					filter for the EIP class 
	 */
	private void eipCommand(String action){
		// TODO validate "action"...how do we get the list of intent-filters for a class via Android API?
	    Intent vpn_intent = new Intent(getActivity().getApplicationContext(), EIP.class);
	    vpn_intent.setAction(action);
	    vpn_intent.putExtra(EIP.RECEIVER_TAG, mEIPReceiver);
	    getActivity().startService(vpn_intent);
	}
	
	@Override
	public void updateState(final String state, final String logmessage, final int localizedResId, final ConnectionStatus level) {
		// Note: "states" are not organized anywhere...collected state strings:
		//		NOPROCESS,NONETWORK,BYTECOUNT,AUTH_FAILED + some parsing thing ( WAIT(?),AUTH,GET_CONFIG,ASSIGN_IP,CONNECTED,SIGINT )
		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (eipStatus != null) {
					boolean switchState = true;
					String statusMessage = "";
					String prefix = getString(localizedResId);
					if (level == ConnectionStatus.LEVEL_CONNECTED){
						statusMessage = getString(R.string.eip_state_connected);
						getActivity().findViewById(R.id.eipProgress).setVisibility(View.GONE);
						mEipStartPending = false; //TODO This should be done in the onReceiveResult from START_EIP command, but right now LaunchVPN isn't notifying anybody the resultcode of the request so we need to listen the states with this listener.
					} else if ( (level == ConnectionStatus.LEVEL_NONETWORK || level == ConnectionStatus.LEVEL_NOTCONNECTED || level == ConnectionStatus.LEVEL_AUTH_FAILED) && !mEipStartPending) {
					    Log.d(TAG, "Not connected updated state");
						statusMessage = getString(R.string.eip_state_not_connected);
						if(getActivity() != null && getActivity().findViewById(R.id.eipProgress) != null)
						    getActivity().findViewById(R.id.eipProgress).setVisibility(View.GONE);
						mEipStartPending = false; //TODO See above
						switchState = false;
					} else if (level == ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED) {
					    if(state.equals("AUTH") || state.equals("GET_CONFIG"))
						statusMessage = prefix + " " + logmessage;
					} else if (level == ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET) {
						statusMessage = prefix + " " + logmessage;
					}
					
					// eipAutoSwitched = true;
					// eipSwitch.setChecked(switchState);
					// eipAutoSwitched = false;
					eipStatus.setText(statusMessage);
				}
			}
		});
	}


	/**
	 * Inner class for handling messages related to EIP status and control requests
	 * 
	 * @author Sean Leonard <meanderingcode@aetherislands.net>
	 */
	protected class EIPReceiver extends ResultReceiver {
		
		protected EIPReceiver(Handler handler){
			super(handler);
		}

		@Override
		protected void onReceiveResult(int resultCode, Bundle resultData) {
			super.onReceiveResult(resultCode, resultData);
		
			String request = resultData.getString(EIP.REQUEST_TAG);
			boolean checked = false;
			
			if (request == EIP.ACTION_IS_EIP_RUNNING) {
				switch (resultCode){
				case Activity.RESULT_OK:
					checked = true;
					break;
				case Activity.RESULT_CANCELED:
					checked = false;
					break;
				}
			} else if (request == EIP.ACTION_START_EIP) {
				switch (resultCode){
				case Activity.RESULT_OK:
				    Log.d(TAG, "Action start eip = Result OK");
					checked = true;
					eipFragment.findViewById(R.id.eipProgress).setVisibility(View.VISIBLE);
					mEipStartPending = false;
					break;
				case Activity.RESULT_CANCELED:
					checked = false;
					eipFragment.findViewById(R.id.eipProgress).setVisibility(View.GONE);
					break;
				}
			} else if (request == EIP.ACTION_STOP_EIP) {
				switch (resultCode){
				case Activity.RESULT_OK:
					checked = false;
					break;
				case Activity.RESULT_CANCELED:
					checked = true;
					break;
				}
			} else if (request == EIP.EIP_NOTIFICATION) {
				switch  (resultCode){
				case Activity.RESULT_OK:
					checked = true;
					break;
				case Activity.RESULT_CANCELED:
					checked = false;
					break;
				}
			} else if (request == EIP.ACTION_CHECK_CERT_VALIDITY) {
			    checked = eipSwitch.isChecked();
			    
			    switch (resultCode) {
			    case Activity.RESULT_OK:
				break;
			    case Activity.RESULT_CANCELED:
				Dashboard dashboard = (Dashboard) getActivity();
				
				dashboard.setProgressBarVisibility(ProgressBar.VISIBLE);
				dashboard.setEipStatus(R.string.updating_certificate_message);
				
				Intent provider_API_command = new Intent(getActivity(), ProviderAPI.class);
				if(dashboard.providerAPI_result_receiver == null) {
				    dashboard.providerAPI_result_receiver = new ProviderAPIResultReceiver(new Handler());
				    dashboard.providerAPI_result_receiver.setReceiver(dashboard);
				}
				
				provider_API_command.setAction(ProviderAPI.DOWNLOAD_CERTIFICATE);
				provider_API_command.putExtra(ProviderAPI.RECEIVER_KEY, dashboard.providerAPI_result_receiver);
				getActivity().startService(provider_API_command);
				break;
			    }
			}
			
			// eipAutoSwitched = true;
			// eipSwitch.setChecked(checked);
			// eipAutoSwitched = false;
		}
	}
    

    public static EIPReceiver getReceiver() {
	return mEIPReceiver;
    }

    public static boolean isEipSwitchChecked() {
	return eipSwitch.isChecked();
    }

    public void checkEipSwitch(boolean checked) {
	eipSwitch.setChecked(checked);
	// Log.d(TAG, "checkEipSwitch");
	// onCheckedChanged(eipSwitch, checked);
    }
}
