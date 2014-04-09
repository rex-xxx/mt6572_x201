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

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;

import com.mediatek.nfc.handover.IWifiP2pProxy.IFastConnectInfo;
// TODO::  IWifiP2pProxy
import com.mediatek.nfc.handover.IWifiP2pProxy;


public class MtkWifiP2pHandover{



	public MtkWifiP2pHandover(Context context){
	
	}
	
	public MtkWifiP2pHandover(Context context,Uri[] uris
			,byte[] macAddr,byte[] goIp,byte[] gcIp,byte[] vendorId){
		
		
	}
	
	public void start(){
		
	}

	public boolean isConnecting(){
		return true;
	}

	public void startBeam(IFastConnectInfo fci,Uri[] uris){
	}
	
	public void acceptBeam(IFastConnectInfo fci){
	}
		
	public void startListen(){
		
	}



}
