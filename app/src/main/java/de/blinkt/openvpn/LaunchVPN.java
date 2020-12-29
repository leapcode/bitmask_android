/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;

import java.io.IOException;

import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.Preferences;
import de.blinkt.openvpn.core.VPNLaunchHelper;
import de.blinkt.openvpn.core.VpnStatus;
import se.leap.bitmaskclient.base.MainActivity;
import se.leap.bitmaskclient.R;

import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_PREPARE_VPN;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_PROFILE;
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

    public static final String EXTRA_KEY = "de.blinkt.openvpn.shortcutProfileUUID";
    public static final String EXTRA_NAME = "de.blinkt.openvpn.shortcutProfileName";
    public static final String EXTRA_HIDELOG = "de.blinkt.openvpn.showNoLogWindow";
    public static final String CLEARLOG = "clearlogconnect";


    private static final int START_VPN_PROFILE = 70;
    private static final String TAG = LaunchVPN.class.getName();


    private VpnProfile mSelectedProfile;
    private boolean mhideLog = false;

    private boolean mCmfixed = false;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        startVpnFromIntent();
    }

    protected void startVpnFromIntent() {
        // Resolve the intent

        final Intent intent = getIntent();
        final String action = intent.getAction();

        // If the intent is a request to create a shortcut, we'll do that and exit


        if (Intent.ACTION_MAIN.equals(action)) {
            // Check if we need to clear the log
            if (Preferences.getDefaultSharedPreferences(this).getBoolean(CLEARLOG, true))
                VpnStatus.clearLog();

            // we got called to be the starting point, most likely a shortcut
            mhideLog = intent.getBooleanExtra(EXTRA_HIDELOG, false);
            VpnProfile profileToConnect = (VpnProfile) intent.getExtras().getSerializable(PROVIDER_PROFILE);

            if (profileToConnect == null) {
                VpnStatus.logError(R.string.shortcut_profile_notfound);
                // show Log window to display error
                showLogWindow();
                finish();
            } else {
                mSelectedProfile = profileToConnect;
                launchVPN();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==START_VPN_PROFILE) {
            SharedPreferences prefs = Preferences.getDefaultSharedPreferences(this);
            boolean showLogWindow = prefs.getBoolean("showlogwindow", true);

            if(!mhideLog && showLogWindow)
                showLogWindow();
            VPNLaunchHelper.startOpenVpn(mSelectedProfile, getBaseContext());
            finish();

        } else if (resultCode == Activity.RESULT_CANCELED) {
            // User does not want us to start, so we just vanish
            VpnStatus.updateStateString("USER_VPN_PERMISSION_CANCELLED", "", R.string.state_user_vpn_permission_cancelled,
                    ConnectionStatus.LEVEL_NOTCONNECTED);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                VpnStatus.logError(R.string.nought_alwayson_warning);

            finish();
        }
    }

    void showLogWindow() {

        Intent startLW = new Intent(getBaseContext(), MainActivity.class);
        startLW.putExtra(MainActivity.ACTION_SHOW_LOG_FRAGMENT, true);
        startLW.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(startLW);

    }

    void showConfigErrorDialog(int vpnok) {
        AlertDialog.Builder d = new AlertDialog.Builder(this);
        d.setTitle(R.string.config_error_found);
        d.setMessage(vpnok);
        d.setPositiveButton(android.R.string.ok, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();

            }
        });
        d.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
            setOnDismissListener(d);
        d.show();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void setOnDismissListener(AlertDialog.Builder d) {
        d.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });
    }

    void launchVPN() {
        int vpnok = mSelectedProfile.checkProfile(this);
        if (vpnok != R.string.no_error_found) {
            showConfigErrorDialog(vpnok);
            return;
        }

        Intent intent = null;
        try {
            intent = VpnService.prepare(this.getApplicationContext());
        } catch (NullPointerException npe) {
            tellToReceiverOrBroadcast(this.getApplicationContext(), EIP_ACTION_PREPARE_VPN, RESULT_CANCELED);
            finish();
            return;
        }

        // Check if we want to fix /dev/tun
        SharedPreferences prefs = Preferences.getDefaultSharedPreferences(this);
        boolean usecm9fix = prefs.getBoolean("useCM9Fix", false);
        boolean loadTunModule = prefs.getBoolean("loadTunModule", false);

        if (loadTunModule)
            execeuteSUcmd("insmod /system/lib/modules/tun.ko");

        if (usecm9fix && !mCmfixed) {
            execeuteSUcmd("chown system /dev/tun");
        }

        if (intent != null) {
            VpnStatus.updateStateString("USER_VPN_PERMISSION", "", R.string.state_user_vpn_permission,
                    ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT);
            // Start the query
            try {
                startActivityForResult(intent, START_VPN_PROFILE);
            } catch (ActivityNotFoundException ane) {
                // Shame on you Sony! At least one user reported that
                // an official Sony Xperia Arc S image triggers this exception
                VpnStatus.logError(R.string.no_vpn_support_image);
                showLogWindow();
            }
        } else {
            onActivityResult(START_VPN_PROFILE, Activity.RESULT_OK, null);
        }

    }

    private void execeuteSUcmd(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("su", "-c", command);
            Process p = pb.start();
            int ret = p.waitFor();
            if (ret == 0)
                mCmfixed = true;
        } catch (InterruptedException | IOException e) {
            VpnStatus.logException("SU command", e);
        }
    }
}