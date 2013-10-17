package se.leap.openvpn;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.SwitchPreference;
import se.leap.bitmaskclient.R;


public class Settings_Authentication extends OpenVpnPreferencesFragment implements OnPreferenceChangeListener, OnPreferenceClickListener {
	private static final int SELECT_TLS_FILE = 23223232;
	private CheckBoxPreference mExpectTLSCert;
	private CheckBoxPreference mCheckRemoteCN;
	private EditTextPreference mRemoteCN;
	private ListPreference mTLSAuthDirection;
	private Preference mTLSAuthFile;
	private SwitchPreference mUseTLSAuth;
	private EditTextPreference mCipher;
	private String mTlsAuthFileData;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.vpn_authentification);

		mExpectTLSCert = (CheckBoxPreference) findPreference("remoteServerTLS");
		mCheckRemoteCN = (CheckBoxPreference) findPreference("checkRemoteCN");
		mRemoteCN = (EditTextPreference) findPreference("remotecn");
		mRemoteCN.setOnPreferenceChangeListener(this);
		
		mUseTLSAuth = (SwitchPreference) findPreference("useTLSAuth" );
		mTLSAuthFile = findPreference("tlsAuthFile");
		mTLSAuthDirection = (ListPreference) findPreference("tls_direction");
		
		
		mTLSAuthFile.setOnPreferenceClickListener(this);		
		
		mCipher =(EditTextPreference) findPreference("cipher");
		mCipher.setOnPreferenceChangeListener(this);
		
		loadSettings();

	}

	@Override
	protected void loadSettings() {
		
		mExpectTLSCert.setChecked(mProfile.mExpectTLSCert);
		mCheckRemoteCN.setChecked(mProfile.mCheckRemoteCN);
		mRemoteCN.setText(mProfile.mRemoteCN);
		onPreferenceChange(mRemoteCN, mProfile.mRemoteCN);
		
		mUseTLSAuth.setChecked(mProfile.mUseTLSAuth);
		mTlsAuthFileData= mProfile.mTLSAuthFilename;
		setTlsAuthSummary(mTlsAuthFileData);
		mTLSAuthDirection.setValue(mProfile.mTLSAuthDirection);
		mCipher.setText(mProfile.mCipher);
		onPreferenceChange(mCipher, mProfile.mCipher);
	}
	
	@Override
	protected void saveSettings() {
		mProfile.mExpectTLSCert=mExpectTLSCert.isChecked();
		mProfile.mCheckRemoteCN=mCheckRemoteCN.isChecked();
		mProfile.mRemoteCN=mRemoteCN.getText();
		
		mProfile.mUseTLSAuth = mUseTLSAuth.isChecked();
		mProfile.mTLSAuthFilename = mTlsAuthFileData;
		
		if(mTLSAuthDirection.getValue()==null)
			mProfile.mTLSAuthDirection=null;
		else
			mProfile.mTLSAuthDirection = mTLSAuthDirection.getValue().toString();
		
		if(mCipher.getText()==null)
			mProfile.mCipher=null;
		else
			mProfile.mCipher = mCipher.getText();
		
	}
	
	

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if(preference==mRemoteCN) {
			if ("".equals(newValue))
				preference.setSummary(mProfile.mServerName);
			else
				preference.setSummary((String)newValue);
		} else if (preference == mCipher) {
			preference.setSummary((CharSequence) newValue);
		}
		return true;
	}
	void startFileDialog() {
		Intent startFC = new Intent(getActivity(),FileSelect.class);
		startFC.putExtra(FileSelect.START_DATA, Environment.getExternalStorageDirectory().getPath());
	
		startActivityForResult(startFC,SELECT_TLS_FILE);
	}
	@Override
	public boolean onPreferenceClick(Preference preference) {
		startFileDialog();
		return true;
		
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode==SELECT_TLS_FILE && resultCode == Activity.RESULT_OK){
			   String result = data.getStringExtra(FileSelect.RESULT_DATA);
			   mTlsAuthFileData=result;
			   setTlsAuthSummary(result);
			
		}
	}

	private void setTlsAuthSummary(String result) {
		if(result==null) result = getString(R.string.no_certificate);
		if(result.startsWith(VpnProfile.INLINE_TAG))
			   mTLSAuthFile.setSummary(R.string.inline_file_data);
		   else
			   mTLSAuthFile.setSummary(result);
	}
}