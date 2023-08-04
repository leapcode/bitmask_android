package se.leap.bitmaskclient.providersetup.fragments;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import se.leap.bitmaskclient.providersetup.activities.SetupActivityCallback;

public class BaseSetupFragment extends Fragment {

    SetupActivityCallback setupActivityCallback;
    private boolean callFragmentSelected = false;
    private final ViewPager2.OnPageChangeCallback viewPagerCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
            if (position == BaseSetupFragment.this.position) {
                handleCallFragmentSelected();
            } else {
                callFragmentSelected = false;
            }
        }
    };
    private final int position;

    public BaseSetupFragment(int position) {
        this.position = position;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (getActivity() instanceof SetupActivityCallback) {
            setupActivityCallback = (SetupActivityCallback) getActivity();
            setupActivityCallback.registerOnPageChangeCallback(viewPagerCallback);
            if (setupActivityCallback.getCurrentPosition() == position) {
                handleCallFragmentSelected();
            }
        } else {
            throw new IllegalStateException("These setup fragments are closely coupled to SetupActivityCallback interface. Activities instantiating them are required to implement the interface");
        }
    }

    private void handleCallFragmentSelected() {
        if (!callFragmentSelected) {
            callFragmentSelected = true;
            onFragmentSelected();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        setupActivityCallback.removeOnPageChangeCallback(viewPagerCallback);
        setupActivityCallback = null;
        callFragmentSelected = false;
    }

    public void onFragmentSelected() {

    }
}
