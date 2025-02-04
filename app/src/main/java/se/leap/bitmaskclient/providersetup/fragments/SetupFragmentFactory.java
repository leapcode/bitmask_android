package se.leap.bitmaskclient.providersetup.fragments;

import android.Manifest;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

public class SetupFragmentFactory {
    public static final int PROVIDER_SELECTION_FRAGMENT = 0;
    public static final int CIRCUMVENTION_SETUP_FRAGMENT = 1;
    public static final int VPN_PERMISSON_FRAGMENT = 2;
    public static final int NOTIFICATION_PERMISSON_EDUCATIONAL_FRAGMENT = 3;
    public static final int NOTIFICATION_PERMISSON_FRAGMENT = 4;
    public static final int CONFIGURE_PROVIDER_FRAGMENT = 5;

    public static final int SUCCESS_FRAGMENT = 6;

    private final Intent vpnPermissionRequest;

    private final ArrayList<Integer> fragmentTypes;

    private final boolean showNotificationPermission;

    public SetupFragmentFactory(@NonNull ArrayList<Integer> fragmentTypes, Intent vpnPermissionRequest, boolean showNotificationPermission) {
        this.fragmentTypes = fragmentTypes;
        this.vpnPermissionRequest = vpnPermissionRequest;
        this.showNotificationPermission = showNotificationPermission;
    }

    public Fragment createFragment(int position) {
        if (position < 0 || position >= fragmentTypes.size()) {
            throw new IllegalStateException("Illegal fragment position");
        }
        int type = fragmentTypes.get(position);
        switch (type) {
            case PROVIDER_SELECTION_FRAGMENT:
                return ProviderSelectionFragment.newInstance(position);
            case CIRCUMVENTION_SETUP_FRAGMENT:
                return CircumventionSetupFragment.newInstance(position);
            case CONFIGURE_PROVIDER_FRAGMENT:
                return ConfigureProviderFragment.newInstance(position);
            case NOTIFICATION_PERMISSON_EDUCATIONAL_FRAGMENT:
                return PermissionExplanationFragment.newInstance(position, showNotificationPermission, vpnPermissionRequest!=null);
            case NOTIFICATION_PERMISSON_FRAGMENT:
                return EmptyPermissionSetupFragment.newInstance(position, Manifest.permission.POST_NOTIFICATIONS);
            case VPN_PERMISSON_FRAGMENT:
                return EmptyPermissionSetupFragment.newInstance(position, vpnPermissionRequest);
            case SUCCESS_FRAGMENT:
                return SetupSuccessFragment.newInstance(position);
            default:
                throw new IllegalArgumentException("Unexpected fragment type: " + type);
        }
    }

    public int getItemCount() {
        return fragmentTypes.size();
    }

    public int getPos(int fragmentType) {
        return fragmentTypes.indexOf(fragmentType);
    }
}
