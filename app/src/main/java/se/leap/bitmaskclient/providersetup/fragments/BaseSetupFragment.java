package se.leap.bitmaskclient.providersetup.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import se.leap.bitmaskclient.providersetup.activities.SetupActivityCallback;

public class BaseSetupFragment extends Fragment {

    public static String EXTRA_POSITION = "EXTRA_POSITION";
    private boolean callFragmentSelected = false;
    SetupActivityCallback setupActivityCallback;

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
    private int position;

    public static Bundle initBundle(int pos) {
        Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_POSITION, pos);
        return bundle;
    }

    public BaseSetupFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.position = getArguments().getInt(EXTRA_POSITION);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (getActivity() instanceof SetupActivityCallback) {
            setupActivityCallback = (SetupActivityCallback) getActivity();
        } else {
            throw new IllegalStateException("These setup fragments are closely coupled to SetupActivityCallback interface. Activities instantiating them are required to implement the interface");
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(EXTRA_POSITION, position);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupActivityCallback.registerOnPageChangeCallback(viewPagerCallback);
        if (setupActivityCallback.getCurrentPosition() == position) {
            handleCallFragmentSelected();
        }
    }

    private void handleCallFragmentSelected() {
        if (!callFragmentSelected) {
            callFragmentSelected = true;
            onFragmentSelected();
        }
    }

    @Override
    public void onDestroyView() {
        setupActivityCallback.removeOnPageChangeCallback(viewPagerCallback);
        callFragmentSelected = false;
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        setupActivityCallback = null;
        super.onDetach();
    }

    public void onFragmentSelected() {

    }
}
