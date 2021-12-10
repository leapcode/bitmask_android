package se.leap.bitmaskclient.eip;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;

import static se.leap.bitmaskclient.base.models.Constants.EIP_ACTION_PREPARE_VPN;
import static se.leap.bitmaskclient.eip.EipResultBroadcast.tellToReceiverOrBroadcast;

public class VoidVpnLauncher extends Activity {

    private static final int VPN_USER_PERMISSION = 71;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUp();
    }

    public void setUp() {
        Intent blockingIntent = null;
        try {
            blockingIntent = VpnService.prepare(getApplicationContext()); // stops the VPN connection created by another application.
        } catch (NullPointerException npe) {
            tellToReceiverOrBroadcast(this.getApplicationContext(), EIP_ACTION_PREPARE_VPN, RESULT_CANCELED);
            finish();
        }
        if (blockingIntent != null) {
            startActivityForResult(blockingIntent, VPN_USER_PERMISSION);
        }
        else {
            EipCommand.startBlockingVPN(this);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VPN_USER_PERMISSION) {
            if (resultCode == RESULT_OK) {
                EipCommand.launchVoidVPN(this);
            }
        }
        finish();
    }
}
