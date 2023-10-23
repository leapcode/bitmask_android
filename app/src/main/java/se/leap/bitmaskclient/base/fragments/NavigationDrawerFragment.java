/**
 * Copyright (c) 2021 LEAP Encryption Access Project and contributers
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
package se.leap.bitmaskclient.base.fragments;


import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static se.leap.bitmaskclient.base.models.Constants.DONATION_URL;
import static se.leap.bitmaskclient.base.models.Constants.ENABLE_DONATION;
import static se.leap.bitmaskclient.base.models.Constants.PREFERRED_CITY;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.base.models.Constants.REQUEST_CODE_SWITCH_PROVIDER;
import static se.leap.bitmaskclient.base.utils.ConfigHelper.isDefaultBitmask;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getPreferredCity;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.getSaveBattery;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.saveBattery;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import java.util.Observable;
import java.util.Observer;

import se.leap.bitmaskclient.BuildConfig;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.FragmentManagerEnhanced;
import se.leap.bitmaskclient.base.MainActivity;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.models.ProviderObservable;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.base.views.IconSwitchEntry;
import se.leap.bitmaskclient.base.views.IconTextEntry;
import se.leap.bitmaskclient.eip.EipStatus;
import se.leap.bitmaskclient.providersetup.activities.SetupActivity;
import se.leap.bitmaskclient.tethering.TetheringObservable;

/**
 * Fragment used for managing interactions for and presentation of a navigation drawer.
 * See the <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for a complete explanation of the behaviors implemented here.
 */
public class NavigationDrawerFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener, Observer {

    /**
     * Per the design guidelines, you should show the drawer on launch until the user manually
     * expands it. This shared preference tracks this.
     */
    private static final String TAG = NavigationDrawerFragment.class.getName();
    public static final int TWO_SECONDS = 2000;

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

    private IconTextEntry manualGatewaySelection;
    private volatile boolean wasPaused;
    private volatile boolean shouldCloseOnResume;

    private final static String KEY_SHOW_SAVE_BATTERY_ALERT = "KEY_SHOW_SAVE_BATTERY_ALERT";
    private volatile boolean showSaveBattery = false;
    AlertDialog alertDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceHelper.registerOnSharedPreferenceChangeListener(this);
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
        TetheringObservable.getInstance().addObserver(this);
        EipStatus.getInstance().addObserver(this);
        return drawerView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        TetheringObservable.getInstance().deleteObserver(this);
        EipStatus.getInstance().deleteObserver(this);
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

        setupEntries();
        setupActionBarDrawerToggle(activity);

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

