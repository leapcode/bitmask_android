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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.Observable;
import java.util.Observer;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.blinkt.openvpn.core.IOpenVPNServiceInternal;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VpnStatus;
import mbanje.kurt.fabbutton.FabButton;
import se.leap.bitmaskclient.eip.Constants;
import se.leap.bitmaskclient.eip.EIP;
import se.leap.bitmaskclient.eip.EipStatus;
import se.leap.bitmaskclient.eip.VoidVpnService;

import static de.blinkt.openvpn.core.ConnectionStatus.LEVEL_NONETWORK;
import static se.leap.bitmaskclient.eip.Constants.ACTION_STOP_BLOCKING_VPN;

public class VpnFragment extends Fragment implements Observer {

    public static String TAG = VpnFragment.class.getSimpleName();

    public static final String IS_PENDING = TAG + ".is_pending";
    protected static final String IS_CONNECTED = TAG + ".is_connected";
    public static final String START_EIP_ON_BOOT = "start on boot";

    @InjectView(R.id.vpn_status_image)
    FabButton vpn_status_image;
    @InjectView(R.id.vpn_main_button)
    Button main_button;

    private static Dashboard dashboard;
    private static EIPReceiver eip_receiver;
    private static EipStatus eip_status;
    private boolean wants_to_connect;

    private IOpenVPNServiceInternal mService;
    private ServiceConnection openVpnConnection = new ServiceConnection() {



        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {

            mService = IOpenVPNServiceInternal.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
        }

    };

