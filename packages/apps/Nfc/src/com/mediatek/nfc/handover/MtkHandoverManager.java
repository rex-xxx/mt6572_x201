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

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
//import android.app.Notification;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.app.Notification.Builder;
//import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothDevice;
//import android.bluetooth.BluetoothHeadset;
//import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
//import android.media.MediaScannerConnection;
import android.net.Uri;


import android.nfc.NfcAdapter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.wps.INfcWpsTestBed;
import android.nfc.wps.WpsCredential;

//import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.Log;
//import android.util.Pair;


import com.android.nfc.NfcService;
import com.android.nfc.R;

import com.mediatek.nfc.handover.CarrierData.HandoverCarrierRecord;
import com.mediatek.nfc.handover.HandoverMessage.HandoverCarrier;
import com.mediatek.nfc.handover.HandoverMessage.HandoverRequest;
import com.mediatek.nfc.handover.HandoverMessage.HandoverSelect;
import com.mediatek.nfc.handover.IWifiP2pProxy.IFastConnectInfo;
import com.mediatek.nfc.handover.WifiCarrierConfiguration.Credential;
import com.mediatek.nfc.handover.WifiCarrierConfiguration.TLV;
import com.mediatek.nfc.handover.MtkWifiP2pHandover;
import com.mediatek.nfc.handover.HandoverBuilderParser;
import com.android.nfc.handover.HandoverManager;





import java.io.UnsupportedEncodingException;

import com.mediatek.nfc.handover.Util;

import com.mediatek.nfc.handover.BeamPlusHandover;
import com.mediatek.nfc.wps.INfcWpsAppInternal;
//import com.mediatek.nfc.wps.INfcWpsTestBed;
import com.mediatek.nfc.wps.NfcForegroundDispatchActivity;
//import com.mediatek.nfc.wps.WpsCredential;

import android.provider.MediaStore;
import android.database.Cursor;

/**
 * Manages handover of NFC to other technologies.
 */
public class MtkHandoverManager {//implements BluetoothProfile.ServiceListener,//	BluetoothHeadsetHandover.Callback {

    static final String TAG = "MtkHandoverManager";
    static final boolean DBG = true;

    public static final boolean BEAM_PLUS_SUPPORT = true;

    static final String NFC_HANDOVER_INTENT_ACTION_WFD_ACTIVE =
            "mediatek.nfc.handover.intent.action.WFD_ACTIVE";


    /** intent extra used to provide MAC addr*/
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

    /** Scenario String is used on whichScenario()*/
    public static final int SCENARIO_JB_ORIGINAL     =0;//= "com.mediatek.nfc.handover.SCENARIO_JB_ORIGINAL";
    public static final int SCENARIO_BEAMPLUS_P2P    =1;//= "com.mediatek.nfc.handover.SCENARIO_BEAMPLUS_P2P";
    public static final int SCENARIO_WFD             =2;//= "com.mediatek.nfc.handover.SCENARIO_WFD";
    public static final int SCENARIO_WIFI_LEGACY     =3;//= "com.mediatek.nfc.handover.SCENARIO_WIFI_LEGACY";
    public static final int SCENARIO_HR_COLLISION    =4;//= "com.mediatek.nfc.handover.SCENARIO_HR_COLLISION";


    // values for mSendState, should the same with p2plinkmanager.java
    static final int SEND_STATE_NOTHING_TO_SEND = 1;
    static final int SEND_STATE_NEED_CONFIRMATION = 2;    
    static final int SEND_STATE_SENDING = 3;



    final Context mContext;
    final BluetoothAdapter mBluetoothAdapter;
	HandoverManager mHandoverManager;
	//final MtkWifiP2pHandover mMtkWifiP2pHandover;
	final BeamPlusHandover mMtkWifiP2pHandover;
	
	WifiDisplayProxy mWifiDisplayProxy;

    public byte[] mP2pRequesterRandom;

    private WpsCredential mWpsCredential = null;
	private Handler mUI_Handler = new Handler();

    private static MtkHandoverManager mStaticInstance = null;
    private int mSendState = SEND_STATE_NOTHING_TO_SEND;

    public MtkHandoverManager(Context context,HandoverManager handoverManager) {
        mContext = context;
        Log.i(TAG, " MtkHandoverManager Construct ");
		if(handoverManager != null)
		mHandoverManager = handoverManager;
				
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        mMtkWifiP2pHandover = new BeamPlusHandover(context);
        
        mWifiDisplayProxy = new WifiDisplayProxy(context);


        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(INfcWpsTestBed.MTK_WPS_NFC_TESTBED_HR_ACTION);
        intentFilter.addAction(INfcWpsTestBed.MTK_WPS_NFC_TESTBED_HS_ACTION);
        mContext.registerReceiver(mReceiver, intentFilter);


    }

    public static void createSingleton(Context context,HandoverManager handoverManager) {
        if (mStaticInstance == null) {
            mStaticInstance = new MtkHandoverManager(context,handoverManager); 
        }
    }

    public static MtkHandoverManager getInstance() {
        return mStaticInstance;
    }


/*
    static NdefRecord createBeamPlusCollisionRecord() {
        byte[] random = new byte[2];
        new Random().nextBytes(random);
        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_COLLISION_RESOLVE, null, random);
    }

    NdefRecord createBluetoothAlternateCarrierRecord(boolean activating) {
        byte[] payload = new byte[4];
        payload[0] = (byte) (activating ? CARRIER_POWER_STATE_ACTIVATING :
            CARRIER_POWER_STATE_ACTIVE);  // Carrier Power State: Activating or active
        payload[1] = 1;   // length of carrier data reference
        payload[2] = 'b'; // carrier data reference: ID for Bluetooth OOB data record
        payload[3] = 0;  // Auxiliary data reference count
        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_ALTERNATIVE_CARRIER, null, payload);
    }

    NdefRecord createWiFiAlternateCarrierRecord(boolean activating) {
        byte[] payload = new byte[4];
        payload[0] = (byte) (activating ? CARRIER_POWER_STATE_ACTIVATING :
            CARRIER_POWER_STATE_ACTIVE);  // Carrier Power State: Activating or active
        payload[1] = 1;   // length of carrier data reference
        payload[2] = 'w'; // carrier data reference: ID for WiFi data record
        payload[3] = 0;  // Auxiliary data reference count
        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_ALTERNATIVE_CARRIER, null, payload);
    }

	

    NdefRecord createBluetoothOobDataRecord() {
        byte[] payload = new byte[8];
        payload[0] = 0;
        payload[1] = (byte)payload.length;

        synchronized (HandoverManager.this) {
            if (mLocalBluetoothAddress == null) {
                mLocalBluetoothAddress = mBluetoothAdapter.getAddress();
            }

            byte[] addressBytes = addressToReverseBytes(mLocalBluetoothAddress);
            System.arraycopy(addressBytes, 0, payload, 2, 6);
        }

        return new NdefRecord(NdefRecord.TNF_MIME_MEDIA, TYPE_BT_OOB, new byte[]{'b'}, payload);
    }
	
    NdefRecord createWifiRequestDataRecord() {
        byte[] payload = new byte[8];
        payload[0] = 0;
        payload[1] = (byte)payload.length;

        synchronized (HandoverManager.this) {
            if (mLocalBluetoothAddress == null) {
                mLocalBluetoothAddress = mBluetoothAdapter.getAddress();
            }

            byte[] addressBytes = addressToReverseBytes(mLocalBluetoothAddress);
            System.arraycopy(addressBytes, 0, payload, 2, 6);
        }

        return new NdefRecord(NdefRecord.TNF_MIME_MEDIA, TYPE_BT_OOB, new byte[]{'b'}, payload);
    }
*/
    public boolean isHandoverSupported() {
        return (mBluetoothAdapter != null);
    }
/*
    NdefRecord GetBluetoothOobData() {
        byte[] payload = new byte[8];
        payload[0] = 0;
        payload[1] = (byte)payload.length;

        synchronized (HandoverManager.this) {
            if (mLocalBluetoothAddress == null) {
                mLocalBluetoothAddress = mBluetoothAdapter.getAddress();
            }

            byte[] addressBytes = addressToReverseBytes(mLocalBluetoothAddress);
            System.arraycopy(addressBytes, 0, payload, 2, 6);
        }

        return new NdefRecord(NdefRecord.TNF_MIME_MEDIA, TYPE_BT_OOB, new byte[]{'b'}, payload);
    }
*/

    /*
     * int:0x112233 btyeCount:3
     *  array[0]: 0x33
     *  array[1]: 0x22
     *  array[2]: 0x11
    */
    byte[] intToByteCountArray(int i,byte btyeCount)
    {
    	byte j;
    	if(btyeCount > 4)
    		return null;
    	
      byte[] result = new byte[btyeCount];

      for(j=0 ; j < btyeCount ; j++)
      {
    	  byte k = (byte) ((byte) j*8);
    	  result[btyeCount-1-j]= (byte)(i >> k);
      }
      //result[0] = (byte) (i >> 24);
      //result[1] = (byte) (i >> 16);
      //result[2] = (byte) (i >> 8);
      //result[3] = (byte) (i /*>> 0*/);

      return result;
    }
    
    
    int byteArrayToint(byte[] btyeCount)
    {
    	byte j;
    	int result=0;
    	int length = btyeCount.length;
		//Log.i(TAG, " ~~  ~~  byteArrayToint " + length);
	    for(j=0 ; j < length ; j++)
	    {
	    	byte k = (byte) ((byte) j*8);
	    	//Log.i(TAG, " ~~  ~~  byteArrayToint  k:" + k +"    ele:"+ (btyeCount[j] << k));
	    	result += btyeCount[j] << k;
	    	//Log.i(TAG, " ~~  ~~  byteArrayToint  result:" + result );
	    }
	      
	    return result;
    }

	/*
	*	The Input MAC Address "06:08:A0:11:CC:6D"
	*	it will convert to 6bytes Array [0]:6D [1]:CC [2]:11 [3]:A0 [4]:08 [5]:06	
	*
	*/
    static byte[] addressToReverseBytes(String address) {
        Log.d(TAG, "addressToReverseBytes: " + address);
        String[] split = address.split(":");
        //Log.d(TAG, "addressToReverseBytes: " + split);
        byte[] result = new byte[split.length];

        for (int i = 0; i < split.length; i++) {
            // need to parse as int because parseByte() expects a signed byte
            result[split.length - 1 - i] = (byte)Integer.parseInt(split[i], 16);
        }

        return result;
    }

	/*
	*	The Input MAC Address "06:08:A0:11:CC:6D"
	*	it will convert to 6bytes Array [0]:06 [1]:08  [2]:A0 [3]:11 [4]:CC [5]:6D
	*
	*/
    static byte[] addressToByteArray(String address) {
        String[] split = address.split(":");
        byte[] result = new byte[split.length];

        for (int i = 0; i < split.length; i++) {
            // need to parse as int because parseByte() expects a signed byte
            result[i] = (byte)Integer.parseInt(split[i], 16);
        }

        return result;
    }


	/*
	*	The InputIP Address "192.168.1.1"
	*	it will convert to 6bytes Array [0]:01 [1]:01 [2]:A8 [3]:C0 	
	*
	*/
    static byte[] ipAddressToReverseBytes(String ipAddress) {
    	//String ipAddress = "192.168.1.1";
    	String[] ipAddressParts = ipAddress.split("\\.");

    	//Log.i(TAG, " ~~  ~~ ipAddressParts.length :" + ipAddressParts.length );
    	// convert int string to byte values
    	byte[] ipAddressBytes = new byte[ipAddressParts.length];
    	for(int i=0; i<ipAddressParts.length; i++){

    	    ipAddressBytes[ipAddressParts.length - 1 - i] = (byte)Integer.parseInt(ipAddressParts[i]);
    	}
    	return ipAddressBytes;
    }

    public static int byteToUnsignedInt(byte b) {
        return 0x00 << 24 | b & 0xff;
     }
    

