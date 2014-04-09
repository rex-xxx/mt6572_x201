package com.mediatek.nfc.wps;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;



import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Parcelable;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.res.Configuration;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;


import com.mediatek.nfc.handover.Util;
import android.nfc.wps.INfcWpsTestBed;
import android.nfc.wps.ConfigurationToken;
import android.nfc.wps.PasswordToken;

import com.android.nfc.R;
//import com.example.helloworld.R;

public class NfcForegroundDispatchActivity extends Activity {
    static final String TAG = "NfcForegroundDispatchActivity";
    static final boolean DBG = true;

	NfcManager mManager;
	NfcAdapter mAdapter;
	PendingIntent pendingIntent;
	IntentFilter[] intentFiltersArray;
	String[][] techListsArray;

	static int command = 0;
	static PasswordToken mPasswordToken;
	static ConfigurationToken mConfigurationToken;

	private Handler mUI_Handler = new Handler();

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    Log.d(TAG, " onCreate() :");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mtk_wps_using_nfc_foreground_dispatch);

        setTitle("NFC Foreground Dispatch");
        
        TextView mTextView0 = (TextView) findViewById(R.id.textView0);
        mTextView0.setVisibility(View.VISIBLE);
        mTextView0.setText("Put this device near to a NDEF tag/P2P device");


		Intent iii = this.getIntent();
		command = iii.getIntExtra(INfcWpsAppInternal.EXTRA_NFC_WPS_CMD, 0);

		// if command = 2, get pswToken
		if (command == INfcWpsAppInternal.WRITE_PASSWORD_TOKEN_CMD) {
            Log.d(TAG, " command == INfcWpsAppInternal.WRITE_PASSWORD_TOKEN_CMD");
			Parcelable parcelablePswToken = (Parcelable) iii
					.getParcelableExtra(INfcWpsAppInternal.EXTRA_NFC_WPS_PWD_TOKEN);
			mPasswordToken = (PasswordToken) parcelablePswToken;
            
		}

		// if command = 4, get configToken
		if (command == INfcWpsAppInternal.WRITE_CONFIGURATION_TOKEN_CMD) {
            Log.d(TAG, " command == INfcWpsAppInternal.WRITE_CONFIGURATION_TOKEN_CMD");
			Parcelable parcelableCfgToken = (Parcelable) iii
					.getParcelableExtra(INfcWpsAppInternal.EXTRA_NFC_WPS_CONFIGURATION_TOKEN);
			mConfigurationToken = (ConfigurationToken) parcelableCfgToken;
		}

		Log.d(TAG, " EXTRA_NFC_WPS_CMD = " + command);

		NfcManager mManager = (NfcManager) this
				.getSystemService(Context.NFC_SERVICE);
		mAdapter = mManager.getDefaultAdapter();

		//
		pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

		//
		IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
		try {
			ndef.addDataType("*/*"); /*
									 * Handles all MIME based dispatches. You
									 * should specify only the ones that you
									 * need.
									 */
		} catch (MalformedMimeTypeException e) {
			throw new RuntimeException("fail", e);
		}

        IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);                                     
        IntentFilter tag = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);                                     

		intentFiltersArray = new IntentFilter[] { ndef,tech,tag };
		techListsArray = new String[][] { new String[] { NfcF.class.getName() }, new String[] { Ndef.class.getName() } };
	}

	public void onRestart() {
        Log.d(TAG, " onRestart() : command"+command);     
		super.onRestart();
	}

	public void onPause() {
        Log.d(TAG, " onPause() : command"+command);     
		super.onPause();
		mAdapter.disableForegroundDispatch(this);
	}

	public void onResume() {
        Log.d(TAG, " onResume() : command"+command);     
		super.onResume();
        Log.d(TAG, " mAdapter.enableForegroundDispatch"); 
		mAdapter.enableForegroundDispatch(this, pendingIntent,
				intentFiltersArray, techListsArray);
	}

	public void onNewIntent(Intent intent) {
        Log.d(TAG, " onNewIntent() :");        
		Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

		byte[] readPayload = new byte[] {};

        if(tagFromIntent == null){
            Log.d(TAG, "========================================= ");
            Log.d(TAG, "onNewIntent()   tagFromIntent == null  ");
            int testCommand = intent.getIntExtra(INfcWpsAppInternal.EXTRA_NFC_WPS_INTERNAL_CMD, 0);
            Log.d(TAG, "     WPS_INTERNAL_CMD:"+testCommand);             
            Log.d(TAG, "========================================= ");
            if(testCommand == INfcWpsAppInternal.HANDOVER_REQUEST_CMD)
                mAdapter.setMtkLegacyPushUris(new Uri[0],this,true);
            if(testCommand == INfcWpsAppInternal.HANDOVER_SELECT_CMD)
                mAdapter.setMtkLegacyPushUris(new Uri[0],this,false);            
            else if(testCommand == INfcWpsAppInternal.HANDOVER_FINISH_CMD)
                mUI_Handler.postDelayed(runnable_finish, 1000);
            
            //Wifi Legacy handle, update UI string ,close this activity
            return;
        }
        else
            Log.d(TAG, "onNewIntent()   tagFromIntent.toString() " + tagFromIntent.toString());

		switch (command) {

		case INfcWpsAppInternal.READ_CONFIGURATION_TOKEN_CMD:
            Log.d(TAG, " READ_CONFIGURATION_TOKEN_CMD :");
			// Read Configuration Token
			// 1. read a configuration token Ndef record
			readPayload = readTag(tagFromIntent);
			mUI_Handler.post(runnable_readCfgTag);

			// 2. parse the token and get the Cfg, set into ConfigurationToken
			ConfigurationToken config = parseCfgTokenNdefPayloadToConfigurationToken(readPayload);
			mUI_Handler.postDelayed(runnable_parseCfgTag, 1000);

			// 3. send a broadcast with Cfg
            Log.d(TAG, "==== sendBroadcast(intentReadCfgToken);");
			Intent intentRCR = new Intent(INfcWpsTestBed.MTK_WPS_NFC_TESTBED_CONFIGURATION_RECEIVED_ACTION);
            
			intentRCR.putExtra(INfcWpsTestBed.MTK_WPS_NFC_TESTBED_EXTRA_CONFIGURATION,
					config);
			sendBroadcast(intentRCR);

			mUI_Handler.postDelayed(runnable_sendCfgIntent, 2000);
			mUI_Handler.postDelayed(runnable_finish, 3000);

			break;

		case INfcWpsAppInternal.WRITE_PASSWORD_TOKEN_CMD:
			// Write Password Token
			// 1. get the PWD from the intent
			Log.d(TAG, " WRITE_PASSWORD_TOKEN_CMD :");
			PasswordToken pwdToken = mPasswordToken;

            if(pwdToken == null)
                Log.e(TAG, " pwdToken == null ");

			mUI_Handler.post(runnable_getPwd);

			Log.d(TAG, "" + new String(pwdToken.getPublicKeyHash()));

			// 2. build a NDEF record with PWD
			byte[] payloadP = buildPswTokenNdefPayload(pwdToken.getPwdId(),
					pwdToken.getPublicKeyHash(), pwdToken.getDevPwd(),
					pwdToken.getVendorEx());
			mUI_Handler.postDelayed(runnable_buildPwdNdef, 1000);

			Log.d(TAG, "After build Ndef");

			// 3. write the PWD into a token
			try {
				writeTag(tagFromIntent, payloadP);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			mUI_Handler.postDelayed(runnable_writePwdToken, 2000);
			mUI_Handler.postDelayed(runnable_finish, 3000);

			break;

		case INfcWpsAppInternal.READ_PASSWORD_TOKEN_CMD:
            Log.d(TAG, " READ_PASSWORD_TOKEN_CMD :");
			// Read Password Token
			// 1. read a password token Ndef record
			readPayload = readTag(tagFromIntent);
			mUI_Handler.post(runnable_readPwdTag);

			// 2. parse the token and get the PSW, set into PasswordToken
			PasswordToken psw = parsePwdTokenNdefPayloadToPasswordToken(readPayload);
			mUI_Handler.postDelayed(runnable_parsePwdTag, 1000);

			// 3. send a broadcast with PSW
			Log.d(TAG, "==== sendBroadcast(intentReadPwdToken);");
			Intent intentRPR = new Intent(
					INfcWpsTestBed.MTK_WPS_NFC_TESTBED_PASSWORD_RECEIVED_ACTION);
			intentRPR.putExtra(INfcWpsTestBed.MTK_WPS_NFC_TESTBED_EXTRA_PASSWORD, psw);
			sendBroadcast(intentRPR);

			mUI_Handler.postDelayed(runnable_sendPwdIntent, 2000);
			mUI_Handler.postDelayed(runnable_finish, 3000);

			break;

		case INfcWpsAppInternal.WRITE_CONFIGURATION_TOKEN_CMD:
            
            Log.d(TAG, " WRITE_CONFIGURATION_TOKEN_CMD :");
			// Write Configuration Token
			// 1. get the CFG from the intent

			ConfigurationToken cfgToken = mConfigurationToken;

            if(cfgToken == null)
                Log.e(TAG, " cfgToken == null ");
            
			mUI_Handler.post(runnable_getCfg);

			// 2. build a NDEF record with CFG
			byte[] payloadC = buildCfgTokenNdefPayload(
					cfgToken.getNetworkIndex(), cfgToken.getSSID(),
					cfgToken.getAuthType(), cfgToken.getEncrypType(),
					cfgToken.getMacAddress(), cfgToken.getNetworkKey(),
					cfgToken.getVendorExtension());
			mUI_Handler.postDelayed(runnable_buildCfgNdef, 1000);

			// 3. write the CFG into a token
			try {
				writeTag(tagFromIntent, payloadC);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			mUI_Handler.postDelayed(runnable_writeCfgToken, 2000);
			mUI_Handler.postDelayed(runnable_finish, 3000);

			break;

		default:
			break;
		}
	}// end of onNewIntent

	// 1
	private Runnable runnable_readCfgTag = new Runnable() {
		public void run() {
			TextView tt1_1 = (TextView) findViewById(R.id.textView1);
			tt1_1.setVisibility(View.VISIBLE);
			tt1_1.setText("Reading a Cfg Tag.");//R.string.reading_Cfg_tag);
		}
	};

	private Runnable runnable_parseCfgTag = new Runnable() {
		public void run() {
			TextView tt1_2 = (TextView) findViewById(R.id.textView2);
			tt1_2.setVisibility(View.VISIBLE);
			tt1_2.setText("Parsing a Cfg Tag.");//R.string.parsing_Cfg_tag);
		}
	};

	private Runnable runnable_sendCfgIntent = new Runnable() {
		public void run() {
			TextView tt1_3 = (TextView) findViewById(R.id.textView3);
			tt1_3.setVisibility(View.VISIBLE);
			tt1_3.setText("Sending a Cfg Intent.");//R.string.sending_Cfg_intent);
		}
	};

	// 2
	private Runnable runnable_getPwd = new Runnable() {
		public void run() {
			TextView tt2_1 = (TextView) findViewById(R.id.textView1);
			tt2_1.setVisibility(View.VISIBLE);
			tt2_1.setText("Getting Pwd from Intent.");//R.string.getting_Pwd_from_intent);
		}
	};

	private Runnable runnable_buildPwdNdef = new Runnable() {
		public void run() {
			TextView tt2_2 = (TextView) findViewById(R.id.textView2);
			tt2_2.setVisibility(View.VISIBLE);
			tt2_2.setText("Building Pwd Ndef.");//R.string.building_Pwd_Ndef_record);
		}
	};

	private Runnable runnable_writePwdToken = new Runnable() {
		public void run() {
			TextView tt2_3 = (TextView) findViewById(R.id.textView3);
			tt2_3.setVisibility(View.VISIBLE);
			tt2_3.setText("Writting Pwd Token.");//R.string.writting_Pwd_token);
		}
	};

	// 3
	private Runnable runnable_readPwdTag = new Runnable() {
		public void run() {
			TextView tt3_1 = (TextView) findViewById(R.id.textView1);
			tt3_1.setVisibility(View.VISIBLE);
			tt3_1.setText("Reading a Pwd Tag.");//R.string.reading_Pwd_tag);
		}
	};

	private Runnable runnable_parsePwdTag = new Runnable() {
		public void run() {
			TextView tt3_2 = (TextView) findViewById(R.id.textView2);
			tt3_2.setVisibility(View.VISIBLE);
			tt3_2.setText("parsing a Pwd Tag.");//R.string.parsing_Pwd_tag);
		}
	};

	private Runnable runnable_sendPwdIntent = new Runnable() {
		public void run() {
			TextView tt3_3 = (TextView) findViewById(R.id.textView3);
			tt3_3.setVisibility(View.VISIBLE);
			tt3_3.setText("sending a Pwd Intent.");//R.string.sending_Pwd_intent);
		}
	};

	// 4
	private Runnable runnable_getCfg = new Runnable() {
		public void run() {
			TextView tt4_1 = (TextView) findViewById(R.id.textView1);
			tt4_1.setVisibility(View.VISIBLE);
			tt4_1.setText("Getting Cfg from Intent.");//R.string.getting_Cfg_from_intent);
		}
	};

	private Runnable runnable_buildCfgNdef = new Runnable() {
		public void run() {
			TextView tt4_2 = (TextView) findViewById(R.id.textView2);
			tt4_2.setVisibility(View.VISIBLE);
			tt4_2.setText("building Cfg Ndef.");//R.string.building_Cfg_Ndef_record);
		}
	};

	private Runnable runnable_writeCfgToken = new Runnable() {
		public void run() {
			TextView tt4_3 = (TextView) findViewById(R.id.textView3);
			tt4_3.setVisibility(View.VISIBLE);
			tt4_3.setText("writting Cfg Token.");//R.string.writting_Cfg_token);
		}
	};

	private Runnable runnable_finish = new Runnable() {
		public void run() {
            Log.d(TAG, "runnable_finish()    finish() ");
			finish();
		}
	};

	/*
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.mtk_wps_using_nfc_foreground_dispatch, menu);
		return true;
	}
	*/

	private void writeTag(Tag tag_in, byte[] payload)
			throws UnsupportedEncodingException {
		Log.d(TAG, "write tag begin...");
		Log.d(TAG, "prepared payload = " + new String(payload));
		if (tag_in == null) {
			Log.d(TAG, "tag = null");
			return;
		}

		Ndef tag = Ndef.get(tag_in);
		Log.d(TAG, "after get tag~");
		NdefRecord record = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
				"application/vnd.wfa.wsc".getBytes(), null, payload);
		// dumpBytes(record.toByteArray());

		try {
			Log.d(TAG, "into try~~");
			NdefRecord[] records = { record };
			NdefMessage message = new NdefMessage(records);
            if(tag == null)
                Log.d(TAG, "tag == null");
            
			tag.connect();
			boolean connected = tag.isConnected();
            Log.d(TAG, "into try step 2~~");
			boolean writeable = tag.isWritable();
            Log.d(TAG, "into try step 3~~");
			if (connected && writeable) {
                
                byte[] tryMessageByteArray = message.toByteArray();
                
                Log.d(TAG, "  Write message length:" + tryMessageByteArray.length);
                Log.d(TAG, "  Write message: ::" + Util.bytesToString(tryMessageByteArray));
                
				tag.writeNdefMessage(message);
			}
            Log.d(TAG, "into try step 4~~");
			tag.close();
		} catch (Exception e) {
			// do error handling
			Log.d(TAG, "got Exception when writting this tag..." + e);
		}
		Log.d(TAG, "==== write tag end.");
	} // end of writeTag

	byte[] buildPswTokenNdefPayload(int pwdId, byte[] publicKeyHash,
			byte[] devPwd, byte[] vendorEx) {

        Log.d(TAG, "buildPswTokenNdefPayload()");

        if(DBG) Log.d(TAG, "pwdId:"+pwdId);
        if(DBG) Log.d(TAG, "publicKeyHash length:"+publicKeyHash.length+"  []:"+ Util.bytesToString(publicKeyHash));
        if(DBG) Log.d(TAG, "devPwd length:"+devPwd.length+"  []:"+ Util.bytesToString(devPwd));
        if(DBG) Log.d(TAG, "vendorEx length:"+vendorEx.length+"  []:"+ Util.bytesToString(vendorEx));

		int dataSize = 2 + publicKeyHash.length + devPwd.length
				+ vendorEx.length + 9;
		ByteBuffer bBuf = ByteBuffer.allocateDirect(dataSize);

		// Version Attribute ID
		bBuf.put(PasswordToken.mPASSWORDTOKEN_ATTRIBUTE_ID_VERSION);
		// Version length
		bBuf.put(new byte[] { 0x00, 0x01 });
		// Version value
		bBuf.put(PasswordToken.mPASSWORDTOKEN_VERSION_10);

		// Device Password Attribute ID
		bBuf.put(PasswordToken.mPASSWORDTOKEN_ATTRIBUTE_ID_OOB_DEVICE_PASSWORD);
		// Device Password length (2 bytes Hex)
		byte[] AttrLen = intToByteArray(publicKeyHash.length + 2
				+ devPwd.length);
		bBuf.put(AttrLen);
		// Device Password value
		bBuf.put(publicKeyHash);
		bBuf.put(intToByteArray(pwdId));
		bBuf.put(devPwd);

		// handle Vendor Extension
		bBuf.put(vendorEx);
		
		/*// Vendor Extension Attribute Id
		bBuf.put(PasswordToken.mPASSWORDTOKEN_ATTRIBUTE_ID_VENDOR_EXTENSION);
		// Vendor extension length
		bBuf.put(new byte[] { 0x00, 0x06 });
		// Vendor Extension value
		bBuf.put(vendorId);
		// Sub element Id
		bBuf.put(new byte[] { 0x00 });
		// Sub element length
		bBuf.put(new byte[] { 0x01 });
		// Sub element value
		bBuf.put(PasswordToken.mPASSWORDTOKEN_VERSION_20);*/

		return bBuf.array();
	}// end of buildPswTokenNdefPayload

	byte[] buildCfgTokenNdefPayload(byte[] networkIndex, byte[] ssid,
			byte[] authType, byte[] encrypType, byte[] macAddress,
			byte[] networkKey, byte[] vendorExtension) {

        Log.d(TAG, "buildCfgTokenNdefPayload()");

        if(DBG) Log.d(TAG, "networkIndex:"+networkIndex);
        if(DBG) Log.d(TAG, "ssid length:"+ssid.length+"  []:"+ Util.bytesToString(ssid));
        if(DBG) Log.d(TAG, "authType length:"+authType.length+"  []:"+ Util.bytesToString(authType));
        if(DBG) Log.d(TAG, "encrypType length:"+encrypType.length+"  []:"+ Util.bytesToString(encrypType));
        if(DBG) Log.d(TAG, "macAddress length:"+macAddress.length+"  []:"+ Util.bytesToString(macAddress));
        if(DBG) Log.d(TAG, "networkKey length:"+networkKey.length+"  []:"+ Util.bytesToString(networkKey));
        if(DBG) Log.d(TAG, "vendorExtension length:"+vendorExtension.length+"  []:"+ Util.bytesToString(vendorExtension));


		int dataSize = networkIndex.length + ssid.length + authType.length
				+ encrypType.length + macAddress.length + networkKey.length
				+ 33 + vendorExtension.length;		
		ByteBuffer bBuf = ByteBuffer.allocateDirect(dataSize);

		// Version Attribute ID
		bBuf.put(ConfigurationToken.mCONFIGURATION_ATTRIBUTE_ID_VERSION);
		// Version length
		bBuf.put(new byte[] { 0x00, 0x01 });
		// Version value
		bBuf.put(ConfigurationToken.mCONFIGURATIONTOKEN_VERSION_10);

		// Device Configuration Attribute ID
		bBuf.put(ConfigurationToken.mCONFIGURATION_ATTRIBUTE_ID_CRIDENTIAL);
		// Device Configuration length (2 bytes Hex)
		byte[] AttrLen = intToByteArray(dataSize + 24 + vendorExtension.length);
		bBuf.put(AttrLen);

		// NetworkIndex Attribute ID
		bBuf.put(ConfigurationToken.mCONFIGURATION_ATTRIBUTE_ID_NETWORK_INDEX);
		// NetworkIndex length
		bBuf.put(intToByteArray(networkIndex.length));
		// NetworkIndex value
		bBuf.put(networkIndex);

		// SSID Attribute ID
		bBuf.put(ConfigurationToken.mCONFIGURATION_ATTRIBUTE_ID_SSID);
		// SSID length
		bBuf.put(intToByteArray(ssid.length));
		// SSID value
		bBuf.put(ssid);

		// AuthType Attribute ID
		bBuf.put(ConfigurationToken.mCONFIGURATION_ATTRIBUTE_ID_AUTHENTICATION_TYPE);
		// AuthType length
		bBuf.put(intToByteArray(authType.length));
		// AuthType value
		bBuf.put(authType);

		// EncrypType Attribute ID
		bBuf.put(ConfigurationToken.mCONFIGURATION_ATTRIBUTE_ID_ENCRYPTION_TYPE);
		// EncrypType length
		bBuf.put(intToByteArray(encrypType.length));
		// EncrypType value
		bBuf.put(encrypType);

		// NetworkKey Attribute ID
		bBuf.put(ConfigurationToken.mCONFIGURATION_ATTRIBUTE_ID_NETWORK_KEY);
		// NetworkKey length
		bBuf.put(intToByteArray(networkKey.length));
		// NetworkKey value
		bBuf.put(networkKey);

		// MacAddress Attribute ID
		bBuf.put(ConfigurationToken.mCONFIGURATION_ATTRIBUTE_ID_MAC_ADDRESS);
		// MacAddress length
		bBuf.put(intToByteArray(macAddress.length));
		// MacAddress value
		bBuf.put(macAddress);

		// handle Vendor Extension
		bBuf.put(vendorExtension);				

		return bBuf.array();
	}// end of buildCfgTokenNdefPayload

	private byte[] readTag(Tag tag_in) {
		if (tag_in == null) {
			Log.d(TAG, "tag = null");
			return null;
		}
		Ndef tag = Ndef.get(tag_in);
		byte[] tokenPayload = null;
		try {
			tag.connect();
			boolean connected = tag.isConnected();
			if (connected) {

				Log.d(TAG, "readTag() into connected");
				NdefMessage message = tag.getNdefMessage();
				NdefRecord[] records = message.getRecords();

				// log
				for (int i = 0; i < records.length; i++) {

					Log.d(TAG,
							"readTag() getType = " + new String(records[i].getType()));
					if (new String(records[i].getType())
							.equals("application/vnd.wfa.wsc")) {

						Log.d(TAG, "readTag() Type = application/vnd.wfa.wsc");
						tokenPayload = records[i].getPayload();
						return tokenPayload;
					}
				}
			}
			tag.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return tokenPayload;
	}// end of readTag

	private void dumpBytes(byte[] in) {
		StringBuilder builder = new StringBuilder();
		for (int counter = 0; counter < in.length; counter++) {
			builder.append(Integer.toHexString(in[counter] & 0xFF)).append(" ");
		}

		Log.d(TAG, "tag content =" + builder.toString());
		// System.out.println(builder.toString());
	}// end of dumpBytes

	PasswordToken parsePwdTokenNdefPayloadToPasswordToken(
			byte[] passwordTokenPayload) {

		Log.d(TAG, "Into parse PwdToken");

		PasswordToken PwdToken = new PasswordToken();

		if (passwordTokenPayload == null) {
			Log.d(TAG, "passwordTokenPayload == null");
			return null;
		}

		if ((passwordTokenPayload[5] == 0x10)
				&& (passwordTokenPayload[6] == 0x2c)) {

			byte[] pubKeyHash = new byte[20];
			byte[] pwdId = new byte[2];
			byte[] devPwd = new byte[16];
			byte[] vendorEx = new byte[passwordTokenPayload.length - 47];

			System.arraycopy(passwordTokenPayload, 9, pubKeyHash, 0, 20);
			System.arraycopy(passwordTokenPayload, 29, pwdId, 0, 2);
			System.arraycopy(passwordTokenPayload, 31, devPwd, 0, 16);
			System.arraycopy(passwordTokenPayload, 47, vendorEx, 0, passwordTokenPayload.length - 47);			

			int intPwdId = byteArrayToInt(pwdId);

			try {
				Log.d(TAG, "1.parse: Password ID = "
						+ getHexString(pwdId));
				Log.d(TAG, "2.parse: Public Key Hash = "
						+ getHexString(pubKeyHash));
				Log.d(TAG, "3.parse:  Device Password = "
						+ getHexString(devPwd));
				Log.d(TAG, "4.parse:  Vendor Extension = "
						+ getHexString(vendorEx));

			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
			PwdToken.setPwdId(intPwdId);
			PwdToken.setPublicKeyHash(pubKeyHash);
			PwdToken.setDevPwd(devPwd);
			PwdToken.setVendorEx(vendorEx);

		}
/*
		if ((passwordTokenPayload[47] == 0x10)
				&& (passwordTokenPayload[48] == 0x49)) {
			if (passwordTokenPayload[50] == 0x06) {
				byte[] venId = new byte[3];

				System.arraycopy(passwordTokenPayload, 51, venId, 0, 3);
				PwdToken.setVendorId(venId);
			}
		}*/

		return PwdToken;
	}// end of parsePwdTokenNdefPayloadToPasswordToken

	ConfigurationToken parseCfgTokenNdefPayloadToConfigurationToken(
			byte[] configurationTokenPayload) {

		ConfigurationToken CfgToken = new ConfigurationToken();

		Log.d(TAG, "Into parse CfgToken");
		if (configurationTokenPayload == null) {
			Log.d(TAG, "configurationTokenPayload == null");
			return null;
		}

		if ((configurationTokenPayload[5] == 0x10)
				&& (configurationTokenPayload[6] == 0x0E)) {

			byte[] networkIndex = new byte[1];
			byte[] ssid = new byte[8];
			byte[] authType = new byte[2];
			byte[] encrypType = new byte[2];
			byte[] networkKey = new byte[14];
			byte[] macAddress = new byte[6];
			byte[] vendorExtension = new byte[configurationTokenPayload.length - 66];

			if ((configurationTokenPayload[9] == 0x10)
					&& (configurationTokenPayload[10] == 0x26)) {
				if (configurationTokenPayload[12] == 0x01)
					System.arraycopy(configurationTokenPayload, 13,
							networkIndex, 0, 1);				
			} else {
				Log.d(TAG, "Fail to parse a Config Token, return Null");
				return null;
			}

			if ((configurationTokenPayload[14] == 0x10)
					&& (configurationTokenPayload[15] == 0x45)) {
				if (configurationTokenPayload[17] == 0x08)
					System.arraycopy(configurationTokenPayload, 18, ssid, 0, 8);				
			} else {
				Log.d(TAG, "Fail to parse a Config Token, return Null");
				return null;
			}

			if ((configurationTokenPayload[26] == 0x10)
					&& (configurationTokenPayload[27] == 0x03)) {
				if (configurationTokenPayload[29] == 0x02)
					System.arraycopy(configurationTokenPayload, 30, authType,
							0, 2);				
			} else {
				Log.d(TAG, "Fail to parse a Config Token, return Null");
				return null;
			}

			if ((configurationTokenPayload[32] == 0x10)
					&& (configurationTokenPayload[33] == 0x0F)) {
				if (configurationTokenPayload[35] == 0x02)
					System.arraycopy(configurationTokenPayload, 36, encrypType,
							0, 2);				
			} else {
				Log.d(TAG, "Fail to parse a Config Token, return Null");
				return null;
			}

			if ((configurationTokenPayload[38] == 0x10)
					&& (configurationTokenPayload[39] == 0x27)) {
				if (configurationTokenPayload[41] == 0x0E)
					System.arraycopy(configurationTokenPayload, 42, networkKey,
							0, 14);				
			} else {
				Log.d(TAG, "Fail to parse a Config Token, return Null");
				return null;
			}

			if ((configurationTokenPayload[56] == 0x10)
					&& (configurationTokenPayload[57] == 0x20)) {
				if (configurationTokenPayload[59] == 0x06)
					System.arraycopy(configurationTokenPayload, 60, macAddress,
							0, 6);				
			} else {
				Log.d(TAG, "Fail to parse a Config Token, return Null");
				return null;
			}
			
			if ((configurationTokenPayload[66] == 0x10)
					&& (configurationTokenPayload[67] == 0x49)) {				
					System.arraycopy(configurationTokenPayload, 66, vendorExtension,
							0, configurationTokenPayload.length - 66);				
			} else {
				Log.d(TAG, "Fail to parse a Config Token, return Null");
				return null;
			}

			try {
				Log.d(TAG, "1.parse: networkIndex = "
						+ getHexString(networkIndex));
				Log.d(TAG, "2.parse: ssid = "
						+ getHexString(ssid));
				Log.d(TAG, "3.parse: authType = "
						+ getHexString(authType));
				Log.d(TAG, "4.parse: encrypType = "
						+ getHexString(encrypType));
				Log.d(TAG, "5.parse: networkKey = "
						+ getHexString(networkKey));
				Log.d(TAG, "6.parse: macAddress = "
						+ getHexString(macAddress));
				Log.d(TAG, "7.parse: vendorExtension = "
						+ getHexString(vendorExtension));

			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			CfgToken.setNetworkIndex(networkIndex);
			CfgToken.setSSID(ssid);
			CfgToken.setAuthType(authType);
			CfgToken.setEncrypType(encrypType);
			CfgToken.setNetworkKey(networkKey);
			CfgToken.setMacAddress(macAddress);
		}

		return CfgToken;
	}// end of parseCfgTokenNdefPayloadToConfigurationToken

	public byte[] intToByteArray(int value) {
		byte[] b = new byte[2];
		for (int i = 0; i < 2; i++) {
			int offset = (b.length - 1 - i) * 8;
			b[i] = (byte) ((value >>> offset) & 0xFF);
		}
		return b;
	}// end of intToByteArray

	public int byteArrayToInt(byte[] pwdId) {
		return ((int) pwdId[0] << 8) | ((int) pwdId[1]);
	}// end of byteArrayToInt

	public String getHexString(byte[] raw) throws UnsupportedEncodingException {
		final byte[] HEX_CHAR_TABLE = { (byte) '0', (byte) '1', (byte) '2',
				(byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7',
				(byte) '8', (byte) '9', (byte) 'a', (byte) 'b', (byte) 'c',
				(byte) 'd', (byte) 'e', (byte) 'f' };
		byte[] hex = new byte[2 * raw.length];
		int index = 0;

		for (byte b : raw) {
			int v = b & 0xFF;
			hex[index++] = HEX_CHAR_TABLE[v >>> 4];
			hex[index++] = HEX_CHAR_TABLE[v & 0xF];
		}
		return new String(hex, "ASCII");
	}// end of getHexString

}// end of NfcForegrounddispatchActivity

