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

/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//For Operator Custom
//MTK_OP03_PROTECT_START
package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.telephony.TelephonyManager;
import android.view.Menu;

import com.android.internal.telephony.PhoneConstants;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;

public class TetherApnSettings extends ApnSettings implements
        Preference.OnPreferenceChangeListener {
    static final String TAG = "TetherApnSettings";

    private static final Uri PREFER_APN_TETHER_URI = Uri.parse("content://telephony/carriers/prefertetheringapn");

    private boolean mIsSwitching = false;
    private boolean mIsSIMReady = true;
    private boolean mIsTetehred = false;
    private ConnectivityManager mConnManager;
    private String[] mUsbRegexs;

    private final BroadcastReceiver mTetheringStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        
            String action = intent.getAction(); 
            if (action.equals(ConnectivityManager.TETHER_CHANGED_DONE_ACTION)) {
                Xlog.d(TAG, "onReceive:ConnectivityManager.TETHER_CHANGED_DONE_ACTION");
                mIsSwitching = false;
                getPreferenceScreen().setEnabled(getScreenEnableState());                
            } else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                Xlog.d(TAG, "onReceive:AIRPLANE_MODE state changed: " + mAirplaneModeEnabled);
                mAirplaneModeEnabled = intent.getBooleanExtra("state", false);
                getPreferenceScreen().setEnabled(getScreenEnableState());
            } else if (action.equals(ConnectivityManager.ACTION_TETHER_STATE_CHANGED)) {
                Xlog.d(TAG, "onReceive: ConnectivityManager.ACTION_TETHER_STATE_CHANGED");
                ArrayList<String> active = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_ACTIVE_TETHER);
        if (active != null) {
                updateTetheredState(active.toArray());  
        } else {
           Xlog.d(TAG, "active tether is null , not update tether state.");
        } 
            }
        }
    };
    

    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mConnManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
 
        if (mConnManager != null) {
            mUsbRegexs = mConnManager.getTetherableUsbRegexs();
        }
        TelephonyManager telManager = TelephonyManager.getDefault();
        
        if (telManager != null) {
            mIsSIMReady = TelephonyManager.SIM_STATE_READY == telManager.getSimState();
        }

        mIsTetherApn = true;
        mRestoreCarrierUri = PREFER_APN_TETHER_URI;
    }   
    
    @Override
    protected void onResume() {
        super.onResume();
        Xlog.d(TAG, "onResume , mIsSwitching = " + mIsSwitching);
        if (mConnManager != null) {
            mIsSwitching = !mConnManager.isTetheringChangeDone();
            String[] tethered = mConnManager.getTetheredIfaces(); 
            updateTetheredState(tethered);
        }
    }

    @Override
    protected IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);       
        filter.addAction(ConnectivityManager.TETHER_CHANGED_DONE_ACTION); 
        filter.addAction(ConnectivityManager.ACTION_TETHER_STATE_CHANGED); 
        return filter;
    }
    
    @Override
    protected BroadcastReceiver  getBroadcastReceiver() {
        return mTetheringStateReceiver;
    }    

    @Override
    protected String getFillListQuery() {
        return "numeric=\"" + mNumeric + "\" AND type=\"" + ApnSettings.TETHER_TYPE + "\"";
    }

    @Override
    protected boolean getScreenEnableState() {
        mIsCallStateIdle = mTelephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE;  
        Xlog.w(TAG,"mIsCallStateIdle : " + mIsCallStateIdle + " mAirplaneModeEnabled : " + 
            mAirplaneModeEnabled + " mIsSIMReady :" + mIsSIMReady + " mIsSwitching: " + 
            mIsSwitching + " mIsTetehred: " + mIsTetehred);
        return !mIsTetehred && mIsCallStateIdle && !mAirplaneModeEnabled && mIsSIMReady && !mIsSwitching;
    }
    
    @Override
    protected void addMenu(Menu menu) {
        /*the 20801 23430 23431 23432 is sim card for orange support*/
        if ("20801".equals(mNumeric) || "23430".equals(mNumeric)
                || "23431".equals(mNumeric) || "23432".equals(mNumeric)) {
                return;
        }

        menu.add(0, MENU_NEW, 0,
            getResources().getString(R.string.menu_new)).setIcon(android.R.drawable.ic_menu_add);
    }
    
    @Override
    protected void setSelectedApnKey(String key) {
        mSelectedKey = key;
        ContentResolver resolver = getContentResolver();
        resolver.delete(PREFER_APN_TETHER_URI, null, null);
        ContentValues values = new ContentValues();
        values.put(APN_ID, mSelectedKey);
        resolver.insert(PREFER_APN_TETHER_URI, values);
        
    }

    @Override
    protected void addNewApn() {
        Intent it = new Intent(Intent.ACTION_INSERT, mUri);
        it.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, mSimId);
        it.putExtra(ApnSettings.APN_TYPE, ApnSettings.TETHER_TYPE);
        startActivity(it);
    }  
   
   private void updateTetheredState(Object[] tethered) {
       
        mIsTetehred = false;
        for (Object o : tethered) {
               String s = (String)o;
               for (String regex : mUsbRegexs) {
                   if (s.matches(regex)) {
                        mIsTetehred = true;
                   }
               }
        }

        getPreferenceScreen().setEnabled(getScreenEnableState());
   } 
}
//MTK_OP03_PROTECT_END

