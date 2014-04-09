/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.phone;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.IBluetoothHeadsetPhone;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Bluetooth headset manager for the Phone app.
 * [MTK Added] 
 * [JB.MR1] This is just a dummy service which adapts JB.MR1 APIs to JB.MR0 HFP code
 * 
 * @hide
 */
public class BluetoothPhoneService extends Service {
    private static final String TAG = "BluetoothPhoneService";
    private static final boolean DBG = true;//(PhoneGlobals.DBG_LEVEL >= 1)
            //&& (SystemProperties.getInt("ro.debuggable", 0) == 1);
    private static final boolean VDBG = true;//(PhoneGlobals.DBG_LEVEL >= 2);  // even more logging

    private static final String MODIFY_PHONE_STATE = android.Manifest.permission.MODIFY_PHONE_STATE;

    private BluetoothAdapter mAdapter;
    /* MTK Added : Begin */
    private BluetoothHandsfree mBtHandsfree;
    /* MTK Added : End */
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter == null) {
            if (VDBG) Log.d(TAG, "mAdapter null");
            return;
        }
        
        /* MTK Modified : Begin */
        //[JB.MR1] In MR1 Phone app does not init BluetoothHandsfree, so we need to init it by ourselves
        PhoneGlobals phoneApp = PhoneGlobals.getInstance();
        mBtHandsfree = BluetoothHandsfree.init(phoneApp, phoneApp.mCM);
        /* MTK Modified : End */
    }

    @Override
    public void onStart(Intent intent, int startId) {
        if (mAdapter == null) {
            Log.w(TAG, "Stopping Bluetooth BluetoothPhoneService Service: device does not have BT");
            stopSelf();
        }
        if (VDBG) Log.d(TAG, "BluetoothPhoneService started");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DBG) log("Stopping Bluetooth BluetoothPhoneService Service");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IBluetoothHeadsetPhone.Stub mBinder = new IBluetoothHeadsetPhone.Stub() {
        public boolean answerCall() {
            enforceCallingOrSelfPermission(MODIFY_PHONE_STATE, null);
            //throw UnsupportedOperationException("answerCall() is not supported yet.");
            return false;
        }

        public boolean hangupCall() {
            enforceCallingOrSelfPermission(MODIFY_PHONE_STATE, null);
            //throw UnsupportedOperationException("hangupCall() is not supported yet.");
            return false;
        }

        public boolean sendDtmf(int dtmf) {
            enforceCallingOrSelfPermission(MODIFY_PHONE_STATE, null);
            //throw UnsupportedOperationException("sendDtmf() is not supported yet.");
            return false;
        }

        public boolean processChld(int chld) {
            enforceCallingOrSelfPermission(MODIFY_PHONE_STATE, null);
            //throw UnsupportedOperationException("processChld() is not supported yet.");
            return false;
        }

        public String getNetworkOperator() {
            enforceCallingOrSelfPermission(MODIFY_PHONE_STATE, null);
            //throw UnsupportedOperationException("getNetworkOperator() is not supported yet.");
            return "";
        }

        public String getSubscriberNumber() {
            enforceCallingOrSelfPermission(MODIFY_PHONE_STATE, null);
            //throw UnsupportedOperationException("getSubscriberNumber() is not supported yet.");
            return "";
        }

        public boolean listCurrentCalls() {
            enforceCallingOrSelfPermission(MODIFY_PHONE_STATE, null);
            //throw UnsupportedOperationException("listCurrentCalls() is not supported yet.");
            return false;
        }

        public boolean queryPhoneState() {
            enforceCallingOrSelfPermission(MODIFY_PHONE_STATE, null);
            //throw UnsupportedOperationException("queryPhoneState() is not supported yet.");
            return false;
        }

        
        public void updateBtHandsfreeAfterRadioTechnologyChange() {
            enforceCallingOrSelfPermission(MODIFY_PHONE_STATE, null);
            Log.d(TAG, "updateBtHandsfreeAfterRadioTechnologyChange()");
            mBtHandsfree.updateBtHandsfreeAfterRadioTechnologyChange();
        }

        public void cdmaSwapSecondCallState() {
            enforceCallingOrSelfPermission(MODIFY_PHONE_STATE, null);
            Log.d(TAG, "cdmaSwapSecondCallState()");
            mBtHandsfree.cdmaSwapSecondCallState();
        }

        public void cdmaSetSecondCallState(boolean state) {
            enforceCallingOrSelfPermission(MODIFY_PHONE_STATE, null);
            Log.d(TAG, "cdmaSetSecondCallState("+ state +")");
            mBtHandsfree.cdmaSetSecondCallState(state);
        }
    };

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