    /*
     *	byte[] s2 ={0x6D,0xCC,0x11,0xA0,0x08,0x06}; 
     *  output "06:08:A0:11:CC:6D"
     *   
     */
    static String macBytesArrayToReverseString(byte[] ByteArray) 
    {
    	int separateLength = 5;
    	
		if(ByteArray.length != 6)
			Log.e(TAG, " Mac Address length not match :" + ByteArray.length);
    			
        StringBuilder sb = new StringBuilder(ByteArray.length + separateLength);
		
        for (int i=0;i< ByteArray.length;i++) {
            if (sb.length() > 0)
                sb.append(':');
            sb.append(String.format("%02x", ByteArray[ByteArray.length-1-i]));
        }
        	String resultString = sb.toString();
        	return resultString.toUpperCase();
        //return sb.toString();
    	
    }

    /*
     *	byte[] s2 ={0x06,0x08,0xA0,0x11,0xCC,0x6D}; 
     *  output "06:08:A0:11:CC:6D"
     *  without reversed string
     *   
     */
    static String macBytesArrayToString(byte[] ByteArray) 
    {
    	int separateLength = 5;
    	
		if(ByteArray.length != 6)
			Log.e(TAG, " Mac Address length not match :" + ByteArray.length);
    			
        StringBuilder sb = new StringBuilder(ByteArray.length + separateLength);
		
        for (int i=0;i< ByteArray.length;i++) {
            if (sb.length() > 0)
                sb.append(':');
            sb.append(String.format("%02x", ByteArray[i]));
        }
        	String resultString = sb.toString();
        	return resultString.toUpperCase();
        //return sb.toString();
    	
    }

    

    /*
     *	    short:288 (hex: 0x120) 
     *  output "0x0120"
     *   
     */
    static String addPrefixShortString(Short value) 
    {
        StringBuilder sb = new StringBuilder(6);//short byte present + 0x 

        sb.append("0x");
        sb.append(String.format("%04x", value));

    	String resultString = sb.toString();
    	return resultString;//.toUpperCase();
    }

    

    /*
     *	byte[] s2 ={1,1,(byte)0xa8,(byte)0xc0}; 
     * 
     *  output 192.168.1.1 
     */
    static String ipBytesArrayToReverseString(byte[] ByteArray) 
    {
    	int separateLength = 3;

   		if(ByteArray.length != 4)
			Log.e(TAG, " IP Address length not match :" + ByteArray.length);
       		
        StringBuilder sb = new StringBuilder(ByteArray.length + separateLength);
		
        for (int i=0;i< ByteArray.length;i++) {
            if (sb.length() > 0)
                sb.append('.');
            //temp = byteToUnsignedInt(ByteArray[ByteArray.length-1-i]);
            //Log.i(TAG, " temp :" + temp);
            //Log.i(TAG, "   b["+ (ByteArray.length-1-i)+"]:  "+(int)ByteArray[ByteArray.length-1-i]+"  integer.toString  " + Integer.toString((int)ByteArray[ByteArray.length-1-i]));
            sb.append(String.format("%d", byteToUnsignedInt(ByteArray[ByteArray.length-1-i])));
        }
        	
        return sb.toString();
    	
    }
    

	/**
	*	The Input String "0x0020"
	*	it will convert to String "0020"
	*
	*/
    static String splitPrefixString(String targetString) {

    	String[] targetStringParts = targetString.split("0x");


        if(targetStringParts.length != 2)
            Log.e(TAG, " splitPrefixString Error :" + targetStringParts.length);
        
    	String outputString = targetStringParts[1];

    	return outputString;
    }

	
    		
	public byte[][] genHandoverRequestAuxData(boolean WifiDisplayActive,boolean isBigFile){
		
		byte[] wifiMacAddress = null;
		Log.i(TAG, "genHandoverRequestAuxData    WifiDisplayActive:" + WifiDisplayActive);

		IFastConnectInfo mDefaultIFastConnectInfo; 
        mDefaultIFastConnectInfo = mMtkWifiP2pHandover.createDefaultFastConnectInfo();

		/* String AAA = "12345abcde";
		 *  byte[] CCC = AAA.getBytes();
		 *   CCC[0]:0x31 CCC[1]:0x32 ... CCC[9]:0x65
		 */
		String strWifiMacAddress = mDefaultIFastConnectInfo.getDeviceAddress();
		Log.i(TAG, " strWifiMacAddress   :" + strWifiMacAddress);
		if(strWifiMacAddress != null)
			wifiMacAddress = addressToByteArray(strWifiMacAddress);//strWifiMacAddress.getBytes();
			
		
		//int:0x112233 btyeCount:2
		//array[0]: 0x33     array[1]: 0x22
		int iWifiVendorId = mDefaultIFastConnectInfo.getVenderId();
		byte[] wifiVendorId = intToByteCountArray(iWifiVendorId,(byte)2);
		Log.i(TAG, " iWifiVendorId   :" + iWifiVendorId);		

		byte[] isBigFileAry = new byte[1];
		isBigFileAry[0] = (byte)(isBigFile?1:0);

		// create HrM with HcR and Aux. In HrM, Extension TLV(Aux) is been append after HcR.
		// set Aux[][]
		if(WifiDisplayActive == true){
			
			//byte[] rtspPort = null;
			byte[] rtspPort = intToByteCountArray(mWifiDisplayProxy.getRtspPortNumber(),(byte)2);
			
			byte[][] AuxData = {wifiMacAddress,
					        wifiVendorId,
					        //isBigFileAry,
					        rtspPort};

			return AuxData;
		}
		else{
			byte[][] AuxData = {wifiMacAddress,
					        wifiVendorId,
					        isBigFileAry};

			return AuxData;
		}
			

	}

	//return the status of BlueTooth , not related to enable success or not
	public boolean getBluetoothPowerState(){
        boolean powerState = mBluetoothAdapter.isEnabled();
		if (DBG) Log.d(TAG, "  getBluetoothPowerState, powerState = "  + powerState);
        return powerState;
	}
	
	//return the status of Wifi, not related to enable success or not
	public boolean powerUpWifi(){
        boolean powerState = mMtkWifiP2pHandover.isWifiEnabled();
		if (DBG) Log.d(TAG, "  powerUpWifi, powerState = " + powerState);	
		if (!powerState) {
			mMtkWifiP2pHandover.enableWifi();
        }
        return powerState;
	}

    
	private String printNdef(NdefMessage PrintNdef) {
        String ResultStr="";
        
        byte[] PrintNdefByteArray = PrintNdef.toByteArray();
        ResultStr += "  Length:"+ PrintNdefByteArray.length;
        ResultStr += "  Array::"+ Util.bytesToString(PrintNdefByteArray);
        
        return ResultStr;
    }
    
	NdefMessage createBeamPlusRequestMessage(Uri[] uris){
		Log.d(TAG, "	createBeamPlusRequestMessage():  ");
		return packHandoverRequestMessage(uris,false);
	}


	//
	/*  WifiDisplayActive:: True :: means it's WFD request,  BT ignore
	 * 	P2p HrM   HR (CR+AC+AC) +  BTCCR + WiFi Aux  (2 elements,without RTSP number)
	 *  WFD HrM   HR (CR+AC)    + WiFi Aux           (3 elements,with RTSP number)
	*/  
	synchronized NdefMessage packHandoverRequestMessage(Uri[] uris,boolean WifiDisplayActive){
	//to check Url, check return, check synchronized
		String strBTMac = null;
		boolean bTPowerState = false;
		
		Log.i(TAG, "packHandoverRequestMessage    WifiDisplayActive:" + WifiDisplayActive);

		if(WifiDisplayActive == false){
	        if (mBluetoothAdapter == null) {
				Log.e(TAG, "  mBluetoothAdapter == null  error exception");
				return null;
	    	}
	        
	        bTPowerState = getBluetoothPowerState();
		}
	
		boolean wifiPowerState = powerUpWifi();

		HandoverMessage hoMessage = new HandoverMessage();
		
		if(WifiDisplayActive == false){
			strBTMac = mBluetoothAdapter.getAddress();//= "11:22:33:44:55:66";
			Log.i(TAG, "   BT Mac Address:: " + strBTMac);
		
			// start BT CCR pack
			BTCarrierConfiguration btCCR = new BTCarrierConfiguration(strBTMac);
			hoMessage.appendAlternativeCarrier(bTPowerState?HandoverMessage.CARRIER_POWER_STATE_ACTIVE:HandoverMessage.CARRIER_POWER_STATE_ACTIVATING, btCCR);
		}
	
		// start WiFi pack
		byte[][] Aux = genHandoverRequestAuxData(WifiDisplayActive,isBigFile(uris));

        //handle getting Wifi MAC null case
        if(Aux[0] == null){
            Log.e(TAG, "!!!! not ERROR,exception: Device doesn't open Wifi on Life Cycle, use BT instead this time" );
            return mHandoverManager.createHandoverRequestMessage();
        }
        
		//HandoverMessage msg_WFp2p_R = new HandoverMessage();
		HandoverCarrierRecord HcR = HandoverCarrierRecord.newInstance(WifiCarrierConfiguration.TYPE);  //application/vnd.wfa.wsc
		
		hoMessage.appendAlternativeCarrier(wifiPowerState?HandoverMessage.CARRIER_POWER_STATE_ACTIVE:HandoverMessage.CARRIER_POWER_STATE_ACTIVATING, HcR, Aux);


		// HrM = HrR + HcR + Aux
		// Create total HandoverRequest Message
		NdefMessage hrM = hoMessage.createHandoverRequestMessage();
		//Log.d(TAG, "HrM = " + Util.bytesToString(wfp2p_hr.toByteArray()));

        
        // TODO:AntiCollision
        //state = InHandoverSection //
        if(WifiDisplayActive == false)
             mP2pRequesterRandom = hoMessage.mRequesterRandom;

		Log.i(TAG, "  mP2pRequesterRandom:  " + Util.bytesToString(mP2pRequesterRandom));
        return hrM;
	}

/*
    NdefRecord createBeamPlusHandoverRequestRecord() {
        NdefMessage nestedMessage = new NdefMessage(createBeamPlusCollisionRecord(),
									                createBluetoothAlternateCarrierRecord(false),
									                createWiFiAlternateCarrierRecord(false));
		
        byte[] nestedPayload = nestedMessage.toByteArray();

        ByteBuffer payload = ByteBuffer.allocate(nestedPayload.length + 1);
        payload.put((byte)0x12);  // connection handover v1.2
        payload.put(nestedMessage.toByteArray());

        byte[] payloadBytes = new byte[payload.position()];
        payload.position(0);
        payload.get(payloadBytes);
        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_HANDOVER_REQUEST, null,
                payloadBytes);
    }
*/
	

	NdefMessage createWiFiDisplayRequestMessage(Uri[] uris){
		Log.d(TAG, "	createWiFiDisplayRequestMessage():  ");
		return packHandoverRequestMessage(uris,true);
	}

	NdefMessage createWiFiLegacyRequestMessage(Uri[] uris){
        Log.i(TAG, "	createWiFiLegacyRequestMessage " );

        boolean wifiPowerState = powerUpWifi();

        return HandoverBuilderParser.createWfLegacyHrM(wifiPowerState);
	}

