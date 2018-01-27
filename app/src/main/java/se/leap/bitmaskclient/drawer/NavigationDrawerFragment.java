package se.leap.bitmaskclient.drawer;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import se.leap.bitmaskclient.ConfigHelper;
import se.leap.bitmaskclient.ProviderListActivity;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.VpnFragment;
import se.leap.bitmaskclient.fragments.AboutFragment;
import se.leap.bitmaskclient.fragments.LogFragment;

import static android.content.Context.MODE_PRIVATE;
import static se.leap.bitmaskclient.BitmaskApp.getRefWatcher;
import static se.leap.bitmaskclient.Constants.REQUEST_CODE_SWITCH_PROVIDER;
import static se.leap.bitmaskclient.Constants.SHARED_PREFERENCES;

/**
 * Fragment used for managing interactions for and presentation of a navigation drawer.
 * See the <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for a complete explanation of the behaviors implemented here.
 */
public class NavigationDrawerFragment extends Fragment {

    /**
     * Per the design guidelines, you should show the drawer on launch until the user manually
     * expands it. This shared preference tracks this.
     */
    private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private ActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout mDrawerLayout;
    private View mDrawerView;
    private ListView mDrawerSettingsListView;
    private ListView mDrawerAccountsListView;
    private View mFragmentContainerView;
    private ArrayAdapter<String> accountListAdapter;

    private boolean mFromSavedInstanceState;
    private boolean mUserLearnedDrawer;

    private String mTitle;

    private SharedPreferences preferences;

    public NavigationDrawerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Read in the flag indicating whether or not the user has demonstrated awareness of the
        // drawer. See PREF_USER_LEARNED_DRAWER for details.
        preferences = getContext().getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        mUserLearnedDrawer = preferences.getBoolean(PREF_USER_LEARNED_DRAWER, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Indicate that this fragment would like to influence the set of actions in the action bar.
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mDrawerView = inflater.inflate(R.layout.drawer_main, container, false);
        return mDrawerView;
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param fragmentId   The android:id of this fragment in its activity's layout.
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setUp(int fragmentId, DrawerLayout drawerLayout) {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        ActionBar actionBar = activity.getSupportActionBar();

        mDrawerSettingsListView = mDrawerView.findViewById(R.id.settingsList);
        mDrawerSettingsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(parent, position);
            }
        });

        mDrawerSettingsListView.setAdapter(new ArrayAdapter<String>(
                actionBar.getThemedContext(),
                android.R.layout.simple_list_item_activated_1,
                android.R.id.text1,
                new String[]{
                        getString(R.string.switch_provider_menu_option),
                        getString(R.string.log_fragment_title),
                        getString(R.string.about_fragment_title),
                }));

        mDrawerAccountsListView = mDrawerView.findViewById(R.id.accountList);
        mDrawerAccountsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(parent, position);
            }
        });



        accountListAdapter = new ArrayAdapter<>(actionBar.getThemedContext(),
                android.R.layout.simple_list_item_activated_1,
                android.R.id.text1);

        String providerName = ConfigHelper.getProviderName(preferences);
        if (providerName == null) {
            //TODO: ADD A header to the ListView containing a useful message.
            //TODO 2: disable switchProvider
        } else {
            accountListAdapter.add(providerName);
        }

        mDrawerAccountsListView.setAdapter(accountListAdapter);

        mFragmentContainerView = activity.findViewById(fragmentId);
        mDrawerLayout = drawerLayout;

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(
                getActivity(),
                mDrawerLayout,
                (Toolbar) drawerLayout.findViewById(R.id.toolbar),
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (!isAdded()) {
                    return;
                }

                getActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) {
                    return;
                }

                if (!mUserLearnedDrawer) {
                    // The user manually opened the drawer; store this flag to prevent auto-showing
                    // the navigation drawer automatically in the future.
                    mUserLearnedDrawer = true;
                    preferences.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).apply();
                }

                getActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }
        };

        // If the user hasn't 'learned' about the drawer, open it to introduce them to the drawer,
        // per the navigation drawer design guidelines.
        if (!mUserLearnedDrawer && !mFromSavedInstanceState) {
            mDrawerLayout.openDrawer(mFragmentContainerView);
        }

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });
        mDrawerLayout.addDrawerListener(mDrawerToggle);

    }

    private void selectItem(AdapterView<?> list, int position) {
        if (list != null) {
            ((ListView) list).setItemChecked(position, true);
        }
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mFragmentContainerView);
        }
        onNavigationDrawerItemSelected(list, position);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mDrawerLayout != null && isDrawerOpen()) {
            showGlobalContextActionBar();
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        if (item.getItemId() == R.id.action_example) {
            Toast.makeText(getActivity(), "Example action.", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getRefWatcher(getActivity()).watch(this);
    }

    /**
     * Per the navigation drawer design guidelines, updates the action bar to show the global app
     * 'context', rather than just what's in the current screen.
     */
    private void showGlobalContextActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(R.string.app_name);
    }

    private ActionBar getActionBar() {
        return ((AppCompatActivity) getActivity()).getSupportActionBar();
    }

    public void onNavigationDrawerItemSelected(AdapterView<?> parent, int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();
        Fragment fragment = null;

        if (parent == mDrawerAccountsListView) {
            mTitle = getString(R.string.vpn_fragment_title);
            fragment = new VpnFragment();
        } else {
            Log.d("Drawer", String.format("Selected position %d", position));
            switch (position) {
                case 0:
                    // TODO STOP VPN
                    // if (provider.hasEIP()) eip_fragment.stopEipIfPossible();
                    preferences.edit().clear().apply();
                    startActivityForResult(new Intent(getActivity(), ProviderListActivity.class), REQUEST_CODE_SWITCH_PROVIDER);
                    break;
                case 1:
                    mTitle = getString(R.string.log_fragment_title);
                    fragment = new LogFragment();
                    break;
                case 2:
                    mTitle = getString(R.string.about_fragment_title);
                    fragment = new AboutFragment();
                    break;
                default:
                    break;
            }
        }

        if (fragment != null) {
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
        }

        restoreActionBar();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setSubtitle(mTitle);
        }
    }


}
