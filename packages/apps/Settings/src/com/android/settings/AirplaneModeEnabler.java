/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneStateIntentReceiver;
import com.android.internal.telephony.TelephonyProperties;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.telephony.TelephonyManagerEx;
import com.mediatek.xlog.Xlog;

public class AirplaneModeEnabler implements
        Preference.OnPreferenceChangeListener {
    private static final String LOG_TAG = "AirplaneModeEnabler";
    private final Context mContext;

    private PhoneStateIntentReceiver mPhoneStateReceiver;
    
    private final CheckBoxPreference mCheckBoxPref;

    private static final int EVENT_SERVICE_STATE_CHANGED = 3;

    /// M:  @{
    private TelephonyManager mTelephonyManager;
    private TelephonyManagerEx mTelephonyManagerEx;
    private int mServiceState1 = ServiceState.STATE_POWER_OFF ;
    private int mServiceState2 = ServiceState.STATE_POWER_OFF ;
    ///M: add for gemini+
    private int mServiceState3 = ServiceState.STATE_POWER_OFF ;
    private int mServiceState4 = ServiceState.STATE_POWER_OFF ;
    /// @}

    private IntentFilter mIntentFilter;

    public AirplaneModeEnabler(Context context, CheckBoxPreference airplaneModeCheckBoxPreference) {
        
        mContext = context;
        mCheckBoxPref = airplaneModeCheckBoxPreference;
        /// M: get telephony manager
        mTelephonyManager = (TelephonyManager)context.getSystemService(
                Context.TELEPHONY_SERVICE);
        mTelephonyManagerEx = new TelephonyManagerEx(context);
        airplaneModeCheckBoxPreference.setPersistent(false);
    }

    public void resume() {
        /// M: @{
        mCheckBoxPref.setChecked(isAirplaneModeOn(mContext));
        // This is the widget enabled state, not the preference toggled state
        if (!Utils.isWifiOnly(mContext)) {
            mCheckBoxPref.setEnabled(true);
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                mTelephonyManagerEx.listen(mPhoneStateListener1,
                        PhoneStateListener.LISTEN_SERVICE_STATE,
                        PhoneConstants.GEMINI_SIM_1);
                mTelephonyManagerEx.listen(mPhoneStateListener2,
                        PhoneStateListener.LISTEN_SERVICE_STATE,
                        PhoneConstants.GEMINI_SIM_2);
                regListenForGeminiPlus(PhoneStateListener.LISTEN_SERVICE_STATE);
            } else {
                mTelephonyManager.listen(mPhoneStateListener1,
                        PhoneStateListener.LISTEN_SERVICE_STATE);
            }
        } else {
            mIntentFilter = new IntentFilter(
                Intent.ACTION_AIRPLANE_MODE_CHANGED);
            mIntentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            mContext.registerReceiver(mAirplaneModeReceiver, mIntentFilter);
        }
        mCheckBoxPref.setOnPreferenceChangeListener(this);
        /// @}
    }

    private BroadcastReceiver mAirplaneModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Xlog.i(LOG_TAG, "mAirplaneModeReceiver AIRPLANE_MODE_CHANGED...");
            boolean airplaneModeEnabled = isAirplaneModeOn(mContext);
            mCheckBoxPref.setChecked(airplaneModeEnabled);
            mCheckBoxPref.setEnabled(true);
        }
    };

    /// M: phone state listner @{
    PhoneStateListener mPhoneStateListener1 = new PhoneStateListener() {
        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            Xlog.i(LOG_TAG, "PhoneStateListener1.onServiceStateChanged: serviceState=" + serviceState);
            mServiceState1 = serviceState.getState();
            onAirplaneModeChanged();
        }            

    };

    PhoneStateListener mPhoneStateListener2 = new PhoneStateListener() {
        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            Xlog.i(LOG_TAG, "PhoneStateListener2.onServiceStateChanged: serviceState=" + serviceState);
            mServiceState2 = serviceState.getState();
            onAirplaneModeChanged();
        }                
    };
    ///M: add for GEMINI+
    PhoneStateListener mPhoneStateListener3 = new PhoneStateListener() {
        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            Xlog.i(LOG_TAG, "PhoneStateListener3.onServiceStateChanged: serviceState=" + serviceState);
            mServiceState3 = serviceState.getState();
            onAirplaneModeChanged();
        }                
    };
    PhoneStateListener mPhoneStateListener4 = new PhoneStateListener() {
        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            Xlog.i(LOG_TAG, "PhoneStateListener4.onServiceStateChanged: serviceState=" + serviceState);
            mServiceState4 = serviceState.getState();
            onAirplaneModeChanged();
        }                
    }; 
    /// @}
    
    public void pause() {
        /// M: @{
        mCheckBoxPref.setOnPreferenceChangeListener(null);
        if (!Utils.isWifiOnly(mContext)) {
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                mTelephonyManagerEx.listen(mPhoneStateListener1,
                        PhoneStateListener.LISTEN_NONE, PhoneConstants.GEMINI_SIM_1);
                mTelephonyManagerEx.listen(mPhoneStateListener2,
                        PhoneStateListener.LISTEN_NONE, PhoneConstants.GEMINI_SIM_2);
                regListenForGeminiPlus(PhoneStateListener.LISTEN_NONE);
            } else {
                mTelephonyManager.listen(mPhoneStateListener1,
                        PhoneStateListener.LISTEN_NONE);
            }
        } else {
            mContext.unregisterReceiver(mAirplaneModeReceiver);
        }
        /// @}
    }

    public static boolean isAirplaneModeOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    private void setAirplaneModeOn(boolean enabling) {
        // Change the system setting
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 
                                enabling ? 1 : 0);
        // Update the UI to reflect system setting
        // / M: @{
        if (!Utils.isWifiOnly(mContext)) {
            mCheckBoxPref.setEnabled(false);
        }
        /// @}
        mCheckBoxPref.setChecked(enabling);
        
        // Post the intent
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", enabling);
        mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    /**
     * Called when we've received confirmation that the airplane mode was set.
     * TODO: We update the checkbox summary when we get notified
     * that mobile radio is powered up/down. We should not have dependency
     * on one radio alone. We need to do the following:
     * - handle the case of wifi/bluetooth failures
     * - mobile does not send failure notification, fail on timeout.
     */
    private void onAirplaneModeChanged() {
        /// M: @{
        boolean airplaneModeEnabled = isAirplaneModeOn(mContext);
        if (FeatureOption.MTK_GEMINI_SUPPORT) { 
            // [ALPS00225004] When AirplaneMode On, make sure both phone1 and phone2 are radio off
            if (airplaneModeEnabled) {
                if (mServiceState1 != ServiceState.STATE_POWER_OFF || 
                    mServiceState2 != ServiceState.STATE_POWER_OFF ||
                    mServiceState3 != ServiceState.STATE_POWER_OFF ||
                    mServiceState4 != ServiceState.STATE_POWER_OFF) {
                    Xlog.d(LOG_TAG, "Unfinish! serviceState1:" + mServiceState1
                            + " serviceState2:" + mServiceState2);
                    return;
                }
            }
        } else {
            // [ALPS00127431] When AirplaneMode On, make sure phone is radio off
            if (airplaneModeEnabled) {
                if (mServiceState1 != ServiceState.STATE_POWER_OFF) {
                    Xlog.d(LOG_TAG, "Unfinish! serviceState:" + mServiceState1);
                    return;
                }
            }
        }
        Xlog.d(LOG_TAG, "Finish! airplaneModeEnabled:" + airplaneModeEnabled);
        mCheckBoxPref.setChecked(airplaneModeEnabled);
        mCheckBoxPref.setEnabled(true);
        // / @}
    }
    
    /**
     * Called when someone clicks on the checkbox preference.
     */
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (Boolean.parseBoolean(
                    SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE))) {
            // In ECM mode, do not update database at this point
        } else {
            setAirplaneModeOn((Boolean) newValue);
        }
        return true;
    }

    public void setAirplaneModeInECM(boolean isECMExit, boolean isAirplaneModeOn) {
        if (isECMExit) {
            // update database based on the current checkbox state
            setAirplaneModeOn(isAirplaneModeOn);
        } else {
            // / M: update summary when the load is not wifi only
            if (!Utils.isWifiOnly(mContext)) {
                onAirplaneModeChanged();
            }
        }
    }
    private void regListenForGeminiPlus(int state) {
        if (FeatureOption.MTK_GEMINI_3SIM_SUPPORT) {
            mTelephonyManager.listenGemini(mPhoneStateListener3,
                state,PhoneConstants.GEMINI_SIM_3);
        } else if (FeatureOption.MTK_GEMINI_4SIM_SUPPORT) {
            mTelephonyManager.listenGemini(mPhoneStateListener3,
                state,PhoneConstants.GEMINI_SIM_3);
            mTelephonyManager.listenGemini(mPhoneStateListener4,
                state,PhoneConstants.GEMINI_SIM_4);
        }    
    }
}
