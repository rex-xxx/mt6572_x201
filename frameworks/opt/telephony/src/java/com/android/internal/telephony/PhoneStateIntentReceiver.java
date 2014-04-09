/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 *
 *                            DO NOT USE THIS CLASS:
 *
 *      Use android.telephony.TelephonyManager and PhoneStateListener instead.
 *
 *
 */
@Deprecated
public final class PhoneStateIntentReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = "PHONE";
    private static final boolean DBG = false;

    private static final int NOTIF_PHONE    = 1 << 0;
    private static final int NOTIF_SERVICE  = 1 << 1;
    private static final int NOTIF_SIGNAL   = 1 << 2;

    private static final int NOTIF_MAX      = 1 << 5;

    PhoneConstants.State mPhoneState = PhoneConstants.State.IDLE;
    ServiceState mServiceState = new ServiceState();
    SignalStrength mSignalStrength = new SignalStrength();

    //MTK-START [mtk04070][111117][ALPS00093395]Used for SIM2
    ServiceState mServiceState2 = new ServiceState();    
    SignalStrength mSignalStrength2 = new SignalStrength();
    //MTK-END [mtk04070][111117][ALPS00093395]Used for SIM2


    private Context mContext;
    private Handler mTarget;
    private IntentFilter mFilter;
    private int mWants;
    private int mPhoneStateEventWhat;
    private int mServiceStateEventWhat;
    private int mLocationEventWhat;
    private int mAsuEventWhat;

    public PhoneStateIntentReceiver() {
        super();
        mFilter = new IntentFilter();
    }

    public PhoneStateIntentReceiver(Context context, Handler target) {
        this();
        setContext(context);
        setTarget(target);
    }

    public void setContext(Context c) {
        mContext = c;
    }

    public void setTarget(Handler h) {
        mTarget = h;
    }

    public PhoneConstants.State getPhoneState() {
        if ((mWants & NOTIF_PHONE) == 0) {
            throw new RuntimeException
                ("client must call notifyPhoneCallState(int)");
        }
        return mPhoneState;
    }

    public ServiceState getServiceState() {
       //MTK-START [mtk04070][111117][ALPS00093395]Modify for Gemini
        return getServiceStateGemini(PhoneConstants.GEMINI_SIM_1);
       //MTK-END [mtk04070][111117][ALPS00093395]Modify for Gemini
    }

    /**
     * Returns current signal strength in as an asu 0..31
     *
     * Throws RuntimeException if client has not called notifySignalStrength()
     */
    public int getSignalStrengthLevelAsu() {
        // TODO: use new SignalStrength instead of asu
        //MTK-START [mtk04070][111117][ALPS00093395]Modified for Gemini
        return getSignalStrengthGemini(PhoneConstants.GEMINI_SIM_1);
        //MTK-END [mtk04070][111117][ALPS00093395]Modified for Gemini
    }

    /**
     * Return current signal strength in "dBm", ranging from -113 - -51dBm
     * or -1 if unknown
     *
     * @return signal strength in dBm, -1 if not yet updated
     * Throws RuntimeException if client has not called notifySignalStrength()
     */
    public int getSignalStrengthDbm() {
        //MTK-START [mtk04070][111117][ALPS00093395]Modified for Gemini
        return getSignalStrengthDbmGemini(PhoneConstants.GEMINI_SIM_1);
        //MTK-END [mtk04070][111117][ALPS00093395]Modified for Gemini
    }

    public void notifyPhoneCallState(int eventWhat) {
        mWants |= NOTIF_PHONE;
        mPhoneStateEventWhat = eventWhat;
        mFilter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
    }

    public boolean getNotifyPhoneCallState() {
        return ((mWants & NOTIF_PHONE) != 0);
    }

    public void notifyServiceState(int eventWhat) {
        mWants |= NOTIF_SERVICE;
        mServiceStateEventWhat = eventWhat;
        mFilter.addAction(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED);
    }

    public boolean getNotifyServiceState() {
        return ((mWants & NOTIF_SERVICE) != 0);
    }

    public void notifySignalStrength (int eventWhat) {
        mWants |= NOTIF_SIGNAL;
        mAsuEventWhat = eventWhat;
        mFilter.addAction(TelephonyIntents.ACTION_SIGNAL_STRENGTH_CHANGED);
    }

    public boolean getNotifySignalStrength() {
        return ((mWants & NOTIF_SIGNAL) != 0);
    }

    public void registerIntent() {
        mContext.registerReceiver(this, mFilter);
    }

    public void unregisterIntent() {
        mContext.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        try {
            if (TelephonyIntents.ACTION_SIGNAL_STRENGTH_CHANGED.equals(action)) {
                //MTK-START [mtk04070][111117][ALPS00093395]Modified for Gemini
                SignalStrength tempSignalStrength = SignalStrength.newFromBundle(intent.getExtras());

                if (tempSignalStrength.getMySimId() == PhoneConstants.GEMINI_SIM_1)
                    mSignalStrength = tempSignalStrength;
                else
                    mSignalStrength2 = tempSignalStrength;
                //MTK-END [mtk04070][111117][ALPS00093395]Modified for Gemini

                if (mTarget != null && getNotifySignalStrength()) {
                    Message message = Message.obtain(mTarget, mAsuEventWhat);
                    mTarget.sendMessage(message);
                }
            } else if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
                if (DBG) Log.d(LOG_TAG, "onReceiveIntent: ACTION_PHONE_STATE_CHANGED, state="
                               + intent.getStringExtra(PhoneConstants.STATE_KEY));
                String phoneState = intent.getStringExtra(PhoneConstants.STATE_KEY);
                mPhoneState = (PhoneConstants.State) Enum.valueOf(
                        PhoneConstants.State.class, phoneState);

                if (mTarget != null && getNotifyPhoneCallState()) {
                    Message message = Message.obtain(mTarget,
                            mPhoneStateEventWhat);
                    mTarget.sendMessage(message);
                }
            } else if (TelephonyIntents.ACTION_SERVICE_STATE_CHANGED.equals(action)) {
                //MTK-START [mtk04070][111117][ALPS00093395]Modified for Gemini
                ServiceState tempServiceState = ServiceState.newFromBundle(intent.getExtras());

                if (tempServiceState.getMySimId() == PhoneConstants.GEMINI_SIM_1)
                    mServiceState = tempServiceState;
                else
                    mServiceState2 = tempServiceState;
                //MTK-END [mtk04070][111117][ALPS00093395]Modified for Gemini

                if (mTarget != null && getNotifyServiceState()) {
                    Message message = Message.obtain(mTarget,
                            mServiceStateEventWhat);
                    mTarget.sendMessage(message);
                }
            }
        } catch (Exception ex) {
            Log.e(LOG_TAG, "[PhoneStateIntentRecv] caught " + ex);
            ex.printStackTrace();
        }
    }

    //MTK-START [mtk04070][111117][ALPS00093395]MTK proprietary methods
    public ServiceState getServiceStateGemini(int simId) {
        if ((mWants & NOTIF_SERVICE) == 0) {
            throw new RuntimeException
                ("client must call notifyServiceState(int)");
        }

        if (simId == PhoneConstants.GEMINI_SIM_1)
        return mServiceState;
        else
            return mServiceState2;
    }
    
    public int getSignalStrengthGemini(int simId) {
        if ((mWants & NOTIF_SIGNAL) == 0) {
            throw new RuntimeException
                ("client must call notifySignalStrength(int)");
        }

        int gsmSignalStrength;
        
        if (simId == PhoneConstants.GEMINI_SIM_1)
            gsmSignalStrength = mSignalStrength.getAsuLevel();
        else
            gsmSignalStrength = mSignalStrength2.getAsuLevel();

        return gsmSignalStrength;
    }
    
    public int getSignalStrengthDbmGemini(int simId) {
        if ((mWants & NOTIF_SIGNAL) == 0) {
            throw new RuntimeException
                ("client must call notifySignalStrength(int)");
        }

        int dBm = -1;
        SignalStrength tempSignalStrength;

        if (simId == PhoneConstants.GEMINI_SIM_1)
            tempSignalStrength = mSignalStrength;
        else
            tempSignalStrength = mSignalStrength2;            

        return tempSignalStrength.getDbm();
    }    
    //MTK-END [mtk04070][111117][ALPS00093395]MTK proprietary methods
}
