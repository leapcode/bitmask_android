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

    public static VpnPermissionSetupFragment newInstance(int position) {
        VpnPermissionSetupFragment fragment = new VpnPermissionSetupFragment();
        fragment.setArguments(initBundle(position));
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FVpnPermissionSetupBinding binding = FVpnPermissionSetupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onFragmentSelected() {
        super.onFragmentSelected();
        setupActivityCallback.setNavigationButtonHidden(false);
        setupActivityCallback.setCancelButtonHidden(true);
    }

}