    static boolean elementExistInTLVByteArray(byte[] tlvData,short tagName){
        
        
        int cursor = 0;
        short innerTag = 0;
        int innerLen = 0;
        int dataLen= tlvData.length;
        
        while(cursor < dataLen){
            innerTag = (short) (tlvData[cursor++] & 0xFF);
        	innerTag = (short) ((innerTag << 8) | (tlvData[cursor++] & 0xFF));

        	innerLen = tlvData[cursor++] & 0xFF;
        	innerLen = (innerLen << 8) | (tlvData[cursor++] & 0xFF);

            Log.i(TAG, "    innerTag:"+innerTag+"  innerLen:"+innerLen+ "  cursor:"+cursor);

            if(innerTag == tagName)
                return true;
            
            cursor = cursor+innerLen;
        }
        
        Log.i(TAG, " dataLen:" +dataLen+"     cursor:"+cursor+"   return false!!");

        return false;
    }
    /**
    *       
    *   
    *   return the following:
    *   SCENARIO_JB_ORIGINAL
    *   SCENARIO_BEAMPLUS_P2P
    *   SCENARIO_WFD
    *   SCENARIO_WIFI_LEGACY 
    *
    */
    /*
    int whichScenario(NdefMessage m){
        NdefRecord r0 = m.getRecords()[0];
        short r0Tnf = r0.getTnf();
        byte[] r0Type = r0.getType();
        byte[] r0Payload = r0.getPayload();
        byte acCount = 0;
        byte recordCount = 0;
            
        byte version = r0Payload[0];
        byte[] acMessageBytes = new byte[r0Payload.length - 1];
        System.arraycopy(r0Payload, 1, acMessageBytes, 0, r0Payload.length - 1);
        r0Payload = null;



        NdefMessage ac = null; 
        
        try{
        	ac = new NdefMessage(acMessageBytes);
        }catch(FormatException E){
        	Log.e(TAG, "	FormatException "+E );
        }
        NdefRecord[] acItems = ac.getRecords();

        for (NdefRecord acRecord : acItems) {

    	    if (acRecord.getTnf() != NdefRecord.TNF_WELL_KNOWN) {
                Log.e(TAG, "	NdefRecord.tnf  not match " );
                //handle by JB original
                return SCENARIO_JB_ORIGINAL;
    		    //continue;
    	    }

    	    if (Arrays.equals(NdefRecord.RTD_ALTERNATIVE_CARRIER,acRecord.getType())){
                acCount++;
                Log.e(TAG, "	acCount:"+acCount );
    		    //continue;
    	    }
        }

        switch(acCount){
            case 2:
                return SCENARIO_BEAMPLUS_P2P;
                //break;
            case 1:
                //BT, WFD , Legacy
                NdefRecord r1 = m.getRecords()[1];
                short r1TNF = r1.getTnf();
                byte[] r1Type = r1.getType();
                byte[] r1Payload = r1.getPayload();
        
                if (r1TNF == NdefRecord.TNF_MIME_MEDIA && Arrays.equals(r1Type,BTCarrierConfiguration.BT_CARRIER_TYPE.getBytes())) 
                    return SCENARIO_JB_ORIGINAL;


                for (NdefRecord mRecord : m.getRecords()){
                    if(mRecord!=null)
                        recordCount++;
                    
                }
                Log.i(TAG, "	recordCount:"+recordCount );
                
                if(r1TNF == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(r1Type, NdefRecord.RTD_HANDOVER_CARRIER) &&
                    Arrays.equals(r0Type, NdefRecord.RTD_HANDOVER_REQUEST) && recordCount==2){
                    //Legacy Hr case only, (Hr(cr+ac)+HCR)
                    Log.e(TAG, "	SCENARIO_WIFI_LEGACY " );
                    return SCENARIO_WIFI_LEGACY;
                }
                
                if(r1TNF == NdefRecord.TNF_MIME_MEDIA && Arrays.equals(r1Type,WifiCarrierConfiguration.TYPE.getBytes()) &&
                    Arrays.equals(r0Type, NdefRecord.RTD_HANDOVER_SELECT)){
                    //WFD Hs    : Hr(cr+ac)+wifiCCR with TLV
                    //Legacy Hs : Hr(cr+ac)+wifiCCR


                    if(elementExistInTLVByteArray(r1Payload,HandoverBuilderParser.WPS_ATTRIBUTE_TYPE_VENDOR_ID)) 
                    	return SCENARIO_WFD;
                    else{
                    	Log.i(TAG, "	SCENARIO_WIFI_LEGACY " );
                    return SCENARIO_WIFI_LEGACY;
                }
                }
                
                Log.e(TAG, "	SCENARIO_JB_ORIGINAL Error case" );
                return SCENARIO_JB_ORIGINAL;
                //break;                
            case 0:
            default:
                return SCENARIO_JB_ORIGINAL;
                //break;

        }



        
    }

	//Check Wifi Alternative Carrier Roeord exist or not
    boolean isWiFiCarrierExist(NdefMessage m,byte[] recordDirection) {
        NdefRecord r = m.getRecords()[0];
        short tnf = r.getTnf();
        byte[] type = r.getType();

        // Check for BT OOB record
        //if (r.getTnf() == NdefRecord.TNF_MIME_MEDIA && Arrays.equals(r.getType(), TYPE_BT_OOB)) {
        //    return parseBtOob(ByteBuffer.wrap(r.getPayload()));
        //}

        // Check for Handover Select, followed by a BT OOB record
        if (tnf == NdefRecord.TNF_WELL_KNOWN &&
                Arrays.equals(type, recordDirection)) {// NdefRecord.RTD_HANDOVER_SELECT
            return searchWifiACR(m);
        }
		
		if (DBG) Log.d(TAG, "isWiFiCarrierExist(): false !!");
        return false;
    }

	//Search Wifi Alternative Carrier Roeord
    boolean searchWifiACR(NdefMessage m) {
        // TODO we could parse this a lot more strictly; right now
        // we just search for a BT OOB record, and try to cross-reference
        // the carrier state inside the 'hs' payload.
    	if (DBG) Log.d(TAG, "searchWifiACR ");
    	byte[] WifiCCRType = WifiCarrierConfiguration.TYPE.getBytes();
    	if (DBG) Log.d(TAG, "WifiCCRType.length" + WifiCCRType.length);
    	
        for (NdefRecord DataRecord : m.getRecords()) {
        	String s = new String(DataRecord.getType());
        	if (DBG) Log.d(TAG, "DataRecord.getType() : " + s);
            if (DataRecord.getTnf() == NdefRecord.TNF_MIME_MEDIA &&
                    Arrays.equals(DataRecord.getType(), WifiCCRType)) {
				if (DBG) Log.d(TAG, "searchWifiACR(): TRUE ");
                return true;
            }
        }
		if (DBG) Log.d(TAG, "searchWifiACR(): false (isWiFiCarrierExist :: false)");
        return false;
    }


	//Search CPS of Alternative Carrier Roeord
    byte searchCPSofAC(NdefMessage m,String acType) {
    	byte result = 1;
    	if (DBG) Log.d(TAG, "searchCPSofAC ");
		
    	byte[] acTypeArray = acType.getBytes();
    	if (DBG) Log.d(TAG, "acTypeArray.length" + acTypeArray.length);
    	
        for (NdefRecord DataRecord : m.getRecords()) {
        	String s = new String(DataRecord.getType());
        	if (DBG) Log.d(TAG, "DataRecord.getType() : " + s);
            if (DataRecord.getTnf() == NdefRecord.TNF_MIME_MEDIA &&
                    Arrays.equals(DataRecord.getType(), acTypeArray)) {
				byte[] payLoad = DataRecord.getPayload();
				if (DBG) Log.d(TAG, "Hr AC payload[0]:" + payLoad[0] +" [1]:"+ payLoad[1]+" [2]:"+ payLoad[2]+" [3]:"+ payLoad[3]);
                return payLoad[0];//CPS
            }
        }
        
        // TODO::FAIL NDEF
        return result;
		//return FAIL NDEF
		//if (DBG) Log.d(TAG, "isWiFiCarrierExist(): false ");
        //return false;
    }
    */


	//Check Wifi Alternative Carrier Roeord exist or not
	/*
	*	Check Wifi CPS and BT CPS 
	*	
	*	
	*	return ture :: choose BT 
	*		   False ::           Wifi
	*
	*/
    boolean determineConnWay(HandoverMessageElement HandoverMsgElement) {
    
        Log.d(TAG, "!!  ALWAYS USE WIFI   !!");
        return false; 
        /*
    	boolean result;

		boolean clientWifiCPS = (HandoverMsgElement.mWifiCPS==HandoverMessage.CARRIER_POWER_STATE_ACTIVE)?true:false;
		boolean clientBtCPS = (HandoverMsgElement.mBtCPS==HandoverMessage.CARRIER_POWER_STATE_ACTIVE)?true:false;

		boolean serverWifiCPS = mMtkWifiP2pHandover.isWifiEnabled();
		boolean serverBtCPS = mBluetoothAdapter.isEnabled();
        
    	Log.d(TAG, "  determineConnWay  clientWifiCPS:"+clientWifiCPS+"  clientBtCPS:"+clientBtCPS);
    	Log.d(TAG, "  determineConnWay  serverWifiCPS:"+serverWifiCPS+"  serverBtCPS:"+serverBtCPS);
        
    	Log.d(TAG, "  HandoverMsgElement.mIsBigFile:: "+HandoverMsgElement.mIsBigFile);

    	if(HandoverMsgElement.mIsBigFile == 1)
    		result = false;//if transfer file > 1M,  --> use wifi
    	else if(clientWifiCPS && serverWifiCPS)
    		result = false;//if transfer file < 1M,  both Wifi direct on  --> use wifi
    	else if(!clientBtCPS && !serverBtCPS)
    		result = false;//if transfer file < 1M,  both Wifi direct on,  both BT OFF--> use wifi
    	else
    		result = true;//others case --> use BT
    	
    	Log.d(TAG, "!!  USE " + (result?"BT":"WIFI")+ "   !!");
    	return result;
        */
   }


	//Check Wifi Alternative Carrier Roeord exist or not
	/*
	*	Check Wifi CPS and BT CPS 
	*	
	*	
	*	return ture :: choose BT 
	*		   False ::           Wifi
	*
	*/
	/*
    boolean determineConnWay(NdefMessage m) {

		boolean result;
		if (DBG) Log.d(TAG, "determineConnWay():  ");
		
        //NdefRecord r = m.getRecords()[0];
        //short tnf = r.getTnf();
        //byte[] type = r.getType();


		byte wifiCPSbyte = searchCPSofAC(m,WifiCarrierConfiguration.TYPE);
		boolean clientWifiCPS = (wifiCPSbyte == HandoverMessage.CARRIER_POWER_STATE_ACTIVE)?true:false;
		byte btCPSbyte = searchCPSofAC(m,BTCarrierConfiguration.BT_CARRIER_TYPE);
		boolean clientBtCPS = (btCPSbyte == HandoverMessage.CARRIER_POWER_STATE_ACTIVE)?true:false;

		boolean serverWifiCPS = mMtkWifiP2pHandover.isWifiEnabled();

		boolean serverBtCPS = mBluetoothAdapter.isEnabled();



		byte[][]auxArray = null;
		
		//parse P2p HrM
		
		try{
			auxArray = HandoverBuilderParser.auxFromParseP2pHrM(m);
		}catch(FormatException fe){
			Log.e(TAG, " FormatException = " + fe);
		}
		
		// TODO:: Error Handle, Create fail Ndef
		if(auxArray == null){
			Log.e(TAG, " auxArray == null "  );
			return false;//Let wifi handle
		}
		
		Log.d(TAG, " auxArray.length  :" + auxArray.length+"  (should be 3) ");
		//if(auxArray.length == 3)
		//	Log.e(TAG, " auxArray.length = " + auxArray.length);
		
		//byte[] macAddr  = auxArray[0];
		//byte[] vendorId = auxArray[1];
		
		byte[] isBigFile = auxArray[2];

		
		if(isBigFile[0] == 1)
			result = false;//if transfer file > 1M,  --> use wifi
		else if(clientWifiCPS && serverWifiCPS)
			result = false;//if transfer file < 1M,  both Wifi direct on  --> use wifi
		else if(!clientBtCPS && !serverBtCPS)
			result = false;//if transfer file < 1M,  both Wifi direct on,  both BT OFF--> use wifi
		else
			result = true;//others case --> use BT
		
		Log.d(TAG, "!!  USE " + (result?"BT":"WIFI")+ "   !!");
		return result;


    }
*/




    private byte[] parseTLV(TLV[] tlvArray,short tagName){
    	//TLV element = tlvArray[0];
    	for(TLV element:tlvArray)
    	{
    		if(element.getTag() == tagName)
    		return element.getValue();
    	}
    	
    	return null;    	
    }

