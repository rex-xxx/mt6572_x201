package com.mediatek.nfc.handover;


import java.io.File;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import com.mediatek.nfc.handover.IWifiDisplayProxy;
//import com.mediatek.nfc.handover.IWifiP2pProxy.WifiP2pProxyListener;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
//import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;



public class WifiDisplayProxy implements IWifiDisplayProxy{

    static final String TAG = "WifiDisplayProxy";
    static final boolean DBG = true;


	public WifiDisplayProxy(Context context){
		Log.i(TAG, "  WifiDisplayProxy "  );
	}
	public int getRtspPortNumber(){
        Log.i(TAG, "  getRtspPortNumber :: 7236"  );
		return 7236;
	}


	/*
	public IWifiP2pProxy getInstance(){
		return new testWifiProxy(null);
	}
	*/



}
