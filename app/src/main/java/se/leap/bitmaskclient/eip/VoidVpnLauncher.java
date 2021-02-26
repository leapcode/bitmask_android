package se.leap.bitmaskclient.eip;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;

public class VoidVpnLauncher extends Activity {

    private static final int VPN_USER_PERMISSION = 71;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUp();
    }

    public void setUp() {
        Intent blocking_intent = VpnService.prepare(getApplicationContext()); // stops the VPN connection created by another application.
        if (blocking_intent != null)
            startActivityForResult(blocking_intent, VPN_USER_PERMISSION);
        else {
            EipCommand.startBlockingVPN(getApplicationContext());
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VPN_USER_PERMISSION) {
            if (resultCode == RESULT_OK) {
                EipCommand.launchVoidVPN(getApplicationContext());
            }
        }
        finish();
    }
}
