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
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Observable;
import java.util.Observer;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnCheckedChanged;
import de.blinkt.openvpn.activities.DisconnectVPN;
import se.leap.bitmaskclient.eip.Constants;
import se.leap.bitmaskclient.eip.EIP;
import se.leap.bitmaskclient.eip.EipStatus;
import se.leap.bitmaskclient.eip.VoidVpnService;

public class EipFragment extends Fragment implements Observer {
	
    public static String TAG = EipFragment.class.getSimpleName();

    protected static final String IS_PENDING = TAG + ".is_pending";
    protected static final String IS_CONNECTED = TAG + ".is_connected";
    protected static final String STATUS_MESSAGE = TAG + ".status_message";
    public static final String START_ON_BOOT = "start on boot";

    private View view;
    @InjectView(R.id.eipSwitch)
    Switch eip_switch;
    @InjectView(R.id.status_message)
    TextView status_message;
    @InjectView(R.id.eipProgress)
    ProgressBar progress_bar;

    private static Activity parent_activity;
    private static EIPReceiver mEIPReceiver;
    private static EipStatus eip_status;
    private boolean is_starting_to_connect;

    @Override
    public void onAttach(Activity activity) {
	super.onAttach(activity);
	parent_activity = activity;

        Dashboard dashboard = (Dashboard) parent_activity;
        Intent provider_API_command = dashboard.prepareProviderAPICommand(Bundle.EMPTY, ProviderAPI.DOWNLOAD_EIP_SERVICE);
        parent_activity.startService(provider_API_command);
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
	view = inflater.inflate(R.layout.eip_service_fragment, container, false);
        ButterKnife.inject(this, view);

	if (eip_status.isConnecting())
	    eip_switch.setVisibility(View.VISIBLE);

	Log.d(TAG, "onCreateView, eip_switch is checked? " + eip_switch.isChecked());

        Bundle arguments = getArguments();
	if(arguments != null && arguments.containsKey(START_ON_BOOT) && arguments.getBoolean(START_ON_BOOT))
	    startEipFromScratch();

        if (savedInstanceState != null) {
            status_message.setText(savedInstanceState.getString(STATUS_MESSAGE));
            if(savedInstanceState.getBoolean(IS_PENDING))
                eip_status.setConnecting();
            else if(savedInstanceState.getBoolean(IS_CONNECTED)) {
                eip_status.setConnectedOrDisconnected();
            }
        }
	return view;
    }

    @Override
    public void onResume() {
	super.onResume();
	eipCommand(Constants.ACTION_CHECK_CERT_VALIDITY);
	handleNewState(eip_status);
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
	outState.putBoolean(IS_PENDING, eip_status.isConnecting());
	outState.putBoolean(IS_CONNECTED, eip_status.isConnected());
	Log.d(TAG, "status message onSaveInstanceState = " + status_message.getText().toString());
	outState.putString(STATUS_MESSAGE, status_message.getText().toString());
	super.onSaveInstanceState(outState);
    }

    protected void saveStatus() {
	boolean is_on = eip_switch.isChecked();
	Log.d(TAG, "saveStatus: is_on = " + is_on);
	Dashboard.preferences.edit().putBoolean(Dashboard.START_ON_BOOT, is_on).commit();
    }

    @OnCheckedChanged(R.id.eipSwitch)
    void handleSwitch(boolean isChecked) {
	if(isChecked)
	    handleSwitchOn();
	else
	    handleSwitchOff();
	
	saveStatus();
    }

    private void handleSwitchOn() {
	if(canStartEIP())
	    startEipFromScratch();
	else if(canLogInToStartEIP()) {
	    Log.d(TAG, "Can Log In to start EIP");
	    Dashboard dashboard = (Dashboard) parent_activity;
        Bundle bundle = new Bundle();
        bundle.putBoolean(IS_PENDING, true);
	    dashboard.sessionDialog(bundle);
	}	    
    }
    
