package se.leap.bitmaskclient.fragments;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import se.leap.bitmaskclient.BuildConfig;
import se.leap.bitmaskclient.R;

import static android.view.View.VISIBLE;

public class AboutFragment extends Fragment {

    final public static String TAG = AboutFragment.class.getSimpleName();
    final public static int VIEWED = 0;

    @InjectView(R.id.version)
    TextView versionTextView;

    @InjectView(R.id.terms_of_service)
    TextView termsOfService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_about, container, false);
        ButterKnife.inject(this, view);
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

    private boolean hasTermsOfServiceResource() {
        return getTermsOfServiceResource() != 0;
    }

    private int getTermsOfServiceResource() {
        return this.getContext().getResources().getIdentifier("terms_of_service", "string", this.getContext().getPackageName());
    }

}
