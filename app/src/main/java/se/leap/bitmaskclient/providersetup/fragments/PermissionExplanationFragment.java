package se.leap.bitmaskclient.providersetup.fragments;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static se.leap.bitmaskclient.base.utils.BuildConfigHelper.isDefaultBitmask;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.databinding.FPermissionExplanationBinding;


public class PermissionExplanationFragment extends BaseSetupFragment {

    private static String EXTRA_SHOW_NOTIFICATION_PERMISSION = "EXTRA_SHOW_NOTIFICATION_PERMISSION";
    private static String EXTRA_SHOW_VPN_PERMISSION = "EXTRA_SHOW_VPN_PERMISSION";
    FPermissionExplanationBinding binding;
    public static PermissionExplanationFragment newInstance(int position, boolean showNotificationPermission, boolean showVpnPermission) {
        PermissionExplanationFragment fragment = new PermissionExplanationFragment();
        Bundle bundle = initBundle(position);
        bundle.putBoolean(EXTRA_SHOW_NOTIFICATION_PERMISSION, showNotificationPermission);
        bundle.putBoolean(EXTRA_SHOW_VPN_PERMISSION, showVpnPermission);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FPermissionExplanationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            boolean showNotificationPermission = getArguments().getBoolean(EXTRA_SHOW_NOTIFICATION_PERMISSION);
            boolean showVpnPermission = getArguments().getBoolean(EXTRA_SHOW_VPN_PERMISSION);
            if (showVpnPermission && showNotificationPermission) {
                binding.tvTitle.setText(R.string.title_upcoming_request);
                binding.titleUpcomingRequestSummary.setVisibility(VISIBLE);
            } else if (showVpnPermission) {
                binding.tvTitle.setText(R.string.title_upcoming_connection_request);
                binding.titleUpcomingRequestSummary.setVisibility(GONE);
            } else if (showNotificationPermission) {
                binding.tvTitle.setText(R.string.title_upcoming_notifications_request);
                binding.titleUpcomingRequestSummary.setVisibility(GONE);
            }

            binding.titleUpcomingNotificationRequestSummary.setVisibility(showNotificationPermission ? VISIBLE: GONE);
            binding.titleUpcomingConnectionRequestSummary.setText(isDefaultBitmask() ?
                    getString(R.string.title_upcoming_connection_request_summary) :
                    getString(R.string.title_upcoming_connection_request_summary_custom, getString(R.string.app_name)));
            binding.titleUpcomingConnectionRequestSummary.setVisibility(showVpnPermission ? VISIBLE : GONE);
        }
    }

    @Override
    public void onFragmentSelected() {
        super.onFragmentSelected();
        setupActivityCallback.setNavigationButtonHidden(false);
        setupActivityCallback.setCancelButtonHidden(true);
    }

}