	//deal with BeamPlus P2p Handover Select Message
	//Client Get HsM response
	private void dealBeamPlusP2pHsM(Uri[] uris,NdefMessage BPhandoverMessage){
	
		Log.d(TAG, "  dealBeamPlusP2pHsM "	);
		
		Credential mCredentialP2p = null;

		try{
			mCredentialP2p = HandoverBuilderParser.credentialFromParseP2pHsM(BPhandoverMessage);
		}catch(FormatException e){
			Log.e(TAG, "format exception = " + e);
		}
		catch(UnsupportedEncodingException unE){
			Log.e(TAG, "Unsupport exception = " + unE);
		}
		
		byte[] macAddr = mCredentialP2p.getMacAddress();
		Log.d(TAG, "macAddr = " + Util.bytesToString(macAddr));

        byte mNetworkId = mCredentialP2p.getNetworkIndex();
        Log.d(TAG, "mNetworkId = " + mNetworkId);

        short mAuthType = mCredentialP2p.getAuthenticationType();
        short mEncType = mCredentialP2p.getEncryptionType();
        String mNetworkKey = mCredentialP2p.getNetworkKey();
        String mSSID = mCredentialP2p.getSSID();

        Log.d(TAG, "mNetworkKey = " + mNetworkKey);
        Log.d(TAG, "mSSID = " + mSSID);
        Log.d(TAG, "mAuthType =" + mAuthType+"  mEncType="+mEncType);

        Log.d(TAG, " addPrefixShortString(mAuthType)=" + addPrefixShortString(mAuthType)+"  addPrefixShortString(mEncType)="+addPrefixShortString(mEncType));

        
		TLV[] tLVArray = mCredentialP2p.getExtensions();
		
		byte[] vendorId = parseTLV(tLVArray,HandoverBuilderParser.WPS_ATTRIBUTE_TYPE_VENDOR_ID);
		byte[] goIp = parseTLV(tLVArray,HandoverBuilderParser.WPS_ATTRIBUTE_TYPE_GO_IP);
		byte[] gcIp = parseTLV(tLVArray,HandoverBuilderParser.WPS_ATTRIBUTE_TYPE_GC_IP);
		
		Log.d(TAG, "vendorId = " + Util.bytesToString(vendorId));
		Log.d(TAG, "goIp = " + Util.bytesToString(goIp));
		Log.d(TAG, "gcIp = " + Util.bytesToString(gcIp));
		

        
		byte[] maxHeight = parseTLV(tLVArray,HandoverBuilderParser.WPS_ATTRIBUTE_TYPE_MAX_HEIGHT);
		byte[] maxWidth = parseTLV(tLVArray,HandoverBuilderParser.WPS_ATTRIBUTE_TYPE_MAX_WIDTH);
		if(maxHeight!=null || maxWidth!=null){      
    		Log.d(TAG, "should null !! maxHeight = " + Util.bytesToString(maxHeight));
    		Log.d(TAG, "should null !! maxWidth = " + Util.bytesToString(maxWidth));
        }
		
		
		// TODO:: Phase II	   handover.startOnConnected();
		// Register a new handover transfer object
		//getOrCreateHandoverTransfer(data.device.getAddress(), false, true); //sure
		//MtkWifiP2pHandover handover = new MtkWifiP2pHandover(mContext,uris,macAddr,goIp,gcIp,vendorId);

		Log.d(TAG, "to BeamPlushandover mAuthType =" + addPrefixShortString(mAuthType)+"  mEncType ="+addPrefixShortString(mEncType));


		IFastConnectInfo mIDefaultFastConnectInfo = mMtkWifiP2pHandover.createDefaultFastConnectInfo();

        mIDefaultFastConnectInfo.setNetworkId((int)mNetworkId);
		mIDefaultFastConnectInfo.setSsid(mSSID);
		mIDefaultFastConnectInfo.setAuthType(addPrefixShortString(mAuthType));
		mIDefaultFastConnectInfo.setEncrType(addPrefixShortString(mEncType));
		mIDefaultFastConnectInfo.setPsk(mNetworkKey);//Network Key

        
        mIDefaultFastConnectInfo.setVenderId(byteArrayToint(vendorId));

        String macAddrStr = macBytesArrayToReverseString(macAddr);
        Log.d(TAG, " to BeamPlushandover macAddrStr = " + macAddrStr);
		mIDefaultFastConnectInfo.setDeviceAddress(macAddrStr);//macBytesArrayToReverseString //macAddr.toString()

        String gcIpStr = ipBytesArrayToReverseString(gcIp);
		Log.d(TAG, " to BeamPlushandover gcIpStr = " + gcIpStr);
		mIDefaultFastConnectInfo.setGcIpAddress(gcIpStr);//gcIp.toString()

        String goIpStr = ipBytesArrayToReverseString(goIp);
        Log.d(TAG, " to BeamPlushandover goIpStr = " + goIpStr);
		mIDefaultFastConnectInfo.setGoIpAddress(goIpStr);// goIp.toString()
        


		//IFastConnectInfo mIFastConnectInfo = mMtkWifiP2pHandover.getFastConnectInfo(mIDefaultFastConnectInfo);

        Log.d(TAG, "BeamPlusHandover.startBeam  [HS end]" );

		//mMtkWifiP2pHandover.startBeam(mIFastConnectInfo,uris);
		mMtkWifiP2pHandover.startBeam(mIDefaultFastConnectInfo,uris);
					
	}

	//	*  	WFD Wifi CCR (with MaxH,MaxW) HS(+AC)    + Wifi CCR (with H,W)

	/**
	*	Requester parse the Handover Select Message on Beam Plus scenario
	*	<p>
	* 	It's possible to receive two type HsM. 
	*	<p>
	* 	1.BT CCR only(JB original) 		HS(+AC)    + BT CCR 
	* 	2.BT Wifi CCR (P2p usage)   	HS(+AC+AC) + BT CCR + Wifi CCR
	*	<p>
	*  	WFD and Legacy message should not enter this funciton
	*	<p>
	*		
	* @param  uris  the URIs of beamPlus files
	* @param  BPhandoverMessage  the selector response message
	* @return	   null
	* @see		   null     
	*/
	public void doBeamPlusHandover(Uri[] uris,NdefMessage BPhandoverMessage){//throws Exception{
		//to check Url, check return

		Log.d(TAG, "  doBeamPlusHandover "  );

		/*
		*		1.parse HsM
		*/



		//1.1 Try Wifi AC exist or not  , if not , BT hit  JB original
        NdefRecord r = BPhandoverMessage.getRecords()[0];
        if (r.getTnf() != NdefRecord.TNF_WELL_KNOWN){
        	Log.e(TAG, "  r.getTnf() != NdefRecord.TNF_WELL_KNOWN  return; "  );
        	return;
        }
        if (!Arrays.equals(r.getType(), NdefRecord.RTD_HANDOVER_SELECT)){
        	Log.e(TAG, "  r.getType() != NRTD_HANDOVER_SELECT  return; "  );
        	return;
        }


        //= whichScenario(tryMessage);
        HandoverMessageElement mHandoverMsgElement = new HandoverMessageElement(BPhandoverMessage);
        int mScenario = mHandoverMsgElement.mscenario;
        Log.d(TAG, "mScenario:" + mScenario);

        //because we will determine connway on selector so we judge mScenario on doBeamPlusHandover() for JB original device(use BT)
        switch(mScenario){

            case SCENARIO_BEAMPLUS_P2P:
                dealBeamPlusP2pHsM(uris,BPhandoverMessage);
                return;
            case SCENARIO_WFD:
                //return mHandoverManager.tryHandoverRequest(tryMessage);
                Log.e(TAG, " should not get WFD HandoverSelectMessage   return "  );
                return;
                //break;

            case SCENARIO_WIFI_LEGACY:
                //return dealWifiLegacyHrM(BPhandoverMessage);
                Log.e(TAG, " should not get WFL HandoverSelectMessage   return"  );
                return;
                
            case SCENARIO_HR_COLLISION:
                //return dealWifiLegacyHrM(BPhandoverMessage);
                Log.e(TAG, " SCENARIO_HR_COLLISION  "  );
                IFastConnectInfo mIDefaultFastConnectInfo = mMtkWifiP2pHandover.createDefaultFastConnectInfo();

                String macAddrStr = macBytesArrayToString(mHandoverMsgElement.mWinnerMac);
                Log.d(TAG, "Colli.. to BeamPlushandover set Device GcIp Addr  macAddrStr = " + macAddrStr);
                mIDefaultFastConnectInfo.setDeviceAddress(macAddrStr);
        		//mIDefaultFastConnectInfo.setGcIpAddress(macAddrStr);
        		Log.d(TAG, "BeamPlusHandover.startBeamWhenConnected  [HS end] Collision case !!!!" );
                
                mMtkWifiP2pHandover.startBeamWhenConnected(mIDefaultFastConnectInfo,uris);
                return;

            default:
            case SCENARIO_JB_ORIGINAL:
                mHandoverManager.doHandoverUri(uris,BPhandoverMessage);
                return;
        }


/*
        
		//1.1 Try Wifi AC exist or not  , if not , BT hit  JB original
		if(false == isWiFiCarrierExist(BPhandoverMessage,NdefRecord.RTD_HANDOVER_SELECT)){
			//type 1 :: BT CCR (JB original)
			Log.d(TAG, "  BT CCR only (JB original) "  );
			mHandoverManager.doHandoverUri(uris,BPhandoverMessage);
			return;
		}

		//1.2 Try BT CCR exist or not
		//Credential mCredential;

		HandoverSelect hs=null;
		try{
			hs = HandoverMessage.tryParseSelect(BPhandoverMessage);
		}catch (FormatException e){
			Log.e(TAG, "FormatException e = " + e);	
		}
		
		HandoverCarrier carriers_S[] = hs.getCarriers();
		//try first CCR 
		BTCarrierConfiguration BTccr = BTCarrierConfiguration.tryParse(carriers_S[0]);
		Log.d(TAG, "HSM - BT CCR = " + Util.bytesToString(BTccr.getMacAddress()));

		if(BTccr != null){
			//type 2 :: BT Wifi CCR (P2p usage)   	
			dealBeamPlusP2pHsM(uris,BPhandoverMessage);
			return;
		}
		*/
		/*
		else{
            //type 3 :: WFD Wifi CCR (with MaxH,MaxW) 	
			doWiFiDisplayHandover(uris,BPhandoverMessage);
            //throw new Exception("mtk BeamPlus BTCCR not exist");
			return;
		}
		*/

		
	}
	
