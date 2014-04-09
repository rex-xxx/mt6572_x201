package com.mediatek.nfc.handover;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.p2p.fastconnect.WifiP2pFastConnectInfo;
//import android.net.wifi.p2p.wfd.WfdInfo;
import android.util.Log;

/**
 * The adapter class of IWifiP2pProxy
 * 
 */
public class WifiP2pProxy implements IWifiP2pProxy {

	/**
	 * Observe the Group Info
	 * 
	 */
	private class GroupInfoObserver implements GroupInfoListener {

		/**
		 * Constructor
		 * 
		 * @param isOwner
		 */
		public GroupInfoObserver(boolean isOwner) {
			mIsOwner = isOwner;
		}

		// Is Owner
		private boolean mIsOwner;

		/**
		 * On Group Info Available
		 * 
		 * @param group
		 */
		@Override
		public void onGroupInfoAvailable(WifiP2pGroup group) {
			
			if (mIsOwner) {
				if (group.getClientList() == null
						|| group.getClientList().size() == 0) {

					Log.e(TAG, "onGroupInfoAvailable(), no client is connected");
					return;
				}				

				Log.e(TAG, "onGroupInfoAvailable(), Time is " + System.currentTimeMillis());
				for (WifiP2pDevice device : group.getClientList()) {				
					for (WifiP2pProxyListener listener : mListeners) {
						listener.onConnected(new TheWifiP2pDevice(device));
					}
				}

			} else {

				WifiP2pDevice device = group.getOwner();
				if (device == null) {
					Log.e(TAG, "onGroupInfoAvailable(), group owner is null");
					return;
				}
				
				// Dispatch Event
				Log.e(TAG, "onGroupInfoAvailable(), Time is " + System.currentTimeMillis());
				for (WifiP2pProxyListener listener : mListeners) {
					listener.onConnected(new TheWifiP2pDevice(device));
				}
			}
			
		}

	}

	/**
	 * Wrapper class of interface IFastConnectInfo
	 * 
	 */
	private class TheFastConnectInfo implements IWifiP2pProxy.IFastConnectInfo {

		private WifiP2pFastConnectInfo mInfo;

		public TheFastConnectInfo(WifiP2pFastConnectInfo info) {
			mInfo = info;
		}

		public int getNetworkId() {
			return mInfo.networkId;
		}

		public int setNetworkId(int id) {
			mInfo.networkId = id;
			return 0;
		}

		public int getVenderId() {
			return mInfo.venderId;
		}

		public int setVenderId(int id) {
			mInfo.venderId = id;
			return 0;
		}

		public String getDeviceAddress() {
			return mInfo.deviceAddress;
		}

		public int setDeviceAddress(String addr) {
			mInfo.deviceAddress = addr;
			return 0;
		}

		public String getSsid() {
			return mInfo.ssid;
		}

		public int setSsid(String ssid) {
			mInfo.ssid = ssid;
			return 0;
		}

		public String getAuthType() {
			return mInfo.authType;
		}

		public int setAuthType(String authType) {
			mInfo.authType = authType;
			return 0;
		}

		public String getEncrType() {
			return mInfo.encrType;
		}

		public int setEncrType(String encrType) {
			mInfo.encrType = encrType;
			return 0;
		}

		public String getPsk() {
			return mInfo.psk;
		}

		public int setPsk(String psk) {
			mInfo.psk = psk;
			return 0;
		}

		public String getGcIpAddress() {
			return mInfo.gcIpAddress;
		}

		public int setGcIpAddress(String ip) {
			mInfo.gcIpAddress = ip;
			return 0;
		}

		public String getGoIpAddress() {
			return mInfo.goIpAddress;
		}

		public int setGoIpAddress(String ip) {
			mInfo.goIpAddress = ip;
			return 0;
		}
	}

	/**
	 * Wrapper class of the interface IWifiP2pDevice
	 * 
	 * @author vend_iii08
	 * 
	 */
	private class TheWifiP2pDevice implements IWifiP2pProxy.IWifiP2pDevice {

		private WifiP2pDevice mDevice;

		public TheWifiP2pDevice(WifiP2pDevice device) {
			mDevice = device;
		}

		public String getDeviceAddress() {
			return mDevice.deviceAddress;
		}
	}

