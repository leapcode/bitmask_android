/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.StringRes;

import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.VpnStatus;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.eip.EipCommand;

import static se.leap.bitmaskclient.base.models.Constants.BROADCAST_RESULT_KEY;
import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_PREPARE_VPN;
import static se.leap.bitmaskclient.base.models.Constants.EIP_N_CLOSEST_GATEWAY;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_PROFILE;
import static se.leap.bitmaskclient.eip.EIP.ERRORS;
import static se.leap.bitmaskclient.eip.EipResultBroadcast.tellToReceiverOrBroadcast;

/**
 * This Activity actually handles two stages of a launcher shortcut's life cycle.
 * <p/>
 * 1. Your application offers to provide shortcuts to the launcher.  When
 * the user installs a shortcut, an activity within your application
 * generates the actual shortcut and returns it to the launcher, where it
 * is shown to the user as an icon.
 * <p/>
 * 2. Any time the user clicks on an installed shortcut, an intent is sent.
 * Typically this would then be handled as necessary by an activity within
 * your application.
 * <p/>
 * We handle stage 1 (creating a shortcut) by simply sending back the information (in the form
 * of an {@link android.content.Intent} that the launcher will use to create the shortcut.
 * <p/>
 * You can also implement this in an interactive way, by having your activity actually present
 * UI for the user to select the specific nature of the shortcut, such as a contact, picture, URL,
 * media item, or action.
 * <p/>
 * We handle stage 2 (responding to a shortcut) in this sample by simply displaying the contents
 * of the incoming {@link android.content.Intent}.
 * <p/>
 * In a real application, you would probably use the shortcut intent to display specific content
 * or start a particular operation.
 */
public class LaunchVPN extends Activity {

    private static final int START_VPN_PROFILE = 70;
    private static final String TAG = LaunchVPN.class.getName();
    private VpnProfile selectedProfile;
    private int selectedGateway;


    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final Intent intent = getIntent();
        final String action = intent.getAction();
        if (!Intent.ACTION_MAIN.equals(action)) {
            finish();
        }

        VpnProfile profileToConnect = (VpnProfile) intent.getExtras().getSerializable(PROVIDER_PROFILE);
        selectedGateway = intent.getExtras().getInt(EIP_N_CLOSEST_GATEWAY, 0);
        if (profileToConnect == null) {
            showAlertInMainActivity(R.string.shortcut_profile_notfound);
            finish();
        } else {
            selectedProfile = profileToConnect;
        }

        Intent vpnIntent;
        try {
            vpnIntent = VpnService.prepare(this.getApplicationContext());
        } catch (NullPointerException npe) {
            showAlertInMainActivity(R.string.vpn_error_establish);
            finish();
            return;
        }

        if (vpnIntent != null) {
            // we don't have the permission yet to start the VPN

            VpnStatus.updateStateString("USER_VPN_PERMISSION", "", R.string.state_user_vpn_permission,
                    ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT);
            // Start the query
            try {
                startActivityForResult(vpnIntent, START_VPN_PROFILE);
            } catch (ActivityNotFoundException ane) {
                // Shame on you Sony! At least one user reported that
                // an official Sony Xperia Arc S image triggers this exception
                showAlertInMainActivity(R.string.no_vpn_support_image);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==START_VPN_PROFILE && resultCode == Activity.RESULT_OK) {
            EipCommand.launchVPNProfile(getApplicationContext(), selectedProfile, selectedGateway);
            finish();

        } else if (resultCode == Activity.RESULT_CANCELED) {
            // User does not want us to start, so we just vanish
            VpnStatus.updateStateString("USER_VPN_PERMISSION_CANCELLED", "", R.string.state_user_vpn_permission_cancelled,
                    ConnectionStatus.LEVEL_NOTCONNECTED);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                showAlertInMainActivity(R.string.nought_alwayson_warning);
            }

            finish();
        }
    }

    void showAlertInMainActivity(@StringRes int errorString) {
        Bundle result = new Bundle();
        setErrorResult(result, errorString);
        tellToReceiverOrBroadcast(this.getApplicationContext(), EIP_ACTION_PREPARE_VPN, RESULT_CANCELED, result);
    }

    /**
     * helper function to add error to result bundle
     *
     * @param result         - result of an action
     * @param errorMessageId - id of string resource describing the error
     */
    void setErrorResult(Bundle result, @StringRes int errorMessageId) {
        VpnStatus.logError(errorMessageId);
        result.putString(ERRORS, getResources().getString(errorMessageId));
        result.putBoolean(BROADCAST_RESULT_KEY, false);
    }
}