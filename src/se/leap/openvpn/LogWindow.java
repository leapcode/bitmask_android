package se.leap.openvpn;

import java.util.Vector;

import se.leap.bitmaskclient.R;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import se.leap.openvpn.OpenVPN.LogItem;
import se.leap.openvpn.OpenVPN.LogListener;
import se.leap.openvpn.OpenVPN.StateListener;

public class LogWindow extends ListActivity implements StateListener  {
	private static final int START_VPN_CONFIG = 0;
	private String[] mBconfig=null;


	class LogWindowListAdapter implements ListAdapter, LogListener, Callback {

		private static final int MESSAGE_NEWLOG = 0;

		private static final int MESSAGE_CLEARLOG = 1;

		private Vector<String> myEntries=new Vector<String>();

		private Handler mHandler;

		private Vector<DataSetObserver> observers=new Vector<DataSetObserver>();


		public LogWindowListAdapter() {
			initLogBuffer();

			if (mHandler == null) {
				mHandler = new Handler(this);
			}

			OpenVPN.addLogListener(this);
		}



		private void initLogBuffer() {
			myEntries.clear();
			for (LogItem litem : OpenVPN.getlogbuffer()) {
				myEntries.add(litem.getString(getContext()));				
			}
		}

		String getLogStr() {
			String str = "";
			for(String entry:myEntries) {
				str+=entry + '\n';
			}
			return str;
		}


		private void shareLog() {
			Intent shareIntent = new Intent(Intent.ACTION_SEND);
			shareIntent.putExtra(Intent.EXTRA_TEXT, getLogStr());
			shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.ics_openvpn_log_file));
			shareIntent.setType("text/plain");
			startActivity(Intent.createChooser(shareIntent, "Send Logfile"));
		}

		@Override
		public void registerDataSetObserver(DataSetObserver observer) {
			observers.add(observer);

		}

		@Override
		public void unregisterDataSetObserver(DataSetObserver observer) {
			observers.remove(observer);
		}

		@Override
		public int getCount() {
			return myEntries.size();
		}

		@Override
		public Object getItem(int position) {
			return myEntries.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView v;
			if(convertView==null)
				v = new TextView(getBaseContext());
			else
				v = (TextView) convertView;
			v.setText(myEntries.get(position));
			return v;
		}

		@Override
		public int getItemViewType(int position) {
			return 0;
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public boolean isEmpty() {
			return myEntries.isEmpty();

		}

		@Override
		public boolean areAllItemsEnabled() {
			return true;
		}

		@Override
		public boolean isEnabled(int position) {
			return true;
		}

		@Override
		public void newLog(LogItem logmessage) {
			Message msg = Message.obtain();
			msg.what=MESSAGE_NEWLOG;
			Bundle mbundle=new Bundle();
			mbundle.putString("logmessage", logmessage.getString(getBaseContext()));
			msg.setData(mbundle);
			mHandler.sendMessage(msg);
		}

		@Override
		public boolean handleMessage(Message msg) {
			// We have been called
			if(msg.what==MESSAGE_NEWLOG) {

				String logmessage = msg.getData().getString("logmessage");
				myEntries.add(logmessage);

				for (DataSetObserver observer : observers) {
					observer.onChanged();
				}
			} else if (msg.what == MESSAGE_CLEARLOG) {
				initLogBuffer();
				for (DataSetObserver observer : observers) {
					observer.onInvalidated();
				}
			} 

			return true;
		}

		void clearLog() {
			// Actually is probably called from GUI Thread as result of the user 
			// pressing a button. But better safe than sorry
			OpenVPN.clearLog();
			OpenVPN.logMessage(0,"","Log cleared.");
			mHandler.sendEmptyMessage(MESSAGE_CLEARLOG);
		}
	}



	private LogWindowListAdapter ladapter;
	private TextView mSpeedView;

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId()==R.id.clearlog) {
			ladapter.clearLog();
			return true;
		} else if(item.getItemId()==R.id.cancel){
			Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.title_cancel);
			builder.setMessage(R.string.cancel_connection_query);
			builder.setNegativeButton(android.R.string.no, null);
			builder.setPositiveButton(android.R.string.yes, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					ProfileManager.setConntectedVpnProfileDisconnected(getApplicationContext());
					OpenVpnManagementThread.stopOpenVPN();		
				}
			});

			builder.show();
			return true;
		} else if(item.getItemId()==R.id.info) {
			if(mBconfig==null)
				OpenVPN.triggerLogBuilderConfig();

		} else if(item.getItemId()==R.id.send) {
			ladapter.shareLog();
		} else if(item.getItemId()==R.id.edit_vpn) {
			VpnProfile lastConnectedprofile = ProfileManager.getLastConnectedVpn();

			if(lastConnectedprofile!=null) {
				Intent vprefintent = new Intent(this,VPNPreferences.class)
				.putExtra(VpnProfile.EXTRA_PROFILEUUID,lastConnectedprofile.getUUIDString());
				startActivityForResult(vprefintent,START_VPN_CONFIG);
			} else {
				Toast.makeText(this, R.string.log_no_last_vpn, Toast.LENGTH_LONG).show();
			}


		}

		return super.onOptionsItemSelected(item);

	}

	protected Context getContext() {
		return this;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.logmenu, menu);
		return true;
	}


	@Override
	protected void onResume() {
		super.onResume();
		OpenVPN.addStateListener(this);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == START_VPN_CONFIG && resultCode==RESULT_OK) {
			String configuredVPN = data.getStringExtra(VpnProfile.EXTRA_PROFILEUUID);

			final VpnProfile profile = ProfileManager.get(configuredVPN);
			ProfileManager.getInstance(this).saveProfile(this, profile);
			// Name could be modified, reset List adapter

			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			dialog.setTitle(R.string.configuration_changed);
			dialog.setMessage(R.string.restart_vpn_after_change);


			dialog.setPositiveButton(R.string.restart,
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent intent = new Intent(getBaseContext(), LaunchVPN.class);
					intent.putExtra(LaunchVPN.EXTRA_KEY, profile.getUUIDString());
					intent.setAction(Intent.ACTION_MAIN);
					startActivity(intent);
				}


			});
			dialog.setNegativeButton(R.string.ignore, null);
			dialog.create().show();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onStop() {
		super.onStop();
		OpenVPN.removeStateListener(this);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.logwindow);
		ListView lv = getListView();

		lv.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				ClipboardManager clipboard = (ClipboardManager)
						getSystemService(Context.CLIPBOARD_SERVICE);
				ClipData clip = ClipData.newPlainText("Log Entry",((TextView) view).getText());
				clipboard.setPrimaryClip(clip);
				Toast.makeText(getBaseContext(), R.string.copied_entry, Toast.LENGTH_SHORT).show();
				return true;
			}
		});

		ladapter = new LogWindowListAdapter();
		lv.setAdapter(ladapter);

		mSpeedView = (TextView) findViewById(R.id.speed);
	}

	@Override
	public void updateState(final String status,final String logmessage, final int resid) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				String prefix=getString(resid) + ":";
				if (status.equals("BYTECOUNT") || status.equals("NOPROCESS") )
					prefix="";
				mSpeedView.setText(prefix + logmessage);
			}
		});

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		OpenVPN.removeLogListener(ladapter);
	}

}