	// TAG
	private static final String TAG = "WifiP2pProxy";
	// Is Power enable
	private boolean mIsEnabled;
	// Android Context
	private Context mContext;
	// This device address
	private String mThisDeviceAddress;
	// Android WifiManager(System)
	private WifiManager mWifiManager;
	// Android WifiP2pManager(System)
	private WifiP2pManager mWifiP2pManager;
	// Android WifiP2pManager.Channel(System)
	private WifiP2pManager.Channel mChannel;
	// Intent filter for receive the WiFi event broadcast
	private IntentFilter mFilter = new IntentFilter();
	// WifiP2pProxy Listener Pool
	private List<WifiP2pProxyListener> mListeners = new ArrayList<WifiP2pProxyListener>();
	// Cached fast connect info (GO)
	private WifiP2pFastConnectInfo mCachedFastConnectInfo;
	
    private static final String PREF = "NfcServicePrefs";//borrow from NfcService
    private static final String PREF_WIFI_MAC = "beamplus.wifi.mac.address";
    private SharedPreferences mPrefs;
    private SharedPreferences.Editor mPrefsEditor;
	
	// Peer Listener
	// private MyPeerListener mPeerListener = new MyPeerListener();

	public WifiP2pProxy(Context context) {

		mContext = context;
        mPrefs = mContext.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        mPrefsEditor = mPrefs.edit();

		mWifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		mWifiP2pManager = (WifiP2pManager) context
				.getSystemService(Context.WIFI_P2P_SERVICE);
		mChannel = mWifiP2pManager.initialize(mContext,
				mContext.getMainLooper(), null);
		if (mChannel == null) {
			Log.d(TAG,
					"FATAL ERROR : Failed to set up connection with wifi p2p service");
			mWifiP2pManager = null;// let it crash
		}

		mFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		mFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
		mFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		mFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		mFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
		mContext.registerReceiver(mReceiver, mFilter);
	}

	public IFastConnectInfo createDefaultFastConnectInfo() {
		TheFastConnectInfo info = new TheFastConnectInfo(
				new WifiP2pFastConnectInfo());
        String address = mPrefs.getString(PREF_WIFI_MAC, "null");
        Log.d(TAG, "address from Preference: " + address);
		if (address.equals("null")) {
			Log.d(TAG, "FATAL ERROR : not get this device address yet");
            address = null;
		}
		info.setDeviceAddress(address);
		info.setAuthType("0x0020");//set default value to pass parsing 
		info.setEncrType("0x0008");//set default value to pass parsing 
		return info;
	}

	public IFastConnectInfo getFastConnectInfo(IFastConnectInfo info) {
		WifiP2pFastConnectInfo infoParam = new WifiP2pFastConnectInfo();
		infoParam.deviceAddress = info.getDeviceAddress().toLowerCase();
		mCachedFastConnectInfo = mWifiP2pManager
				.fastConnectAsGo(infoParam);
		return new TheFastConnectInfo(mCachedFastConnectInfo);
	}
	
	public int fastConnect(IFastConnectInfo info) {
        Log.d(TAG, "fastConnect()");
		WifiP2pFastConnectInfo param = new WifiP2pFastConnectInfo();
		param.networkId = info.getNetworkId();
		param.venderId = info.getVenderId();
		param.deviceAddress = info.getDeviceAddress().toLowerCase();
		param.ssid = info.getSsid();
		param.authType = info.getAuthType();
		param.encrType = info.getEncrType();
		param.psk = info.getPsk();
		param.gcIpAddress = info.getGcIpAddress();
		mWifiP2pManager.fastConnectAsGc(mChannel, param, null);
		return 0;
	}

	public void addListener(WifiP2pProxyListener listener) {
		mListeners.add(listener);
	}

	public boolean isEnabled() {
		return mIsEnabled;
	}

	public int enable() {
		mWifiManager.setWifiEnabled(true);
		return 0;
	}

	public int disable() {
		mWifiManager.setWifiEnabled(false);
		return 0;
	}

	public int disconnect() {
		mWifiP2pManager.removeGroup(mChannel, null);
		return 0;
	}

	/**
	 * Handle WIFI_P2P_CONNECTION_CHANGED_ACTION
	 * 
	 * @param intent
	 */
	private void handleConnectionChange(Intent intent) {

		Log.d(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION");
		WifiP2pInfo info = intent
				.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);

		if (info.groupFormed) {
			
			Log.d(TAG, "handleConnectionChange(), ===> Group Formed");
			mWifiP2pManager.requestGroupInfo(mChannel, new GroupInfoObserver(
					info.isGroupOwner));

		} else {
			
			Log.d(TAG, "handleConnectionChange(), ===> Group removed");
			for (WifiP2pProxyListener listener : mListeners) {
				listener.onDisconnected();
			}
		}
	}

