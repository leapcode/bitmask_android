package se.leap.openvpn;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import se.leap.bitmaskclient.R;


public class Settings_Routing extends OpenVpnPreferencesFragment implements OnPreferenceChangeListener {
	private EditTextPreference mCustomRoutes;
	private CheckBoxPreference mUseDefaultRoute;
	private EditTextPreference mCustomRoutesv6;
	private CheckBoxPreference mUseDefaultRoutev6;
	private CheckBoxPreference mRouteNoPull;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.vpn_routing);
		mCustomRoutes = (EditTextPreference) findPreference("customRoutes");
		mUseDefaultRoute = (CheckBoxPreference) findPreference("useDefaultRoute");
		mCustomRoutesv6 = (EditTextPreference) findPreference("customRoutesv6");
		mUseDefaultRoutev6 = (CheckBoxPreference) findPreference("useDefaultRoutev6");
		mRouteNoPull = (CheckBoxPreference) findPreference("routenopull");

		mCustomRoutes.setOnPreferenceChangeListener(this);
		mCustomRoutesv6.setOnPreferenceChangeListener(this);

		loadSettings();
	}

	@Override
	protected void loadSettings() {

		mUseDefaultRoute.setChecked(mProfile.mUseDefaultRoute);
		mUseDefaultRoutev6.setChecked(mProfile.mUseDefaultRoutev6);

		mCustomRoutes.setText(mProfile.mCustomRoutes);
		mCustomRoutesv6.setText(mProfile.mCustomRoutesv6);

		mRouteNoPull.setChecked(mProfile.mRoutenopull);

		// Sets Summary
		onPreferenceChange(mCustomRoutes, mCustomRoutes.getText());
		onPreferenceChange(mCustomRoutesv6, mCustomRoutesv6.getText());
		mRouteNoPull.setEnabled(mProfile.mUsePull);
	}


	@Override
	protected void saveSettings() {
		mProfile.mUseDefaultRoute = mUseDefaultRoute.isChecked();
		mProfile.mUseDefaultRoutev6 = mUseDefaultRoutev6.isChecked();
		mProfile.mCustomRoutes = mCustomRoutes.getText();
		mProfile.mCustomRoutesv6 = mCustomRoutesv6.getText();
		mProfile.mRoutenopull = mRouteNoPull.isChecked();
	}

	@Override
	public boolean onPreferenceChange(Preference preference,
			Object newValue) {
		if(	 preference == mCustomRoutes || preference == mCustomRoutesv6 )
			preference.setSummary((String)newValue);

		saveSettings();
		return true;
	}


}