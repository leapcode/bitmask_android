package se.leap.bitmaskclient;

import android.app.Fragment;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import se.leap.bitmaskclient.R;

public class AboutFragment extends Fragment  {
	
    final public static String TAG = "aboutFragment";

	public static Fragment newInstance() {
		AboutFragment provider_detail_fragment = new AboutFragment();
		return provider_detail_fragment;
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
    		Bundle savedInstanceState) {
    	View v= inflater.inflate(R.layout.about, container, false);
    	TextView ver = (TextView) v.findViewById(R.id.version);
    	
    	String version;
    	String name="Openvpn";
		try {
			PackageInfo packageinfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
			version = packageinfo.versionName;
			name = getString(R.string.app);
		} catch (NameNotFoundException e) {
			version = "error fetching version";
		}

    	
    	ver.setText(getString(R.string.version_info,name,version));
    	
    	TextView translation = (TextView) v.findViewById(R.id.translation);
    	
    	// Don't print a text for myself
    	if ( getString(R.string.translationby).contains("Arne Schwabe"))
    		translation.setText("");
    	else
    		translation.setText(R.string.translationby);
    	return v;
    }

}