	void doWiFiDisplayHandover(Uri[] uris,NdefMessage WFDhandoverMessage){
		//to check Url, check return
		Log.d(TAG, "  doWiFiDisplayHandover "	);
		
		Credential mCredentialP2p = null;
		TLV[] mTLVData=null;
		try{
			mCredentialP2p = HandoverBuilderParser.credentialFromParseWfDisplayHsM(WFDhandoverMessage);
		}catch(FormatException e){
			Log.e(TAG, " Fexception = " + e);
		}
		catch(UnsupportedEncodingException unE){
			Log.e(TAG, "Unsupport exception = " + unE);
		}
		
		byte[] macAddr = mCredentialP2p.getMacAddress();
		Log.d(TAG, "macAddr = " + Util.bytesToString(macAddr));

        
        String mSSID= mCredentialP2p.getSSID();      
        Log.d(TAG, "mSSID = " + mSSID);
        String mNetworkKey = mCredentialP2p.getNetworkKey();
        Log.d(TAG, "mNetworkKey = " + mNetworkKey);

        byte mNetworkId = mCredentialP2p.getNetworkIndex();
        Log.d(TAG, "mNetworkId = " + mNetworkId);

        short mAuthType = mCredentialP2p.getAuthenticationType();
        short mEncType = mCredentialP2p.getEncryptionType();
        Log.d(TAG, "mAuthType =" + mAuthType+"  mEncType="+mEncType);

        
		TLV[] tLVArray = mCredentialP2p.getExtensions();
		int len = tLVArray.length;
		
		try{
			mTLVData = HandoverBuilderParser.auxFromParseWfDisplayHsM(WFDhandoverMessage);
		}catch(FormatException e){
			Log.e(TAG, " Fexception = " + e);
		}
		catch(UnsupportedEncodingException unE){
			Log.e(TAG, "Unsupport exception = " + unE);
		}
				
				
		byte[] vendorId = parseTLV(mTLVData,HandoverBuilderParser.WPS_ATTRIBUTE_TYPE_VENDOR_ID);
		byte[] goIp = parseTLV(mTLVData,HandoverBuilderParser.WPS_ATTRIBUTE_TYPE_GO_IP);
		byte[] gcIp = parseTLV(mTLVData,HandoverBuilderParser.WPS_ATTRIBUTE_TYPE_GC_IP);
		
		Log.d(TAG, "vendorId = " + Util.bytesToString(vendorId));
		Log.d(TAG, "goIp = " + Util.bytesToString(goIp));
		Log.d(TAG, "gcIp = " + Util.bytesToString(gcIp));
		
		
		byte[] maxHeight = parseTLV(mTLVData,HandoverBuilderParser.WPS_ATTRIBUTE_TYPE_MAX_HEIGHT);
		byte[] maxWidth = parseTLV(mTLVData,HandoverBuilderParser.WPS_ATTRIBUTE_TYPE_MAX_WIDTH);
		
		Log.d(TAG, " maxHeight = " + Util.bytesToString(maxHeight));
		Log.d(TAG, " maxWidth = " + Util.bytesToString(maxWidth));
		
		
		//Send Broadcast
		sendWFDActiveBroadcast(macAddr,mNetworkKey,mSSID,mNetworkId,mAuthType,mEncType,vendorId,gcIp,goIp,maxHeight,maxWidth);

		


		
	}
	
	void doWiFiLegacyHandover(Uri[] uris,NdefMessage WifiLegacyHandoverMessage){
		//to check Url, check return
		Log.d(TAG, "  doWiFiLegacyHandover()"	);
		
		Credential mCredentialWL = null;
		TLV[] mTLVData=null;
		try{
			mCredentialWL = HandoverBuilderParser.credentialFromParseWfLegacyHsM(WifiLegacyHandoverMessage);
		}catch(FormatException e){
			Log.e(TAG, " Fexception = " + e);
		}
		catch(UnsupportedEncodingException unE){
			Log.e(TAG, "Unsupport exception = " + unE);
		}

		byte[] macAddr = mCredentialWL.getMacAddress();
		Log.d(TAG, "macAddr = " + Util.bytesToString(macAddr));
        
        String mSSID= mCredentialWL.getSSID();      
        Log.d(TAG, "mSSID = " + mSSID);
        String mNetworkKey = mCredentialWL.getNetworkKey();
        Log.d(TAG, "mNetworkKey = " + mNetworkKey);

        byte mNetworkId = mCredentialWL.getNetworkIndex();
        Log.d(TAG, "mNetworkId = " + mNetworkId);

        short mAuthType = mCredentialWL.getAuthenticationType();
        short mEncType = mCredentialWL.getEncryptionType();
        Log.d(TAG, "mAuthType =" + mAuthType+"  mEncType="+mEncType);


        sendWifiLEgacyBroadcast(macAddr,mNetworkKey,mSSID,mNetworkId,mAuthType,mEncType);
	}

	/*
	*	handle Beam Plus P2p Handover Request Message
	*	
	*	this function do the following..
	*		1.Set Hr AUX to wifi
	*		2.Set hr BT mac to BT
	*		3.judge MtkWifiP2PHandover status and start trigger
	*
	*/
	public NdefMessage dealBeamPlusP2pHrM(NdefMessage BPP2pHrM){

		byte[][]auxArray = null;
		
		Log.i(TAG, " dealBeamPlusP2pHrM  " );
		
		//parse P2p HrM
		try{
			auxArray = HandoverBuilderParser.auxFromParseP2pHrM(BPP2pHrM);
		}catch(FormatException fe){
			Log.e(TAG, " FormatException = " + fe);
		}
		
		// TODO:: Error Handle, Create fail Ndef  null install
		if(auxArray == null){
			Log.e(TAG, " auxArray == null " );
			return null;
		}
		
		Log.d(TAG, " auxArray.length = " + auxArray.length+"  should be 3 ");
		if(auxArray.length != 3)
			Log.e(TAG, " auxArray.length = " + auxArray.length);
		
		byte[] macAddr  = auxArray[0];
		byte[] vendorId = auxArray[1];
		
		Log.e(TAG, "  vendorId = " + Util.bytesToString(vendorId));
		Log.e(TAG, "  rec. wifi macAddr = " + Util.bytesToString(macAddr));

		//Log.e(TAG, " To ReverseString macAddr = " + macBytesArrayToReverseString(macAddr));
		Log.e(TAG, " rec. wifi macAddr To String  = " + macBytesArrayToString(macAddr));



		//Set Hr AUX to wifi
		if(false == mMtkWifiP2pHandover.isConnecting()){

			IFastConnectInfo mDefaultIFastConnectInfo; 
			mDefaultIFastConnectInfo = mMtkWifiP2pHandover.createDefaultFastConnectInfo();

			mDefaultIFastConnectInfo.setVenderId(byteArrayToint(vendorId));
			mDefaultIFastConnectInfo.setDeviceAddress(macBytesArrayToString(macAddr)); //(String)macAddr.toString()
			//mIFastConnectInfo.setGcIpAddress(gcIp.toString());
			//mIFastConnectInfo.setGoIpAddress(goIp.toString());
            Log.d(TAG, " BeamPlusHandover.acceptIncomingBeam    ");

			mMtkWifiP2pHandover.acceptIncomingBeam(mDefaultIFastConnectInfo);

			return createBeamPlusP2pHsM(mDefaultIFastConnectInfo);
		}
		else{
		// TODO:: isConnecting return true, start on connected ,Phase II
			Log.e(TAG, " TODO:: BeamPlusHandover isConnecting return true  "  );
			return null;
		}
		
		//Set hr BT mac to BT
		// TODO:: Do nothing


	}
	
	/*
	*	Create Beam Plus P2p Handover Selcet Message
	*	
	*	this function do the following..
	*		1.Get Fast Conn Info by hr's defaultFastConnInfo
	*		2.
	*		
	*/
	public NdefMessage createBeamPlusP2pHsM(IFastConnectInfo connInfoWithVidMAC){
		NdefMessage p2pHsM = null;

        Log.i(TAG, " createBeamPlusP2pHsM  "  );
    	//Get Wiif Mac,vender,GOIP
    	
    	IFastConnectInfo mIFastConnectInfo; 

    	String strBTMac = mBluetoothAdapter.getAddress();//= "11:22:33:44:55:66";

		/// R: @ {
		if (mMtkWifiP2pHandover.isConnected()) {
			if (mMtkWifiP2pHandover.isDeviceAlreadyConnected(connInfoWithVidMAC)) {
				Log.d(TAG, " ===> Already CONNECTED to the same device");
				mIFastConnectInfo = mMtkWifiP2pHandover.createDefaultFastConnectInfo();
			} else {
				Log.d(TAG, " ===> Already CONNECTED, but not this one. Ignore it.");
				return null;
			}
		} else {
			Log.d(TAG, " ===> Not CONNECTED, normal case");
			mIFastConnectInfo = mMtkWifiP2pHandover.getFastConnectInfo(connInfoWithVidMAC);
			mMtkWifiP2pHandover.setThisDeviceInfo(mIFastConnectInfo);
		}
		/// }
		
    	int vendorId = mIFastConnectInfo.getVenderId();
    	String macAddr = mIFastConnectInfo.getDeviceAddress();
    	String gcIp = mIFastConnectInfo.getGcIpAddress();
    	String goIp = mIFastConnectInfo.getGoIpAddress();


		Log.i(TAG, " NetworkId:"+mIFastConnectInfo.getNetworkId()  );
		Log.i(TAG, " Hs wifi macAddr:"+macAddr );

		String mSSID = mIFastConnectInfo.getSsid();

		String mAuthType = mIFastConnectInfo.getAuthType();

		String mEncrType= mIFastConnectInfo.getEncrType();

		String mNetworkKey= mIFastConnectInfo.getPsk();


		Log.i(TAG, " mSSID:" + mSSID );
		Log.i(TAG, " mNetworkKey:" + mNetworkKey );

        String mCutPrefixAuthType = splitPrefixString(mAuthType);
        String mCutPrefixEncrType = splitPrefixString(mEncrType);

		Log.i(TAG, " mAuthType:" + mAuthType +"   mCutPrefixAuthType:"+mCutPrefixAuthType);
		Log.i(TAG, " mEncrType:" + mEncrType +"   mCutPrefixEncrType:"+mCutPrefixEncrType);


    	//pack HS
    	// TODO::  Credential just use MAC, clientTable is null
    	p2pHsM = HandoverBuilderParser.createP2PHsM(strBTMac,
    			mNetworkKey,//String wifi_NetworkKey,
    			mSSID,//String wifi_SSID,
    			Short.parseShort(mCutPrefixAuthType,16),
    			Short.parseShort(mCutPrefixEncrType,16),
    			addressToReverseBytes(macAddr),//macAddr.getBytes(),//byte[] wifi_MACAddress,
    			intToByteCountArray(vendorId,(byte)2),//byte[] vendorID,
    			ipAddressToReverseBytes(goIp),//goIp.getBytes(),//byte[] GOIP,
    			ipAddressToReverseBytes(gcIp),//gcIp.getBytes(),//byte[] GCIP,
    			null,//byte[][] clientTable);
    			getBluetoothPowerState(),
    			powerUpWifi());
    	
    	if (DBG) Log.d(TAG, "  p2pHsM" + p2pHsM);
    	return p2pHsM;
	}


	/*
	*	handle Wifi Legacy Handover Request Message
	*	
	*	this function don't need to do anything because there is no Hr content include.
	*
	*/
	public NdefMessage dealWifiLegacyHrM(NdefMessage WLHrM){
    	return createWifiLegacyHsM();
    }


	/*
	*	Create Wifi Legacy Handover Selcet Message, the wifi CCR contains Credential only..
	*	
	*	this function do the following..
	*		1.Get Credential 
	*		2.pack 
	*		
	*/
	public NdefMessage createWifiLegacyHsM(){//(IFastConnectInfo connInfoWithCredential){
		NdefMessage mWLHsM = null;
        if (DBG) Log.d(TAG, "  createWifiLegacyHsM()");


        if(mWpsCredential != null){
        
    	//Get Wiif Mac,vender,GOIP

        //mWpsCredential.getNetworkIndex() 

        
        if (DBG) Log.d(TAG, "  NetworkIndex set to Default" );
        if (DBG) Log.d(TAG, "  mWpsCredential.getMacAddress() :" + Util.bytesToString(mWpsCredential.getMacAddress()));

        if (DBG) Log.d(TAG, " String getNetworkKey:" + mWpsCredential.getNetworkKey());
        if (DBG) Log.d(TAG, " String getSSID:" + mWpsCredential.getSSID());

        if (DBG) Log.d(TAG, "  AuthType:" + mWpsCredential.getAuthType());
        if (DBG) Log.d(TAG, "  EncrypType:" + mWpsCredential.getEncrypType());


    	//pack HS
    	mWLHsM = HandoverBuilderParser.createWfLegacyHsM(
    			mWpsCredential.getNetworkKey(),//String wifi_NetworkKey,
    			mWpsCredential.getSSID(),//String wifi_SSID,
    			mWpsCredential.getMacAddress(), //addressToBytes(macAddr),//byte[] wifi_MACAddress,
    			mWpsCredential.getAuthType(),//short AuthType
    			mWpsCredential.getEncrypType() //short EnrcType
    		    );
        }


    
	if (DBG) Log.d(TAG, "  Wifi Legacy HsM" + mWLHsM);

    
	return mWLHsM;
	}


