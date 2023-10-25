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

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.databinding.FEmptyPermissionSetupBinding;

public class EmptyPermissionSetupFragment extends BaseSetupFragment {

    public static String EXTRA_VPN_INTENT = "EXTRA_VPN_INTENT";
    public static String EXTRA_NOTIFICATION_PERMISSON_ACTION = "EXTRA_NOTIFICATION_PERMISSON_ACTION";

    private String notificationPermissionAction = null;
    private Intent vpnPermissionIntent = null;

    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    if (setupActivityCallback != null) {
                        setupActivityCallback.onConfigurationSuccess();
                    }
                } else {
                    Toast.makeText(getContext(), getText(R.string.permission_rejected), Toast.LENGTH_LONG).show();
                    if (setupActivityCallback != null) {
                        setupActivityCallback.onConfigurationSuccess();
                    }
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
                            Toast.makeText(getContext(), getText(R.string.permission_rejected), Toast.LENGTH_LONG).show();
                            if (setupActivityCallback != null) {
                                setupActivityCallback.onConfigurationSuccess();
                            }
                        }
                    }
            );


    public static EmptyPermissionSetupFragment newInstance(int position, Intent vpnPermissionIntent) {
        Bundle bundle = initBundle(position);
        bundle.putParcelable(EXTRA_VPN_INTENT, vpnPermissionIntent);
        EmptyPermissionSetupFragment fragment = new EmptyPermissionSetupFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public static EmptyPermissionSetupFragment newInstance(int position, String notificationPermissionAction) {
        Bundle bundle = initBundle(position);
        bundle.putString(EXTRA_NOTIFICATION_PERMISSON_ACTION, notificationPermissionAction);
        EmptyPermissionSetupFragment fragment = new EmptyPermissionSetupFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.vpnPermissionIntent = getArguments().getParcelable(EXTRA_VPN_INTENT);
        this.notificationPermissionAction = getArguments().getString(EXTRA_NOTIFICATION_PERMISSON_ACTION);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FEmptyPermissionSetupBinding binding = FEmptyPermissionSetupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (vpnPermissionIntent != null) {
            outState.putParcelable(EXTRA_VPN_INTENT, vpnPermissionIntent);
        }
        if (notificationPermissionAction != null) {
            outState.putString(EXTRA_NOTIFICATION_PERMISSON_ACTION, notificationPermissionAction);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
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