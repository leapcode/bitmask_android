package se.leap.bitmaskclient.providersetup.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import se.leap.bitmaskclient.databinding.FEmptyPermissionSetupBinding;
import se.leap.bitmaskclient.databinding.FVpnPermissionSetupBinding;

public class EmptyPermissionSetupFragment extends BaseSetupFragment {

    private String notificationPermissionAction = null;
    private Intent vpnPermissionIntent = null;

    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    if (setupActivityCallback != null) {
                        setupActivityCallback.onConfigurationSuccess();
                    }
                } else {
                    Toast.makeText(getContext(), "Permission request failed :(", Toast.LENGTH_LONG).show();
                    setupActivityCallback.setNavigationButtonHidden(false);
                    // TODO: implement sth. useful
                }
            });

    private final ActivityResultLauncher<Intent> requestVpnPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            if (setupActivityCallback != null) {
                                setupActivityCallback.onConfigurationSuccess();
                            }
                        } else {
                            Toast.makeText(getContext(), "Permission request failed :(", Toast.LENGTH_LONG).show();
                            setupActivityCallback.setNavigationButtonHidden(false);
                            // TODO: implement sth. useful
                        }
                    }
            );

    private EmptyPermissionSetupFragment(int position, String permissionAction) {
        super(position);
        this.notificationPermissionAction = permissionAction;
    }

    private EmptyPermissionSetupFragment(int position, Intent vpnPermissionIntent) {
        super(position);
        this.vpnPermissionIntent = vpnPermissionIntent;
    }

    public static EmptyPermissionSetupFragment newInstance(int position, Intent vpnPermissionIntent) {
        return new EmptyPermissionSetupFragment(position, vpnPermissionIntent);
    }

    public static EmptyPermissionSetupFragment newInstance(int position, String notificationPermissionAction) {
        return new EmptyPermissionSetupFragment(position, notificationPermissionAction);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FEmptyPermissionSetupBinding binding = FEmptyPermissionSetupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onFragmentSelected() {
        super.onFragmentSelected();
        if (notificationPermissionAction != null) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else if (vpnPermissionIntent != null) {
            requestVpnPermissionLauncher.launch(vpnPermissionIntent);
        }

        setupActivityCallback.setNavigationButtonHidden(true);
        setupActivityCallback.setCancelButtonHidden(true);
    }


}