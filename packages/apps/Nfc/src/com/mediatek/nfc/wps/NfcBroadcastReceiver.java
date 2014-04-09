package com.mediatek.nfc.wps;

import java.util.ArrayList;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.sax.StartElementListener;
import android.util.Log;
import android.view.textservice.SentenceSuggestionsInfo;

import android.nfc.wps.INfcWpsTestBed;
import android.nfc.wps.ConfigurationToken;
import android.nfc.wps.PasswordToken;

import com.mediatek.nfc.handover.Util;

public class NfcBroadcastReceiver extends BroadcastReceiver {
    static final String TAG = "NfcBroadcastReceiver";
    static final boolean DBG = true;






    

	private static Context mContext;
	private static IntentFilter mIntentFilter = new IntentFilter();
	
	//public static int nowCommand = 0;

	public NfcBroadcastReceiver(Context t) {
    	Log.d(TAG, "NfcBroadcastReceiver()...");	
		mContext = t;		
	}
	
	public void init() {
		
		Log.d(TAG, "init() is called...");					
		mIntentFilter.addAction(INfcWpsTestBed.MTK_WPS_NFC_TESTBED_R_CONFIGURATION_ACTION);
		mIntentFilter.addAction(INfcWpsTestBed.MTK_WPS_NFC_TESTBED_W_PASSWORD_ACTION);
		mIntentFilter.addAction(INfcWpsTestBed.MTK_WPS_NFC_TESTBED_R_PASSWORD_ACTION);
		mIntentFilter.addAction(INfcWpsTestBed.MTK_WPS_NFC_TESTBED_W_CONFIGURATION_ACTION);		
		mContext.registerReceiver(this, mIntentFilter);				
        //Handover case: Requester , Selector move to MtkHandoverManager
        
	}
	
	public void deInit() {
		Log.d(TAG, "deInit() is called...");		
		mContext.unregisterReceiver(this);
	}


	@Override
	public void onReceive(Context context, Intent intent) {
		// cannot not switch(String) without jre 1.7
		Log.d(TAG, " onReceive() An intent is Incoming... " + intent.getAction());		

		// [1] RC
		if (intent.getAction().equals(
				INfcWpsTestBed.MTK_WPS_NFC_TESTBED_R_CONFIGURATION_ACTION)) {

			// start Nfc Foreground Dispatch activity with command [1]
			Intent intentReadCfgToken = new Intent(context,
					NfcForegroundDispatchActivity.class);
			intentReadCfgToken.putExtra(INfcWpsAppInternal.EXTRA_NFC_WPS_CMD, INfcWpsAppInternal.READ_CONFIGURATION_TOKEN_CMD);
			intentReadCfgToken.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intentReadCfgToken);
		}
		
		// [2] WP
		else if (intent.getAction().equals(
				INfcWpsTestBed.MTK_WPS_NFC_TESTBED_W_PASSWORD_ACTION)) {

			// get password for EM_SendBroadcastActivity &
			Parcelable parcelablePswToken = (Parcelable) intent
					.getParcelableExtra(INfcWpsTestBed.MTK_WPS_NFC_TESTBED_EXTRA_PASSWORD);
			PasswordToken pwdToken = (PasswordToken) parcelablePswToken;

            if(pwdToken==null)
                Log.e(TAG, "pwdToken==null.");    

            if (pwdToken.getPublicKeyHash() == null) {
                Log.d(TAG, "wtf!? public key hash is null!?");
            }

            Log.d(TAG, "pwdToken.getPwdId " + pwdToken.getPwdId());
            Log.d(TAG, "pwdToken.getPublicKeyHash " + new String(pwdToken.getPublicKeyHash()));
            Log.d(TAG, "pwdToken.getPublicKeyHash " + Util.bytesToString(pwdToken.getPublicKeyHash()));
            Log.d(TAG, "pwdToken.getDevPwd " + Util.bytesToString(pwdToken.getDevPwd()));
            Log.d(TAG, "pwdToken.getVendorEx " + Util.bytesToString(pwdToken.getVendorEx()));


			// start Nfc Foreground Dispatch activity with command [2]
			Intent intentWritePwdToken = new Intent(context,
					NfcForegroundDispatchActivity.class);
			intentWritePwdToken.putExtra(INfcWpsAppInternal.EXTRA_NFC_WPS_CMD, INfcWpsAppInternal.WRITE_PASSWORD_TOKEN_CMD);
			intentWritePwdToken.putExtra(INfcWpsAppInternal.EXTRA_NFC_WPS_PWD_TOKEN, pwdToken);			
			intentWritePwdToken.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intentWritePwdToken);
			
		}

		// [3] RP
		else if (intent.getAction().equals(
				INfcWpsTestBed.MTK_WPS_NFC_TESTBED_R_PASSWORD_ACTION)) {

			// start Nfc Foreground Dispatch activity with command [3]
			Intent intentReadPwdToken = new Intent(context,
					NfcForegroundDispatchActivity.class);
			intentReadPwdToken.putExtra(INfcWpsAppInternal.EXTRA_NFC_WPS_CMD, INfcWpsAppInternal.READ_PASSWORD_TOKEN_CMD);
			intentReadPwdToken.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intentReadPwdToken);
		}
			
		//[4] WC
			else if (intent.getAction().equals(
					INfcWpsTestBed.MTK_WPS_NFC_TESTBED_W_CONFIGURATION_ACTION)) {

				// get configuration for EM_SendBroadcastActivity &
				Parcelable parcelableCfgToken = (Parcelable) intent
						.getParcelableExtra(INfcWpsTestBed.MTK_WPS_NFC_TESTBED_EXTRA_CONFIGURATION);
				ConfigurationToken cfgToken = (ConfigurationToken) parcelableCfgToken;

				// start Nfc Foreground Dispatch activity with command [4]
				Intent intentWriteCfgToken = new Intent(context,
						NfcForegroundDispatchActivity.class);
				intentWriteCfgToken.putExtra(INfcWpsAppInternal.EXTRA_NFC_WPS_CMD, INfcWpsAppInternal.WRITE_CONFIGURATION_TOKEN_CMD);
				intentWriteCfgToken.putExtra(INfcWpsAppInternal.EXTRA_NFC_WPS_CONFIGURATION_TOKEN, cfgToken);			
				intentWriteCfgToken.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(intentWriteCfgToken);							

		} else
			Log.d(TAG, "On Receive an unknow intent= =...");

	}// end of onreceive
}// end of FW_ReceiveBroadcast_and_Handle