	/*
	*	handle WFD Handover Request Message
	*	
	*	this function do the following..
	*		1.Set Hr AUX to wifi
	*		3.judge MtkWifiP2PHandover status and start trigger
	*
	*/
	/*
	public void dealWiFiDisplayHrM(NdefMessage BPP2pHrM){
		byte[][]auxArray = null;
		//parse P2p HrM
		Log.i(TAG, " dealWiFiDisplayHrM  " );

		try{
			auxArray = HandoverBuilderParser.auxFromParseWfDisplayHrM(BPP2pHrM);
		}catch(FormatException fe){
			Log.e(TAG, " FormatException = " + fe);
		}
		
		Log.d(TAG, " auxArray.length = " + auxArray.length+"  should be 4 ");//include bigfileInd
		if(auxArray.length != 4)
			Log.e(TAG, " auxArray.length = " + auxArray.length);
		
		byte[] macAddr  = auxArray[0];
		byte[] vendorId = auxArray[1];
		byte[] isBigFile  = auxArray[2];
		byte[] rtspNumber = auxArray[3];

		Log.e(TAG, "  vendorId = " + Util.bytesToString(vendorId));
		Log.e(TAG, "  macAddr = " + Util.bytesToString(macAddr));
		Log.e(TAG, "  rtspNumber = " + Util.bytesToString(rtspNumber));


		IFastConnectInfo mDefaultIFastConnectInfo; 
		mDefaultIFastConnectInfo = mtestWifiProxy.createDefaultFastConnectInfo();
		
		mDefaultIFastConnectInfo.setVenderId(byteArrayToint(vendorId));
		mDefaultIFastConnectInfo.setDeviceAddress(macBytesArrayToReverseString(macAddr));
		// TODO:: Set RTSP number
		//mDefaultIFastConnectInfo.setRTSP(macBytesArrayToReverseString(rtspNumber));


	
	}
	*/

	/*
	*	Create WFD Handover Select Message
	*	
	*	this function do the following..
	*		1.Set Hr AUX to wifi
	*		2.judge MtkWifiP2PHandover status and start trigger
	*
	*/
	/*
	private NdefMessage CreateWiFiDisplayHsM(){
		NdefMessage p2pHsM = null;
	
	//Get Wiif Mac,vender,GOIP
	
	IFastConnectInfo mIFastConnectInfo; 

	//String strBTMac = mBluetoothAdapter.getAddress();//= "11:22:33:44:55:66";


	mIFastConnectInfo = mtestWifiProxy.getFastConnectInfo(connInfoWithVidMAC);

	int vendorId = mIFastConnectInfo.getVenderId();
	String macAddr = mIFastConnectInfo.getDeviceAddress();
	String gcIp = mIFastConnectInfo.getGcIpAddress();
	String goIp = mIFastConnectInfo.getGoIpAddress();

	//pack HS
	// TODO::  Credential just use MAC, clientTable is null
	p2pHsM = HandoverBuilderParser.createWfDisplayHsM(
			null,//String wifi_NetworkKey,
			null,//String wifi_SSID,
			addressToReverseBytes(macAddr),//macAddr.getBytes(),//byte[] wifi_MACAddress,
			intToByteCountArray(vendorId,(byte)2),//byte[] vendorID,
			ipAddressToReverseBytes(goIp),//goIp.getBytes(),//byte[] GOIP,
			ipAddressToReverseBytes(gcIp),//gcIp.getBytes(),//byte[] GCIP,
			null,//byte[][] clientTable);
			getBluetoothPowerState(),
			powerUpWifi());
	
	if (DBG) Log.d(TAG, "  p2pHsM" + p2pHsM);
	return p2pHsM;
	}
	*/


	/**
	*	Create Specific collision Handover Select Message when Beam+ P2P case
	*	we force return one NDEF record which include the MAC address of Selector with small Collision number
	*       
	* @param  null
	* @return   NdefMessage  Beam+ P2P Collision Hs Message
	* @see		   null     	
	*/
	public NdefMessage createBeamPlusP2pCollisionHsM(){
		Log.i(TAG, " createBeamPlusP2pCollisionHsM  " );
        
        NdefMessage result;
		IFastConnectInfo mDefaultIFastConnectInfo; 
        mDefaultIFastConnectInfo = mMtkWifiP2pHandover.createDefaultFastConnectInfo();

		/* String AAA = "12345abcde";
		 *  byte[] CCC = AAA.getBytes();
		 *   CCC[0]:0x31 CCC[1]:0x32 ... CCC[9]:0x65
		 */
		String strWifiMacAddress = mDefaultIFastConnectInfo.getDeviceAddress();
		Log.i(TAG, "selector strWifiMacAddress   :" + strWifiMacAddress);
		byte[] wifiMacAddress = addressToByteArray(strWifiMacAddress);//strWifiMacAddress.getBytes();


        result = HandoverBuilderParser.createMtkSpecificHsM(HandoverMessage.SPECIFIC_RECORD_TYPE_HANDOVER_REQUEST_COLLISION,wifiMacAddress);
        //p2pHsM = dealBeamPlusP2pHrM(tryMessage,true);

       return result;

    }

    
    /**
    *       P2pLinkManager doGet
    *
    *       It's possible to receive three type HrM. 
    *       1.BT CCR only(JB original) 		    HR(+AC)    + BT CCR 
    *       2.BT Wifi  AUX  (P2p usage)   	            HR(+AC+AC) + BT CCR + Wifi CCR(AUX only)
    *       
    *       3.Wifi Legacy (WPS)                         HR(+AC)    + Wifi Type only
    *		
    *       We always act as Client on WFD
    *		WFD Wifi Aux (with RTSP port)        HR(+AC)    + Wifi  Aux, (with RTSP port)
    *
    *		return null , p2pLinkManager will response SnepMessage.RESPONSE_NOT_FOUND
    */
	public NdefMessage tryMtkHandoverRequest(NdefMessage tryMessage){
		//to check Url, check return
		NdefMessage p2pHsM = null;
        boolean mWifiDisplayCase = true;
            
        int NfcSendState;

            
        if (tryMessage == null) return null; 
        //if (mBluetoothAdapter == null) return null;

        NfcSendState = getP2pState();

        //byte[] tryMessageByteArray = tryMessage.toByteArray();
        if (DBG) Log.d(TAG, "tryMtkHandoverRequest():  NfcSendState:" + NfcSendState);
        Log.d(TAG, "  tryMessageByteArray "+printNdef(tryMessage));//length:"+ tryMessageByteArray.length + " Array::" + Util.bytesToString(tryMessageByteArray));

		/*
		*		1.parse HrM
		*/

		//1.1 Try Wifi AC exist or not  , if not , BT hit  JB original
        NdefRecord r = tryMessage.getRecords()[0];
        if (r.getTnf() != NdefRecord.TNF_WELL_KNOWN) return null;
        if (!Arrays.equals(r.getType(), NdefRecord.RTD_HANDOVER_REQUEST)) return null;


        //= whichScenario(tryMessage);
        HandoverMessageElement mHandoverMsgElement = new HandoverMessageElement(tryMessage);
        int mScenario = mHandoverMsgElement.mscenario;
        Log.d(TAG, "mScenario:" + mScenario);

        switch(mScenario){

            case SCENARIO_BEAMPLUS_P2P:
                if(determineConnWay(mHandoverMsgElement))
                    return mHandoverManager.tryHandoverRequest(tryMessage);
                else{
                        //AntiCollision ,compare Random number
                        if((NfcSendState == SEND_STATE_SENDING) && actAsRequester(mP2pRequesterRandom,mHandoverMsgElement.mCRArray)){
                            Log.d(TAG, "P2pLink actAsRequester return True !!!!    p2pHsM set to Specific record  return!!!!"  );
                            p2pHsM = createBeamPlusP2pCollisionHsM();
                            //p2pHsM = dealBeamPlusP2pHrM(tryMessage,true);
                        }
                        else                        
                	        p2pHsM = dealBeamPlusP2pHrM(tryMessage);

				        return p2pHsM;
                }
                
                
            case SCENARIO_WFD:
                //return mHandoverManager.tryHandoverRequest(tryMessage);
                Log.d(TAG, " should not get WFD HandoverRequestMessage   return null"  );
                return null;
                //break;

            case SCENARIO_WIFI_LEGACY:
                return dealWifiLegacyHrM(tryMessage);

            default:
            case SCENARIO_JB_ORIGINAL:
                return mHandoverManager.tryHandoverRequest(tryMessage);
        }



		//if(false == isWiFiCarrierExist(tryMessage,NdefRecord.RTD_HANDOVER_REQUEST)){
			//type 1. BT CCR only(JB original) 
		//	return mHandoverManager.tryHandoverRequest(tryMessage);
		//}

		

		//1.2 Try BT CCR exist or not
		//Credential mCredential;

		//HandoverRequest hr=null;
		//try{
		//	hr = HandoverMessage.tryParseRequest(tryMessage);
		//}catch (FormatException e){
		//	Log.e(TAG, "FormatException e = " + e);	
		//}
		
		/*
		HandoverCarrier carriers_S[] = hr.getCarriers();
		//try first CCR 
		BTCarrierConfiguration BTccr = BTCarrierConfiguration.tryParse(carriers_S[0]);
		Log.d(TAG, "BT CCR = " + Util.bytesToString(BTccr.getMacAddress()));

		if(BTccr != null){
			//Check File Size and Both Wifi Direct on or BT device on
			if(determineConnWay(tryMessage)){
				//return True :: BT  ,  Use type 1. BT CCR only(JB original) 
				return mHandoverManager.tryHandoverRequest(tryMessage);
			}
			else{
				//type 2 :: BT Wifi CCR (P2p usage)   	
				p2pHsM = dealBeamPlusP2pHrM(tryMessage);
				return p2pHsM;
			}

			
		}
		else{

            try{
                mWifiDisplayCase = HandoverBuilderParser.isAuxExistOnWifiHrM(tryMessage);
            }
            catch(FormatException e){
                Log.e(TAG, " FormatException::" + e);	
            }
            
            if(mWifiDisplayCase){
                //WFD case
                // We don't act Selector in Wifi Display
                Log.e(TAG, " doGet   return null ");	
                return null;
            }
            else{
                //Wifi Legacy case
                return dealWifiLegacyHrM(tryMessage);
            }
            
			//dealWiFiDisplayHrM(tryMessage)
			//Log.e(TAG, " doGet   return null ");	
			//Log.e(TAG, " Create Error HSM  REASON_NOT_MATCH ");	
			//return null;//HandoverBuilderParser.createErrorHsM(HandoverMessage.ERROR_RECORD_REASON_NOT_MATCH,(byte)0);
		}
        */
	
		//return p2pHsM;

	}

	
	/********************** 
		Static handover	
	***********************/
	//NfcDispather use, reName to tryTagHandover
	//boolean tryConfigurationHandover(NdefMessage tryConfigHandoverMessage){
	//	return true;
	//}



    /**
        *	The device with big Collision number will act as Requester
        *       only less will return false
        *		
        * @param  SendRandomNumber          the RandomNumber We send
        * @param  RecieveRandomNumber       the RecieveRandomNumber we receive
        * @returrn boolean  true,act as Requester
        * @see		   null     
        * @hide 
        */	
    boolean actAsRequester(byte[] SendRandomNumber,byte[] ReceiveRandomNumber){
        int mSendNumber = byteArrayToint(SendRandomNumber);
        int mReceiveNumber = byteArrayToint(ReceiveRandomNumber);

        Log.i(TAG, " actAsRequester  mSendNumber:" +mSendNumber+"   mReceiveNumber:"+mReceiveNumber ); 

        Log.i(TAG, " actAsRequester  Return :"+(mSendNumber>=mReceiveNumber) ); 

        return (mSendNumber>=mReceiveNumber)?true:false;
    }   

