package com.mediatek.wfd.t2d;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Slog;

import android.net.wifi.p2p.fastconnect.WifiP2pFastConnectInfo;
import android.media.RemoteDisplay;
import com.mediatek.wfd.t2d.WfdT2dServiceAdapter.*;

public class WfdT2dService 
	implements IWfdT2dService {
	private static final String TAG = "WfdT2dService";
	
	static final String NFC_HANDOVER_INTENT_ACTION_WFD_ACTIVE =
            "mediatek.nfc.handover.intent.action.WFD_ACTIVE";

    public static final String EXTRA_NFC_WFD_MAC_ADDR =
            "mediatek.nfc.handover.intent.extra.WFD_MAC_ADDR";

    public static final String EXTRA_NFC_WFD_SSID =
            "mediatek.nfc.handover.intent.extra.WFD_SSID";
    
    public static final String EXTRA_NFC_WFD_NETWORK_KEY =
            "mediatek.nfc.handover.intent.extra.WFD_NETWORK_KEY";

    public static final String EXTRA_NFC_WFD_NETWORK_ID =
            "mediatek.nfc.handover.intent.extra.WFD_NETWORK_ID";

    public static final String EXTRA_NFC_WFD_AUTH_TYPE =
            "mediatek.nfc.handover.intent.extra.WFD_AUTH_TYPE";

    public static final String EXTRA_NFC_WFD_ENC_TYPE =
            "mediatek.nfc.handover.intent.extra.WFD_ENC_TYPE";    

    public static final String EXTRA_NFC_WFD_VENDOR_ID =
            "mediatek.nfc.handover.intent.extra.WFD_VENDOR_ID";

    public static final String EXTRA_NFC_WFD_GC_IP =
            "mediatek.nfc.handover.intent.extra.WFD_GC_IP";

    public static final String EXTRA_NFC_WFD_GO_IP =
            "mediatek.nfc.handover.intent.extra.WFD_GO_IP";

    public static final String EXTRA_NFC_WFD_MAX_HEIGHT =
            "mediatek.nfc.handover.intent.extra.WFD_MAX_HEIGHT";	

    public static final String EXTRA_NFC_WFD_MAX_WIDTH =
            "mediatek.nfc.handover.intent.extra.WFD_MAX_WIDTH";
    
    public int startFastRtsp(int emConfig) {
    	Log.d(TAG, "startFastRtsp: emConfig=0x" + Integer.toHexString(emConfig));
    	// kenping, Fix-Me!!
    	//return mRemoteDisplay.startServerEx(127, 0, 0, 1, 7236, (emConfig | 0x01));
    	return 1;
    }
    
    private Context mContext;
    private Listener mListener;
	private WifiP2pManager mWifiP2pManager;
	private WifiP2pManager.Channel mChannel;
	private IntentFilter mFilter = new IntentFilter();
	private String mNull;
	private RemoteDisplay mRemoteDisplay;
    private WifiP2pFastConnectInfo mFastInfo;
    private byte[] mGcIp;

    public WfdT2dService(Context context, WifiP2pManager manager, WifiP2pManager.Channel channel,
    		RemoteDisplay remoteDisplay, Listener listener) {
    	mContext = context;
    	mListener = listener;
    	mWifiP2pManager = manager;
    	mChannel = channel;
    	mRemoteDisplay = remoteDisplay;
		mFilter.addAction(NFC_HANDOVER_INTENT_ACTION_WFD_ACTIVE);
		mContext.registerReceiver(mReceiver, mFilter);
    }
    
    private void crashMe() {
    	Slog.d(TAG, "crash me!!");
    	Slog.d(TAG, "this will crash" + mNull.length());
    }
    
    private String convertDeviceAddress(byte[] macAddr) {
    	if (macAddr == null || macAddr.length != 6) {
    		Slog.d(TAG, "convertDevcieAddress() fail, macAddr wrong");
    		return null;
    	}
    	final String hex[] = {"a", "b", "c", "d", "e", "f"};
    	String out = "";
    	for (int i=0; i < macAddr.length; i++) {
    		int q, r, v;
    		v = (int) macAddr[i] & 0xff;
    		q = v / 16;
    		r = v - q * 16;
    		out += (q < 10) ? q : hex[q - 10];
    		out += (r < 10) ? r : hex[r - 10];
    		if (i != 5) {
    			out += ":";
    		}
    	}
    	return out;
    }
    
    private String convertIp(byte[] ByteArray) {
    	int separateLength = 3;

   		if(ByteArray.length != 4) {
			Slog.e(TAG, " IP Address length not match :" + ByteArray.length);
   		}
       		
        StringBuilder sb = new StringBuilder(ByteArray.length + separateLength);
		
        for (int i=0;i< ByteArray.length;i++) {
            if (sb.length() > 0)
                sb.append('.');
            sb.append(String.format("%d", (int) ByteArray[ByteArray.length-1-i] & 0xFF));
        }
        	
        return sb.toString();
	}

	private String convertTo4DigitsHexString(short encType) {
		String ret = "0x";
		ret += String.format("%04X", encType);
		return ret;
	}

    private WifiP2pFastConnectInfo constructFastConnectInfo(Intent intent) {
    	byte[] macAddr = intent.getByteArrayExtra(EXTRA_NFC_WFD_MAC_ADDR);
    	String NetworkKey = intent.getStringExtra(EXTRA_NFC_WFD_NETWORK_KEY);
    	String SSID = intent.getStringExtra(EXTRA_NFC_WFD_SSID);
    	byte NetworkId = intent.getByteExtra(EXTRA_NFC_WFD_NETWORK_ID, (byte)0);
    	short AuthType = intent.getShortExtra(EXTRA_NFC_WFD_AUTH_TYPE, (short)0);
    	short EncType = intent.getShortExtra(EXTRA_NFC_WFD_ENC_TYPE, (short)0);
    	byte[] vendorId = intent.getByteArrayExtra(EXTRA_NFC_WFD_VENDOR_ID);
    	byte[] gcIp = intent.getByteArrayExtra(EXTRA_NFC_WFD_GC_IP);
    	byte[] goIp = intent.getByteArrayExtra(EXTRA_NFC_WFD_GO_IP);
    	byte[] maxHeight = intent.getByteArrayExtra(EXTRA_NFC_WFD_MAX_HEIGHT);
    	byte[] maxWidth = intent.getByteArrayExtra(EXTRA_NFC_WFD_MAX_WIDTH);
    	WifiP2pFastConnectInfo info = new WifiP2pFastConnectInfo();
    	info.networkId = (int)NetworkId;
    	info.venderId = -1;//(int)vendorId[0];
    	info.deviceAddress = convertDeviceAddress(macAddr);//from byte[] to String
    	Slog.d(TAG, "deviceAddress: " + info.deviceAddress);
    	info.ssid = SSID;
    	info.authType = convertTo4DigitsHexString(AuthType);//from short to String
    	Slog.d(TAG, "authType: " + info.authType);
    	info.encrType = convertTo4DigitsHexString(EncType);//from short to String
    	Slog.d(TAG, "encrType: " + info.encrType);
    	info.psk = NetworkKey;
        mGcIp = gcIp;
    	info.gcIpAddress = convertIp(gcIp);//from byte[] to String
    	Slog.d(TAG, "gcIp: " + info.gcIpAddress);
    	info.goIpAddress = convertIp(goIp);//from byte[] to String
    	Slog.d(TAG, "goIp: " + info.goIpAddress);
    	Slog.d(TAG, "WifiP2pFastConnectInfo: " + info);
		return info;    	
    }
    
	private boolean doFastConnection(WifiP2pFastConnectInfo info) {
		mWifiP2pManager.fastConnectAsGc(mChannel, info, null);
    	return true;
    }
    
    private void handleBroadcastT2d(Intent intent) {
		Slog.d(TAG, "NFC_HANDOVER_INTENT_ACTION_WFD_ACTIVE");
    	int condition = mListener.onT2dRequestReceived();
    	if (Listener.REJECT_T2D == condition) {
    		Slog.d(TAG, "REJECT_T2D");
    	} else if (Listener.ALLOW_T2D == condition) {
    		Log.d(TAG, "ALLOW_T2D");
    		mFastInfo = constructFastConnectInfo(intent);
    		if (!doFastConnection(mFastInfo)) {
    			Slog.d(TAG, "doFastConnection() fail");
    			mListener.onT2dConnectFail();
    		} else {
    			mListener.onT2dConnecting(mFastInfo.deviceAddress);
    		}
    	} else {
    		Slog.d(TAG, "invalid value returned from onWfdRequestReceived");
    		crashMe();
    	}
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			String action = arg1.getAction();

			if (NFC_HANDOVER_INTENT_ACTION_WFD_ACTIVE.equals(action)) {
				handleBroadcastT2d(arg1);
			} 
		}
    	
    };    
}
