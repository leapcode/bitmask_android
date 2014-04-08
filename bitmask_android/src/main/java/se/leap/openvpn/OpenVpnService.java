package se.leap.openvpn;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Vector;

import se.leap.bitmaskclient.Dashboard;
import se.leap.bitmaskclient.R;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.VpnService;
import android.os.Binder;
import android.os.Handler.Callback;
import android.os.Build;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import se.leap.openvpn.OpenVPN.StateListener;

public class OpenVpnService extends VpnService implements StateListener, Callback {
	public static final String START_SERVICE = "se.leap.openvpn.START_SERVICE";
	public static final String RETRIEVE_SERVICE = "se.leap.openvpn.RETRIEVE_SERVICE";

	private Thread mProcessThread=null;

	private Vector<String> mDnslist=new Vector<String>();

	private VpnProfile mProfile;

	private String mDomain=null;

	private Vector<CIDRIP> mRoutes=new Vector<CIDRIP>();
	private Vector<String> mRoutesv6=new Vector<String>();

	private CIDRIP mLocalIP=null;

	private OpenVpnManagementThread mSocketManager;

	private Thread mSocketManagerThread;
	private int mMtu;
	private String mLocalIPv6=null;
	private NetworkSateReceiver mNetworkStateReceiver;
	private NotificationManager mNotificationManager;

	private boolean mDisplayBytecount=false;

	private boolean mStarting=false;

	private long mConnecttime;


	private static final int OPENVPN_STATUS = 1;

	public static final int PROTECT_FD = 0;

	private final IBinder mBinder = new LocalBinder();
	
	public class LocalBinder extends Binder {
		public OpenVpnService getService() {
            // Return this instance of LocalService so clients can call public methods
            return OpenVpnService.this;
        }
    }
	
	@Override
	public IBinder onBind(Intent intent) {
		String action = intent.getAction();
		if( action !=null && (action.equals(START_SERVICE) || action.equals(RETRIEVE_SERVICE)) )
			return mBinder;
		else
			return super.onBind(intent);
	}
	
	@Override
	public void onRevoke() {
		OpenVpnManagementThread.stopOpenVPN();
		endVpnService();
	}

	// Similar to revoke but do not try to stop process
	public void processDied() {
		endVpnService();
	}

	private void endVpnService() {
		mProcessThread=null;
		OpenVPN.logBuilderConfig(null);
		ProfileManager.setConntectedVpnProfileDisconnected(this);
		if(!mStarting) {
			stopSelf();
			stopForeground(true);
		}
	}