	private String getFilePathByContentUri(Uri uri) {
        Log.d(TAG, "getFilePathByContentUri(), uri.toString() = " + uri.toString());
	    Uri filePathUri = uri;
	    if (uri.getScheme().toString().compareTo("content")==0) {    
            Cursor cursor = null;
            try {
                cursor = mContext.getContentResolver().query(uri, null, null, null, null);
                if (cursor.moveToFirst()) {
                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);//Instead of "MediaStore.Images.Media.DATA" can be used "_data"
                    filePathUri = Uri.parse(cursor.getString(column_index));
                    Log.d(TAG, "getFilePathByContentUri : " + filePathUri.getPath());
                    return filePathUri.getPath();
                }
            } catch (Exception e) {
                Log.d(TAG, "exception...");
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
	    } 
        Log.d(TAG, "getFilePathByContentUri doesn't work, try direct getPath");
	    return uri.getPath();
	}

	//if File > 1M  return true
	boolean isBigFile(Uri[] Uris){  
	  int sizeSummary = 0;

      if(Uris == null)
        return false;
      
	  for(int i=0; i<Uris.length; i++){
	   File file = new File(getFilePathByContentUri(Uris[i]));
	   Log.d(TAG, "isBigFile::  size of file X = " + file.length());

	   sizeSummary = (int) (sizeSummary + file.length()); 
	  }
        //1024*1024=1048576
	  if (sizeSummary > 1048576){
	   return true;
	  }
	  else
	   return false;  
	}// end of isBigFile

	private void sendWFDActiveBroadcast(byte[] macAddr,String NetworkKey,String SSID,byte NetworkId ,short AuthType,short EncType,byte[] vendorId,byte[] gcIp,byte[] goIp,byte[] maxHeight,byte[] maxWidth){
        // Deal with handover-initiated transfers separately
        Intent handoverIntent = new Intent(NFC_HANDOVER_INTENT_ACTION_WFD_ACTIVE);

		// TODO:: PutExtra : mac,vendorId,GOIP,GCIP,max Width Height,Client (white)table
        handoverIntent.putExtra(EXTRA_NFC_WFD_MAC_ADDR,macAddr);
        handoverIntent.putExtra(EXTRA_NFC_WFD_SSID,SSID);
        handoverIntent.putExtra(EXTRA_NFC_WFD_NETWORK_KEY,NetworkKey);
        handoverIntent.putExtra(EXTRA_NFC_WFD_NETWORK_ID,NetworkId);
        handoverIntent.putExtra(EXTRA_NFC_WFD_AUTH_TYPE,AuthType);
        handoverIntent.putExtra(EXTRA_NFC_WFD_ENC_TYPE,EncType);

        
        handoverIntent.putExtra(EXTRA_NFC_WFD_VENDOR_ID,vendorId);

        handoverIntent.putExtra(EXTRA_NFC_WFD_GC_IP,gcIp);
        handoverIntent.putExtra(EXTRA_NFC_WFD_GO_IP,goIp);
        handoverIntent.putExtra(EXTRA_NFC_WFD_MAX_HEIGHT,maxHeight);
        handoverIntent.putExtra(EXTRA_NFC_WFD_MAX_WIDTH,maxWidth);

		Log.d(TAG, " sendWFDActiveBroadcast  sendBroadcast");

        mContext.sendBroadcast(handoverIntent);
        return;
    }

    /**
        *	Requester get Handover select Message return.
        *    Broadcast intent finally
        *	<p>
        *	<p>
        *		
        * @param  
        * @param  
        * @return	   null
        * @see		   null     
        */
	private void sendWifiLEgacyBroadcast(byte[] macAddr,String NetworkKey,String SSID,byte NetworkId ,short AuthType,short EncType){
        // Deal with handover-initiated transfers separately
        Intent handoverIntent = new Intent(INfcWpsTestBed.MTK_WPS_NFC_TESTBED_HS_RECEIVED_ACTION);

        WpsCredential mWLCredential = new WpsCredential();
        mWLCredential.setNetworkIndex(NetworkId);
        mWLCredential.setSSID(SSID);
        mWLCredential.setAuthType(AuthType);
        mWLCredential.setEncrypType(EncType);
        mWLCredential.setNetworkKey(NetworkKey);
        mWLCredential.setMacAddress(macAddr);
        //mWLCredential.setVendorExtension(byte [ ] vendorExtension)

        handoverIntent.putExtra(INfcWpsTestBed.MTK_WPS_NFC_TESTBED_EXTRA_CREDENTIAL,mWLCredential);

		Log.d(TAG, "====  sendWifiLEgacyBroadcast  sendBroadcast  ====");

        mContext.sendBroadcast(handoverIntent);
        return;
    }

    /**
        *	Requester Create Handover request Message Entry.
        *       it will dispatch to correct Create function by handoverCase
        *	<p>
        *	<p>
        *		
        * @param  uris  the URIs of beamPlus files
        * @param  handoverCase  the handoverCase is used to determine Create funciton
        * @return	   null
        * @see		   null     
        */

    public NdefMessage CreateHrMEntry(Uri[] uris,String handoverCase){
        Log.d(TAG, "    CreateHrMEntry()  handoverCase:"+handoverCase);
        NdefMessage request = null;



        if( handoverCase.equals(NfcAdapter.MTK_P2P_CASE) == true)
            request = createBeamPlusRequestMessage(uris);
        else if(handoverCase.equals(NfcAdapter.MTK_WFD_CASE) == true)
            request = createWiFiDisplayRequestMessage(uris);
        else if(handoverCase.equals(NfcAdapter.MTK_WL_REQ_CASE) == true)
            request = createWiFiLegacyRequestMessage(uris);
        else
            Log.e(TAG,"Exception: CreateHrMEntry handoverCase not match  :"+handoverCase);

        Log.d(TAG, "handoverCase:"+handoverCase +" HrM:"+ printNdef(request));
        
        return request;
    }

    /**
        *   Requester deal Handover select Message Entry.
        *       it will dispatch to correct doHandover function by handoverCase
        *   <p>
        *   <p>
        *       
        * @param  uris  the URIs of beamPlus files
        * @param  handoverCase  the handoverCase is used to determine doHandover funciton
        * @return      null
        * @see         null     
        */

    public void doHsMHandoverEntry(Uri[] uris,NdefMessage response,String handoverCase){
        Log.d(TAG, "    doHsMEntry()  handoverCase:"+handoverCase);


        //byte[] responseByteArray = response.toByteArray();
        Log.d(TAG, "  responseByteArray " + printNdef(response));

        if(handoverCase.equals(NfcAdapter.MTK_P2P_CASE) == true)
            doBeamPlusHandover(uris,response);
        else if(handoverCase.equals( NfcAdapter.MTK_WFD_CASE) == true )
            doWiFiDisplayHandover(uris,response);
        else if(handoverCase.equals(NfcAdapter.MTK_WL_REQ_CASE) == true)
            doWiFiLegacyHandover(uris,response);
        else
            Log.e(TAG,"Exception: doHsMEntry handoverCase not match  :"+handoverCase);
        

    }

    /**
    *   check mSendState SEND_STATE_SENDING of P2pLinkManager.java
    *       
    *   <p>
    *       
    * @param  Sending   SEND_STATE_SENDING of mSendState
    * @return      null
    * @see         null     
    */
    public void checkSendstateSending(int Sending)throws Exception {
    if(Sending != SEND_STATE_SENDING)
        throw new Exception("mSendState SEND_STATE_SENDING not match on P2pLinkManager!!   Sending:" +Sending);
    }
        
    public void checkSendstateNTS(int value)throws Exception {
    if(value != SEND_STATE_NOTHING_TO_SEND)
        throw new Exception("mSendState SEND_STATE_NOTHING_TO_SEND not match on P2pLinkManager!!   Sending:" +value);
    }
    
    public void checkSendstateNC(int value)throws Exception {
    if(value != SEND_STATE_NEED_CONFIRMATION)
        throw new Exception("mSendState SEND_STATE_NEED_CONFIRMATION not match on P2pLinkManager!!   Sending:" +value);
    }

    /**
    *   Set  mSendState from P2pLinkManager.java
    *       
    *   <p>
    *       
    * @param  vaule   mSendState of P2pLinkManager
    * @return      null
    * @see         null     
    * @hide
    */
    public void setP2pState(int value){
        Log.d(TAG, "setP2pState: "+sendStateToString(value));
        mSendState = value;
    }

    /**
    *   Get  mSendState from P2pLinkManager.java
    * @hide
    */            
    private int getP2pState(){
        Log.d(TAG, " getP2pState:: "+sendStateToString(mSendState));
        return mSendState;
    }

    private String sendStateToString(int state) {
        switch (state) {
            case SEND_STATE_NOTHING_TO_SEND:
                return "SEND_STATE_NOTHING_TO_SEND";
            case SEND_STATE_NEED_CONFIRMATION:
                return "SEND_STATE_NEED_CONFIRMATION";
            case SEND_STATE_SENDING:
                return "SEND_STATE_SENDING";
            default:
                return "<error>";
        }
    }

        

    	/**
	 *  this class is used to set common element on HR/HS
	 */
	public static class HandoverMessageElement {

        static final String TAG = "HandoverMessageElement";

        static final byte CARRIER_DATA_REF_LENGTH = 1;
        static final boolean HME_BDG = true;

        //static final String TAG = "Credential";
        byte[] mHandoverType;//Hr or Hs

        //CR related
        byte[] mCRArray;

        //AC related
        byte[] mAcCpsArray;
        byte[][] mAcPayloadArray;
        byte mAcCount;
        byte mWifiCPS;
        byte mBtCPS;
        byte mAuxDataCount;

        //Specific related
        byte[] mWinnerMac;

        //All related
        byte mRecordCount;
        int mscenario;

        byte mIsBigFile;

        
        ArrayList<Byte> mCpsArrayList;
        
        ArrayList<byte[]> mAcPayloadArrayList;
        

        //constructor
        HandoverMessageElement(NdefMessage m){

            try{
                ParsedMessage(m);
            }catch(FormatException fe){
                Log.e(TAG, "    FormatException fe:"+fe);
            }

            dump();
        }

