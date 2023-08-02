package se.leap.bitmaskclient.providersetup.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import se.leap.bitmaskclient.databinding.FVpnPermissionSetupBinding;

public class VpnPermissionSetupFragment extends BaseSetupFragment {


    private VpnPermissionSetupFragment(int position) {
        super(position);
    }

    public static VpnPermissionSetupFragment newInstance(int position) {
        return new VpnPermissionSetupFragment(position);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FVpnPermissionSetupBinding binding = FVpnPermissionSetupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public void onFragmentSelected() {
        super.onFragmentSelected();
        setupActivityCallback.setNavigationButtonHidden(false);
        setupActivityCallback.setCancelButtonHidden(true);
    }

}