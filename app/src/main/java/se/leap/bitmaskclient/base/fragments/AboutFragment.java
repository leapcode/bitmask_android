package se.leap.bitmaskclient.base.fragments;

import static android.view.View.VISIBLE;
import static se.leap.bitmaskclient.R.string.about_fragment_title;
import static se.leap.bitmaskclient.base.utils.ViewHelper.setActionBarSubtitle;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import se.leap.bitmaskclient.BuildConfig;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.databinding.FAboutBinding;

public class AboutFragment extends Fragment {

    final public static String TAG = AboutFragment.class.getSimpleName();
    final public static int VIEWED = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FAboutBinding binding = FAboutBinding.inflate(inflater);
        setActionBarSubtitle(this, about_fragment_title);

        String version;
        String name = "Bitmask";
        try {
            PackageInfo packageinfo = getActivity().getPackageManager().getPackageInfo(
                    getActivity().getPackageName(), 0);
            version = packageinfo.versionName;
            name = getString(R.string.app_name);
        } catch (NameNotFoundException e) {
            version = "error fetching version";
        }

        binding.version.setText(getString(R.string.version_info, name, version));

        if (BuildConfig.FLAVOR_branding.equals("custom") && hasTermsOfServiceResource()) {
            binding.termsOfService.setText(getString(getTermsOfServiceResource()));
            binding.termsOfService.setVisibility(VISIBLE);
        }
        return binding.getRoot();
    }

    private boolean hasTermsOfServiceResource() {
        return getTermsOfServiceResource() != 0;
    }

    private int getTermsOfServiceResource() {
        return this.getContext().getResources().getIdentifier("terms_of_service", "string", this.getContext().getPackageName());
    }

}