    //FIXME: replace with onAttach(Context context)
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        dashboard = (Dashboard) activity;
        downloadEIPServiceConfig();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        eip_status = EipStatus.getInstance();
        eip_status.addObserver(this);
        eip_receiver = new EIPReceiver(new Handler());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.eip_service_fragment, container, false);
        ButterKnife.inject(this, view);

        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(START_EIP_ON_BOOT) && arguments.getBoolean(START_EIP_ON_BOOT)) {
            startEipFromScratch();
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        //FIXME: avoid race conditions while checking certificate an logging in at about the same time
        //eipCommand(Constants.ACTION_CHECK_CERT_VALIDITY);
        handleNewState();
        bindOpenVpnService();
    }

    @Override
    public void onPause() {
        super.onPause();
        dashboard.unbindService(openVpnConnection);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(IS_PENDING, eip_status.isConnecting());
        outState.putBoolean(IS_CONNECTED, eip_status.isConnected());
        super.onSaveInstanceState(outState);
    }

    private void saveStatus(boolean restartOnBoot) {
        //boolean is_on = eip_status.isConnected() || eip_status.isConnecting() || eip_status.isBlocking();
        Dashboard.preferences.edit().putBoolean(Constants.RESTART_ON_BOOT, restartOnBoot).commit();
    }

    @OnClick(R.id.vpn_main_button)
    void handleIcon() {
        if (eip_status.isConnected() || eip_status.isConnecting())
            handleSwitchOff();
        else
            handleSwitchOn();
        //FIXME ONBOOT IS BROKEN!
        saveStatus(eip_status.isConnected() || eip_status.isConnecting());
    }

    private void handleSwitchOn() {
        if (canStartEIP())
            startEipFromScratch();
        else if (canLogInToStartEIP()) {
            wants_to_connect = true;
            Bundle bundle = new Bundle();
            bundle.putBoolean(IS_PENDING, true);
            dashboard.sessionDialog(bundle);
        } else {
            Log.d(TAG, "WHAT IS GOING ON HERE?!");
            // TODO: implement a fallback: check if vpncertificate was not downloaded properly or give
            // a user feedback. A button that does nothing on click is not a good option
        }
    }

    private boolean canStartEIP() {
        boolean certificateExists = !Dashboard.preferences.getString(Constants.VPN_CERTIFICATE, "").isEmpty();
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
        } else if (eip_status.isBlocking()) {
            //FIXME DEAD CODE
            stop();
        } else {
            updateIcon();
        }
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
        //eip_status.setEipLevel(BLOCKING);

        saveStatus(true);
        eipCommand(Constants.ACTION_START_EIP);
    }

    private void stop() {
        if (eip_status.isBlockingVpnEstablished()) {
            stopBlockingVpn();
        }
        disconnect();
    }

    private void stopBlockingVpn() {
        Log.d(TAG, "stop VoidVpn!");
        Intent stopVoidVpnIntent = new Intent(dashboard, VoidVpnService.class);
        stopVoidVpnIntent.setAction(ACTION_STOP_BLOCKING_VPN);
        dashboard.startService(stopVoidVpnIntent);
    }

    private void disconnect() {
        ProfileManager.setConntectedVpnProfileDisconnected(dashboard);
        if (mService != null) {
            try {
                mService.stopVPN(false);
            } catch (RemoteException e) {
                VpnStatus.logException(e);
            }
        }
    }

    protected void stopEipIfPossible() {
        //FIXME: no need to start a service here!
        eipCommand(Constants.ACTION_STOP_EIP);
    }

    private void downloadEIPServiceConfig() {
        ProviderAPIResultReceiver provider_api_receiver = new ProviderAPIResultReceiver(new Handler(), dashboard);
        if(eip_receiver != null)
            ProviderAPICommand.execute(Bundle.EMPTY, ProviderAPI.DOWNLOAD_EIP_SERVICE, provider_api_receiver);
    }

    protected void askToStopEIP() {
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
        vpn_intent.putExtra(Constants.RECEIVER_TAG, eip_receiver);
        dashboard.startService(vpn_intent);
    }

    @Override
    public void update(Observable observable, Object data) {
        if (observable instanceof EipStatus) {
            eip_status = (EipStatus) observable;
            dashboard.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    handleNewState();
                }
            });
        }
    }

    private void handleNewState() {
        updateIcon();
        updateButton();
    }

    private void updateIcon() {
        if (eip_status.isBlocking()) {
            vpn_status_image.showProgress(false);
            vpn_status_image.setIcon(R.drawable.ic_stat_vpn_blocking, R.drawable.ic_stat_vpn_blocking);
            vpn_status_image.setTag(R.drawable.ic_stat_vpn_blocking);
        } else if (eip_status.isConnecting()) {
            vpn_status_image.showProgress(true);
            vpn_status_image.setIcon(R.drawable.ic_stat_vpn_empty_halo, R.drawable.ic_stat_vpn_empty_halo);
            vpn_status_image.setTag(R.drawable.ic_stat_vpn_empty_halo);
        } else  if (eip_status.isConnected()){
            vpn_status_image.showProgress(false);
            vpn_status_image.setIcon(R.drawable.ic_stat_vpn, R.drawable.ic_stat_vpn);
            vpn_status_image.setTag(R.drawable.ic_stat_vpn);
        } else {
            vpn_status_image.setIcon(R.drawable.ic_stat_vpn_offline, R.drawable.ic_stat_vpn_offline);
            vpn_status_image.setTag(R.drawable.ic_stat_vpn_offline);
            vpn_status_image.showProgress(false);
        }
    }

    private void updateButton() {
        if (eip_status.isConnecting()) {
            main_button.setText(dashboard.getString(android.R.string.cancel));
        } else if (eip_status.isConnected() || isOpenVpnRunningWithoutNetwork()) {
            main_button.setText(dashboard.getString(R.string.vpn_button_turn_off));
        } else {
            main_button.setText(dashboard.getString(R.string.vpn_button_turn_on));
        }
    }

    private boolean isOpenVpnRunningWithoutNetwork() {
        boolean isRunning = false;
        try {
            isRunning = eip_status.getLevel() == LEVEL_NONETWORK &&
                    mService.isVpnRunning();
        } catch (Exception e) {
            //eat me
            e.printStackTrace();
        }

        return isRunning;
    }

    private void bindOpenVpnService() {
        Intent intent = new Intent(dashboard, OpenVPNService.class);
        intent.setAction(OpenVPNService.START_SERVICE);
        dashboard.bindService(intent, openVpnConnection, Context.BIND_AUTO_CREATE);
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
                        handleNewState();
                        break;
                }
            }
        }
    }


    public static EIPReceiver getReceiver() {
        return eip_receiver;
    }
}
