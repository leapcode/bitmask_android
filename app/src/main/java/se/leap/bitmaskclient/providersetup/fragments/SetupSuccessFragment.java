package se.leap.bitmaskclient.providersetup.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.databinding.FSetupSuccessBinding;

public class SetupSuccessFragment extends BaseSetupFragment {

    public static SetupSuccessFragment newInstance(int position) {
        SetupSuccessFragment fragment = new SetupSuccessFragment();
        fragment.setArguments(initBundle(position));
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FSetupSuccessBinding binding = FSetupSuccessBinding.inflate(inflater, container, false);

        binding.mainButton.setOnClickListener(v -> {
            setupActivityCallback.onSetupFinished();
            binding.mainButton.setEnabled(false);
            binding.mainButton.setCustomDrawable(R.drawable.button_setup_circle_progress);
        });
        binding.mainButton.setCustomDrawable(R.drawable.button_setup_circle_start);

        return binding.getRoot();
    }

    @Override
    public void onFragmentSelected() {
        super.onFragmentSelected();
        setupActivityCallback.setNavigationButtonHidden(true);
        setupActivityCallback.setCancelButtonHidden(true);
    }

}