                activity.invalidateOptionsMenu();
            }
        };
        setDrawerToggleColor(activity, ContextCompat.getColor(activity, R.color.amber200));
    }

    private void setupEntries() {
        initAccountEntry();
        initSwitchProviderEntry();
        initSaveBatteryEntry();
        initManualGatewayEntry();
        initAdvancedSettingsEntry();
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
            setDrawerToggleColor(drawerView.getContext(), ContextCompat.getColor(drawerView.getContext(), R.color.actionbar_connectivity_state_text_color_dark));
            Bundle arguments = new Bundle();
            arguments.putParcelable(PROVIDER_KEY, currentProvider);
            fragment.setArguments(arguments);
            fragmentManager.replace(R.id.main_container, fragment, MainActivity.TAG);
            closeDrawer();
        });
    }

    public void setDrawerToggleColor(Context context, int color) {
        DrawerArrowDrawable drawable = new DrawerArrowDrawable(context);
        drawable.setTint(color);
        drawable.setColor(color);
        drawerToggle.setDrawerArrowDrawable(drawable);
    }

    private void initSwitchProviderEntry() {
        if (isDefaultBitmask()) {
            IconTextEntry switchProvider = drawerView.findViewById(R.id.switch_provider);
            switchProvider.setVisibility(VISIBLE);
            switchProvider.setOnClickListener(v -> {
                closeDrawer();
                Intent intent = new Intent(getActivity(), SetupActivity.class);
                intent.putExtra(SetupActivity.EXTRA_SWITCH_PROVIDER, true);
                getActivity().startActivityForResult(intent, REQUEST_CODE_SWITCH_PROVIDER);
            });
        }
    }

    private void initAdvancedSettingsEntry() {
        IconTextEntry advancedSettings = drawerView.findViewById(R.id.advancedSettings);
        FragmentManagerEnhanced fragmentManager = new FragmentManagerEnhanced(getActivity().getSupportFragmentManager());
        advancedSettings.setOnClickListener(v -> {
            closeDrawer();
            Fragment fragment = new SettingsFragment();
            setDrawerToggleColor(drawerView.getContext(), ContextCompat.getColor(drawerView.getContext(), R.color.colorActionBarTitleFont));
            fragmentManager.replace(R.id.main_container, fragment, MainActivity.TAG);
        });
    }

    private void initSaveBatteryEntry() {
        saveBattery = drawerView.findViewById(R.id.battery_switch);
        saveBattery.showSubtitle(false);
        saveBattery.setChecked(getSaveBattery());
        saveBattery.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) {
                return;
            }
            if (isChecked) {
                showSaveBatteryAlert();
            } else {
                saveBattery(false);
            }
        }));
        boolean enableEntry = !TetheringObservable.getInstance().getTetheringState().isVpnTetheringRunning();
        enableSaveBatteryEntry(enableEntry);
    }

    private void enableSaveBatteryEntry(boolean enabled) {
        if (saveBattery.isEnabled() == enabled) {
            return;
        }
        saveBattery.setEnabled(enabled);
        saveBattery.showSubtitle(!enabled);
    }

    private void initManualGatewayEntry() {
        if (!BuildConfig.allow_manual_gateway_selection) {
            return;
        }
        manualGatewaySelection = drawerView.findViewById(R.id.manualGatewaySelection);
        String preferredGateway = getPreferredCity();
        String subtitle = preferredGateway != null ? preferredGateway : getString(R.string.gateway_selection_recommended_location);
        manualGatewaySelection.setSubtitle(subtitle);
        boolean show =  ProviderObservable.getInstance().getCurrentProvider().hasGatewaysInDifferentLocations();
        manualGatewaySelection.setVisibility(show ? VISIBLE : GONE);

        manualGatewaySelection.setOnClickListener(v -> {
            FragmentManagerEnhanced fragmentManager = new FragmentManagerEnhanced(getActivity().getSupportFragmentManager());
            closeDrawer();
            Fragment current = fragmentManager.findFragmentByTag(MainActivity.TAG);
            if (current instanceof GatewaySelectionFragment) {
                return;
            }
            Fragment fragment = new GatewaySelectionFragment();
            setDrawerToggleColor(drawerView.getContext(), ContextCompat.getColor(drawerView.getContext(), R.color.colorActionBarTitleFont));
            fragmentManager.replace(R.id.main_container, fragment, MainActivity.TAG);
        });
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
            setDrawerToggleColor(drawerView.getContext(), ContextCompat.getColor(drawerView.getContext(), R.color.colorActionBarTitleFont));
            fragmentManager.replace(R.id.main_container, fragment, MainActivity.TAG);
        });
    }

    private void initAboutEntry() {
        IconTextEntry about = drawerView.findViewById(R.id.about);
        FragmentManagerEnhanced fragmentManager = new FragmentManagerEnhanced(getActivity().getSupportFragmentManager());
        about.setOnClickListener((buttonView) -> {
            closeDrawer();
            Fragment fragment = new AboutFragment();
            setDrawerToggleColor(drawerView.getContext(), ContextCompat.getColor(drawerView.getContext(), R.color.colorActionBarTitleFont));
            fragmentManager.replace(R.id.main_container, fragment, MainActivity.TAG);
        });
    }

    private void closeDrawer() {
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(fragmentContainerView);
        }
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
        if (showSaveBattery) {
            outState.putBoolean(KEY_SHOW_SAVE_BATTERY_ALERT, true);
            alertDialog.dismiss();
        }
    }

    private void restoreFromSavedInstance(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_SHOW_SAVE_BATTERY_ALERT)) {
            showSaveBatteryAlert();
        }
    }

    private void showSaveBatteryAlert() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        try {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
            showSaveBattery = true;
            alertDialog = alertBuilder
                    .setTitle(activity.getString(R.string.save_battery))
                    .setMessage(activity.getString(R.string.save_battery_message))
                    .setPositiveButton((android.R.string.yes), (dialog, which) -> {
                        saveBattery(true);
                    })
                    .setNegativeButton(activity.getString(android.R.string.no), (dialog, which) -> saveBattery.setCheckedQuietly(false))
                    .setOnDismissListener(dialog -> showSaveBattery = false)
                    .setOnCancelListener(dialog -> saveBattery.setCheckedQuietly(false)).show();
        } catch (IllegalStateException e) {
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PreferenceHelper.unregisterOnSharedPreferenceChangeListener(this);
    }

    public void refresh() {
        Provider currentProvider = ProviderObservable.getInstance().getCurrentProvider();
        account.setText(currentProvider.getName());
        initManualGatewayEntry();
    } 

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key != null && key.equals(PREFERRED_CITY)) {
            initManualGatewayEntry();
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof TetheringObservable || o instanceof EipStatus) {
            try {
                getActivity().runOnUiThread(() ->
                        enableSaveBatteryEntry(!TetheringObservable.getInstance().getTetheringState().isVpnTetheringRunning()));
            } catch (NullPointerException npe) {
                // eat me
            }
        }
    }
}
