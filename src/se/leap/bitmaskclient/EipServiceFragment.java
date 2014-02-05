package se.leap.bitmaskclient;

import se.leap.bitmaskclient.R;
import se.leap.openvpn.LogWindow;
import se.leap.openvpn.OpenVPN;
import se.leap.openvpn.OpenVPN.StateListener;
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
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

public class EipServiceFragment extends Fragment implements StateListener, OnCheckedChangeListener {
	
	private static final String IS_EIP_PENDING = "is_eip_pending";
	
	private View eipFragment;
	private Switch eipSwitch;
	private View eipDetail;
	private TextView eipStatus;

	private boolean eipAutoSwitched = true;
	
	private boolean mEipStartPending = false;

    private EIPReceiver mEIPReceiver;

    
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

			
		eipSwitch.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				eipAutoSwitched = false;
				return false;
			}
		});
		eipSwitch.setOnCheckedChangeListener(this);
		
		
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

		OpenVPN.addStateListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();

		OpenVPN.removeStateListener(this);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(IS_EIP_PENDING, mEipStartPending);
	}
	
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if (buttonView.equals(eipSwitch) && !eipAutoSwitched){
			if (isChecked){
				mEipStartPending = true;
				eipFragment.findViewById(R.id.eipProgress).setVisibility(View.VISIBLE);
				((TextView) eipFragment.findViewById(R.id.eipStatus)).setText(R.string.eip_status_start_pending);
				eipCommand(EIP.ACTION_START_EIP);
			} else {
				if (mEipStartPending){
					AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
					alertBuilder.setTitle(getResources().getString(R.string.eip_cancel_connect_title));
					alertBuilder
					.setMessage(getResources().getString(R.string.eip_cancel_connect_text))
					.setPositiveButton(getResources().getString(R.string.eip_cancel_connect_cancel), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							eipCommand(EIP.ACTION_STOP_EIP);
							mEipStartPending = false;
						}
					})
					.setNegativeButton(getResources().getString(R.string.eip_cancel_connect_false), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							eipAutoSwitched = true;
							eipSwitch.setChecked(true);
							eipAutoSwitched = false;
						}
					})
					.show();
				} else {
					eipCommand(EIP.ACTION_STOP_EIP);
				}
			}
		}
		eipAutoSwitched = true;
	}
	

	
	/**
	 * Send a command to EIP
	 * 
	 * @param action	A valid String constant from EIP class representing an Intent
	 * 					filter for the EIP class 
	 */
	private void eipCommand(String action){
		// TODO validate "action"...how do we get the list of intent-filters for a class via Android API?
		Intent vpnIntent = new Intent(action);
		vpnIntent.putExtra(EIP.RECEIVER_TAG, mEIPReceiver);
		getActivity().startService(vpnIntent);
	}
	
	@Override
	public void updateState(final String state, final String logmessage, final int localizedResId) {
		// Note: "states" are not organized anywhere...collected state strings:
		//		NOPROCESS,NONETWORK,BYTECOUNT,AUTH_FAILED + some parsing thing ( WAIT(?),AUTH,GET_CONFIG,ASSIGN_IP,CONNECTED,SIGINT )
		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (eipStatus != null) {
					boolean switchState = true;
					String statusMessage = "";
					String prefix = getString(localizedResId);
					if (state.equals("CONNECTED")){

						statusMessage = getString(R.string.eip_state_connected);
						getActivity().findViewById(R.id.eipProgress).setVisibility(View.GONE);
						mEipStartPending = false;
					} else if (state.equals("BYTECOUNT")) {
					statusMessage = getString(R.string.eip_state_connected); getActivity().findViewById(R.id.eipProgress).setVisibility(View.GONE);
                                                mEipStartPending = false;
						
					} else if ( (state.equals("NOPROCESS") && !mEipStartPending ) || state.equals("EXITING") || state.equals("FATAL")) {
						statusMessage = getString(R.string.eip_state_not_connected);
						getActivity().findViewById(R.id.eipProgress).setVisibility(View.GONE);
						mEipStartPending = false;
						switchState = false;
					} else if (state.equals("NOPROCESS")){
						statusMessage = logmessage;
					} else if (state.equals("ASSIGN_IP")){ //don't show assigning message in eipStatus
						statusMessage = (String) eipStatus.getText();
					}
					else {
						statusMessage = prefix + " " + logmessage;
					}
					
					eipAutoSwitched = true;
					eipSwitch.setChecked(switchState);
					eipAutoSwitched = false;
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
					checked = true;
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
			}
			
			eipAutoSwitched = true;
			eipSwitch.setChecked(checked);
			eipAutoSwitched = false;
		}
	}
}
