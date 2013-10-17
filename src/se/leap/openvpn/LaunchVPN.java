/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package se.leap.openvpn;

import java.io.IOException;
import java.util.Collection;
import java.util.Vector;

import se.leap.bitmaskclient.ConfigHelper;
import se.leap.bitmaskclient.EIP;
import se.leap.bitmaskclient.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

/**
 * This Activity actually handles two stages of a launcher shortcut's life cycle.
 * 
 * 1. Your application offers to provide shortcuts to the launcher.  When
 *    the user installs a shortcut, an activity within your application
 *    generates the actual shortcut and returns it to the launcher, where it
 *    is shown to the user as an icon.
 *
 * 2. Any time the user clicks on an installed shortcut, an intent is sent.
 *    Typically this would then be handled as necessary by an activity within
 *    your application.
 *    
 * We handle stage 1 (creating a shortcut) by simply sending back the information (in the form
 * of an {@link android.content.Intent} that the launcher will use to create the shortcut.
 * 
 * You can also implement this in an interactive way, by having your activity actually present
 * UI for the user to select the specific nature of the shortcut, such as a contact, picture, URL,
 * media item, or action.
 * 
 * We handle stage 2 (responding to a shortcut) in this sample by simply displaying the contents
 * of the incoming {@link android.content.Intent}.
 * 
 * In a real application, you would probably use the shortcut intent to display specific content
 * or start a particular operation.
 */
public class LaunchVPN extends ListActivity implements OnItemClickListener {

	public static final String EXTRA_KEY = "se.leap.openvpn.shortcutProfileUUID";
	public static final String EXTRA_NAME = "se.leap.openvpn.shortcutProfileName";
	public static final String EXTRA_HIDELOG =  "se.leap.openvpn.showNoLogWindow";;

	public static final int START_VPN_PROFILE= 70;
	
	// Dashboard, maybe more, want to know!
	private ResultReceiver mReceiver;

	private ProfileManager mPM;
	private VpnProfile mSelectedProfile;
	private boolean mhideLog=false;

