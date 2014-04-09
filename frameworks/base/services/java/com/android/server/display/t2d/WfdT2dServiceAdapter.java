package com.mediatek.wfd.t2d;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pManager;

import com.mediatek.wfd.t2d.WfdT2dService;
import android.media.RemoteDisplay;

public class WfdT2dServiceAdapter {
    public interface Listener {
    	public static final int REJECT_T2D = 0;
    	public static final int ALLOW_T2D = 1;    	
    	public int onT2dRequestReceived();
    	public void onT2dConnectFail();
    	public void onT2dConnecting(String desiredMac);
    }
    
    public interface IWfdT2dService {
    	public int startFastRtsp(int emConfig);
    }
    
    public static IWfdT2dService createT2dService(Context context, WifiP2pManager manager, WifiP2pManager.Channel channel,
    		RemoteDisplay remoteDisplay,Listener listener) {
    	IWfdT2dService instance = new WfdT2dService(context,  manager, channel, remoteDisplay, listener);
    	return instance;
    }
}