	/**
	 * Handle WIFI_P2P_THIS_DEVICE_CHANGED_ACTION
	 * 
	 * @param intent
	 */
	private void handleThisDeviceChange(Intent intent) {

		Log.d(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
		WifiP2pDevice thisDevice = intent
				.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
		Log.d(TAG, describeWifiP2pDevice(thisDevice));
		mThisDeviceAddress = thisDevice.deviceAddress;
        mPrefsEditor.putString(PREF_WIFI_MAC, mThisDeviceAddress).apply();
	}

	/**
	 * Handle WIFI_P2P_DISCOVERY_CHANGED_ACTION
	 * 
	 * @param intent
	 */
	private void handleDiscoveryChange(Intent intent) {

		Log.d(TAG, "WIFI_P2P_DISCOVERY_CHANGED_ACTION");
		int state = intent
				.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1);
		switch (state) {
		case WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED:
			Log.d(TAG, "WIFI_P2P_DISCOVERY_STARTED");
			break;
		case WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED:
			Log.d(TAG, "WIFI_P2P_DISCOVERY_STOPPED");
			break;
		default:
			Log.d(TAG, "incorrect EXTRA_DISCOVERY_STATE = " + state);
			break;
		}
	}

	/**
	 * Handle WIFI_P2P_STATE_CHANGED_ACTION
	 * 
	 * @param intent
	 * 
	 */
	private void handleStateChange(Intent intent) {

		Log.d(TAG, "WIFI_P2P_STATE_CHANGED_ACTION");
		int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
		switch (state) {
		case WifiP2pManager.WIFI_P2P_STATE_ENABLED:
			Log.d(TAG, "===> ENABLED");
			mIsEnabled = true;
			for (WifiP2pProxyListener listener : mListeners) {
				listener.onEnabled();
			}
			break;
		case WifiP2pManager.WIFI_P2P_STATE_DISABLED:
			Log.d(TAG, "===> DISABLED");
			mIsEnabled = false;
			for (WifiP2pProxyListener listener : mListeners) {
				listener.onDisabled();
			}
			break;
		default:
			Log.d(TAG, "incorrect EXTRA_WIFI_STATE = " + state);
			break;
		}
	}

	/**
	 * Handle WIFI_P2P_PEERS_CHANGED_ACTION
	 * 
	 * @param intent
	 */
	private void handlePeerChange(Intent intent) {

		Log.d(TAG, "WIFI_P2P_PEERS_CHANGED_ACTION");
		mWifiP2pManager.requestPeers(mChannel, new PeerListListener() {

			@Override
			public void onPeersAvailable(WifiP2pDeviceList peers) {

				Log.d(TAG, "onPeersAvailable(),started");
				if (peers.getDeviceList() == null
						|| peers.getDeviceList().size() == 0) {

					Log.d(TAG, "peer list has been flushed");
					return;
				}

				for (WifiP2pDevice device : peers.getDeviceList()) {
					Log.d(TAG, describeWifiP2pDevice(device));
				}
			}
		});
	}

	/**
	 * BroadcastReceiver Class
	 */
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			String action = intent.getAction();

			if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
				handleStateChange(intent);
			} else if (WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION
					.equals(action)) {
				handleDiscoveryChange(intent);
			} else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION
					.equals(action)) {
				handlePeerChange(intent);
			} else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION
					.equals(action)) {
				handleConnectionChange(intent);
			} else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION
					.equals(action)) {
				handleThisDeviceChange(intent);
			}
		}
	};

	// ////////////////////////////////////////
	// utilities
	// ////////////////////////////////////////
	private String describeWifiP2pDevice(WifiP2pDevice device) {
		String status = "[]";
		switch (device.status) {
		case WifiP2pDevice.AVAILABLE:
			status = "[AVAILABLE]";
			break;
		case WifiP2pDevice.CONNECTED:
			status = "[CONNECTED]";
			break;
		case WifiP2pDevice.INVITED:
			status = "[INVITED]";
			break;
		case WifiP2pDevice.FAILED:
			status = "[FAILED]";
			break;
		case WifiP2pDevice.UNAVAILABLE:
			status = "[UNAVAILABLE]";
			break;
		}
		return "===> " + status + " " + device.deviceName + "@"
				+ device.deviceAddress;
	}

}
