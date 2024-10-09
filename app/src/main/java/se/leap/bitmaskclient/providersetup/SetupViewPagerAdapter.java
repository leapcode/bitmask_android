package se.leap.bitmaskclient.providersetup;

import static se.leap.bitmaskclient.base.utils.BuildConfigHelper.isDefaultBitmask;
import static se.leap.bitmaskclient.providersetup.fragments.SetupFragmentFactory.CIRCUMVENTION_SETUP_FRAGMENT;
import static se.leap.bitmaskclient.providersetup.fragments.SetupFragmentFactory.CONFIGURE_PROVIDER_FRAGMENT;
import static se.leap.bitmaskclient.providersetup.fragments.SetupFragmentFactory.NOTIFICATION_PERMISSON_EDUCATIONAL_FRAGMENT;
import static se.leap.bitmaskclient.providersetup.fragments.SetupFragmentFactory.NOTIFICATION_PERMISSON_FRAGMENT;
import static se.leap.bitmaskclient.providersetup.fragments.SetupFragmentFactory.PROVIDER_SELECTION_FRAGMENT;
import static se.leap.bitmaskclient.providersetup.fragments.SetupFragmentFactory.SUCCESS_FRAGMENT;
import static se.leap.bitmaskclient.providersetup.fragments.SetupFragmentFactory.VPN_PERMISSON_EDUCATIONAL_FRAGMENT;
import static se.leap.bitmaskclient.providersetup.fragments.SetupFragmentFactory.VPN_PERMISSON_FRAGMENT;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;

import se.leap.bitmaskclient.providersetup.fragments.SetupFragmentFactory;

public class SetupViewPagerAdapter extends FragmentStateAdapter {

    private SetupFragmentFactory setupFragmentFactory;

    private SetupViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    public SetupViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle, boolean providerSetup, Intent vpnPermissionRequest, Boolean showNotificationPermission) {
        this(fragmentManager, lifecycle);
        ArrayList<Integer> fragments = new ArrayList<>();
        if (providerSetup) {
            if (isDefaultBitmask()) {
                fragments.add(PROVIDER_SELECTION_FRAGMENT);
            }
            fragments.add(CIRCUMVENTION_SETUP_FRAGMENT);
        }

        if (vpnPermissionRequest != null) {
            fragments.add(VPN_PERMISSON_EDUCATIONAL_FRAGMENT);
            fragments.add(VPN_PERMISSON_FRAGMENT);
        }
        if (showNotificationPermission) {
            fragments.add(NOTIFICATION_PERMISSON_EDUCATIONAL_FRAGMENT);
            fragments.add(NOTIFICATION_PERMISSON_FRAGMENT);
        }
        if (providerSetup) {
            fragments.add(CONFIGURE_PROVIDER_FRAGMENT);
        }
        fragments.add(SUCCESS_FRAGMENT);
        setupFragmentFactory = new SetupFragmentFactory(fragments, vpnPermissionRequest);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return setupFragmentFactory.createFragment(position);
    }

    @Override
    public int getItemCount() {
        return setupFragmentFactory.getItemCount();
    }

    public int getFragmentPostion(int fragmentType) {
        return setupFragmentFactory.getPos(fragmentType);
    }


}
