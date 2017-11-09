package se.leap.bitmaskclient;

import android.app.*;
import android.content.pm.*;
import android.content.pm.PackageManager.*;
import android.os.*;
import android.widget.*;

public class AboutActivity extends Activity {

    final public static String TAG = "aboutFragment";
    final public static int VIEWED = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        TextView ver = (TextView) findViewById(R.id.version);

        String version;
        String name = "Bitmask";
        try {
            PackageInfo packageinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = packageinfo.versionName;
            name = getString(R.string.app);
        } catch (NameNotFoundException e) {
            version = "error fetching version";
        }


        ver.setText(getString(R.string.version_info, name, version));
        setResult(VIEWED);
    }

}
