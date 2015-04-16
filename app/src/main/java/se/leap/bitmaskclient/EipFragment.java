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

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import org.jetbrains.annotations.*;

import java.util.*;

import butterknife.*;
import de.blinkt.openvpn.activities.*;
import mbanje.kurt.fabbutton.*;
import se.leap.bitmaskclient.eip.*;

public class EipFragment extends Fragment implements Observer {

    public static String TAG = EipFragment.class.getSimpleName();

    protected static final String IS_PENDING = TAG + ".is_pending";
    protected static final String IS_CONNECTED = TAG + ".is_connected";
    protected static final String STATUS_MESSAGE = TAG + ".status_message";
    public static final String START_ON_BOOT = "start on boot";

    @InjectView(R.id.eipSwitch)
    Switch eip_switch;
    @InjectView(R.id.status_message)
    TextView status_message;
    @InjectView(R.id.eipProgress)
    ProgressBar progress_bar;
    @InjectView(R.id.vpn_Status_Image)
    FabButton vpn_status_image;

    private static Dashboard dashboard;
    private static EIPReceiver mEIPReceiver;
    private static EipStatus eip_status;
    private boolean wants_to_connect;

    public void onAttach(Activity activity) {
        super.onAttach(activity);

        dashboard = (Dashboard) activity;
        dashboard.providerApiCommand(Bundle.EMPTY, 0, ProviderAPI.DOWNLOAD_EIP_SERVICE);
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
        View view = inflater.inflate(R.layout.eip_service_fragment, container, false);
        ButterKnife.inject(this, view);

        if (eip_status.isConnecting())
            eip_switch.setVisibility(View.VISIBLE);

        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(START_ON_BOOT) && arguments.getBoolean(START_ON_BOOT))
            startEipFromScratch();
        if (savedInstanceState != null) restoreState(savedInstanceState);

