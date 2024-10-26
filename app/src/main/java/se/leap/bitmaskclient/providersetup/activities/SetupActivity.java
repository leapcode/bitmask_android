package se.leap.bitmaskclient.providersetup.activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static androidx.appcompat.app.ActionBar.DISPLAY_SHOW_CUSTOM;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.base.utils.BuildConfigHelper.isDefaultBitmask;
import static se.leap.bitmaskclient.base.utils.PreferenceHelper.deleteProviderDetailsFromPreferences;
import static se.leap.bitmaskclient.providersetup.fragments.SetupFragmentFactory.CIRCUMVENTION_SETUP_FRAGMENT;
import static se.leap.bitmaskclient.providersetup.fragments.SetupFragmentFactory.CONFIGURE_PROVIDER_FRAGMENT;
import static se.leap.bitmaskclient.providersetup.fragments.SetupFragmentFactory.PROVIDER_SELECTION_FRAGMENT;
import static se.leap.bitmaskclient.tor.TorStatusObservable.TorStatus.OFF;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.os.BundleCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

import se.leap.bitmaskclient.BuildConfig;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.FragmentManagerEnhanced;
import se.leap.bitmaskclient.base.models.Introducer;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.models.ProviderObservable;
import se.leap.bitmaskclient.base.utils.ViewHelper;
import se.leap.bitmaskclient.base.views.ActionBarTitle;
import se.leap.bitmaskclient.databinding.ActivitySetupBinding;
import se.leap.bitmaskclient.providersetup.ProviderSetupFailedDialog;
import se.leap.bitmaskclient.providersetup.ProviderSetupObservable;
import se.leap.bitmaskclient.providersetup.SetupViewPagerAdapter;
import se.leap.bitmaskclient.providersetup.fragments.ProviderSelectionFragment;
import se.leap.bitmaskclient.tor.TorServiceCommand;
import se.leap.bitmaskclient.tor.TorStatusObservable;

public class SetupActivity extends AppCompatActivity implements SetupActivityCallback, ProviderSetupFailedDialog.DownloadFailedDialogInterface {

    public static final String EXTRA_PROVIDER = "EXTRA_PROVIDER";
    public static final String EXTRA_CURRENT_POSITION = "EXTRA_CURRENT_POSITION";
    public static final String EXTRA_SWITCH_PROVIDER = "EXTRA_SWITCH_PROVIDER";
    private static final String TAG = SetupActivity.class.getSimpleName();
    ActivitySetupBinding binding;
    Provider provider;
    private int currentPosition = 0;
    private boolean switchProvider = false;

