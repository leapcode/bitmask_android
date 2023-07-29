package se.leap.bitmaskclient.providersetup.fragments;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.databinding.FCircumventionSetupBinding;

public class CircumventionSetupFragment extends Fragment {

    public static CircumventionSetupFragment newInstance() {
        return new CircumventionSetupFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FCircumventionSetupBinding binding = FCircumventionSetupBinding.inflate(inflater, container, false);

        binding.circumventionRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (binding.rbCircumvention.getId() == checkedId) {
                PreferenceHelper.useBridges(getContext(), true);
                PreferenceHelper.useSnowflake(getContext(), true);
                binding.tvCircumventionDetailDescription.setVisibility(View.VISIBLE);
                binding.rbCircumvention.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                binding.rbPlainVpn.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
                return;
            }

            PreferenceHelper.useBridges(getContext(), false);
            PreferenceHelper.useSnowflake(getContext(), false);
            binding.tvCircumventionDetailDescription.setVisibility(View.GONE);
            binding.rbPlainVpn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            binding.rbCircumvention.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        });
        binding.circumventionRadioGroup.check(binding.rbPlainVpn.getId());
        return binding.getRoot();
    }
}