        return view;
    }

    private void restoreState(@NotNull Bundle savedInstanceState) {
        if (savedInstanceState.getBoolean(IS_PENDING))
            eip_status.setConnecting();
        else if (savedInstanceState.getBoolean(IS_CONNECTED))
            eip_status.setConnectedOrDisconnected();
        else
            status_message.setText(savedInstanceState.getString(STATUS_MESSAGE));
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
        outState.putString(STATUS_MESSAGE, status_message.getText().toString());
        super.onSaveInstanceState(outState);
    }

    protected void saveStatus() {
        boolean is_on = eip_switch.isChecked();
        Dashboard.preferences.edit().putBoolean(Dashboard.START_ON_BOOT, is_on).commit();
    }

    @OnClick(R.id.vpn_Status_Image)
    void handleIcon() {
        if (eip_status.isConnected() || eip_status.isConnecting())
            handleSwitchOff();
        else
            handleSwitchOn();

        saveStatus();
    }

    void handleNewVpnCertificate() {
        handleSwitch(!eip_switch.isEnabled());
    }

    @OnCheckedChanged(R.id.eipSwitch)
    void handleSwitch(boolean isChecked) {
        if (isChecked)
            handleSwitchOn();
        else
            handleSwitchOff();

        saveStatus();
    }

    private void handleSwitchOn() {
        if (canStartEIP())
            startEipFromScratch();
        else if (canLogInToStartEIP()) {
            wants_to_connect = true;
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
        return isAllowedRegistered && !isLoggedIn && !eip_status.isConnecting() && !eip_status.isConnected();
    }

    private void handleSwitchOff() {
        if (eip_status.isConnecting()) {
            askPendingStartCancellation();
        } else if (eip_status.isConnected()) {
            askToStopEIP();
        } else
            setDisconnectedUI();
    }

    private void askPendingStartCancellation() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(dashboard);
        alertBuilder.setTitle(dashboard.getString(R.string.eip_cancel_connect_title))
                .setMessage(dashboard.getString(R.string.eip_cancel_connect_text))
                .setPositiveButton((R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        askToStopEIP();
                    }
                })
                .setNegativeButton(dashboard.getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        eip_switch.setChecked(true);
                    }
                })
                .show();
    }

    public void startEipFromScratch() {
        wants_to_connect = false;
        eip_status.setConnecting();
        progress_bar.setVisibility(View.VISIBLE);
        eip_switch.setVisibility(View.VISIBLE);
        String status = dashboard.getString(R.string.eip_status_start_pending);
        status_message.setText(status);

        if (!eip_switch.isChecked()) {
            eip_switch.setChecked(true);
        }
        saveStatus();
        eipCommand(Constants.ACTION_START_EIP);
    }

    private void stop() {
        if (eip_status.isConnecting())
            VoidVpnService.stop();
        disconnect();
    }

    private void disconnect() {
        Intent disconnect_vpn = new Intent(dashboard, DisconnectVPN.class);
        dashboard.startActivityForResult(disconnect_vpn, EIP.DISCONNECT);
        eip_status.setDisconnecting();
    }

    protected void stopEipIfPossible() {

        hideProgressBar();

        String message = dashboard.getString(R.string.eip_state_not_connected);
        status_message.setText(message);

        eipCommand(Constants.ACTION_STOP_EIP);
    }

    private void askToStopEIP() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(dashboard);
        alertBuilder.setTitle(dashboard.getString(R.string.eip_cancel_connect_title))
                .setMessage(dashboard.getString(R.string.eip_warning_browser_inconsistency))
                .setPositiveButton((R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        stopEipIfPossible();
                    }
                })
                .setNegativeButton(dashboard.getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        eip_switch.setChecked(true);
                    }
                })
                .show();
    }

    protected void updateEipService() {
        eipCommand(Constants.ACTION_UPDATE_EIP_SERVICE);
    }

    /**
     * Send a command to EIP
     *
     * @param action A valid String constant from EIP class representing an Intent
     *               filter for the EIP class
     */
    private void eipCommand(String action) {
        // TODO validate "action"...how do we get the list of intent-filters for a class via Android API?
        Intent vpn_intent = new Intent(dashboard.getApplicationContext(), EIP.class);
        vpn_intent.setAction(action);
        vpn_intent.putExtra(Constants.RECEIVER_TAG, mEIPReceiver);
        dashboard.startService(vpn_intent);
    }

    @Override
    public void update(Observable observable, Object data) {
        if (observable instanceof EipStatus) {
            eip_status = (EipStatus) observable;
            final EipStatus eip_status = (EipStatus) observable;
            dashboard.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    handleNewState(eip_status);
                }
            });
        }
    }

    private void handleNewState(EipStatus eip_status) {
        if (eip_status.wantsToDisconnect())
            setDisconnectedUI();
        else if (eip_status.isConnecting())
            setInProgressUI(eip_status);
        else if (eip_status.isConnected())
            setConnectedUI();
        else if (eip_status.isDisconnected() && !eip_status.isConnecting())
            setDisconnectedUI();
    }

    private void setConnectedUI() {
        hideProgressBar();
        adjustSwitch();
        status_message.setText(dashboard.getString(R.string.eip_state_connected));
    }

    private void setDisconnectedUI() {
        hideProgressBar();
        adjustSwitch();
        if (eip_status.errorInLast(5, dashboard.getApplicationContext())
                && !status_message.getText().toString().equalsIgnoreCase(dashboard.getString(R.string.eip_state_not_connected))) {
            dashboard.showLog();
            VoidVpnService.stop();
        }
        status_message.setText(dashboard.getString(R.string.eip_state_not_connected));
    }

    private void adjustSwitch() {
        if (eip_status.isConnected() || eip_status.isConnecting()) {
            if (!eip_switch.isChecked()) {
                eip_switch.setChecked(true);
            }
            if(eip_status.isConnecting()) {
                vpn_status_image.showProgress(true);
                vpn_status_image.setIcon(R.drawable.ic_stat_vpn_empty_halo, R.drawable.ic_stat_vpn_empty_halo);
            } else {
                vpn_status_image.showProgress(false);
                vpn_status_image.setIcon(R.drawable.ic_stat_vpn, R.drawable.ic_stat_vpn);
            }
        } else {
            if (eip_switch.isChecked()) {
                eip_switch.setChecked(false);
            }
            vpn_status_image.setIcon(R.drawable.ic_stat_vpn_offline, R.drawable.ic_stat_vpn_offline);
            vpn_status_image.showProgress(false);
        }
    }

    private void setInProgressUI(EipStatus eip_status) {
        int localizedResId = eip_status.getLocalizedResId();
        String logmessage = eip_status.getLogMessage();
        String prefix = dashboard.getString(localizedResId);

        showProgressBar();
        status_message.setText(prefix + " " + logmessage);
        adjustSwitch();
    }

    private void updatingCertificateUI() {
        showProgressBar();
        status_message.setText(getString(R.string.updating_certificate_message));
    }

    private void showProgressBar() {
        if (progress_bar != null)
            progress_bar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        if (progress_bar != null)
            progress_bar.setVisibility(View.GONE);
    }

    protected class EIPReceiver extends ResultReceiver {

        protected EIPReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);

            String request = resultData.getString(Constants.REQUEST_TAG);

            if (request.equals(Constants.ACTION_START_EIP)) {
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        break;
                    case Activity.RESULT_CANCELED:

                        break;
                }
            } else if (request.equals(Constants.ACTION_STOP_EIP)) {
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        stop();
                        break;
                    case Activity.RESULT_CANCELED:
                        break;
                }
            } else if (request.equals(Constants.EIP_NOTIFICATION)) {
                switch (resultCode) {
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
                        updatingCertificateUI();
                        dashboard.downloadVpnCertificate();
                        break;
                }
            } else if (request.equals(Constants.ACTION_UPDATE_EIP_SERVICE)) {
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        if (wants_to_connect)
                            startEipFromScratch();
                        break;
                    case Activity.RESULT_CANCELED:
                        handleNewState(eip_status);
                        break;
                }
            }
        }
    }


    public static EIPReceiver getReceiver() {
        return mEIPReceiver;
    }
}
