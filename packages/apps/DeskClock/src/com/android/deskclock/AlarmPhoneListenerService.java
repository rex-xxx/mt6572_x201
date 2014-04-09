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

package com.android.deskclock;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.ITelephony;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.telephony.TelephonyManagerEx;

/**
 * Manages alarms and vibe. Runs as a service so that it can continue to play if
 * another activity overrides the AlarmAlert dialog.
 */
public class AlarmPhoneListenerService extends Service {

    private static final int GIMINI_SIM_1 = 0;
    private static final int GIMINI_SIM_2 = 1;

    private static final int DELAY_START_ALARM = 900;

    private TelephonyManager mTelephonyManager;
    private TelephonyManagerEx mTelephonyManagerEx;
    private ITelephony mTelephonyService;
    private int mCurrentCallState;
    private int mCurrentCallState1;
    private int mCurrentCallState2;

    private Alarm mAlarm;
    private Handler mHandler = new Handler();

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String ignored) {
            if (state == TelephonyManager.CALL_STATE_IDLE && state != mCurrentCallState) {
                Log.v("state == TelephonyManager.CALL_STATE_IDLE && state != mCurrentCallState");
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        if(mAlarm != null){
                            sendStartAlarmBroadcast();
                        }
                    }
                }, DELAY_START_ALARM);
            }
        }
    };

    private PhoneStateListener mPhoneStateListener1 = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String ignored) {

            Log.v("state:" + state + "SIM1: " + mCurrentCallState1 + ", SIM2:" + mCurrentCallState2
                + ", Current:" + mCurrentCallState);

            boolean sipCallActive = (mCurrentCallState != TelephonyManager.CALL_STATE_IDLE)
                    && (mCurrentCallState1 == TelephonyManager.CALL_STATE_IDLE)
                    && (mCurrentCallState2 == TelephonyManager.CALL_STATE_IDLE);

            boolean sim1Active = mCurrentCallState1 != TelephonyManager.CALL_STATE_IDLE;

            int totalCallState = TelephonyManager.CALL_STATE_IDLE;
            try {
                totalCallState = mTelephonyService.getPreciseCallState();
            } catch (RemoteException ex) {
                Log.v("Catch exception when getPreciseCallState: ex = "
                        + ex.getMessage());
            }
            if (state != TelephonyManager.CALL_STATE_IDLE || totalCallState != TelephonyManager.CALL_STATE_IDLE) {
                 // first callback after register or totalCallStatle != idle
                return;
            } if (sim1Active || sipCallActive) {
                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            if(mAlarm != null){
                                sendStartAlarmBroadcast();
                            }
                        }
                    }, DELAY_START_ALARM);
                }
            }
    };

    private PhoneStateListener mPhoneStateListener2 = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String ignored) {
          if (state != TelephonyManager.CALL_STATE_IDLE){
              return;
              //first callback after register
          }else if (mCurrentCallState2 != TelephonyManager.CALL_STATE_IDLE){
                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            if(mAlarm != null){
                                sendStartAlarmBroadcast();
                            }
                        }
                    }, DELAY_START_ALARM);
                }
            }
    };

    @Override
    public void onCreate() {
        // Listen for incoming calls to kill the alarm.
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManagerEx = TelephonyManagerEx.getDefault();
        mTelephonyService = ITelephony.Stub.asInterface(ServiceManager
                .getService(Context.TELEPHONY_SERVICE));

        // Check if the device is gemini supported
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mTelephonyManagerEx.listen(mPhoneStateListener1,
                    PhoneStateListener.LISTEN_CALL_STATE
                            | PhoneStateListener.LISTEN_SERVICE_STATE,
                    GIMINI_SIM_1);
            mTelephonyManagerEx.listen(mPhoneStateListener2,
                    PhoneStateListener.LISTEN_CALL_STATE
                            | PhoneStateListener.LISTEN_SERVICE_STATE,
                    GIMINI_SIM_2);

        } else {

            mTelephonyManager.listen(mPhoneStateListener,
                    PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    @Override
    public void onDestroy() {

        // Stop listening for incoming calls.
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mTelephonyManagerEx.listen(mPhoneStateListener1, 0, GIMINI_SIM_1);
            mTelephonyManagerEx.listen(mPhoneStateListener2, 0, GIMINI_SIM_2);
        } else {
            mTelephonyManager.listen(mPhoneStateListener, 0);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mAlarm = intent.getParcelableExtra(Alarms.ALARM_INTENT_EXTRA);
        if (mAlarm == null) {
            Log.v("AlarmKlaxon failed to parse the alarm from the intent");
            stopSelf();
            return START_NOT_STICKY;
        }
        try {
            if (mTelephonyService != null) {
                mCurrentCallState = mTelephonyService.getPreciseCallState();
            }
            mCurrentCallState1 = mTelephonyManager
                    .getCallStateGemini(GIMINI_SIM_1);
            mCurrentCallState2 = mTelephonyManager
                    .getCallStateGemini(GIMINI_SIM_2);
            Log.v("CallState=" + mCurrentCallState + ",CallState1=" + mCurrentCallState1 + ",CallState2=" + mCurrentCallState2);
        } catch (RemoteException ex) {
            Log.v("Catch exception when getPreciseCallState: ex = "
                    + ex.getMessage());
        }
        return START_REDELIVER_INTENT;
    }

    private void sendStartAlarmBroadcast() {
        Intent startAlarm = new Intent(Alarms.ALARM_ALERT_ACTION);
        startAlarm.putExtra("setNextAlert", false);
        ///M: @{
        mAlarm.vibrate = false;
        mAlarm.silent = true;
        ///@}
        startAlarm.putExtra(Alarms.ALARM_INTENT_EXTRA, mAlarm);
        sendBroadcast(startAlarm);
        Log.v("AlarmPhoneListenerService sendStartAlarmBroadcast");
        stopSelf();
    }

}