	private void showNotification(String state, String msg, String tickerText, boolean lowpriority, long when, boolean persistant) {
		String ns = Context.NOTIFICATION_SERVICE;
		mNotificationManager = (NotificationManager) getSystemService(ns);
		int icon;
		if (state.equals("NOPROCESS") || state.equals("AUTH_FAILED") || state.equals("NONETWORK") || state.equals("EXITING")){
			icon = R.drawable.ic_vpn_disconnected;
		}else{	
			icon = R.drawable.ic_stat_vpn;
		}
		
		android.app.Notification.Builder nbuilder = new Notification.Builder(this);

		nbuilder.setContentTitle(getString(R.string.notifcation_title,mProfile.mLocation));
		nbuilder.setContentText(msg);
		nbuilder.setOnlyAlertOnce(true);
		nbuilder.setOngoing(persistant);
		nbuilder.setContentIntent(getLogPendingIntent());
		nbuilder.setSmallIcon(icon);
		if(when !=0)
			nbuilder.setWhen(when);


		// Try to set the priority available since API 16 (Jellybean)
		jbNotificationExtras(lowpriority, nbuilder);
		if(tickerText!=null)
			nbuilder.setTicker(tickerText);

		@SuppressWarnings("deprecation")
		Notification notification = nbuilder.getNotification();


		mNotificationManager.notify(OPENVPN_STATUS, notification);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void jbNotificationExtras(boolean lowpriority,
			android.app.Notification.Builder nbuilder) {
		try {
			if(lowpriority) {
				Method setpriority = nbuilder.getClass().getMethod("setPriority", int.class);
				// PRIORITY_MIN == -2
				setpriority.invoke(nbuilder, -2 );

				nbuilder.setUsesChronometer(true);
				/*				PendingIntent cancelconnet=null;

				nbuilder.addAction(android.R.drawable.ic_menu_close_clear_cancel, 
						getString(R.string.cancel_connection),cancelconnet); */
			}

			//ignore exception
		} catch (NoSuchMethodException nsm) {
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		}

	}

	PendingIntent getLogPendingIntent() {
		// Let the configure Button show the Dashboard
		Intent intent = new Intent(Dashboard.getAppContext(),Dashboard.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent startLW = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		return startLW;

	}


	private LocalServerSocket openManagmentInterface(int tries) {
		// Could take a while to open connection
		String socketname = (getCacheDir().getAbsolutePath() + "/" +  "mgmtsocket");
		LocalSocket sock = new LocalSocket();

		while(tries > 0 && !sock.isConnected()) {
			try {
				sock.bind(new LocalSocketAddress(socketname,
						LocalSocketAddress.Namespace.FILESYSTEM));
			} catch (IOException e) {
				// wait 300 ms before retrying
				try { Thread.sleep(300);
				} catch (InterruptedException e1) {}

			} 
			tries--;
		}

		try {
			LocalServerSocket lss = new LocalServerSocket(sock.getFileDescriptor());
			return lss;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;


	}

	void registerNetworkStateReceiver() {
		// Registers BroadcastReceiver to track network connection changes.
		IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		mNetworkStateReceiver = new NetworkSateReceiver(mSocketManager);
		this.registerReceiver(mNetworkStateReceiver, filter);
	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {		
		
		if( intent != null && intent.getAction() !=null &&
				(intent.getAction().equals(START_SERVICE) || intent.getAction().equals(RETRIEVE_SERVICE)) )
			return START_NOT_STICKY;
			

		// Extract information from the intent.
		String prefix = getPackageName();
		String[] argv = intent.getStringArrayExtra(prefix + ".ARGV");
		String nativelibdir = intent.getStringExtra(prefix + ".nativelib");
		String profileUUID = intent.getStringExtra(prefix + ".profileUUID");

		mProfile = ProfileManager.get(profileUUID);

		//showNotification("Starting VPN " + mProfile.mName,"Starting VPN " + mProfile.mName, false,0);


		OpenVPN.addStateListener(this);

		// Set a flag that we are starting a new VPN
		mStarting=true;
		// Stop the previous session by interrupting the thread.
		if(OpenVpnManagementThread.stopOpenVPN()){
			// an old was asked to exit, wait 2s
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}

		if (mProcessThread!=null) {
			mProcessThread.interrupt();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
		// An old running VPN should now be exited
		mStarting=false;


		// Open the Management Interface
		LocalServerSocket mgmtsocket = openManagmentInterface(8);

		if(mgmtsocket!=null) {
			// start a Thread that handles incoming messages of the managment socket
			mSocketManager = new OpenVpnManagementThread(mProfile,mgmtsocket,this);
			mSocketManagerThread = new Thread(mSocketManager,"OpenVPNMgmtThread");
			mSocketManagerThread.start();
			OpenVPN.logInfo("started Socket Thread");
			registerNetworkStateReceiver();
		}


		// Start a new session by creating a new thread.
		OpenVPNThread processThread = new OpenVPNThread(this, argv,nativelibdir);

		mProcessThread = new Thread(processThread, "OpenVPNProcessThread");
		mProcessThread.start();

		ProfileManager.setConnectedVpnProfile(this, mProfile);

		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		if (mProcessThread != null) {
			mSocketManager.managmentCommand("signal SIGINT\n");

			mProcessThread.interrupt();
		}
		if (mNetworkStateReceiver!= null) {
			this.unregisterReceiver(mNetworkStateReceiver);
		}

	}



	public ParcelFileDescriptor openTun() {
		Builder builder = new Builder();

		if(mLocalIP==null && mLocalIPv6==null) {
			OpenVPN.logMessage(0, "", getString(R.string.opentun_no_ipaddr));
			return null;
		}

		if(mLocalIP!=null) {
			builder.addAddress(mLocalIP.mIp, mLocalIP.len);
		}

		if(mLocalIPv6!=null) {
			String[] ipv6parts = mLocalIPv6.split("/");
			builder.addAddress(ipv6parts[0],Integer.parseInt(ipv6parts[1]));
		}


		for (String dns : mDnslist ) {
			try {
				builder.addDnsServer(dns);
			} catch (IllegalArgumentException iae) {
				OpenVPN.logError(R.string.dns_add_error, dns,iae.getLocalizedMessage());
			}
		}


		builder.setMtu(mMtu);


		for (CIDRIP route:mRoutes) {
			try {
				builder.addRoute(route.mIp, route.len);
			} catch (IllegalArgumentException ia) {
				OpenVPN.logMessage(0, "", getString(R.string.route_rejected) + route + " " + ia.getLocalizedMessage());
			}
		}

		for(String v6route:mRoutesv6) {
			try {
				String[] v6parts = v6route.split("/");
				builder.addRoute(v6parts[0],Integer.parseInt(v6parts[1]));
			} catch (IllegalArgumentException ia) {
				OpenVPN.logMessage(0, "", getString(R.string.route_rejected) + v6route + " " + ia.getLocalizedMessage());
			}
		}

		if(mDomain!=null)
			builder.addSearchDomain(mDomain);

		String bconfig[] = new String[6];

		bconfig[0]=  getString(R.string.last_openvpn_tun_config);
		bconfig[1] = getString(R.string.local_ip_info,mLocalIP.mIp,mLocalIP.len,mLocalIPv6, mMtu);
		bconfig[2] = getString(R.string.dns_server_info, joinString(mDnslist));
		bconfig[3] = getString(R.string.dns_domain_info, mDomain);
		bconfig[4] = getString(R.string.routes_info, joinString(mRoutes));
		bconfig[5] = getString(R.string.routes_info6, joinString(mRoutesv6));

		String session = mProfile.mLocation;
		/* we don't want the IP address in the notification bar
		if(mLocalIP!=null && mLocalIPv6!=null)
			session = getString(R.string.session_ipv6string,session, mLocalIP, mLocalIPv6);
		else if (mLocalIP !=null)
			session= getString(R.string.session_ipv4string, session, mLocalIP);
		 */
		builder.setSession(session);


		OpenVPN.logBuilderConfig(bconfig);

		// No DNS Server, log a warning 
		if(mDnslist.size()==0)
			OpenVPN.logInfo(R.string.warn_no_dns);

		// Reset information
		mDnslist.clear();
		mRoutes.clear();
		mRoutesv6.clear();
		mLocalIP=null;
		mLocalIPv6=null;
		mDomain=null;

		builder.setConfigureIntent(getLogPendingIntent());

		try {
			ParcelFileDescriptor pfd = builder.establish();
			return pfd;
		} catch (Exception e) {
			OpenVPN.logMessage(0, "", getString(R.string.tun_open_error));
			OpenVPN.logMessage(0, "", getString(R.string.error) + e.getLocalizedMessage());
			OpenVPN.logMessage(0, "", getString(R.string.tun_error_helpful));
			return null;
		}

	}


	// Ugly, but java has no such method
	private <T> String joinString(Vector<T> vec) {
		String ret = "";
		if(vec.size() > 0){ 
			ret = vec.get(0).toString();
			for(int i=1;i < vec.size();i++) {
				ret = ret + ", " + vec.get(i).toString();
			}
		}
		return ret;
	}






	public void addDNS(String dns) {
		mDnslist.add(dns);		
	}


	public void setDomain(String domain) {
		if(mDomain==null) {
			mDomain=domain;
		}
	}


	public void addRoute(String dest, String mask) {
		CIDRIP route = new CIDRIP(dest, mask);		
		if(route.len == 32 && !mask.equals("255.255.255.255")) {
			OpenVPN.logMessage(0, "", getString(R.string.route_not_cidr,dest,mask));
		}

		if(route.normalise())
			OpenVPN.logMessage(0, "", getString(R.string.route_not_netip,dest,route.len,route.mIp));

		mRoutes.add(route);
	}

	public void addRoutev6(String extra) {
		mRoutesv6.add(extra);		
	}


	public void setLocalIP(String local, String netmask,int mtu, String mode) {
		mLocalIP = new CIDRIP(local, netmask);
		mMtu = mtu;

		if(mLocalIP.len == 32 && !netmask.equals("255.255.255.255")) {
			// get the netmask as IP
			long netint = CIDRIP.getInt(netmask);
			if(Math.abs(netint - mLocalIP.getInt()) ==1) {
				if(mode.equals("net30"))
					mLocalIP.len=30;
				else
					mLocalIP.len=31;
			} else {
				OpenVPN.logMessage(0, "", getString(R.string.ip_not_cidr, local,netmask,mode));
			}
		}
	}

	public void setLocalIPv6(String ipv6addr) {
		mLocalIPv6 = ipv6addr;
	}

	public boolean isRunning() {
		if (mStarting == true || mProcessThread != null)
			return true;
		else
			return false;
	}
	
	@Override
	public void updateState(String state,String logmessage, int resid) {
		// If the process is not running, ignore any state, 
		// Notification should be invisible in this state
		if(mProcessThread==null)
			return;	
		if("CONNECTED".equals(state)) {
			mNotificationManager.cancel(OPENVPN_STATUS);
		} else if(!"BYTECOUNT".equals(state)) {

			// Other notifications are shown,
			// This also mean we are no longer connected, ignore bytecount messages until next
			// CONNECTED
			String ticker = getString(resid);
			boolean persist = false;
			if (("NOPROCESS".equals(state) ) || ("EXITING").equals(state)){
				showNotification(state, getString(R.string.eip_state_not_connected), ticker, false, 0, persist);
			}
			else if (state.equals("GET_CONFIG") || state.equals("ASSIGN_IP")){ //don't show them in the notification message
			}
			else{
				persist = true;
				showNotification(state, getString(resid) +" " + logmessage,ticker,false,0,persist);
			}			 
		}
	}

	@Override
	public boolean handleMessage(Message msg) {
		Runnable r = msg.getCallback();
		if(r!=null){
			r.run();
			return true;
		} else {
			return false;
		}
	}
}
