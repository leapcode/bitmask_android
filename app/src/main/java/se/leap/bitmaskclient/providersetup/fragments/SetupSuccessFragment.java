package se.leap.bitmaskclient.providersetup.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import se.leap.bitmaskclient.databinding.FSetupSuccessBinding;

public class SetupSuccessFragment extends BaseSetupFragment {


    private SetupSuccessFragment(int position) {
        super(position);
    }

    public static SetupSuccessFragment newInstance(int position) {
        return new SetupSuccessFragment(position);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FSetupSuccessBinding binding = FSetupSuccessBinding.inflate(inflater, container, false);

        binding.mainButton.setOnClickListener(v -> {
            setupActivityCallback.onSetupFinished();
            binding.mainButton.updateState(false, true);
            binding.mainButton.setEnabled(false);
        });

        return binding.getRoot();
    }

    @Override
    public void onFragmentSelected() {
        super.onFragmentSelected();
        setupActivityCallback.setNavigationButtonHidden(true);
        setupActivityCallback.setCancelButtonHidden(true);
    }

}