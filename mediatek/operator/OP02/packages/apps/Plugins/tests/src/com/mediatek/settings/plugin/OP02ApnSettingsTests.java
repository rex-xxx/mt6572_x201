/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */
package com.mediatek.settings.plugin;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.jayway.android.robotium.solo.Solo;
import com.mediatek.pluginmanager.PluginManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class OP02ApnSettingsTests extends OP02PluginBase {
	
    private Instrumentation mInst;
    private Context mContext;
    private PreferenceActivity mActivity;
    PreferenceGroup mApnList;
    private Preference mPrf;
    private List<SIMInfo> mSimList = null;
    private static final String TAG =  "OP02ApnSettingsTests";
    private final String ANDROID_IME = "com.android.inputmethod.latin/.LatinIME" ;
    private Solo mSolo;
	
	public static final int SIM_CARD_1 = PhoneConstants.GEMINI_SIM_1;
    public static final int SIM_CARD_2 = PhoneConstants.GEMINI_SIM_2;
    public static final int SIM_CARD_SINGLE = 2;

	public static final int SIM_CARD_TYPE_CU = 1;
	public static final int SIM_CARD_TYPE_CMCC = 1;
	public static final int SIM_CARD_TYPE_OTHER = 1; 
	private ISettingsMiscExt mSettingMiscExt;
	
    private static Class<?> launcherActivityClass;
    private static final String PACKAGE_ID_STRING = "com.android.settings";
    private static final String ACTIVITY_FULL_CLASSNAME = "com.android.settings.ApnSettings";
   
    static {
        try {
            launcherActivityClass = Class.forName(ACTIVITY_FULL_CLASSNAME);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        } 
    }
	
    // Constructor
	public OP02ApnSettingsTests() {
		super(PACKAGE_ID_STRING, launcherActivityClass);
	}
  
    // Sets up the test environment before each test.
    @Override
    protected void setUp() throws Exception {	
    	super.setUp();
        mInst = getInstrumentation();
    	mContext = mInst.getTargetContext();
		
        Intent i = new Intent();
        
    	mSimList = SIMInfo.getInsertedSIMList(mContext);
    	Log.i(TAG, "mSimList size : "+mSimList.size());
    	if(mSimList == null || mSimList.size() == 0) {
    		return;
    	}

    	SIMInfo simInfo = mSimList.get(0);    	   	
    	int simSlot = simInfo.mSlot;
        Log.i(TAG, "Slot = "+simSlot); 
        
        i.putExtra("simId", simSlot);
            
        setActivityIntent(i);
        setActivityInitialTouchMode(false);
		
    	mActivity = (PreferenceActivity)getActivity(); 
        mSolo = new Solo(mInst, mActivity); 	
        mApnList = (PreferenceGroup)mActivity.findPreference("apn_list");
        mSettingMiscExt = (ISettingsMiscExt)PluginManager.createPluginObject(mContext, ISettingsMiscExt.class.getName());

    }

	public void testCase01ResetApnType() {
    	mSimList = SIMInfo.getInsertedSIMList(mContext);
    	Log.i(TAG, "mSimList size : "+mSimList.size());
    	if (mSimList == null || mSimList.size() == 0) {
    		return;
    	}
		int selIndex;
		int count = mApnList.getPreferenceCount();
		Log.i(TAG, "APN List count : " + count);
		if (count > 0) {
			mSolo.clickOnRadioButton(1);
			mSolo.sleep(1000);
			mInst.invokeMenuActionSync(mActivity,2,0);
			for (selIndex = 0; selIndex < count; selIndex++) {
				if(mSolo.isRadioButtonChecked(selIndex)) {
					break;
				}
			}
			String selApnName = mApnList.getPreference(selIndex).getSummary().toString();
			
			Log.i(TAG, "Select APN Name : " + selApnName);
			if (SIM_CARD_TYPE_CMCC == getSimCardType((int)mSimList.get(0).mSimId)) {
				assert(selApnName.equals("cmnet"));
			}
			else if (SIM_CARD_TYPE_CU == getSimCardType((int)mSimList.get(0).mSimId)) {
				assert(selApnName.equals("3gnet"));
			}
		}
		
        mInst.waitForIdleSync();
	}
    
    public void testCase02MenuNewSave() {
    	mSimList = SIMInfo.getInsertedSIMList(mContext);
    	Log.i(TAG, "mSimList size : " + mSimList.size());
    	if(mSimList == null || mSimList.size() == 0) {
    		return;
    	}

        int count = mApnList.getPreferenceCount();
        launchNewApn();
        mSolo.sleep(500);
        //mInst.invokeMenuActionSync(mActivity,Menu.FIRST+1,0); 
		sendKeys(KeyEvent.KEYCODE_MENU);
		sendKeys(KeyEvent.KEYCODE_DPAD_UP);
		sendKeys(KeyEvent.KEYCODE_DPAD_UP);
		sendKeys(KeyEvent.KEYCODE_ENTER);

        mSolo.sleep(500);
        mApnList = (PreferenceGroup) mActivity.findPreference("apn_list");
        int newCount = mApnList.getPreferenceCount();
        assertEquals(count+1 , newCount);
        mInst.waitForIdleSync();
    }

    public void testCase03OmacpApnSender() {
	
		mSimList = SIMInfo.getInsertedSIMList(mContext);
		Log.i(TAG, "mSimList size : " + mSimList.size());
		if(mSimList == null || mSimList.size() == 0) {
			return;
		}
        Intent it = new Intent();
        it.setAction("com.mediatek.omacp.settings.result");
        it.putExtra("apnId", "ap004");
        it.putExtra("result", true);
        mContext.sendBroadcast(it);
		//boolean result = mSettingMiscExt.getResult();
		//assert(result);
    }
	private boolean isEditPreference(int index) {
		mPrf = mApnList.getPreference(index);
		String smy = mPrf.getSummary().toString();
		Log.i(TAG, "mPrf(): " + mPrf.getSummary());
		Boolean res = true;
		try {
			Class pc = Class.forName("com.android.settings.ApnPreference");
			Field field = pc.getDeclaredField("mEditable");
			field.setAccessible(true);
			res = (Boolean) field.get(mPrf);
			Log.i(TAG, "res: " + res);
		} catch (Exception e) {
			// ignore
		}
		return res;
	}

	private int getSimCardType(int simID) {
		String numeric = null;
		switch(simID)
		{
            case SIM_CARD_1:
            case SIM_CARD_SINGLE:
                numeric = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC, "-1");
                break;

            case SIM_CARD_2:
                numeric = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC_2, "-1");
                break; 
			default:
				break;
		}
		if(numeric.equals("46001")) {
			return SIM_CARD_TYPE_CU;
		}
		else if(numeric.equals("46000") || numeric.equals("46002")) {
			return SIM_CARD_TYPE_CMCC;
		}
		else {
			return SIM_CARD_TYPE_OTHER;
		}
	}
	
    
    private void launchNewApn() {
        Settings.Secure.putString(mInst.getTargetContext().getContentResolver(),
                Settings.Secure.DEFAULT_INPUT_METHOD,
                ANDROID_IME);
        mInst.invokeMenuActionSync(mActivity,Menu.FIRST,0);
		
		mSolo.clickInList(1);
        mSolo.sleep(500);
        mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_A);
        mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_C);
        mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
        mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);
        mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER);        
        //Set apn to 'c'
        
        mSolo.sleep(500);
		mSolo.clickInList(2);
        mSolo.sleep(500);
        mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_C);
        mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
        mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);
        mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER);        
    }  
   /*
     * (non-Javadoc)
     * @see android.test.ActivityInstrumentationTestCase2#tearDown()
     */   
    @Override
    public void tearDown() throws Exception {
    	super.tearDown();
    	if(mActivity != null) {
    		mActivity.finish();
    	}
    }

   
}
