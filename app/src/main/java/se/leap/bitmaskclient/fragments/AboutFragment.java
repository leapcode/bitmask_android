package se.leap.bitmaskclient.fragments;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import se.leap.bitmaskclient.R;

public class AboutFragment extends Fragment {

    final public static String TAG = "aboutFragment";
    final public static int VIEWED = 0;

    @InjectView(R.id.version)
    TextView versionTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.about, container, false);
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
    }

}