        void ParsedMessage(NdefMessage m)throws FormatException {
            Log.d(TAG, "HandoverMessageElement.ParsedMesssage(remove static)");
            
            NdefRecord r0 = m.getRecords()[0];
            short r0Tnf = r0.getTnf();
            mHandoverType = r0.getType();
            byte[] r0Payload = r0.getPayload();
            mAcCount = 0;
            mRecordCount = 0;
                 
            byte version = r0Payload[0];
            byte[] acMessageBytes = new byte[r0Payload.length - 1];
            System.arraycopy(r0Payload, 1, acMessageBytes, 0, r0Payload.length - 1);
            r0Payload = null;


            
            NdefMessage ac = null; 
             
            try{
                ac = new NdefMessage(acMessageBytes);
            }catch(FormatException E){
                Log.e(TAG, "    FormatException "+E );
            }
            NdefRecord[] recordItems = ac.getRecords();

            //parse record (cr,ac)  of HR/HS
            for (NdefRecord mRecord : recordItems) {
            
                if (mRecord.getTnf() != NdefRecord.TNF_WELL_KNOWN) {
                    Log.e(TAG, "    NdefRecord.tnf  not match " );
                    //handle by JB original
                    //return SCENARIO_JB_ORIGINAL;
                    throw new FormatException(" record in HR/HS  TNF not match:"+mRecord.getTnf());
                    //continue;
                }

                if (Arrays.equals(HandoverMessage.COLLISION_RESOLUTION_RECORD_TYPE,mRecord.getType())){
                    mCRArray = mRecord.getPayload();
                    continue;
                }

                if (Arrays.equals(HandoverMessage.SPECIFIC_RECORD_TYPE,mRecord.getType())){
                    byte[] specArray = mRecord.getPayload();
                    //array[0] :: error reason
                    //array[1] :: error data
                    if(specArray[0]== HandoverMessage.SPECIFIC_RECORD_TYPE_HANDOVER_REQUEST_COLLISION){
                        //mHrCollision = true;
                        mscenario = SCENARIO_HR_COLLISION;
                        mWinnerMac = new byte[specArray.length - 1]; 
                        System.arraycopy(specArray, 1, mWinnerMac, 0, specArray.length-1 );
                        Log.e(TAG, "  Handover Request Collision Happen" );
                    }
                        
                    return;
                }

                                
                if (Arrays.equals(NdefRecord.RTD_ALTERNATIVE_CARRIER,mRecord.getType())){
                    byte[] mAcRecord = mRecord.getPayload();
                    int cursor=0;
                    byte acLength= (byte)mAcRecord.length;
                    byte carrierDataLength=0;

                    Log.i(TAG, "    AC Record  payload count:"+((acLength-2)/2)+"  acLength:"+acLength );
                     
                    ByteBuffer acPayload = ByteBuffer.allocate((acLength-2)/2);//-2 :: means cps 1byte, AuxRefCount 1byte,


                    if(mCpsArrayList == null)
                        mCpsArrayList = new ArrayList<Byte>();
                    
                    mCpsArrayList.add(mAcRecord[cursor++]);
                        
                    //mAcCpsArray[mAcCount] = mAcRecord[cursor++];

                    carrierDataLength = mAcRecord[cursor++];
                    if(carrierDataLength != CARRIER_DATA_REF_LENGTH)
                        throw new FormatException(" CARRIER_DATA_REF_LENGTH !=1  value:"+carrierDataLength);

                    acPayload.put(mAcRecord[cursor++]);

                    mAuxDataCount = mAcRecord[cursor++];

                    //Log.i(TAG, "   cursor should be 4    cursor:"+cursor );
                    
                    while(cursor < acLength){
                        carrierDataLength = mAcRecord[cursor++];
                        if(carrierDataLength != CARRIER_DATA_REF_LENGTH)
                            throw new FormatException(" CARRIER_DATA_REF_LENGTH !=1  value:"+carrierDataLength);

                        acPayload.put(mAcRecord[cursor++]);
                    }

                    if(mAcPayloadArrayList == null)
                        mAcPayloadArrayList = new ArrayList<byte[]>();
                        
                    mAcPayloadArrayList.add(acPayload.array());
                    //mAcPayloadArray[mAcCount]=acPayload.array();
                    
                    mAcCount++;
                    Log.i(TAG, "    mAcCount:"+mAcCount);
                    
                }
             }

            int cpsSize = mCpsArrayList.size();
            if(cpsSize == 1)
            Log.i(TAG, "    mCpsArrayList.size():"+cpsSize+"(==1)  [0]:"+mCpsArrayList.get(0) );
            else if(cpsSize >= 2)
            Log.i(TAG, "    mCpsArrayList.size():"+cpsSize+"  [0]:"+mCpsArrayList.get(0)+"   [1]:"+mCpsArrayList.get(1) );
            
            mAcCpsArray = new byte[cpsSize];
            mAcPayloadArray = new byte[cpsSize][];

            int i;
            for(i=0;i<cpsSize;i++){
            	mAcCpsArray[i]=mCpsArrayList.get(i);
                byte[] test= (byte[]) mAcPayloadArrayList.get(i);
                Log.i(TAG, "    mAcPayload:"+Util.bytesToString(test));
                mAcPayloadArray[i]= test;
            }

              Log.i(TAG, "    mAcCpsArray:"+Util.bytesToString(mAcCpsArray));
            
              for (NdefRecord mRecord : m.getRecords()){
                  if(mRecord!=null){

                      mRecordCount++;
                      
                      if(mRecord.getTnf() == NdefRecord.TNF_MIME_MEDIA&& Arrays.equals(mRecord.getType(), HandoverMessage.OCTET_STREAM) &&
                          Arrays.equals(mHandoverType, NdefRecord.RTD_HANDOVER_REQUEST) && mRecordCount==6){
                           byte[] mdata= mRecord.getPayload();

                           if(mdata.length != 1)
                            throw new FormatException("mRecordCount:"+mRecordCount+"  IsBigFile not match : "+Util.bytesToString(mdata));
                           
                           mIsBigFile = mdata[0];
                           Log.i(TAG, " mRecordCount:"+mRecordCount+"    mIsBigFile assign:"+mIsBigFile);

                      }
                          
                  }
                  
              }


            
             switch(mAcCount){
                 case 2:
                    mscenario = SCENARIO_BEAMPLUS_P2P;
                    mBtCPS = mAcCpsArray[0];
                    mWifiCPS = mAcCpsArray[1];
                    Log.i(TAG, "    mAcPayload:"+Util.bytesToString(mAcCpsArray));
                     //return SCENARIO_BEAMPLUS_P2P;
                     break;
                 case 1:
                     //BT, WFD , Legacy
                     NdefRecord r1 = m.getRecords()[1];
                     short r1TNF = r1.getTnf();
                     byte[] r1Type = r1.getType();
                     byte[] r1Payload = r1.getPayload();
             
                     if(r1TNF == NdefRecord.TNF_MIME_MEDIA && Arrays.equals(r1Type,BTCarrierConfiguration.BT_CARRIER_TYPE.getBytes())) {
                            mscenario = SCENARIO_JB_ORIGINAL;
                            mBtCPS = mAcCpsArray[0];
                            break;
                         //return SCENARIO_JB_ORIGINAL;
                     }


                     Log.i(TAG, "    mRecordCount:"+mRecordCount );
                     
                     if(r1TNF == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(r1Type, NdefRecord.RTD_HANDOVER_CARRIER) &&
                         Arrays.equals(mHandoverType, NdefRecord.RTD_HANDOVER_REQUEST) && mRecordCount==2){
                         //Legacy Hr case only, (Hr(cr+ac)+HCR)
                         Log.e(TAG, "    SCENARIO_WIFI_LEGACY " );
                         mscenario = SCENARIO_WIFI_LEGACY;
                         mWifiCPS = mAcCpsArray[0];
                         break;
                     }
                     
                     if(r1TNF == NdefRecord.TNF_MIME_MEDIA && Arrays.equals(r1Type,WifiCarrierConfiguration.TYPE.getBytes()) &&
                         Arrays.equals(mHandoverType, NdefRecord.RTD_HANDOVER_SELECT)){
                         //WFD Hs    : Hr(cr+ac)+wifiCCR with TLV
                         //Legacy Hs : Hr(cr+ac)+wifiCCR
            
            
                         if(elementExistInTLVByteArray(r1Payload,HandoverBuilderParser.WPS_ATTRIBUTE_TYPE_VENDOR_ID)){
                            mscenario = SCENARIO_WFD;
                            Log.i(TAG, "    SCENARIO_WFD " );
                         }
                         else{
                            Log.i(TAG, "    SCENARIO_WIFI_LEGACY " );
                            mscenario = SCENARIO_WIFI_LEGACY;
                         }
                         
                         mWifiCPS = mAcCpsArray[0];

                         break;
                     }
                     
                     Log.e(TAG, " Error case handle by  SCENARIO_JB_ORIGINAL  mAcCount== 1" );
                     mscenario = SCENARIO_JB_ORIGINAL;
                     break;                

                 case 0:
                 default:
                    Log.e(TAG, " Error case -- handle by  SCENARIO_JB_ORIGINAL  mAcCount == 0" );
                     mscenario =  SCENARIO_JB_ORIGINAL;
                     break;
            
             }
        }
        void dump(){
            int i=0;
                            
            if(HME_BDG){
                Log.i(TAG, "  mHandoverType:"+Util.bytesToString(mHandoverType) );

                Log.i(TAG, "  mCRArray:"+Util.bytesToString(mCRArray) );
                
                Log.i(TAG, "  mAcCpsArray:"+Util.bytesToString(mAcCpsArray) );

                for(i=0;i<mAcCount;i++)
                    Log.i(TAG, "  mAcPayloadArray["+i+ "]:"+Util.bytesToString(mAcPayloadArray[i]) );
                
                Log.i(TAG, "  mAcCount:"+mAcCount );
                Log.i(TAG, "  mBtCPS:"+mBtCPS );
                Log.i(TAG, "  mWifiCPS:"+mWifiCPS );
                Log.i(TAG, "  mRecordCount:"+mRecordCount);
                Log.i(TAG, "  mscenario:"+mscenario+"  0:JB_Ori  1:P2P  2:WFD  3:WL" );
                
            }
        }
        
    }

    /**
    *   Requester deal Handover select Message Entry.
    *       it will dispatch to correct doHandover function by handoverCase
    *   <p>
    *   <p>
    *       
    * @param  handoverCase  the handoverCase is used to determine doHandover funciton
    * @return      null
    * @see         null     
    */
    public void closeForegroundDispatchActivity(String handoverCase){
    	PendingIntent mPendingIntent;

        Log.i(TAG, "  closeForegroundDispatchActivity() handoverCase" +handoverCase);

        if(handoverCase == null)
            return;
        
        if(handoverCase.equals(NfcAdapter.MTK_WL_REQ_CASE) || handoverCase.equals(NfcAdapter.MTK_WL_SEL_CASE)){
            //return;
            setInternalCmdToForegroundDispatchActivity(INfcWpsAppInternal.HANDOVER_FINISH_CMD);
            mWpsCredential = null;
        }
    
    }

    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(INfcWpsTestBed.MTK_WPS_NFC_TESTBED_HR_ACTION)) {
                Log.d(TAG, "mReceiver: MTK_WPS_NFC_TESTBED_HR_ACTION");
                //Uri uri0 = Uri.parse("file:///tmp/dummy.txt");
                //Uri[] mUriArray ={uri0};

                mUI_Handler.postDelayed(runnable_postReqNewIntent, 1000);

                return;
            } else if (action.equals(INfcWpsTestBed.MTK_WPS_NFC_TESTBED_HS_ACTION)){
                Log.d(TAG, "mReceiver: MTK_WPS_NFC_TESTBED_HS_ACTION S");
                
                Parcelable parcelableCredential = (Parcelable) intent
                .getParcelableExtra(INfcWpsTestBed.MTK_WPS_NFC_TESTBED_EXTRA_CREDENTIAL);

                mWpsCredential = (WpsCredential) parcelableCredential;

                mUI_Handler.postDelayed(runnable_postSelNewIntent, 1000);

                return;
            }else
            	Log.e(TAG, "mReceiver: error Receiver case");
        }
    };

    private void setInternalCmdToForegroundDispatchActivity(int cmd){
        
        Log.i(TAG, "  setInternalCmdToForegroundDispatchActivity() cmd:" +cmd);
        // start Nfc Foreground Dispatch activity with command [2]
        Intent intentHr = new Intent(mContext,
                NfcForegroundDispatchActivity.class);
        intentHr.putExtra(INfcWpsAppInternal.EXTRA_NFC_WPS_INTERNAL_CMD, cmd);
        
        //set android:launchMode="singleTask" at Manifest.xml
        intentHr.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intentHr.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mContext.startActivity(intentHr);
    }

    /**
        *   is Wifi Legacy Selector
        *       it will dispatch to correct doHandover function by handoverCase
        *   <p>
        *   <p>
        *       
        * @param  uris  the URIs of beamPlus files
        * @param  handoverCase  the handoverCase is used to determine doHandover funciton
        * @return      null
        * @see         null     
        */

    public boolean isWLSelector(String handoverCase){
        
        Log.i(TAG, "  isWLSelector() handoverCase:" +handoverCase);
        if(handoverCase == null)
            return false;

        
        if(handoverCase.equals(NfcAdapter.MTK_WL_SEL_CASE)){
            
            Log.i(TAG, "  isWLSelector()   return True :: Set URI to null");
            return true;
        }

        return false;
    }

    
    private Runnable runnable_postReqNewIntent = new Runnable() {
        public void run() {

        Log.d(TAG, "========================================= ");
        Log.d(TAG, "runnable_postReqNewIntent ");
        Log.d(TAG, "========================================= ");

        setInternalCmdToForegroundDispatchActivity(INfcWpsAppInternal.HANDOVER_REQUEST_CMD);

        }
    };

    private Runnable runnable_postSelNewIntent = new Runnable() {
        public void run() {

        Log.d(TAG, "========================================= ");
        Log.d(TAG, "runnable_postSelNewIntent ");
        Log.d(TAG, "========================================= ");

        setInternalCmdToForegroundDispatchActivity(INfcWpsAppInternal.HANDOVER_SELECT_CMD);

        }
    };    

}

