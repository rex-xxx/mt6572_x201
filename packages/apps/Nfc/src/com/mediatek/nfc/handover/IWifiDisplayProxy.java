package com.mediatek.nfc.handover;

public interface IWifiDisplayProxy {

	int getRtspPortNumber();


/*	
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
	public IFastConnectInfo createDefaultFastConnectInfo();

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
	public IFastConnectInfo getFastConnectInfo(IFastConnectInfo info);
	public boolean isEnabled();
	int enable();
	int disable();
	int fastConnect(IFastConnectInfo info);
	int disconnect(); 
	public IWifiP2pProxy getInstance();
*/
	
}