    private boolean canStartEIP() {
	boolean certificateExists = !Dashboard.preferences.getString(Constants.CERTIFICATE, "").isEmpty();
	boolean isAllowedAnon = Dashboard.preferences.getBoolean(Constants.ALLOWED_ANON, false);
	return (isAllowedAnon || certificateExists) && !eip_status.isConnected() && !eip_status.isConnecting();
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
	    askToStopEIP();
	}
    }

    private void askPendingStartCancellation() {	
	AlertDialog.Builder alertBuilder = new AlertDialog.Builder(parent_activity);
	alertBuilder.setTitle(parent_activity.getString(R.string.eip_cancel_connect_title))
	    .setMessage(parent_activity.getString(R.string.eip_cancel_connect_text))
	    .setPositiveButton((R.string.yes), new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
			askToStopEIP();
		    }
		})
	    .setNegativeButton(parent_activity.getString(R.string.no), new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
			eip_switch.setChecked(true);
		    }
		})
	    .show();
    }

    public void startEipFromScratch() {
        is_starting_to_connect = true;
        progress_bar.setVisibility(View.VISIBLE);
	eip_switch.setVisibility(View.VISIBLE);
	String status = parent_activity.getString(R.string.eip_status_start_pending);
	status_message.setText(status);
	
	if(!eip_switch.isChecked()) {
	    eip_switch.setChecked(true);
	}
        saveStatus();
	eipCommand(Constants.ACTION_START_EIP);
    }

    private void stopEIP() {
	if(eip_status.isConnecting())
	    VoidVpnService.stop();
	Intent disconnect_vpn = new Intent(parent_activity, DisconnectVPN.class);
	parent_activity.startActivityForResult(disconnect_vpn, EIP.DISCONNECT);
	eip_status.setDisconnecting();
    }

    protected void askToStopEIP() {
        hideProgressBar();

	String status = parent_activity.getString(R.string.eip_state_not_connected);
	status_message.setText(status);

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
    else if(eip_status.isConnecting() || is_starting_to_connect)
        setInProgressUI(eip_status);
	else if (eip_status.isConnected())
	    setConnectedUI();
	else if (eip_status.isDisconnected() && !eip_status.isConnecting())
	    setDisconnectedUI();
    }

    private void setConnectedUI() {
	hideProgressBar();
	Log.d(TAG, "setConnectedUi? " + eip_status.isConnected());
	adjustSwitch();
    is_starting_to_connect = false;
	status_message.setText(parent_activity.getString(R.string.eip_state_connected));
    }

    private void setDisconnectedUI(){
	hideProgressBar();
	adjustSwitch();
	status_message.setText(parent_activity.getString(R.string.eip_state_not_connected));
    }

    private void adjustSwitch() {
	if(eip_status.isConnected() || eip_status.isConnecting() || is_starting_to_connect) {
	    Log.d(TAG, "adjustSwitch, isConnected || isConnecting, is checked");
	    if(!eip_switch.isChecked()) {
		eip_switch.setChecked(true);
	    }
	} else {
	    Log.d(TAG, "adjustSwitch, !isConnected && !isConnecting? " + eip_status.toString());

	    if(eip_switch.isChecked()) {
		eip_switch.setChecked(false);
	    }
	}
    }

    private void setInProgressUI(EipStatus eip_status) {
	int localizedResId = eip_status.getLocalizedResId();
	String logmessage = eip_status.getLogMessage();
	String prefix = parent_activity.getString(localizedResId);

	status_message.setText(prefix + " " + logmessage);
        is_starting_to_connect = false;
	adjustSwitch();
    }

    private void hideProgressBar() {
	if(progress_bar != null)
	    progress_bar.setVisibility(View.GONE);
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
		    break;
		case Activity.RESULT_CANCELED:
		    
		    break;
		}
	    } else if (request.equals(Constants.ACTION_STOP_EIP)) {
		switch (resultCode){
		case Activity.RESULT_OK:
		    stopEIP();
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

		    progress_bar.setVisibility(View.VISIBLE);
		    status_message.setText(getString(R.string.updating_certificate_message));
		    if(LeapSRPSession.getToken().isEmpty() && !Dashboard.preferences.getBoolean(Constants.ALLOWED_ANON, false)) {
			dashboard.sessionDialog(Bundle.EMPTY);
		    } else {
			Intent provider_API_command = dashboard.prepareProviderAPICommand(Bundle.EMPTY, ProviderAPI.DOWNLOAD_CERTIFICATE);
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
