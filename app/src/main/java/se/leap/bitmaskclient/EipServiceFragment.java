package se.leap.bitmaskclient;

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
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Observable;
import java.util.Observer;

import de.blinkt.openvpn.activities.DisconnectVPN;
import se.leap.bitmaskclient.eip.Constants;
import se.leap.bitmaskclient.eip.EIP;
import se.leap.bitmaskclient.eip.EipStatus;

public class EipServiceFragment extends Fragment implements Observer, CompoundButton.OnCheckedChangeListener {
	
    public static String TAG = "se.leap.bitmask.EipServiceFragment";

    protected static final String IS_PENDING = TAG + ".is_pending";
    protected static final String IS_CONNECTED = TAG + ".is_connected";
    protected static final String STATUS_MESSAGE = TAG + ".status_message";
    public static final String START_ON_BOOT = "start on boot";

    private View eipFragment;
    private static Switch eipSwitch;
    private TextView status_message;

    private static Activity parent_activity;
    private static EIPReceiver mEIPReceiver;
    private static EipStatus eip_status;

    @Override
    public void onAttach(Activity activity) {
	super.onAttach(activity);
	parent_activity = activity;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	eip_status = EipStatus.getInstance();
	eip_status.addObserver(this);
	mEIPReceiver = new EIPReceiver(new Handler());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
	eipFragment = inflater.inflate(R.layout.eip_service_fragment, container, false);
    View eipDetail = eipFragment.findViewById(R.id.eipDetail);
	eipDetail.setVisibility(View.VISIBLE);

	View eipSettings = eipFragment.findViewById(R.id.eipSettings);
	eipSettings.setVisibility(View.GONE); // FIXME too!

	if (eip_status.isConnecting())
	    eipFragment.findViewById(R.id.eipProgress).setVisibility(View.VISIBLE);
		
	status_message = (TextView) eipFragment.findViewById(R.id.status_message);

	eipSwitch = (Switch) eipFragment.findViewById(R.id.eipSwitch);
	Log.d(TAG, "onCreateView, eipSwitch is checked? " + eipSwitch.isChecked());
	eipSwitch.setOnCheckedChangeListener(this);
		
	if(getArguments() != null && getArguments().containsKey(START_ON_BOOT) && getArguments().getBoolean(START_ON_BOOT))
	    startEipFromScratch();

        if (savedInstanceState != null) {
            setStatusMessage(savedInstanceState.getString(STATUS_MESSAGE));
            if(savedInstanceState.getBoolean(IS_PENDING))
                eip_status.setConnecting();
            else if(savedInstanceState.getBoolean(IS_CONNECTED)) {
                eip_status.setConnectedOrDisconnected();
            }
        }
	return eipFragment;
    }

    @Override
    public void onResume() {
	super.onResume();
	eipCommand(Constants.ACTION_CHECK_CERT_VALIDITY);
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
	outState.putBoolean(IS_PENDING, eip_status.isConnecting());
	outState.putBoolean(IS_CONNECTED, eip_status.isConnected());
	Log.d(TAG, "status message onSaveInstanceState = " + status_message.getText().toString());
	outState.putString(STATUS_MESSAGE, status_message.getText().toString());
	super.onSaveInstanceState(outState);
    }

