package se.leap.bitmaskclient.providersetup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import se.leap.bitmaskclient.providersetup.fragments.CircumventionSetupFragment;
import se.leap.bitmaskclient.providersetup.fragments.ConfigureProviderFragment;
import se.leap.bitmaskclient.providersetup.fragments.ProviderSelectionFragment;

public class SetupViewPagerAdapter extends FragmentStateAdapter {


    public SetupViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    public SetupViewPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    public SetupViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return ProviderSelectionFragment.newInstance();
            case 1:
                return CircumventionSetupFragment.newInstance();
            case 2:
                return ConfigureProviderFragment.newInstance(position);
            default:
                return ProviderSelectionFragment.newInstance();
        }
    }



    @Override
    public int getItemCount() {
        return 4;
    }


}
