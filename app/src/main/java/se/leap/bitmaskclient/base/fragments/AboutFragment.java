package se.leap.bitmaskclient.base.fragments;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.Fragment;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import se.leap.bitmaskclient.BuildConfig;
import se.leap.bitmaskclient.R;

import static android.view.View.VISIBLE;
import static se.leap.bitmaskclient.R.string.about_fragment_title;
import static se.leap.bitmaskclient.base.utils.ViewHelper.setActionBarTitle;

public class AboutFragment extends Fragment {

    final public static String TAG = AboutFragment.class.getSimpleName();
    final public static int VIEWED = 0;
    private Unbinder unbinder;

    @BindView(R.id.version)
    AppCompatTextView versionTextView;

    @BindView(R.id.terms_of_service)
    AppCompatTextView termsOfService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_about, container, false);
        unbinder = ButterKnife.bind(this, view);
        setActionBarTitle(this, about_fragment_title);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
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

        versionTextView.setText(getString(R.string.version_info, name, version));

        if (BuildConfig.FLAVOR_branding.equals("custom") && hasTermsOfServiceResource()) {
            termsOfService.setText(getString(getTermsOfServiceResource()));
            termsOfService.setVisibility(VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    private boolean hasTermsOfServiceResource() {
        return getTermsOfServiceResource() != 0;
    }

    private int getTermsOfServiceResource() {
        return this.getContext().getResources().getIdentifier("terms_of_service", "string", this.getContext().getPackageName());
    }

}
