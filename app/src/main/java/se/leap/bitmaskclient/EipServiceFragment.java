package se.leap.bitmaskclient;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.ProviderAPIResultReceiver;
import se.leap.bitmaskclient.ProviderAPIResultReceiver.Receiver;
import se.leap.bitmaskclient.eip.*;

import de.blinkt.openvpn.activities.*;
import de.blinkt.openvpn.core.*;
import android.app.*;
import android.content.*;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.widget.*;
import java.util.*;

public class EipServiceFragment extends Fragment implements Observer, CompoundButton.OnCheckedChangeListener {
	
    protected static final String IS_EIP_PENDING = "is_eip_pending";
    public static final String START_ON_BOOT = "start on boot";
	
    private View eipFragment;
    private static Switch eipSwitch;
    private View eipDetail;
    private TextView status_message;

    private static EIPReceiver mEIPReceiver;
    private static EipStatus eip_status;
    
    public static String TAG = "se.leap.bitmask.EipServiceFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	eip_status = EipStatus.getInstance();
	eip_status.addObserver(this);
	mEIPReceiver = new EIPReceiver(new Handler());

	if (savedInstanceState != null && savedInstanceState.getBoolean(IS_EIP_PENDING))
	    eip_status.setConnecting();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
			     Bundle savedInstanceState) {
		
	eipFragment = inflater.inflate(R.layout.eip_service_fragment, container, false);		
	eipDetail = ((RelativeLayout) eipFragment.findViewById(R.id.eipDetail));
	eipDetail.setVisibility(View.VISIBLE);

	View eipSettings = eipFragment.findViewById(R.id.eipSettings);
	eipSettings.setVisibility(View.GONE); // FIXME too!

	if (eip_status.isConnecting())
	    eipFragment.findViewById(R.id.eipProgress).setVisibility(View.VISIBLE);
		
	status_message = (TextView) eipFragment.findViewById(R.id.status_message);

	eipSwitch = (Switch) eipFragment.findViewById(R.id.eipSwitch);
	eipSwitch.setOnCheckedChangeListener(this);
		
	if(getArguments() != null && getArguments().containsKey(START_ON_BOOT) && getArguments().getBoolean(START_ON_BOOT))
	    startEipFromScratch();
		
	return eipFragment;
    }

    @Override
    public void onResume() {
	super.onResume();
	eipCommand(Constants.ACTION_CHECK_CERT_VALIDITY);
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
	super.onSaveInstanceState(outState);
	outState.putBoolean(IS_EIP_PENDING, eip_status.isConnecting());
    }

    protected void saveEipStatus() {
	boolean eip_is_on = false;
	Log.d(TAG, "saveEipStatus");
	if(eipSwitch.isChecked()) {
	    eip_is_on = true;
	}

	if(getActivity() != null)
	    Dashboard.preferences.edit().putBoolean(Dashboard.START_ON_BOOT, eip_is_on).commit();
    }
    
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
	if (buttonView.equals(eipSwitch)){
	    handleSwitch(isChecked);
	}
    }
    
    private void handleSwitch(boolean isChecked) {
	if(isChecked)
	    handleSwitchOn();
	else
	    handleSwitchOff();
	
	saveEipStatus();
    }

    private void handleSwitchOn() {
	if(canStartEIP())
	    startEipFromScratch();
	else if(canLogInToStartEIP()) {
	    Log.d(TAG, "Can Log In to start EIP");
	    Dashboard dashboard = (Dashboard) getActivity();
	    dashboard.logInDialog(Bundle.EMPTY);
	}	    
    }
    
    private boolean canStartEIP() {
	boolean certificateExists = !Dashboard.preferences.getString(Constants.CERTIFICATE, "").isEmpty();
	boolean isAllowedAnon = Dashboard.preferences.getBoolean(Constants.ALLOWED_ANON, false);
	return (isAllowedAnon || certificateExists) && !eip_status.isConnected();
    }
    
    private boolean canLogInToStartEIP() {
	boolean isAllowedRegistered = Dashboard.preferences.getBoolean(Constants.ALLOWED_REGISTERED, false);
	boolean isLoggedIn = !LeapSRPSession.getToken().isEmpty();
	Log.d(TAG, "Allow registered? " + isAllowedRegistered);
	Log.d(TAG, "Is logged in? " + isLoggedIn);
	return isAllowedRegistered && !isLoggedIn && !eip_status.isConnecting() && !eip_status.isConnected();
    }

    private void handleSwitchOff() {
	if(eip_status.isConnecting()) {
	    askPendingStartCancellation();
	} else if(eip_status.isConnected()) {
	    stopEIP();
	}
    }

    private void askPendingStartCancellation() {	
	AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
	alertBuilder.setTitle(getResources().getString(R.string.eip_cancel_connect_title))
	    .setMessage(getResources().getString(R.string.eip_cancel_connect_text))
	    .setPositiveButton((R.string.yes), new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
			stopEIP();
		    }
		})
	    .setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
			eipSwitch.setChecked(true);
		    }
		})
	    .show();
    }

    public void startEipFromScratch() {
	eip_status.setConnecting();
	eipFragment.findViewById(R.id.eipProgress).setVisibility(View.VISIBLE);
	String status = getResources().getString(R.string.eip_status_start_pending);
	setEipStatus(status);
	
	if(!eipSwitch.isChecked()) {
	    eipSwitch.setChecked(true);
	    saveEipStatus();
	}
	eipCommand(Constants.ACTION_START_EIP);
    }

    protected void stopEIP() {
	View eipProgressBar = getActivity().findViewById(R.id.eipProgress);
	if(eipProgressBar != null)
	    eipProgressBar.setVisibility(View.GONE);
	
	String status = getResources().getString(R.string.eip_state_not_connected);
	setEipStatus(status);
	eipCommand(Constants.ACTION_STOP_EIP);
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
	    vpn_intent.putExtra(Constants.RECEIVER_TAG, mEIPReceiver);
	    getActivity().startService(vpn_intent);
	}
	
    @Override
    public void update (Observable observable, Object data) {
	if(observable instanceof EipStatus) {
	    this.eip_status = (EipStatus) observable;
	    final EipStatus eip_status = (EipStatus) observable;
	    getActivity().runOnUiThread(new Runnable() {
		    @Override
		    public void run() {
			handleNewState(eip_status);
		    }
		});
	}
    }

    private void handleNewState(EipStatus eip_status) {
	final String state = eip_status.getState();
	if(eip_status.wantsToDisconnect())
	    setDisconnectedUI();
	else if (eip_status.isConnected())
	    setConnectedUI();
	else if (eip_status.isDisconnected() && !eip_status.isConnecting())
	    setDisconnectedUI();
	else
	    setInProgressUI(eip_status);
    }

    private void setConnectedUI() {
	hideProgressBar();
	adjustSwitch();
	setEipStatus(getString(R.string.eip_state_connected));
    }

    private void setDisconnectedUI(){
	hideProgressBar();
	adjustSwitch();
	setEipStatus(getString(R.string.eip_state_not_connected));
    }

    private void adjustSwitch() {
	if(eip_status.isConnected()) {
	    if(!eipSwitch.isChecked()) {
		eipSwitch.setChecked(true);
	    }
	} else {
	    if(eipSwitch.isChecked()) {
		eipSwitch.setChecked(false);
	    }
	}
    }

    private void setInProgressUI(EipStatus eip_status) {
	int localizedResId = eip_status.getLocalizedResId();
	String logmessage = eip_status.getLogMessage();
	String prefix = getString(localizedResId);
	
	setEipStatus(prefix + " " + logmessage);
    }

    protected void setEipStatus(String status) {
	if(status_message == null)
	    status_message = (TextView) getActivity().findViewById(R.id.status_message);
	status_message.setText(status);
    }

    private void hideProgressBar() {
	if(getActivity() != null && getActivity().findViewById(R.id.eipProgress) != null)
	    getActivity().findViewById(R.id.eipProgress).setVisibility(View.GONE);
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
		
			String request = resultData.getString(Constants.REQUEST_TAG);
			boolean checked = false;
			
			if (request == Constants.ACTION_IS_EIP_RUNNING) {
				switch (resultCode){
				case Activity.RESULT_OK:
					checked = true;
					break;
				case Activity.RESULT_CANCELED:
					checked = false;
					break;
				}
			} else if (request == Constants.ACTION_START_EIP) {
				switch (resultCode){
				case Activity.RESULT_OK:
				    Log.d(TAG, "Action start eip = Result OK");
					checked = true;
					eipFragment.findViewById(R.id.eipProgress).setVisibility(View.VISIBLE);
					break;
				case Activity.RESULT_CANCELED:
					checked = false;
					eipFragment.findViewById(R.id.eipProgress).setVisibility(View.GONE);
					break;
				}
			} else if (request == Constants.ACTION_STOP_EIP) {
				switch (resultCode){
				case Activity.RESULT_OK:
				    Intent disconnect_vpn = new Intent(getActivity(), DisconnectVPN.class);
				    getActivity().startActivityForResult(disconnect_vpn, 33);
				    eip_status.setDisconnecting();
				    break;
				case Activity.RESULT_CANCELED:
					checked = true;
					break;
				}
			} else if (request == Constants.EIP_NOTIFICATION) {
				switch  (resultCode){
				case Activity.RESULT_OK:
					checked = true;
					break;
				case Activity.RESULT_CANCELED:
					checked = false;
					break;
				}
			} else if (request == Constants.ACTION_CHECK_CERT_VALIDITY) {
			    checked = eipSwitch.isChecked();
			    
			    switch (resultCode) {
			    case Activity.RESULT_OK:
				break;
			    case Activity.RESULT_CANCELED:
				Dashboard dashboard = (Dashboard) getActivity();

				dashboard.showProgressBar();
				String status = getResources().getString(R.string.updating_certificate_message);
				setEipStatus(status);

                if(LeapSRPSession.getToken().isEmpty() && !Dashboard.preferences.getBoolean(Constants.ALLOWED_ANON, false)) {
                        dashboard.logInDialog(Bundle.EMPTY);
                } else {

                    Intent provider_API_command = new Intent(getActivity(), ProviderAPI.class);
                    if (dashboard.providerAPI_result_receiver == null) {
                        dashboard.providerAPI_result_receiver = new ProviderAPIResultReceiver(new Handler());
                        dashboard.providerAPI_result_receiver.setReceiver(dashboard);
                    }

                    provider_API_command.setAction(ProviderAPI.DOWNLOAD_CERTIFICATE);
                    provider_API_command.putExtra(ProviderAPI.RECEIVER_KEY, dashboard.providerAPI_result_receiver);
                    getActivity().startService(provider_API_command);
                }
				break;
			    }
			}
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
    }
}
