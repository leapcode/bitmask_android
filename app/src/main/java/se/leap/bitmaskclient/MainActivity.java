package se.leap.bitmaskclient;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;

import se.leap.bitmaskclient.fragments.LogFragment;
import se.leap.bitmaskclient.userstatus.SessionDialog;


public class MainActivity extends AppCompatActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    private static Provider provider = new Provider();
    private static FragmentManagerEnhanced fragmentManager;
    private SharedPreferences preferences;


    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        fragmentManager = new FragmentManagerEnhanced(getSupportFragmentManager());
        preferences = getSharedPreferences(Constants.SHARED_PREFERENCES, MODE_PRIVATE);
        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = null;
        switch (position) {
            case 1:
                // TODO STOP VPN
                // if (provider.hasEIP()) eip_fragment.stopEipIfPossible();
                preferences.edit().clear().apply();
                startActivityForResult(new Intent(this, ConfigurationWizard.class), Constants.REQUEST_CODE_SWITCH_PROVIDER);
                break;
            case 2:
                fragment = new LogFragment();
                break;
            default:
                fragment = new VpnFragment();
                break;
        }
        if (fragment != null) {
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
        }
        onSectionAttached(position);
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.switch_provider_menu_option);
                break;
            case 2:
                mTitle = getString(R.string.log_fragment_title);
                break;
            default:
                mTitle = getString(R.string.vpn_fragment_title);
                break;
        }
        restoreActionBar();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setSubtitle(mTitle);
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance() {
            PlaceholderFragment fragment = new PlaceholderFragment();
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);
        }
    }

    public static void sessionDialog(Bundle resultData) {
        try {
            FragmentTransaction transaction = fragmentManager.removePreviousFragment(SessionDialog.TAG);
            SessionDialog.getInstance(provider, resultData).show(transaction, SessionDialog.TAG);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

}
