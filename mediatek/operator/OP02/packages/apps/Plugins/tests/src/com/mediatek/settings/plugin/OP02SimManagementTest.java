/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2011. All rights reserved.
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

import android.app.Dialog;
import android.app.Instrumentation;
import android.content.ContentResolver;
import android.content.Context;
import android.inputmethodservice.Keyboard.Key;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.jayway.android.robotium.solo.Solo;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.pluginmanager.PluginManager;
import com.mediatek.xlog.Xlog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class OP02SimManagementTest extends OP02PluginBase {
	
	private Solo mSolo;
	private final int SLEEP_TIME=500;
	private final int WAITING_TIME=35000;
	private Context mContext;
	private Instrumentation mIns;
	private PreferenceActivity mActivity;
	private ContentResolver mConResolver;
	private final String TAG="OP02SimManagementTest";
	private Map<Long,SIMInfo> mSimMap;
	private int mSimNum=0;
	private static final int NO_SIM_CARD=0;
	private static final int SINGLE_CARD=1;
	private static final int TWO_SIM_CARD=2;
	private final static String BASEBAND_INFO="gsm.baseband.capability";
	private final static int SLOT1=0;
	private final static int SLOT2=1;
	private ISettingsMiscExt mSettingMiscExt;
	
	private static Class<?> launcherActivityClass;
    private static final String PACKAGE_ID_STRING = "com.android.settings";
    private static final String ACTIVITY_FULL_CLASSNAME = "com.android.settings.Settings";
   
    static {
        try {
            launcherActivityClass = Class.forName(ACTIVITY_FULL_CLASSNAME);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        } 
    }
	
    // Constructor
	public OP02SimManagementTest() {
		super(PACKAGE_ID_STRING, launcherActivityClass);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setActivityInitialTouchMode(false);
		mIns = getInstrumentation();
        mContext = mIns.getTargetContext();
        mConResolver = mContext.getContentResolver();
        mActivity = (PreferenceActivity)getActivity();
        mSolo = new Solo(mIns, mActivity);
        mSettingMiscExt = (ISettingsMiscExt)PluginManager.createPluginObject(mContext, ISettingsMiscExt.class.getName());
	}
	public void testcase01ActivitySimManagement() {
		
		Xlog.d(TAG,"***testcase01ActivitySimManagement***");
		assertTrue(mIns != null);
		assertTrue(mActivity != null);
		assertTrue(mContext != null);
		assertTrue(mConResolver != null);
		assertTrue(mSolo != null);
	}
	
	public void testcase02VideoCall() {
		
		Xlog.d(TAG,"***testcase02VoiceCall***");
		
        if(mSolo.searchText("SIM management")) {
			
            mSolo.clickOnText("SIM management");
			
			loadSimInfo();
			if(mSimNum == NO_SIM_CARD || !isVideoCallSupport()) {
				return;
			}
			if (!FeatureOption.MTK_GEMINI_3G_SWITCH && (mSimNum == SINGLE_CARD)) {
				if (mSimMap.get(Long.valueOf(PhoneConstants.GEMINI_SIM_1)) == null) {
					return;
				} else {
					long origID = Settings.System.getLong(mConResolver,Settings.System.VIDEO_CALL_SIM_SETTING,Settings.System.DEFAULT_SIM_NOT_SET);
					long newID = 0;
					long simid = 0;
					mSolo.clickOnText("Video call");
					mSolo.sleep(SLEEP_TIME);
					mSolo.clickInList(0);
					mSolo.sleep(SLEEP_TIME);
					newID= Settings.System.getLong(mConResolver, Settings.System.VIDEO_CALL_SIM_SETTING,Settings.System.DEFAULT_SIM_NOT_SET);
					simid=mSimMap.get(Long.valueOf(PhoneConstants.GEMINI_SIM_1)).mSimId;
					Xlog.d(TAG,"newID=" + newID + " simid=" + simid);
					
					assertTrue(newID == simid);
					Settings.System.putLong(mConResolver, Settings.System.VIDEO_CALL_SIM_SETTING, origID);
				}
			}
        }
		
	}
	public void testcase03SetScreenTimeOut() {			
		Xlog.d(TAG,"***testcase03SetScreenTimeOut***");
		
        if(mSolo.searchText("Display")) {
            mSolo.clickOnText("Display");
			assert(mSolo.searchText("Screen timeout"));
			mSolo.clickOnText("Screen timeout");
			
			ListPreference screenTimeoutPreference = new ListPreference(mContext);
			screenTimeoutPreference.setDialogTitle("Sleep");
			mSettingMiscExt.setTimeoutPrefTitle(screenTimeoutPreference);
			
			String prefTitle = screenTimeoutPreference.getDialogTitle().toString();
			assert(prefTitle.equals("Screen timeout"));
        }
	}
	private void loadSimInfo() {
		List<SIMInfo> simList = SIMInfo.getInsertedSIMList(getActivity());
		mSimNum = simList.size();
		mSimMap = new HashMap<Long,SIMInfo>();
		Xlog.d(TAG,"mSimNum="+mSimNum);
		for (SIMInfo siminfo:simList) {
			Xlog.d(TAG,"siminfo.mSlot=" + siminfo.mSlot);
    		mSimMap.put(Long.valueOf(siminfo.mSlot), siminfo);
    	}
	}

	private boolean hasSimCard() {
		boolean isSimCardInserted = false;
		isSimCardInserted=(android.provider.Telephony.SIMInfo.getInsertedSIMCount(mContext) != 0);
		Xlog.d(TAG,"isSimCardInserted="+isSimCardInserted);
		return isSimCardInserted;
	}
	private boolean isVideoCallSupport() {
		boolean m3gSupport = false;
		String baseband = SystemProperties.get(BASEBAND_INFO);
        Xlog.i(TAG, "baseband is "+baseband);
        if((baseband != null)&&(baseband.length() != 0) && (Integer.parseInt(baseband) > 3)) {
        	m3gSupport = true;
        }
        if((!m3gSupport) || (!FeatureOption.MTK_VT3G324M_SUPPORT)) {
        	return false;
        }
        return true;
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		try {
        	mSolo.finishOpenedActivities();
        } catch (Exception e) {
        	//ignore
        }
	}
}
