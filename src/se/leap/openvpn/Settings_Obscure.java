package se.leap.openvpn;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import se.leap.bitmaskclient.R;

public class Settings_Obscure extends OpenVpnPreferencesFragment implements OnPreferenceChangeListener {
	private CheckBoxPreference mUseRandomHostName;
	private CheckBoxPreference mUseFloat;
	private CheckBoxPreference mUseCustomConfig;
	private EditTextPreference mCustomConfig;
	private ListPreference mLogverbosity;
	private CheckBoxPreference mPersistent;
	private ListPreference mConnectretrymax;
	private EditTextPreference mConnectretry;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.vpn_obscure);

		
		mUseRandomHostName = (CheckBoxPreference) findPreference("useRandomHostname");
		mUseFloat = (CheckBoxPreference) findPreference("useFloat");
		mUseCustomConfig = (CheckBoxPreference) findPreference("enableCustomOptions");
		mCustomConfig = (EditTextPreference) findPreference("customOptions");
		mLogverbosity = (ListPreference) findPreference("verblevel");
		mPersistent = (CheckBoxPreference) findPreference("usePersistTun");
		mConnectretrymax = (ListPreference) findPreference("connectretrymax");
		mConnectretry = (EditTextPreference) findPreference("connectretry");

		mLogverbosity.setOnPreferenceChangeListener(this);
		mLogverbosity.setSummary("%s");
		
		mConnectretrymax.setOnPreferenceChangeListener(this);
		mConnectretrymax.setSummary("%s");
		
		mConnectretry.setOnPreferenceChangeListener(this);
		
		
		loadSettings();

	}
	
	protected void loadSettings() {
		mUseRandomHostName.setChecked(mProfile.mUseRandomHostname);
		mUseFloat.setChecked(mProfile.mUseFloat);
		mUseCustomConfig.setChecked(mProfile.mUseCustomConfig);
		mCustomConfig.setText(mProfile.mCustomConfigOptions);
		mPersistent.setChecked(mProfile.mPersistTun);
		
		mLogverbosity.setValue(mProfile.mVerb);
		onPreferenceChange(mLogverbosity, mProfile.mVerb);
		
		mConnectretrymax.setValue(mProfile.mConnectRetryMax);
		onPreferenceChange(mConnectretrymax, mProfile.mConnectRetryMax);
				
		mConnectretry.setText(mProfile.mConnectRetry);
		onPreferenceChange(mConnectretry, mProfile.mConnectRetry);
	}


	protected void saveSettings() {
		mProfile.mUseRandomHostname = mUseRandomHostName.isChecked();
		mProfile.mUseFloat = mUseFloat.isChecked();
		mProfile.mUseCustomConfig = mUseCustomConfig.isChecked();
		mProfile.mCustomConfigOptions = mCustomConfig.getText();
		mProfile.mVerb = mLogverbosity.getValue();
		mProfile.mConnectRetryMax = mConnectretrymax.getValue();
		mProfile.mPersistTun = mPersistent.isChecked();
		mProfile.mConnectRetry = mConnectretry.getText();
	}

	
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if(preference==mLogverbosity) {
			// Catch old version problem
			if(newValue==null){
				newValue="1";
			}
			mLogverbosity.setDefaultValue(newValue);
			//This is idiotic. 
			int i =Integer.parseInt((String) newValue);
			
			// verb >= 5 is not supported by the chooser
			if(i < mLogverbosity.getEntries().length )
				mLogverbosity.setSummary(mLogverbosity.getEntries()[i]);
			else
				mLogverbosity.setSummary(String.format("debug verbosity: %d",i));
		} else if (preference == mConnectretrymax) {
			if(newValue==null) {
				newValue="5";
			}
			mConnectretrymax.setDefaultValue(newValue);
			
			for(int i=0;i<mConnectretrymax.getEntryValues().length;i++){
				if(mConnectretrymax.getEntryValues().equals(newValue))
					mConnectretrymax.setSummary(mConnectretrymax.getEntries()[i]);
			}
			
		} else if (preference == mConnectretry) {
			if(newValue==null || newValue=="")
				newValue="5";
			mConnectretry.setSummary(String.format("%s s" , newValue));
		}
			
		return true;
	}

}