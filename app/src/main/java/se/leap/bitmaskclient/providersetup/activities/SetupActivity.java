package se.leap.bitmaskclient.providersetup.activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static androidx.appcompat.app.ActionBar.DISPLAY_SHOW_CUSTOM;

import static se.leap.bitmaskclient.base.models.Constants.REQUEST_CODE_CONFIGURE_LEAP;
import static se.leap.bitmaskclient.tor.TorStatusObservable.TorStatus.OFF;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;

import se.leap.bitmaskclient.BuildConfig;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.views.ActionBarTitle;
import se.leap.bitmaskclient.databinding.ActivitySetupBinding;
import se.leap.bitmaskclient.providersetup.SetupViewPagerAdapter;
import se.leap.bitmaskclient.tor.TorServiceCommand;
import se.leap.bitmaskclient.tor.TorStatusObservable;

public class SetupActivity extends AppCompatActivity implements SetupActivityCallback {

    private static final String TAG = SetupActivity.class.getSimpleName();
    ActivitySetupBinding binding;
    Provider provider;
    private final HashSet<CancelCallback> cancelCallbacks = new HashSet<>();

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ArrayList<View> indicatorViews = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            addIndicatorView(indicatorViews);
        }

        Intent requestVpnPermission = VpnService.prepare(this);
        if (requestVpnPermission != null) {
            addIndicatorView(indicatorViews);
            addIndicatorView(indicatorViews);
        }

        boolean showNotificationPermissionFragments = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                showNotificationPermissionFragments = shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS);
                if (showNotificationPermissionFragments) {
                    addIndicatorView(indicatorViews);
                    addIndicatorView(indicatorViews);
                }
            }
        }

        SetupViewPagerAdapter adapter = new SetupViewPagerAdapter(getSupportFragmentManager(), getLifecycle(), requestVpnPermission, showNotificationPermissionFragments);

        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                for (int i = 0; i < indicatorViews.size(); i++) {
                    ((ViewGroup) indicatorViews.get(i)).
                            getChildAt(0).
                            setBackgroundColor(ContextCompat.getColor(SetupActivity.this, (i == position) ? R.color.colorPrimaryDark : R.color.colorDisabled));
                }
            }
        });
        binding.viewPager.setAdapter(adapter);
        binding.viewPager.setUserInputEnabled(false);
        binding.setupNextButton.setOnClickListener(v -> {
            int currentPos = binding.viewPager.getCurrentItem();
            int newPos = currentPos + 1;
            if (newPos >= binding.viewPager.getAdapter().getItemCount()) {
                Toast.makeText(SetupActivity.this, "SetupFinished \\o/", Toast.LENGTH_LONG).show();
                return;
            }
            binding.viewPager.setCurrentItem(newPos);
        });
        binding.setupCancelButton.setOnClickListener(v -> {
            binding.viewPager.setCurrentItem(0, false);
            if (TorStatusObservable.getStatus() != OFF) {
                Log.d(TAG, "SHUTDOWN - cancelSettingUpProvider");
                TorServiceCommand.stopTorServiceAsync(this);
            }
            provider = null;
            for (CancelCallback cancelCallback : cancelCallbacks) {
                cancelCallback.onCanceled();
            }
        });
        setupActionBar();

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
        actionBarTitle.showSubtitle(false);

        @ColorInt int titleColor = ContextCompat.getColor(context, R.color.colorActionBarTitleFont);
        actionBarTitle.setTitleTextColor(titleColor);

        actionBarTitle.setCentered(BuildConfig.actionbar_center_title);
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
        return binding.viewPager.getCurrentItem();
    }

    @Override
    public void onSetupFinished() {
        Intent intent = getIntent();
        intent.putExtra(Provider.KEY, provider);
        setResult(RESULT_OK, intent);
        finish();
    }

}