	private boolean mCmfixed=false;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		mPM =ProfileManager.getInstance(this);
	
	}	

	@Override
	protected void onStart() {
		super.onStart();
		// Resolve the intent

		final Intent intent = getIntent();
		final String action = intent.getAction();
		
		// If something wants feedback, they sent us a Receiver
		mReceiver = intent.getParcelableExtra(EIP.RECEIVER_TAG);
		
		// If the intent is a request to create a shortcut, we'll do that and exit

		
		if(Intent.ACTION_MAIN.equals(action)) {
			// we got called to be the starting point, most likely a shortcut
			String shortcutUUID = intent.getStringExtra( EXTRA_KEY);
			String shortcutName = intent.getStringExtra( EXTRA_NAME);
			mhideLog = intent.getBooleanExtra(EXTRA_HIDELOG, false);

			VpnProfile profileToConnect = ProfileManager.get(shortcutUUID);
			if(shortcutName != null && profileToConnect ==null)
				profileToConnect = ProfileManager.getInstance(this).getProfileByName(shortcutName);

			if(profileToConnect ==null) {
				OpenVPN.logError(R.string.shortcut_profile_notfound);
				// show Log window to display error
				showLogWindow();
				finish();
				return;
			}

			mSelectedProfile = profileToConnect;
			launchVPN();

		} else if (Intent.ACTION_CREATE_SHORTCUT.equals(action)) {
			createListView();
		}
	}

	private void createListView() {
		ListView lv = getListView();
		//lv.setTextFilterEnabled(true);

		Collection<VpnProfile> vpnlist = mPM.getProfiles();

		Vector<String> vpnnames=new Vector<String>();
		for (VpnProfile vpnProfile : vpnlist) {
			vpnnames.add(vpnProfile.mName);
		}



		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,vpnnames);
		lv.setAdapter(adapter);

		lv.setOnItemClickListener(this);
	}

	/**
	 * This function creates a shortcut and returns it to the caller.  There are actually two 
	 * intents that you will send back.
	 * 
	 * The first intent serves as a container for the shortcut and is returned to the launcher by 
	 * setResult().  This intent must contain three fields:
	 * 
	 * <ul>
	 * <li>{@link android.content.Intent#EXTRA_SHORTCUT_INTENT} The shortcut intent.</li>
	 * <li>{@link android.content.Intent#EXTRA_SHORTCUT_NAME} The text that will be displayed with
	 * the shortcut.</li>
	 * <li>{@link android.content.Intent#EXTRA_SHORTCUT_ICON} The shortcut's icon, if provided as a
	 * bitmap, <i>or</i> {@link android.content.Intent#EXTRA_SHORTCUT_ICON_RESOURCE} if provided as
	 * a drawable resource.</li>
	 * </ul>
	 * 
	 * If you use a simple drawable resource, note that you must wrapper it using
	 * {@link android.content.Intent.ShortcutIconResource}, as shown below.  This is required so
	 * that the launcher can access resources that are stored in your application's .apk file.  If 
	 * you return a bitmap, such as a thumbnail, you can simply put the bitmap into the extras 
	 * bundle using {@link android.content.Intent#EXTRA_SHORTCUT_ICON}.
	 * 
	 * The shortcut intent can be any intent that you wish the launcher to send, when the user 
	 * clicks on the shortcut.  Typically this will be {@link android.content.Intent#ACTION_VIEW} 
	 * with an appropriate Uri for your content, but any Intent will work here as long as it 
	 * triggers the desired action within your Activity.
	 * @param profile 
	 */
	private void setupShortcut(VpnProfile profile) {
		// First, set up the shortcut intent.  For this example, we simply create an intent that
		// will bring us directly back to this activity.  A more typical implementation would use a 
		// data Uri in order to display a more specific result, or a custom action in order to 
		// launch a specific operation.

		Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
		shortcutIntent.setClass(this, LaunchVPN.class);
		shortcutIntent.putExtra(EXTRA_KEY,profile.getUUID().toString());

		// Then, set up the container intent (the response to the caller)

		Intent intent = new Intent();
		intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, profile.getName());
		Parcelable iconResource = Intent.ShortcutIconResource.fromContext(
				this,  R.drawable.icon);
		intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);

		// Now, return the result to the launcher

		setResult(RESULT_OK, intent);
	}


	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		String profilename = ((TextView) view).getText().toString();

		VpnProfile profile = mPM.getProfileByName(profilename);

		// if (Intent.ACTION_CREATE_SHORTCUT.equals(action)) {	
		setupShortcut(profile);
		finish();
		return;
		//    }

	}

	

	private void askForPW(final int type) {

		final EditText entry = new EditText(this);
		entry.setSingleLine();
		entry.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		entry.setTransformationMethod(new PasswordTransformationMethod());

		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle("Need " + getString(type));
		dialog.setMessage("Enter the password for profile " + mSelectedProfile.mName);
		dialog.setView(entry);

		dialog.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String pw = entry.getText().toString();
				if(type == R.string.password) {
					mSelectedProfile.mTransientPW = pw;
				} else {
					mSelectedProfile.mTransientPCKS12PW = pw;
				}
				onActivityResult(START_VPN_PROFILE, Activity.RESULT_OK, null);

			}

		});
		dialog.setNegativeButton(android.R.string.cancel,
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		});

		dialog.create().show();

	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(requestCode==START_VPN_PROFILE) {
			if(resultCode == Activity.RESULT_OK) {
				int needpw = mSelectedProfile.needUserPWInput();
				if(needpw !=0) {
					askForPW(needpw);
				} else {
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);        
					boolean showlogwindow = prefs.getBoolean("showlogwindow", false);
					
					if(!mhideLog && showlogwindow)
						showLogWindow();
					new startOpenVpnThread().start();
				}
			} else if (resultCode == Activity.RESULT_CANCELED) {
				// User does not want us to start, so we just vanish (well, now we tell our receiver, then vanish)
				Bundle resultData = new Bundle();
				// For now, nothing else is calling, so this "request" string is good enough
				resultData.putString(EIP.REQUEST_TAG, EIP.ACTION_START_EIP);
				mReceiver.send(RESULT_CANCELED, resultData);
				finish();
			}
		}
	}
	void showLogWindow() {

		Intent startLW = new Intent(getBaseContext(),LogWindow.class);
		startLW.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(startLW);

	}

	void showConfigErrorDialog(int vpnok) {
		AlertDialog.Builder d = new AlertDialog.Builder(this);
		d.setTitle(R.string.config_error_found);
		d.setMessage(vpnok);
		d.setPositiveButton(android.R.string.ok, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();

			}
		});
		d.show();
	}

	void launchVPN () {
		int vpnok = mSelectedProfile.checkProfile(this);
		if(vpnok!= R.string.no_error_found) {
			showConfigErrorDialog(vpnok);
			return;
		}

		Intent intent = VpnService.prepare(this);
		// Check if we want to fix /dev/tun
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);        
		boolean usecm9fix = prefs.getBoolean("useCM9Fix", false);
		boolean loadTunModule = prefs.getBoolean("loadTunModule", false);

		if(loadTunModule)
			execeuteSUcmd("insmod /system/lib/modules/tun.ko");

		if(usecm9fix && !mCmfixed ) {
			execeuteSUcmd("chown system /dev/tun");
		}


		if (intent != null) {
			// Start the query
			try {
				startActivityForResult(intent, START_VPN_PROFILE);
			} catch (ActivityNotFoundException ane) {
				// Shame on you Sony! At least one user reported that 
				// an official Sony Xperia Arc S image triggers this exception
				OpenVPN.logError(R.string.no_vpn_support_image);
				showLogWindow();
			}
		} else {
			onActivityResult(START_VPN_PROFILE, Activity.RESULT_OK, null);
		}

	}

	private void execeuteSUcmd(String command) {
		ProcessBuilder pb = new ProcessBuilder(new String[] {"su","-c",command});
		try {
			Process p = pb.start();
			int ret = p.waitFor();
			if(ret ==0)
				mCmfixed=true;
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private class startOpenVpnThread extends Thread {

		@Override
		public void run() {
			VPNLaunchHelper.startOpenVpn(mSelectedProfile, getBaseContext());
			// Tell whom-it-may-concern that we started VPN
			Bundle resultData = new Bundle();
			// For now, nothing else is calling, so this "request" string is good enough
			resultData.putString(EIP.REQUEST_TAG, EIP.ACTION_START_EIP);
			mReceiver.send(RESULT_OK, resultData);
			finish();

		}

	}


}
