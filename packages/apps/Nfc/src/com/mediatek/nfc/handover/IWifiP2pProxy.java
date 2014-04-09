package com.mediatek.nfc.handover;

public interface IWifiP2pProxy {
	public interface IFastConnectInfo {
		int getNetworkId();
		int setNetworkId(int id);
		int getVenderId();
		int setVenderId(int id);
		String getDeviceAddress();
		int setDeviceAddress(String address);
		String getSsid();
		int setSsid(String ssid);
		String getAuthType();
		int setAuthType(String authType);
		String getEncrType();
		int setEncrType(String encrType);
		String getPsk();
		int setPsk(String psk);
		String getGcIpAddress();
		int setGcIpAddress(String ip);
		String getGoIpAddress();
		int setGoIpAddress(String ip);
	}
	IFastConnectInfo createDefaultFastConnectInfo();

	public interface IWifiP2pDevice {
		String getDeviceAddress();
	}

	public interface WifiP2pProxyListener {
		public void onEnabled();
		public void onDisabled();
		public void onConnected(IWifiP2pDevice device);
		public void onDisconnected();
	}

	void addListener(WifiP2pProxyListener listener);
	IFastConnectInfo getFastConnectInfo(IFastConnectInfo info);
	int fastConnect(IFastConnectInfo info);
	boolean isEnabled();
	int enable();
	int disable();
	int disconnect(); 
}

