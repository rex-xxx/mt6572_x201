/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.mediatek.oobe.basic.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.widget.Toast;

import com.mediatek.oobe.ext.IWifiExt;
import com.mediatek.oobe.R;
import com.mediatek.oobe.utils.OOBEConstants;
import com.mediatek.oobe.utils.Utils;
import com.mediatek.xlog.Xlog;

import java.util.concurrent.atomic.AtomicBoolean;

public class WifiEnabler {
    private static final String TAG = OOBEConstants.TAG;

    private final Context mContext;
    private SwitchPreference mSwitchPre;
    private AtomicBoolean mConnected = new AtomicBoolean(false);
    private static final String WIFI_SWITCH_ENABLER = "wifi_switch";

    private final WifiManager mWifiManager;
    private boolean mStateMachineEvent;
    private final IntentFilter mIntentFilter;
    /// M: plug in
    IWifiExt mExt;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                handleWifiStateChanged(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN));
            } else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
                if (!mConnected.get()) {
                    handleStateChanged(WifiInfo.getDetailedStateOf((SupplicantState) intent
                            .getParcelableExtra(WifiManager.EXTRA_NEW_STATE)));
                }
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                mConnected.set(info.isConnected());
                handleStateChanged(info.getDetailedState());
            }
        }
    };

    /**
     * WifiEnabler constructor
     * @param context Context
     * @param switchPref Preference
     */
    public WifiEnabler(Context context, Preference switchPref) {
        mContext = context;
        mSwitchPre = (SwitchPreference) switchPref;

        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mIntentFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        // The order matters! We really should not depend on this. :(
        mIntentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        /// M: get plug in
        mExt = Utils.getWifiPlugin(mContext);
    }

    OnPreferenceChangeListener mPreferenceChangeListener = new OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            // TODO Auto-generated method stub
            if (preference.getKey().equals(WIFI_SWITCH_ENABLER)) {
                Xlog.d(TAG, "wifi enabler switch change:" + newValue.toString());

                boolean isChecked = (Boolean) newValue;
                // Do nothing if called as a result of a state machine event
                if (mStateMachineEvent) {
                    Xlog.d(TAG, "OOBE wifi mStateMachineEvent: is true ,return");
                    mStateMachineEvent = false;
                    return true;
                }
                // Show toast message if Wi-Fi is not allowed in airplane mode
                if (isChecked && !isRadioAllowed(mContext, Settings.System.RADIO_WIFI)) {
                    Toast.makeText(mContext, R.string.wifi_in_airplane_mode, Toast.LENGTH_SHORT).show();
                    // Reset switch to off. No infinite check/listenenr loop.
                    Xlog.d(TAG, "OOBE wifi isRadioAllowed:");

                    return true;
                }

                // Disable tethering if enabling Wifi
                int wifiApState = mWifiManager.getWifiApState();
                if (isChecked
                        && ((wifiApState == WifiManager.WIFI_AP_STATE_ENABLING) 
                                || (wifiApState == WifiManager.WIFI_AP_STATE_ENABLED))) {
                    Xlog.d(TAG, "OOBE wifi getWifiApState:");

                    mWifiManager.setWifiApEnabled(null, false);
                }

                if (mWifiManager.setWifiEnabled(isChecked)) {
                    // Intent has been taken into account, disable until new state is active
                    Xlog.d(TAG, "OOBE wifi setWifiEnabled:" + isChecked);
                    mSwitchPre.setEnabled(false);
                } else {
                    // Error
                    Toast.makeText(mContext, R.string.wifi_error, Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        }
    };

    /**
     * resume
     */
    public void resume() {
        int wifiState = mWifiManager.getWifiState();
        if(wifiState == WifiManager.WIFI_STATE_ENABLED) {
            mSwitchPre.setChecked(true);
        } else if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
            mSwitchPre.setChecked(false);
        }
        
        // Wi-Fi state is sticky, so just let the receiver update UI
        mContext.registerReceiver(mReceiver, mIntentFilter);
        mSwitchPre.setOnPreferenceChangeListener(mPreferenceChangeListener);
        /// M: register airplane mode observer, and init state of switch @{
        mExt.registerAirplaneModeObserver(mSwitchPre);
        mExt.initSwitchState(mSwitchPre);
        /// @}
    }

    /**
     * pause
     */
    public void pause() {
        mContext.unregisterReceiver(mReceiver);
        mSwitchPre.setOnPreferenceChangeListener(null);
        /// M: unregister airplane mode observer
        mExt.unRegisterAirplaneObserver();
    }

    private void handleWifiStateChanged(int state) {
        switch (state) {
            case WifiManager.WIFI_STATE_ENABLING:
                mSwitchPre.setEnabled(false);
                break;
            case WifiManager.WIFI_STATE_ENABLED:
                setSwitchChecked(true);
                mSwitchPre.setEnabled(mExt.getSwitchState());
                break;
            case WifiManager.WIFI_STATE_DISABLING:
                mSwitchPre.setEnabled(false);
                break;
            case WifiManager.WIFI_STATE_DISABLED:
                setSwitchChecked(false);
                mSwitchPre.setEnabled(mExt.getSwitchState());
                break;
            default:
                setSwitchChecked(false);
                mSwitchPre.setEnabled(mExt.getSwitchState());
                break;
        }
    }

    private void setSwitchChecked(boolean checked) {
        if (checked != mSwitchPre.isChecked()) {
            mStateMachineEvent = true;
            mSwitchPre.setChecked(checked);
        }
    }

    private void handleStateChanged(@SuppressWarnings("unused") NetworkInfo.DetailedState state) {
        // After the refactoring from a CheckBoxPreference to a Switch, this method is useless since
        // there is nowhere to display a summary.
        // This code is kept in case a future change re-introduces an associated text.
        /*
         * // WifiInfo is valid if and only if Wi-Fi is enabled. // Here we use the state of the switch as an optimization.
         * if (state != null && mSwitch.isChecked()) { WifiInfo info = mWifiManager.getConnectionInfo(); if (info != null) {
         * //setSummary(Summary.get(mContext, info.getSSID(), state)); } }
         */
    }

    /**
     * isAirplaneModeOn
     * @param context Context
     * @return boolean
     */
    public static boolean isAirplaneModeOn(Context context) {
        return Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) != 0;
    }

    /**
     * isRadioAllowed
     * @param context Context
     * @param type String
     * @return boolean
     */
    public static boolean isRadioAllowed(Context context, String type) {
        if (!isAirplaneModeOn(context)) {
            return true;
        }
        // Here we use the same logic in onCreate().
        String toggleable = Settings.System.getString(context.getContentResolver(),
                Settings.System.AIRPLANE_MODE_TOGGLEABLE_RADIOS);
        return toggleable != null && toggleable.contains(type);
    }
}
