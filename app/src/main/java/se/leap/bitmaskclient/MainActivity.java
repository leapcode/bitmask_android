package se.leap.bitmaskclient;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import se.leap.bitmaskclient.drawer.NavigationDrawerFragment;
import se.leap.bitmaskclient.userstatus.SessionDialog;


public class MainActivity extends AppCompatActivity {

    private static Provider provider = new Provider();
    private static FragmentManagerEnhanced fragmentManager;

    public final static String ACTION_SHOW_VPN_FRAGMENT = "action_show_vpn_fragment";

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        NavigationDrawerFragment navigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        fragmentManager = new FragmentManagerEnhanced(getSupportFragmentManager());
        // Set up the drawer.
        navigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        handleIntentAction(getIntent());
    }

    public static void sessionDialog(Bundle resultData) {
        try {
            FragmentTransaction transaction = fragmentManager.removePreviousFragment(SessionDialog.TAG);
            SessionDialog.getInstance(provider, resultData).show(transaction, SessionDialog.TAG);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntentAction(intent);
    }

    private void handleIntentAction(Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        Fragment fragment = null;

        switch (intent.getAction()) {
            case ACTION_SHOW_VPN_FRAGMENT:
                fragment = new VpnFragment();
                break;
            default:
                break;
        }

        if (fragment != null) {
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
        }
    }


}
