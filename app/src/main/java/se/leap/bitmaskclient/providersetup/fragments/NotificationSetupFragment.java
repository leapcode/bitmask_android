package se.leap.bitmaskclient.providersetup.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import se.leap.bitmaskclient.databinding.FNotificationSetupBinding;

public class NotificationSetupFragment extends BaseSetupFragment {

    public static NotificationSetupFragment newInstance(int position) {
        NotificationSetupFragment fragment = new NotificationSetupFragment();
        fragment.setArguments(initBundle(position));
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FNotificationSetupBinding binding = FNotificationSetupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onFragmentSelected() {
        super.onFragmentSelected();
        setupActivityCallback.setNavigationButtonHidden(false);
        setupActivityCallback.setCancelButtonHidden(true);
    }

}