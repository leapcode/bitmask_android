package se.leap.bitmaskclient;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.widget.TextView;

public class AboutActivity extends Activity  {
	
    final public static String TAG = "aboutFragment";
    final public static int VIEWED = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.about);
    	TextView ver = (TextView) findViewById(R.id.version);
    	
    	String version;
    	String name="Openvpn";
		try {
			PackageInfo packageinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			version = packageinfo.versionName;
			name = getString(R.string.app);
		} catch (NameNotFoundException e) {
			version = "error fetching version";
		}

    	
    	ver.setText(getString(R.string.version_info,name,version));
    	setResult(VIEWED);
    }

}
