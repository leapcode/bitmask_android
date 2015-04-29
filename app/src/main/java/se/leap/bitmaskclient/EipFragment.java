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

    public static final String IS_PENDING = TAG + ".is_pending";
    protected static final String IS_CONNECTED = TAG + ".is_connected";
    public static final String START_ON_BOOT = "start on boot";

    @InjectView(R.id.vpn_Status_Image)
    FabButton vpn_status_image;
    @InjectView(R.id.vpn_main_button)
    Button main_button;

    private static Dashboard dashboard;
    private static EIPReceiver provider_api_receiver;
    private static EipStatus eip_status;
    private boolean wants_to_connect;

    public void onAttach(Activity activity) {
        super.onAttach(activity);

        dashboard = (Dashboard) activity;
        if(provider_api_receiver != null)
            ProviderAPICommand.execute(Bundle.EMPTY, ProviderAPI.DOWNLOAD_EIP_SERVICE, provider_api_receiver);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        eip_status = EipStatus.getInstance();
        eip_status.addObserver(this);
        provider_api_receiver = new EIPReceiver(new Handler());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.eip_service_fragment, container, false);
        ButterKnife.inject(this, view);

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
        super.onSaveInstanceState(outState);
    }

    protected void saveStatus() {
        boolean is_on = eip_status.isConnected() || eip_status.isConnecting();
        Dashboard.preferences.edit().putBoolean(Dashboard.START_ON_BOOT, is_on).commit();
    }

    @OnClick(R.id.vpn_main_button)
    void handleIcon() {
        if (eip_status.isConnected() || eip_status.isConnecting())
            handleSwitchOff();
        else
            handleSwitchOn();

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
            updateIcon();
    }

    private void askPendingStartCancellation() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(dashboard);
        alertBuilder.setTitle(dashboard.getString(R.string.eip_cancel_connect_title))
                .setMessage(dashboard.getString(R.string.eip_cancel_connect_text))
                .setPositiveButton((android.R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        askToStopEIP();
                    }
                })
                .setNegativeButton(dashboard.getString(android.R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    public void startEipFromScratch() {
        wants_to_connect = false;
        eip_status.setConnecting();

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
        eipCommand(Constants.ACTION_STOP_EIP);
    }

    private void askToStopEIP() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(dashboard);
        alertBuilder.setTitle(dashboard.getString(R.string.eip_cancel_connect_title))
                .setMessage(dashboard.getString(R.string.eip_warning_browser_inconsistency))
                .setPositiveButton((android.R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        stopEipIfPossible();
                    }
                })
                .setNegativeButton(dashboard.getString(android.R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
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
        vpn_intent.putExtra(Constants.RECEIVER_TAG, provider_api_receiver);
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
        Context context = dashboard.getApplicationContext();
        String error = eip_status.lastError(5, context);

        if (!error.isEmpty()) {
            dashboard.showLog();
            VoidVpnService.stop();
        }
        updateIcon();
        updateButton();
    }

    private void updateIcon() {
        if (eip_status.isConnected() || eip_status.isConnecting()) {
            if(eip_status.isConnecting()) {
                vpn_status_image.showProgress(true);
                vpn_status_image.setIcon(R.drawable.ic_stat_vpn_empty_halo, R.drawable.ic_stat_vpn_empty_halo);
            } else {
                vpn_status_image.showProgress(false);
                vpn_status_image.setIcon(R.drawable.ic_stat_vpn, R.drawable.ic_stat_vpn);
            }
        } else {
            vpn_status_image.setIcon(R.drawable.ic_stat_vpn_offline, R.drawable.ic_stat_vpn_offline);
            vpn_status_image.showProgress(false);
        }
    }

    private void updateButton() {
        if (eip_status.isConnected() || eip_status.isConnecting()) {
            if(eip_status.isConnecting()) {
                main_button.setText(dashboard.getString(android.R.string.cancel));
            } else {
                main_button.setText(dashboard.getString(R.string.vpn_button_turn_off));
            }
        } else {
            main_button.setText(dashboard.getString(R.string.vpn_button_turn_on));
        }
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
        return provider_api_receiver;
    }
}