    protected void saveEipStatus() {
	boolean eip_is_on = false;
	Log.d(TAG, "saveEipStatus");
	if(eipSwitch.isChecked()) {
	    eip_is_on = true;
	}

	if(parent_activity != null)
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
	    Dashboard dashboard = (Dashboard) parent_activity;
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
	AlertDialog.Builder alertBuilder = new AlertDialog.Builder(parent_activity);
	alertBuilder.setTitle(parent_activity.getString(R.string.eip_cancel_connect_title))
	    .setMessage(parent_activity.getString(R.string.eip_cancel_connect_text))
	    .setPositiveButton((R.string.yes), new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
			stopEIP();
		    }
		})
	    .setNegativeButton(parent_activity.getString(R.string.no), new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
			eipSwitch.setChecked(true);
		    }
		})
	    .show();
    }

    public void startEipFromScratch() {
	eipFragment.findViewById(R.id.eipProgress).setVisibility(View.VISIBLE);
	String status = parent_activity.getString(R.string.eip_status_start_pending);
	setStatusMessage(status);
	
	if(!eipSwitch.isChecked()) {
	    eipSwitch.setChecked(true);
	    saveEipStatus();
	}
	eipCommand(Constants.ACTION_START_EIP);
    }

    protected void stopEIP() {
	View eipProgressBar = parent_activity.findViewById(R.id.eipProgress);
	if(eipProgressBar != null)
	    eipProgressBar.setVisibility(View.GONE);
	
	String status = parent_activity.getString(R.string.eip_state_not_connected);
	setStatusMessage(status);
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
	Intent vpn_intent = new Intent(parent_activity.getApplicationContext(), EIP.class);
	vpn_intent.setAction(action);
	vpn_intent.putExtra(Constants.RECEIVER_TAG, mEIPReceiver);
	parent_activity.startService(vpn_intent);
    }
	
    @Override
    public void update (Observable observable, Object data) {
	Log.d(TAG, "handleNewState?");	
	if(observable instanceof EipStatus) {
	    eip_status = (EipStatus) observable;
	    final EipStatus eip_status = (EipStatus) observable;
	    parent_activity.runOnUiThread(new Runnable() {
	    	    @Override
	    	    public void run() {
			handleNewState(eip_status);
		    }
		});
	}
    }

    private void handleNewState(EipStatus eip_status) {
	Log.d(TAG, "handleNewState: " + eip_status.toString());
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
	Log.d(TAG, "setConnectedUi? " + eip_status.isConnected());
	adjustSwitch();
	setStatusMessage(parent_activity.getString(R.string.eip_state_connected));
    }

    private void setDisconnectedUI(){
	hideProgressBar();
	adjustSwitch();
	setStatusMessage(parent_activity.getString(R.string.eip_state_not_connected));
    }

    private void adjustSwitch() {
	if(eip_status.isConnected() || eip_status.isConnecting()) {
	    Log.d(TAG, "adjustSwitch, isConnected || isConnecting, is checked? " + eipSwitch.isChecked());
	    if(!eipSwitch.isChecked()) {
		eipSwitch.setChecked(true);
	    }
	} else {
	    Log.d(TAG, "adjustSwitch, !isConnected && !isConnecting? " + eip_status.toString());
	    
	    if(eipSwitch.isChecked()) {
		eipSwitch.setChecked(false);
	    }
	}
    }

    private void setInProgressUI(EipStatus eip_status) {
	int localizedResId = eip_status.getLocalizedResId();
	String logmessage = eip_status.getLogMessage();
	String prefix = parent_activity.getString(localizedResId);
	
	setStatusMessage(prefix + " " + logmessage);
	adjustSwitch();
    }

    protected void setStatusMessage(String status) {
	if(status_message == null)
	    status_message = (TextView) parent_activity.findViewById(R.id.status_message);
	status_message.setText(status);
    }

    private void hideProgressBar() {
	if(parent_activity != null && parent_activity.findViewById(R.id.eipProgress) != null)
	    parent_activity.findViewById(R.id.eipProgress).setVisibility(View.GONE);
    }

    protected class EIPReceiver extends ResultReceiver {
		
	protected EIPReceiver(Handler handler){
	    super(handler);
	}

	@Override
	protected void onReceiveResult(int resultCode, Bundle resultData) {
	    super.onReceiveResult(resultCode, resultData);
		
	    String request = resultData.getString(Constants.REQUEST_TAG);

	    if (request.equals(Constants.ACTION_START_EIP)) {
		switch (resultCode){
		case Activity.RESULT_OK:
		    Log.d(TAG, "Action start eip = Result OK");
		    eipFragment.findViewById(R.id.eipProgress).setVisibility(View.VISIBLE);
		    break;
		case Activity.RESULT_CANCELED:
		    eipFragment.findViewById(R.id.eipProgress).setVisibility(View.GONE);
		    break;
		}
	    } else if (request.equals(Constants.ACTION_STOP_EIP)) {
		switch (resultCode){
		case Activity.RESULT_OK:
		    Intent disconnect_vpn = new Intent(parent_activity, DisconnectVPN.class);
		    parent_activity.startActivityForResult(disconnect_vpn, EIP.DISCONNECT);
		    eip_status.setDisconnecting();
		    break;
		case Activity.RESULT_CANCELED:
		    break;
		}
	    } else if (request.equals(Constants.EIP_NOTIFICATION)) {
		switch  (resultCode){
		case Activity.RESULT_OK:
		    break;
		case Activity.RESULT_CANCELED:
		    break;
		}
	    } else if (request.equals(Constants.ACTION_CHECK_CERT_VALIDITY)) {
		switch (resultCode) {
		case Activity.RESULT_OK:
		    break;
		case Activity.RESULT_CANCELED:
		    Dashboard dashboard = (Dashboard) parent_activity;

		    dashboard.showProgressBar();
		    String status = parent_activity.getString(R.string.updating_certificate_message);
		    setStatusMessage(status);
		    if(LeapSRPSession.getToken().isEmpty() && !Dashboard.preferences.getBoolean(Constants.ALLOWED_ANON, false)) {
			dashboard.logInDialog(Bundle.EMPTY);
		    } else {	
			Intent provider_API_command = new Intent(parent_activity, ProviderAPI.class);
			if(dashboard.providerAPI_result_receiver == null) {
			    dashboard.providerAPI_result_receiver = new ProviderAPIResultReceiver(new Handler());
			    dashboard.providerAPI_result_receiver.setReceiver(dashboard);
			}
				
			provider_API_command.setAction(ProviderAPI.DOWNLOAD_CERTIFICATE);
			provider_API_command.putExtra(ProviderAPI.RECEIVER_KEY, dashboard.providerAPI_result_receiver);
			parent_activity.startService(provider_API_command);
		    }
		    break;
		}
	    }
	}
    }
    

    public static EIPReceiver getReceiver() {
	return mEIPReceiver;
    }
}
