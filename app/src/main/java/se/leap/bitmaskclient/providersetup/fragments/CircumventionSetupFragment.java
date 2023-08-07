package se.leap.bitmaskclient.providersetup.fragments;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.databinding.FCircumventionSetupBinding;

public class CircumventionSetupFragment extends BaseSetupFragment {

    public static CircumventionSetupFragment newInstance(int position) {
        CircumventionSetupFragment fragment = new CircumventionSetupFragment();
        fragment.setArguments(initBundle(position));
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FCircumventionSetupBinding binding = FCircumventionSetupBinding.inflate(inflater, container, false);

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
    public void onFragmentSelected() {
        super.onFragmentSelected();
        setupActivityCallback.setCancelButtonHidden(false);
        setupActivityCallback.setNavigationButtonHidden(false);
    }
}