    private final HashSet<CancelCallback> cancelCallbacks = new HashSet<>();
    private FragmentManagerEnhanced fragmentManager;
    SetupViewPagerAdapter adapter;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            provider = BundleCompat.getParcelable(savedInstanceState, EXTRA_PROVIDER, Provider.class);
            currentPosition = savedInstanceState.getInt(EXTRA_CURRENT_POSITION);
            switchProvider = savedInstanceState.getBoolean(EXTRA_SWITCH_PROVIDER);
        }
        if (getIntent() != null && getIntent().hasExtra(EXTRA_SWITCH_PROVIDER)) {
            switchProvider = getIntent().getBooleanExtra(EXTRA_SWITCH_PROVIDER, false);
        }

        binding = ActivitySetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        fragmentManager = new FragmentManagerEnhanced(getSupportFragmentManager());
        ArrayList<View> indicatorViews = new ArrayList<>();

        if (isDefaultBitmask()) {
            // indicator view for provider selection
            addIndicatorView(indicatorViews);
        }

        if (getIntent() != null) {
            if (ProviderObservable.getInstance().getCurrentProvider().isConfigured()){
                switchProvider = true;
            }
            manageIntent(getIntent());
        }

        // indicator views for config setup
        boolean basicProviderSetup = !ProviderObservable.getInstance().getCurrentProvider().isConfigured() || switchProvider;
        if (basicProviderSetup) {
            addIndicatorView(indicatorViews);
            addIndicatorView(indicatorViews);
        }


        // indicator views for VPN permission
        Intent requestVpnPermission = VpnService.prepare(this.getApplicationContext());
        if (requestVpnPermission != null) {
            addIndicatorView(indicatorViews);
            addIndicatorView(indicatorViews);
        }

        // indicator views for notification permission
        boolean requestNotificationPermission = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission = true;
                addIndicatorView(indicatorViews);
                addIndicatorView(indicatorViews);
            }
        }

        // indicator views for "all set" Fragment
        addIndicatorView(indicatorViews);

        adapter = new SetupViewPagerAdapter(getSupportFragmentManager(), getLifecycle(), basicProviderSetup, requestVpnPermission, requestNotificationPermission);

        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPosition = position;
                for (int i = 0; i < indicatorViews.size(); i++) {
                    ((ViewGroup) indicatorViews.get(i)).
                            getChildAt(0).
                            setBackgroundColor(ContextCompat.getColor(SetupActivity.this, (i == position) ? R.color.colorPrimaryDark : R.color.colorDisabled));
                }
                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(position == 0 && switchProvider);
                }
            }
        });
        binding.viewPager.setAdapter(adapter);
        binding.viewPager.setUserInputEnabled(false);


        binding.setupNextButton.setOnClickListener(v -> {
            int currentPos = binding.viewPager.getCurrentItem();
            int newPos = currentPos + 1;
            if (newPos == CIRCUMVENTION_SETUP_FRAGMENT && provider.hasIntroducer()) {
                newPos = newPos + 1; // skip configuration of `CIRCUMVENTION_SETUP_FRAGMENT` when invite code provider is selected
            }
            if (newPos >= binding.viewPager.getAdapter().getItemCount()) {
                return;
            }
            binding.viewPager.setCurrentItem(newPos);
        });
        binding.setupCancelButton.setOnClickListener(v -> {
            cancel();
        });
        setupActionBar();

        if (ProviderSetupObservable.isSetupRunning()) {
            provider = BundleCompat.getParcelable(ProviderSetupObservable.getResultData(), PROVIDER_KEY, Provider.class);
            if (provider != null) {
                currentPosition = adapter.getFragmentPostion(CONFIGURE_PROVIDER_FRAGMENT);
            }
        }
        binding.viewPager.setCurrentItem(currentPosition, false);
    }

    /**
     * Manages the incoming intent and processes the provider selection if the intent action is ACTION_VIEW
     * and the data scheme is "obfsvpnintro". This method create an introducer from the URI data and sets the
     * current provider to the introducer.
     * <p>
     *     If the current fragment is a ProviderSelectionFragment, it will notify the fragment that the provider
     *     selection has changed.
     * </p>
     *
     *
     * @param intent The incoming intent to be managed.
     * @see #onProviderSelected(Provider)
     * @see ProviderSelectionFragment#providerSelectionChanged()
     */
    private void manageIntent(Intent intent) {
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            String scheme = intent.getData().getScheme();

            if (Objects.equals(scheme, "obfsvpnintro")) {
                try {
                    onProviderSelected(new Provider(Introducer.fromUrl(intent.getData().toString())));
                    binding.viewPager.setCurrentItem(adapter.getFragmentPostion(PROVIDER_SELECTION_FRAGMENT));
                    binding.viewPager.post(() -> {
                        /**
                         * @see FragmentStateAdapter#saveState()
                         */
                        String fragmentTag = "f" + binding.viewPager.getCurrentItem();
                        Fragment fragment = getSupportFragmentManager().findFragmentByTag(fragmentTag);
                        if (fragment instanceof ProviderSelectionFragment){
                            ((ProviderSelectionFragment) fragment).providerSelectionChanged();
                        }
                    });
                } catch (Exception e) {
                    Log.d("invite", e.getMessage());
                }
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        manageIntent(intent);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (provider != null) {
            outState.putParcelable(EXTRA_PROVIDER, provider);
            outState.putInt(EXTRA_CURRENT_POSITION, currentPosition);
            outState.putBoolean(EXTRA_SWITCH_PROVIDER, switchProvider);
        }
    }

    private void cancel() {
        binding.viewPager.setCurrentItem(0, false);
        ProviderSetupObservable.cancel();
        if (TorStatusObservable.getStatus() != OFF) {
            Log.d(TAG, "SHUTDOWN - cancelSettingUpProvider");
            TorServiceCommand.stopTorServiceAsync(this);
        }
        provider = null;
        for (CancelCallback cancelCallback : cancelCallbacks) {
            cancelCallback.onCanceled();
        }
    }

    private void addIndicatorView(ArrayList<View> indicatorViews) {
        // FIXME: we have to work around a bug in our usage of CardView, that
        //  doesn't let us programmatically add new indicator views as needed.
        //  for some reason the cardBackgroundColor property is ignored if we add
        //  the card view dynamically
        View v = binding.indicatorContainer.getChildAt(indicatorViews.size());
        if (v == null) {
            throw new IllegalStateException("Too few indicator views in layout hard-coded");
        }
        v.setVisibility(VISIBLE);
        indicatorViews.add(v);
    }

    private void setupActionBar() {
        setSupportActionBar(binding.toolbar);
        final ActionBar actionBar = getSupportActionBar();
        Context context = actionBar.getThemedContext();
        actionBar.setDisplayOptions(DISPLAY_SHOW_CUSTOM);

        ActionBarTitle actionBarTitle = new ActionBarTitle(context);
        actionBarTitle.setTitleCaps(BuildConfig.actionbar_capitalize_title);
        actionBarTitle.setTitle(getString(R.string.app_name));

        final Drawable upArrow = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_back, getTheme());
        actionBar.setHomeAsUpIndicator(upArrow);

        actionBar.setDisplayHomeAsUpEnabled(currentPosition == 0 && switchProvider);
        ViewHelper.setActivityBarColor(this, R.color.bg_setup_status_bar, R.color.bg_setup_action_bar, R.color.colorActionBarTitleFont);
        @ColorInt int titleColor = ContextCompat.getColor(context, R.color.colorActionBarTitleFont);
        actionBarTitle.setTitleTextColor(titleColor);

        actionBarTitle.setCentered(BuildConfig.actionbar_center_title);
        actionBarTitle.setSingleBoldTitle();
        if (BuildConfig.actionbar_center_title) {
            ActionBar.LayoutParams params = new ActionBar.LayoutParams(
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    ActionBar.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER);
            actionBar.setCustomView(actionBarTitle, params);
        } else {
            actionBar.setCustomView(actionBarTitle);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSetupStepValidationChanged(boolean isValid) {
        binding.setupNextButton.setEnabled(isValid);
    }

    @Override
    public void registerOnPageChangeCallback(ViewPager2.OnPageChangeCallback callback) {
        binding.viewPager.registerOnPageChangeCallback(callback);
    }

    @Override
    public void removeOnPageChangeCallback(ViewPager2.OnPageChangeCallback callback) {
        binding.viewPager.unregisterOnPageChangeCallback(callback);
    }

    @Override
    public void registerCancelCallback(CancelCallback cancelCallback) {
        cancelCallbacks.add(cancelCallback);
    }

    @Override
    public void removeCancelCallback(CancelCallback cancelCallback) {
        cancelCallbacks.remove(cancelCallback);
    }

    @Override
    public void setNavigationButtonHidden(boolean isHidden) {
        binding.setupNextButton.setVisibility(isHidden ? GONE : VISIBLE);
    }

    @Override
    public void setCancelButtonHidden(boolean isHidden) {
        binding.setupCancelButton.setVisibility(isHidden ? GONE : VISIBLE);
    }

    @Override
    public void onProviderSelected(Provider provider) {
        this.provider = provider;
    }

    @Override
    public void onConfigurationSuccess() {
        binding.viewPager.setCurrentItem(binding.viewPager.getCurrentItem() + 1);
    }

    @Override
    public Provider getSelectedProvider() {
        return provider;
    }

    @Override
    public int getCurrentPosition() {
        return currentPosition;
    }

    @Override
    public void onSetupFinished() {
        ProviderSetupObservable.reset();
        Intent intent = getIntent();
        if (provider == null && ProviderObservable.getInstance().getCurrentProvider().isConfigured()) {
            // only permissions were requested, no new provider configured, so reuse previously configured one
            provider = ProviderObservable.getInstance().getCurrentProvider();
        }
        intent.putExtra(Provider.KEY, provider);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onError(String reasonToFail) {
        binding.viewPager.setCurrentItem(0, false);
        try {
            FragmentTransaction fragmentTransaction = fragmentManager.removePreviousFragment(ProviderSetupFailedDialog.TAG);
            DialogFragment newFragment;
            try {
                JSONObject errorJson = new JSONObject(reasonToFail);
                newFragment = ProviderSetupFailedDialog.newInstance(provider, errorJson, false);
            } catch (JSONException e) {
                e.printStackTrace();
                newFragment = ProviderSetupFailedDialog.newInstance(provider, reasonToFail);
            } catch (NullPointerException e) {
                //reasonToFail was null
                return;
            }
            newFragment.show(fragmentTransaction, ProviderSetupFailedDialog.TAG);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void retrySetUpProvider(@NonNull Provider provider) {
        ProviderSetupObservable.reset();
        onProviderSelected(provider);
        binding.viewPager.setCurrentItem(adapter.getFragmentPostion(CONFIGURE_PROVIDER_FRAGMENT));
    }

    @Override
    public void cancelSettingUpProvider() {
        cancel();
    }

    @Override
    public void updateProviderDetails() {
        provider.reset();
        deleteProviderDetailsFromPreferences(provider.getDomain());
        binding.viewPager.setCurrentItem(adapter.getFragmentPostion(CONFIGURE_PROVIDER_FRAGMENT));
    }

    @Override
    public void addAndSelectNewProvider(String url) {
        // ignore, not implemented anymore in new setup flow
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        adapter = null;
    }
}