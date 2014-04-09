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

package com.mediatek.hdmi;

import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;

import java.io.FileReader;
import java.io.FileNotFoundException;

import com.mediatek.common.featureoption.FeatureOption;
//add for MFR
import com.mediatek.common.hdmi.IHDMIObserver;

/**
 * <p>HDMIObserver monitors for HDMI cable.
 */
//add IHDMIObserver interface for MFR
public class HDMIObserver extends UEventObserver implements IHDMIObserver {
//    private static final String TAG = HDMIObserver.class.getSimpleName();
    private static final String TAG = "hdmi";
    private static final boolean LOG = true;
    
    private static final String HDMI_UEVENT_MATCH = "DEVPATH=/devices/virtual/switch/hdmi"; 
    private static final String HDMI_UEVENT_MATCH_MTK = "DEVPATH=/devices/virtual/switch/mtk_hdmi";
    private static final String HDMI_STATE_PATH = "/sys/class/switch/mtk_hdmi/state";
    private static final String HDMI_NAME_PATH = "/sys/class/switch/mtk_hdmi/name";

//    private static final String HDMI_UEVENT_MATCH = "DEVPATH=/devices/virtual/switch/h2w";
//    private static final String HDMI_STATE_PATH = "/sys/class/switch/h2w/state";
//    private static final String HDMI_NAME_PATH = "/sys/class/switch/h2w/name";
//    
    
    private int mHDMIState;
    private int mPrevHDMIState;
    private String mHDMIName;

    private final Context mContext;
    private final WakeLock mWakeLock;  // held while there is a pending route change

    public HDMIObserver(Context context) {
        mContext = context;
        PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HeadsetObserver");
        mWakeLock.setReferenceCounted(false);

        startObserving(HDMI_UEVENT_MATCH);
		startObserving(HDMI_UEVENT_MATCH_MTK);

        init();  // set initial status
    }

    @Override
    public void onUEvent(UEventObserver.UEvent event) {
        if (LOG) Slog.v(TAG, "HDMIObserver: HDMI UEVENT: " + event.toString());

        try {
            String name = event.get("SWITCH_NAME");
            int state = Integer.parseInt(event.get("SWITCH_STATE"));
            Slog.i(TAG, "HDMIObserver.onUEvent(), name="+name+", state="+state);
            update(name, state);
        } catch (NumberFormatException e) {
            Slog.e(TAG, "HDMIObserver: Could not parse switch state from event " + event);
        }
    }

    private synchronized final void init() {
        char[] buffer = new char[1024];

        String newName = mHDMIName;
        int newState = mHDMIState;
        mPrevHDMIState = mHDMIState;
        try {
            FileReader file = new FileReader(HDMI_STATE_PATH);
            int len = file.read(buffer, 0, 1024);
            newState = Integer.valueOf((new String(buffer, 0, len)).trim());

            file = new FileReader(HDMI_NAME_PATH);
            len = file.read(buffer, 0, 1024);
            newName = new String(buffer, 0, len).trim();
            Slog.i(TAG, "HDMIObserver.init(), state="+newState+", newName="+newName);
        } catch (FileNotFoundException e) {
            Slog.w(TAG, "This kernel does not have HDMI support");
        } catch (Exception e) {
            Slog.e(TAG, "" , e);
        }

        update(newName, newState);
    }

    private synchronized final void update(String newName, int newState) {
        Log.d(TAG, "HDMIOberver.update(), oldState="+mHDMIState+", newState="+newState);
        // Retain only relevant bits
        int HDMIState = newState;
        int newOrOld = HDMIState | mHDMIState;
        int delay = 0;
        // reject all suspect transitions: only accept state changes from:
        // - a: 0 HDMI to 1 HDMI
        // - b: 1 HDMI to 0 HDMI

        /** HDMI states
           * HDMI_STATE_NO_DEVICE
           * HDMI_STATE_ACTIVE
           *
           * Following are for MT8193
           *
           * HDMI_STATE_PLUGIN_ONLY
           * HDMI_STATE_EDID_UPDATE
           * HDMI_STATE_CEC_UPDATE
           */

//        use headset to simulate
        if (FeatureOption.MTK_MT8193_HDMI_SUPPORT) {
            if (mHDMIState == HDMIState) {
                return;
            }
        } else {
            if (mHDMIState == HDMIState || ((newOrOld & (newOrOld - 1)) != 0)) {
                return;
            }
        }

        mHDMIName = newName;
        mPrevHDMIState = mHDMIState;
        mHDMIState = HDMIState;

        mWakeLock.acquire();
        mHandler.sendMessageDelayed(mHandler.obtainMessage(0, mHDMIState, mPrevHDMIState, mHDMIName), delay);
    }

    private synchronized final void sendIntents(int HDMIState, int prevHDMIState, String HDMIName) {
        int curHDMI = 1;
//        int curHDMI = 3;
        sendIntent(curHDMI, HDMIState, prevHDMIState, HDMIName);
    }

    private final void sendIntent(int HDMI, int HDMIState, int prevHDMIState, String HDMIName) {
        if ((HDMIState & HDMI) != (prevHDMIState & HDMI)) {
            //  Pack up the values and broadcast them to everyone
            Intent intent = new Intent(Intent.ACTION_HDMI_PLUG);
            intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
            int state = 0;
            if ((HDMIState & HDMI) != 0) {
                state = 1;
            }
            intent.putExtra("state", state);
            intent.putExtra("name", HDMIName);

            if (LOG) Slog.e(TAG, "HDMIObserver: Broadcast HDMI event, ACTION_HEADSET_PLUG: state: "+state+" name: "+HDMIName);
            // TODO: Need to test for solve build error
            ActivityManagerNative.broadcastStickyIntent(intent, null, UserHandle.myUserId());
        }
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            sendIntents(msg.arg1, msg.arg2, (String)msg.obj);
            mWakeLock.release();
        }
    };
}
