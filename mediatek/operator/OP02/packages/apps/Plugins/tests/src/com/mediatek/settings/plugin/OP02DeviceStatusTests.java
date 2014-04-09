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
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Telephony.SIMInfo;
import android.test.InstrumentationTestCase;
import android.view.KeyEvent;
import android.view.Menu;

import com.mediatek.settings.ext.IDeviceInfoSettingsExt;
import com.mediatek.settings.ext.IStatusGeminiExt;
import com.mediatek.pluginmanager.PluginManager;
import com.mediatek.xlog.Xlog;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class OP02DeviceStatusTests extends InstrumentationTestCase {
    // The intsrumenation
    private Instrumentation mInst;
    private Context mContext;
    private static final String TAG =  "OP02DeviceStatusTests";
    private IStatusGeminiExt mStatusExt;
    private IDeviceInfoSettingsExt mDeviceInfoExt;
	

  
    // Sets up the test environment before each test.
    @Override
    protected void setUp() throws Exception {      
        super.setUp();
        mInst = getInstrumentation();
        mContext = mInst.getTargetContext();
        mStatusExt = (IStatusGeminiExt)PluginManager.createPluginObject(mContext, IStatusGeminiExt.class.getName());
		mDeviceInfoExt = (IDeviceInfoSettingsExt)PluginManager.createPluginObject(mContext, IDeviceInfoSettingsExt.class.getName());
	}

    public void testCase01EntryDeviceStatusScreen() { 
            Xlog.i(TAG, "testCase01_EntryDeviceStatusScreen");

            assertNotNull(mInst);
            assertNotNull(mContext);
            try {
				Context mmsCtx = mContext.createPackageContext("com.android.settings",
					Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
                Class<?> iclass = Class.forName("com.android.settings.deviceinfo.StatusGemini",
					true, mmsCtx.getClassLoader());
				
                Object iobject = iclass.newInstance();
				Field tag = iclass.getDeclaredField("TAG");
				tag.setAccessible(true);
				String itag = (String)tag.get(iobject);
				
				Xlog.i(TAG, "tag = " + itag);
				
				PreferenceActivity prefActivity = (PreferenceActivity)iobject;
				
				Preference p = new Preference(mContext);
				p.setKey("sim_status");
				p.setSummary("sim_status");
				//PreferenceScreen pScreen = new PreferenceScreen(mContext, null);
				PreferenceScreen pScreen = prefActivity.getPreferenceScreen();
				//pScreen.addPreference(p);

                //assertNotNull(p);
                //assertNotNull(pScreen); 
                
                //mStatusExt.initUI(pScreen, p);
                //Preference pAfter = (Preference)findPref.invoke(iobject, "sim_status");
                Preference pAfter = prefActivity.findPreference("sim_status");
                assertNull(pAfter);
                
               } catch (NoSuchFieldException e) {
                    Xlog.i(TAG, "catch NoSuchMethodException");
					throw new RuntimeException(e);
               } catch (IllegalAccessException e) {
                    Xlog.i(TAG, "catch IllegalAccessException"); 
					throw new RuntimeException(e);
               } catch (ClassNotFoundException e) {
                    Xlog.i(TAG, "catch ClassNotFoundException"); 
					throw new RuntimeException(e);
               } catch (InstantiationException e) {
                    Xlog.i(TAG, "catch InstantiationException"); 
					throw new RuntimeException(e);
               } catch (NameNotFoundException e) {
                    Xlog.i(TAG, "catch InvocationTargetException"); 
					throw new RuntimeException(e);
               }
        }
	
    public void testCase02SetDeviceInfoSummary() {
		
		Xlog.i(TAG, "testCase02SetDeviceInfoSummary");
		
		Preference p = new Preference(mContext);
		p.setSummary("CU_Plugin");
		mDeviceInfoExt.initSummary(p);
		assert(p.getSummary().equals(""));
		
	}
	
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }
}
