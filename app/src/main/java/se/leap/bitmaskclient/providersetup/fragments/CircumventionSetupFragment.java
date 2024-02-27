package se.leap.bitmaskclient.providersetup.fragments;

import static se.leap.bitmaskclient.base.utils.BuildConfigHelper.isDefaultBitmask;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.databinding.FCircumventionSetupBinding;
import se.leap.bitmaskclient.providersetup.ProviderManager;
import se.leap.bitmaskclient.providersetup.activities.CancelCallback;

public class CircumventionSetupFragment extends BaseSetupFragment implements CancelCallback {

    public static CircumventionSetupFragment newInstance(int position) {
        CircumventionSetupFragment fragment = new CircumventionSetupFragment();
        fragment.setArguments(initBundle(position));
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FCircumventionSetupBinding binding = FCircumventionSetupBinding.inflate(inflater, container, false);
        binding.rbPlainVpn.setText(getString(R.string.use_standard_vpn, getString(R.string.app_name)));
        binding.tvCircumventionDetailDescription.setText(getString(R.string.circumvention_setup_hint, getString(R.string.app_name)));
        binding.circumventionRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (binding.rbCircumvention.getId() == checkedId) {
                PreferenceHelper.useBridges(true);
                PreferenceHelper.useSnowflake(true);
                binding.tvCircumventionDetailDescription.setVisibility(View.VISIBLE);
                binding.rbCircumvention.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                binding.rbPlainVpn.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
                return;
            }

            PreferenceHelper.useBridges(false);
            PreferenceHelper.useSnowflake(false);
            binding.tvCircumventionDetailDescription.setVisibility(View.GONE);
            binding.rbPlainVpn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            binding.rbCircumvention.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        });

        int id = (PreferenceHelper.hasSnowflakePrefs() && PreferenceHelper.getUseSnowflake()) ?
                binding.rbCircumvention.getId() :
                binding.rbPlainVpn.getId();
        binding.circumventionRadioGroup.check(id);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupActivityCallback.registerCancelCallback(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        setupActivityCallback.removeCancelCallback(this);
    }

    @Override
    public void onFragmentSelected() {
        super.onFragmentSelected();
        setupActivityCallback.setCancelButtonHidden(!isDefaultBitmask());
        setupActivityCallback.setNavigationButtonHidden(false);
        if (!isDefaultBitmask()) {
            loadProviderFromAssets();
        }
    }

    private void loadProviderFromAssets() {
        ProviderManager providerManager = ProviderManager.getInstance(getContext().getApplicationContext().getAssets());
        providerManager.setAddDummyEntry(false);
        setupActivityCallback.onProviderSelected(providerManager.providers().get(0));
    }

    @Override
    public void onCanceled() {
        if (!isDefaultBitmask()) {
            loadProviderFromAssets();
        }
    }
}