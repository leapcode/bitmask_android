/**
 * Copyright (c) 2018 LEAP Encryption Access Project and contributers
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
package se.leap.bitmaskclient.drawer;


import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
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
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Toast;

import se.leap.bitmaskclient.ConfigHelper;
import se.leap.bitmaskclient.DrawerSettingsAdapter;
import se.leap.bitmaskclient.DrawerSettingsAdapter.DrawerSettingsItem;
import se.leap.bitmaskclient.FragmentManagerEnhanced;
import se.leap.bitmaskclient.fragments.AlwaysOnDialog;
import se.leap.bitmaskclient.EipFragment;
import se.leap.bitmaskclient.Provider;
import se.leap.bitmaskclient.ProviderListActivity;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.fragments.AboutFragment;
import se.leap.bitmaskclient.fragments.LogFragment;

import static android.content.Context.MODE_PRIVATE;
import static se.leap.bitmaskclient.BitmaskApp.getRefWatcher;
import static se.leap.bitmaskclient.ConfigHelper.getSaveBattery;
import static se.leap.bitmaskclient.ConfigHelper.getShowAlwaysOnDialog;
import static se.leap.bitmaskclient.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.Constants.REQUEST_CODE_SWITCH_PROVIDER;
import static se.leap.bitmaskclient.Constants.SHARED_PREFERENCES;
import static se.leap.bitmaskclient.DrawerSettingsAdapter.ABOUT;
import static se.leap.bitmaskclient.DrawerSettingsAdapter.ALWAYS_ON;
import static se.leap.bitmaskclient.DrawerSettingsAdapter.BATTERY_SAVER;
import static se.leap.bitmaskclient.DrawerSettingsAdapter.DrawerSettingsItem.getSimpleTextInstance;
import static se.leap.bitmaskclient.DrawerSettingsAdapter.DrawerSettingsItem.getSwitchInstance;
import static se.leap.bitmaskclient.DrawerSettingsAdapter.LOG;
import static se.leap.bitmaskclient.DrawerSettingsAdapter.SWITCH_PROVIDER;
import static se.leap.bitmaskclient.R.string.about_fragment_title;
import static se.leap.bitmaskclient.R.string.log_fragment_title;
import static se.leap.bitmaskclient.R.string.switch_provider_menu_option;

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
    private DrawerSettingsAdapter settingsListAdapter;
    private Toolbar mToolbar;

    private boolean mFromSavedInstanceState;
    private boolean mUserLearnedDrawer;

    private String mTitle;

    private SharedPreferences preferences;

    private final static String KEY_SHOW_ENABLE_EXPERIMENTAL_FEATURE = "KEY_SHOW_ENABLE_EXPERIMENTAL_FEATURE";
    private boolean showEnableExperimentalFeature = false;
    AlertDialog alertDialog;

    public NavigationDrawerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Read in the flag indicating whether or not the user has demonstrated awareness of the
        // drawer. See PREF_USER_LEARNED_DRAWER for details.
        preferences = getContext().getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        mUserLearnedDrawer = preferences.getBoolean(PREF_USER_LEARNED_DRAWER, false);
        if (savedInstanceState != null) {
            mFromSavedInstanceState = true;
        }
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
        mDrawerView = inflater.inflate(R.layout.f_drawer_main, container, false);
        restoreFromSavedInstance(savedInstanceState);
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
        mDrawerLayout = drawerLayout;

        mToolbar = mDrawerLayout.findViewById(R.id.toolbar);
        activity.setSupportActionBar(mToolbar);

        final ActionBar actionBar = activity.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        mDrawerSettingsListView = mDrawerView.findViewById(R.id.settingsList);
        mDrawerSettingsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(parent, position);
            }
        });
        settingsListAdapter = new DrawerSettingsAdapter(getLayoutInflater());
        if (getContext() != null) {
            settingsListAdapter.addItem(getSwitchInstance(getString(R.string.save_battery),
                    getSaveBattery(getContext()),
                    BATTERY_SAVER,
                    new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean newStateIsChecked) {
                            onSwitchItemSelected(BATTERY_SAVER, newStateIsChecked);
                        }
                    }));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            settingsListAdapter.addItem(getSimpleTextInstance(getString(R.string.always_on_vpn), ALWAYS_ON));
        }
        settingsListAdapter.addItem(getSimpleTextInstance(getString(switch_provider_menu_option), SWITCH_PROVIDER));
        settingsListAdapter.addItem(getSimpleTextInstance(getString(log_fragment_title), LOG));
        settingsListAdapter.addItem(getSimpleTextInstance(getString(about_fragment_title), ABOUT));

        mDrawerSettingsListView.setAdapter(settingsListAdapter);
        mDrawerAccountsListView = mDrawerView.findViewById(R.id.accountList);
        mDrawerAccountsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(parent, position);
            }
        });

        accountListAdapter = new ArrayAdapter<>(actionBar.getThemedContext(),
                R.layout.v_single_list_item,
                android.R.id.text1);

        createListAdapterData();

        mDrawerAccountsListView.setAdapter(accountListAdapter);

        mFragmentContainerView = activity.findViewById(fragmentId);

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

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
                    mToolbar.setNavigationIcon(R.drawable.ic_menu_default);
                }

                getActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }
        };

        Handler navigationDrawerHandler = new Handler();
        // If the user hasn't 'learned' about the drawer, open it to introduce them to the drawer,
        // per the navigation drawer design guidelines.
        if (!mUserLearnedDrawer && !mFromSavedInstanceState) {
            mDrawerLayout.openDrawer(mFragmentContainerView, false);
            navigationDrawerHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDrawerLayout.closeDrawer(mFragmentContainerView, true);
                }
            }, 1500);
            navigationDrawerHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mToolbar.setNavigationIcon(R.drawable.ic_menu_color_point);
                    mToolbar.playSoundEffect(android.view.SoundEffectConstants.CLICK);
                }
            }, 3000);

        } else if (!mUserLearnedDrawer) {
            navigationDrawerHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mToolbar.setNavigationIcon(R.drawable.ic_menu_color_point);
                    mToolbar.playSoundEffect(android.view.SoundEffectConstants.CLICK);
                }
            }, 1500);
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
        onTextItemSelected(list, position);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (showEnableExperimentalFeature) {
            outState.putBoolean(KEY_SHOW_ENABLE_EXPERIMENTAL_FEATURE, true);
        }
    }

    private void restoreFromSavedInstance(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_SHOW_ENABLE_EXPERIMENTAL_FEATURE)) {
            showExperimentalFeatureAlert();
        }
    }

    private void showExperimentalFeatureAlert() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        try {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
            showEnableExperimentalFeature = true;
            alertDialog = alertBuilder
                    .setTitle(activity.getString(R.string.save_battery))
                    .setMessage(activity.getString(R.string.save_battery_message))
                    .setPositiveButton((android.R.string.yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            DrawerSettingsItem item = settingsListAdapter.getDrawerItem(BATTERY_SAVER);
                            item.setChecked(true);
                            settingsListAdapter.notifyDataSetChanged();
                            ConfigHelper.saveBattery(getContext(), item.isChecked());
                        }
                    })
                    .setNegativeButton(activity.getString(android.R.string.no), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            disableSwitch(BATTERY_SAVER);
                        }
                    }).setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            showEnableExperimentalFeature = false;
                        }
                    }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            disableSwitch(BATTERY_SAVER);
                        }
                    }).show();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void showAlwaysOnDialog() {
        try {

            FragmentTransaction fragmentTransaction = new FragmentManagerEnhanced(
                    getActivity().getSupportFragmentManager()).removePreviousFragment(
                    AlwaysOnDialog.TAG);
            DialogFragment newFragment = new AlwaysOnDialog();
            newFragment.show(fragmentTransaction, AlwaysOnDialog.TAG);
        } catch (IllegalStateException | NullPointerException e) {
            e.printStackTrace();
        }

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

    private void onSwitchItemSelected(int elementType, boolean newStateIsChecked) {
        switch (elementType) {
            case BATTERY_SAVER:
                if (ConfigHelper.getSaveBattery(getContext()) == newStateIsChecked) {
                    //initial ui setup, ignore
                    return;
                }
                if (newStateIsChecked) {
                    showExperimentalFeatureAlert();
                } else {
                    ConfigHelper.saveBattery(this.getContext(), false);
                    disableSwitch(BATTERY_SAVER);
                }
                break;
            default:
                break;
        }
    }

    private void disableSwitch(int elementType) {
        DrawerSettingsItem item = settingsListAdapter.getDrawerItem(elementType);
        item.setChecked(false);
        settingsListAdapter.notifyDataSetChanged();
    }

    public void onTextItemSelected(AdapterView<?> parent, int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();
        Fragment fragment = null;
        String fragmentTag = null;

        if (parent == mDrawerAccountsListView) {
            mTitle = getString(R.string.vpn_fragment_title);
            fragment = new EipFragment();
            fragmentTag = EipFragment.TAG;
            Bundle arguments = new Bundle();
            Provider currentProvider = ConfigHelper.getSavedProviderFromSharedPreferences(preferences);
            arguments.putParcelable(PROVIDER_KEY, currentProvider);
            fragment.setArguments(arguments);
        } else {
            Log.d("Drawer", String.format("Selected position %d", position));
            DrawerSettingsItem settingsItem = settingsListAdapter.getItem(position);
            switch (settingsItem.getItemType()) {
                case SWITCH_PROVIDER:
                    getActivity().startActivityForResult(new Intent(getActivity(), ProviderListActivity.class), REQUEST_CODE_SWITCH_PROVIDER);
                    break;
                case LOG:
                    mTitle = getString(log_fragment_title);
                    fragment = new LogFragment();
                    break;
                case ABOUT:
                    mTitle = getString(about_fragment_title);
                    fragment = new AboutFragment();
                    break;
                case ALWAYS_ON:
                    if (getShowAlwaysOnDialog(getContext())) {
                        showAlwaysOnDialog();
                    } else {
                        Intent intent = new Intent("android.net.vpn.SETTINGS");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                    break;
                default:
                    break;
            }
        }

        if (fragment != null) {
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment, fragmentTag)
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


    public void refresh() {
        createListAdapterData();
        accountListAdapter.notifyDataSetChanged();
        mDrawerAccountsListView.setAdapter(accountListAdapter);
    }

    private void createListAdapterData() {
        accountListAdapter.clear();
        String providerName = ConfigHelper.getProviderName(preferences);
        if (providerName == null) {
            //TODO: ADD A header to the ListView containing a useful message.
            //TODO 2: disable switchProvider
        } else {
            accountListAdapter.add(providerName);
        }
    }

}
