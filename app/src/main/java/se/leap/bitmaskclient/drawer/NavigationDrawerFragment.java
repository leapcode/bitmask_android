/**
 * Copyright (c) 2019 LEAP Encryption Access Project and contributers
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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.Set;

import de.blinkt.openvpn.core.VpnStatus;
import se.leap.bitmaskclient.EipFragment;
import se.leap.bitmaskclient.FragmentManagerEnhanced;
import se.leap.bitmaskclient.MainActivity;
import se.leap.bitmaskclient.Provider;
import se.leap.bitmaskclient.ProviderListActivity;
import se.leap.bitmaskclient.ProviderObservable;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.eip.EipCommand;
import se.leap.bitmaskclient.fragments.AboutFragment;
import se.leap.bitmaskclient.fragments.AlwaysOnDialog;
import se.leap.bitmaskclient.fragments.ExcludeAppsFragment;
import se.leap.bitmaskclient.fragments.LogFragment;
import se.leap.bitmaskclient.utils.PreferenceHelper;
import se.leap.bitmaskclient.views.IconSwitchEntry;
import se.leap.bitmaskclient.views.IconTextEntry;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static se.leap.bitmaskclient.BitmaskApp.getRefWatcher;
import static se.leap.bitmaskclient.Constants.DONATION_URL;
import static se.leap.bitmaskclient.Constants.ENABLE_DONATION;
import static se.leap.bitmaskclient.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.Constants.REQUEST_CODE_SWITCH_PROVIDER;
import static se.leap.bitmaskclient.Constants.SHARED_PREFERENCES;
import static se.leap.bitmaskclient.R.string.about_fragment_title;
import static se.leap.bitmaskclient.R.string.exclude_apps_fragment_title;
import static se.leap.bitmaskclient.R.string.log_fragment_title;
import static se.leap.bitmaskclient.utils.ConfigHelper.isDefaultBitmask;
import static se.leap.bitmaskclient.utils.PreferenceHelper.getSaveBattery;
import static se.leap.bitmaskclient.utils.PreferenceHelper.getShowAlwaysOnDialog;
import static se.leap.bitmaskclient.utils.PreferenceHelper.getUsePluggableTransports;
import static se.leap.bitmaskclient.utils.PreferenceHelper.saveBattery;
import static se.leap.bitmaskclient.utils.PreferenceHelper.usePluggableTransports;

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
    private static final String TAG = NavigationDrawerFragment.class.getName();
    public static final int TWO_SECONDS = 2000;
    public static final int THREE_SECONDS = 3500;

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private ActionBarDrawerToggle drawerToggle;

    private DrawerLayout drawerLayout;
    private View drawerView;
    private View fragmentContainerView;
    private Toolbar toolbar;
    private IconTextEntry account;
    private IconSwitchEntry saveBattery;

    private boolean userLearnedDrawer;
    private volatile boolean wasPaused;
    private volatile boolean shouldCloseOnResume;

    private SharedPreferences preferences;

    private final static String KEY_SHOW_ENABLE_EXPERIMENTAL_FEATURE = "KEY_SHOW_ENABLE_EXPERIMENTAL_FEATURE";
    private boolean showEnableExperimentalFeature = false;
    AlertDialog alertDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Reads in the flag indicating whether or not the user has demonstrated awareness of the
        // drawer. See PREF_USER_LEARNED_DRAWER for details.
        preferences = getContext().getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        userLearnedDrawer = preferences.getBoolean(PREF_USER_LEARNED_DRAWER, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Indicates that this fragment would like to influence the set of actions in the action bar.
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        drawerView = inflater.inflate(R.layout.f_drawer_main, container, false);
        restoreFromSavedInstance(savedInstanceState);
        return drawerView;
    }

    public boolean isDrawerOpen() {
        return drawerLayout != null && drawerLayout.isDrawerOpen(fragmentContainerView);
    }

    @Override
    public void onResume() {
        super.onResume();
        wasPaused = false;
        if (shouldCloseOnResume) {
            closeDrawerWithDelay();
            showDottedIconWithDelay();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        wasPaused = true;
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param fragmentId   The android:id of this fragment in its activity's layout.
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setUp(int fragmentId, DrawerLayout drawerLayout) {
        final AppCompatActivity activity = (AppCompatActivity) getActivity();
        fragmentContainerView = activity.findViewById(fragmentId);
        this.drawerLayout = drawerLayout;
        // set a custom shadow that overlays the main content when the drawer opens
        this.drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        toolbar = this.drawerLayout.findViewById(R.id.toolbar);

        setupActionBar();
        setupEntries();
        setupActionBarDrawerToggle(activity);

        if (!userLearnedDrawer) {
            openNavigationDrawerForFirstTimeUsers();
        }

        // Defer code dependent on restoration of previous instance state.
        this.drawerLayout.post(() -> drawerToggle.syncState());
        this.drawerLayout.addDrawerListener(drawerToggle);
    }

    private void setupActionBarDrawerToggle(final AppCompatActivity activity) {
        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        drawerToggle = new ActionBarDrawerToggle(
                activity,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (!isAdded()) {
                    return;
                }
                activity.invalidateOptionsMenu();
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) {
                    return;
                }

                if (!userLearnedDrawer) {
                    // The user manually opened the drawer; store this flag to prevent auto-showing
                    // the navigation drawer automatically in the future.
                    userLearnedDrawer = true;
                    preferences.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).apply();
                    toolbar.setNavigationIcon(R.drawable.ic_menu_default);
                }
                activity.invalidateOptionsMenu();
            }
        };
    }

    private void setupEntries() {
        initAccountEntry();
        initSwitchProviderEntry();
        initUseBridgesEntry();
        initSaveBatteryEntry();
        initAlwaysOnVpnEntry();
        initExcludeAppsEntry();
        initDonateEntry();
        initLogEntry();
        initAboutEntry();
    }

    private void initAccountEntry() {
        account = drawerView.findViewById(R.id.account);
        FragmentManagerEnhanced fragmentManager = new FragmentManagerEnhanced(getActivity().getSupportFragmentManager());
        Provider currentProvider = ProviderObservable.getInstance().getCurrentProvider();
        account.setText(currentProvider.getName());
        account.setOnClickListener((buttonView) -> {
            Fragment fragment = new EipFragment();
            Bundle arguments = new Bundle();
            arguments.putParcelable(PROVIDER_KEY, currentProvider);
            fragment.setArguments(arguments);
            hideActionBarSubTitle();
            fragmentManager.replace(R.id.main_container, fragment, MainActivity.TAG);
            closeDrawer();
        });
    }

    private void initSwitchProviderEntry() {
        if (isDefaultBitmask()) {
            IconTextEntry switchProvider = drawerView.findViewById(R.id.switch_provider);
            switchProvider.setVisibility(VISIBLE);
            switchProvider.setOnClickListener(v ->
                    getActivity().startActivityForResult(new Intent(getActivity(), ProviderListActivity.class), REQUEST_CODE_SWITCH_PROVIDER));
        }
    }

    private void initUseBridgesEntry() {
        IconSwitchEntry useBridges = drawerView.findViewById(R.id.bridges_switch);
        if (ProviderObservable.getInstance().getCurrentProvider().supportsPluggableTransports()) {
            useBridges.setVisibility(VISIBLE);
            useBridges.setChecked(getUsePluggableTransports(getContext()));
            useBridges.setOnCheckedChangeListener((buttonView, isChecked) -> {
                usePluggableTransports(getContext(), isChecked);
                if (VpnStatus.isVPNActive()) {
                    EipCommand.startVPN(getContext(), true);
                    closeDrawer();
                }
            });


        } else {
            useBridges.setVisibility(GONE);
        }
    }

    private void initSaveBatteryEntry() {
        saveBattery = drawerView.findViewById(R.id.battery_switch);
        saveBattery.setChecked(getSaveBattery(getContext()));
        saveBattery.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) {
                return;
            }
            if (isChecked) {
                showExperimentalFeatureAlert();
            } else {
                saveBattery(getContext(), false);
            }
        }));
    }

    private void initAlwaysOnVpnEntry() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            IconTextEntry alwaysOnVpn = drawerView.findViewById(R.id.always_on_vpn);
            alwaysOnVpn.setVisibility(VISIBLE);
            alwaysOnVpn.setOnClickListener((buttonView) -> {
                closeDrawer();
                if (getShowAlwaysOnDialog(getContext())) {
                    showAlwaysOnDialog();
                } else {
                    Intent intent = new Intent("android.net.vpn.SETTINGS");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            });
        }
    }

    private void initExcludeAppsEntry() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            IconTextEntry excludeApps = drawerView.findViewById(R.id.exclude_apps);
            excludeApps.setVisibility(VISIBLE);
            Set<String> apps = PreferenceHelper.getExcludedApps(this.getContext());
            if (apps != null) {
                updateExcludeAppsSubtitle(excludeApps, apps.size());
            }
            FragmentManagerEnhanced fragmentManager = new FragmentManagerEnhanced(getActivity().getSupportFragmentManager());
            excludeApps.setOnClickListener((buttonView) -> {
                closeDrawer();
                Fragment fragment = new ExcludeAppsFragment();
                setActionBarTitle(exclude_apps_fragment_title);
                fragmentManager.replace(R.id.main_container, fragment, MainActivity.TAG);
            });
        }
    }

    private void initDonateEntry() {
        if (ENABLE_DONATION) {
            IconTextEntry donate = drawerView.findViewById(R.id.donate);
            donate.setVisibility(VISIBLE);
            donate.setOnClickListener((buttonView) -> {
                closeDrawer();
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(DONATION_URL));
                startActivity(browserIntent);

            });
        }
    }

    private void initLogEntry() {
        IconTextEntry log = drawerView.findViewById(R.id.log);
        FragmentManagerEnhanced fragmentManager = new FragmentManagerEnhanced(getActivity().getSupportFragmentManager());
        log.setOnClickListener((buttonView) -> {
            closeDrawer();
            Fragment fragment = new LogFragment();
            setActionBarTitle(log_fragment_title);
            fragmentManager.replace(R.id.main_container, fragment, MainActivity.TAG);
        });
    }

    private void initAboutEntry() {
        IconTextEntry about = drawerView.findViewById(R.id.about);
        FragmentManagerEnhanced fragmentManager = new FragmentManagerEnhanced(getActivity().getSupportFragmentManager());
        about.setOnClickListener((buttonView) -> {
            closeDrawer();
            Fragment fragment = new AboutFragment();
            setActionBarTitle(about_fragment_title);
            fragmentManager.replace(R.id.main_container, fragment, MainActivity.TAG);
        });
    }

    private void closeDrawer() {
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(fragmentContainerView);
        }
    }

    private ActionBar setupActionBar() {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setSupportActionBar(toolbar);
        final ActionBar actionBar = activity.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        return actionBar;
    }

    private void openNavigationDrawerForFirstTimeUsers() {
        if (userLearnedDrawer) {
            return;
        }

        drawerLayout.openDrawer(fragmentContainerView, false);
        closeDrawerWithDelay();
        showDottedIconWithDelay();

    }

    private void showDottedIconWithDelay() {
        final Handler navigationDrawerHandler = new Handler();
        navigationDrawerHandler.postDelayed(() -> {
            if (!wasPaused) {
                toolbar.setNavigationIcon(R.drawable.ic_menu_color_point);
                toolbar.playSoundEffect(android.view.SoundEffectConstants.CLICK);
            }

        }, THREE_SECONDS);
    }

    @NonNull
    private void closeDrawerWithDelay() {
        final Handler navigationDrawerHandler = new Handler();
        navigationDrawerHandler.postDelayed(() -> {
            if (!wasPaused) {
                drawerLayout.closeDrawer(fragmentContainerView, true);
            } else {
                shouldCloseOnResume = true;
            }

        }, TWO_SECONDS);
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
                    .setPositiveButton((android.R.string.yes), (dialog, which) -> {
                        saveBattery(getContext(), true);
                    })
                    .setNegativeButton(activity.getString(android.R.string.no), (dialog, which) -> saveBattery.setCheckedQuietly(false))
                    .setOnDismissListener(dialog -> showEnableExperimentalFeature = false)
                    .setOnCancelListener(dialog -> saveBattery.setCheckedQuietly(false)).show();
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
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (drawerLayout != null && isDrawerOpen()) {
            showGlobalContextActionBar();
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
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

    private void setActionBarTitle(@StringRes int resId) {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(resId);
        }
    }

    private void hideActionBarSubTitle() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(null);
        }
    }

    public void refresh() {
        Provider currentProvider = ProviderObservable.getInstance().getCurrentProvider();
        account.setText(currentProvider.getName());
        initUseBridgesEntry();
    }

    private void updateExcludeAppsSubtitle(IconTextEntry excludeApps, int number) {
        if (number > 0) {
            excludeApps.setSubtitle(getContext().getResources().getQuantityString(R.plurals.subtitle_exclude_apps, number, number));
            excludeApps.setSubtitleColor(R.color.colorError);
        } else {
            excludeApps.hideSubtitle();
        }
    }

    public void onAppsExcluded(int number) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            IconTextEntry excludeApps = drawerView.findViewById(R.id.exclude_apps);
            updateExcludeAppsSubtitle(excludeApps, number);
        }
    }
}
